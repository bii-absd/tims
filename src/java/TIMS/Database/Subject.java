/*
 * Copyright @2015-2016
 */
package TIMS.Database;

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
 * 31-Mar-2016 - Only keep attributes subject_id, grp_id, country_code, race
 * and gender in this class. Move the rest to StudySubject Class.
 */

public class Subject {
    // subject table attributes
    private String subject_id, grp_id, country_code, race;
    private char gender;

    // Machine generated constructor
    public Subject(String subject_id, char gender, String country_code, 
            String race, String grp_id) {
        this.subject_id = subject_id;
        this.gender = gender;
        this.country_code = country_code;
        this.race = race;
        this.grp_id = grp_id;
    }
    
    
    // Return the string representation of this subject in the format of:
    // Subject_ID|Gender|Nationality|Race|GroupID
    @Override
    public String toString() {
        return subject_id + "|" + gender + "|" + country_code + "|" 
                + race +  "|" + grp_id;
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
}
