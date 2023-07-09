package ed.inf.adbs.minibase.operator;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.Catalog;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ScanOperator will scan and return the scan object table's rows one by one as a tuple
 * when the getNextTuple() method is called.
 */
public class ScanOperator extends Operator{
    //name of the object relation
    private String relationName;
    //dataPath of the object relation's data file
    private String dataPath;
    //the buffer reader for file scanning
    private BufferedReader br;
    //the schema file of this relation
    ArrayList<String> schema;

    /**
     * Constructor of scanOperator, which initialise some essential data.
     * @param scanObject the object relationalAtom
     */
    public ScanOperator(RelationalAtom scanObject){
        try {
            relationName = scanObject.getName();
            //get filePath's location and schema's location from database catalog
            dataPath = Catalog.getFilePath(relationName);
            schema = Catalog.getSchema(relationName);
            //get this relations input parameter
            List<Term> terms = scanObject.getTerms();
            //initialize the list to store input parameter's variable name or constant when it's a constant term.
            variableList = new ArrayList<>();
            //loop through input parameters, store variable name or constant for each term.
            for (Term t: terms){
                if (t instanceof Variable){
                    variableList.add(((Variable) t).getName());
                }
                else {
                    variableList.add("constant");
                }
            }
            //initialize buffer
            br = new BufferedReader(new FileReader(dataPath));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * each time this function be called, it will return the next row in this table
     * and return null if there is no other row left.
     * @return the tuple of current row or null if no row left.
     */
    @Override
    public Tuple getNextTuple() {
        try{
            //read current line from file
            String slot = br.readLine();
            //if not eof, keep processing
            if (slot!=null) {
                String[] elements = slot.split(", ");
                Term currentTerm;
                ArrayList<Term> termList = new ArrayList<>();
                for (int i = 0; i < elements.length;i++) {
                    if (Objects.equals(schema.get(i), "int")) {
                        currentTerm = new IntegerConstant(Integer.parseInt(elements[i]));
                    } else {
                        currentTerm = new StringConstant(elements[i].replaceAll("'",""));
                    }
                    termList.add(currentTerm);
                }
                //return output tuple
                Tuple outputTuple = new Tuple(relationName, termList);
                return outputTuple;
            }
            else{
                return null;
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * reset this operator to initial state
     */
    @Override
    public void reset(){
        try{
            br.close();
            br = new BufferedReader(new FileReader(dataPath));
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }
}
