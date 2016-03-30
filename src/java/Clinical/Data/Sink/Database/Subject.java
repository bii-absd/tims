/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Database;

import java.sql.Date;

/**
 * Subject is used to represent the subject table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 10-Dec-2015
 * 
 * Revision History
 * 10-Dec-2015 - Created with all the standard getters and setters.
 * 14-Dec-2015 - Override the method toString() to return the string
 * representation of the subject meta data.
 * 25-Feb-2016 - Implementation for database 3.0 (Part 2).
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 30-Mar-2016 - Added 3 new attributes i.e. remarks, event and event_date.
 */

public class Subject {
    // subject table attributes
    private String subject_id, grp_id, country_code, race, remarks, event;
    private char gender;
    private int age_at_diagnosis;
    private float height, weight;
    private Date event_date;

    // Machine generated constructor
    public Subject(String subject_id, int age_at_diagnosis, char gender, 
            String country_code, String race, float height, float weight, 
            String grp_id, String remarks, String event, Date event_date) {
        this.subject_id = subject_id;
        this.age_at_diagnosis = age_at_diagnosis;
        this.gender = gender;
        this.country_code = country_code;
        this.race = race;
        this.height = height;
        this.weight = weight;
        this.grp_id = grp_id;
        this.remarks = remarks;
        this.event = event;
        this.event_date = event_date;
    }
    
    
    // Return the string representation of this subject in the format of:
    // Subject_ID|Age_at_diagnosis|Gender|Nationality|Race|Height|Weight|GroupID
    @Override
    public String toString() {
        return subject_id + "|" + age_at_diagnosis + "|" + gender + "|" +
               country_code + "|" + race + "|" + height + "|" + weight + 
               "|" + grp_id;
    }
    
    // Return the country name for this user nationality.
    public String getCountry_name() {
        return NationalityDB.getCountryName(country_code);
    }
    
    // Machine generated getters and setters
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }
    public String getGrp_id() {
        return grp_id;
    }
    public void setGrp_id(String grp_id) {
        this.grp_id = grp_id;
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
    public char getGender() {
        return gender;
    }
    public void setGender(char gender) {
        this.gender = gender;
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
    public Date getEvent_date() {
        return event_date;
    }
    public void setEvent_date(Date event_date) {
        this.event_date = event_date;
    }
}
