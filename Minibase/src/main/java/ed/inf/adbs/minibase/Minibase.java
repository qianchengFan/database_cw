package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.operator.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * In-memory database system
 *
 */
public class Minibase {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println("Usage: Minibase database_dir input_file output_file");
            return;
        }

        String databaseDir = args[0];
        String inputFile = args[1];
        String outputFile = args[2];

        evaluateCQ(databaseDir, inputFile, outputFile);

    }

    /**
     * read input file's query and generate output at outputFile
     * @param databaseDir database's directory
     * @param inputFile input file
     * @param outputFile output file
     */
    public static void evaluateCQ(String databaseDir, String inputFile, String outputFile) {
        try {
            //initialize catalog
            Catalog catal = Catalog.getInstance();
            catal.initialize(databaseDir);
            Query query = QueryParser.parse(Paths.get(inputFile));
            //generate query plan and return root operator by QueryBuilder
            QueryBuilder qb = new QueryBuilder(query);
            Operator root = qb.runQueryBuilder();
            //write output result to file
            writeToFile(outputFile,root);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * This method is used to write the output data to output file
     * @param filePath the file path of output file
     * @param root the root operator
     */
    public static void writeToFile(String filePath, Operator root){
        try {
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(filePath));
            Tuple nextTuple = root.getNextTuple();
            //call nextTuple on the root to generate output and write them to file
            while (nextTuple != null) {
                fileWriter.write(nextTuple.toString()+"\n");
                nextTuple = root.getNextTuple();
            }
            fileWriter.close();
        }
        catch (IOException e) {
            System.out.print(e.getMessage());
        }
    }
}
