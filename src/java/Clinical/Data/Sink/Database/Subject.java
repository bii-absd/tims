/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

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
 */

public class Subject {
    // subject table attributes
    private String subject_id, dept_id, country_code, race;
    private char gender;
    private int age_at_diagnosis;
    private float height, weight;

    // Machine generated constructor
    public Subject(String subject_id, int age_at_diagnosis, char gender, 
            String country_code, String race, float height, float weight, 
            String dept_id) {
        this.subject_id = subject_id;
        this.age_at_diagnosis = age_at_diagnosis;
        this.gender = gender;
        this.country_code = country_code;
        this.race = race;
        this.height = height;
        this.weight = weight;
        this.dept_id = dept_id;
    }
    
    // Return the string representation of this subject in the format of:
    // Subject_ID|Age_at_diagnosis|Gender|Nationality|Race|Height|Weight|Department_ID
    @Override
    public String toString() {
        return subject_id + "|" + age_at_diagnosis + "|" + gender + "|" +
               country_code + "|" + race + "|" + height + "|" + weight + 
               "|" + dept_id;
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
    public String getDept_id() {
        return dept_id;
    }
    public void setDept_id(String dept_id) {
        this.dept_id = dept_id;
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
}
