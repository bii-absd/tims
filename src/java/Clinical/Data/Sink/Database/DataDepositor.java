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
import java.util.ArrayList;
import java.util.List;
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
 * 23-Dec-2015 - Instead of receiving a single job_id, the constructor will
 * receive a list of job entries. This class will then process all those job_id
 * found in the list of job entries.
 * 28-Dec-2015 - Moved method subjectExistInDB to SubjectDB. Improve on the 
 * code in method insertFinalizedDataIntoDB().
 */

public class DataDepositor extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataDepositor.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private final String study_id, dept_id, annot_ver;
    private String fileUri;
    private int job_id;
    private List<FinalizingJobEntry> jobList = new ArrayList<>();
    
    public DataDepositor(String study_id, List<FinalizingJobEntry> jobList) {
        this.study_id = study_id;
        this.jobList = jobList;
        // Retrieve the value of dept_id and annot_ver from database.
        dept_id = UserAccountDB.getDeptID(AuthenticationBean.getUserName());
        annot_ver = StudyDB.getAnnotVer(study_id);
        logger.debug("DataDepositor created for study: " + study_id);
    }
    
    @Override
    public void run() {
        Boolean finalizeStatus = Constants.OK;
        
        try {
            // All the SQL statements executed here will be treated as one 
            // big transaction.
            logger.debug("DataDepositor start - Set auto-commit to OFF.");
            conn.setAutoCommit(false);
            
            for (FinalizingJobEntry job : jobList) {
                // Retrieve the job ID and pipeline output file for this 
                // selected job.
                job_id = job.getJob_id();
                fileUri = SubmittedJobDB.getOutputPath(job_id);
                logger.debug("Data insertion for: " + study_id + " - " + 
                             job.getTid() + " - Job ID: " + job_id);
                
                if (!insertFinalizedDataIntoDB()) {
                    // Error occurred during data insertion, stop the transaction.
                    finalizeStatus = Constants.NOT_OK;
                    logger.error("DataDepositor - Hit error!");
                    break;
                }
            }
            
            if (finalizeStatus) {
                // All the SQL statements executed successfully, commit the changes.
                logger.debug("DataDepositor - Commit transaction.");
                conn.commit();
            }
            else {
                // Error occurred during data insertion, rollback the transaction.
                logger.error("DataDepositor - Rollback transaction.");
                conn.rollback();
            }
            
            conn.setAutoCommit(true);
            logger.debug("DataDepositor completed - Set auto-commit to ON.");
            // Update job status to finalized
            for (FinalizingJobEntry job : jobList) {
                SubmittedJobDB.updateJobStatusToFinalized(job.getJob_id());
            }
        }
        catch (SQLException e) {
            logger.error("Falied to insert finalized data into database!");
            logger.error(e.getMessage());
        }
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
            }
            rs.close();
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
            StringBuilder subjectNotFound = new StringBuilder();
            
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
                        if (SubjectDB.isSubjectExistInDept(values[i], dept_id)) {
                            processedRecord++;
                            arrayIndex[i] = getNextArrayInd();
                            FinalizedOutput record = new FinalizedOutput
                                (arrayIndex[i], annot_ver, values[i], job_id);
                            // Insert the finalized output record.
                            insertToFinalizedOutput(insertStm, record);                            
                        }
                        else {
                            subjectNotFound.append(values[i]).append(" ");
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
                // Record those subject ID not found; finalized data will not be stored.
                if (subjectNotFound.toString().isEmpty()) {
                    logger.debug("All the subject ID is found in database.");
                }
                else {
                    logger.debug("The following subject ID is not found in database " + 
                                 subjectNotFound);
                }
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
                // Close the stream and releases any system resources associated
                // with it.
                br.close();
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
    
    /* NOT IN USE ANYMORE!
    
    // Check whether the subject Meta info exists in the database.
    // Moved to SubjectDB. 
    // NOT IN USE ANYMORE!
    private Boolean subjectExistInDB(String subject_id) throws SQLException {
        String queryStr = "SELECT * FROM subject WHERE subject_id = ? AND "
                        + "dept_id = \'" + dept_id + "\'";
        
        PreparedStatement queryStm = conn.prepareStatement(queryStr);
        queryStm.setString(1, subject_id);
        ResultSet rs = queryStm.executeQuery();

        return rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
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
            logger.error("SQLException when updating data depository record!");
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Insert a new finalized_output record into the database.
    // NOT IN USER ANYMORE!
    private void insertFinalizedOutput(FinalizedOutput record) {
        String insertStr = "INSERT INTO finalized_output(array_index,annot_ver,"
                         + "job_id,subject_id) VALUES(?,?,?,?)";

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
    */
}
