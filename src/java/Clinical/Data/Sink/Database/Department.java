/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

/**
 * Department is used to represent the department table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Created with all the standard getters and setters.
 * 01-Dec-2015 - Implementation for database 2.0
 * 09-Dec-2015 - Added new attribute, inst_name.
 */

public class Department {
    // department table fields
    private String inst_id, dept_id, dept_name;
    // Addition attribute to store the institution name
    private String inst_name;

    public Department(String inst_id, String dept_id, 
            String dept_name) {
        this.inst_id = inst_id;
        this.dept_id = dept_id;
        this.dept_name = dept_name;
    }

    // Return the institution name for this inst_id.
    public String getInst_name() {
        return InstitutionDB.getInstName(inst_id);
    }
    
    // Machine generated getters and setters
    public String getInst_id() {
        return inst_id;
    }
    public void setInst_id(String inst_id) {
        this.inst_id = inst_id;
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
