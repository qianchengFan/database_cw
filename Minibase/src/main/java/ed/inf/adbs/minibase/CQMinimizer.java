package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * Minimization of conjunctive queries
 *
 */
public class CQMinimizer {
    //used to store variables in body atoms that also appear in the head
    private static List<Term> bodyHeadTerms;
    //store mapping between relation name to list of relations appeared in body with this relation type
    private static Map<String, ArrayList<ArrayList<Term>>> relations;
    //store mapping between a body relation atom to its possible homomorphism
    private static Map<RelationalAtom,ArrayList<RelationalAtom>> possibleHomomorphism;

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage: CQMinimizer input_file output_file");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];

        minimizeCQ(inputFile, outputFile);
    }

    /**
     * This method is used to write the minimized query to output file
     * @param filePath the file path of output file
     * @param data the body of query
     * @param head the head of query
     */
    public static void writeToFile(String filePath, List<RelationalAtom> data, Head head){
        try {
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filePath));

            //change input format to list of Atoms and build new query
            List<Atom> returnList = new ArrayList<>(data);
            Query returnQuery = new Query(head, returnList);

            //write to file
            fileWriter.write(returnQuery.toString()+'\n');
            fileWriter.close();
        }
        catch (IOException e) {
            System.out.print(e.getMessage());
        }
    }

    /**
     * CQ minimization procedure
     * The main body of the minimization algorithm
     *
     * @param inputFile path of input file
     * @param outputFile path of output file
     *
     * Assume the body of the query from inputFile has no comparison atoms
     * but could potentially have constants in its relational atoms.
     *
     */
    public static void minimizeCQ(String inputFile, String outputFile) {
        // TODO: add your implementation
        try {
            //read the original query, record essential data
            Query query = QueryParser.parse(Paths.get(inputFile));
            Head head = query.getHead();
            List<Atom> body = query.getBody();
            List<Variable> headTerms = head.getVariables();

            //initialize variables for intermediate data storing
            List<RelationalAtom> bodyAtoms = new ArrayList<>();
            bodyHeadTerms = new ArrayList<>();
            relations = new HashMap<>();
            possibleHomomorphism = new HashMap<>();

            //collect essential data for processing minimization
            for (Atom atom : body) {
                //if this atom is a valid relational atom
                if (atom instanceof RelationalAtom) {
                    //store this atom to body atom list
                    bodyAtoms.add((RelationalAtom) atom);
                    //collect essential data
                    String relationName = ((RelationalAtom) atom).getName();
                    List<Term> terms = ((RelationalAtom) atom).getTerms();
                    //update relations mapping table
                    //update mapping or insert a new mapping if it's a new relation
                    if (relations.containsKey(relationName)) {
                        ArrayList<ArrayList<Term>> temp = new ArrayList<>(relations.get(relationName));
                        temp.add((ArrayList<Term>) terms);
                        relations.replace(relationName, temp);
                    } else {
                        ArrayList<ArrayList<Term>> temp = new ArrayList<>();
                        temp.add((ArrayList<Term>) terms);
                        relations.put(relationName, temp);
                    }

                    //record body's terms that appear in head
                    for (Term term : ((RelationalAtom) atom).getTerms()) {
                        if (term instanceof Variable) {
                            for (Variable variable : headTerms) {
                                if (variable.toString().equals(term.toString())) {
                                    bodyHeadTerms.add(term);
                                }
                            }
                        }
                    }
                }
            }
            //flag to indicate whether this minimization is finish
            boolean flag = false;
            do {
                //boolean to indicate whether there is a query change in this round
                boolean noQueryChange=true;
                //check body atoms one by one
                //to see whether it can be removed
                //if valid to remove, remove it and keep checking the rest
                for (int i = bodyAtoms.size(); i > 0; i--) {
                    int testIndex = bodyAtoms.size() - i;
                    //check if there is candidate homomorphism
                    if (canFindCandidateHomomorphism(testIndex, bodyAtoms)) {
                        //check if any candidate is valid
                        if (checkHomomorphism(bodyAtoms.get(testIndex))) {
                            bodyAtoms.remove(testIndex);
                            //update change flag
                            noQueryChange = false;
                        }
                    }
                }
                //if nothing can change, minimization finish
                if (noQueryChange){
                    flag = true;
                }
                //if there is something changed
                // do one more round for checking
            } while (!flag);
            writeToFile(outputFile, bodyAtoms, head);
        }
        catch (Exception e)
        {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }
    }

    /**
     * Used to check if any candidate homomorphism of this atom is valid
     * @param currentAtom the atom to check
     * @return boolean to indicate whether this atom can be removed
     */
    public static boolean checkHomomorphism(RelationalAtom currentAtom){
        //collect essential data for processing
        List<Term> currentTerms = currentAtom.getTerms();
        ArrayList<RelationalAtom> possibleHomos = possibleHomomorphism.get(currentAtom);
        Map<String, Term> mappedTerms = new HashMap<>();

        //loop through all possible homomorphisms
        //if there is a valid one, return true
        for (RelationalAtom testAtom : possibleHomos){
            List<Term> testTerms = testAtom.getTerms();
            //filter out the case that these 2 atoms have same terms with different order
            //for example, R(x,y),R(y,x) is not homomorphism to R(x,y)
            if (justChangeOrder((ArrayList<Term>) testTerms, (ArrayList<Term>) currentAtom.getTerms())){
                continue;
            }
            //record terms mapping for this candidate homomorphism
            for (int i = 0; i < testTerms.size(); i++) {
                if (!currentTerms.get(i).equals(testTerms.get(i))){
                    mappedTerms.put(currentTerms.get(i).toString(),testTerms.get(i));
                }
            }
            //this flag is used to indicate if this candidate is invalid
            //when we find a case can show this one is invalid
            //we turn the flag to true and break out of this loop
            Boolean flag = false;
            //check according to relation order
            for (String name : relations.keySet()){
                if (flag){
                    break;
                }
                //record this relation type's relation list
                ArrayList<ArrayList<Term>> currentRelationList = relations.get(name);
                //loop through each relation's atom list
                for (ArrayList<Term> atom : currentRelationList){
                    //skip itself
                    if (sameTermValue(atom, (ArrayList<Term>) currentAtom.getTerms())){continue;}

                    Integer index = currentRelationList.indexOf(atom);
                    //apply terms mapping
                    ArrayList<Term> mappedAtom = useMapping(mappedTerms,atom);
                    Boolean valid = false;
                    //if there is a change after mapping,do checking
                    if (!sameTermValue(mappedAtom,atom)){
                        //check if the mapped atom is a subset of original query
                        for (ArrayList<Term> atom2 : currentRelationList){
                            if (!index.equals(currentRelationList.indexOf(atom2))){
                                if (sameTermValue(atom2,mappedAtom)){
                                    valid = true;
                                }
                            }
                        }
                    }
                    else {
                        valid = true;
                    }
                    //if a mapped atom not a subset of original query
                    //this mapping is invalid, return invalid is true
                    if (!valid){
                        flag = true;
                        break;
                    }
                }
            }
            //if this change is valid, remove this atom from related mapping table
            //and return true
            if(!flag){
                ArrayList<Term> needRemove = (ArrayList<Term>) currentAtom.getTerms();
                relations.get(currentAtom.getName()).remove(needRemove);
            return true;}
        }
        return false;
    }

    /**
     * method to check if two atom contains same variables with different order
     * @param a1 atom1
     * @param a2 atom2
     * @return checking result
     */
    public static boolean justChangeOrder(ArrayList<Term> a1, ArrayList<Term> a2){
        boolean flag = true;
        //if their variable order also the same, return false
        if (sameTermValue(a1,a2)){
            return false;
        }
        //else, check if they contain same variables
        for (Term currentTerm : a1){
            boolean contains = false;
            for (Term currentTerm2: a2){
                if (currentTerm2.toString().equals(currentTerm.toString())){
                    contains = true;
                    break;
                }
            }
            //if you find a difference, return false
            if (!contains){
                flag = false;
                break;
            }
        }
        //otherwise return true
        return flag;
    }

    /**
     * method to check if these atoms are totally the same
     * @param a1 atom1
     * @param a2 atom2
     * @return checking result
     */
    public static boolean sameTermValue(ArrayList<Term> a1, ArrayList<Term> a2){
        for (int i = 0; i < a2.size();i++){
            if (!a1.get(i).toString().equals(a2.get(i).toString())){
                return false;
            }
        }
        return true;
    }

    /**
     * method to apply mapping on an atom
     * @param mappedTerms mapping table
     * @param currentAtom the atom to apply mapping
     * @return mapped atom
     */
    public static ArrayList<Term> useMapping(Map<String, Term> mappedTerms, ArrayList<Term> currentAtom){
        //initialise a new list to store the mapped atoms
        ArrayList<Term> changedAtom = new ArrayList<>();
        //do mapping term by term
        for (Term term: currentAtom){
            if (!mappedTerms.containsKey(term.toString())){
                    changedAtom.add(term);
                }
                else{
                    changedAtom.add(mappedTerms.get(term.toString()));
                }
        }
        return changedAtom;
    }

    /**
     * Method to find candidate homomorphisms for an atom,
     * if found, update the possible homomorphism table and return true
     * @param testAtoms the atom that want to find candidate homomorphism
     * @param inputAtoms all body atoms
     * @return a boolean to show if any candidate found
     */
    public static boolean canFindCandidateHomomorphism(int testAtoms, List<RelationalAtom> inputAtoms){
        //record data for processing
        String name = inputAtoms.get(testAtoms).getName();
        List<Term> testTermList = inputAtoms.get(testAtoms).getTerms();

        boolean find = false;
        //loop through all atoms try to find candidate homomorphism
        for (RelationalAtom currentAtom: inputAtoms) {
            //skip itself, and skip atoms from other relation types
            if (inputAtoms.indexOf(currentAtom) == testAtoms
                || !Objects.equals(currentAtom.getName(), name)){continue;}

            List<Term> currentTerms = currentAtom.getTerms();
            boolean findHomomorphism = false;
            //check term by term
            for (Term currentTerm: currentTerms) {

                //these 2 blocks are used for preventing map output terms to other variable
                if(bodyHeadTerms.contains(currentTerm)){
                    if (!(inputAtoms.get(testAtoms).getTerms().get(currentTerms.indexOf(currentTerm)) instanceof Variable)){
                        findHomomorphism = false;
                        break;
                    }
                    else{
                        findHomomorphism = true;
                        continue;
                    }
                }
                if (bodyHeadTerms.contains(inputAtoms.get(testAtoms).getTerms().get(currentTerms.indexOf(currentTerm)))){
                    if (!inputAtoms.get(testAtoms).getTerms().get(currentTerms.indexOf(currentTerm)).toString().equals(currentTerm.toString())){
                        findHomomorphism = false;
                        break;
                    }
                    else{
                        findHomomorphism = true;
                        continue;
                    }
                }

                int index = currentTerms.indexOf(currentTerm);
                Term testTerm = testTermList.get(index);
                //these blocks pick candidate homomorphism according to term type
                if(currentTerm instanceof Constant){
                    //variable is possible to map to constant
                    if (testTerm instanceof  Variable){
                        findHomomorphism = true;
                    }
                    //but constant can not map to another constant
                    //unless they are the same value and map to themselves
                    else{
                        if(!(testTerm.toString()).equals(currentTerm.toString())){
                            findHomomorphism = false;
                            break;
                        }
                    }
                }
                else{
                    if (testTerm instanceof  Variable) {
                        findHomomorphism = true;
                    }
                    else{
                        findHomomorphism = false;
                        break;
                    }
                }
            }
            //if you find candidate, record it
            if(findHomomorphism){
                find = true;
                if (possibleHomomorphism.containsKey(inputAtoms.get(testAtoms))){
                    ArrayList<RelationalAtom> temp = new ArrayList<>(possibleHomomorphism.get(inputAtoms.get(testAtoms)));
                    temp.add(currentAtom);
                    possibleHomomorphism.replace(inputAtoms.get(testAtoms),temp);

                }
                else {
                    ArrayList<RelationalAtom> temp = new ArrayList<>();
                    temp.add(currentAtom);
                    possibleHomomorphism.put(inputAtoms.get(testAtoms),temp);
                }
            }
        }
        return find;
    }
}
