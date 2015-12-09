/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

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
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
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
 * static methods createNewStudy() and getAnnotList().
 * 09-Dec-2015 - Added one attribute, dept_id. Added new method setupGrouping(),
 * to build the MultiSelectListbox options for Institution -> Departments.
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
    private Boolean completed;
    private LinkedHashMap<String, String> annotList;
    private List<SelectItem> grouping;
    
    public StudyManagementBean() {
        user_id = AuthenticationBean.getUserName();
        sqlDate = new Date(Calendar.getInstance().getTime().getTime());
        logger.debug("StudyManagementBean created.");
        logger.debug(user_id + ": access Study ID Management page.");
    }
    
    @PostConstruct
    public void init() {
        annotList = new LinkedHashMap<>();
        grouping = new ArrayList<>();
        annotList = StudyDB.getAnnotHashMap();
        setupGrouping();
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
        Study study = new Study(study_id, dept_id, user_id, annot_ver, 
                                description, sqlDate);
        
        if (StudyDB.insertStudy(study)) {
            logger.info(user_id + ": created new Study ID: " + study_id);
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "New Study ID created.", ""));
        }
        else {
            logger.info("Failed to create new Study ID: " + study_id);
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
    public LinkedHashMap<String, String> getAnnotList() {
        return annotList;
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }

    // Machine generated getters and setters
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
    public Boolean getCompleted() {
        return completed;
    }
}
