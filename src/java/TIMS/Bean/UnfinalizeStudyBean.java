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
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

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
