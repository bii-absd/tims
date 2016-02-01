/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.StudyDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.util.LinkedHashMap;
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
 */

@ManagedBean (name="menuSelectionBean")
@ViewScoped
public class MenuSelectionBean implements Serializable{
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MenuSelectionBean.class.getName());
    private String study_id, plConfigPageURL, plName;
    private Boolean haveNewData;
    private LinkedHashMap<String, String> studyList;
    // Store the user ID of the current user.
    private final String userName;
    
    public MenuSelectionBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        // Set the single user mode to false when the user enter the main page.
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("singleUser", false);
        logger.debug("MenuSelectionBean created.");
    }
    
    @PostConstruct
    public void init() {
        studyList = StudyDB.getStudyHash(userName);
    }
    
    // All the pipeline link will be using this method to setup the URL for 
    // pipeline configuration page.
    public void setupPlConfigPageURL() {
        plConfigPageURL = plName + "?faces-redirect=true";
    }
    
    // User decided not to proceed to pipeline configuration page. Stay at the
    // current page.
    public void backToMainMenu() {
        logger.debug(userName + ": return to main menu.");
        plConfigPageURL = null;
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
    
    // Return the list of Study ID setup for this user's department.
    public LinkedHashMap<String, String> getStudyList() {
        return studyList;
    }

    // Setup the NGSConfigBean according to the specific pipeline selected.
    public String ngsPipeline() {
        return Constants.NGS_PAGE;
    }
    
    // User accessing his/her work area. Set the single user mode to true.
    public String userJobStatus() {
        // Save the single user mode selection in the session map to be use by
        // job status bean.
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("singleUser", true);
        return Constants.JOB_STATUS;
    }
    
    // Machine generated getters and setters
    public String getStudy_id() { return study_id; }
    public void setStudy_id(String study_id) { this.study_id = study_id; }
    public String getPlName() { return plName; }
    public void setPlName(String plName) { this.plName = plName; }
    public Boolean getHaveNewData() { return haveNewData; }
    public void setHaveNewData(Boolean haveNewData) 
    { this.haveNewData = haveNewData; }
    
    /*
    
    // NOT IN USE.
    // Setup config_page for GEX Illumina pipeline processing.
    public void gexIllumina() {
        config_page = Constants.GEX_ILLUMINA_PAGE;
    }
    
    // Setup config_page for GEX Affymetrix pipeline processing.
    public void gexAffymetrix() {
        config_page = Constants.GEX_AFFYMETRIX_PAGE;
    }
    
    // Setup config_page for METH pipeline processing.
    public void methPipeline() {
        config_page = Constants.METH_PIPELINE_PAGE;
    }
    
    // Setup config_page for CNV pipeline processing.
    public void cnvPipeline() {
        config_page = Constants.CNV_PIPELINE_PAGE;
    }
    
    */
}
