/*
 * Copyright @2015-2017
 */
package TIMS.Bean;

import TIMS.Database.PipelineDB;
import TIMS.Database.StudyDB;
import TIMS.Database.UserAccountDB;
import TIMS.Database.UserRoleDB;
import TIMS.General.Constants;
import java.io.Serializable;
import java.util.LinkedHashMap;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * MenuSelectionBean is used as the backing bean for the main view.
 * 
 * Author: Tay Wei Hong
 * Date: 23-Oct-2015
 * 
 * Revision History
 * 23-Oct-2015 - Created with all the standard getters and setters.
 * 27-Oct-2015 - Added 2 functions (gexPipeline and ngsPipeline) that will help
 * to setup the pipeline backing bean.
 * 28-Oct-2015 - Split the gexPipeline function into 2 new functions, 
 * gexIllumina and gexAffymetrix.
 * 13-Nov-2015 - Changes to methods, gexIllumina() and gexAffymetrix() after 
 * refactoring ArrayConfigBean.
 * 25-Nov-2015 - Implementation for database 2.0
 * 15-Dec-2015 - Changed from RequestScoped to ViewScoped. Removed param 
 * command. Implemented the new workflow (i.e. User to select Study ID before 
 * proceeding to pipeline configuration. To construct the Study List during
 * PostConstruct phase.
 * 22-Dec-2015 - Added one attribute haveNewData, to indicate whether user
 * have new data to upload or not.
 * 30-Dec-2015 - Updated proceedToConfig method to use the method setup from
 * ConfigBean to setup the pipeline configuration.
 * 11-Jan-2016 - Added the support for METH pipeline.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 14-Jan-2016 - Removed all the static variables in Pipeline Configuration
 * Management module.
 * 19-Jan-2016 - Added the support for CNV pipeline.
 * 20-Jan-2016 - To streamline the navigation flow and passing of pipeline name
 * from main menu to pipeline configuration pages.
 * 01-Feb-2016 - When retrieving submitted jobs, there are now 2 options 
 * available i.e. to retrieve for single user or all users (enable for 
 * administrator only).
 * 18-Feb-2016 - Moved method userJobStatus() to AuthenticationBean.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 30-Mar-2016 - Added a dialog for user to select the Study; to manage it's 
 * Meta data.
 * 04-Apr-2016 - Only 'opened' studies (i.e. not finalized) will be available 
 * for user selection when managing subject Meta data.
 * 07-Apr-2016 - Only build the study and open study list when the user click
 * on the pipeline or subject meta data management link.
 * 12-Apr-2016 - The Study ID list for administrator will be the full list of
 * unclosed Study ID; to be selected for raw data uploading.
 * 19-Apr-2016 - Bug Fix: When the user entered Main Menu page, the Single User
 * Mode should be set to True.
 * 25-Aug-2016 - Added 2 new methods, setupRawDataMgnt() and 
 * proceedToRawDataMgnt() to support Raw Data Management module Part I.
 * 13-Dec-2016 - Raw data management should be allowed for finalized study 
 * (i.e. ad-hoc study).
 * 24-Apr-2017 - Meta data management will be allowed for all studies that are
 * not closed.
 * 13-Jul-2017 - Changes due to the addition of GATK Sequencing Pipelines.
 */

@ManagedBean (name="menuSelBean")
@ViewScoped
public class MenuSelectionBean implements Serializable{
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MenuSelectionBean.class.getName());
    private String study_id, plConfigPageURL, plName;
    private Boolean haveNewData;
    private LinkedHashMap<String, String> studyList, pipelineList;
    // Store the user ID of the current user.
    private final String userName;
    private final int roleID;
    
    public MenuSelectionBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        roleID = UserAccountDB.getRoleID(userName);
        // Set the single user mode to true when the user enter the main page.
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("singleUser", true);
        logger.debug("MenuSelectionBean created.");
    }
    
    @PostConstruct
    public void init() {
        // Do nothing for now.
    }
    
    // Setup the URL for pipeline config page, and the Study ID list for user 
    // selection.
    public void setupPlConfigPageURL() {
        String nextPage = plName;
        
        if (UserRoleDB.isLead(roleID)) {
            studyList = StudyDB.getPIStudyHash(userName);
        }
        else if (roleID == UserRoleDB.admin()) {
            studyList = StudyDB.getAllStudyHash();
        }
        else {
            studyList = StudyDB.getUserStudyHash(userName);
        }
        // For GATK Sequencing pipelines, whole-genome pipelines will share
        // the same bean and xhtml (the same goes for targeted pipelines.)
        if (plName.equals(PipelineDB.GATK_WG_GERM) || 
            plName.equals(PipelineDB.GATK_WG_SOMA)) {
            nextPage = "gatk-whole-genome-seq";
        }
        else if (plName.equals(PipelineDB.GATK_TAR_GERM) || 
                plName.equals(PipelineDB.GATK_TAR_SOMA)) {
            nextPage = "gatk-targeted-seq";
        }
        
        plConfigPageURL = nextPage + "?faces-redirect=true";
    }
    
    // Setup the hash maps needed for raw data management.
    public void setupRawDataMgnt() {
        setupStudyList();
        pipelineList = PipelineDB.getEditablePlHash();
    }

    // Setup the Study ID list for user selection to manage Meta and raw data.
    public void setupStudyList() {
        if (UserRoleDB.isLead(roleID)) {
            studyList = StudyDB.getPIStudyHash(userName);
        }
        else {
            studyList = StudyDB.getUserStudyHash(userName);
        }        
    }
    
    // User decided not to proceed to pipeline configuration page. Stay at the
    // current page.
    public void backToMainMenu() {
        logger.debug(userName + ": return to main menu.");
        plConfigPageURL = null;
    }
    
    // Save the study selection in the session map, and proceed to meta data
    // management page.
    public String proceedToMetaDataMgnt() {
        // Save the Study ID selection in the session map to be use by 
        // MetaDataManagementBean.
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("study_id", study_id);

        logger.debug(userName + ": selected study " + study_id + 
                     " to manage it's Meta data.");
        // Proceed to Meta data upload page.
        return Constants.META_DATA_MANAGEMENT + "?faces-redirect=true";
    }
    
    // Save the study and pipeline selection in the session map, and proceed to
    // raw data management page.
    public String proceedToRawDataMgnt() {
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("study_id", study_id);
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("pipeline", plName);
        
        logger.debug(userName + ": choose to work on the " + 
                     plName + " raw data in study " + study_id);
        // Proceed to Raw data management page.
        return Constants.RAW_DATA_MANAGEMENT + "?faces-redirect=true";
    }

    // User selected Study to work on, and has decided to proceed to pipeline
    // configuration page.
    public String proceedToConfig() {
        // Save the Study ID, pipeline name and haveNewData selection in the 
        // session map to be use by pipeline configuration bean.
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("study_id", study_id);
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("haveNewData", haveNewData);
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("pipeline", plName);
        
        logger.debug(userName + ": selected " + plName);
        logger.debug(haveNewData?"User have new data to upload.":
                     "No new data to upload.");
        // Proceed to pipeline configuration page.
        return plConfigPageURL;
    }
    
    // Only allow user to proceed to pipeline configuration page if a Study ID
    // has been selected.
    public Boolean getStudySelectedStatus() {
        return study_id != null;
    }
    
    // Return the list of Study ID setup that are available for this user to
    // execute pipeline.
    public LinkedHashMap<String, String> getStudyList() {
        return studyList;
    }
    
    // Setup the NGSConfigBean according to the specific pipeline selected.
    public String ngsPipeline() {
        return Constants.NGS_PAGE;
    }
    
    // Machine generated getters and setters
    public LinkedHashMap<String, String> getPipelineList() { return pipelineList; }
    public String getStudy_id() { return study_id; }
    public void setStudy_id(String study_id) { this.study_id = study_id; }
    public String getPlName() { return plName; }
    public void setPlName(String plName) { this.plName = plName; }
    public Boolean getHaveNewData() { return haveNewData; }
    public void setHaveNewData(Boolean haveNewData) { this.haveNewData = haveNewData; }
}
