package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.Utils;
import ed.inf.adbs.minibase.base.Term;
import java.util.List;

/**
 * Class for storing data for a tuple object
 */
public class Tuple {
    //tuple's related relation
    private String relationName;
    //tuple's terms
    private List<Term> terms;

    /**
     * Contractor for tuple
     * @param relationName
     * @param terms
     */
    public Tuple(String relationName, List<Term> terms) {
        this.relationName = relationName;
        this.terms = terms;
    }

    /**
     * relationName's getter
     * @return
     */
    public String getRelationName() {
        return relationName;
    }
    /**
     * terms' getter
     * @return
     */
    public List<Term> getTerms() {
        return terms;
    }

    /**
     * return the tuple's terms as string in a row seperated by ","
     * @return
     */
    public String toString(){
        return Utils.join(terms, ", ");
    }
}
