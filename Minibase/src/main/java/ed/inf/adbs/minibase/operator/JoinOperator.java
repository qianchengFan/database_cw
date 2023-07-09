package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.List;

import static ed.inf.adbs.minibase.operator.QueryBuilder.appearedVariableName;
import static ed.inf.adbs.minibase.operator.QueryBuilder.unappearedChar;

/**
 * This class is for joinOperator that joins 2 child operator according to join conditions and their variables.
 * For each tuple from left child, it will check all tuple from right child, if a valid match found, return the output,
 * if arrive end of right child, reset right child and check next left tuple. If end of left child arrived, it means all valid
 * output has been returned, so return null.
 * For detailed join evaluation algorithm, I use an example to explain this: If we have an input query Q(x,y,z) :- R(x, y, z), S(y, w, x),x=1,
 * join operator's 2 child operators will be a scan operator of R and a scan operator of S. The operator
 * will first change identical variable to an unappeared char and add corresponding comparisons, to
 * change it to R(x,y,z),S(b,w,a),x=1,x=b,y=a. And then call joinEvaluation with input relations R(x,y,z),S(b,w,a) and input
 * comparisons x=1,x=b,y=a. In joinEvaluation, it will check comparisons 1 by 1. For x=1, since both relations' original form have x, it will
 * check both relation to make sure both side's x's position's value =1. For x=b and y=a, these variables can directly find in current
 * relation's table, just compare their value. In this way, it can achieve restriction filtering and join on same variables.
 */
public class JoinOperator extends Operator{
    private final Operator leftChild;
    private final Operator rightChild;
    private final List<ComparisonAtom> comparisonAtom;
    //left child's current output tuple
    private Tuple leftPointer;
    //right child's current output tuple
    private Tuple rightPointer;
    //left child's current variable table
    private final List<String> leftVariables;
    //right child's current variable table
    private List<String> rightVariables;
    //left child's original variable table
    private final List<String> rightOriginalVariables;
    //right child's original variable table
    private final List<String> leftOriginalVariables;
    private Tuple outputTuple;
    private String outputName;
    //the last altered char, also used for variable renaming
    // in convertList() function
    private char lastChar;

    //used to show this join is valid.
    private final boolean valid;

    public JoinOperator(Operator leftChild, Operator rightChild, List<ComparisonAtom> comparisonAtom) {
        //initialize variables
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.comparisonAtom = comparisonAtom;
        this.leftPointer = leftChild.getNextTuple();
        this.rightPointer = rightChild.getNextTuple();
        this.leftVariables = leftChild.getVariableList();
        this.rightVariables = rightChild.getVariableList();
        this.rightOriginalVariables = new ArrayList<>();
        this.leftOriginalVariables = new ArrayList<>();
        this.outputTuple = null;
        //if this join can happen
        if (leftPointer!=null && rightPointer != null){
            leftOriginalVariables.addAll(leftChild.getVariableList());
            rightOriginalVariables.addAll(rightChild.getVariableList());
            this.valid = true;
            //list of all variables
            //initialize essential data
            List<Term> terms = new ArrayList<>();
            List<Term> leftTerms = leftPointer.getTerms();
            List<Term> rightTerms = rightPointer.getTerms();
            terms.addAll(leftTerms);
            terms.addAll(rightTerms);
            //reformat join child
            //change the variables table and add comparison
            for (int j = 0;j<leftVariables.size();j++){
                String variable1 = leftVariables.get(j);
                //constant must be preserved
                if (variable1.equals("constant")){
                    continue;
                }

                for (int i =0;i<rightVariables.size();i++){
                    String variable2 = rightVariables.get(i);
                    if (variable2.equals("constant")){
                        continue;
                    }
                    //if they are identical, reformat it
                    if (variable1.equals(variable2)){
                        rightVariables = convertList(rightVariables, i);
                        Variable newVariable = new Variable(Character.toString(lastChar));
                        Variable newVariable2 = new Variable(variable1);
                        this.comparisonAtom.add(new ComparisonAtom(newVariable, newVariable2, ComparisonOperator.EQ));
                    }
                }
            }
            //change tuple name
            outputName = leftPointer.getRelationName()+" + "+rightPointer.getRelationName();
            this.variableList = new ArrayList<>();
            this.variableList.addAll(leftVariables);
            this.variableList.addAll(rightVariables);
        }
        else {
            this.valid = false;
        }
    }

    /**
     * get next valid tuple after join these relations
     * @return next valid tuple
     */
    @Override
    public Tuple getNextTuple() {
        //if this is not a possible join, return null
        if (!valid){
            return null;
        }
        while (leftPointer!=null){
            //check by using joinEvaluation
            JoinEvaluation checker = new JoinEvaluation(leftVariables,rightVariables,this.leftOriginalVariables,this.rightOriginalVariables,leftPointer,rightPointer,comparisonAtom);
            boolean find = checker.evaluate();
            //if you find a valid tuple, initialize output Tuple
            if (find){
                outputTuple = new Tuple(outputName,checker.outputTerms());
            }
            //check next tuple
            rightPointer = rightChild.getNextTuple();
            //if reach end of rightChild, resit rightChild and start check next left tuple
            if(rightPointer==null){
                this.rightChild.reset();
                rightPointer = rightChild.getNextTuple();
                leftPointer = leftChild.getNextTuple();
            }
            //return output tuple
            if (find){
                return outputTuple;
            }
        }
        return null;
    }

    /**
     * function used to change a variable in a relation to another name
     * @param variableList variable table
     * @param index the position of the variable that need to change
     * @return a changed list
     */
    public List<String> convertList(List<String> variableList, Integer index) {
        boolean find = false;
        while (!find) {
            //the unappearedChar and appearedVariableName here are static variables from QueryBuilder class
            //use static variable to avoid possible collision.
            String variable = Character.toString(unappearedChar);
            if (!appearedVariableName.contains(variable)) {
                appearedVariableName.add(variable);
                variableList.set(index, variable);
                find = true;
            }
            lastChar = unappearedChar;
            unappearedChar += 1;
        }
        return variableList;
    }

    /**
     * reset to initial state
     */
    @Override
    public void reset() {
        this.rightChild.reset();
        this.leftChild.reset();
        this.leftPointer = leftChild.getNextTuple();
        this.rightPointer = rightChild.getNextTuple();
    }
}
