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
 */

public class Department {
    // department table fields
    private String institution_code, department_code, department_name;

    public Department(String institution_code, String department_code, 
            String department_name) {
        this.institution_code = institution_code;
        this.department_code = department_code;
        this.department_name = department_name;
    }

    // Machine generated getters and setters
    public String getInstitution_code() {
        return institution_code;
    }
    public void setInstitution_code(String institution_code) {
        this.institution_code = institution_code;
    }
    public String getDepartment_code() {
        return department_code;
    }
    public void setDepartment_code(String department_code) {
        this.department_code = department_code;
    }
    public String getDepartment_name() {
        return department_name;
    }
    public void setDepartment_name(String department_name) {
        this.department_name = department_name;
    }   
}
