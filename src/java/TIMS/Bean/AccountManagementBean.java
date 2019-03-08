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
import TIMS.Database.Group;
import TIMS.Database.UserAccount;
import TIMS.Database.UserAccountDB;
import TIMS.Database.UserRoleDB;
import TIMS.Database.WorkUnitDB;
import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;
// Libraries for PrimeFaces
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.util.ComponentUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.TabChangeEvent;

@Named("acctMgntBean")
@ViewScoped
public class AccountManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(AccountManagementBean.class.getName());
    // User account table attributes
    private String user_id, first_name, last_name, email, pwd;
    private Boolean active;
    private int role_id;
    private String unit_id, inst_id, new_pwd, cfm_pwd;
    // Store the user ID of the current user.
    private final String userName;
    private List<UserAccount> userAcctList;
    private LinkedHashMap<String,String> instNameHash, instUnitIDHash, userIDHash;
//    private LinkedHashMap<String,Integer> roleNameHash;
    // For uploading of user photo.
    private FileUploadBean photo = null;
    private final WorkUnitDB work_unit;
    
    public AccountManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        instUnitIDHash = new LinkedHashMap<>();
        work_unit = new WorkUnitDB();
        logger.info(userName + ": access account management page.");
    }
    
    @PostConstruct
    public void init() {
        if (UserAccountDB.isAdministrator(userName)) {
            userAcctList = UserAccountDB.getAllUserAcct();
            userIDHash = UserAccountDB.getUserIDHash();
        }
        // Retrieve the list of role name setup in the system.
//        roleNameHash = UserRoleDB.getRoleNameHash();
        // Retrieve the list of institution setup in the system.
        instNameHash = work_unit.getInstDB().getAllInstNameHash();
    }
    
    // Clear the instUnitIDHash, and reset the institution and role selection 
    // whenever there is a tab change.
    public void onTabChange(TabChangeEvent event) {
        role_id = 0;
        inst_id = "None";
        instUnitIDHash.clear();
    }
    
    // Return all the user ID currently in the system.
    public LinkedHashMap<String,String> getUserIDHash() {
        return userIDHash;
    }
    
    // Return the list of user accounts currenlty in the system.
    public List<UserAccount> getUserAcctList() {
        return userAcctList;
    }
    
    // Build the unit ID Hash according to the user role at the selected row.
    public void onRowEditInit(RowEditEvent event) {
        UserAccount user = (UserAccount) event.getObject();
        // Build the unit ID Hash based on the institution and user role.
        String instID = work_unit.getInstDB().getInstID(user.getUnit_id());
        configInstUnitIDHash(instID, user.getRole_id());
        // Update the selectOneMenu (with ID unitID) for the selected row.
        String updateClientId = ComponentUtils.findComponentClientId("unitID");
        RequestContext.getCurrentInstance().update(updateClientId);
    }
    
    // Update the user account detail in the database.
    // After edit, return to the same page but keep the current view scope alive.
    public void onRowEdit(RowEditEvent event) {
        try {
            UserAccountDB.updateAccount((UserAccount) event.getObject());
            // Record this user account update activity into database.
            StringBuilder detail = new StringBuilder("User account ").
                    append(((UserAccount) event.getObject()).getUser_id());
//            String detail = "User account " + ((UserAccount) event.getObject())
//                            .getUser_id();
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail.toString());
            StringBuilder oper = new StringBuilder(userName).
                    append(": updated ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": updated " + detail);
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "User account updated.", ""));
        }
        catch (SQLException|NamingException e) {
            logger.error("Fail to update user account!");
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "Failed to update user account!", ""));            
        }
    }

    // Display a message if the user cancel the account update.
    public void onRowEditCancel(RowEditEvent event) {
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "Cancel Account Update.", ""));        
    }
    
    // Create the FileUploadBean object to store the photo of the user.
    public void preparePhotoUpload() {
        if (photo == null) {
            // All the user photo will be store in TIMS/images/ directory.
            photo = new FileUploadBean(Constants.getSYSTEM_PATH() + 
                                       Constants.getPIC_PATH());            
        }
    }
    
    // Create a new UserAccount object and call insertAccount to insert a new 
    // record into the user_account table.
    public String createUserAccount() {
        FacesContext facesContext = getFacesContext();
        // Filepath of the photo uploaded (if uploaded).
        String filename = "NA";
        
        if (!photo.isFilelistEmpty()) {
            filename = photo.getInputFilename();
            int ext = filename.indexOf(".");
            // Rename the photo filename to be the same as the unitID-userID, 
            // but keep the file extentsion.
            filename = unit_id + "-" + user_id + filename.substring(ext);
            // Rename the stored photo filename.
            photo.renameFilename(filename);
        }
        // By default, all new account will be active upon creation.
        UserAccount newAcct = new UserAccount(user_id, role_id, first_name, 
                    last_name, filename, email, true, pwd, unit_id, " ");
        
        try {
            // Insert the new account into database.
            UserAccountDB.insertAccount(newAcct);
            StringBuilder detail = new StringBuilder("User ID ").
                    append(user_id).append(" with ").
                    append(UserRoleDB.getRoleNameFromHash(role_id)).
                    append(" right");
//            String detail = "User ID " + user_id + " with " + 
//                         UserRoleDB.getRoleNameFromHash(role_id) + " right";
            // Record account creation activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail.toString());
            StringBuilder oper = new StringBuilder(userName).
                    append(": create ").append(detail);
            logger.info(oper);
//            logger.info(userName + ": create " + detail);
            // Create user working directories after the user account has been
            // successfully inserted into database.
            if (FileUploadBean.createUsersDirectories(Constants.getSYSTEM_PATH() +
                                                      Constants.getUSERS_PATH() +
                                                      user_id)) {
                logger.info("Working directories created.");
                facesContext.addMessage("newacctstatus", new FacesMessage(
                        FacesMessage.SEVERITY_INFO, 
                        "User Account successfully created.", ""));
            }
            else {
                logger.error("FAIL to create working directories for " + user_id);
                facesContext.addMessage("newacctstatus", new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, 
                        "Failed to create working directories for user, "
                        + "please contact the administrator!", ""));
            }
            
            // If this new user account is a PI, check on the pi field of the
            // group he is assigned to.
            if (role_id == UserRoleDB.pi()) {
                // If the PI field is not setup (i.e. null), then set it with 
                // this new user ID.
                if (work_unit.getGrpDB().getGrpPIID(unit_id) == null) {
                    Group tmp = work_unit.getGrpDB().getGrpByGrpID(unit_id);
                    // Update the pi field.
                    tmp.setPi(user_id);
                    work_unit.getGrpDB().updateGroup(tmp);
                }
            }
        }
        catch (SQLException|NamingException e) {
            // Try to get the detail error message from the exception
            int start = e.getMessage().indexOf("Detail:");
            // Trim the detail error message by removing "Detail: " (i.e. 8 characters)
            String errorMsg = e.getMessage().substring(start+8);
            StringBuilder err = new StringBuilder("FAIL to create User ID: ").
                    append(user_id).append(" : ").append(errorMsg);
            logger.error(err);
//            logger.error("FAIL to create User ID: " + user_id + 
//                         " : " + errorMsg);
            logger.error(e.getMessage());
            facesContext.addMessage("newacctstatus", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Failed: " + errorMsg, ""));
        }
        // Return to the same page, and recreate the AccountManagementBean.
        return Constants.ACCOUNT_MANAGEMENT;
    }
    
    // Update the photo ID of the current user.
    public void changePhotoIDListener(FileUploadEvent event) {
        // Upload the new picture.
        photo.singleFileUploadListener(event);
        // Get user account.
        UserAccount user = UserAccountDB.getUserAct(userName);
        String filename = photo.getInputFilename();
        int ext = filename.indexOf(".");
        // Rename the photo filename to be the same as the unitID-userID, 
        // but keep the file extentsion.
        filename = user.getUnit_id() + "-" + userName + filename.substring(ext);
        // Rename the stored photo filename.
        photo.renameFilename(filename);
        // Update user account in database.
        user.setPhoto(photo.getInputFilename());
        
        try {
            UserAccountDB.updateAccount(user);
            logger.info(userName + " updated picture ID.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update picture ID!");
            logger.error(e.getMessage());
        }
    }
    
    // Update the password of the current user if the two passwords entered 
    // are the same.
    public String changePassword() {
        FacesContext facesContext = getFacesContext();

        if (new_pwd.compareTo(cfm_pwd) == 0) {
            String id = userName;
            
            // If this user is a administrator, he/she might be changing the
            // password of another user.
            if (UserAccountDB.isAdministrator(userName) &&
                (user_id.compareTo("none") != 0) ) {
                id = user_id;
                // Record password change activity into database.
                ActivityLogDB.recordUserActivity(userName, Constants.CHG_PWD, id);
                StringBuilder oper = new StringBuilder(userName).
                        append(": change password of ").append(id);
                logger.info(oper);
//                logger.info(userName + ": change password of " + id);
            }
            
            try {
                // Update the password of the current user into database.
                UserAccountDB.updatePassword(id, new_pwd);
                // Record password change success into database.
                ActivityLogDB.recordUserActivity(id, Constants.CHG_PWD, "Success");
                logger.info(id + " password successfully updated.");
                facesContext.addMessage("changepwdstatus", new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Password successfully updated.", ""));
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to change password!");
                logger.error(e.getMessage());
                facesContext.addMessage("changepwdstatus", new FacesMessage(
                        FacesMessage.SEVERITY_FATAL, 
                        "Database Error, failed to change password!", ""));            
            }
        }
        else {
            logger.info("The passwords entered are not the same.");
            facesContext.addMessage("changepwdstatus", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "The passwords entered are not the same!", ""));
        }
        // Return to the same page, and recreate the AccountManagementBean.        
        return Constants.ACCOUNT_MANAGEMENT;
    }
    
    // Return the list of Role setup in the database
//    public LinkedHashMap<String, Integer> getRoleNameHash() {
//        return roleNameHash;
//    }
    // Return the list of Institution setup in the database
    public LinkedHashMap<String, String> getInstNameHash() {
        return instNameHash;
    }
    // Return the list of Unit ID (InstID|DeptID|GrpID) depending on the
    // institution and the role selected.
    public LinkedHashMap<String, String> getInstUnitIDHash() {
        return instUnitIDHash;
    }
    
    // The enabled/disabled status of "Select User Unit" will depend on
    // whether the institution and role have been selected or not.
    public boolean isInstUnitIDHashReady() {
        return instUnitIDHash.isEmpty();
    }
    
    // Listener for institution and role selection change, it's job is to update
    // the unit ID selection list accordingly to the institution + type of role.
    public void instRoleChange() {
        // Both the institution and role need to have a valid value in order to
        // proceed.
        if (inst_id != null) {
            if ( (role_id != 0) && (inst_id.compareTo("None") != 0) ) {
                configInstUnitIDHash(inst_id, role_id);
            }
        }
    }
    // Listener for role selection change in the "Edit User Account" panel.
    public void roleEditChange() {
        UserAccount user = getFacesContext().getApplication().
                evaluateExpressionGet(getFacesContext(), "#{acct}", 
                UserAccount.class);
        String instID = work_unit.getInstDB().getInstID(user.getUnit_id());
        configInstUnitIDHash(instID, user.getRole_id());
    }
    // Helper method to setup the instUnitIDHash based on the institution and
    // user's role.
    private void configInstUnitIDHash(String instID, int roleID) {
        if (roleID == UserRoleDB.director()) {
            instUnitIDHash = work_unit.getInstDB().getSingleInstNameHash(instID);
        }
        else if (roleID == UserRoleDB.hod()) {
            instUnitIDHash = work_unit.getDeptDB().getDeptHashForInst(instID);
        }
        else {
            instUnitIDHash = work_unit.getGrpDB().getGrpHashForInst(instID);
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }

    //Machine generated setters
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setFirst_name(String first_name) { this.first_name = first_name; }
    public void setLast_name(String last_name) { this.last_name = last_name; }
    public void setPhoto(FileUploadBean photo) { this.photo = photo; }
    public void setEmail(String email) { this.email = email; }
    public void setActive(Boolean active) { this.active = active; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public void setRole_id(int role_id) { this.role_id = role_id; }
    public void setUnit_id(String unit_id) { this.unit_id = unit_id; }
    public void setInst_id(String inst_id) { this.inst_id = inst_id; }
    public void setNew_pwd(String new_pwd) { this.new_pwd = new_pwd; }
    public void setCfm_pwd(String cfm_pwd) { this.cfm_pwd = cfm_pwd; }
    // Machine generated getters
    public String getUser_id() { return user_id; }
    public String getFirst_name() { return first_name; }
    public String getLast_name() { return last_name; }
    public FileUploadBean getPhoto() { return photo; }
    public String getEmail() { return email; }
    public Boolean getActive() { return active; }
    public String getPwd() { return pwd; }
    public int getRole_id() { return role_id; }
    public String getUnit_id() { return unit_id; }
    public String getInst_id() { return inst_id; }
    public String getNew_pwd() { return new_pwd; }
    public String getCfm_pwd() { return cfm_pwd; }
}
