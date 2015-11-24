/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ProcessedDataHelper is a helper class that help to read in the gene data 
 * from the pipeline output and stored them into the database.
 * 
 * Author: Tay Wei Hong
 * Date: 19-Nov-2015
 * 
 * Revision History
 * 19-Nov-2015 - Created with all the standard getters and setters.
 * 24-Nov-2015 - ProcessedDataHelper will be run as a thread. 
 */

public class ProcessedDataHelper extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ProcessedDataHelper.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private String fileUri;
    
    public ProcessedDataHelper(String fileUri) {
        this.fileUri = fileUri;
        logger.debug("ProcessedDataHelper to upload data file: " + fileUri);
    }
    
    @Override
    public void run() {
        logger.debug("ProcessedDataHelper start running.");
        insertFinalizedDataIntoDB(fileUri);
    }
    
    // Update the dataTable's data field (at array_index) with the value 
    // passed in.
    private Boolean updateDataArray(String dataTable, String genename, 
            int array_index, String value) {
        Boolean result = Constants.OK;
        
        String updateStr = "UPDATE " + dataTable + " SET data[?] = ? "
                + "WHERE genename = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setInt(1, array_index);
            updateStm.setString(2, value);
//            updateStm.setFloat(2, Float.parseFloat(value));
            updateStm.setString(3, genename);
            
            updateStm.executeUpdate();
        }
        catch (SQLException e) {
            logger.error("SQLException when updating " + dataTable + 
                    " record " + genename);
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Insert a new finalized_data record into the database.
    private Boolean createFinalizedData(PipelineRecord record) {
        Boolean result = Constants.OK;
        
        String insertStr = "INSERT INTO pipeline_record"
                + "(tid,array_index,pid,study_id,pipeline_name,rdate) "
                + "VALUES(?,?,?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, record.getTid());
            insertStm.setInt(2, record.getArray_index());
            insertStm.setString(3, record.getPid());
            insertStm.setString(4, record.getStudy_id());
            insertStm.setString(5, record.getPipeline_name());
            insertStm.setDate(6, record.getRdate());
            insertStm.executeUpdate();
            
            logger.debug("Finalized data for " + record.getStudy_id() + 
                         " inserted at index: " + record.getArray_index());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting new pipeline record!");
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Return the next array index to be use for this record.
    private int getNextArrayInd(String tid) {
        int count = Constants.DATABASE_INVALID_ID;
        String queryStr = "SELECT COUNT(*) FROM pipeline_record WHERE tid = ?";

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, tid);
            ResultSet result = queryStm.executeQuery();
            
            while (result.next()) {
                count = result.getInt(1);                
            }
        }
        catch (SQLException e) {
            logger.debug("SQLException when getting the no of row in table " + tid);
            logger.debug(e.getMessage());
        }
        
        return count;
    }
    
    // Check whether the gene exists in the database or not.
    private Boolean geneExistInDB(String dataTable, String genename) {
        Boolean geneExist = Constants.OK;
        String queryStr = "SELECT * FROM " + dataTable + " WHERE genename = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, genename);
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
    
    // Check whether the patient exists in the database or not.
    private Boolean PIDExistInDB(String pid) {
        Boolean pidExist = Constants.OK;
        String queryStr = "SELECT * FROM patient WHERE pid = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, pid);
            ResultSet result = queryStm.executeQuery();
            
            // Check whether does this pid exist in the database
            if (!result.isBeforeFirst()) {
                pidExist = Constants.NOT_OK;
                logger.debug(pid + " doesn't exist in the database.");
            }
        }
        catch (SQLException e) {
            logger.error("SQLException when searching for pid!");
            logger.error(e.getMessage());
        }
        
        return pidExist;
    }
    
    // Insert the finalized data into database.
    private Boolean insertFinalizedDataIntoDB(String fileUriX) {
        Boolean result = Constants.OK;
        int[] arrayIndex;
        String[] values;
        // To record the time taken to insert the processed data
        long startTime, elapsedTime;
        // Hardcored values for now.
        String tid = "Array";
        String study_id = "Bayer";
        String pipeline_name = "gex-affymetrix";
        Date rdate = Date.valueOf("2015-11-20");
        // From tid, we can get the data table name; hard core for now
        String dataTable = "arraydata";
        // Hardcode the filename for now
        String fileUri = "C:\\temp\\output-trial-23Oct2015_0329PM.txt";
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileUri));
            String line;
            String pidNotFound = null;
            // Subject line processing
            line = br.readLine();
            values = line.split("\t");
            // Define the no of integers to be the same as the no of columns read
            arrayIndex = new int[values.length];
            
            // Ignore the first two strings (i.e. geneID and EntrezID); 
            // start at index 2. 
            for (int i = 2; i < values.length; i++) {
                // Only store the pipeline output if the subject metadata is 
                // available in the database.
                if (PIDExistInDB(values[i])) {
                    // Temporarily hardcore tid to Array
                    arrayIndex[i] = getNextArrayInd(tid);
                    // Insert a record into pipeline_record
                    PipelineRecord record = new PipelineRecord(tid, values[i],
                                                study_id, pipeline_name, 
                                                arrayIndex[i], rdate);
                    // Create the finalized data record.
                    createFinalizedData(record);
                }
                else {
                    if (pidNotFound == null) {
                        pidNotFound = values[i] + " ";
                    }
                    else {
                        pidNotFound = pidNotFound + values[i] + " ";
                    }
                    arrayIndex[i] = Constants.DATABASE_INVALID_ID;
                }
            }
            // Record those pid(s) not found; finalized data will not be stored.
            logger.debug("The following pid(s) is not in our database " + 
                    pidNotFound);
            // gene data processing
            String genename;
            // For record purpose
            int totalRecord, processedRecord;
            totalRecord = processedRecord = 0;
            // To record the total time taken to insert the finalized data.
            startTime = System.nanoTime();

            while ((line = br.readLine()) != null) {
                totalRecord++;
                values = line.split("\t");
                // The first string is the gene symbol
                genename = values[0];
                // Check whether gene exist in generef.
                if (geneExistInDB(dataTable,genename)) {
                    processedRecord++;
                    // Start reading in the data from 3rd string; start from index 2.
                    for (int i = 2; i < values.length; i++) {
                        // Only process those data with valid PID
                        if (arrayIndex[i] != Constants.DATABASE_INVALID_ID) {
                            // Update data table.
                            updateDataArray(dataTable,genename,arrayIndex[i],values[i]);
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
