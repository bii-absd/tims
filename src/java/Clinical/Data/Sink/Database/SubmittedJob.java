/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Database;

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
 * 30-Nov-2015 - Implementation for database 2.0
 * 05-Jan-2015 - Changes in submitted_job table, removed ctrl_file and annot_
 * file fields. Added input_path field.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 */

public class SubmittedJob implements Serializable {
    // Additional attributes (study_id, pipeline_name, chip_type, 
    // ctrl_file, normalization, probe_filtering, probe_select, 
    // phenotype_column, summarization, sample_average, standardization & 
    // region) added for DB 2.0
    private int job_id;
    private String study_id;
    private String user_id;
    private String pipeline_name;
    private int status_id;
    private String submit_time;
    private String chip_type, input_path, normalization, probe_filtering;
    private Boolean probe_select;
    private String phenotype_column, summarization;
    private String output_file;
    private String sample_average, standardization, region;
    private String report;
    // status_name will be used by the job status page
    private String status_name;

    // Full constructor
    public SubmittedJob(int job_id, String study_id, String user_id,
            String pipeline_name, int status_id, String submit_time, 
            String chip_type, String input_path, 
            String normalization, String probe_filtering, Boolean probe_select, 
            String phenotype_column, String summarization, String output_file, 
            String sample_average, String standardization, String region, 
            String report) 
    {
        this.job_id = job_id;
        this.study_id = study_id;
        this.user_id = user_id;
        this.pipeline_name = pipeline_name;
        this.status_id = status_id;
        this.submit_time = submit_time;
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
            String output_file, String report) 
    {
        this.job_id = job_id;
        this.study_id = study_id;
        this.user_id = user_id;
        this.pipeline_name = pipeline_name;
        this.status_id = status_id;
        this.submit_time = submit_time;
        this.output_file = output_file;
        this.report = report;
    }
    
    // Return the job status name of this submitted job.
    public String getStatus_name() {
        return JobStatusDB.getStatusName(status_id);
    }
    
    // Based on the job status, the download link at jobstatus.xhtml will be
    // enabled or disabled accordingly.
    public String getAvailable() {
        if (JobStatusDB.getStatusName(status_id).compareTo("Completed") == 0)
        {
            return Constants.FALSE;
        }
        else {
            return Constants.TRUE;
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
    public String getSubmit_time() {
        return submit_time;
    }
    public void setSubmit_time(String submit_time) {
        this.submit_time = submit_time;
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
    public String getSample_average() {
        return sample_average;
    }
    public void setSample_average(String sample_average) {
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
