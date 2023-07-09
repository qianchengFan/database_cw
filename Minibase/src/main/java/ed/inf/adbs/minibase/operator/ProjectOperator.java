package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements project operator that will produce unique tuple
 * that only contains head variables
 */
public class ProjectOperator extends Operator{
    //its child operator
    private final Operator child;
    //head name, used for output tuple's name
    private final String headName;
    //buffer for appeared tuples, used for keep output unique
    private final List<String> usedTuple;

    /**
     * constructor for project operator, and initialize essential data
     * @param child child operator
     * @param head head of query
     */
    public ProjectOperator(Operator child, Head head){
        this.child = child;
        List<Variable> headVariable = head.getVariables();
        this.headName = head.getName();
        this.variableList = new ArrayList<>();
        this.usedTuple = new ArrayList<>();
        //set variable position table to head variables' order
        for (Variable variable : headVariable){
            this.variableList.add(variable.getName());
        }
    }

    /**
     * used to output the next output tuple
     * @return the next valid unique output tuple
     */
    @Override
    public Tuple getNextTuple() {
        //set initial data
        Tuple nextTuple = child.getNextTuple();
        List<String> variablePosition = child.getVariableList();

        while (nextTuple!=null){
            List<Term> outputTerms = new ArrayList<>();
            List<Term> tupleTerms = nextTuple.getTerms();
            Map<String, Term> relationTerm = new HashMap<>();
            //store child tuple's value that map to each value's
            //corresponding variable name
            for (int i = 0; i < variablePosition.size();i++){
                relationTerm.put(variablePosition.get(i),tupleTerms.get(i));
            }
            //set output variable's value map
            for (String variable: this.variableList){
                if (relationTerm.containsKey(variable)){
                    outputTerms.add(relationTerm.get(variable));
                }
            }

            //initialize output tuple
            Tuple outputTuple = new Tuple(headName, outputTerms);
            //if this tuple never appeared before, return
            if (!usedTuple.contains(outputTuple.toString())){
                usedTuple.add(outputTuple.toString());
                return outputTuple;
            }
            else {
                //otherwise check next tuple
                nextTuple = child.getNextTuple();
            }
        }
        return null;
    }

    /**
     * reset this operator to initial state
     */
    @Override
    public void reset() {
        child.reset();
    }
}
