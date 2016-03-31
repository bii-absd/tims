/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Database;

import java.sql.Date;

/**
 * SubjectDetail is used to represent the subject_detail view in the database.
 * The objects from this class will be used to present the full detail of the
 * subjects in the metadatamanagement page.
 * 
 * Author: Tay Wei Hong
 * Date: 31-Mar-2016
 * 
 * Revision History
 * 31-Mar-2016 - Created with all the standard getters and setters.
 * 
 */

public class SubjectDetail {
    // subject_detail view attributes
    private String grp_id, study_id, subject_id, country_code, race, 
            subtype_code, remarks, event;
    private int age_at_diagnosis;
    private float height, weight;
    private Date event_date;
    private char gender;

    // Machine generated constructor.
    public SubjectDetail(String grp_id, String study_id, String subject_id, 
                         String country_code, String race, String subtype_code, 
                         String remarks, String event, int age_at_diagnosis, 
                         float height, float weight, Date event_date, char gender) 
    {
        this.grp_id = grp_id;
        this.study_id = study_id;
        this.subject_id = subject_id;
        this.country_code = country_code;
        this.race = race;
        this.subtype_code = subtype_code;
        this.remarks = remarks;
        this.event = event;
        this.age_at_diagnosis = age_at_diagnosis;
        this.height = height;
        this.weight = weight;
        this.event_date = event_date;
        this.gender = gender;
    }
    
    // Return the country name for this user nationality.
    public String getCountry_name() {
        return NationalityDB.getCountryName(country_code);
    }
    
    // Machine generated getters and setters.
    public String getGrp_id() {
        return grp_id;
    }
    public void setGrp_id(String grp_id) {
        this.grp_id = grp_id;
    }
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }
    public String getCountry_code() {
        return country_code;
    }
    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }
    public String getRace() {
        return race;
    }
    public void setRace(String race) {
        this.race = race;
    }
    public String getSubtype_code() {
        return subtype_code;
    }
    public void setSubtype_code(String subtype_code) {
        this.subtype_code = subtype_code;
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
    public int getAge_at_diagnosis() {
        return age_at_diagnosis;
    }
    public void setAge_at_diagnosis(int age_at_diagnosis) {
        this.age_at_diagnosis = age_at_diagnosis;
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
    public Date getEvent_date() {
        return event_date;
    }
    public void setEvent_date(Date event_date) {
        this.event_date = event_date;
    }
    public char getGender() {
        return gender;
    }
    public void setGender(char gender) {
        this.gender = gender;
    }
}
