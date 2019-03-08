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
import TIMS.General.QueryStringGenerator;
// Libraries for Java
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("visStudyBean")
@ViewScoped
public class VisualizeStudyDataBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(VisualizeStudyDataBean.class.getName());
    private final String userName;
    private final UserAccount user;
    private List<Study> studies;
    private Study selectedStudy;
    
    public VisualizeStudyDataBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        user = UserAccountDB.getUserAct(userName);
        logger.debug("VisualizeStudyDataBean created.");
        logger.info(userName + " access Visualize my Study Data page.");
    }
    
    @PostConstruct
    public void init() {
        String groupQuery = QueryStringGenerator.genGrpQuery4Visualize(user);
        studies = StudyDB.queryStudies(groupQuery);
    }

    // Launch the cBioPortal application on a new tab.
    public void tocBioPortal(Study study) throws IOException {
        String cbio = study.getCbio_url();
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        // Record this activity.
        ActivityLogDB.recordUserActivity(userName, Constants.VIS_DAT, study.getStudy_id());
        ec.redirect(cbio);
    }
    
    // Machine generated getters and setters.
    public List<Study> getStudies() {
        return studies;
    }
    public Study getSelectedStudy() {
        return selectedStudy;
    }
    public void setSelectedStudy(Study selectedStudy) {
        this.selectedStudy = selectedStudy;
    }
}
