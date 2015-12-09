/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

// Libraries for Log4j
import Clinical.Data.Sink.Database.Department;
import Clinical.Data.Sink.Database.DepartmentDB;
import Clinical.Data.Sink.Database.Institution;
import Clinical.Data.Sink.Database.InstitutionDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.primefaces.event.RowEditEvent;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ItemListManagementBean is the backing bean for the itemlistmanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 16-Nov-2015
 * 
 * Revision History
 * 06-Nov-2015 - Created with all the standard getters and setters. Added new
 * method createNewInstitution() for creating new institution.
 * 18-Nov-2015 - Added one new method onInstRowEdit() to allow user to edit
 * the institution's information.
 * 09-Dec-2015 - Added in the module for adding and updating department info.
 */

@ManagedBean (name="itemListMgntBean")
@ViewScoped
public class ItemListManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ItemListManagementBean.class.getName());
    // Attributes for Institution object
    private String inst_id, inst_name;
    // Attributes for Department object
    private String dept_id, dept_name;
    private List<Institution> instList;
    private List<Department> deptList;
    
    public ItemListManagementBean() {
        logger.debug("ItemListManagementBean created.");
        logger.info(AuthenticationBean.getUserName() + 
                ": access Item List Management page.");
    }
    
    @PostConstruct
    public void init() {
        System.out.println("Post Construct.");
        instList = InstitutionDB.getInstList();
        deptList = DepartmentDB.getDeptList();
    }

    // Create the new institution ID
    public String createNewInstID() {
        Institution newInst = new Institution(inst_id, inst_name);
        
        if (InstitutionDB.insertInstitution(newInst)) {
            logger.info(AuthenticationBean.getUserName() +
                    ": created new institution ID: " + inst_id);
            addFacesInfoMsg("New institution ID created.");
        }
        else {
            logger.error("Failed to create new institution ID: " + inst_id);
            addFacesErrorMsg("Failed to create new institution ID!");
        }
        
        return Constants.ITEM_LIST_MANAGEMENT;
    }
    
    // Create the new department ID
    public String createNewDeptID() {
        Department newDept = new Department(inst_id, dept_id, dept_name);
        
        if (DepartmentDB.insertDepartment(newDept)) {
            logger.info(AuthenticationBean.getUserName() +
                    ": created new department ID: " + dept_id);
            addFacesInfoMsg("New department ID created.");
        }
        else {
            logger.error("Failed to create new department ID: " + dept_id);
            addFacesErrorMsg("Failed to create new department ID!");
        }
        
        return Constants.ITEM_LIST_MANAGEMENT;
    }
    
    // Update the institution table in the database.
    public void onInstRowEdit(RowEditEvent event) {
        if (InstitutionDB.updateInstitution((Institution) event.getObject())) {
            logger.info(AuthenticationBean.getUserName() + 
                    ": updated Institution.");
            addFacesInfoMsg("Institution updated.");
        }
        else {
            logger.error("Institution update failed.");
            addFacesErrorMsg("Failed to update institution!");
        }
    }
    
    // Return the HashMap of inst_name-inst_id for user selection.
    public LinkedHashMap<String, String> getInstNameHash() {
        return InstitutionDB.getInstNameHash();
    }
    
    // Update the department table in the database.
    public void onDeptRowEdit(RowEditEvent event) {
        if (DepartmentDB.updateDepartment((Department) event.getObject())) {
            logger.info(AuthenticationBean.getUserName() +
                    ": updated Department.");
            addFacesInfoMsg("Department updated.");
        }
        else {
            logger.error("Department update failed.");
            addFacesErrorMsg("Failed to update department!");
        }
    }
    
    // Show a Faces Info Message at the current context.
    private void addFacesInfoMsg(String msg) {
        getFacesContext().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, ""));
    }
    // Show a Faces Error Message ath the current context.
    private void addFacesErrorMsg(String msg) {
        getFacesContext().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, ""));
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Machine generated getters and setters
    public List<Institution> getInstList() {
        return instList;
    }
    public void setInstList(List<Institution> instList) {
        this.instList = instList;
    }
    public List<Department> getDeptList() {
        return deptList;
    }
    public void setDeptList(List<Department> deptList) {
        this.deptList = deptList;
    }
    public String getInst_id() {
        return inst_id;
    }
    public void setInst_id(String inst_id) {
        this.inst_id = inst_id;
    }
    public String getInst_name() {
        return inst_name;
    }
    public void setInst_name(String inst_name) {
        this.inst_name = inst_name;
    }
    public String getDept_id() {
        return dept_id;
    }
    public void setDept_id(String dept_id) {
        this.dept_id = dept_id;
    }
    public String getDept_name() {
        return dept_name;
    }
    public void setDept_name(String dept_name) {
        this.dept_name = dept_name;
    }   
}
