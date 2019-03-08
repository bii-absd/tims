// Copyright (C) 2019 A*STAR
//
// TIMS (Translation Informatics Management System) is an software effort 
// by the ABSD (Analytics of Biological Sequence Data) team in the 
// Bioinformatics Institute (BII), Agency of Science, Technology and Research 
// (A*STAR), Singapore.
//

// This file is part of TIMS.
// 
// TIMS is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as 
// published by the Free Software Foundation, either version 3 of the 
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
package TIMS.Database;

import TIMS.General.Constants;
import TIMS.General.FileHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
                     + "submit_time, input_sn, input_desc,"
                     + "parameters, output_file, detail_output, report) "
                     + "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        
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
        stm.setTimestamp(5, job.getSubmit_time());
        stm.setInt(6, job.getInput_sn());
        stm.setString(7, job.getInput_desc());
        stm.setString(8, job.getParameters());
        stm.setString(9, job.getOutput_file());
        stm.setString(10, job.getDetail_output());
        stm.setString(11, job.getReport());
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
            try (PreparedStatement stm = conn.prepareStatement(query)) {
                stm.executeUpdate();
            }
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
    public static void updateJobCompleteTime(int job_id, Timestamp complete_time) {
        Connection conn = null;
        String query = "UPDATE submitted_job SET complete_time = ? "
                     + "WHERE job_id = " + job_id;
        
        try {
            conn = DBHelper.getDSConn();
            try (PreparedStatement stm = conn.prepareStatement(query)) {
                stm.setTimestamp(1, complete_time);
                stm.executeUpdate();
            }
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
        String query = "UPDATE submitted_job SET input_sn = " + input_sn 
                     + " WHERE job_id = " + job_id;
        
        try {
            updateSJField(query);
            logger.debug("Job ID " + job_id + " input SN updated to " + input_sn);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update job input SN!");
            logger.error(e.getMessage());
        }
    }
    // Update the detail_output location. Will be called after the detail_output
    // has been zipped.
    public static void updateDetailOutputPath(int job_id, String detail_output) {
        String query = "UPDATE submitted_job SET detail_output = \'"
                     + detail_output + "\' WHERE job_id = " + job_id;
        try {
            updateSJField(query);
            logger.debug("Job ID " + job_id + " detail output updated to " 
                         + detail_output);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update detail output!");
            logger.error(e.getMessage());
        }
        
    }
    // Update the output_file location. Will be called after the output_file
    // has been zipped.
    public static void updateOutputPath(int job_id, String output_file) {
        String query = "UPDATE submitted_job SET output_file = \'"
                     + output_file + "\' WHERE job_id = " + job_id;
        try {
            updateSJField(query);
            logger.debug("Job ID " + job_id + " output file updated to " 
                         + output_file);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update output file!");
            logger.error(e.getMessage());
        }
    }
    // Set the cbio_target to true for this job. Will be called once the user 
    // decided which job(s) to export for visualization.
    public static void setCbioTarget4Job(int job_id) {
        String query = "UPDATE submitted_job SET cbio_target = true "
                     + "WHERE job_id = " + job_id;
        try {
            updateSJField(query);
            logger.debug("cbio_target updated to true for Job " + job_id);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to set cbio_target for job!");
            logger.error(e.getMessage());
        }
    }
    // Reset the cbio_target to false for all the jobs in this study.
    public static void resetCbioTarget4Study(String study_id) {
        String query = "UPDATE submitted_job SET cbio_target = false "
                     + "WHERE study_id = \'" + study_id + "\'";
        try {
            updateSJField(query);
            logger.debug("cbio_target updated to false for Sudy " + study_id);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to reset cbio_target for study!");
            logger.error(e.getMessage());
        }
        
    }
    
    // Helper function to update the individual field of the submitted job.
    private static void updateSJField(String query) 
            throws SQLException, NamingException 
    {
        Connection conn = DBHelper.getDSConn();
        try (PreparedStatement stm = conn.prepareStatement(query)) {
            stm.executeUpdate();
        }
        DBHelper.closeDSConn(conn);
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
            logger.error("FAIL to retrieve pipeline technologies!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return tidList;
    }
    
    // Return the list of distinct pipeline name that have job(s) that 
    // successfully executed in this study.
    public static List<String> getCompletedPlNameInStudy(String studyID) {
        Connection conn = null;
        List<String> plList = new ArrayList<>();
        String query = "SELECT DISTINCT pipeline_name FROM submitted_job "
                     + "WHERE study_id = ? AND status_id IN (3,5) "
                     + "ORDER BY pipeline_name";
        
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
    
    // Return the list of completed jobs for this profile in this study.
    public static List<FinalizingJobEntry> getCompletedProfileJobsInStudy
        (String studyID, String vname, String profile) {
        Connection conn = null;
        List<FinalizingJobEntry> jobList = new ArrayList<>();
        String query = "SELECT * FROM submitted_job sj INNER JOIN "
                     + "pipeline pl ON sj.pipeline_name = pl.name WHERE "
                     + "status_id IN (3,5) AND study_id = ? AND pipeline_name "
                     + "IN (SELECT pipeline_name FROM visual_profile_detail "
                     + "WHERE vname = ? AND profile = ?) "
                     + "ORDER BY pipeline_name, job_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            stm.setString(2, vname);
            stm.setString(3, profile);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                jobList.add(new FinalizingJobEntry(rs));
            }
            
            stm.close();
            logger.debug("No of completed jobs for " + studyID + " under " +
                         profile + " is " + jobList.size());
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
    
    // Return the list of completed jobs for this pipeline in this study.
    public static List<FinalizingJobEntry> getCompletedPlJobsInStudy
        (String studyID, String pipeline) {
        Connection conn = null;
        List<FinalizingJobEntry> jobList = new ArrayList<>();
        String query = "SELECT * FROM submitted_job sj INNER JOIN "
                     + "pipeline pl ON sj.pipeline_name = pl.name WHERE "
                     + "status_id IN (3,5) AND study_id = ? AND "
                     + "pipeline_name = ? ORDER BY job_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            stm.setString(2, pipeline);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                jobList.add(new FinalizingJobEntry(rs));
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
    
    // Return the list of jobs that have been exported to cBioPortal for this
    // Study ID.
    public static List<SubmittedJob> getcBioExportedJobs(String studyID) {
        String query = "SELECT * FROM submitted_job WHERE study_id =\'"
                + studyID + "\' AND cbio_target = true";
        
        return getJobsFullDetail(query);
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
                jobList.add(new SubmittedJob(rs));
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
    
    // Return the detail output filepath for this job.
    public static String getDetailOutputPath(int jobID) {
        String query = "SELECT detail_output FROM submitted_job WHERE job_id = " 
                     + jobID;
        String path = getFilePath(query, "detail_output");
        
        if (path.compareTo(Constants.DATABASE_INVALID_STR) != 0) {
            logger.debug("Detail output file for Job ID " + jobID + 
                         " stored at " + path);
        }
        
        return path;
    }
    // Return the output filepath for this job.
    public static String getOutputPath(int jobID) {
        String query = "SELECT output_file FROM submitted_job WHERE job_id = " 
                     + jobID;
        String path = getFilePath(query, "output_file");
        
        if (path.compareTo(Constants.DATABASE_INVALID_STR) != 0) {
            logger.debug("Output file for Job ID " + jobID + 
                         " stored at " + path);
        }
        
        return path;
    }

    // Helper function to retrieve the filepath of this fileType using the 
    // query passed in.
    private static String getFilePath(String query, String fileType) {
        Connection conn = null;
        String path = Constants.DATABASE_INVALID_STR;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                path = rs.getString(fileType);
            }
            
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve " + fileType);
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
    
    // Zip the detail output file of this submitted job, and update the 
    // detail_output path to the zipped filepath. The original filepath will
    // be returned.
    public static String zipDetailOutput(int job_id) {
        String doPath = getDetailOutputPath(job_id);
        String zipPath = zipTextFile(doPath);
        
        if (zipPath != null) {
            // Update the detail output filepath to the zipped version.
            updateDetailOutputPath(job_id, zipPath);
        }
        else {
            // Failed to zip the detail output file, return null.
            doPath = null;
        }
        
        return doPath;
    }
    // Zip the output file of this submitted job, and update the output_file
    // path to the zipped filepath. The original filepath will be returned.
    public static String zipOutputFile(int job_id) {
        String opPath = getOutputPath(job_id);
        String zipPath = zipTextFile(opPath);
        
        if (zipPath != null) {
            // Update the output filepath to the zipped version.
            updateOutputPath(job_id, zipPath);
        }
        else {
            // Failed to zip the output file, return null.
            opPath = null;
        }
        
        return opPath;
    }
    
    // Helper function to zip the text file (i.e. .txt), and return the 
    // filepath of the zipped text file.
    private static String zipTextFile(String filepath) {
        String result = null;
        String[] srcFile = {filepath};
        // Remove the .txt extension from the filename, and replace it with .zip
        String zipPath = filepath.substring
                         (0, filepath.indexOf(Constants.getOUTPUTFILE_EXT()));
        zipPath += Constants.getZIPFILE_EXT();

        try {
            FileHelper.zipFiles(zipPath, srcFile);
            // Return the zipped output filepath.
            result = zipPath;
            logger.debug(filepath + " zipped.");
            // Added the below statement due to a bug in Java that prevent the
            // original output file for being deleted.
            System.gc();
        }
        catch (IOException e) {
            logger.error("FAIL to zip " + filepath);
            logger.error(e.getMessage());
        }
        
        return result;        
    }
    
    // Unzip the output file of this submitted job; to be called during 
    // finalization of Study and exporting of data to visualizer. The unzipped 
    // output filepath will be returned.
    public static String unzipOutputFile(int job_id) {
        String result = null;
        byte[] buffer = new byte[2048];
        String zipPath = getOutputPath(job_id);
        // The unzipped output file will be stored in the tmp folder.
        String opPath = Constants.getSYSTEM_PATH() + Constants.getTMP_PATH();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry ze = zis.getNextEntry();
            
            if (ze != null) {
                String filename = ze.getName();
                opPath += filename;
                File opFile = new File(opPath);
                
                if (!opFile.exists()) {
                    // Only perform the unzip once.
                    try ( FileOutputStream fos = new FileOutputStream(opFile)) {
                        int len;
                        
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    logger.debug("Output file for Job ID " + job_id +
                                 " unzipped to " + opFile.getAbsolutePath());
                }
            }
            
            zis.closeEntry();
            // Return the unzipped output filepath.
            result = opPath;
            // Added the below statement due to a bug in Java that prevent the
            // temporary output file for being deleted.
            System.gc();
        }
        catch (IOException e) {
            logger.error("FAIL to unzip output file!");
            logger.error(e.getMessage());
        }
        
        return result;
    }
}
