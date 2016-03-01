/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.ActivityLogDB;
import Clinical.Data.Sink.Database.DataVoid;
import Clinical.Data.Sink.Database.Study;
import Clinical.Data.Sink.Database.StudyDB;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
 */

@ManagedBean (name="unfinBean")
@ViewScoped
public class UnfinalizeStudyBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(UnfinalizeStudyBean.class.getName());
    private Study selectedStudy;
    // Store the list of finalized study.
    private List<Study> finalizedStudies = new ArrayList<>();
    // Store the user ID and dept ID of the current user.
    private final String userName, dept_id;

    public UnfinalizeStudyBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        dept_id = UserAccountDB.getDeptID(userName);
        logger.debug("FinalizeStudyBean created.");
        logger.info(userName + ": access Finalize Study page.");
    }
    
    @PostConstruct
    public void init() {
        // Get the list of finalized study from the user's department.
        finalizedStudies = StudyDB.queryFinalizedStudies(dept_id);
    }
    
    // User has selected the study and decided to proceed with the 
    // unfinalization.
    public String proceedWithUnfinalization() {
        // Record this unfinalization of study into database.
        ActivityLogDB.recordUserActivity(userName, Constants.EXE_UNFIN, 
                selectedStudy.getStudy_id());
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
        
        return Constants.MAIN_PAGE;
    }
    
    // Return the confirmation message for user to decide.
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
