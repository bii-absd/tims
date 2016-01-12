/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.Subject;
import Clinical.Data.Sink.Database.SubjectDB;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for primefaces
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.UploadedFile;

/**
 * ClinicalDataManagementBean is the backing bean for the 
 * clinicaldatamanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 14-Dec-2015
 * 
 * Revision History
 * 14-Dec-2015 - Created with all the standard getters and setters. Added 3 new
 * methods insertMetaData(), metaDataUpload and onRowEdit.
 * 04-Jan-2016 - Added new method buildSubjectList(). Improved the method 
 * metaDataUpload.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 */

@ManagedBean (name="clDataMgntBean")
@ViewScoped
public class ClinicalDataManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ClinicalDataManagementBean.class.getName());
    private String subject_id, dept_id, race;
    private char gender;
    private int age_at_diagnosis;
    private float height, weight;
    private List<Subject> subjectList;
    // Store the user ID of the current user.
    private final String userName;
    
    public ClinicalDataManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        logger.debug("ClinicalDataManagementBean created.");
        logger.info(userName + ": access Clinical Data Management Page.");
    }

    @PostConstruct
    public void init() {
        dept_id = UserAccountDB.getDeptID(userName);
        buildSubjectList();
    }

    // Retrieve the subjects detail from database, and build the subject list.
    private void buildSubjectList() {
        try {
            subjectList = SubjectDB.querySubject(dept_id);
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve subject detail!");
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "System failed to retrieve meta data from database!", ""));
        }
    }

    // Insert subject meta data into database.
    public String insertMetaData() {
        FacesContext fc = getFacesContext();
        Subject subject = new Subject(subject_id, age_at_diagnosis, gender,
                                      race, height, weight, dept_id);
        
        if (SubjectDB.insertSubject(subject)) {
            logger.info(userName + ": inserted new subject meta data: " 
                        + subject_id);
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
        
        return Constants.CLINICAL_DATA_MANAGEMENT;
    }
    
    // Upload the subject meta data.
    public String metaDataUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();

        try (InputStream is = uFile.getInputstream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is));)
        {
            String lineRead;
            String[] data;
            StringBuilder uploadStatus = 
                new StringBuilder("Incorrect meta data at line(s): ");
            int lineNum = 1;
            int incompleteLine = 0;
            FacesMessage.Severity faceStatus = FacesMessage.SEVERITY_INFO;

            while ((lineRead = br.readLine()) != null) {
                // Subject_ID|Age_at_diagnosis|gender|race|height|weight
                data = lineRead.split("\\|");
                // The system will only insert the complete meta data into database.
                if (data.length == 6) {
                    Subject tmp = new Subject(data[0], Integer.parseInt(data[1]), 
                                              data[2].charAt(0), data[3], 
                                              Float.parseFloat(data[4]), 
                                              Float.parseFloat(data[5]), 
                                              dept_id);
                    
                    if (!SubjectDB.insertSubject(tmp)) {
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
            
            logger.debug(uploadStatus);
            getFacesContext().addMessage(null, new FacesMessage(
                    faceStatus, uploadStatus.toString(), ""));
        }
        catch (IOException ioe) {
            logger.debug("FAIL to upload meta data!");
            logger.debug(ioe.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Failed to upload meta data!", ""));
        }
        
        // Update the subject list.
        buildSubjectList();
        
        return Constants.CLINICAL_DATA_MANAGEMENT;
    }
    
    // Update the subject meta data in database
    public void onRowEdit(RowEditEvent event) {
        FacesContext fc = getFacesContext();
        
        if (SubjectDB.updateSubject((Subject) event.getObject())) {
            logger.info(userName + ": updated subject meta data " + 
                    ((Subject) event.getObject()).getSubject_id());
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Meta data updated.", ""));
        }
        else {
            logger.error("FAIL to update meta data!");
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Failed to update meta data!", ""));
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Machine generated getters and setters.
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
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
    // Return the list of meta data belonging to the same department ID as
    // the user.
    public List<Subject> getSubjectList() {
        return subjectList;
    }    
}
