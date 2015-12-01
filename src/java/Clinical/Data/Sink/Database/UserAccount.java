/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

/**
 * UserAccount is used to represent the user_account table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 25-Sep-2015
 * 
 * Revision History
 * 25-Sep-2015 - Created with all the standard getters and setters.
 * 28-Sep-2015 - Added in two methods (checkPwd and insertAccount).
 * 02-Oct-2015 - Added in the comments for the code.
 * 07-Oct-2015 - Added Log4j2 for this class. Changed to connection based for
 * database access.
 * 09-Oct-2015 - Update the class to handle the additional 2 fields (inst_id
 * and dept_id) in user_account table. Moved the two query methods to the
 * new class UserAccountDB.
 * 09-Nov-2015 - Added one variable "last_login" to this class. Added two 
 * methods getActiveString() and getRoleString() that return the active status
 * and role ID in wording.
 * 13-Nov-2015 - Removed the Log4j2 and SQL libraries from this class.
 * 01-Dec-2015 - Implementation for database 2.0
 */

public class UserAccount {
    // user_account table fields
    private String user_id, first_name, last_name, email, pwd;
    private Boolean active;
    private int role_id;
    private String dept_id, inst_id, last_login;

    public UserAccount(String user_id, int role_id, String first_name, 
                       String last_name, String email, Boolean active, 
                       String pwd, String dept_id, String inst_id,
                       String last_login) {
        this.user_id = user_id;
        this.role_id = role_id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
        this.active = active;
        this.pwd = pwd;
        this.dept_id = dept_id;
        this.inst_id = inst_id;
        this.last_login = last_login;
    }

    //Machine generated setters
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setFirst_name(String first_name) { 
        this.first_name = first_name; 
    }
    public void setLast_name(String last_name) { this.last_name = last_name; }
    public void setEmail(String email) { this.email = email; }
    public void setActive(Boolean active) { this.active = active; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public void setRole_id(int role_id) { this.role_id = role_id; }
    public void setDept_id(String dept_id) {
        this.dept_id = dept_id;
    }
    public void setInst_id(String inst_id) {
        this.inst_id = inst_id;
    }
    public void setLast_login(String last_login) {
        this.last_login = last_login;
    }
    
    // Return the Active status.
    public String getActiveStatus() {
        return active?"Enabled":"Disabled";
    }
    // Return the Role Name.
    public String getRoleName() {
        return UserRoleDB.getRoleNameFromHash(role_id);
    }
    
    // Machine generated getters
    public String getUser_id() { return user_id; }
    public String getFirst_name() { return first_name; }
    public String getLast_name() { return last_name; }
    public String getEmail() { return email; }
    public Boolean getActive() { return active; }
    public String getPwd() { return pwd; }
    public int getRole_id() { return role_id; }
    public String getDept_id() { return dept_id; }
    public String getInst_id() { return inst_id; }
    public String getLast_login() { return last_login; }
}
