/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * SubmittedJobDB is an abstract class and not mean to be instantiate, its 
 * main job is to perform SQL operations on the submitted_job table in the 
 * database.
 * 
 * Author: Tay Wei Hong
 * Date: 2-Oct-2015
 * 
 * Revision History
 * 02-Oct-2015 - First baseline with two static methods (insertJob and 
 * querySubmittedJob) created.
 * 05-Oct-2015 - Added 2 new variables queryOrderBy and orderIn, and the usual
 * getters and setters. Added the code to allow the query to be order by
 * different column.
 * 06-Oct-2015 - Added method getLastInsertedJob to return the job_id of the
 * most recent insert job request. Added a check at querySubmittedJob to prevent
 * the query from being run multiple times. Added Log4j2 to this class.
 * 07-Oct-2015 - Changed to connection based for database access.
 * 08-Oct-2015 - Add methods to update the status_id of submitted job.
 * 12-Oct-2015 - Added job_id field during query. Log the exception message.
 * 23-Oct-2015 - Added report field during query.
 * 30-Nov-2015 - Commented out unused code. Implementation for database 2.0
 * 04-Dec-2015 - Removed unused code. Modify method insertJob() to return the
 * job_id of the newly inserted job. Fix: querySubmittedJob should not return
 * those jobs that are either finalizing or finalized.
 * 10-Dec-2015 - Changed to abstract class. Removed unused code. Improve the
 * method getLastInsertedJob().
 */

public abstract class SubmittedJobDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubmittedJobDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private static Map<Integer,SubmittedJob> submittedJobs = new HashMap<>();
    
    SubmittedJobDB() {}
    
    // insertJob insert a new job request into the submitted_job table.
    // The job_id of the insert job request will be returned for further
    // processing. Any exception encountered here will be throw and to be 
    // handled by the caller.
    public static int insertJob(SubmittedJob job) throws SQLException {
        String insertStr = "INSERT INTO submitted_job"
                + "(study_id, user_id, pipeline_name, status_id, "
                + "submit_time, chip_type, ctrl_file, annot_file, "
                + "normalization, probe_filtering, probe_select, "
                + "phenotype_column, summarization, output_file, "
                + "sample_average, standardization, region, report) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement insertStm = conn.prepareStatement(insertStr, 
                Statement.RETURN_GENERATED_KEYS);
        // Build the INSERT statement using the variables retrieved from the
        // SubmittedJob object (i.e. job) passed in.
        insertStm.setString(1, job.getStudy_id());
        insertStm.setString(2, job.getUser_id());
        insertStm.setString(3, job.getPipeline_name());
        insertStm.setInt(4, job.getStatus_id());
        insertStm.setString(5, job.getSubmit_time());
        insertStm.setString(6, job.getChip_type());
        insertStm.setString(7, job.getCtrl_file());
        insertStm.setString(8, job.getAnnot_file());
        insertStm.setString(9, job.getNormalization());
        insertStm.setString(10, job.getProbe_filtering());
        insertStm.setBoolean(11, job.getProbe_select());
        insertStm.setString(12, job.getPhenotype_column());
        insertStm.setString(13, job.getSummarization());
        insertStm.setString(14, job.getOutput_file());
        insertStm.setString(15, job.getSample_average());
        insertStm.setString(16, job.getStandardization());
        insertStm.setString(17, job.getRegion());
        insertStm.setString(18, job.getReport());
        // Execute the INSERT statement
        insertStm.executeUpdate();
        // Retrieve and store the last inserted Job ID
        ResultSet rs = insertStm.getGeneratedKeys();
        int job_id = Constants.DATABASE_INVALID_ID;
        
        if (rs.next()) {
            job_id = rs.getInt(1);
        }
        
        logger.debug("New job request inserted into database. ID: " + job_id);
        
        // Return the job_id of the inserted job request
        return job_id;
    }

    // Return the job_id of the most recently inserted job request.
    // NOT IN USE!
    public static int getLastInsertedJob() throws SQLException {
        int job_id = Constants.DATABASE_INVALID_ID;
        ResultSet rs = DBHelper.runQuery("SELECT MAX(job_id) FROM submitted_job");
        
        if (rs.next()) {
            job_id = rs.getInt(1);
        }
        
        return job_id;
    }
    
    // The following functions update the status_id of the submitted_job
    public static void updateJobStatusToInprogress(int job_id) {
        updateJobStatus(job_id, 2);
    }    
    public static void updateJobStatusToCompleted(int job_id) {
        updateJobStatus(job_id, 3);
    }
    public static void updateJobStatusToFinalizing(int job_id) {
        updateJobStatus(job_id,4);
    }
    public static void updateJobStatusToFinalized(int job_id) {
        updateJobStatus(job_id, 5);
    }
    public static void updateJobStatusToFailed(int job_id) {
        updateJobStatus(job_id, 6);
    }
    
    // Internal helper function for updateJobStatusToXXX
    // It will update the status_id of the job according to the status_id 
    // passed in.
    private static void updateJobStatus(int job_id, int status_id) {
        String updateStr = "UPDATE submitted_job SET status_id = " + status_id 
                           + " WHERE job_id = " + job_id;

        try (PreparedStatement updateStatus = conn.prepareStatement(updateStr))
        {
            updateStatus.executeUpdate();
            logger.debug("Updated job status.");
        }
        catch (SQLException e) {
            logger.error("SQLException when updating job status.");
            logger.error(e.getMessage());
        }
    }
    
    // Query the submitted_job table using the user_id as a match condition. 
    // The results from the query will be returned as a list to the caller.
    public static List<SubmittedJob> querySubmittedJob(String user_id) {
        // Only execute the query if the list is empty
        // This is to prevent the query from being run multiple times.
        if (submittedJobs.isEmpty()) {
            ResultSet result;
            // Don't retrieve those jobs which are in finalizing or finalized
            // stages.
            String queryStr = "SELECT job_id, study_id, pipeline_name, "
                    + "status_id, submit_time, output_file, report FROM "
                    + "submitted_job WHERE user_id = ? AND status_id NOT IN (4,5) "
                    + "ORDER BY job_id DESC"; 

            // Additional logging to get the logger context
            /*
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration cfg = ctx.getConfiguration();
            FileAppender fa = (FileAppender) cfg.getAppender("File");
            */
            
            try (PreparedStatement queryJob = conn.prepareStatement(queryStr))
            {
                // Set the query condition using the user_id passed in.
                queryJob.setString(1, user_id);
                result = queryJob.executeQuery();
                int id = 1;
            
                while (result.next()) {
                    // Create a new SubmittedJob object for every row of data 
                    // retrieved from the database.
                    SubmittedJob job = new SubmittedJob(
                                    result.getInt("job_id"),
                                    result.getString("study_id"),
                                    result.getString("pipeline_name"),
                                    result.getInt("status_id"),
                                    result.getString("submit_time"),
                                    result.getString("output_file"),
                                    result.getString("report"));
                    // Add the object to the HashMap
                    submittedJobs.put(id++, job);
                }
                logger.debug("Query submitted job completed.");
            } catch (SQLException e) {
                logger.error("SQLException when query submitted job!");
                logger.error(e.getMessage());
                // Exception has occurred, return back a empty list.
                return new ArrayList<>(0);
            }
        }
        
        return new ArrayList<>(submittedJobs.values());
    }

    // Clear the HashMap, so that the query to the database will be run again.
    public static void clearSubmittedJobs() {
        submittedJobs.clear();
    }
}
