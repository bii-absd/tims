/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.UserAccount;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.Database.UserRole;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.primefaces.event.RowEditEvent;

/**
 * AccountManagementBean is the backing bean for the accountmanagement.xhtml.
 * 
 * Author: Tay Wei Hong
 * Date: 28-Sep-2015
 * 
 * Revision History
 * 28-Sep-2015 - Created with all the standard getters and setters.
 * 06-Oct-2015 - Added in the comments for the code. Added Log4j2 for this 
 * class.
 * 23-Oct-2015 - Role will be inputted by the user during account creation i.e.
 * no longer defaulted to 'User'.
 * 04-Nov-2015 - Port to JSF 2.2. Added new method changePassword to allow user
 * to change his/her password.  Added 2 new variables, new_pwd and cfm_pwd.
 * 09-Nov-2015 - Added the following methods to support the 'update user
 * account' module:
 * 1. init()
 * 2. getUserList()
 * 3. onRowEdit
 */

@ManagedBean (name="accountManagementBean")
@ViewScoped
public class AccountManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(AccountManagementBean.class.getName());
    private String user_id;
    private String first_name, last_name;
    private String email, pwd;
    private Boolean active;
    private int role_id;
    private String department, institution;
    private String new_pwd, cfm_pwd;
    private static List<UserAccount> userList = new ArrayList<>();
    
    public AccountManagementBean() {
        logger.debug("AccountManagementBean created.");
    }
    
    @PostConstruct
    public void init() {
        if (AuthenticationBean.isAdministrator()) {
            userList = UserAccountDB.getAllUser();
        }
    }
    
    // Return the list of user accounts currenlty in the system.
    public List<UserAccount> getUserList() {
        return userList;
    }
    
    // Update the user account detail in the database.
    public void onRowEdit(RowEditEvent event) {
        try {
            UserAccountDB.updateAccount((UserAccount) event.getObject());
            logger.debug("User account: " + 
                    ((UserAccount) event.getObject()).getUser_id() + 
                    " updated.");
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "User account updated.", ""));
        }
        catch (SQLException e) {
            logger.error("User account update failed.");
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "Failed to update user account!", ""));            
        }
    }

    // Create a new UserAccount object and call insertAccount to insert a new 
    // record into the user_account table.
    public String createUserAccount() {
        FacesContext facesContext = getFacesContext();
        // By default, all new account will be active upon creation
        UserAccount newAcct = new UserAccount(user_id, role_id, first_name, 
                    last_name, email, true, pwd, department, institution, 
                    " ");
        
        try {
            // Insert the new account into database        
            UserAccountDB.insertAccount(newAcct);
            logger.info(AuthenticationBean.getUserName() + 
                    ": created new User ID " + user_id + 
                    " with " + UserRole.getRoleFromHash(role_id) + " right.");
            facesContext.addMessage("newacctstatus", new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "User Account: " 
                    + user_id + " successfully created.", ""));
        }
        catch (SQLException e) {
            // Try to get the detail error message from the exception
            int start = e.getMessage().indexOf("Detail:");
            // Trim the detail error message by removing "Detail: " (i.e. 8 characters)
            String errorMsg = e.getMessage().substring(start+8);
            
            logger.error("SQLException encountered while creating new User ID: "
                    + user_id + " : " + errorMsg);
            logger.error(e.getMessage());
            facesContext.addMessage("newacctstatus", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Failed: " + errorMsg, ""));
        }
        // Return to the same page, but recreate the AccountManagementBean.
        return Constants.ACCOUNT_MANAGEMENT;
    }
    
    // Update the password of the current user if the two passwords entered 
    // are the same.
    public String changePassword() {
        FacesContext facesContext = getFacesContext();

        if (new_pwd.compareTo(cfm_pwd) == 0) {
            try {
                // Update the password of the current user into the database
                UserAccountDB.updatePassword(AuthenticationBean.getUserName(), 
                        new_pwd);
                logger.info(AuthenticationBean.getUserName() + 
                        ": successfully updated password.");
                facesContext.addMessage("changepwdstatus", new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Password successfully updated.", ""));
            }
            catch (SQLException e) {
                logger.error("SQLException while trying to update password of " 
                        + AuthenticationBean.getUserName());
                logger.error(e.getMessage());
            facesContext.addMessage("changepwdstatus", new FacesMessage(
                    FacesMessage.SEVERITY_FATAL, 
                    "Database Error, failed to update password!", ""));            }
        }
        else {
            logger.info(AuthenticationBean.getUserName() + 
                    ": the two new passwords entered are not the same.");
            facesContext.addMessage("changepwdstatus", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "The passwords entered are not the same", ""));
        }
        // Return to the same page, but recreate the AccountManagementBean.        
        return Constants.ACCOUNT_MANAGEMENT;
    }
    
    // Return the list of Role setup in the database
    public LinkedHashMap<String, Integer> getRoleList() {
        return UserRole.getRoleList();
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }

    //Machine generated setters
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setFirst_name(String first_name) { this.first_name = first_name; }
    public void setLast_name(String last_name) { this.last_name = last_name; }
    public void setEmail(String email) { this.email = email; }
    public void setActive(Boolean active) { this.active = active; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public void setRole_id(int role_id) { this.role_id = role_id; }
    public void setDepartment(String department) 
    {   this.department = department;   }
    public void setInstitution(String institution) 
    {   this.institution = institution; }
    public void setNew_pwd(String new_pwd) { this.new_pwd = new_pwd; }
    public void setCfm_pwd(String cfm_pwd) { this.cfm_pwd = cfm_pwd; }

    // Machine generated getters
    public String getUser_id() { return user_id; }
    public String getFirst_name() { return first_name; }
    public String getLast_name() { return last_name; }
    public String getEmail() { return email; }
    public Boolean getActive() { return active; }
    public String getPwd() { return pwd; }
    public int getRole_id() { return role_id; }
    public String getDepartment() { return department; }
    public String getInstitution() { return institution; }
    public String getNew_pwd() { return new_pwd; }
    public String getCfm_pwd() { return cfm_pwd; }
}