/*
 * Copyright @2016-2018
 */
package TIMS.Database;

// Libraries for Java
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * 28-Apr-2017 - Change of attribute name from age_at_diagnosis to 
 * age_at_baseline. Add new constructor that build the object directly from
 * the result set returned from database query.
 * 29-May-2017 - Changes due to change in Subject table (i.e. age_at_baseline
 * changed to float type.)
 * 06-Apr-2018 - Database version 2.0 changes to support meta data upload
 * through Excel.
 * 15-May-2018 - Removed method getCaseControlInInt, because casecontrol will
 * be represented by case or control (instead of 0 and 1).
 */

public class SubjectDetail {
    // subject_detail view attributes
    private final String study_id, subject_id;
    private String race, gender, height, weight, remarks, event, casecontrol;
    private LocalDate event_date, record_date, dob;
    private byte[] dat;
    
    // Construct the SubjectDetail object directly using the result set returned
    // from the database query.
    public SubjectDetail(ResultSet rs) throws SQLException {
        this.study_id = rs.getString("study_id");
        this.subject_id = rs.getString("subject_id");
        this.race = rs.getString("race");
        this.gender = rs.getString("gender");
        this.remarks = rs.getString("remarks");
        this.event = rs.getString("event");
        this.height = rs.getString("height");
        this.weight = rs.getString("weight");
        this.event_date = (rs.getDate("event_date") != null)?
                           rs.getDate("event_date").toLocalDate():null;
        this.record_date = (rs.getDate("record_date") != null)?
                            rs.getDate("record_date").toLocalDate():null;
        this.dob = rs.getDate("dob").toLocalDate();
        this.casecontrol = rs.getString("casecontrol");
        this.dat = rs.getBytes("dat");
    }
    
    // Machine generated constructor.
    public SubjectDetail(String study_id, String subject_id, String race, 
            String gender, String remarks, String event, String height, 
            String weight, LocalDate event_date, LocalDate record_date, 
            LocalDate dob, String casecontrol) 
    {
        this.study_id = study_id;
        this.subject_id = subject_id;
        this.race = race;
        this.gender = gender;
        this.remarks = remarks;
        this.event = event;
        this.height = height;
        this.weight = weight;
        this.event_date = event_date;
        this.record_date = record_date;
        this.dob = dob;
        this.casecontrol = casecontrol;
    }

    // Machine generated getters and setters.
    public String getStudy_id() 
    {   return study_id;    }
    public String getSubject_id() 
    {   return subject_id;  }
    public String getRace() 
    {   return race;    }
    public void setRace(String race) 
    {   this.race = race;   }
    public String getGender() 
    {   return gender;  }
    public void setGender(String gender) 
    {   this.gender = gender;   }
    public String getRemarks() 
    {   return remarks; }
    public void setRemarks(String remarks) 
    {   this.remarks = remarks; }
    public String getEvent() 
    {   return event;   }
    public void setEvent(String event) 
    {   this.event = event; }
    public String getHeight() 
    {   return height;  }
    public void setHeight(String height) 
    {   this.height = height;   }
    public String getWeight() 
    {   return weight;  }
    public void setWeight(String weight) 
    {   this.weight = weight;   }
    public LocalDate getEvent_date() 
    {   return event_date;  }
    public void setEvent_date(LocalDate event_date) 
    {   this.event_date = event_date;   }
    public LocalDate getRecord_date() 
    {   return record_date; }
    public void setRecord_date(LocalDate record_date) 
    {   this.record_date = record_date; }
    public LocalDate getDob() 
    {   return dob; }
    public void setDob(LocalDate dob) 
    {   this.dob = dob; }
    public String getCasecontrol() 
    {   return casecontrol;    }
    public void setCasecontrol(String casecontrol) 
    {   this.casecontrol = casecontrol;   }
    public byte[] getDat() 
    {   return dat; }
}
