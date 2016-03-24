/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.General.ResourceRetriever;
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
 */

public class SubmittedJob implements Serializable {
    // Additional attributes (study_id, pipeline_name, chip_type, 
    // ctrl_file, normalization, probe_filtering, probe_select, 
    // phenotype_column, summarization, sample_average, standardization & 
    // region) added for DB 2.0
    private int job_id, status_id;
    private String study_id, user_id, pipeline_name, submit_time, complete_time;
    private String chip_type, input_path, normalization, probe_filtering;
    private Boolean probe_select, sample_average;
    private String phenotype_column, summarization, output_file;
    private String standardization, region, report;
    // status_name will be used by the job status page
    private String status_name;

    // Full constructor
    public SubmittedJob(int job_id, String study_id, String user_id,
            String pipeline_name, int status_id, String submit_time, 
            String complete_time, String chip_type, String input_path, 
            String normalization, String probe_filtering, Boolean probe_select, 
            String phenotype_column, String summarization, String output_file, 
            Boolean sample_average, String standardization, String region, 
            String report) 
    {
        this.job_id = job_id;
        this.study_id = study_id;
        this.user_id = user_id;
        this.pipeline_name = pipeline_name;
        this.status_id = status_id;
        this.submit_time = submit_time;
        this.complete_time = complete_time;
        this.chip_type = chip_type;
        this.input_path = input_path;
        this.normalization = normalization;
        this.probe_filtering = probe_filtering;
        this.probe_select = probe_select;
        this.phenotype_column = phenotype_column;
        this.summarization = summarization;
        this.output_file = output_file;
        this.sample_average = sample_average;
        this.standardization = standardization;
        this.region = region;
        this.report = report;
    }
    
    // Simplify constructor for data table.
    public SubmittedJob(int job_id, String study_id, String user_id,
            String pipeline_name, int status_id, String submit_time, 
            String complete_time, String output_file, String report) 
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
    public String getSubmit_time() {
        return submit_time;
    }
    public void setSubmit_time(String submit_time) {
        this.submit_time = submit_time;
    }
    public String getComplete_time() {
        return complete_time;
    }
    public void setComplete_time(String complete_time) {
        this.complete_time = complete_time;
    }
    public String getChip_type() {
        return chip_type;
    }
    public void setChip_type(String chip_type) {
        this.chip_type = chip_type;
    }
    public String getInput_path() {
        return input_path;
    }
    public void setInput_path(String input_path) {
        this.input_path = input_path;
    }
    public String getNormalization() {
        return normalization;
    }
    public void setNormalization(String normalization) {
        this.normalization = normalization;
    }
    public String getProbe_filtering() {
        return probe_filtering;
    }
    public void setProbe_filtering(String probe_filtering) {
        this.probe_filtering = probe_filtering;
    }
    public Boolean getProbe_select() {
        return probe_select;
    }
    public void setProbe_select(Boolean probe_select) {
        this.probe_select = probe_select;
    }
    public String getPhenotype_column() {
        return phenotype_column;
    }
    public void setPhenotype_column(String phenotype_column) {
        this.phenotype_column = phenotype_column;
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
    public Boolean getSample_average() {
        return sample_average;
    }
    public void setSample_average(Boolean sample_average) {
        this.sample_average = sample_average;
    }
    public String getStandardization() {
        return standardization;
    }
    public void setStandardization(String standardization) {
        this.standardization = standardization;
    }
    public String getRegion() {
        return region;
    }
    public void setRegion(String region) {
        this.region = region;
    }
    public String getReport() {
        return report;
    }
    public void setReport(String report) {
        this.report = report;
    }
}
