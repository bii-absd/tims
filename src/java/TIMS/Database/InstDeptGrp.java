/*
 * Copyright @2016
 */
package TIMS.Database;

/**
 * InstDeptGrp is used to represent the inst_dept_grp view in the database. The
 * objects from this class will be used to present the group hierarchy structure
 * together with the leading PI in the groupmanagement page.
 * 
 * Author: Tay Wei Hong
 * Date: 04-Apr-2016
 * 
 * Revision History
 * 04-Apr-2016 - Created with all the standard getters and setters.
 */

public class InstDeptGrp {
    // inst_dept_grp view attributes.
    private final String inst_id, inst_name, dept_id, dept_name, 
                         grp_id, grp_name, pi;
    private final boolean active;
    
    // Machine generated constructor.
    public InstDeptGrp(String inst_id, String inst_name, String dept_id, 
                       String dept_name, String grp_id, String grp_name, 
                       String pi, boolean active) 
    {
        this.inst_id = inst_id;
        this.inst_name = inst_name;
        this.dept_id = dept_id;
        this.dept_name = dept_name;
        this.grp_id = grp_id;
        this.grp_name = grp_name;
        this.pi = pi;
        this.active = active;
    }

    // Return the group active status.
    public String getActiveStatus() {
        return active?"Active":"Inactive";
    }
    
    // Machine generated getters.
    public String getInst_id() {
        return inst_id;
    }
    public String getInst_name() {
        return inst_name;
    }
    public String getDept_id() {
        return dept_id;
    }
    public String getDept_name() {
        return dept_name;
    }
    public String getGrp_id() {
        return grp_id;
    }
    public String getGrp_name() {
        return grp_name;
    }
    public String getPi() {
        return pi;
    }
    public boolean isActive() {
        return active;
    }
}
