/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.ActivityLogDB;
import Clinical.Data.Sink.Database.Department;
import Clinical.Data.Sink.Database.DepartmentDB;
import Clinical.Data.Sink.Database.Group;
import Clinical.Data.Sink.Database.GroupDB;
import Clinical.Data.Sink.Database.Institution;
import Clinical.Data.Sink.Database.InstitutionDB;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for PrimeFaces
import org.primefaces.event.RowEditEvent;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * GroupManagementBean is the backing bean for the groupmanagement view.
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
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Dec-2016 - Removed all the static variables in Study and ItemList
 * management modules.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 23-Feb-2016 - Rename class name from ItemListManagementBean to 
 * GroupManagementBean.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 15-Mar-2016 - Changes due to a new field (i.e. active) added in the grp table.
 */

@ManagedBean (name="grpMgntBean")
@ViewScoped
public class GroupManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(GroupManagementBean.class.getName());
    // Attributes for Institution object
    private String inst_id, inst_name;
    // Attributes for Department object
    private String dept_id, dept_name;
    // Attributes for Group object
    private String grp_id, grp_name, pi;
    private List<Institution> instList;
    private List<Department> deptList;
    private List<Group> grpList;
    private LinkedHashMap<String,String> instNameHash, deptNameHash, piIDHash;
    // Store the user ID of the current user.
    private final String userName;
    
    public GroupManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        logger.debug("ItemListManagementBean created.");
        logger.info(userName + ": access Item List Management page.");
    }
    
    @PostConstruct
    public void init() {
        instList = InstitutionDB.getInstList();
        deptList = DepartmentDB.getDeptList();
        grpList = GroupDB.getFullGrpList();
        instNameHash = InstitutionDB.getInstNameHash();
        deptNameHash = DepartmentDB.getAllDeptHash();
        piIDHash = UserAccountDB.getPiIDHash();
    }

    // Create the new institution ID
    public String createNewInstID() {
        Institution newInst = new Institution(inst_id, inst_name);
        
        if (InstitutionDB.insertInstitution(newInst)) {
            // Record this institution creation activity into database.
            String detail = "Institution " + inst_id;
            ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail);
            logger.info(userName + ": created " + detail);
            addFacesInfoMsg("New institution ID created.");
        }
        else {
            logger.error("FAIL to create new institution ID: " + inst_id);
            addFacesErrorMsg("Failed to create new institution ID!");
        }
        
        return Constants.GROUP_MANAGEMENT;
    }
    
    // Create the new department ID
    public String createNewDeptID() {
        Department newDept = new Department(inst_id, dept_id, dept_name);
        
        if (DepartmentDB.insertDepartment(newDept)) {
            // Record this department creation activity into database.
            String detail = "Department " + dept_id + " for institution " 
                            + inst_id;
            ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail);
            logger.info(userName + ": created " + detail);
            addFacesInfoMsg("New department ID created.");
        }
        else {
            logger.error("FAIL to create new department ID: " + dept_id);
            addFacesErrorMsg("Failed to create new department ID!");
        }
        
        return Constants.GROUP_MANAGEMENT;
    }
    
    // Create the new group ID
    public String createNewGrpID() {
        // By default, all the newly created group will be active.
        Group newGrp = new Group(grp_id, pi, dept_id, grp_name, true);
        
        if (GroupDB.insertGroup(newGrp)) {
            // Record this group creation activity into database.
            String detail = "Group " + grp_id + " for department " + dept_id;
            ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail);
            logger.info(userName + ": created " + detail);
            addFacesInfoMsg("New group ID created.");
        }
        else {
            logger.error("FAIL to create new group ID: " + grp_id);
            addFacesErrorMsg("Failed to create new group ID!");
        }
        
        return Constants.GROUP_MANAGEMENT;
    }
    
    // Update the inst table in the database.
    public void onInstRowEdit(RowEditEvent event) {
        String detail = "Institution " + ((Institution) event.getObject()).getInst_id();
        
        if (InstitutionDB.updateInstitution((Institution) event.getObject())) {
            // Record this institution update activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail);
            logger.info(userName + ": updated " + detail);
            addFacesInfoMsg(detail + " updated.");
        }
        else {
            logger.error("FAIL to update " + detail);
            addFacesErrorMsg("Failed to update " + detail);
        }
    }
    
    // Return the HashMap of inst_name-inst_id for user selection.
    public LinkedHashMap<String, String> getInstNameHash() {
        return instNameHash;
    }
    // Return the HashMap of dept_id-dept_id for user selection.
    public LinkedHashMap<String, String> getDeptNameHash() {
        return deptNameHash;
    }
    // Return the HashMap of user ID that can be PI.
    public LinkedHashMap<String, String> getPiIDHash() {
        return piIDHash;
    }
    
    // Update the dept table in the database.
    public void onDeptRowEdit(RowEditEvent event) {
        String detail = "Department " + ((Department) event.getObject()).getDept_id();
        
        if (DepartmentDB.updateDepartment((Department) event.getObject())) {
            // Record this department update activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail);
            logger.info(userName + ": updated " + detail);
            // Update the department data table.
            deptList = DepartmentDB.getDeptList();
            addFacesInfoMsg(detail + " updated.");
        }
        else {
            logger.error("FAIL to update " + detail);
            addFacesErrorMsg("Failed to update " + detail);
        }
    }
    
    // Update the grp table in the database.
    public void onGrpRowEdit(RowEditEvent event) {
        String detail = "Group " + ((Group) event.getObject()).getGrp_id();
        
        if (GroupDB.updateGroup((Group) event.getObject())) {
            // Record this group update activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail);
            logger.info(userName + ": updated " + detail);
            // Update the group data table.
            grpList = GroupDB.getFullGrpList();
            addFacesInfoMsg(detail + " updated.");
        }
        else {
            logger.error("FAIL to update " + detail);
            addFacesErrorMsg("Failed to update " + detail);
        }
    }
    
    // Show a Faces Info Message at the current context.
    private void addFacesInfoMsg(String msg) {
        getFacesContext().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, ""));
    }
    // Show a Faces Error Message at the current context.
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
    public List<Group> getGrpList() {
        return grpList;
    }
    public void setGrpList(List<Group> grpList) {
        this.grpList = grpList;
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
    public String getGrp_id() {
        return grp_id;
    }
    public void setGrp_id(String grp_id) {
        this.grp_id = grp_id;
    }
    public String getGrp_name() {
        return grp_name;
    }
    public void setGrp_name(String grp_name) {
        this.grp_name = grp_name;
    }
    public String getPi() {
        return pi;
    }
    public void setPi(String pi) {
        this.pi = pi;
    }
}
