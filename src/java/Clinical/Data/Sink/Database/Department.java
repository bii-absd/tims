/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Database;

/**
 * Department is used to represent the dept table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Created with all the standard getters and setters.
 * 01-Dec-2015 - Implementation for database 2.0
 * 09-Dec-2015 - Added new attribute, inst_name.
 * 13-Dec-2016 - Removed all the static variables in Study and ItemList
 * management modules.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 28-Mar-2016 - To retrieve the institution name from the DepartmentDB class
 * instead of InstitutionDB class.
 * 08-Apr-2016 - Bug fix: inst_name being setup with institution ID value.
 */

public class Department {
    // dept table fields
    private String inst_id, dept_id, dept_name;
    // Additional attribute to store the institution name
    private final String inst_name;

    public Department(String inst_id, String dept_id, 
            String dept_name) {
        this.inst_id = inst_id;
        this.dept_id = dept_id;
        this.dept_name = dept_name;
        inst_name = InstitutionDB.getInstNameFromHash(inst_id);
    }

    // Return the institution name for this inst_id.
    public String getInst_name() {
        return inst_name;
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
