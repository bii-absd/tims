/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.ActivityLogDB;
import Clinical.Data.Sink.Database.DepartmentDB;
import Clinical.Data.Sink.Database.Institution;
import Clinical.Data.Sink.Database.InstitutionDB;
import Clinical.Data.Sink.Database.Study;
import Clinical.Data.Sink.Database.StudyDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
// Libraries for primefaces
import org.primefaces.event.RowEditEvent;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
 */

@ManagedBean (name="studyMgntBean")
@ViewScoped
public class StudyManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudyManagementBean.class.getName());
    // Attributes for Study object
    private String study_id, dept_id, user_id, annot_ver, description;
    private Date sqlDate;
    private java.util.Date utilDate;
    private Boolean finalized;
    private LinkedHashMap<String,String> annotHash, deptHash;
    private List<SelectItem> grouping;
    private List<Study> studyList;
    
    public StudyManagementBean() {
        user_id = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        sqlDate = new Date(Calendar.getInstance().getTime().getTime());
        logger.debug("StudyManagementBean created.");
        logger.debug(user_id + ": access Study ID Management page.");
    }
    
    @PostConstruct
    public void init() {
        annotHash = StudyDB.getAnnotHash();
        deptHash = DepartmentDB.getAllDeptHash();
        studyList = StudyDB.queryStudy();
        grouping = new ArrayList<>();
        setupGrouping();
    }
    
    // Update the study table in database.
    public void onRowEdit(RowEditEvent event) {
        FacesContext fc = getFacesContext();
        // Because the system is receiving the date as java.util.Date hence
        // we need to perform a conversion here before storing it into database.
        if (utilDate != null) {
            ((Study) event.getObject()).setSqlDate(new Date(utilDate.getTime()));            
        }

        if (StudyDB.updateStudy((Study) event.getObject())) {
            // Record this study update activity into database.
            String detail = "Study " + ((Study) event.getObject()).getStudy_id();
            ActivityLogDB.recordUserActivity(user_id, Constants.CHG_ID, detail);
            logger.info(user_id + ": updated " + detail);
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "Study updated.", ""));
        }
        else {
            logger.error("FAIL to update study!");
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Failed to update study!", ""));
        }
    }

    // Setup the MultiSelectListbox options i.e. Insitution -> Departments.
    private void setupGrouping() {
        List<Institution> instList = InstitutionDB.getInstList();
        
        // Loop through the list of institutions setup in the system.
        for (Institution inst : instList) {
            SelectItemGroup grp = new SelectItemGroup(inst.getInst_id());
            List<String> deptIDList = DepartmentDB.getDeptIDList(inst.getInst_id());
            SelectItem[] options = new SelectItem[deptIDList.size()];
            int i = 0;
            
            for (String dept_id : deptIDList) {
                // Every department under this institution will be an option
                // for selection.
                options[i++] = new SelectItem(dept_id, dept_id);
            }

            // Setup the options for this institution.
            grp.setSelectItems(options);
            // Add this institution to the list.
            grouping.add(grp);
        }   
    }
    
    // Create new Study
    public String createNewStudy() {
        FacesContext fc = getFacesContext();
        // Append the institution ID and department ID to the study ID.
        study_id = DepartmentDB.getInstID(dept_id) + "-" + dept_id + "-" + 
                   study_id.toUpperCase();
        // New Study will always be created with empty finalized_output and 
        // summary fields.
        Study study = new Study(study_id, dept_id, user_id, annot_ver, 
                                description, sqlDate, finalized);
        
        if (StudyDB.insertStudy(study)) {
            // Create a separate input directory for the newly created Study ID.
            FileUploadBean.createStudyDirectory(study_id);
            // Record this study creation activity into database.
            String detail = "Study " + study_id;
            ActivityLogDB.recordUserActivity(user_id, Constants.CRE_ID, detail);
            logger.info(user_id + ": created " + detail);
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
    
    // Return the full list of departments setup in the system.
    public LinkedHashMap<String, String> getDeptHash() {
        return deptHash;
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
    public String getDept_id() {
        return dept_id;
    }
    public void setDept_id(String dept_id) {
        this.dept_id = dept_id;
    }
    public String getUser_id() {
        return user_id;
    }
    public String getAnnot_ver() {
        return annot_ver;
    }
    public void setAnnot_ver(String annot_ver) {
        this.annot_ver = annot_ver;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Date getSqlDate() {
        return sqlDate;
    }
    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }
    public Boolean getFinalized() {
        return finalized;
    }
    // utilDate is used as a temporary placement for attribute sqlDate during
    // edit operation.
    public java.util.Date getUtilDate() {
        return utilDate;
    }
    public void setUtilDate(java.util.Date utilDate) {
        this.utilDate = utilDate;
    }
}
