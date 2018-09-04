/*
 * Copyright @2018
 */
package TIMS.Database;

/**
 * WorkUnitDB is use to group the database accessors for institution, 
 * department and group tables. This class is use in StudyManagementBean,
 * AccountManagementBean and AuthenticationBean.
 * 
 * Author: Tay Wei Hong
 * Date: 03-Sep-2018
 * 
 * Revision History
 * 03-Sep-2018 - Created with the standard getters and constructor.
 */

public class WorkUnitDB {
    private final InstitutionDB instDB;
    private final DepartmentDB deptDB;
    private final GroupDB grpDB;

    // Machine generated code.
    public WorkUnitDB() {
        instDB = new InstitutionDB();
        deptDB = new DepartmentDB();
        grpDB  = new GroupDB();
    }
    public InstitutionDB getInstDB() {
        return instDB;
    }
    public DepartmentDB getDeptDB() {
        return deptDB;
    }
    public GroupDB getGrpDB() {
        return grpDB;
    }
}
