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
 */

public class Subject {
    // subject table attributes
    private String subject_id, dept_id, race;
    private char gender;
    private int age_at_diagnosis;
    private float height, weight;

    // Machine generated constructor
    public Subject(String subject_id, String dept_id, String race, char gender, 
            int age_at_diagnosis, float height, float weight) {
        this.subject_id = subject_id;
        this.dept_id = dept_id;
        this.race = race;
        this.gender = gender;
        this.age_at_diagnosis = age_at_diagnosis;
        this.height = height;
        this.weight = weight;
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
