/*
 * Copyright @2016
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.Study;
import TIMS.Database.StudyDB;
import TIMS.Database.UserAccount;
import TIMS.Database.UserAccountDB;
import TIMS.General.Constants;
import TIMS.General.FileHelper;
import TIMS.General.QueryStringGenerator;
import java.io.Serializable;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * StudiesReviewBean is the backing bean for the studiesreview and 
 * completedstudyoutput view.
 * 
 * Author: Tay Wei Hong
 * Date: 06-Jan-2016
 * 
 * Revision History
 * 06-Jan-2016 - Implemented the module for downloading of study's consolidated 
 * output and finalized summary.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 20-Jan-2016 - Updated study table in database; added one new variable closed, 
 * and renamed completed to finalized.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 24-Feb-2016 - Implemented studies review module.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 19-May-2016 - To allow user to download the detail output file.
 * 22-Jun-2016 - Updated init() to use the query string generation function 
 * in QueryStringGenerator.
 */

@ManagedBean (name = "StudiesBean")
@ViewScoped
public class StudiesReviewBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudiesReviewBean.class.getName());
    private List<Study> finalizedStudies;
    private List<Study> studiesReview;
    // Store the user ID of the current user.
    private final String userName;
    private final UserAccount user;

    public StudiesReviewBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        user = UserAccountDB.getUserAct(userName);
        logger.debug("StudiesBean created.");
        logger.info(userName + ": access Studies Review page.");
    }
    
    @PostConstruct
    public void init() {
        // For studiesReview: retrieve the list of studies that this user is 
        // allowed to review based on his role.
        String groupQuery = QueryStringGenerator.genGrpQuery4Review(user);
        studiesReview = StudyDB.queryStudies(groupQuery);
        
        // For finalizedStudies: retrieve the list of finalized studies that 
        // this user is allowed to access.
        switch (user.getRoleName()) {
            case "Director":
            case "HOD":
            case "PI":
                finalizedStudies = StudyDB.queryFinalizedStudiesByGrps(userName);
                break;
            case "Admin":
            case "User":
            default:
                finalizedStudies = StudyDB.queryFinalizedStudiesByGrp
                                   (user.getUnit_id());
                break;
        }
    }
    
    // Download the consolidated output for this study.
    public void downloadFinalizedOP(Study study) {
        String detail = "Finalized output " + study.getFinalized_output();
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, detail);
        FileHelper.download(study.getFinalized_output());
    }
    // Download the detail output for this study.
    public void downloadDetailOutput(Study study) {
        String detail = "Detail output " + study.getDetail_files();
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, detail);
        FileHelper.download(study.getDetail_files());
    }
    // Download the finalized summary report for this study.
    public void downloadSummary(Study study) {
        String detail = "Summary report " + study.getSummary();
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, detail);
        FileHelper.download(study.getSummary());
    }
    
    // Machine generated getters.
    public List<Study> getFinalizedStudies() {
        return finalizedStudies;
    }
    public List<Study> getStudiesReview() {
        return studiesReview;
    }
}
