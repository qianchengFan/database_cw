package ed.inf.adbs.minibase.operator;

import java.util.List;

/**
 *abstract class for Operator, include basic functions and variables available for every operator type
 */
public abstract  class Operator {

    //the list of string used to store a relation's variables
    //for example R(x,y,1) stored as ["x","y","constant"]
    protected List<String> variableList;

    /**
     * function used to get next tuple, here is just a foundation,
     * it will be extended in subclasses.
     * @return the next tuple object.
     */
    public abstract Tuple getNextTuple();

    /**
     * Reset the operator to initial state,this function will be extended in subclasses.
     */
    public abstract void reset();

    /**
     * function used to print every output tuples of an operator
     * for testing purpose
     */
    public void dump(){
        Tuple nextTuple = getNextTuple();

        while (nextTuple != null) {
            System.out.println(nextTuple.getRelationName()+" "+ nextTuple.getTerms());
            nextTuple = getNextTuple();
        }
    }

    /**
     * getter for relation's variable list
     * @return the relation's variable list
     */
    public List<String> getVariableList(){
        return variableList;
    }
}
