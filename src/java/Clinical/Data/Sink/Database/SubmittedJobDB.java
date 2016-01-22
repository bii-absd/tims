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
 */

public abstract class SubmittedJobDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubmittedJobDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    
    // Insert a new job request into the submitted_job table.
    // The job_id of the insert job request will be returned for further
    // processing. Any exception encountered here will be throw and to be 
    // handled by the caller.
    public static int insertJob(SubmittedJob job) throws SQLException {
        int job_id = Constants.DATABASE_INVALID_ID;
        String insertStr = "INSERT INTO submitted_job"
                + "(study_id, user_id, pipeline_name, status_id, "
                + "submit_time, chip_type, input_path,"
                + "normalization, probe_filtering, probe_select, "
                + "phenotype_column, summarization, output_file, "
                + "sample_average, standardization, region, report) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        // To request for the return of generated key upon successful insertion.
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
        insertStm.setString(7, job.getInput_path());
        insertStm.setString(8, job.getNormalization());
        insertStm.setString(9, job.getProbe_filtering());
        insertStm.setBoolean(10, job.getProbe_select());
        insertStm.setString(11, job.getPhenotype_column());
        insertStm.setString(12, job.getSummarization());
        insertStm.setString(13, job.getOutput_file());
        insertStm.setBoolean(14, job.getSample_average());
        insertStm.setString(15, job.getStandardization());
        insertStm.setString(16, job.getRegion());
        insertStm.setString(17, job.getReport());
        // Execute the INSERT statement
        insertStm.executeUpdate();
        // Retrieve and store the last inserted Job ID
        ResultSet rs = insertStm.getGeneratedKeys();
        
        if (rs.next()) {
            job_id = rs.getInt(1);
        }
        
        logger.debug("New job request inserted into database. ID: " + job_id);
        
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
        String updateStr = "UPDATE submitted_job SET status_id = " + status_id 
                         + " WHERE job_id = " + job_id;

        try (PreparedStatement updateStm = conn.prepareStatement(updateStr))
        {
            updateStm.executeUpdate();
            logger.debug("Job ID " + job_id + ": status updated to " + 
                    JobStatusDB.getStatusName(status_id));
        }
        catch (SQLException e) {
            logger.error("FAIL to update job status!");
            logger.error(e.getMessage());
        }
    }
    
    // Return the list of pipeline technologies used in this study.
    public static List<String> getTIDUsedInStudy(String studyID) {
        List<String> tidList = new ArrayList<>();
        String queryStr = "SELECT DISTINCT tid FROM submitted_job sj INNER JOIN "
                        + "pipeline pl ON sj.pipeline_name = pl.name "
                        + "WHERE study_id = ? ORDER BY tid";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, studyID);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                tidList.add(rs.getString("tid"));
            }
            logger.debug("Pipeline technologies run for " + studyID + ": "
                         + tidList.toString());
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve pipeline technologies executed!");
            logger.error(e.getMessage());
        }
        
        return tidList;
    }
    
    // Return the list of distinct pipeline that have been executed in this study.
    public static List<String> getPipelineExeInStudy(String studyID) {
        List<String> plList = new ArrayList<>();
        String queryStr = "SELECT DISTINCT pipeline_name FROM submitted_job "
                        + "WHERE study_id = ? ORDER BY pipeline_name";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, studyID);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                plList.add(rs.getString("pipeline_name"));
            }
            logger.debug("Pipeline executed in " + studyID + ": " + 
                         plList.toString());
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve pipeline executed!");
            logger.error(e.getMessage());
        }
        
        return plList;
    }
    
    // Return the list of completed jobs for this pipeline that are ready to
    // be finalized for this study.
    public static List<FinalizingJobEntry> getCompletedPlJobsInStudy
        (String studyID, String pipeline) {
        List<FinalizingJobEntry> jobList = new ArrayList<>();
        String queryStr = "SELECT job_id, tid, submit_time, user_id "
                        + "FROM submitted_job sj INNER JOIN pipeline pl "
                        + "ON sj.pipeline_name = pl.name WHERE "
                        + "status_id = 3 AND study_id = ? AND "
                        + "pipeline_name = ? ORDER BY job_id";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, studyID);
            queryStm.setString(2, pipeline);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                FinalizingJobEntry tmp = new FinalizingJobEntry(
                                            rs.getInt("job_id"),
                                            rs.getString("tid"),
                                            pipeline,
                                            rs.getString("submit_time"),
                                            rs.getString("user_id"));
                
                jobList.add(tmp);
            }
            logger.debug("No of completed jobs for " + studyID + " under " +
                         pipeline + " is " + jobList.size());
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve completed pipeline jobs for " + studyID);
            logger.error(e.getMessage());
        }
        
        return jobList;
    }
    
    // Return the list of completed jobs that are ready to be finalized for
    // this study.
    public static List<FinalizingJobEntry> getCompletedJobsInStudy
        (String studyID, String tid) {
        List<FinalizingJobEntry> jobList = new ArrayList<>();
        String queryStr = "SELECT job_id, tid, pipeline_name, submit_time, "
                        + "user_id FROM submitted_job sj INNER JOIN pipeline pl "
                        + "ON sj.pipeline_name = pl.name WHERE "
                        + "status_id = 3 AND study_id = ? AND tid = ? "
                        + "ORDER BY job_id";

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, studyID);
            queryStm.setString(2, tid);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                FinalizingJobEntry tmp = new FinalizingJobEntry(
                                            rs.getInt("job_id"),
                                            rs.getString("tid"),
                                            rs.getString("pipeline_name"),
                                            rs.getString("submit_time"),
                                            rs.getString("user_id"));
                jobList.add(tmp);
            }
            logger.debug("No of completed jobs for " + studyID + " under " +
                         tid + " technology is " + jobList.size());
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve completed jobs for " + studyID);
            logger.error(e.getMessage());
        }
        
        return jobList;
    }

    // Return the list of jobs (full detail) that have been submitted by 
    // this user.
    public static List<SubmittedJob> getJobsFullDetail(String user_id) {
        List<SubmittedJob> jobList = new ArrayList<>();
        // Don't retrieve those jobs which are in finalizing stage.
        String queryStr = "SELECT * FROM submitted_job WHERE user_id = ? AND "
                        + "status_id NOT IN (4) ORDER BY job_id DESC"; 

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, user_id);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                SubmittedJob job = new SubmittedJob(
                                rs.getInt("job_id"),
                                rs.getString("study_id"),
                                user_id,
                                rs.getString("pipeline_name"),
                                rs.getInt("status_id"),
                                rs.getString("submit_time"),
                                rs.getString("chip_type"),
                                rs.getString("input_path"),
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
            logger.debug("Jobs full detail retrieved for " + user_id);
        } 
        catch (SQLException e) {
                logger.error("FAIL to retrieve jobs full detail!");
                logger.error(e.getMessage());
        }
        
        return jobList;
    }

    // Return the list of jobs that have been submitted by this user.
    public static List<SubmittedJob> getSubmittedJobs(String user_id) {
        List<SubmittedJob> jobList = new ArrayList<>();
        // Don't retrieve those jobs which are in finalizing stage.
        String queryStr = "SELECT job_id, study_id, pipeline_name, "
                + "status_id, submit_time, output_file, report FROM "
                + "submitted_job WHERE user_id = ? AND status_id NOT IN (4) "
                + "ORDER BY job_id DESC"; 

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, user_id);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                SubmittedJob job = new SubmittedJob(
                                rs.getInt("job_id"),
                                rs.getString("study_id"),
                                user_id,
                                rs.getString("pipeline_name"),
                                rs.getInt("status_id"),
                                rs.getString("submit_time"),
                                rs.getString("output_file"),
                                rs.getString("report"));
                
                jobList.add(job);
            }
            logger.debug("Submitted job retrieved for " + user_id);
        } 
        catch (SQLException e) {
                logger.error("FAIL to retrieve submitted job!");
                logger.error(e.getMessage());
        }
        
        return jobList;
    }

    // Return the output filepath for this job.
    public static String getOutputPath(int jobID) {
        String path = Constants.DATABASE_INVALID_STR;
        String query = "SELECT output_file FROM submitted_job WHERE job_id = " 
                     + jobID;
        ResultSet rs = DBHelper.runQuery(query);
        
        try {
            if (rs.next()) {
                path = rs.getString("output_file");
                logger.debug("Output file for job_id " + jobID + 
                             " stored at " + path);
            }
            rs.close();
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve output filepath!");
            logger.error(e.getMessage());
        }
        
        return path;
    }

    // Return the pipeline name for this job.
    public static String getPipelineName(int jobID) {
        String plName = Constants.DATABASE_INVALID_STR;
        String queryStr = "SELECT pipeline_name FROM submitted_job "
                        + "WHERE job_id = " + jobID;
        ResultSet rs = DBHelper.runQuery(queryStr);
        
        try {
            if (rs.next()) {
                plName = rs.getString("pipeline_name");
            }
            rs.close();
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve pipeline name!");
            logger.error(e.getMessage());
        }
        
        return plName;
    }
}
