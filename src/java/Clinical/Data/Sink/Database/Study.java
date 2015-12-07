/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import java.sql.Date;
import java.util.Calendar;

/**
 * Study is used to represent the study table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 07-Dec-2015
 * 
 * Revision History
 * 07-Dec-2015 - Created with all the standard getters and setters.
 */

public class Study {
    // study table attributes
    private String study_id, user_id, annot_ver, description;
    private Date sqlDate;
    private Boolean completed;

    public Study(String study_id, String user_id, String annot_ver, String description) {
        this.study_id = study_id;
        this.user_id = user_id;
        this.annot_ver = annot_ver;
        this.description = description;
        sqlDate = new Date(Calendar.getInstance().getTime().getTime());
        // The study is not completed until it has been finalized.
        completed = false;
        System.out.println("Study ID: " + study_id + " created on " + sqlDate);
    }
    
    // Machine generated getters and setters
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
    public Date getSqlDate() {
        return sqlDate;
    }
    public void setSqlDate(Date sqlDate) {
        this.sqlDate = sqlDate;
    }
    public Boolean getCompleted() {
        return completed;
    }
    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
