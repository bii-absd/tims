/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.io.File;
import java.sql.Date;

/**
 * Study is used to represent the study table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 07-Dec-2015
 * 
 * Revision History
 * 07-Dec-2015 - Created with all the standard getters and setters.
 * 09-Dec-2015 - Added one attribute, dept_id.
 * 06-Jan-2016 - Added two attributes, finalized_output and summary.
 * 11-Jan-2016 - Added methods to disable/enable the download links for
 * output and summary.
 * 14-Jan-2016 - Bug Fix: To make sure the finalized_output / summary is not
 * null before checking whether the file is ready for download.
 * 20-Jan-2016 - Updated study table in database; added one new variable closed, 
 * and renamed completed to finalized.
 */

public class Study {
    // study table attributes
    private String study_id, dept_id, user_id, annot_ver, 
                   description, finalized_output, summary;
    private Date sqlDate;
    private Boolean finalized, closed;

    // This constructor is used when retrieving the study table for database.
    public Study(String study_id, String dept_id, String user_id, 
            String annot_ver, String description, String finalized_output,
            String summary, Date sqlDate, Boolean finalized, Boolean closed) {
        this.study_id = study_id;
        this.dept_id = dept_id;
        this.user_id = user_id;
        this.annot_ver = annot_ver;
        this.description = description;
        this.finalized_output = finalized_output;
        this.summary = summary;
        this.sqlDate = sqlDate;
        this.finalized = finalized;
        this.closed = closed;
    }
    // This constructor is used for constructing new Study.
    // For every new Study created, the finalized_output and summary will be
    // empty, and closed status will be false (i.e. not closed).
    public Study(String study_id, String dept_id, String user_id, 
            String annot_ver, String description, Date sqlDate, 
            Boolean finalized) {
        this.study_id = study_id;
        this.dept_id = dept_id;
        this.user_id = user_id;
        this.annot_ver = annot_ver;
        this.description = description;
        finalized_output = summary = null;
        this.sqlDate = sqlDate;
        this.finalized = finalized;
        closed = false;
    }
    
    // If finalized output is ready for download, don't disable the link (i.e.
    // return false).
    public Boolean getOutputReadyStatus() {
        if (finalized_output != null) {
            return !checkFileReady(finalized_output);            
        }
        else {
            // Finalized output is not ready for download.
            return Constants.OK;
        }
    }
    
    // If summary is ready for download, don't disable the link (i.e. return
    // false).
    public Boolean getSummaryReadyStatus() {
        if (summary != null) {
            return !checkFileReady(summary);            
        }
        else {
            // Summary is not ready for download.
            return Constants.OK;
        }
    }
    
    // Check whether the file is ready for download by checking whether it 
    // exist or not.
    private Boolean checkFileReady(String filename) {
        File file = new File(filename);
        
        return file.exists()?Constants.OK:Constants.NOT_OK;
    }
    
    // Machine generated getters and setters
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getDept_id() {
        return dept_id;
    }
    public void setDept_id(String dept_id) {
        this.dept_id = dept_id;
    }
    public String getUser_id() {
        return user_id;
    }
    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
    public String getAnnot_ver() {
        return annot_ver;
    }
    public void setAnnot_ver(String annot_ver) {
        this.annot_ver = annot_ver;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getFinalized_output() {
        return finalized_output;
    }
    public void setFinalized_output(String finalized_output) {
        this.finalized_output = finalized_output;
    }
    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }
    public Date getSqlDate() {
        return sqlDate;
    }
    public void setSqlDate(Date sqlDate) {
        this.sqlDate = sqlDate;
    }
    public Boolean getFinalized() {
        return finalized;
    }
    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }
    public Boolean getClosed() {
        return closed;
    }
    public void setClosed(Boolean closed) {
        this.closed = closed;
    }
}
