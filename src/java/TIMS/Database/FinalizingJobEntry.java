/*
 * Copyright @2015
 */
package TIMS.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
 * 22-Jan-2016 - Added one new field, user_id and one new method getUserName().
 * 08-Apr-2016 - Added new fields; input_sn, study_id, chip_type and 
 * normalization, and one new method getInputDesc().
 * 12-Apr-2016 - Added one new field, summarization.
 * 14-Apr-2016 - Change type for submit_time to Timestamp.
 * 19-May-2016 - Added one attribute detail_output. Update the constructor to
 * receive the FinalizingJobEntry parameters directly from the database.
 */

public class FinalizingJobEntry {
    private int job_id, input_sn;
    private String study_id, tid, pipeline_name, user_id, 
            chip_type, normalization, summarization, detail_output;
    private Timestamp submit_time;
    private final static DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mmaa");

    // Construct the FinalizingJobEntry object directly using the result set
    // returned from the database query.
    public FinalizingJobEntry(ResultSet rs, String pipeline_name) 
            throws SQLException {
        this.job_id = rs.getInt("job_id");
        this.input_sn = rs.getInt("input_sn");
        this.study_id = rs.getString("study_id");
        this.tid = rs.getString("tid");
        this.pipeline_name = pipeline_name;
        this.submit_time = rs.getTimestamp("submit_time");
        this.user_id = rs.getString("user_id");
        this.chip_type = rs.getString("chip_type");
        this.normalization = rs.getString("normalization");
        this.summarization = rs.getString("summarization");
        this.detail_output = rs.getString("detail_output");
    }
    
    // Full constructor.
    // NOT IN USE ANYMORE!
    /*
    public FinalizingJobEntry(int job_id, int input_sn, String study_id, 
            String tid, String pipeline_name, Timestamp submit_time, 
            String user_id, String chip_type, String normalization, 
            String summarization, String detail_output) 
    {
        this.job_id = job_id;
        this.input_sn = input_sn;
        this.study_id = study_id;
        this.tid = tid;
        this.pipeline_name = pipeline_name;
        this.submit_time = submit_time;
        this.user_id = user_id;
        this.chip_type = chip_type;
        this.normalization = normalization;
        this.summarization = summarization;
        this.detail_output = detail_output;
    }
    */
    
    // Return a string representation of this object.
    @Override
    public String toString() {
        return job_id + " - " + pipeline_name + " - " + user_id 
                + " - " + submit_time;
    }
    
    // Return the user full name.
    public String getUserName() {
        return UserAccountDB.getFullName(user_id);
    }
    
    // Return the input data description.
    public String getInputDesc() {
        return InputDataDB.getInputDescription(study_id, input_sn);
    }
    
    // Return the submit_time in format "dd-MMM-yyyy hh:mmaa" for showing in
    // the finalization summary report.
    public String getSubmitTimeString() {
        return df.format(submit_time);
    }
    
    // Machine generated getters and setters.
    public int getJob_id() 
    { return job_id; }
    public void setJob_id(int job_id) 
    { this.job_id = job_id; }
    public int getInput_sn() 
    { return input_sn; }
    public void setInput_sn(int input_sn) 
    { this.input_sn = input_sn; }
    public String getStudy_id() 
    { return study_id; }
    public void setStudy_id(String study_id) 
    { this.study_id = study_id; }
    public String getTid() 
    { return tid; }
    public void setTid(String tid) 
    { this.tid = tid; }
    public String getPipeline_name() 
    { return pipeline_name; }
    public void setPipeline_name(String pipeline_name) 
    { this.pipeline_name = pipeline_name; }
    public Timestamp getSubmit_time() 
    { return submit_time; }
    public void setSubmit_time(Timestamp submit_time) 
    { this.submit_time = submit_time; }
    public String getUser_id() 
    { return user_id; }
    public void setUser_id(String user_id) 
    { this.user_id = user_id; }
    public String getChip_type() 
    { return chip_type; }
    public void setChip_type(String chip_type) 
    { this.chip_type = chip_type; }
    public String getNormalization() 
    { return normalization; }
    public void setNormalization(String normalization) 
    { this.normalization = normalization; }
    public String getSummarization() 
    { return summarization; }
    public void setSummarization(String summarization) 
    { this.summarization = summarization; }
    public String getDetail_output() 
    { return detail_output; }
    public void setDetail_output(String detail_output) 
    { this.detail_output = detail_output; }
}
