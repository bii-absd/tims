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
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.omnifaces.cdi.ViewScoped;

@Named("clstudyBean")
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
