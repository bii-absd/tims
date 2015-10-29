/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.Bean.AuthenticationBean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * SubmittedJobDB is not mean to be instantiate, its main job is to perform
 * SQL operations on the submitted_job table in the database.
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
 */

public class SubmittedJobDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubmittedJobDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private static Map<Integer,SubmittedJob> submittedJobs = new HashMap<>();
    // By default, the query will be order by job_id DESC
    private static String queryOrderBy = "job_id";
    private static String orderIn = "DESC";
    
    SubmittedJobDB() {}
    
    // insertJob insert a new job request into the submitted_job table.
    // The job_id of the insert job request will be returned for further
    // processing. Any exception encountered here will be throw and to be 
    // handled by the caller.
    public static int insertJob(SubmittedJob job) throws SQLException {
        String insertStr = "INSERT INTO submitted_job"
                + "(user_id, status_id, study_id, submit_time, output_file) "
                + "VALUES(?,?,?,?,?)";
        PreparedStatement insertStm = conn.prepareStatement(insertStr);
        // Build the INSERT statement using the variables retrieved from the
        // SubmittedJob object (i.e. job) passed in.
        insertStm.setString(1, job.getUser_id());
        insertStm.setInt(2, job.getStatus_id());
        insertStm.setString(3, job.getStudy_id());
        insertStm.setString(4, job.getSubmit_time());
        insertStm.setString(5, job.getOutput_file());
        // Execute the INSERT statement
        insertStm.executeUpdate();
        // Get the job_id of the inserted job
        int job_id = getLastInsertedJob();
        
        logger.debug("New job request inserted into database. ID: " + job_id);
        
        // Return the job_id of the inserted job request
        return job_id;
    }

    // getLastInsertedJob will return the job_id of the most recently inserted
    // job request.
    public static int getLastInsertedJob() throws SQLException {
        ResultSet lastJobID;
        int job_id = 0;
        
        String queryLast = "SELECT job_id FROM submitted_job "
                + "ORDER BY job_id DESC LIMIT 1";
        PreparedStatement queryStm = conn.prepareStatement(queryLast);
        lastJobID = queryStm.executeQuery();
        
        while (lastJobID.next()) {
            job_id = lastJobID.getInt("job_id");
        }
        
        return job_id;
    }
    
    // The following 3 functions will update the status_id of the submitted_job
    // KIV: Can we do this without hardcoding?
    public static void updateJobStatusToInprogress(int job_id) {
        updateJobStatus(job_id, 2);
    }    
    public static void updateJobStatusToCompleted(int job_id) {
        updateJobStatus(job_id, 3);
    }
    public static void updateJobStatusToFailed(int job_id) {
        updateJobStatus(job_id, 4);
    }
    
    // updateJobStatus is the internal helper function for updateJobStatusToXXX
    // It will update the status_id of the job according to the status_id 
    // passed in.
    private static void updateJobStatus(int job_id, int status_id) {
        String updateStr = "UPDATE submitted_job SET status_id = " + status_id 
                           + " WHERE job_id = " + job_id;
        
        logger.debug(updateStr);
        try (PreparedStatement updateStatus = conn.prepareStatement(updateStr))
        {
            updateStatus.executeUpdate();
            logger.debug("updateJobStatus success.");
        }
        catch (SQLException e) {
            logger.error(AuthenticationBean.getUserName() + 
                    ": encountered SQLException at updateJobStatus.");
            System.out.println(e.getMessage());
        }
    }
    
    // querySubmittedJob will query the submitted_job table using the user_id 
    // as a match condition. The results from the query will be returned as
    // a list to the caller.
    public static List querySubmittedJob(String user_id) {
        // Only execute the query if the list is empty
        // This is to prevent the query from being run multiple times.
        if (submittedJobs.isEmpty()) {
            ResultSet result;
        
            String queryStr = "SELECT job_id, status_id, submit_time, "
                    + "output_file, study_id FROM submitted_job WHERE "
                    + "user_id = ? ORDER BY " + queryOrderBy + " " + orderIn;
        
            logger.debug(queryStr);
                
            try (PreparedStatement queryJob = conn.prepareStatement(queryStr))
            {
                // Set the query condition using the user_id passed in.
                queryJob.setString(1, user_id);
                result = queryJob.executeQuery();
                int id = 1;
            
                while (result.next()) {
                    // Create a new SubmittedJob object for every row of data 
                    // retrieved from the data.
                    SubmittedJob job = new SubmittedJob(
                                    result.getInt("job_id"),
                                    result.getInt("status_id"),
                                    result.getString("study_id"),
                                    result.getString("submit_time"),
                                    result.getString("output_file"));
                    // Add the object to the HashMap
                    submittedJobs.put(id++, job);
                }
                logger.debug("querySubmittedJob success.");
            } catch (SQLException e) {
                logger.error(AuthenticationBean.getUserName() + 
                        ": encountered SQLException at querySubmittedJob.");
                logger.error(e.getMessage());
                // Some errors has occurred, return back a empty list.
                return new ArrayList(0);
            }
        }
        
        return new ArrayList<SubmittedJob>(submittedJobs.values());
    }

    // setQueryOrderBy will sort the result return by query according to the
    // parameter passed in. The order (e.g. ASC/DESC) will be based on the
    // order by type.
    public static void setQueryOrderBy(String queryOrderBy) {
        if (queryOrderBy.equals("study_id")) {
            // For order by study_id, the order will be in ASC
            setOrderIn("ASC");
            SubmittedJobDB.queryOrderBy = queryOrderBy;            
            // Clear away the result of the last query operation
            clearSubmittedJobs();
        }
        else {
            setQueryOrderBy();
        }
        
        logger.info(AuthenticationBean.getUserName() + 
                ": setup job submission query - ORDER BY " + 
                getQueryOrderBy() + " " + getOrderIn());
    }
    
    // setQueryOrderBy without parameter will set the default order by and
    // order in condition.
    public static void setQueryOrderBy() {
        queryOrderBy = "job_id";
        setOrderIn("DESC");
        // Clear away the result of the last query operation
        clearSubmittedJobs();
    }
    
    // clearSubmittedJobs will clear the HashMap, so that the query to the database
    // will be run again.
    private static void clearSubmittedJobs() {
        submittedJobs.clear();
    }
    // Machine generated getters and setters
    public static String getQueryOrderBy() { return queryOrderBy; }
    public static String getOrderIn() { return orderIn; }
    public static void setOrderIn(String orderIn) {
        SubmittedJobDB.orderIn = orderIn;
    }
}
