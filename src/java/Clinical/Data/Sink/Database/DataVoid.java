/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * DataVoid perform the unfinalization process on selected Study. It will void
 * the records in the finalized output table (i.e. by setting job ID to 0 and 
 * subject ID to VOID), and set the data to VOID in the data depository table.
 * 
 * Author: Tay Wei Hong
 * Date: 15-Feb-2016
 * 
 * Revision History
 * 15-Feb-2016 - Implemented the module to unfinalize study..
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 */

public class DataVoid extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataVoid.class.getName());
    private Connection conn = null;
    private List<Integer> jobIDList = new ArrayList<>();
    private List<Integer> arrayIndList = new ArrayList<>();
    private final String userName, study_id, annot_ver;
    
    public DataVoid(String userName, String study_id) 
            throws SQLException, NamingException
    {
        conn = DBHelper.getDSConn();
        this.userName = userName;
        this.study_id = study_id;
        annot_ver = StudyDB.getStudyAnnotVer(study_id);
        // Retrieve the list of job IDs that have been finalized for this study.
        jobIDList = SubmittedJobDB.getFinalizedJobIDs(study_id);
    }
    
    @Override
    public void run() {
        Boolean unfinResult = Constants.OK;
        // To record the time taken to unfinalize the study.
        long startTime, elapsedTime;

        for (Integer jobID : jobIDList) {
            arrayIndList.addAll(getArrayIndexes(jobID));
        }
        logger.debug("Job IDs: " + jobIDList.toString());
        logger.debug("Indexes: " + arrayIndList.toString());
        
        startTime = System.nanoTime();
        try {
            // All the SQL statements executed here will be treated as one
            // big transaction.
            logger.debug("DataVoid start - Set auto-commit to OFF.");
            conn.setAutoCommit(false);
            
            if (!voidAllData()) {
                logger.error("DataVoid - Hit error when trying to void the data!");
                unfinResult = Constants.NOT_OK;
            }
            else {
                // All the data in data depository have been voided, now 
                // proceed to void all the records in finalized output table.
                if (!voidAllFinalizedRecords()) {
                    logger.error("DataVoid - Hit error when trying to void the finalized record!");
                    unfinResult = Constants.NOT_OK;
                }
            }
            
            if (unfinResult) {
                // Revert study back to unfinalized.
                StudyDB.updateStudyFinalizedStatus(study_id, false);
                // All the SQL statements executed successfully, commit the changes.
                logger.debug("DataVoid - Commit transaction.");
                conn.commit();
            }
            else {
                // Error occurred during unfinalization, rollback the transaction.
                logger.debug("DataVoid - Rollback transaction.");
                conn.rollback();
            }
            
            conn.setAutoCommit(true);
            logger.debug("DataVoid completed - Set auto-commit to ON");
            
        }
        catch (SQLException e) {
            logger.error("FAIL to unfinalize study!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        elapsedTime = System.nanoTime() - startTime;
        logger.debug("Total time taken to unfinalize " + study_id + 
                " is " + (elapsedTime / 1000000000.0) + " sec.");
    }
    
    // Retrieve all the array indexes that belong to this job ID.
    private List<Integer> getArrayIndexes(int job_id) {
        List<Integer> indexList = new ArrayList<>();
        String query = "SELECT array_index FROM finalized_output WHERE "
                     + "annot_ver = \'" + annot_ver + "\' AND job_id = " 
                     + job_id;
        
        try (PreparedStatement stm = conn.prepareStatement(query)) {
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                indexList.add(rs.getInt("array_index"));
            }
            logger.debug("Array indexes retrieved for job ID " + job_id);
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve array indexes!");
            logger.error(e.getMessage());
        }

        return indexList;
    }
    
    // Set all the related data at data depository table to VOID.
    private Boolean voidAllData() {
        Boolean result = Constants.OK;
        // To record the time taken to void the data.
        long startTime, elapsedTime;
        String query = "UPDATE data_depository SET data[?] = \'VOID\' "
                     + "WHERE annot_ver = \'" + annot_ver + "\'";
        
        try (PreparedStatement stm = conn.prepareStatement(query)) {            
            for (Integer index : arrayIndList) {
                startTime = System.nanoTime();
                voidData(stm, index);
                elapsedTime = System.nanoTime() - startTime;
                logger.debug("Void data for index " + index + " took " + 
                            (elapsedTime / 1000000000.0) + " sec.");
            }
            logger.debug("All data voided.");
        }
        catch (SQLException e) {
            logger.error("FAIL to void data!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }

        return result;
    }
    
    // Set the data at data depository table to VOID.
    private void voidData(PreparedStatement stm, int index) 
            throws SQLException {
        stm.setInt(1, index);
        stm.executeUpdate();
    }
    
    // Void all the related records in finalized output table i.e. by setting
    // job_id to 0 and subject_id to VOID.
    private Boolean voidAllFinalizedRecords() {
        Boolean result = Constants.OK;
        String query = "UPDATE finalized_output SET job_id = 0, "
                     + "subject_id = \'VOID\' WHERE annot_ver = \'" 
                     + annot_ver + "\' AND job_id = ?";
        
        try (PreparedStatement stm = conn.prepareStatement(query)) {
            for (Integer jobID : jobIDList) {
                stm.setInt(1, jobID);
                stm.executeUpdate();
                // Revert job status to completed.
                SubmittedJobDB.updateJobStatusToCompleted(jobID);
            }
        }
        catch (SQLException e) {
            logger.error("FAIL to void finalized record!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
}
