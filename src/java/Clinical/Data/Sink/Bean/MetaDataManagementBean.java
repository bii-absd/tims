/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.ActivityLogDB;
import Clinical.Data.Sink.Database.NationalityDB;
import Clinical.Data.Sink.Database.Subject;
import Clinical.Data.Sink.Database.SubjectDB;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.sql.Date;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for primefaces
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.UploadedFile;

/**
 * MetaDataManagementBean is the backing bean for the metadatamanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 14-Dec-2015
 * 
 * Revision History
 * 14-Dec-2015 - Created with all the standard getters and setters. Added 3 new
 * methods insertMetaData(), metaDataUpload and onRowEdit.
 * 04-Jan-2016 - Added new method buildSubjectList(). Improved the method 
 * metaDataUpload.
 * 13-Jan-2016 - Removed all the static variables in Clinical Data Management
 * module.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 25-Feb-2016 - Implementation for database 3.0 (Part 2). Bug fix: To check
 * that the strings read in from the meta data uploaded represent valid integer
 * and float values.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 30-Mar-2016 - Changed the class name from ClinicalDataManagementBean to 
 * MetaDataManagementBean. Added the handling for 3 new attributes in subject
 * (i.e. remarks, event and event_date).
 */

@ManagedBean (name="MDMgntBean")
@ViewScoped
public class MetaDataManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MetaDataManagementBean.class.getName());
    private String subject_id, grp_id, country_code, race, remarks, event;
    private char gender;
    private int age_at_diagnosis;
    private float height, weight;
    private Date event_date;
    private java.util.Date util_event_date;
    private List<Subject> subjectList;
    // Store the user ID of the current user, and the study's Meta data that 
    // we are managing.
    private final String userName, study_id;
    private LinkedHashMap<String, String> nationalityCodeHash;
    
    public MetaDataManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        study_id = (String) getFacesContext().getExternalContext().
                getSessionMap().get("study_id");
        logger.debug("MetaDataManagementBean created.");
        logger.info(userName + ": access Meta Data Management for study " 
                    + study_id);
        
    }

    @PostConstruct
    public void init() {
        // The subject(s) uploaded or created will be park under the user's group.
        grp_id = UserAccountDB.getUnitID(userName);
        // Retrieve the list of nationality code setup in the system.
        nationalityCodeHash = NationalityDB.getNationalityCodeHash();
        buildSubjectList();
    }

    // Retrieve the subjects detail from database, and build the subject list.
    private void buildSubjectList() {
        try {
            subjectList = SubjectDB.getSubjectList(grp_id);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to build subject list!");
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "System failed to retrieve meta data from database!", ""));
        }
    }

    // Insert subject meta data into database.
    public String insertMetaData() {
        FacesContext fc = getFacesContext();
        
        if (util_event_date != null) {
            event_date = new Date(util_event_date.getTime());
        }
        
        Subject subject = new Subject(subject_id, age_at_diagnosis, gender,
                                      country_code, race, height, weight, 
                                      grp_id, remarks, event, event_date);
        
        if (SubjectDB.insertSubject(subject)) {
            // Record this subject meta data creation into database.
            String detail = "Subject " + subject_id;
            ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail);
            logger.info(userName + ": created " + detail); 
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Subject meta data inserted into database.", ""));
        }
        else {
            logger.error("FAIL to insert subject meta data!");
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Failed to insert subject meta data!", ""));
        }
        
        return Constants.META_DATA_MANAGEMENT;
    }
    
    // Upload the subject meta data.
    public String metaDataUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        StringBuilder uploadStatus = 
                new StringBuilder("Incorrect meta data at line(s): ");
        FacesMessage.Severity faceStatus = FacesMessage.SEVERITY_INFO;

        try (InputStream is = uFile.getInputstream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is));)
        {
            String lineRead;
            String[] data;
            int lineNum = 1;
            int incompleteLine = 0;

            while ((lineRead = br.readLine()) != null) {
                // Subject_ID|Age_at_diagnosis|gender|nationality|race|height|weight
                data = lineRead.split("\\|");
                // The system will only insert the complete meta data into database.
                if (data.length == 7) {
                    // By default, the nationality of all the subjects will be Singapore i.e. SGP.
                    String cc = "SGP";
                    if (!data[3].isEmpty()) {
                        cc = data[3];
                    }
                    // Need to make sure the strings represent valid integer and
                    // float values.
                    if (isInteger(data[1]) && isFloat(data[5]) && isFloat(data[6])) {
                        Subject tmp = new Subject(data[0], Integer.parseInt(data[1]), 
                                                  data[2].charAt(0), 
                                                  cc, data[4], 
                                                  Float.parseFloat(data[5]), 
                                                  Float.parseFloat(data[6]), 
                                                  grp_id, "", "", null);
                        
                        if (!SubjectDB.insertSubject(tmp)) {
                            uploadStatus.append(lineNum).append(" ");
                            incompleteLine++;
                        }
                    }
                    else {
                        // Some of the strings does not represent valid integer
                        // and/or float.
                        uploadStatus.append(lineNum).append(" ");
                        incompleteLine++;                        
                    }
                    
                }
                else {
                    uploadStatus.append(lineNum).append(" ");
                    incompleteLine++;
                }
                lineNum++;
            }
            
            if (incompleteLine == 0) {
                // All the meta data have complete info.
                uploadStatus.replace(0, uploadStatus.length()-1, 
                        "All meta data have been uploaded successfully.");
            }
            else {
                faceStatus = FacesMessage.SEVERITY_WARN;
            }
            // Record this subject meta data upload activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, 
                    uploadStatus.toString());
            logger.debug(uploadStatus);
        }
        catch (IOException ioe) {
            logger.debug("FAIL to upload meta data!");
            logger.debug(ioe.getMessage());
            uploadStatus.replace(0, uploadStatus.length()-1, 
                    "Failed to upload meta data!");
            faceStatus = FacesMessage.SEVERITY_ERROR;
        }
        
        // Display the growl message.
        getFacesContext().addMessage(null, new FacesMessage(
                    faceStatus, uploadStatus.toString(), ""));
        // Update the subject list.
        buildSubjectList();
        
        return Constants.META_DATA_MANAGEMENT;
    }
    
    // Update the subject meta data in database
    public void onRowEdit(RowEditEvent event) {
        FacesContext fc = getFacesContext();
        // Because the system is receiving the date as java.util.Date hence
        // we need to perform a conversion here before storing it into database.
        if (util_event_date != null) {
            ((Subject) event.getObject()).setEvent_date
                        (new Date(util_event_date.getTime()));
        }
        
        if (SubjectDB.updateSubject((Subject) event.getObject())) {
            // Record this subject meta data update into database.
            String detail = "Subject " + ((Subject) event.getObject()).
                            getSubject_id();
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail);
            logger.info(userName + ": updated " + detail);
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, "Meta data updated.", ""));
        }
        else {
            logger.error("FAIL to update meta data!");
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, "Failed to update meta data!", ""));
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Return true if the string represent a number, else return false.
    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        }
        catch (NumberFormatException nfe) {
            return false;
        }
    }
    
    // Return true if the string represent a float, else return false.
    private boolean isFloat(String str) {
        try {
            Float.parseFloat(str);
            return true;
        }
        catch (NumberFormatException nfe) {
            return false;
        }        
    }
    
    // Return the wording to be display at the link under the BreadCrumb in the
    // Clinical Data Management page.
    public String getBreadCrumbLink() {
        return "Meta Data Management for " + study_id;
    }
    
    // Machine generated getters and setters.
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }
    public int getAge_at_diagnosis() {
        return age_at_diagnosis;
    }
    public void setAge_at_diagnosis(int age_at_diagnosis) {
        this.age_at_diagnosis = age_at_diagnosis;
    }
    public char getGender() {
        return gender;
    }
    public void setGender(char gender) {
        this.gender = gender;
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
    public java.util.Date getUtil_event_date() {
        return util_event_date;
    }
    public void setUtil_event_date(java.util.Date util_event_date) {
        this.util_event_date = util_event_date;
    }
    
    // Return the list of meta data belonging to the same department ID as
    // the user.
    public List<Subject> getSubjectList() {
        return subjectList;
    }
    // Return the list of nationality code setup in the system.
    public LinkedHashMap<String, String> getNationalityCodeHash() {
        return nationalityCodeHash;
    }
}
