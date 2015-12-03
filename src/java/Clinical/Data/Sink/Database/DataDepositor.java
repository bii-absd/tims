/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * DataDepositor read in the processed data from the pipeline output and 
 * stored them into the database.
 * 
 * Author: Tay Wei Hong
 * Date: 19-Nov-2015
 * 
 * Revision History
 * 19-Nov-2015 - Created with all the standard getters and setters.
 * 24-Nov-2015 - DataDepositor will be run as a thread. 
 */

public class DataDepositor extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataDepositor.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    // Temporarily fix the job_id, department ID, filepath and gene annotation version
    private String annot_ver = "HG19-2H2015";
    private String fileUri = "C:\\\\temp\\\\output-trial-23Oct2015_0329PM.txt";
    private String dept_id = "ABSD";
    private int job_id = 2;
    
    public DataDepositor(String fileUri) {
//        this.fileUri = fileUri;
        logger.debug("DataDepositor to upload data file: " + fileUri);
    }
    
    @Override
    public void run() {
        logger.debug("DataDepositor start running.");
        insertFinalizedDataIntoDB();
    }
    
    // Update data_depository's data field (at array_index) with the value 
    // passed in.
    private Boolean updateDataArray(String genename, int array_index, 
            String value) {
        Boolean result = Constants.OK;
        
        String updateStr = "UPDATE data_depository SET data[?] = ? "
                + "WHERE genename = ? AND annot_ver = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setInt(1, array_index);
            updateStm.setString(2, value);
            updateStm.setString(3, genename);
            updateStm.setString(4, annot_ver);
            
            updateStm.executeUpdate();
        }
        catch (SQLException e) {
            logger.error("SQLException when updating data depository record " 
                    + genename);
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Insert a new finalized_output record into the database, and return 
    // auto generated key i.e. array_index.
    private int insertFinalizedOutput(FinalizedOutput record) {
        int ind = Constants.DATABASE_INVALID_ID;
        
        String insertStr = "INSERT INTO finalized_output(job_id,subject_id) "
                           + "VALUES(?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr,
                Statement.RETURN_GENERATED_KEYS)) {
            insertStm.setInt(1, record.getJob_id());
            insertStm.setString(2, record.getSubject_id());
            insertStm.executeUpdate();
            ResultSet rs = insertStm.getGeneratedKeys();
            
            if (rs.next()) {
                ind = rs.getInt(1);
            }

            logger.debug("Finalized output for " + record.getSubject_id() + 
                         " inserted with array_index: " + ind);
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting new finalized output!");
            logger.error(e.getMessage());
        }
        
        return ind;
    }
    
    // Return the next array index to be use for this record.
    // NOT IN USE.
    private int getNextArrayInd() {
        int count = Constants.DATABASE_INVALID_ID;
        String queryStr = "SELECT MAX(array_index) FROM finalized_output";

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            ResultSet result = queryStm.executeQuery();
            
            while (result.next()) {
                count = result.getInt(1);                
            }
        }
        catch (SQLException e) {
            logger.debug("SQLException when getting MAX(array_index)!");
            logger.debug(e.getMessage());
        }
        
        return count;
    }
    
    // Check whether the genename exists in the database or not.
    private Boolean geneExistInDB(String genename) {
        Boolean geneExist = Constants.OK;
        String queryStr = "SELECT * FROM data_depository "
                        + "WHERE genename = ? AND annot_ver = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, genename);
            queryStm.setString(2, annot_ver);
            ResultSet result = queryStm.executeQuery();
            
            // Check whether does this gene exist in the database
            if (!result.isBeforeFirst()) {
                geneExist = Constants.NOT_OK;
            }
        }
        catch (SQLException e) {
            logger.error("SQLException when searching for genename!");
            logger.error(e.getMessage());
        }
        
        return geneExist;
    }
    
    // Check whether the subject Meta info exists in the database or not.
    private Boolean subjectExistInDB(String subject_id) {
        Boolean subjectExist = Constants.OK;
        String queryStr = "SELECT * FROM subject WHERE subject_id = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, subject_id);
            ResultSet result = queryStm.executeQuery();
            
            // Check whether does this subject_id exist in the database
            if (!result.isBeforeFirst()) {
                subjectExist = Constants.NOT_OK;
                logger.debug(subject_id + " Meta Info not in database.");
            }
        }
        catch (SQLException e) {
            logger.error("SQLException when searching for subject!");
            logger.error(e.getMessage());
        }
        
        return subjectExist;
    }
    
    // Insert the finalized data into database.
    private Boolean insertFinalizedDataIntoDB() {
        Boolean result = Constants.OK;
        int[] arrayIndex;
        String[] values;
        // For record purpose
        int totalRecord, processedRecord;
        // To record the time taken to insert the processed data
        long startTime, elapsedTime;
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileUri));
            String lineRead;
            String studentNotFound = null;
            // Subject line processing
            lineRead = br.readLine();
            values = lineRead.split("\t");
            // Declare a integer array with size equal to the no of columns
            arrayIndex = new int[values.length];
            // No of subjects = total column - 2
            totalRecord = values.length - 2;
            processedRecord = 0;
            // Ignore the first two strings (i.e. geneID and EntrezID); 
            // start at index 2. 
            for (int i = 2; i < values.length; i++) {
                // Only store the pipeline output if the subject metadata is 
                // available in the database.
                if (subjectExistInDB(values[i])) {
                    processedRecord++;
                    FinalizedOutput record = new FinalizedOutput(values[i], job_id);
                    // Insert the finalized output record, and store the index returned.
                    arrayIndex[i] = insertFinalizedOutput(record);
                }
                else {
                    if (studentNotFound == null) {
                        studentNotFound = values[i] + " ";
                    }
                    else {
                        studentNotFound = studentNotFound + values[i] + " ";
                    }
                    arrayIndex[i] = Constants.DATABASE_INVALID_ID;
                }
            }
            logger.info("Subject records processed: " + processedRecord + 
                    " out of " + totalRecord);
            // Record those pid(s) not found; finalized data will not be stored.
            logger.debug("The following pid(s) is not in our database " + 
                    studentNotFound);
            // gene data processing
            String genename;
            totalRecord = processedRecord = 0;
            // To record the total time taken to insert the finalized data.
            startTime = System.nanoTime();

            while ((lineRead = br.readLine()) != null) {
                totalRecord++;
                values = lineRead.split("\t");
                // The first string is the gene symbol
                genename = values[0];
                // Check whether genename exist in data_depository.
                if (geneExistInDB(genename)) {
                    processedRecord++;
                    // Start reading in the data from 3rd string; start from index 2.
                    for (int i = 2; i < values.length; i++) {
                        // Only process those data with valid PID
                        if (arrayIndex[i] != Constants.DATABASE_INVALID_ID) {
                            // Update data table.
                            updateDataArray(genename,arrayIndex[i],values[i]);
                        }
                    }
                }
            }
            
            // Record total time taken for the insertion.
            elapsedTime = System.nanoTime() - startTime;
            
            logger.debug("Total gene record processed: " + 
                    processedRecord + "/" + totalRecord);
            logger.debug("Total time taken: " + (elapsedTime / 1000000000.0) +
                    " sec");
        }
        catch (IOException ioe) {
            logger.error("IOException when storing finalized data into database.");
            logger.error(ioe.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
}
