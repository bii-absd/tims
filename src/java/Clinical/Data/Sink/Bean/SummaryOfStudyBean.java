/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.Study;
import Clinical.Data.Sink.Database.StudyDB;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.General.FileLoader;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * SummaryOfStudyBean is the backing bean for the summaryofstudy view.
 * 
 * Author: Tay Wei Hong
 * Date: 06-Jan-2016
 * 
 * Revision History
 * 06-Jan-2016 - Implemented the module for downloading of study's consolidated 
 * output and finalized summary.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 */

@ManagedBean (name = "SOSBean")
@ViewScoped
public class SummaryOfStudyBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SummaryOfStudyBean.class.getName());
    private List<Study> completedStudy;
    // Store the user ID of the current user.
    private final String userName;

    public SummaryOfStudyBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        logger.debug("SummaryOfStudyBean created.");
        logger.info(userName + ": access Summary of Study page.");
    }
    
    @PostConstruct
    public void init() {
        // Retrieve the list of completed study that belong to the user's
        // department.
        completedStudy = StudyDB.queryCompletedStudy
                        (UserAccountDB.getDeptID(userName));        
    }
    
    // Download the consolidated output for this study.
    public void downloadFinalizedOP(Study study) {
        FileLoader.download(study.getFinalized_output());
    }
    
    // Download the finalized summary report for this study.
    public void downloadSummary(Study study) {
        FileLoader.download(study.getSummary());
    }
    
    // Return the list of completed Study that belong to the user's department.
    public List<Study> getCompletedStudy() {
        return completedStudy;
    }
}
