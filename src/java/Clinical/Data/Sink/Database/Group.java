/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Database;

/**
 * Group is used to represent the grp table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 04-Mar-2016
 * 
 * Revision History
 * 04-Mar-2016 - Created with all the standard getters and setters.
 */

public class Group {
    // grp table fields
    private String grp_id, pi, dept_id, grp_name;
    // Additional attribute to store the department name.
    private String dept_name, pi_name;

    public Group(String grp_id, String pi, String dept_id, String grp_name) {
        this.grp_id = grp_id;
        this.pi = pi;
        this.dept_id = dept_id;
        this.grp_name = grp_name;
        dept_name = DepartmentDB.getDeptName(dept_id);
        pi_name = UserAccountDB.getFullName(pi);
    }
    
    // Return the department name for this dept_id.
    public String getDept_name() {
        return dept_name;
    }
    // Return the name of the PI incharge of this group.
    public String getPi_name() {
        return pi_name;
    }
    
    // Machine generated getters and setters.
    public String getGrp_id() {
        return grp_id;
    }
    public void setGrp_id(String grp_id) {
        this.grp_id = grp_id;
    }
    public String getPi() {
        return pi;
    }
    public void setPi(String pi) {
        this.pi = pi;
    }
    public String getDept_id() {
        return dept_id;
    }
    public void setDept_id(String dept_id) {
        this.dept_id = dept_id;
    }
    public String getGrp_name() {
        return grp_name;
    }
    public void setGrp_name(String grp_name) {
        this.grp_name = grp_name;
    }
}
