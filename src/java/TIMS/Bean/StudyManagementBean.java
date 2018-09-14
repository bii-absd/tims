/*
 * Copyright @2015-2018
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.ICD10DB;
import TIMS.Database.Institution;
import TIMS.Database.Study;
import TIMS.Database.StudyDB;
import TIMS.Database.UserAccountDB;
import TIMS.Database.WorkUnitDB;
import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.inject.Named;
// Libraries for primefaces
import org.primefaces.event.RowEditEvent;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

/**
 * StudyManagementBean is the backing bean for the studymanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 07-Dec-2015
 * 
 * Revision History
 * 07-Dec-2015 - Created with all the standard getters and setters, plus two
 * static methods createNewStudy() and getAnnotHash().
 * 09-Dec-2015 - Added one attribute, dept_id. Added new method setupGrouping(),
 * to build the MultiSelectListbox options for Institution -> Departments.
 * 11-Dec-2015 - Added the module to edit study detail.
 * 15-Dec-2015 - To create a separate input directory for each Study ID created.
 * 22-Dec-2015 - Updated due to changes in some of the method name from 
 * Database Classes.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Dec-2016 - Removed all the static variables in Study and ItemList
 * management modules.
 * 19-Jan-2016 - To cater for adhoc study creation i.e. where the study is 
 * created with completed flag set to true.
 * 20-Jan-2016 - Updated study table in database; added one new variable closed, 
 * and renamed completed to finalized.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 18-Feb-2016 - During creation of study ID, automatically append the 
 * institution ID and department ID to the study ID.
 * 23-Feb-2016 - Implementation for database 3.0 (Part 1).
 * 01-Mar-2016 - Changes due to one addition attribute (i.e. title) in Study
 * class.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 15-Mar-2016 - During creation or updating of Study ID, only those groups
 * which are active, and have pi setup will be available for selection.
 * 22-Mar-2016 - Changes due to the addition field (i.e. icd_code) in the study
 * table.
 * 23-Mar-2016 - To allow user to update the study description, background and
 * grant information.
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 */

//@ManagedBean (name="studyMgntBean")
@Named("studyMgntBean")
@ViewScoped
public class StudyManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudyManagementBean.class.getName());
    // Attributes for Study object
    private String study_id, title, owner_id, grp_id, annot_ver, description, 
                   background, grant_info, icd_code;
    private Date start_date, end_date;
    private java.util.Date util_start_date, util_end_date;
    private Boolean finalized;
    private LinkedHashMap<String,String> annotHash, deptHash, grpHash, 
                                         piIDHash, icdHash;
    private List<SelectItem> grouping;
    private List<Study> studyList;
    // Store the user ID of the current user.
    private final String userName;
    private final WorkUnitDB work_unit;
    private final ICD10DB icd_db;
    
    public StudyManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        work_unit = new WorkUnitDB();
        icd_db = new ICD10DB();
        logger.info(userName + ": access Study ID Management page.");
    }
    
    @PostConstruct
    public void init() {
        start_date = end_date = null;
        annotHash = StudyDB.getAnnotHash();
        grpHash = work_unit.getGrpDB().getActiveGrpHashWithPI();
        piIDHash = UserAccountDB.getPiIDHash();
        icdHash = icd_db.getICDCodeHash();
        studyList = StudyDB.queryStudy();
        grouping = new ArrayList<>();
        setupGrouping();
    }
    
    // Update study description, background or grant information (aka DBGI) in
    // database.
    public void onStudyDBGIEdit(RowEditEvent event) {
        FacesContext fc = getFacesContext();
        
        if (StudyDB.updateStudyDBGI((Study) event.getObject())) {
            // Record this study update activity into database.
            StringBuilder detail = new StringBuilder("Study ").
                    append(((Study) event.getObject()).getStudy_id()).
                    append(" DBGI");
//            String detail = "Study " + ((Study) event.getObject()).getStudy_id() + 
//                            " DBGI";
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail.toString());
            StringBuilder oper = new StringBuilder(userName).
                                    append(": updated ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": updated " + detail);
            // Refresh the study list.
            studyList = StudyDB.queryStudy();
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, 
                    "Study DBGI updated.", ""));
        }
        else {
            logger.error("FAIL to update study DBGI!");
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Failed to update study!", ""));
        }
    }
    
    // Update study main info in database.
    public void onStudyMIEdit(RowEditEvent event) {
        FacesContext fc = getFacesContext();
        // Because the system is receiving the date as java.util.Date hence
        // we need to perform a conversion here before storing it into database.
        if (util_start_date != null) {
            ((Study) event.getObject()).setStart_date(new Date(util_start_date.getTime()));            
        }
        if (util_end_date != null) {
            ((Study) event.getObject()).setEnd_date(new Date(util_end_date.getTime()));
        }
        
        if (StudyDB.updateStudyMI((Study) event.getObject())) {
            // Record this study update activity into database.
            StringBuilder detail = new StringBuilder("Study ").
                    append(((Study) event.getObject()).getStudy_id()).
                    append(" main info");
//            String detail = "Study " + ((Study) event.getObject()).getStudy_id()
//                          + " main info";
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail.toString());
            StringBuilder oper = new StringBuilder(userName).
                                    append(": updated ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": updated " + detail);
            // Refresh the study list.
            studyList = StudyDB.queryStudy();
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "Study main info updated.", ""));
        }
        else {
            logger.error("FAIL to update study main info!");
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Failed to update study!", ""));
        }
    }

    // Setup the MultiSelectListbox options i.e. Insitution -> Departments -> Groups.
    private void setupGrouping() {
        List<Institution> instList = work_unit.getInstDB().getInstList();

        // Loop through the list of institutions setup in the system.
        for (Institution inst : instList) {
            SelectItemGroup instGrp = new SelectItemGroup(inst.getInst_id());
            // Retrieve the list of departments under this institution.
            List<String> deptIDList = work_unit.getDeptDB().getDeptIDList(inst.getInst_id());
            SelectItemGroup[] deptGrp = new SelectItemGroup[deptIDList.size()];
            int i = 0;

            for (String dept_id : deptIDList) {
                // Retrieve the list of groups under this department.
                // Only retrieve those groups which are active, and have pi setup.
                List<String> grpIDList = work_unit.getGrpDB().getActiveGrpIDListByDept(dept_id);
                SelectItem[] grpOpts = new SelectItem[grpIDList.size()];
                int j = 0;

                for (String grp_id : grpIDList) {
                    // Every group under this department will be an option
                    // for selection.
                    grpOpts[j++] = new SelectItem(grp_id, grp_id);
                }
                // Setup the options for this department.
                deptGrp[i] = new SelectItemGroup(dept_id);
                deptGrp[i++].setSelectItems(grpOpts);
            }
            
            // Setup the options for this institution.
            instGrp.setSelectItems(deptGrp);
            // Add this institution to the list.
            grouping.add(instGrp);
        }
    }
    
    // Create new Study
    public String createNewStudy() {
        FacesContext fc = getFacesContext();
        // Set the PI that is heading the group as the owner of this study.
        owner_id = work_unit.getGrpDB().getGrpPIID(grp_id);
        // Only proceed to create the new study ID if the group has a PI 
        // in-charge setup, else display error message.
        // After the changes to the query statement to retrieve the group ID,
        // only group with pi setup will be retrieve i.e. owner_id will never 
        // be null.
        if (owner_id == null) {
            logger.debug("PI in-charge for group " + grp_id + " not setup!");
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "The PI in-charge of group " + grp_id + 
                    " is not setup; failed to create new Study ID!", ""));
        } 
        else {
            // Append the institution ID and group ID to the study ID.
            study_id = work_unit.getGrpDB().getInstIDForGrp(grp_id) + "-" + grp_id + "-" + 
                       study_id.toUpperCase();
            // Because the system is receiving the date as java.util.Date hence
            // we need to perform a conversion here before storing it into database.
            if (util_start_date != null) {
                start_date = new Date(util_start_date.getTime());
            }
            if (util_end_date != null) {
                end_date = new Date(util_end_date.getTime());
            }
            // New Study will always be created with empty finalized_output and 
            // summary fields.
            Study study = new Study(study_id, title, grp_id, annot_ver, 
                                    icd_code, description, background, 
                                    grant_info, start_date, end_date, finalized);
        
            if (StudyDB.insertStudy(study)) {
                // Create a separate input directory for the newly created Study ID.
                    FileUploadBean.createStudyDirectory(study_id);
                // Record this study creation activity into database.
                String detail = "Study " + study_id;
                ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail);
                logger.info(userName + ": created " + detail);
                fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, 
                        "New Study ID created.", ""));
            }
            else {
                logger.debug("FAIL to create new Study ID: " + study_id);
                fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, 
                        "Failed to create new Study ID!", ""));
            }
        }

        return Constants.STUDY_MANAGEMENT;
    }
    
    // Return the Institution-Department grouping for user selection.
    public List<SelectItem> getGrouping() {
        return grouping;
    }
    
    // Return the list of Annotation Version setup in the system.
    public LinkedHashMap<String, String> getAnnotHash() {
        return annotHash;
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Return the full list of groups setup in the system.
    public LinkedHashMap<String, String> getGrpHash() {
        return grpHash;
    }

    // Return the list of user ID that belongs that to PIs.
    public LinkedHashMap<String, String> getPiIDHash() {
        return piIDHash;
    }
    
    // Return the list of ICD Code setup in the system.
    public LinkedHashMap<String, String> getIcdHash() {
        return icdHash;
    }
    
    // Machine generated getters and setters
    public List<Study> getStudyList() {
        return studyList;
    }
    public void setStudyList(List<Study> studyList) {    
        this.studyList = studyList;
    }
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getGrp_id() {
        return grp_id;
    }
    public void setGrp_id(String grp_id) {
        this.grp_id = grp_id;
    }
    public String getAnnot_ver() {
        return annot_ver;
    }
    public void setAnnot_ver(String annot_ver) {
        this.annot_ver = annot_ver;
    }
    public String getIcd_code() {
        return icd_code;
    }
    public void setIcd_code(String icd_code) {
        this.icd_code = icd_code;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getBackground() {
        return background;
    }
    public void setBackground(String background) {
        this.background = background;
    }
    public String getGrant_info() {
        return grant_info;
    }
    public void setGrant_info(String grant_info) {
        this.grant_info = grant_info;
    }
    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }
    public Boolean getFinalized() {
        return finalized;
    }
    // util_start_date and util_end_date are used to get the date inputs from
    // the UI.
    public java.util.Date getUtil_start_date() {
        return util_start_date;
    }
    public void setUtil_start_date(java.util.Date util_start_date) {
        this.util_start_date = util_start_date;
    }
    public java.util.Date getUtil_end_date() {
        return util_end_date;
    }
    public void setUtil_end_date(java.util.Date util_end_date) {
        this.util_end_date = util_end_date;
    }
}
