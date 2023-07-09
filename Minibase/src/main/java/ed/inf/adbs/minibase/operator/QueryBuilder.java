package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.Catalog;
import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class build the query plan based of the input query
 * and optimized it, and runQueryBuilder() function will return
 * the root operator of this query plan.
 */
public class QueryBuilder {
    //the input query
    private final Query query;
    //the list of query's relations
    private List<RelationalAtom> relationalAtomList;
    //list of appeared variables, used for variable renaming
    public static List<String> appearedVariableName;
    //work together with the above variable, used for variable renaming
    public static char unappearedChar;
    //list of all comparison atoms
    private List<ComparisonAtom> comparisonAtomList;
    //list of selection atoms that not used as a join condition
    private List<ComparisonAtom> selectAtomList;
    //list of join condition, that can be a join condition for 2 or more relations
    private List<ComparisonAtom> joinAtomList;
    //relations' variables, used mainly in variable renaming
    private Map<String,List<List<String>>> relationToInitialVariables;
    //these variables that either in head output or comparison atoms
    //used in projection for query optimisation
    private ArrayList<String> necessaryVariables;

    /**
     * Constructor for queryBuilder, set essential data.
     * @param query the input query
     */
    public QueryBuilder(Query query){
        this.query = query;
        unappearedChar = 'a';
    }

    /**
     * function that generate and optimise query plan, and return the root operator
     * This function's work is mainly about data preprocessing, but at its last line,
     * it will call buildQuery() to generate plan and return root based on the preprocessed data.
     * @return the root operator
     */
    public Operator runQueryBuilder(){
        //handle empty query
        if (query == null){
            return null;
        }
        //initialize variables
        relationalAtomList = new ArrayList<>();
        comparisonAtomList = new CopyOnWriteArrayList<>();
        appearedVariableName = new ArrayList<>();
        relationToInitialVariables = new HashMap<>();
        joinAtomList = new ArrayList<>();
        selectAtomList = new ArrayList<>();
        necessaryVariables = new ArrayList<>();
        //add head output's variables to necessary variable list
        for (Variable variable:query.getHead().getVariables()){
            necessaryVariables.add(variable.getName());
        }
        //split atoms to relations and comparisons, and for comparisons, store
        //their variable terms to list of necessary variables
        for (Atom atom: query.getBody()){
            if (atom instanceof RelationalAtom){
                relationalAtomList.add((RelationalAtom) atom);
                for (Term term: ((RelationalAtom) atom).getTerms()){
                    if (term instanceof Variable && !appearedVariableName.contains(((Variable) term).getName())){
                        appearedVariableName.add(((Variable) term).getName());
                    }
                }
            }
            else {
                comparisonAtomList.add((ComparisonAtom) atom);
                Term term1 = ((ComparisonAtom) atom).getTerm1();
                Term term2 = ((ComparisonAtom) atom).getTerm2();
                if (!(term1 instanceof Constant)&&!necessaryVariables.contains((term1.toString()))){
                    necessaryVariables.add(term1.toString());
                }
                if (!(term2 instanceof Constant)&&!necessaryVariables.contains(term2.toString())){
                    necessaryVariables.add(term2.toString());
                }
            }
        }

        //convert relation atom that include constant to variable form and add extra comparison for it
        //for example, convert R(1,x,y) to R(a,x,y),a=1
        for (int i = 0;i < relationalAtomList.size();i++){
            RelationalAtom atom = relationalAtomList.get(i);
            List<Term> terms = atom.getTerms();
            List<String> variableString = new ArrayList<>();
            for (int j = 0;j<terms.size();j++){
                //if contains constant, convert the relation and add comparison for it
                if (terms.get(j) instanceof Constant){
                    Term originalTerm = terms.get(j);
                    RelationalAtom newAtom = convertAtom(atom,j);
                    Variable newVariable = (Variable) newAtom.getTerms().get(j);
                    comparisonAtomList.add(new ComparisonAtom(newVariable, originalTerm, ComparisonOperator.EQ));
                    relationalAtomList.set(i,newAtom);
                }
                else{
                    variableString.add(((Variable) terms.get(j)).getName());
                }
            }
            //update the variables in mapping tables if they changed in above reformat stage.
            if (relationToInitialVariables.containsKey(atom.getName())){
                List<List<String>> tempList = relationToInitialVariables.get(atom.getName());
                tempList.add(variableString);
                relationToInitialVariables.replace(atom.getName(),tempList);
            }
            else {
                List<List<String>> tempList = new ArrayList<>();
                tempList.add(variableString);
                relationToInitialVariables.put(atom.getName(),tempList);
            }
        }
        //call buildQuery function to get root based on input relations and comparisons.
        Operator root = buildQuery();
        return root;
    }

    /**
     * function that generate plan and return root operator
     * @return
     */
    public Operator buildQuery(){
        Operator root = null;
        //split compare atoms to selection compare list and join condition list.
        splitCompare();
        //query relations 1 by 1
        for (int i = 0;i<relationalAtomList.size();i++){
            //initialize variables
            RelationalAtom atom = relationalAtomList.get(i);
            //each relation's base operator should be scanOperator to get data
            Operator operatorPointer = new ScanOperator(atom);
            List<ComparisonAtom> suitableCompare = new ArrayList<>();
            List<String> variableString = operatorPointer.variableList;
            //query selections to find selection that suitable for this case
            for (ComparisonAtom compare: selectAtomList){
                Term term1 = compare.getTerm1();
                Term term2 = compare.getTerm2();
                if (checkCompareSuit(term1,term2,variableString)){
                    suitableCompare.add(compare);
                }
            }
            //if there is at least 1 suitable selection condition
            //create a selectOperator using the base scanOperator as child.
            if (!suitableCompare.isEmpty()){
                operatorPointer = new SelectOperator(operatorPointer,suitableCompare);
            }
            //if this relation is the first relation, then set the current operator as root
            if (root == null){
                root = operatorPointer;
            }
            else {
                //else join it with the previous root
                List<ComparisonAtom> joinComparison = new ArrayList<>();
                //find suitable join conditions
                for (ComparisonAtom comparisonAtom: joinAtomList){
                    if (isJoinPair(comparisonAtom,root.getVariableList(),operatorPointer.getVariableList())){
                        joinComparison.add(comparisonAtom);
                    }
                }
                //change the root to this joinOperator
                root = new JoinOperator(root,operatorPointer,joinComparison);
            }
            //if these query do not have SUM(), then apply projection to remove duplication
            // and simplify intermediate tuple to reduce intermediate result
            //the projection's target variables are variables in necessaryVariableList, so
            //it will not influence the output.
            if (query.getHead().getSumAggregate()== null && root.variableList!=null){
                if (root.getVariableList().size()>(necessaryVariables.size()+1)) {
                    List<Variable> projectObj = new ArrayList<>();
                    for (String variable : root.getVariableList()) {
                        if (necessaryVariables.contains(variable)) {
                            projectObj.add(new Variable(variable));
                        }
                    }
                    Head input = new Head(query.getHead().getName(), projectObj, query.getHead().getSumAggregate());
                    root = new ProjectOperator(root, input);
                }
            }
        }
        //if this query don't have SUM(), put root under projectOperator and return it as new root
        if (query.getHead().getSumAggregate()== null){
            return new ProjectOperator(root,query.getHead());
        }
        //otherwise use sumOperator and return it as root
        else {
            return new SumOperator(root,query.getHead());
        }
    }

    /**
     * Check if this compare is a join condition,
     * by checking if the compare involve more than 1 relation
     * @param compare the check object
     * @param atomList the list of all relations
     * @return if it is a join condition
     */
    public boolean isJoinCondition(ComparisonAtom compare,List<RelationalAtom> atomList){
        Term term1 = compare.getTerm1();
        Term term2 = compare.getTerm2();
        if (term1 instanceof Constant && term2 instanceof Constant){return false;}
        return involveEnoughRelation(atomList, term1, term2);
    }

    /**
     * split comparisonAtoms to select condition and join condition
     */
    public void splitCompare(){
        for (ComparisonAtom comparisonAtom:comparisonAtomList){
            if (isJoinCondition(comparisonAtom,relationalAtomList)){
                joinAtomList.add(comparisonAtom);
            }
            else {
                selectAtomList.add(comparisonAtom);
            }
        }
    }

    /**
     * check if this 2 terms appear in more than 1 atom
     * @param atomList list of all atoms
     * @param term1 term1
     * @param term2 term2
     * @return if this 2 terms appear in more than 1 atom
     */
    private boolean involveEnoughRelation(List<RelationalAtom> atomList, Term term1, Term term2) {
        Integer count=0;
        for (RelationalAtom atom:atomList){
            if (involveVariable(atom,term1,term2)){
                count+=1;
                if (count >=2){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the input join condition can be a suitable join comparison for the 2 input atoms
     * @param compare input join condition
     * @param atom1Variable atom1's variables
     * @param atom2Variable atom2's variables
     * @return if the join condition suitable here.
     */
    public boolean isJoinPair(ComparisonAtom compare,List<String> atom1Variable,List<String> atom2Variable){
        Term term1 = compare.getTerm1();
        Term term2 = compare.getTerm1();
        if (term1 instanceof Constant && term2 instanceof Constant){return false;}
        List<List<String>> variableList = new ArrayList<>();
        variableList.add(atom1Variable);
        variableList.add(atom2Variable);
        Integer count = 0;
        for (List<String> variables: variableList){
            if (containsVariable(variables,compare)){
                count+=1;
                if (count>=2){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check if the variable appears in this comparisonAtom
     * @param variables input variable
     * @param comparisonAtom input comparisonAtom
     * @return if the variable appears in this comparisonAtom
     */
    public boolean containsVariable(List<String> variables,ComparisonAtom comparisonAtom){
        Term term1 = comparisonAtom.getTerm1();
        Term term2 = comparisonAtom.getTerm2();
        if (term1 instanceof Variable){
            if (variables.contains(((Variable) term1).getName())){
                return true;
            }
        }
        if (term2 instanceof Variable){
            if (variables.contains(((Variable) term2).getName())){
                return true;
            }
        }
        return false;
    }

    /**
     * check if this RelationalAtom involve any of these 2 term
     * @param atom input RelationalAtom
     * @param term1 term1
     * @param term2 term2
     * @return if this RelationalAtom involve any of these 2 term
     */
    public boolean involveVariable(RelationalAtom atom,Term term1,Term term2){
        for (Term termHere: atom.getTerms()){
            if (termHere instanceof Constant){
                continue;
            }
            if (term1 instanceof Variable) {
                if (((Variable) termHere).getName().equals(((Variable) term1).getName())) {
                    return true;
                }
            }
            if (term2 instanceof Variable){
                if (((Variable) termHere).getName().equals(((Variable) term2).getName())){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * check if the relation's variable table contains all variables in this comparison
     * @param term1 comparison's term1
     * @param term2 comparison's term2
     * @param variableString relation's variable table
     * @return if this selection compare is suitable for this relation
     */
    public boolean checkCompareSuit(Term term1,Term term2,List<String> variableString){
        if (term1 instanceof Variable){
            if (!variableString.contains(((Variable) term1).getName())){
                return false;
            }
        }
        if (term2 instanceof Variable){
            if (!variableString.contains(((Variable) term2).getName())){
                return false;
            }
        }
        return true;
    }

    /**
     * reformat the relationalAtom, change the atom's variable at position index
     * to an unappeared char
     * @param atom atom object
     * @param index object variable's position
     * @return a reformatted atom
     */
    public RelationalAtom convertAtom(Atom atom,Integer index){
        List<Term> terms = ((RelationalAtom) atom).getTerms();
        String name = ((RelationalAtom) atom).getName();
        boolean find = false;
        while (!find){
            String variable = Character.toString(unappearedChar);
            if (!appearedVariableName.contains(variable)) {
                appearedVariableName.add(variable);
                terms.set(index,new Variable(variable));
                find = true;
            }
            unappearedChar+=1;
        }
        return new RelationalAtom(name,terms);
    }
}
