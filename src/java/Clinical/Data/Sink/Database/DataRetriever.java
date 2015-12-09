/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * DataRetriever read in the finalized data from the database and output them
 * to a text file.
 * 
 * Author: Tay Wei Hong
 * Date: 04-Dec-2015
 * 
 * Revision History
 * 04-Dec-2015 - Created with the necessary methods implemented.
 * 07-Dec-2015 - Added in the code for time logging.
 */

public class DataRetriever extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataRetriever.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private String study_id;
    private List<OutputItems> opList = new ArrayList<>();
    private List<String> geneList = new ArrayList<>();
    private String header = "Subject|Technology";
    
    // Using the study_id received, DataRetriever will retrieve the finalized
    // data from the database.
    public DataRetriever(String study_id) {
        this.study_id = study_id;
        logger.debug("DataRetriever created for study_id: " + study_id);
    }
    
    @Override
    public void run() {
        logger.debug("DataRetriever start running.");
        consolidateFinalizedData();
    }
    
    // To build the subject line for the output file.
    private String getSubjectData(OutputItems item) {
        // For time logging purpose.
        long elapsedTime;
        long startTime = System.nanoTime();
        String data = item.getSubject_id() + "|" + item.getTid();
        String queryStr = 
                "SELECT data[?] FROM data_depository " +
                "WHERE annot_ver = (SELECT annot_ver FROM study " +
                "WHERE study_id = ?) ORDER BY genename";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setInt(1, item.getArray_index());
            queryStm.setString(2, study_id);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                data += "|" + rs.getString(1);
            }
            
            elapsedTime = System.nanoTime() - startTime;
            logger.debug(item.getSubject_id() + ": " + 
                    (elapsedTime / 1000000000.0) + " sec");
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving subject data!");
            logger.error(e.getMessage());
        }
        
        return data;
    }
    
    // Write the finalized data to a text file.
    private void writeToFile(String filename) {
        // For time logging purpose.
        long elapsedTime;
        long startTime = System.nanoTime();
        
        try {
            PrintStream ps = new PrintStream(new File(filename));
            // Write the header/subject line first
            ps.println(header);
            // Loop through the output items and write the subject output one
            // at a time.
            for (OutputItems item : opList) {
                ps.println(getSubjectData(item));
            }
            elapsedTime = System.nanoTime() - startTime;
            logger.debug("Total time taken to write output: " + 
                    (elapsedTime / 1000000000.0) + " sec.");
        }
        catch (IOException ioe) {
            logger.error("IOException when writing output to file!");
            logger.error(ioe.getMessage());
        }
    }
    
    // Retrieve the list of genename that is relevant to the annotation 
    // version used in the study.
    private List<String> getGeneList() {
        // For time logging purpose.
        long elapsedTime;
        long startTime = System.nanoTime();
        String queryStr = 
                "SELECT genename FROM data_depository " +
                "WHERE annot_ver = (SELECT annot_ver FROM study " +
                "WHERE study_id = ?) ORDER BY genename";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, study_id);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                geneList.add(rs.getString("genename"));
                header += "|" + rs.getString("genename");
            }
            
            elapsedTime = System.nanoTime() - startTime;
            logger.debug("No of genename retrieved: " + geneList.size());
            logger.debug("Time taken: " + (elapsedTime / 1000000000.0) + " sec.");
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving genename!");
            logger.error(e.getMessage());
        }
        
        return geneList;
    }
    
    // Retrieve the output row information (i.e. subject_id|tid|array_index 
    // where gene's values are stored) from the database.
    private List<OutputItems> getOpList() {
        String queryStr = 
                "SELECT y.subject_id, x.tid, y.array_index FROM " +
                "(SELECT tid, job_id FROM submitted_job sj INNER JOIN pipeline pl " +
                "ON sj.pipeline_name = pl.name " +
                "WHERE study_id = ? AND status_id = 5) x " +
                "NATURAL JOIN " +
                "(SELECT * FROM finalized_output WHERE job_id IN " +
                "(SELECT job_id FROM submitted_job WHERE study_id = ? AND status_id = 5) " +
                "AND annot_ver = " +
                "(SELECT annot_ver FROM study WHERE study_id = ?)) y " +
                "ORDER BY y.subject_id, y.array_index";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, study_id);
            queryStm.setString(2, study_id);
            queryStm.setString(3, study_id);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                OutputItems item = new OutputItems(
                                    rs.getString("subject_id"),
                                    rs.getString("tid"),
                                    rs.getInt("array_index"));
                opList.add(item);                
            }
            
            logger.debug("Total output row retrieved: " + opList.size());
        }
        catch (SQLException e) {
            logger.error("SQLException when building output list!");
            logger.error(e.getMessage());
        }
        
        return opList;
    }
    
    // Retrieve the finalized data from the database, consolidate and output
    // them to a text file.
    private void consolidateFinalizedData() {
        // Retrieve the list of OutputItems (i.e. Subject|Technology|Index)
        getOpList();
        getGeneList();
        writeToFile("C://temp//iCOMIC2S//finalizedOP.txt");

        /*
        // NOTE: When using System.out.println the last character CANNOT be a "|"
        // else weird message will be printed.
        // e.g. [#|2015-12-04T11:42:02.340+0800|INFO|glassfish 4.1||_ThreadID=933;
        System.out.println("Subject|Technology|Array_index");
        for (OutputItems item : opList) {
            System.out.println(item.toString());
        }
        */
    }
    
    // Machine generated getters and setters.
}