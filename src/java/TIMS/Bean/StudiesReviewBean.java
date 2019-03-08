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
import TIMS.Database.UserAccount;
import TIMS.Database.UserAccountDB;
import TIMS.General.Constants;
import TIMS.General.FileHelper;
import TIMS.General.QueryStringGenerator;
// Libraries for Java
import java.io.Serializable;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.inject.Named;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("StudiesBean")
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
