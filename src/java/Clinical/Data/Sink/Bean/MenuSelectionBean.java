/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.DataDepositor;
import Clinical.Data.Sink.Database.DataRetriever;
import Clinical.Data.Sink.Database.StudyDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.util.LinkedHashMap;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * MenuSelectionBean is used as the backing bean for the main.xhtml
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
 */

@ManagedBean (name="menuSelectionBean")
@ViewScoped
public class MenuSelectionBean implements Serializable{
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MenuSelectionBean.class.getName());
    private String study_id, config_page;
    private LinkedHashMap<String, String> studyList;
    
    public MenuSelectionBean() {
        logger.debug("MenuSelectionBean created.");
    }
    
    @PostConstruct
    public void init() {
        studyList = StudyDB.getStudyList(AuthenticationBean.getUserName());
    }
    
    // Setup the ConfigBean for GEX Illumina pipeline processing.
    public void gexIllumina() {
        ConfigBean.setCommandLink("run-gex-pipeline (Illumina)");
        config_page = Constants.GEX_ILLUMINA_PAGE;
    }
    
    // Setup the ConfigBean for GEX Affymetrix pipeline processing.
    public void gexAffymetrix() {
        ConfigBean.setCommandLink("run-gex-pipeline (Affymetrix)");
        config_page = Constants.GEX_AFFYMETRIX_PAGE;
    }
    
    // User decided not to proceed to pipeline configuration page. Stay at the
    // current page.
    public void backToMainMenu() {
        logger.debug(AuthenticationBean.getUserName() + ": return to main menu.");
        config_page = null;
    }
    
    // User selected Study to work on, and has decided to proceed to pipeline
    // configuration page.
    public String proceedToConfig() {
        // Setup Study ID in pipeline configuration
        ConfigBean.setStudyID(study_id);
        logger.debug(AuthenticationBean.getUserName() + ": selected " +
                     config_page);
        // Proceed to pipeline configuration page.
        return config_page;
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
    
    // For testing DataDepositor
    public String dataDepositor() {
        // Pass in the job_id of the submitted job that is going to be finalize.
        // For testing purpose, assume we are going to finalize job_id 2,3 and 15 and 16.
        DataDepositor depositorThread = new DataDepositor(3);
        depositorThread.start();

        return Constants.NGS_PAGE;
    }
    
    // For testing DataRetriever
    public String dataRetriever() {
        // Pass in the study_id of the study that the system is going to 
        // generate the consolidated output.
        // For testing purpose, use study_id 'Bayer'
        DataRetriever retrieverThread = new DataRetriever("Bayer");
        retrieverThread.start();

        return Constants.NGS_PAGE;
    }
    
    // Machine generated getters and setters
    public String getStudy_id() { return study_id; }
    public void setStudy_id(String study_id) { this.study_id = study_id; }
}
