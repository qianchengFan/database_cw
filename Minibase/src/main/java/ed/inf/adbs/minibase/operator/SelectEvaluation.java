package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;
import java.util.*;

/**
 * class used to evaluate if the input tuple satisfies the input restrictions
 */
public class SelectEvaluation {
    //the list of restrictions
    private final List<ComparisonAtom> comparisonAtomList;
    //map from variable name to its term value in this tuple
    private final Map<String, Term> relationTerm;

    /**
     * constructor for selectEvaluation, set up initial variables
     * @param relationVariable the relation's variable table
     * @param tuple the input tuple
     * @param comparisonAtomList restrictions on the input tuple
     */
    public SelectEvaluation(List<String> relationVariable, Tuple tuple, List<ComparisonAtom> comparisonAtomList){
        //initialize essential variables
        this.comparisonAtomList = comparisonAtomList;
        relationTerm = new HashMap<>();
        List<Term> tupleTerms=tuple.getTerms();
        //store the map between each variable to its term value.
        for (int i = 0; i < relationVariable.size(); i++){
            relationTerm.put(relationVariable.get(i),tupleTerms.get(i));
        }
    }

    /**
     * function used to check current tuple against restrictions
     * @return boolean to show if the check pass
     */
    public boolean evaluate(){
        //loop through all restrictions, if anyone fails, return false
        for (ComparisonAtom cAtom: comparisonAtomList){
            if (!evaluateAtom(cAtom)){
                return false;
            }
        }
        return true;
    }

    /**
     * function used to check if current tuple satisfies the input restriction
     * @param cAtom input restriction
     * @return satisfy or not
     */
    public boolean evaluateAtom(ComparisonAtom cAtom){
        //left atom, right atom, and comparison operator
        Term left = cAtom.getTerm1();
        Term right = cAtom.getTerm2();
        String cOperator = cAtom.getOp().toString();

        //split to cast of constant compare constant, variable compare constant,
        // and variable compare variable
        // if any term is a variable, then need to convert the variable
        // to constant value and then call evaluateCompare() to compare these values
        if (left instanceof Constant){
            if (right instanceof Constant){
                return evaluateCompare(cOperator,(Constant) left,(Constant) right);
            }
            else {
                Term tupleRight = relationTerm.get(right.toString());
                return evaluateCompare(cOperator, (Constant) left, (Constant) tupleRight);
            }
        }
        else {
            Term tupleLeft = relationTerm.get(left.toString());
            if (right instanceof Constant){
                return evaluateCompare(cOperator,(Constant) tupleLeft,(Constant) right);
            }
            else {
                Term tupleRight = relationTerm.get(right.toString());
                return evaluateCompare(cOperator, (Constant) tupleLeft, (Constant) tupleRight);
            }
        }
    }

    /**
     * split comparison to string comparison and integer comparison
     * then call compareByOperator() to compare them
     * @param cOperator comparison operator
     * @param left value1
     * @param right value2
     * @return comparison result
     */
    public boolean evaluateCompare(String cOperator, Constant left, Constant right){
        //
        if (left instanceof IntegerConstant){
            if (right instanceof IntegerConstant){
                return compareByOperator(cOperator,left,right);
            }
            else {
                return false;
            }
        }
        else {
            if (right instanceof IntegerConstant){
                return false;
            }
            else {
                return compareByOperator(cOperator,left,right);
            }
        }
    }

    /**
     * compare input terms according to their type and comparison operator
     * @param cOperator comparison operator
     * @param left term1
     * @param right term2
     * @return compare pass or not
     */
    public boolean compareByOperator(String cOperator, Term left, Term right){
        //initialise variables
        boolean integerCompare;
        Integer leftI = null;
        Integer rightI = null;
        String leftS = null;
        String rightS= null;
        //assign values according to their type
        if (left instanceof IntegerConstant){
            integerCompare = true;
            leftI = ((IntegerConstant) left).getValue();
            rightI = ((IntegerConstant) right).getValue();
        }
        else {
            integerCompare = false;
            leftS = ((StringConstant) left).getValue();
            rightS = ((StringConstant) right).getValue();
        }
        //do comparison
        switch (cOperator){
            case "=":
                return (left.toString().equals(right.toString()))
                        ;
            case "!=":
                return !left.toString().equals(right.toString())
                        ;
            case ">":
                if (integerCompare){
                    return (leftI > rightI);
                }
                else {
                    return (leftS.compareTo(rightS) > 0);
                }
            case ">=":
                if (integerCompare){
                    return (leftI >= rightI);
                }
                else {
                    return (leftS.compareTo(rightS) >= 0);
                }
            case "<":
                if (integerCompare){
                    return (leftI < rightI);
                }
                else {
                    return (leftS.compareTo(rightS) < 0);
                }

            case "<=":
                if (integerCompare){
                    return (leftI <= rightI);
                }
                else {
                    return (leftS.compareTo(rightS) <= 0);
                }
            default:return false;
        }
    }
}
