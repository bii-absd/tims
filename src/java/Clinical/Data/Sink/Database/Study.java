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
 * 23-Feb-2016 - Implementation for database 3.0 (Part 1).
 * 24-Feb-2016 - Added one new method, getReviewTitle() to return the title
 * for each study in the studies review page.
 * 01-Mar-2016 - Added one attribute, title.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 */

public class Study {
    // study table attributes
    private String study_id, title, owner_id, grp_id, annot_ver, description, 
                   background, grant_info, finalized_output, summary;
    private Date start_date, end_date;
    private Boolean finalized, closed;

    // This constructor is used when retrieving the study table for database.
    public Study(String study_id, String title, String grp_id, 
                 String annot_ver, String description, String background, 
                 String grant_info, String finalized_output, String summary, 
                 Date start_date, Date end_date, Boolean finalized, Boolean closed) 
    {
        this.study_id = study_id;
        this.title = title;
        this.grp_id = grp_id;
        this.annot_ver = annot_ver;
        this.description = description;
        this.background = background;
        this.grant_info = grant_info;
        this.finalized_output = finalized_output;
        this.summary = summary;
        this.start_date = start_date;
        this.end_date = end_date;
        this.finalized = finalized;
        this.closed = closed;
        this.owner_id = GroupDB.getGrpPIID(grp_id);
    }
    // This constructor is used for constructing new Study.
    // For every new Study created, the finalized_output and summary will be
    // empty, and closed status will be false (i.e. not closed).
    public Study(String study_id, String title, String grp_id, 
                 String annot_ver, String description, String background, 
                 String grant_info, Date start_date, Date end_date, Boolean finalized) 
    {
        this.study_id = study_id;
        this.title = title;
        this.grp_id = grp_id;
        this.annot_ver = annot_ver;
        this.description = description;
        this.background = background;
        this.grant_info = grant_info;
        finalized_output = summary = null;        
        this.start_date = start_date;
        this.end_date = end_date;
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

    // Return the owner full name for this study.
    public String getOwnerFullName() {
        return UserAccountDB.getFullName(owner_id);
    }
    
    // Return the Group name for this study.
    public String getGroupName() {
        return GroupDB.getGrpName(grp_id);
    }
    
    // Return the review title for each study.
    public String getReviewTitle() {
        String status;
        if (closed) {
            status = "Closed";
        }
        else {
            status = finalized?"Finalized":"Active";
        }
        
        return study_id + "  [PI: " + getOwnerFullName() + 
               "]  [Grant Duration: " + start_date + " to " + end_date + 
               "]  [Status: " + status + "]";
    }
    
    // Machine generated getters and setters
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getGrp_id() {
        return grp_id;
    }
    public void setGrp_id(String grp_id) {
        this.grp_id = grp_id;
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
    public String getBackground() {
        return background;
    }
    public void setBackground(String background) {
        this.background = background;
    }
    public String getGrant_info() {
        return grant_info;
    }
    public void setGrant_info(String grant_info) {
        this.grant_info = grant_info;
    }
    public Date getStart_date() {
        return start_date;
    }
    public void setStart_date(Date start_date) {
        this.start_date = start_date;
    }
    public Date getEnd_date() {
        return end_date;
    }
    public void setEnd_date(Date end_date) {
        this.end_date = end_date;
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
