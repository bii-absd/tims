/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * SubmittedJobDB is an abstract class and not mean to be instantiate, its 
 * main job is to perform SQL operations on the submitted_job table in the 
 * database.
 * 
 * Author: Tay Wei Hong
 * Date: 02-Oct-2015
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
 * 23-Dec-2015 - To close the ResultSet after use. Added 2 new methods, 
 * queryTIDUsedInStudy and queryCompletedJobsInStudy to support the finalize
 * study module.
 * 24-Dec-2015 - Added one method, getOutputPath to retrieve the output 
 * filepath of the submitted job.
 * 30-Dec-2015 - Changed the query in method querySubmittedJob; to include
 * jobs that are in finalized stage.
 * 05-Jan-2015 - Changes in submitted_job table, removed ctrl_file and annot_
 * file fields. Added input_path field.
 * 11-Jan-2015 - Added new method, getPipelineName to retrieve the name of the
 * pipeline executed for the job.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Jan-2016 - Removed all the static variables in Job Status module.
 * 18-Jan-2016 - Changed the type of variable sample_average from String to
 * Boolean.
 * 21-Jan-2016 - Added 3 new methods; getJobsFullDetail to return the list of
 * full detail SubmittedJob objects, getPipelineExeInStudy to return the
 * list of pipeline executed in the study, and updateJobStatusToWaiting.
 * 22-Jan-2016 - Study finalization logic change; finalization will be 
 * performed for each pipeline instead of each technology.
 * 01-Feb-2016 - When retrieving submitted jobs, there are now 2 options 
 * available i.e. to retrieve for single user or all users (enable for 
 * administrator only).
 * 15-Feb-2016 - Added one new method getFinalizedJobIDs, to return all the
 * finalized job ID for the respective study ID.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 14-Mar-2016 - To fetch the condition for the query in method 
 * getFinalizedJobIDs from JobStatusDB class, instead of hard-coding it.
 * 24-Mar-2016 - Changes due to the new attribute (i.e. complete_time) added in
 * submitted_job table.
 * 29-Mar-2016 - Changes due to the removal of input_path and the addition of
 * input_sn in submitted_job table.
 * 08-Apr-2016 - Changes due to addition fields defined in FinalizingJobEntry
 * class.
 */

public abstract class SubmittedJobDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubmittedJobDB.class.getName());
    
    // Insert a new job request into the submitted_job table.
    // The job_id of the insert job request will be returned for further
    // processing. Any exception encountered here will be throw and to be 
    // handled by the caller.
    public static int insertJob(SubmittedJob job) 
            throws SQLException, NamingException 
    {
        Connection conn = null;
        int job_id = Constants.DATABASE_INVALID_ID;
        String query = "INSERT INTO submitted_job"
                     + "(study_id, user_id, pipeline_name, status_id, "
                     + "submit_time, chip_type, input_sn,"
                     + "normalization, probe_filtering, probe_select, "
                     + "phenotype_column, summarization, output_file, "
                     + "sample_average, standardization, region, report) "
                     + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        
        conn = DBHelper.getDSConn();
        // To request for the return of generated key upon successful insertion.
        PreparedStatement stm = conn.prepareStatement(query, 
                                Statement.RETURN_GENERATED_KEYS);
        // Build the INSERT statement using the variables retrieved from the
        // SubmittedJob object (i.e. job) passed in.
        stm.setString(1, job.getStudy_id());
        stm.setString(2, job.getUser_id());
        stm.setString(3, job.getPipeline_name());
        stm.setInt(4, job.getStatus_id());
        stm.setString(5, job.getSubmit_time());
        stm.setString(6, job.getChip_type());
        stm.setInt(7, job.getInput_sn());
        stm.setString(8, job.getNormalization());
        stm.setString(9, job.getProbe_filtering());
        stm.setBoolean(10, job.getProbe_select());
        stm.setString(11, job.getPhenotype_column());
        stm.setString(12, job.getSummarization());
        stm.setString(13, job.getOutput_file());
        stm.setBoolean(14, job.getSample_average());
        stm.setString(15, job.getStandardization());
        stm.setString(16, job.getRegion());
        stm.setString(17, job.getReport());
        // Execute the INSERT statement
        stm.executeUpdate();
        // Retrieve and store the last inserted Job ID
        ResultSet rs = stm.getGeneratedKeys();
        
        if (rs.next()) {
            job_id = rs.getInt(1);
        }
        logger.debug("New job request inserted into database. ID: " + job_id);

        stm.close();
        DBHelper.closeDSConn(conn);
        // Return the job_id of the inserted job request
        return job_id;
    }

    // The following functions update the status_id of the submitted_job
    public static void updateJobStatusToWaiting(int job_id) {
        updateJobStatus(job_id, 1);
    }
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
        Connection conn = null;
        String query = "UPDATE submitted_job SET status_id = " + status_id 
                     + " WHERE job_id = " + job_id;

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.executeUpdate();
            stm.close();
            logger.debug("Job ID " + job_id + ": status updated to " + 
                    JobStatusDB.getJobStatusName(status_id));
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update job status!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Pipeline has completed execution, update the complete time for this job.
    public static void updateJobCompleteTime(int job_id, String complete_time) {
        Connection conn = null;
        String query = "UPDATE submitted_job SET complete_time = ? "
                     + "WHERE job_id = " + job_id;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, complete_time);
            stm.executeUpdate();
            stm.close();
            logger.debug("Job ID " + job_id + " completion time updated to " 
                    + complete_time);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update job completion time!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Update the input sn used in this job. Will be called when there is new
    // raw data uploaded during pipeline configuration.
    public static void updateJobInputSN(int job_id, int input_sn) {
        Connection conn = null;
        String query = "UPDATE submitted_job SET input_sn = " + input_sn 
                     + " WHERE job_id = " + job_id;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.executeUpdate();
            stm.close();
            logger.debug("Job ID " + job_id + " input SN updated to " + input_sn);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update job input SN!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Return the list of pipeline technologies used in this study.
    public static List<String> getTIDUsedInStudy(String studyID) {
        Connection conn = null;
        List<String> tidList = new ArrayList<>();
        String query = "SELECT DISTINCT tid FROM submitted_job sj INNER JOIN "
                     + "pipeline pl ON sj.pipeline_name = pl.name "
                     + "WHERE study_id = ? ORDER BY tid";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                tidList.add(rs.getString("tid"));
            }
            
            stm.close();
            logger.debug("Pipeline technologies run for " + studyID + ": "
                         + tidList.toString());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve pipeline technologies executed!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return tidList;
    }
    
    // Return the list of distinct pipeline that have been executed in this study.
    public static List<String> getPipelineExeInStudy(String studyID) {
        Connection conn = null;
        List<String> plList = new ArrayList<>();
        String query = "SELECT DISTINCT pipeline_name FROM submitted_job "
                     + "WHERE study_id = ? ORDER BY pipeline_name";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                plList.add(rs.getString("pipeline_name"));
            }
            
            stm.close();
            logger.debug("Pipeline executed in " + studyID + ": " + 
                         plList.toString());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve pipeline executed!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return plList;
    }
    
    // Return the list of completed jobs for this pipeline that are ready to
    // be finalized for this study.
    public static List<FinalizingJobEntry> getCompletedPlJobsInStudy
        (String studyID, String pipeline) {
        Connection conn = null;
        List<FinalizingJobEntry> jobList = new ArrayList<>();
        String query = "SELECT study_id, job_id, tid, submit_time, user_id, "
                     + "chip_type, input_sn, normalization "
                     + "FROM submitted_job sj INNER JOIN pipeline pl "
                     + "ON sj.pipeline_name = pl.name WHERE "
                     + "status_id = 3 AND study_id = ? AND "
                     + "pipeline_name = ? ORDER BY job_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            stm.setString(2, pipeline);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                FinalizingJobEntry tmp = new FinalizingJobEntry(
                                            rs.getInt("job_id"),
                                            rs.getInt("input_sn"),
                                            rs.getString("study_id"),
                                            rs.getString("tid"),
                                            pipeline,
                                            rs.getString("submit_time"),
                                            rs.getString("user_id"),
                                            rs.getString("chip_type"),
                                            rs.getString("normalization"));
                
                jobList.add(tmp);
            }
            
            stm.close();
            logger.debug("No of completed jobs for " + studyID + " under " +
                         pipeline + " is " + jobList.size());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve completed pipeline jobs for " + studyID);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return jobList;
    }
    
    // Return the list of completed jobs that are ready to be finalized for
    // this study.
    // NOT IN USE.
    public static List<FinalizingJobEntry> getCompletedJobsInStudy
        (String studyID, String tid) {
        Connection conn = null;
        List<FinalizingJobEntry> jobList = new ArrayList<>();
        String query = "SELECT study_id, job_id, tid, pipeline_name, submit_time, "
                     + "user_id, chip_type, input_sn, normalization "
                     + "FROM submitted_job sj INNER JOIN pipeline pl "
                     + "ON sj.pipeline_name = pl.name WHERE "
                     + "status_id = 3 AND study_id = ? AND tid = ? "
                     + "ORDER BY job_id";

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            stm.setString(2, tid);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                FinalizingJobEntry tmp = new FinalizingJobEntry(
                                            rs.getInt("job_id"),
                                            rs.getInt("input_sn"),
                                            rs.getString("study_id"),
                                            rs.getString("tid"),
                                            rs.getString("pipeline_name"),
                                            rs.getString("submit_time"),
                                            rs.getString("user_id"),
                                            rs.getString("chip_type"),
                                            rs.getString("normalization"));
                jobList.add(tmp);
            }
            
            stm.close();
            logger.debug("No of completed jobs for " + studyID + " under " +
                         tid + " technology is " + jobList.size());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve completed jobs for " + studyID);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return jobList;
    }
    
    // Return the list of jobs that have been submitted by all the users.
    public static List<SubmittedJob> getAllUsersJobs() {
        String query = "SELECT * FROM submitted_job ORDER BY user_id, job_id DESC";
        
        return getJobsFullDetail(query);
    }
    
    // Return the list of jobs that have been submitted by this user.
    public static List<SubmittedJob> getUserJobs(String user_id) {
        // Don't retrieve those jobs which are in finalizing stage.
        String query = "SELECT * FROM submitted_job WHERE user_id = \'" 
                     + user_id + "\' AND status_id NOT IN (4) ORDER BY job_id DESC"; 
        
        return getJobsFullDetail(query);
    }
    
    // Return the list of jobs (full detail) based on the query.
    public static List<SubmittedJob> getJobsFullDetail(String query) {
        Connection conn = null;
        List<SubmittedJob> jobList = new ArrayList<>();

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                SubmittedJob job = new SubmittedJob(
                                rs.getInt("job_id"),
                                rs.getString("study_id"),
                                rs.getString("user_id"),
                                rs.getString("pipeline_name"),
                                rs.getInt("status_id"),
                                rs.getString("submit_time"),
                                rs.getString("complete_time"),
                                rs.getString("chip_type"),
                                rs.getInt("input_sn"),
                                rs.getString("normalization"),
                                rs.getString("probe_filtering"),
                                rs.getBoolean("probe_select"),
                                rs.getString("phenotype_column"),
                                rs.getString("summarization"),
                                rs.getString("output_file"),
                                rs.getBoolean("sample_average"),
                                rs.getString("standardization"),
                                rs.getString("region"),
                                rs.getString("report"));
                
                jobList.add(job);
            }
            
            stm.close();
            logger.debug("Jobs full detail retrieved.");
        } 
        catch (SQLException|NamingException e) {
                logger.error("FAIL to retrieve jobs full detail!");
                logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return jobList;
    }

    // Return the list of jobs that have been submitted by this user.
    // NOT IN USE ANYMORE!
    public static List<SubmittedJob> getSubmittedJobs(String user_id) {
        Connection conn = null;
        List<SubmittedJob> jobList = new ArrayList<>();
        // Don't retrieve those jobs which are in finalizing stage.
        String query = "SELECT job_id, study_id, pipeline_name, "
                + "status_id, submit_time, complete_time, output_file, report "
                + "FROM submitted_job WHERE user_id = ? AND "
                + "status_id NOT IN (4) ORDER BY job_id DESC"; 

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, user_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                SubmittedJob job = new SubmittedJob(
                                rs.getInt("job_id"),
                                rs.getString("study_id"),
                                user_id,
                                rs.getString("pipeline_name"),
                                rs.getInt("status_id"),
                                rs.getString("submit_time"),
                                rs.getString("complete_time"),
                                rs.getString("output_file"),
                                rs.getString("report"));
                
                jobList.add(job);
            }
            
            stm.close();
            logger.debug("Submitted job retrieved for " + user_id);
        } 
        catch (SQLException|NamingException e) {
                logger.error("FAIL to retrieve submitted job!");
                logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return jobList;
    }

    // Retrieve all the finalized job IDs for this study ID.
    public static List<Integer> getFinalizedJobIDs(String study_id) {
        Connection conn = null;
        List<Integer> jobIDList = new ArrayList<>();
        String query = "SELECT job_id FROM submitted_job WHERE status_id = "
                     + JobStatusDB.finalized() + " AND study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                jobIDList.add(rs.getInt("job_id"));
            }
            
            stm.close();
            logger.debug("Finalized job IDs retrieved for " + study_id);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve finalized job IDs!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return jobIDList;
    }
    
    // Return the output filepath for this job.
    public static String getOutputPath(int jobID) {
        Connection conn = null;
        String path = Constants.DATABASE_INVALID_STR;
        String query = "SELECT output_file FROM submitted_job WHERE job_id = " 
                     + jobID;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                path = rs.getString("output_file");
                logger.debug("Output file for job_id " + jobID + 
                             " stored at " + path);
            }
            
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve output filepath!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return path;
    }

    // Return the pipeline name for this job.
    public static String getPipelineName(int jobID) {
        Connection conn = null;
        String plName = Constants.DATABASE_INVALID_STR;
        String query = "SELECT pipeline_name FROM submitted_job "
                     + "WHERE job_id = " + jobID;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
        
            if (rs.next()) {
                plName = rs.getString("pipeline_name");
            }
            
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve pipeline name!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return plName;
    }
}
