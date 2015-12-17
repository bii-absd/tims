/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.Bean.AuthenticationBean;
import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * 19-Nov-2015 - Created with the necessary methods implemented.
 * 24-Nov-2015 - DataDepositor will be run as a thread.
 * 03-Dec-2015 - Retrieve all the necessary info from the database.
 * 17-Dec-2015 - Group all the SQL statements as one transaction (i.e. 
 * autoCommit is set to off). Improve on the logic to insert gene value into
 * data array. Big improvement in the timing for inserting 22 records of 
 * 18,210/34,694 gene values; from 178 sec to 56 sec.
 */

public class DataDepositor extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataDepositor.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private final String dept_id, fileUri, annot_ver;
    private final int job_id;
    
    public DataDepositor(int job_id) {
        this.job_id = job_id;
        // Retrieve the value of dept_id, fileUri and annot_ver from database.
        annot_ver = getAnnotVersion(job_id);
        fileUri = getOutputPath(job_id);
        dept_id = getDeptID(AuthenticationBean.getUserName());
        logger.debug("DataDepositor created for job_id: " + job_id);
    }
    
    @Override
    public void run() {        
        try {
            // All the SQL statements executed in method 
            // insertFinalizedDataIntoDB() will be treated as one transaction.
            conn.setAutoCommit(false);
            logger.debug("DataDepositor start - Set auto-commit to OFF.");
            
            if (insertFinalizedDataIntoDB()) {
                // All the SQL statements executed successfully, commit the changes.
                logger.debug("DataDepositor - Commit transaction.");
                conn.commit();
            }
            else {
                // Error occurred during data insertion, rollback the transaction.
                logger.error("DataDepositor - Rollback transaction.");
                conn.rollback();
            }
            logger.debug("DataDepositor completed - Set auto-commit to ON.");            
        }
        catch (SQLException e) {
            logger.error("Falied to insert finalized data into database!");
            logger.error(e.getMessage());
        }
    }
    
    // Retrieve the gene annotation version used in the study where this
    // job_id belongs to.
    private String getAnnotVersion(int jobID) {
        String annotVer = Constants.DATABASE_INVALID_STR;
        String query = "SELECT annot_ver FROM study NATURAL JOIN "
                     + "submitted_job WHERE job_id = " + jobID;
        ResultSet rs = DBHelper.runQuery(query);
        
        try {
            if (rs.next()) {
                annotVer = rs.getString("annot_ver");
                logger.debug("Annotation version used in job_id " + jobID + 
                             " is " + annotVer);
                rs.close();
            }
        } 
        catch (SQLException e) {
            logger.error("SQLException when retrieving annotation version!");
            logger.error(e.getMessage());
        }
        
        return annotVer;
    }
    
    // Retrieve the output filepath for this submiited job.
    private String getOutputPath(int jobID) {
        String path = Constants.DATABASE_INVALID_STR;
        String query = "SELECT output_file FROM submitted_job WHERE job_id = " 
                     + jobID;
        ResultSet rs = DBHelper.runQuery(query);
        
        try {
            if (rs.next()) {
                path = rs.getString("output_file");
                logger.debug("Output file for job_id " + jobID + " stored at " +
                             path);
                rs.close();
            }
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving output filepath!");
            logger.error(e.getMessage());
        }
        
        return path;
    }
    
    // Retrieve the department ID where this user belong to.
    private String getDeptID(String userID) {
        String dept = Constants.DATABASE_INVALID_STR;
        String queryStr = "SELECT dept_id FROM user_account WHERE user_id = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, userID);
            ResultSet rs = queryStm.executeQuery();
            
            if (rs.next()) {
                dept = rs.getString("dept_id");
                logger.debug("User ID " + userID + " belongs to department " +
                             dept);
            }
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving department ID!");
            logger.error(e.getMessage());
        }
        
        return dept;
    }
    
    // Insert the gene value into the data array using the PreparedStatement
    // passed in.
    private void insertToDataArray(PreparedStatement stm, int array_index, 
            String value, String genename) throws SQLException {
        stm.setInt(1, array_index);
        stm.setString(2, value);
        stm.setString(3, genename);
        stm.executeUpdate();
    }
    
    // Insert a record into finalized_output table using the PreparedStatement
    // passed in.
    private void insertToFinalizedOutput(PreparedStatement stm, 
            FinalizedOutput record) throws SQLException {
        stm.setInt(1, record.getArray_index());
        stm.setString(2, record.getAnnot_ver());
        stm.setInt(3, record.getJob_id());
        stm.setString(4, record.getSubject_id());
        stm.executeUpdate();
            
        logger.debug("Output for " + record.getSubject_id() + 
                     " stored at index: " + record.getArray_index());        
    }
    
    // Return the next array index to be use for this record in 
    // finalized_output table.
    private int getNextArrayInd() {
        int count = Constants.DATABASE_INVALID_ID;
        String queryStr = "SELECT MAX(array_index) FROM finalized_output "
                        + "WHERE annot_ver = \'" + annot_ver + "\'";
        ResultSet rs = DBHelper.runQuery(queryStr);
        
        try{
            if (rs.next()) {
                count = rs.getInt(1) + 1;
                rs.close();
            }
        }
        catch (SQLException e) {
            logger.debug("SQLException when getting MAX(array_index)!");
            logger.debug(e.getMessage());
        }
        
        return count;
    }
    
    // Check whether genename exists using the PreparedStatement passed in.
    private Boolean checkGeneExistInDB(PreparedStatement stm, 
            String genename) throws SQLException {
        stm.setString(1, genename);
        ResultSet rs = stm.executeQuery();
        
        return rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
    }
    
    // Check whether the subject Meta info exists in the database.
    private Boolean subjectExistInDB(String subject_id) throws SQLException {
        String queryStr = "SELECT * FROM subject WHERE subject_id = ? AND "
                        + "dept_id = \'" + dept_id + "\'";
        
        PreparedStatement queryStm = conn.prepareStatement(queryStr);
        queryStm.setString(1, subject_id);
        ResultSet rs = queryStm.executeQuery();

        return rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
    }
    
    // Insert the finalized pipeline output into database.
    private Boolean insertFinalizedDataIntoDB() throws SQLException {
        Boolean result = Constants.OK;
        int[] arrayIndex;
        String[] values;
        // For record purpose
        int totalRecord, processedRecord;
        // To record the time taken to insert the processed data
        long startTime, elapsedTime;
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileUri));
            // Store those subject ID whom meta data is not in the database.
            String studentNotFound = null;
            
            // **Subject line processing start here.
            String lineRead = br.readLine();
            values = lineRead.split("\t");
            // Declare a integer array with size equal to the no of columns
            arrayIndex = new int[values.length];
            // No of subjects = total column - 2
            totalRecord = values.length - 2;
            processedRecord = 0;
            // INSERT statement to insert a record into finalized_output table.
            String insertStr = "INSERT INTO finalized_output(array_index,"
                             + "annot_ver,job_id,subject_id) VALUES(?,?,?,?)";
            
            try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
                // Ignore the first two strings (i.e. geneID and EntrezID); 
                // start at index 2. 
                for (int i = 2; i < values.length; i++) {
                    // Only store the pipeline output if the subject metadata is 
                    // available in the database.
                    try {
                        if (subjectExistInDB(values[i])) {
                            processedRecord++;
                            arrayIndex[i] = getNextArrayInd();
                            FinalizedOutput record = new FinalizedOutput
                                (arrayIndex[i], annot_ver, values[i], job_id);
                            // Insert the finalized output record.
                            insertToFinalizedOutput(insertStm, record);                            
                        }
                        else {
                            studentNotFound = (studentNotFound == null)?
                                    (values[i] + " "):
                                    (studentNotFound + values[i] + " ");
                            arrayIndex[i] = Constants.DATABASE_INVALID_ID;
                        }
                    } catch (SQLException e) {
                        // Error occurred, return to caller.
                        logger.error(e.getMessage());
                        return Constants.NOT_OK;
                    }
                }
                logger.info("Subject records processed: " + processedRecord + 
                            " out of " + totalRecord);
                // Record those pid(s) not found; finalized data will not be stored.
                logger.debug("The following pid(s) is not in our database " + 
                            studentNotFound);
            }
            catch (SQLException e) {
                logger.error("SQLException when inserting finalized records!");
                logger.error(e.getMessage());
                // Error occurred, return to caller.
                return Constants.NOT_OK;
            }
            
            // **Gene data processing start here.
            String genename;
            totalRecord = processedRecord = 0;
            // To record the total time taken to insert the finalized data.
            startTime = System.nanoTime();
            // UPDATE statement to update the data array in data_depository table.
            String updateStr = 
                    "UPDATE data_depository SET data[?] = ? WHERE " +
                    "genename = ? AND annot_ver = \'" + annot_ver + "\'";
            // SELECT statement to check the existence of gene in database.
            String queryGene = "SELECT 1 FROM data_depository "
                        + "WHERE genename = ? AND annot_ver = \'" 
                        + annot_ver + "\'";

            try (PreparedStatement updateStm = conn.prepareStatement(updateStr);
                 PreparedStatement queryGeneStm = conn.prepareStatement(queryGene)) 
            {
                while ((lineRead = br.readLine()) != null) {
                    totalRecord++;
                    values = lineRead.split("\t");
                    // The first string is the gene symbol.
                    genename = values[0];
                    try {
                        // Check whether genename exist in data_depository.
                        if (checkGeneExistInDB(queryGeneStm,genename)) {
                            processedRecord++;
                            // Start reading in the data from 3rd string; 
                            // start from index 2.
                            for (int i = 2; i < values.length; i++) {
                                // Only process those data with valid PID
                                if (arrayIndex[i] != Constants.DATABASE_INVALID_ID) {
                                    // Insert gene value into data array.
                                    insertToDataArray(updateStm, arrayIndex[i],
                                            values[i],genename);                                    
                                }
                            }
                        }
                    } catch (SQLException e) {
                        // Error occurred, return to caller.
                        logger.error(e.getMessage());
                        return Constants.NOT_OK;
                    }
                }
            } catch (SQLException e) {
                logger.error("SQLException when inserting into data array!");
                logger.error(e.getMessage());
                // Error occurred, return to caller.
                return Constants.NOT_OK;
            }
            // Record total time taken for the insertion.
            elapsedTime = System.nanoTime() - startTime;
            
            logger.debug("Total gene record processed: " + 
                    processedRecord + "/" + totalRecord);
            logger.debug("Total time taken: " + (elapsedTime / 1000000000.0) +
                    " sec");
        }
        catch (IOException ioe) {
            logger.error("IOException when reading pipeline output file!");
            logger.error(ioe.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Update data_depository's data field (at array_index) with the value 
    // passed in.
    // NOT IN USE ANYMORE!
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
    
    // Insert a new finalized_output record into the database.
    // NOT IN USER ANYMORE!
    private void insertFinalizedOutput(FinalizedOutput record) {
        String insertStr = "INSERT INTO finalized_output(array_index,annot_ver,job_id,subject_id) "
                           + "VALUES(?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setInt(1, record.getArray_index());
            insertStm.setString(2, record.getAnnot_ver());
            insertStm.setInt(3, record.getJob_id());
            insertStm.setString(4, record.getSubject_id());
            insertStm.executeUpdate();
            
            logger.debug("Finalized output for " + record.getSubject_id() + 
                         " inserted with array_index: " + record.getArray_index());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting new finalized output!");
            logger.error(e.getMessage());
        }
    }
    
    // Check whether the genename exists in the database or not.
    // NOT IN USE ANYMORE.
    private Boolean geneExistInDB(String genename) {
        Boolean geneExist = Constants.OK;
        String queryStr = "SELECT * FROM data_depository "
                        + "WHERE genename = ? AND annot_ver = \'" 
                        + annot_ver + "\'";
        
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
}
