/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * DepartmentDB is not mean to be instantiate, its main job is to perform
 * SQL operations on the department table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Created with all the standard getters and setters.
 * 16-Nov-2015 - Added 2 new methods, getAllDeptList() to get the full 
 * department list and getDeptHashMap(inst_code) to get the department list 
 * for the respective institution.
 */

public class DepartmentDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DepartmentDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private final static List<Department> deptList = new ArrayList<>();

    public DepartmentDB() {}
    
    // Return the full list of Department setup in the system.
    public static List<Department> getAllDeptList() {
        // Only execute the query if the list is empty.
        // To prevent the query from being run multiple times.
        if (deptList.isEmpty()) {
            ResultSet result = DBHelper.runQuery
                ("SELECT * FROM department ORDER BY institution_code");
            
            try {
                while (result.next()) {
                    Department dept = new Department(
                                        result.getString("institution_code"),
                                        result.getString("department_code"),
                                        result.getString("department_name"));
                    deptList.add(dept);
                }
            }
            catch (SQLException e) {
                logger.error("SQLException at getAllDept!");
                logger.error(e.getMessage());
            }
        }
        
        return deptList;
    }
    
    // Return the list of departments setup under the specific institution
    public static LinkedHashMap<String, String> getDeptHashMap
        (String inst_code) 
    {
        LinkedHashMap<String, String> departmentList = new LinkedHashMap<>();
        String queryStr = "SELECT department_code, department_name FROM "
                    + "department WHERE institution_code = ?";

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, inst_code);
            ResultSet result = queryStm.executeQuery();
            
            while (result.next()) {
                departmentList.put(result.getString("department_code"), 
                                   result.getString("department_name"));
            }
            logger.debug("Department list for " + inst_code + ": " +
                    departmentList.toString());
        }
        catch (SQLException e) {
            logger.error("SQLException at getDeptHashMap: " + inst_code);
            logger.error(e.getMessage());
        }
        
        return departmentList;
    }
}
