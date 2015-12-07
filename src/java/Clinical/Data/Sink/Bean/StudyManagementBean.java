/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.Study;
import Clinical.Data.Sink.Database.StudyDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.sql.Date;
import java.util.Calendar;
import java.util.LinkedHashMap;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * StudyManagementBean is the backing bean for the studymanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 07-Dec-2015
 * 
 * Revision History
 * 07-Dec-2015 - Created with all the standard getters and setters, plus two
 * static methods createNewStudy() and getAnnotList().
 */

@ManagedBean (name="studyMgntBean")
@ViewScoped
public class StudyManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudyManagementBean.class.getName());
    // Attributes for Study object
    private String study_id, user_id, annot_ver, description;
    private Date sqlDate;
    private Boolean completed;
    private LinkedHashMap<String, String> annotList = new LinkedHashMap<>();
    
    public StudyManagementBean() {
        user_id = AuthenticationBean.getUserName();
        sqlDate = new Date(Calendar.getInstance().getTime().getTime());
        logger.debug("StudyManagementBean created.");
        logger.debug(user_id + ": access Study ID Management page.");
    }
    
    @PostConstruct
    public void init() {
        annotList = StudyDB.getAnnotHashMap();
    }
    
    // Create new Study
    public String createNewStudy() {
        FacesContext fc = getFacesContext();
        Study study = new Study(study_id, user_id, annot_ver, description, sqlDate);
        
        if (StudyDB.insertStudy(study)) {
            logger.info(user_id + ": created new Study ID: " + study_id);
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "New Study ID created.", ""));
        }
        else {
            logger.info("Failed to create new Study ID: " + study_id);
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "Failed to create new Study ID!", ""));
        }
        
        return Constants.STUDY_MANAGEMENT;
    }
    
    // Return the list of Annotation Version setup in the system.
    public LinkedHashMap<String, String> getAnnotList() {
        return annotList;
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }

    // Machine generated getters and setters
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getUser_id() {
        return user_id;
    }
    public String getAnnot_ver() {
        return annot_ver;
    }
    public void setAnnot_ver(String annot_ver) {
        this.annot_ver = annot_ver;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Date getSqlDate() {
        return sqlDate;
    }
    public Boolean getCompleted() {
        return completed;
    }
}
