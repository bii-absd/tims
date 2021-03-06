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
import TIMS.Database.Department;
import TIMS.Database.Group;
import TIMS.Database.Institution;
import TIMS.Database.UserAccountDB;
import TIMS.Database.WorkUnitDB;
import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
// Libraries for PrimeFaces
import org.primefaces.event.RowEditEvent;
import org.primefaces.context.RequestContext;
import org.primefaces.util.ComponentUtils;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.omnifaces.cdi.ViewScoped;

@Named("grpMgntBean")
@ViewScoped
public class WorkUnitManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(WorkUnitManagementBean.class.getName());
    // Attributes for Institution object
    private String inst_id, inst_name;
    // Attributes for Department object
    private String dept_id, dept_name;
    // Attributes for Group object
    private String grp_id, grp_name, pi;
    private List<Institution> instList;
    private List<Department> deptList;
    private List<Group> grpList;
    private LinkedHashMap<String,String> instNameHash, instDeptHash, instPiIDHash;
    // Store the user ID of the current user.
    private final String userName;
    // Get the database accessors of institution, department and group tables.
    private final WorkUnitDB work_unit;
    
    public WorkUnitManagementBean() {
        instPiIDHash = new LinkedHashMap<>();
        instDeptHash = new LinkedHashMap<>();
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        work_unit = new WorkUnitDB();
        logger.info(userName + ": access Work Unit Management page.");
    }
    
    @PostConstruct
    public void init() {
        instList = work_unit.getInstDB().getInstList();
        deptList = work_unit.getDeptDB().getDeptList();
        grpList = work_unit.getGrpDB().getFullGrpList();
        instNameHash = work_unit.getInstDB().getAllInstNameHash();
    }

    // Create the new institution ID
    public String createNewInstID() {
        Institution newInst = new Institution(inst_id, inst_name);
        
        if (work_unit.getInstDB().insertInstitution(newInst)) {
            // Record this institution creation activity into database.
            StringBuilder detail = new StringBuilder("Institution ").append(inst_id);
//            String detail = "Institution " + inst_id;
            ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail.toString());
            StringBuilder oper = new StringBuilder(userName).append(": created ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": created " + detail);
            addFacesInfoMsg("New institution ID created.");
        }
        else {
            logger.error("FAIL to create new institution ID: " + inst_id);
            addFacesErrorMsg("Failed to create new institution ID!");
        }
        
        return Constants.WORKUNIT_MANAGEMENT;
    }
    
    // Create the new department ID
    public String createNewDeptID() {
        Department newDept = new Department(inst_id, dept_id, dept_name);
        
        if (work_unit.getDeptDB().insertDepartment(newDept)) {
            // Record this department creation activity into database.
            StringBuilder detail = new StringBuilder("Department ").
                    append(dept_id).append(" for institution ").append(inst_id);
//            String detail = "Department " + dept_id + " for institution " 
//                            + inst_id;
            ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail.toString());
            StringBuilder oper = new StringBuilder(userName).append(": created ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": created " + detail);
            addFacesInfoMsg("New department ID created.");
        }
        else {
            logger.error("FAIL to create new department ID: " + dept_id);
            addFacesErrorMsg("Failed to create new department ID!");
        }
        
        return Constants.WORKUNIT_MANAGEMENT;
    }
    
    // Create the new group ID
    public String createNewGrpID() {
        // By default, all the newly created group will be active.
        Group newGrp = new Group(grp_id, pi, dept_id, grp_name, true);
        
        if (work_unit.getGrpDB().insertGroup(newGrp)) {
            // Record this group creation activity into database.
            StringBuilder detail = new StringBuilder("Group ").append(grp_id).
                    append(" for department ").append(dept_id);
//            String detail = "Group " + grp_id + " for department " + dept_id;
            ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail.toString());
            StringBuilder oper = new StringBuilder(userName).append(": created ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": created " + detail);
            addFacesInfoMsg("New group ID created.");
        }
        else {
            logger.error("FAIL to create new group ID: " + grp_id);
            addFacesErrorMsg("Failed to create new group ID!");
        }
        
        return Constants.WORKUNIT_MANAGEMENT;
    }
    
    // Update the inst table in the database.
    public void onInstRowEdit(RowEditEvent event) {
        String detail = "Institution " + ((Institution) event.getObject()).getInst_id();
        
        if (work_unit.getInstDB().updateInstitution((Institution) event.getObject())) {
            // Record this institution update activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail);
            StringBuilder oper = new StringBuilder(userName).append(": updated ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": updated " + detail);
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
    // Return the HashMap of PI IDs under this institution.
    public LinkedHashMap<String, String> getInstPiIDHash() {
        return instPiIDHash;
    }
    // Return the HashMap of department IDs under this institution.
    public LinkedHashMap<String, String> getInstDeptHash() {
        return instDeptHash;
    }
    
    // Update the dept table in the database.
    public void onDeptRowEdit(RowEditEvent event) {
        String detail = "Department " + ((Department) event.getObject()).getDept_id();
        
        if (work_unit.getDeptDB().updateDepartment((Department) event.getObject())) {
            // Record this department update activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail);
            StringBuilder oper = new StringBuilder(userName).append(": updated ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": updated " + detail);
            // Update the department data table.
            deptList = work_unit.getDeptDB().getDeptList();
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
        
        if (work_unit.getGrpDB().updateGroup((Group) event.getObject())) {
            // Record this group update activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail);
            StringBuilder oper = new StringBuilder(userName).append(": updated ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": updated " + detail);
            // Update the group data table.
            grpList = work_unit.getGrpDB().getFullGrpList();
            addFacesInfoMsg(detail + " updated.");
        }
        else {
            logger.error("FAIL to update " + detail);
            addFacesErrorMsg("Failed to update " + detail);
        }
    }
    
    // Build the instPiIDHash and instDeptHash according to the user's 
    // institution.
    public void onGrpRowEditInit(RowEditEvent event) {
        Group grp = (Group) event.getObject();
        // Build the instPiIDHash based on the user's institution.
        String instID = work_unit.getGrpDB().getInstIDForGrp(grp.getGrp_id());
        instPiIDHash = UserAccountDB.getInstPiIDHash(instID);
        // Build the instDeptHash based on the user's institution.
        instDeptHash = work_unit.getDeptDB().getDeptHashForInst(instID);
        // Update the selectOneMenu (with ID editGrpPi and editGrpDept) for 
        // the selected row.
        String editGrpPi = ComponentUtils.findComponentClientId("editGrpPi");
        String editGrpDept = ComponentUtils.findComponentClientId("editGrpDept");
        RequestContext.getCurrentInstance().update(editGrpPi);
        RequestContext.getCurrentInstance().update(editGrpDept);
    }
    
    // Listener for institution selection change, it's job is to update the 
    // department and PI incharge lists according to the institution selected.
    public void instChange() {
        if (inst_id.compareTo("None") == 0) {
            instDeptHash.clear();
            instPiIDHash.clear();
        }
        else {
            instDeptHash = work_unit.getDeptDB().getDeptHashForInst(inst_id);
            instPiIDHash = UserAccountDB.getInstPiIDHash(inst_id);
        }
    }
    // The enabled/disabled status of "Select Department" and "Select PI 
    // Incharge" will depend on whether the institution has been selected or not.
    public boolean isInstDeptHashReady() {
        return instDeptHash.isEmpty();
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
