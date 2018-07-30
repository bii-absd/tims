/*
 * Copyright @2016-2018
 */
package TIMS.Database;

import TIMS.General.FileHelper;
// Libraries for Java
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
 * 06-Apr-2018 - Database version 2.0 changes to support meta data upload
 * through Excel.
 * 13-Jul-2018 - Removed attributes event, event_date and remarks. Added one
 * new attribute sample_id.
 */

public class SubjectRecord {
    private String subject_id, study_id, height, weight, sample_id;
    private LocalDate record_date;
    private byte[] dat;
    
    // Construct the SubjectRecord object directly using the result set
    // returned from the database query.
    public SubjectRecord(ResultSet rs) throws SQLException {
        this.study_id = rs.getString("study_id");
        this.subject_id = rs.getString("subject_id");
        this.record_date = rs.getDate("record_date").toLocalDate();
        this.height = rs.getString("height");
        this.weight = rs.getString("weight");
        this.dat = rs.getBytes("dat");
        // Initialise to empty string for now.
        this.sample_id = "";
    }
    
    // Constructor used during Meta data upload to insert new subject record.
    public SubjectRecord(String study_id, String subject_id, 
                         LocalDate record_date, String height, 
                         String weight, byte[] dat) {
        this.study_id = study_id;
        this.subject_id = subject_id;
        this.record_date = record_date;
        this.height = height;
        this.weight = weight;
        this.dat = dat;
        // Initialise to empty string for now.
        this.sample_id = "";
    }
    
    // Machine generated constructor
    public SubjectRecord(String subject_id, String study_id, LocalDate record_date,
                        String height, String weight, String sample_id, byte[] dat) {
        this.subject_id = subject_id;
        this.study_id = study_id;
        this.record_date = record_date;
        this.height = height;
        this.weight = weight;
        this.sample_id = sample_id;
        this.dat = dat;
    }
    
    // Return the list of data column value.
    public List<String> getDataValueList() {
        List<String> valueList = new ArrayList<>();
        
        if (dat != null) {
            valueList = FileHelper.convertByteArrayToList(dat);
        }
        
        return valueList;
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
    public String getHeight() {
        return height;
    }
    public void setHeight(String height) {
        this.height = height;
    }
    public String getWeight() {
        return weight;
    }
    public void setWeight(String weight) {
        this.weight = weight;
    }
    public String getSample_id() {
        return sample_id;
    }
    public void setSample_id(String sample_id) {
        this.sample_id = sample_id;
    }
}
