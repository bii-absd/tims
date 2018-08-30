/*
 * Copyright @2016-2018
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.Study;
import TIMS.Database.StudyDB;
import TIMS.Database.VaultKeeper;
import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.omnifaces.cdi.ViewScoped;

/**
 * CloseStudyBean is the backing bean for the closestudy view.
 * 
 * Author: Tay Wei Hong
 * Date: 14-Mar-2016
 * 
 * Revision History
 * 14-Mar-2016 - Created with all the standard getters and setters. Implemented 
 * the module to close study.
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 */

@Named("clstudyBean")
//@ManagedBean (name="clstudyBean")
@ViewScoped
public class CloseStudyBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(CloseStudyBean.class.getName());
    private Study selectedStudy;
    // Store the list of finalized study.
    private List<Study> finalizedStudies = new ArrayList<>();
    // Store the user IS of the Admin.
    private final String userName;
    
    public CloseStudyBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        logger.info(userName + ": access Close Study page.");
    }
    
    @PostConstruct
    public void init() {
        // Get the list of finalized study.
        finalizedStudies = StudyDB.queryAllFinalizedStudies();
    }
    
    // Admin has selected the study and decided to close the study.
    public String proceedWithClosure() {
        String study_id = selectedStudy.getStudy_id();
        // Record this activity into database.
        ActivityLogDB.recordUserActivity(userName, Constants.EXE_CLSTUDY, 
                                         study_id);
        
        try {
            VaultKeeper clStudyThread = new VaultKeeper(userName, study_id);
            logger.debug(userName + " begin the closure process for " + study_id);
            // Update study status to closed.
            StudyDB.updateStudyClosedStatus(study_id, true);
            // Start the closure of the study.
            clStudyThread.start();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to close study " + study_id);
            logger.error(e.getMessage());
        }

        return Constants.MAIN_PAGE;
    }
    
    // Return the confirmation message for Admin to decide the next action.
    public String getDlgMsg() {
        if (selectedStudy != null) {
            return "Proceed to close " + selectedStudy.getStudy_id() + "?";
        }
        else {
            return "Please select a study before proceeding.";
        }
    }
    
    // Return true if study has been selected.
    public boolean getSelectedStudyStatus() {
        return (selectedStudy != null);
    }
    
    // Machine generated getters and setters.
    public Study getSelectedStudy() {
        return selectedStudy;
    }
    public void setSelectedStudy(Study selectedStudy) {
        this.selectedStudy = selectedStudy;
    }
    public List<Study> getFinalizedStudies() {
        return finalizedStudies;
    }
}
