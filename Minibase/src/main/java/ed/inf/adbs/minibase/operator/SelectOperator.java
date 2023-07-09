package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.ComparisonAtom;
import java.util.List;

/**
 * SelectOperator acts as a filter to get tuples that satisfies
 * the input restrictions. For each tuple, it will check it by
 * using selectEvaluation class.
 */
public class SelectOperator extends Operator{
    //child operator
    private final Operator child;
    //the list of restrictions
    private final List<ComparisonAtom> comparisonAtomList;

    /**
     * constructor for select operator.
     * @param child child operator
     * @param comparisonAtomList the list of restrictions
     */
    public SelectOperator(Operator child, List<ComparisonAtom> comparisonAtomList){
        this.child = child;
        this.comparisonAtomList = comparisonAtomList;
        this.variableList = child.getVariableList();
    }

    /**
     * get next valid tuple that satisfy given restrictions
     * @return the output tuple
     */
    @Override
    public Tuple getNextTuple() {
        Tuple nextTuple = child.getNextTuple();
        while (nextTuple !=null){
            //use selectEvaluation to evaluate whether this tuple is valid
            SelectEvaluation selectEvaluation = new SelectEvaluation(variableList,nextTuple,comparisonAtomList);
            if (selectEvaluation.evaluate()){
                return nextTuple;
            }
            else {
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
        this.child.reset();
    }
}
