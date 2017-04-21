/*
 * Copyright @2016-2017
 */
package TIMS.Database;

import java.time.LocalDate;

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
 * 18-Apr-2017 - Removed grp_id, and added record_date.
 */

public class SubjectDetail {
    // subject_detail view attributes
    private String study_id, subject_id, country_code, race, subtype_code, 
                   remarks, event;
    private int age_at_diagnosis;
    private float height, weight;
    private LocalDate event_date, record_date;
    private char gender;

    // Machine generated constructor.
    
    public SubjectDetail(String study_id, String subject_id, LocalDate record_date,
                         String country_code, String race, String subtype_code, 
                         String remarks, String event, int age_at_diagnosis, 
                         float height, float weight, LocalDate event_date, char gender) 
    {
        this.study_id = study_id;
        this.subject_id = subject_id;
        this.record_date = record_date;
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
    public LocalDate getRecord_date() {
        return record_date;
    }
    public void setRecord_date(LocalDate record_date) {
        this.record_date = record_date;
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
    public LocalDate getEvent_date() {
        return event_date;
    }
    public void setEvent_date(LocalDate event_date) {
        this.event_date = event_date;
    }
    public char getGender() {
        return gender;
    }
    public void setGender(char gender) {
        this.gender = gender;
    }
}
