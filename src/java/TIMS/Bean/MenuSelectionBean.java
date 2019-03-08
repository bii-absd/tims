// Copyright (C) 2019 A*STAR
//
// TIMS (Translation Informatics Management System) is an software effort 
// by the ABSD (Analytics of Biological Sequence Data) team in the 
// Bioinformatics Institute (BII), Agency of Science, Technology and Research 
// (A*STAR), Singapore.
//

// This file is part of TIMS.
// 
// TIMS is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as 
// published by the Free Software Foundation, either version 3 of the 
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
package TIMS.Bean;

import TIMS.Database.PipelineDB;
import TIMS.Database.StudyDB;
import TIMS.Database.UserAccountDB;
import TIMS.Database.UserRoleDB;
import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.util.LinkedHashMap;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.inject.Named;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("menuSelBean")
@ViewScoped
public class MenuSelectionBean implements Serializable{
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MenuSelectionBean.class.getName());
    private String study_id, plConfigPageURL, plName, visualiser;
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
        setupStudyList();
        
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
        PipelineDB plDB = new PipelineDB();
        
        setupStudyList();
        pipelineList = plDB.getEditablePlHash();
    }

    // Setup the Study ID list for user selection to manage Meta and raw data.
    public void setupStudyList() {
        if (UserRoleDB.isLead(roleID)) {
            studyList = StudyDB.getPIStudyHash(userName);
        }
        else if (roleID == UserRoleDB.admin()) {
            studyList = StudyDB.getAllStudyHash();
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
    
    // Save the visualiser selection in the session map, and proceed to job
    // selection for visualisation page.
    public String proceedToJobSelection4v() {
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("visualiser", visualiser);

        logger.debug(userName + ": selected " + visualiser + 
                     " to view it's pipeline data.");
        // Proceed to job selection for visualisation page.
        return Constants.JOB_SELECTION_4V + "?faces-redirect=true";
    }
    
    // Proceed to meta data management page.
    public String proceedToMetaDataMgnt() {
        // Save the Study ID selection in the session map to be use by 
        // MetaDataManagementBean.
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("study_id", study_id);

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
    public boolean getStudySelectedStatus() {
        return study_id != null;
    }
    
    // Only allow user to proceed to job selection for visualiser page if a 
    // visualiser has been selected.
    public boolean getVisualiserSelectedStatus() {
        return visualiser != null;
    }
    
    // Return the list of Study ID setup that are available for this user to
    // execute pipeline.
    public LinkedHashMap<String, String> getStudyList() {
        return studyList;
    }
    
    // Machine generated getters and setters
    public LinkedHashMap<String, String> getPipelineList() { return pipelineList; }
    public String getStudy_id() { return study_id; }
    public void setStudy_id(String study_id) { this.study_id = study_id; }
    public String getPlName() { return plName; }
    public void setPlName(String plName) { this.plName = plName; }
    public String getVisualiser() { return visualiser;  }
    public void setVisualiser(String visualiser) {  this.visualiser = visualiser;   }
    public Boolean getHaveNewData() { return haveNewData; }
    public void setHaveNewData(Boolean haveNewData) { this.haveNewData = haveNewData; }
}
