/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.Bean.AuthenticationBean;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;

/**
 * SubmittedJob is used to represent the submitted_job table in the database.
 * It contain one extra variable, status_name, which contains the status name
 * of this job.
 * 
 * Author: Tay Wei Hong
 * Date: 01-Oct-2015
 * 
 * Revision History
 * 01-Oct-2015 - Created with all the standard getters and setters.
 * 02-Oct-2015 - Added in one extra variable status_name, and method 
 * getStatus_name.
 * 07-Oct-2015 - Added Log4j2 for this class.
 * 12-Oct-2015 - Added job_id field.
 * 23-Oct-2015 - Added report field.
 * 30-Oct-2015 - Added one new function getAvailable.
 */
public class SubmittedJob implements Serializable {
    private int job_id;
    private String user_id;
    private int status_id;
    private String study_id;
    private String submit_time;
    private String output_file;
    private String report;
    // status_name will be used by the job status page
    private String status_name;

    public SubmittedJob(int job_id, int status_id, String study_id, 
            String submit_time, String output_file, String report) {
        this.job_id = job_id;
        this.user_id = AuthenticationBean.getUserName();
        this.status_id = status_id;
        this.study_id = study_id;
        this.submit_time = submit_time;
        this.output_file = output_file;
        this.report = report;
    }

    // Return the job status name of this submitted job.
    public String getStatus_name() {
        return JobStatus.getStatusName(status_id);
    }
    
    // Based on the job status, the download link at jobstatus.xhtml will be
    // enabled or disabled accordingly.
    public String getAvailable() {
        if (JobStatus.getStatusName(status_id).compareTo("Completed") == 0)
        {
            return Constants.FALSE;
        }
        else {
            return Constants.TRUE;
        }
    }
    
    // Machine generated setters
    public void setJob_id(int job_id) { this.job_id = job_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setStatus_id(int status_id) { this.status_id = status_id; }
    public void setStudy_id(String study_id) { this.study_id = study_id; }
    public void setSubmit_time(String submit_time) { this.submit_time = submit_time; }
    public void setOutput_file(String output_file) { this.output_file = output_file; }
    public void setReport(String report) { this.report = report; }
    
    // Machine generated geters
    public int getJob_id() { return job_id; }
    public String getUser_id() { return user_id; }
    public int getStatus_id() { return status_id; }
    public String getStudy_id() { return study_id; }
    public String getSubmit_time() { return submit_time; }
    public String getOutput_file() { return output_file; }
    public String getReport() { return report; }
}
