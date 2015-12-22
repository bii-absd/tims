/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
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
 * DepartmentDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the department table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Created with all the standard getters and setters.
 * 16-Nov-2015 - Added 2 new methods, getDeptList() to get the full 
 * department list and getDeptHashMap(inst_code) to get the department list 
 * for the respective institution.
 * 30-Nov-2015 - Implementation for database 2.0
 * 09-Dec-2015 - Added new method getDeptIDList, to return the list of dept_id 
 * setup under the specific institution. Added in the module for adding and
 * updating department info.
 * 11-Dec-2015 - Added new attribute deptHash, that stores the full list of
 * Department ID that have been setup in the system.
 * 22-Dec-2015 - Changed the class to abstract. To close the ResultSet after 
 * use.
 */

public abstract class DepartmentDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DepartmentDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private final static List<Department> deptList = new ArrayList<>();
    private final static LinkedHashMap<String,String> deptHash = new LinkedHashMap<>();

    // Clear the department list, so that it will be rebuild again.
    private static void clearDeptList() {
        deptList.clear();
    }
    
    // Return the full list of Department setup in the system, and setup the 
    // deptHash.
    public static List<Department> getDeptList() {
        // Only execute the query if the list is empty.
        // To prevent the query from being run multiple times.
        if (deptList.isEmpty()) {
            ResultSet rs = DBHelper.runQuery
                ("SELECT * FROM dept ORDER BY dept_id");
            
            try {
                while (rs.next()) {
                    Department dept = new Department(
                                        rs.getString("inst_id"),
                                        rs.getString("dept_id"),
                                        rs.getString("dept_name"));
                    deptList.add(dept);
                    deptHash.put(rs.getString("dept_id"), 
                                 rs.getString("dept_id"));
                }
                rs.close();
            }
            catch (SQLException e) {
                logger.error("SQLException at getAllDept!");
                logger.error(e.getMessage());
            }
        }
        
        return deptList;
    }
    
    // Insert the new department ID into database.
    public static Boolean insertDepartment(Department dept) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO dept(dept_id,inst_id,dept_name) VALUES(?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, dept.getDept_id());
            insertStm.setString(2, dept.getInst_id());
            insertStm.setString(3, dept.getDept_name());
            insertStm.executeUpdate();
            // Clear and rebuild the department list.
            clearDeptList();
            getDeptList();
            logger.debug("New department ID inserted into database: " + 
                    dept.getDept_id());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting department ID!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Update the department information in the database.
    public static Boolean updateDepartment(Department dept) {
        Boolean result = Constants.OK;
        String updateStr = "UPDATE dept SET inst_id = ?, dept_name = ? "
                         + "WHERE dept_id = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setString(1, dept.getInst_id());
            updateStm.setString(2, dept.getDept_name());
            updateStm.setString(3, dept.getDept_id());
            
            updateStm.executeUpdate();
            // Clear and rebuild the department list.
            clearDeptList();
            getDeptList();
            logger.debug("Department " + dept.getDept_id() + " updated.");
        }
        catch (SQLException e) {
            logger.error("SQLException when updating department!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Return the list of dept_id setup under this inst_id.
    public static List<String> getDeptIDList(String inst_id) {
        List<String> deptIDList = new ArrayList<>();
        String queryStr = "SELECT dept_id FROM dept WHERE inst_id = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, inst_id);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                deptIDList.add(rs.getString("dept_id"));
            }
            logger.debug("Department list for instituion " + inst_id + " is " 
                    + deptIDList.toString());
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving department ID!");
            logger.error(e.getMessage());
        }
        
        return deptIDList;
    }
    
    // Return the HashMap of departments setup under the specific inst_id.
    public static LinkedHashMap<String, String> getDeptHash(String inst_id) {
        LinkedHashMap<String, String> deptHashMap = new LinkedHashMap<>();
        String queryStr = "SELECT dept_id, dept_name FROM "
                        + "dept WHERE inst_id = ?";

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, inst_id);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                deptHashMap.put(rs.getString("dept_id"), 
                                rs.getString("dept_id"));
            }
            logger.debug("Department list for " + inst_id + ": " +
                    deptHashMap.toString());
        }
        catch (SQLException e) {
            logger.error("SQLException when query department!");
            logger.error(e.getMessage());
        }
        
        return deptHashMap;
    }

    // Return the HashMap of all the department IDs setup in the system.
    public static LinkedHashMap<String, String> getAllDeptHash() {
        return deptHash;
    }
}
