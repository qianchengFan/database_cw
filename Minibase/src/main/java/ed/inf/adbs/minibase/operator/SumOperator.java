package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.*;
/**
 * this class implements SumOperator, the implementation is similar to an
 * advanced version of projectOperator, which supports SUM()'s calculation.
 * In this operator, for each nextTuple(), we need to loop through all child's output tuple
 * and find the tuple that have not been outputted before, and in each nextTuple loop,
 * for this current output target, we will calculate this tuple's duplication's SUM according
 * to its SUM()'s variables, if in a loop we do not find any tuple that have not been outputted before,
 * that means we outputted all valid outputs, and it's time to return null.
 */
public class SumOperator extends Operator{
    //the child operator
    private final Operator child;
    //variables for sum calculation
    private final List<String> sumVariables;
    //variables need to be projected out
    private final List<String> projectVariables;
    //appeared group, already outputted tuple
    private final List<List<Term>> appearedGroup;
    //child operator's variables position
    private final List<String> childVariableMap;
    //the integer used to store current target tuple's sum output
    private Integer groupSum;

    /**
     * constructor for SumOperator
     * @param child it's child Operator
     * @param head the head query
     */
    public SumOperator(Operator child, Head head) {
        //initialize variables
        this.child = child;
        this.appearedGroup = new ArrayList<>();
        this.childVariableMap = child.getVariableList();
        this.projectVariables = new ArrayList<>();
        this.sumVariables = new ArrayList<>();
        this.groupSum = 0;
        //get head's sumAggregate object
        SumAggregate sumAggregate = head.getSumAggregate();
        //store variables for sum calculation
        for (Term term: sumAggregate.getProductTerms()){
            sumVariables.add(term.toString());
        }
        //store variables for projection
        for (Term term: head.getVariables()){
            projectVariables.add(term.toString());
        }
    }

    /**
     * Get next tuple output
     * @return next tuple output
     */
    @Override
    public Tuple getNextTuple() {
        //initialize variables
        Tuple childNextTuple = child.getNextTuple();
        List<Term> currentGroup = new ArrayList<>();
        boolean findNextOutput=false;
        //this is used to indicate whether there is no projection
        //if no projection, simply calculate total sum and return the sum
        // in other words:"countAll".
        boolean countAll = false;
        if (projectVariables.size()==0){
            countAll = true;
        }

        while (childNextTuple!=null){
            List<Term> outputTerms = new ArrayList<>();
            List<Term> projectTerms = new ArrayList<>();
            List<Term> tupleTerms = childNextTuple.getTerms();
            Map<String, Term> relationTerm = new HashMap<>();
            for (int i = 0; i < childVariableMap.size();i++){
                relationTerm.put(childVariableMap.get(i),tupleTerms.get(i));
            }
            if (projectVariables.size()!=0){
                for (String variable:projectVariables){
                    projectTerms.add(relationTerm.get(variable));
                }
            }
            //calculate the sum value for this tuple
            boolean firstTerm = true;
            Integer sum = 0;
            for (String variable:sumVariables){
                //if this is the first term, we do plus, since we can't do multiply on 0
                if (firstTerm){
                    firstTerm = false;
                    //if this variable is a constant, just add it
                    if (variable.matches("\\d+")){
                        sum+=Integer.parseInt(variable);
                    }
                    //otherwise add the corresponding value
                    else {
                        if (relationTerm.get(variable) instanceof IntegerConstant){
                            sum += ((IntegerConstant) relationTerm.get(variable)).getValue();
                        }
                    }
                }
                //do multiply on sum
                else {
                    if (variable.matches("\\d+")){
                        sum*=Integer.parseInt(variable);
                    }
                    else {
                        if (relationTerm.get(variable) instanceof IntegerConstant){
                            sum *= ((IntegerConstant) relationTerm.get(variable)).getValue();
                        }
                    }
                }
            }

            //if no projection, just add all sum value.
            if (countAll){
                groupSum+=sum;
                childNextTuple = child.getNextTuple();
                if (childNextTuple==null){
                    outputTerms.add(new IntegerConstant(groupSum));
                    return new Tuple("",outputTerms);
                }
                continue;
            }
            //if target output not found
            if (!findNextOutput){
                //if seen before, check next
                if (checkContains(projectTerms,appearedGroup)){
                    childNextTuple = child.getNextTuple();
                }
                //if this tuple is new, set it to this round's target group
                //and add sum to groupSum, and get next tuple
                else {
                    findNextOutput = true;
                    appearedGroup.add(projectTerms);
                    currentGroup = projectTerms;
                    groupSum += sum;
                    childNextTuple = child.getNextTuple();
                }
                //if no child tuple left
                if (childNextTuple == null){
                    //if no unique tuple left, and no target found, it's time to left
                    if (!findNextOutput) {
                        return null;
                    }
                    //if target found, then the above tuple is the only one in this group,
                    //just return output
                    else {
                        outputTerms.addAll(currentGroup);
                        outputTerms.add(new IntegerConstant(groupSum));
                        child.reset();
                        groupSum = 0;
                        return new Tuple("",outputTerms);
                    }
                }
                else {
                    continue;
                }
            }
            else {
                //check if this tuple is our target and do corresponding operation
                if (checkEquals(currentGroup,projectTerms)){
                    groupSum += sum;
                    childNextTuple = child.getNextTuple();
                }
                else {
                    childNextTuple = child.getNextTuple();
                }
                if (childNextTuple == null){
                    outputTerms.addAll(currentGroup);
                    outputTerms.add(new IntegerConstant(groupSum));
                    child.reset();
                    groupSum = 0;
                    return new Tuple("",outputTerms);
                }
            }
        }
        return null;
    }

    /**
     * check if the input lists are the same
     * @param list1 list 1
     * @param list2 list 2
     * @return whether they are the same
     */
    public boolean checkEquals(List<Term> list1,List<Term>list2){
        if (list1.size()!=list2.size()){return false;}
        for (int i = 0;i<list1.size();i++){
            if (!Objects.equals(list1.get(i).toString(), list2.get(i).toString())){
                return false;
            }
        }
        return true;
    }

    /**
     * check if this list contains this target group
     * @param testObj target group
     * @param appearedGroup list of groups
     * @return
     */
    public boolean checkContains(List<Term> testObj,List<List<Term>> appearedGroup){
        boolean pass;
        for (List<Term> terms: appearedGroup){
            pass = true;
            if (terms.size() != testObj.size()){continue;}
            for (int i =0;i<terms.size();i++){
                if (!testObj.get(i).toString().equals(terms.get(i).toString())){
                    pass = false;
                }
            }
            if (pass){
                return true;
            }
        }
        return false;
    }

    /**
     * reset this operator to initial state
     */
    @Override
    public void reset() {
        child.reset();
    }
}
