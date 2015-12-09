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
 */

public class DataDepositor extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataDepositor.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    // job_id, dept_id, fileUri and annot_ver will be retrieved from database.
    private String annot_ver;
    private String fileUri;
    private String dept_id;
    private int job_id;
    
    public DataDepositor(int job_id) {
        this.job_id = job_id;
        annot_ver = getAnnotVersion(job_id);
        fileUri = getOutputPath(job_id);
        dept_id = getDeptID(AuthenticationBean.getUserName());
        logger.debug("DataDepositor created for job_id: " + job_id);
    }
    
    @Override
    public void run() {
        logger.debug("DataDepositor start running.");
        insertFinalizedDataIntoDB();
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
        String query = "SELECT output_file FROM submitted_job WHERE job_id = " + jobID;
        ResultSet rs = DBHelper.runQuery(query);
        
        try {
            if (rs.next()) {
                path = rs.getString("output_file");
                logger.debug("Output file for job_id " + jobID + " stored at " +
                             path);
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
    
    // Insert a new finalized_output record into the database.
    private void insertFinalizedOutput(FinalizedOutput record) {
        String insertStr = "INSERT INTO finalized_output(array_index,annot_ver,job_id,subject_id) "
                           + "VALUES(?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setInt(1, record.getArray_index());
            insertStm.setString(2, record.getAnnot_ver());
            insertStm.setInt(3, record.getJob_id());
            insertStm.setString(4, record.getSubject_id());
            insertStm.executeUpdate();
            ResultSet rs = insertStm.getGeneratedKeys();
            
            logger.debug("Finalized output for " + record.getSubject_id() + 
                         " inserted with array_index: " + record.getArray_index());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting new finalized output!");
            logger.error(e.getMessage());
        }
    }
    
    // Return the next array index to be use for this record.
    private int getNextArrayInd() {
        int count = Constants.DATABASE_INVALID_ID;
        String queryStr = "SELECT MAX(array_index) FROM finalized_output "
                + "WHERE annot_ver = ?";

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, annot_ver);
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
        String queryStr = "SELECT * FROM subject WHERE subject_id = ? AND dept_id = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, subject_id);
            queryStm.setString(2, dept_id);
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
                    arrayIndex[i] = getNextArrayInd() + 1;
                    FinalizedOutput record = new FinalizedOutput(arrayIndex[i],
                            annot_ver,values[i], job_id);
                    // Insert the finalized output record.
//                    insertFinalizedOutput(record);
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
//                            updateDataArray(genename,arrayIndex[i],values[i]);
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