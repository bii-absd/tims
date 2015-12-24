/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

/**
 * FinalizingJobEntry is used to represent the job_id, pipeline technology 
 * (i.e. tid), pipeline name and submission time that will be retrieved from
 * database when the user is ready to finalize the study.
 * 
 * Author: Tay Wei Hong
 * Date: 22-Dec-2015
 * 
 * Revision History
 * 22-Dec-2015 - Created with the necessary methods implemented.
 * 24-Dec-2015 - Added one method, toString() to return a string representation
 * of the object.
 */

public class FinalizingJobEntry {
    private int job_id;
    private String tid, pipeline_name, submit_time;

    public FinalizingJobEntry(int job_id, String tid, String pipeline_name, 
            String submit_time) {
        this.job_id = job_id;
        this.tid = tid;
        this.pipeline_name = pipeline_name;
        this.submit_time = submit_time;
    }

    // Return a string representation of this object.
    @Override
    public String toString() {
        return job_id + " - " + pipeline_name + " - " + submit_time;
    }
    
    // Machine generated getters and setters.
    public int getJob_id() 
    { return job_id; }
    public void setJob_id(int job_id) 
    { this.job_id = job_id; }
    public String getTid() 
    { return tid; }
    public void setTid(String tid) 
    { this.tid = tid; }
    public String getPipeline_name() 
    { return pipeline_name; }
    public void setPipeline_name(String pipeline_name) 
    { this.pipeline_name = pipeline_name; }
    public String getSubmit_time() 
    { return submit_time; }
    public void setSubmit_time(String submit_time) 
    { this.submit_time = submit_time; }
}
