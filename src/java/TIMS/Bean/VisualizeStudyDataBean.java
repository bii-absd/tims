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
import TIMS.General.QueryStringGenerator;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * VisualizeStudyDataBean is the backing bean for the visualizemystudy view.
 * 
 * Author: Tay Wei Hong
 * Date: 28-Jun-2016
 * 
 * Revision History
 * 04-Jul-2016 - Implemented the visualization module. Integration with the 
 * first visualizer i.e. cBioportal.
 * 07-Jul-2016 - Added one new variable, selectedStudy and it's associated 
 * functions.
 */

@ManagedBean (name = "visStudyBean")
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
