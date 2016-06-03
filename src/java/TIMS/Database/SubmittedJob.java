/*
 * Copyright @2015-2016
 */
package TIMS.Database;

import TIMS.General.Constants;
import TIMS.General.ResourceRetriever;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
 * 30-Nov-2015 - Implementation for database 2.0
 * 05-Jan-2015 - Changes in submitted_job table, removed ctrl_file and annot_
 * file fields. Added input_path field.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 14-Jan-2016 - To allow users to download the output and report from 
 * finalized job.
 * 18-Jan-2016 - Changed the type of variable sample_average from String to
 * Boolean.
 * 21-Jan-2016 - Added one new method, getPlDescription() to return the text 
 * description for this pipeline.
 * 03-Feb-2016 - Download link for report will also be enabled for failed job.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 24-Mar-2016 - Added one new attribute complete_time, to record the time when
 * the pipeline completed it's execution.
 * 29-Mar-2016 - Instead of storing the input path, the system will store the 
 * input SN.
 * 11-Apr-2016 - Changes due to the removal of attributes (sample_average, 
 * standardization, region and probe_select) from submitted_job table.
 * 12-Apr-2016 - Changes due to the removal of attributes (probe_filtering and
 * phenotype_column) from submitted_job table. Added 3 methods to check for the
 * availability of chip type, normalization and summarization info.
 * 14-Apr-2016 - Change type for submit_time and complete_time to Timestamp.
 * 19-May-2016 - Added one new attribute detail_output, to store the filepath
 * of the detail output file.
 */

public class SubmittedJob implements Serializable {
    // Additional attributes (study_id, pipeline_name, chip_type, 
    // ctrl_file, normalization, probe_filtering, probe_select, 
    // phenotype_column, summarization, sample_average, standardization & 
    // region) added for DB 2.0
    // For DB 3.0, removed input_path and added input_sn.
    // For DB 3.3, Removed sample_average, standardization, region, probe_select,
    // probe_filtering and phenotype_column.
    private int job_id, status_id, input_sn;
    private String study_id, user_id, pipeline_name, chip_type, normalization, 
                   summarization, output_file, detail_output, report;
    private Timestamp submit_time, complete_time;
    // status_name will be used by the job status page
    private String status_name;
    private final static DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mmaa");

    // Full constructor
    public SubmittedJob(int job_id, String study_id, String user_id,
            String pipeline_name, int status_id, Timestamp submit_time, 
            Timestamp complete_time, String chip_type, int input_sn, 
            String normalization, String summarization, String output_file, 
            String detail_output, String report) 
    {
        this.job_id = job_id;
        this.study_id = study_id;
        this.user_id = user_id;
        this.pipeline_name = pipeline_name;
        this.status_id = status_id;
        this.submit_time = submit_time;
        this.complete_time = complete_time;
        this.chip_type = chip_type;
        this.input_sn = input_sn;
        this.normalization = normalization;
        this.summarization = summarization;
        this.output_file = output_file;
        this.detail_output = detail_output;
        this.report = report;
    }
    
    // Simplify constructor for data table.
    public SubmittedJob(int job_id, String study_id, String user_id,
            String pipeline_name, int status_id, Timestamp submit_time, 
            Timestamp complete_time, String output_file, String report) 
    {
        this.job_id = job_id;
        this.study_id = study_id;
        this.user_id = user_id;
        this.pipeline_name = pipeline_name;
        this.status_id = status_id;
        this.submit_time = submit_time;
        this.complete_time = complete_time;
        this.output_file = output_file;
        this.report = report;
    }
    
    // Return the job status name of this submitted job.
    public String getStatus_name() {
        return JobStatusDB.getJobStatusName(status_id);
    }
    
    // Based on the job status, the download link for output at jobstatus.xhtml 
    // will be enabled or disabled accordingly.
    public String getOutputReady() {
        if ( (status_id == JobStatusDB.completed()) ||
             (status_id == JobStatusDB.finalized()) )
        {
            return Constants.FALSE;
        }
        else {
            return Constants.TRUE;
        }
    }
    // Download link for report will also be enabled for failed job.
    public String getReportReady() {
        if ( (status_id == JobStatusDB.completed()) || 
             (status_id == JobStatusDB.finalized()) ||
             (status_id == JobStatusDB.failed()) )
        {
            return Constants.FALSE;
        }
        else {
            return Constants.TRUE;
        }        
    }
    
    // Retrieve and return the text description for this pipeline from the 
    // resource bundle.
    public String getPlDescription() {
        return ResourceRetriever.getMsg(pipeline_name);
    }
    
    // Return the description of the input data used in this job.
    public String getInputDescrition() {
        return InputDataDB.getInputDescription(study_id, input_sn);
    }

    // Return true if chip type info is available.
    public boolean isTypeAvail() {
        return (chip_type.compareTo("NA") != 0);
    }
    // Return true if normalization info is available.
    public boolean isNormAvail() {
        return (normalization.compareTo("NA") != 0);
    }
    // Return true if summarization info is available.
    public boolean isSummAvail() {
        return (summarization.compareTo("NA") != 0);
    }

    // Return the submit_time and complete_time in format "dd-MMM-yyyy hh:mmaa"
    // for showing in pipeline job status page.
    public String getSubmitTimeString() {
        return df.format(submit_time);
    }
    public String getCompleteTimeString() {
        if (complete_time != null) {
            return df.format(complete_time);
        }
        else {
            return "In-progress";
        }
    }
    
    // Machine generated getters and setters
    public int getJob_id() {
        return job_id;
    }
    public void setJob_id(int job_id) {
        this.job_id = job_id;
    }
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getUser_id() {
        return user_id;
    }
    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
    public String getPipeline_name() {
        return pipeline_name;
    }
    public void setPipeline_name(String pipeline_name) {
        this.pipeline_name = pipeline_name;
    }
    public int getStatus_id() {
        return status_id;
    }
    public void setStatus_id(int status_id) {
        this.status_id = status_id;
    }
    public Timestamp getSubmit_time() {
        return submit_time;
    }
    public void setSubmit_time(Timestamp submit_time) {
        this.submit_time = submit_time;
    }
    public Timestamp getComplete_time() {
        return complete_time;
    }
    public void setComplete_time(Timestamp complete_time) {
        this.complete_time = complete_time;
    }
    public String getChip_type() {
        return chip_type;
    }
    public void setChip_type(String chip_type) {
        this.chip_type = chip_type;
    }
    public int getInput_sn() {
        return input_sn;
    }
    public void setInput_sn(int input_sn) {
        this.input_sn = input_sn;
    }
    public String getNormalization() {
        return normalization;
    }
    public void setNormalization(String normalization) {
        this.normalization = normalization;
    }
    public String getSummarization() {
        return summarization;
    }
    public void setSummarization(String summarization) {
        this.summarization = summarization;
    }
    public String getOutput_file() {
        return output_file;
    }
    public void setOutput_file(String output_file) {
        this.output_file = output_file;
    }
    public String getDetail_output() {
        return detail_output;
    }
    public void setDetail_output(String detail_output) {
        this.detail_output = detail_output;
    }
    public String getReport() {
        return report;
    }
    public void setReport(String report) {
        this.report = report;
    }
}