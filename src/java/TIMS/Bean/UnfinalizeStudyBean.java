/*
 * Copyright @2016-2018
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.DataVoid;
import TIMS.Database.Study;
import TIMS.Database.StudyDB;
import TIMS.Database.SubmittedJobDB;
import TIMS.Database.UserAccountDB;
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
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

/**
 * UnfinalizeStudyBean is the backing bean for the unfinalizestudy view.
 * 
 * Author: Tay Wei Hong
 * Date: 15-Feb-2016
 * 
 * Revision History
 * 15-Feb-2016 - Created with all the standard getters and setters. Implemented 
 * the module to unfinalize study.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 14-Dec-2016 - Do nothing for ad-hoc study.
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 */

//@ManagedBean (name="unfinBean")
@Named("unfinBean")
@ViewScoped
public class UnfinalizeStudyBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(UnfinalizeStudyBean.class.getName());
    private Study selectedStudy;
    // Store the full list of finalized study.
    private List<Study> finalizedStudies = new ArrayList<>();
    // Store the user ID and grp ID of the Admin.
    private final String userName, grp_id;

    public UnfinalizeStudyBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        grp_id = UserAccountDB.getUnitID(userName);
        logger.debug("FinalizeStudyBean created.");
        logger.info(userName + ": access Finalize Study page.");
    }
    
    @PostConstruct
    public void init() {
        // Get the full list of finalized study.
        finalizedStudies = StudyDB.queryAllFinalizedStudies();
    }
    
    // Admin has selected the study and decided to proceed with the 
    // unfinalization.
    public String proceedWithUnfinalization() {
        // Record this unfinalization of study into database.
        ActivityLogDB.recordUserActivity(userName, Constants.EXE_UNFIN, 
                selectedStudy.getStudy_id());
        
        if (SubmittedJobDB.getFinalizedJobIDs(selectedStudy.getStudy_id()).size() == 0) {
            // This is an ad-hoc study, exit without doing anything.
            logger.info(userName + " trying to unfinalize an ad-hoc study. Not allowed!");
        }
        else {
            try {
                DataVoid unfinThread = new DataVoid(userName, selectedStudy.getStudy_id());
                logger.info(userName + " begin unfinalization process for " + 
                            selectedStudy.getStudy_id());
        
                unfinThread.start();
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to begin unfinalization process for " + 
                             selectedStudy.getStudy_id());
                logger.error(e.getMessage());
            }
        }
        
        return Constants.MAIN_PAGE;
    }
    
    // Return the confirmation message for Admin to decide.
    public String getMessage() {
        if (selectedStudy != null) {
            return "Proceed to unfinalize " + 
                    selectedStudy.getStudy_id() + " ?";
        }
        else {
            return "Please select a study before proceeding.";
        }
    }
    
    // Return true if study has been selected.
    public Boolean getSelectedStudyStatus() {
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
