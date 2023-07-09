package ed.inf.adbs.minibase;

import java.io.*;
import java.util.*;

/**
 * Class for database catalog, which keeps track essential database information
 */
public class Catalog {
    private static Catalog instance;
    //map used to store each table's schema
    private static Map<String, ArrayList<String>> schema_map = new HashMap<>();
    //path for database
    private static String db_path;

    /**
     * Constructor for catalog
     */
    private Catalog(){};

    /**
     * This function is used for singleton pattern for catalog
     * @return a Catalog object
     */
    public static synchronized Catalog getInstance(){
        if (instance==null){
            instance = new Catalog();
        }
        return instance;
    }

    /**
     * function to initialize the database catalog
     * @param databaseDir the database's directory path
     */
    public void initialize(String databaseDir){
        //database's path
        db_path = databaseDir;
        //schema's file path
        String schema = db_path + File.separator + "schema.txt";
        //readh from schema file, store each table's schema
        try {
            BufferedReader reader = new BufferedReader(new FileReader(schema));
            String line;
            while ((line = reader.readLine()) !=null){
                List<String> elements = Arrays.asList(line.split(" "));
                ArrayList<String> element_list = new ArrayList<>(elements);
                ArrayList<String> types = new ArrayList<>(element_list.subList(1,element_list.size()));
                schema_map.put(element_list.get(0),types);
            }
            reader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * used to get object table's schema
     * @param name the table's name
     * @return the schema of this table
     */
    public static ArrayList<String> getSchema(String name) {
        return schema_map.get(name);
    }

    /**
     * used to get this table's file path
     * @param name table name
     * @return table's file location
     */
    public static String getFilePath(String name){
        return db_path+File.separator+"files"+File.separator+name+".csv";
    }
}
