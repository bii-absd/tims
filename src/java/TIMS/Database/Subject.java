/*
 * Copyright @2015-2018
 */
package TIMS.Database;

// Libraries for Java
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

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
 * 18-Apr-2017 - Removed grp_id, and added study_id, subtype_code and
 * age_at_baseline.
 * 29-May-2017 - Variable age_at_baseline changed from integer to float type.
 * 06-Apr-2018 - Database version 2.0 changes to support meta data upload
 * through Excel.
 * 15-May-2018 - Removed method getCaseControlInInt, because casecontrol will
 * be represented by case or control (instead of 0 and 1).
 */

public class Subject {
    private String subject_id, study_id, race, gender, casecontrol;
    private LocalDate dob;

    // Construct the Subject object directly using the result set returned 
    // from the database query.
    public Subject(ResultSet rs) throws SQLException {
        this.subject_id = rs.getString("subject_id");
        this.study_id = rs.getString("study_id");
        this.race = rs.getString("race");
        this.gender = rs.getString("gender");
        this.dob = rs.getDate("dob").toLocalDate();
        this.casecontrol = rs.getString("casecontrol");
        
    }
    
    // Machine generated constructor
    public Subject(String subject_id, String study_id, String race, 
            String gender, LocalDate dob, String casecontrol) {
        this.subject_id = subject_id;
        this.study_id = study_id;
        this.race = race;
        this.gender = gender;
        this.dob = dob;
        this.casecontrol = casecontrol;
    }

    // Return the string representation of this subject in the format of:
    // Study_ID|Subject_ID|Race|Gender|DOB
    @Override
    public String toString() {
        return study_id + "|" + subject_id + "|" + race + "|" + gender 
                + "|" + dob;
    }
    
    // Machine generated getters and setters
    public String getSubject_id() 
    {   return subject_id;  }
    public String getStudy_id() 
    {   return study_id;    }
    public String getRace() 
    {   return race;    }
    public void setRace(String race) 
    {   this.race = race;   }
    public String getGender() 
    {   return gender;  }
    public void setGender(String gender) 
    {   this.gender = gender;   }
    public LocalDate getDob() 
    {   return dob; }
    public void setDob(LocalDate dob) 
    {   this.dob = dob; }
    public String getCasecontrol() 
    {   return casecontrol;    }
    public void setCasecontrol(String casecontrol) 
    {   this.casecontrol = casecontrol;   }
}
