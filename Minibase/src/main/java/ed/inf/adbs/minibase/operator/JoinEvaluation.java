package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;

import java.util.*;

/**
 * This class is used for evaluate valid tuple output for a join. In specific, it
 * checks if the 2 input tuple satisfy join restrictions.
 * The way to achieve this is described in joinOperator's class comment section.
 */
public class JoinEvaluation {
    private final List<Term> leftTerms;
    private final List<Term> rightTerms;

    private final List<ComparisonAtom> comparisonAtomList;
    //map variable to their value
    private final Map<String,Term> leftVariableMap;
    private final Map<String,Term> rightVariableMap;
    //map original variable to their value
    private Map<String,Term> rightVariableMapOriginal;
    private Map<String,Term> leftVariableMapOriginal;
    private final List<Term> outputTermsList;

    /**
     * Constructor for joinEvaluation
     * @param leftVariable left relation's reformatted variables
     * @param rightVariable right relation's reformatted variables
     * @param leftOriginalVariables left relation's original variables
     * @param rightOriginalVariable right relation's original variables
     * @param leftTuple left input tuple
     * @param rightTuple right input tuple
     * @param comparisonAtomList join restrictions
     */
    public JoinEvaluation(List<String> leftVariable,List<String> rightVariable,List<String> leftOriginalVariables,
                          List<String> rightOriginalVariable,Tuple leftTuple,Tuple rightTuple,
                          List<ComparisonAtom> comparisonAtomList) {
        //initialize essential data
        this.comparisonAtomList = comparisonAtomList;
        this.leftVariableMap = new HashMap<>();
        this.rightVariableMap = new HashMap<>();
        this.rightVariableMapOriginal = new HashMap<>();
        this.leftVariableMapOriginal = new HashMap<>();
        this.leftTerms = leftTuple.getTerms();
        this.rightTerms = rightTuple.getTerms();
        this.outputTermsList = new ArrayList<>();
        for (int i = 0; i < leftVariable.size(); i++) {
            leftVariableMap.put(leftVariable.get(i),leftTerms.get(i));
            leftVariableMapOriginal.put(leftOriginalVariables.get(i),leftTerms.get(i));
        }
        for (int i = 0; i < rightVariable.size(); i++) {
            rightVariableMap.put(rightVariable.get(i),rightTerms.get(i));
            rightVariableMapOriginal.put(rightOriginalVariable.get(i),rightTerms.get(i));
        }
    }

    /**
     * Used to evaluate if this join is valid
     * @return valid or not
     */
    public boolean evaluate(){
        //if no restriction, it is cross product, always valid
        if (comparisonAtomList.isEmpty()){
            outputTermsList.addAll(leftTerms);
            outputTermsList.addAll(rightTerms);
            return true;
        }
        //check join conditions 1 by 1
        for (ComparisonAtom currentCompareAtom : comparisonAtomList) {
            Term term1 = currentCompareAtom.getTerm1();
            Term term2 = currentCompareAtom.getTerm2();
            Term leftTerm;
            Term rightTerm;
            ComparisonOperator operator = currentCompareAtom.getOp();
            //if case of variable compare variable
            if (term1 instanceof  Variable && term2 instanceof Variable) {
                String variable1 = term1.toString();
                String variable2 = term2.toString();
                if (leftVariableMap.containsKey(variable1)) {
                    leftTerm = leftVariableMap.get(variable1);
                    rightTerm = rightVariableMap.get(variable2);
                    if (rightTerm == null){
                        //check if it is because of variable reformat
                        rightTerm = rightVariableMapOriginal.get(variable2);
                    }
                } else {
                    leftTerm = leftVariableMap.get(variable2);
                    rightTerm = rightVariableMap.get(variable1);
                }
                if (rightTerm== null){
                    //try switch order
                    if (leftVariableMap.containsKey(variable2)) {
                        leftTerm = leftVariableMap.get(variable2);
                        rightTerm = rightVariableMap.get(variable1);
                        //might happen when left tuple contains both compare variables
                        if (rightTerm == null){
                            leftTerm = leftVariableMap.get(variable1);
                            rightTerm = leftVariableMap.get(variable2);
                        }
                    } else {
                        leftTerm = leftVariableMap.get(variable1);
                        rightTerm = rightVariableMap.get(variable2);
                    }
                }
                //if compare fail, return false
                if (!compareByOperator(operator.toString(), leftTerm, rightTerm)) {
                    return false;
                }
                else {
                    //double check if this compare can be a restriction for one of the tuple inputs
                    if (leftVariableMap.containsKey(variable1)&&leftVariableMap.containsKey(variable2)){
                        leftTerm = leftVariableMap.get(variable1);
                        rightTerm = leftVariableMap.get(variable2);
                        if (!compareByOperator(operator.toString(), leftTerm, rightTerm)){
                            return false;
                        }
                    }
                    if (rightVariableMapOriginal.containsKey(variable1)&&rightVariableMapOriginal.containsKey(variable2)){
                        leftTerm = rightVariableMapOriginal.get(variable1);
                        rightTerm = rightVariableMapOriginal.get(variable2);
                        if (!compareByOperator(operator.toString(), leftTerm, rightTerm)){
                            return false;
                        }
                    }
                }
            }
            //case of at least 1 constant
            else {
                Term constant = null;
                //assign constant and variable's value base on their order
                if (term1 instanceof Constant){
                    constant = term1;
                    String variable = term2.toString();
                    if (leftVariableMap.containsKey(variable)){
                        leftTerm = leftVariableMap.get(variable);
                        rightTerm = rightVariableMapOriginal.get(variable);
                    }
                    else {
                        leftTerm = rightVariableMapOriginal.get(variable);
                        rightTerm = rightVariableMap.get(variable);
                    }
                }
                else {
                    String variable = term1.toString();
                    constant = term2;
                    if (leftVariableMap.containsKey(variable)){
                        leftTerm = leftVariableMap.get(variable);
                        rightTerm = rightVariableMapOriginal.get(variable);
                    }
                    else {
                        leftTerm = rightVariableMapOriginal.get(variable);
                        rightTerm = rightVariableMap.get(variable);
                    }
                }
                //if left tuple's comparison fail return false
                if (!compareByOperator(operator.toString(), leftTerm,constant)) {
                    return false;
                }
                //check right tuple
                else {
                    if (!compareByOperator(operator.toString(), rightTerm,constant)){
                        return false;
                    }
                }
            }
        }
        //record output
        outputTermsList.addAll(leftTerms);
        outputTermsList.addAll(rightTerms);
        return true;
    }

    /**
     * get list of outputTerms
     * @return list of terms
     */
    public List<Term> outputTerms(){
        return outputTermsList;
    }

    /**
     * compare input terms by the operator
     * @param cOperator compare operator
     * @param left left term
     * @param right right term
     * @return compare result
     */
    public boolean compareByOperator(String cOperator, Term left, Term right){
        boolean integerCompare;
        Integer leftI = null;
        Integer rightI = null;
        String leftS = null;
        String rightS= null;
        //do compare by integer compare or string compare
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
