/*
 * Copyright @2015-2016
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
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * DepartmentDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the dept table in the database.
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
 * 13-Dec-2016 - Removed all the static variables in Study and ItemList
 * management modules.
 * 18-Feb-2016 - Added new method getDeptInstID, to retrieve the institution ID 
 * from the dept table.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 */

public abstract class DepartmentDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DepartmentDB.class.getName());

    // Return the full list of Department setup in the system.
    public static List<Department> getDeptList() {
        Connection conn = null;
        String query = "SELECT * FROM dept ORDER BY inst_id, dept_id";
        List<Department> deptList = new ArrayList<>();

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                Department dept = new Department(
                                        rs.getString("inst_id"),
                                        rs.getString("dept_id"),
                                        rs.getString("dept_name"));
                deptList.add(dept);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to build full department list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return deptList;
    }
    
    // Insert the new department ID into database.
    public static Boolean insertDepartment(Department dept) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO dept(dept_id,inst_id,dept_name) VALUES(?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dept.getDept_id());
            stm.setString(2, dept.getInst_id());
            stm.setString(3, dept.getDept_name());
            stm.executeUpdate();
            stm.close();
            logger.debug("New department ID inserted into database: " + 
                    dept.getDept_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert new department " + dept.getDept_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return result;
    }
    
    // Update the department information in the database.
    public static Boolean updateDepartment(Department dept) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE dept SET inst_id = ?, dept_name = ? "
                     + "WHERE dept_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dept.getInst_id());
            stm.setString(2, dept.getDept_name());
            stm.setString(3, dept.getDept_id());
            stm.executeUpdate();
            stm.close();
            logger.debug("Department " + dept.getDept_id() + " updated.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update department " + dept.getDept_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Return the list of department ID setup under this institution.
    public static List<String> getDeptIDList(String inst_id) {
        return new ArrayList<>(getDeptHash(inst_id).values());
    }
    
    // Return the HashMap of department ID setup under this institution.
    public static LinkedHashMap<String, String> getDeptHash(String inst_id) {
        Connection conn = null;
        LinkedHashMap<String, String> deptHash = new LinkedHashMap<>();
        String query = "SELECT dept_id FROM dept WHERE inst_id = ?";

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, inst_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                deptHash.put(rs.getString("dept_id"), 
                             rs.getString("dept_id"));
            }
            
            stm.close();
            logger.debug("Department list for " + inst_id + ": " +
                    deptHash.toString());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query department for " + inst_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return deptHash;
    }

    // Return the HashMap of all the department IDs setup in the system.
    public static LinkedHashMap<String, String> getAllDeptHash() {
        Connection conn = null;
        String query = "SELECT dept_id, dept_name FROM dept ORDER BY dept_id";
        LinkedHashMap<String, String> allDeptHash = new LinkedHashMap<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                allDeptHash.put(rs.getString("dept_name"), 
                                rs.getString("dept_id"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve full list of department ID!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return allDeptHash;
    }
    
    // Return the name for this department.
    public static String getDeptName(String dept_id) {
        return getDeptPropValue(dept_id, "dept_name");
    }    
    // Retrieve the institution ID that this department belongs to.
    public static String getDeptInstID(String dept_id) {
        return getDeptPropValue(dept_id, "inst_id");
    }
    
    // Helper function to retrieve one of the department's property value.
    public static String getDeptPropValue(String dept_id, String property) {
        Connection conn = null;
        String propValue = Constants.DATABASE_INVALID_STR;
        String query = "SELECT * FROM dept WHERE dept_id = ?";

        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dept_id);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                // Retrieve the requested property value.
                propValue = rs.getString(property);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve " + property + "for department " + dept_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return propValue;
    }
}
