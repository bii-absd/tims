/*
 * Copyright @2016-2019
 */
package TIMS.Database;

import TIMS.General.Constants;
import TIMS.General.FileHelper;
// Libraries for Java
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
// Library for Trove
import gnu.trove.map.hash.THashMap;

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
 * 13-Jul-2018 - Added two new methods, convertDataToHashMap and 
 * retrieveDataFromHashMap. Removed attributes remarks, event and event_date.
 * Added one new attribute age_at_baseline.
 * 08-Nov-2018 - Added one new method, getCoreData(data_name) to return the 
 * core data value for the data name passed in.
 * 04-Jan-2019 - Added one new attribute ssf_content to store the specific
 * field content of this subject; to be used in generating the specific field
 * report in the dashboard page.
 */

public class SubjectDetail {
    // subject_detail view attributes
    private final String study_id, subject_id;
    private String race, gender, height, weight, casecontrol, age_at_baseline;
    private LocalDate record_date, dob;
    private byte[] dat;
    private Map<String, String> data_hashmap;
    // Store the specific field content of this subject detail.
    private StringBuilder ssf_content;
    public final static String FIELD_BREAKER = "|";
    
    // Construct the SubjectDetail object directly using the result set returned
    // from the database query.
    public SubjectDetail(ResultSet rs) throws SQLException {
        this.study_id = rs.getString("study_id");
        this.subject_id = rs.getString("subject_id");
        this.race = rs.getString("race");
        this.gender = rs.getString("gender");
        this.height = rs.getString("height");
        this.weight = rs.getString("weight");
        this.record_date = (rs.getDate("record_date") != null)?
                            rs.getDate("record_date").toLocalDate():null;
        this.dob = rs.getDate("dob").toLocalDate();
        this.casecontrol = rs.getString("casecontrol");
        this.age_at_baseline = rs.getString("age_at_baseline");
        this.dat = rs.getBytes("dat");
        init();
    }
    
    // Machine generated constructor.
    public SubjectDetail(String study_id, String subject_id, String race, 
            String gender, String height, String weight, LocalDate record_date, 
            LocalDate dob, String casecontrol, String age_at_baseline) 
    {
        this.study_id = study_id;
        this.subject_id = subject_id;
        this.race = race;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.record_date = record_date;
        this.dob = dob;
        this.casecontrol = casecontrol;
        this.age_at_baseline = age_at_baseline;
        init();
    }

    private void init() {
        ssf_content = new StringBuilder(subject_id).append(FIELD_BREAKER);        
    }
    
    // Append this field to the specific field content of this subject.
    public void appendSpecificField(String field) {
        ssf_content.append(field).append(FIELD_BREAKER);
    }
    
    // Return the specific field content as a string.
    public String getSsf_content() {
        return ssf_content.toString();
    }

    // Convert the dat from byte[] to hashmap using the list of column name
    // passed in.
    public boolean convertDataToHashMap(List<String> colNameL) {
        boolean result = Constants.OK;
        data_hashmap = new THashMap<>(colNameL.size());
        List<String> datL = FileHelper.convertByteArrayToList(dat);
        Iterator<String> datItr = datL.iterator();
        
        for (String colName : colNameL) {
            if (datItr.hasNext()) {
                data_hashmap.put(colName, datItr.next());
            }
            else {
                // Something is wrong if the column name list is longer than
                // the data list.
                result = Constants.NOT_OK;
                break;
            }
        }
        
        return result;
    }
    
    // Retrieve the data value of this column from the hashmap.
    public String retrieveDataFromHashMap(String column) {
        if (data_hashmap.isEmpty()) {
            return null;
        }
        else {
            return data_hashmap.get(column);
        }
    }

    // Return the core data based on the data name passed in.
    // This method is used in dashboard module to support the chart of core
    // data vs specific field. Currently only race, casecontrol and gender are
    // supported.
    public String getCoreData(String data_name) {
	String core_data = "";
	switch(data_name) {
            case "race":
                core_data = race;
                break;
            case "casecontrol":
                core_data = casecontrol;
		break;
            case "gender":
		core_data = gender;
		break;
	}
        
//        return core_data.toUpperCase();
        return core_data;
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
    public String getHeight() 
    {   return height;  }
    public void setHeight(String height) 
    {   this.height = height;   }
    public String getWeight() 
    {   return weight;  }
    public void setWeight(String weight) 
    {   this.weight = weight;   }
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
    public String getAge_at_baseline() 
    {   return age_at_baseline; }
    public void setAge_at_baseline(String age_at_baseline) 
    {   this.age_at_baseline = age_at_baseline; }
    public byte[] getDat() 
    {   return dat; }
}
