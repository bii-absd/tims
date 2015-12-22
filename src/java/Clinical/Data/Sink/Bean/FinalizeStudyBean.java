/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.StudyDB;
import java.io.Serializable;
import java.util.LinkedHashMap;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * FinalizeStudyBean is the backing bean for the finalizestudy view.
 * 
 * Author: Tay Wei Hong
 * Date: 22-Dec-2015
 * 
 * Revision History
 * 22-Dec-2015 - Created with all the standard getters and setters. 
 */

@ManagedBean (name="finalizedBean")
@ViewScoped
public class FinalizeStudyBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FinalizeStudyBean.class.getName());
    private String study_id;
    private LinkedHashMap<String, String> studyHash;
    
    public FinalizeStudyBean() {
        logger.debug("FinalizeStudyBean created.");
    }
    
    @PostConstruct
    public void init() {
        studyHash = StudyDB.getStudyHash(AuthenticationBean.getUserName());
    }
    
    // Return the study ID hash map for this user's department.
    public LinkedHashMap<String, String> getStudyHash() {
       return studyHash; 
    }
    
    // Proceed to select job(s) for finalization after a study has been selected.
    public Boolean getStudySelectedStatus() {
        return study_id != null;
    }
    
    // Machine generated getters and setters
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    
}
