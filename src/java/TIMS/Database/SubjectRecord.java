/*
 * Copyright @2016-2017
 */
package TIMS.Database;

import java.time.LocalDate;

/**
 * SubjectRecord is used to represent the subject_record table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 31-Mar-2016
 * 
 * Revision History
 * 31-Mar-2016 - Created with all the standard getters and setters.
 * 19-Apr-2017 - Rename to SubjectRecord. Removed grp_id, age_at_diagnosis and 
 * subtype_code, added record_date.
 */

public class SubjectRecord {
    // study_subject table attributes
    private String subject_id, study_id, remarks, event;
    private float height, weight;
    private LocalDate event_date, record_date;
    
    // Machine generated constructor
    public SubjectRecord(String subject_id, String study_id, LocalDate record_date,
                        String remarks, String event, float height, 
                        float weight, LocalDate event_date) {
        this.subject_id = subject_id;
        this.study_id = study_id;
        this.record_date = record_date;
        this.remarks = remarks;
        this.event = event;
        this.height = height;
        this.weight = weight;
        this.event_date = event_date;
    }
    
    // Machine generated getters and setters.
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public LocalDate getRecord_date() {
        return record_date;
    }
    public void setRecord_date(LocalDate record_date) {
        this.record_date = record_date;
    }
    public String getRemarks() {
        return remarks;
    }
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    public String getEvent() {
        return event;
    }
    public void setEvent(String event) {
        this.event = event;
    }
    public float getHeight() {
        return height;
    }
    public void setHeight(float height) {
        this.height = height;
    }
    public float getWeight() {
        return weight;
    }
    public void setWeight(float weight) {
        this.weight = weight;
    }
    public LocalDate getEvent_date() {
        return event_date;
    }
    public void setEvent_date(LocalDate event_date) {
        this.event_date = event_date;
    }
}
