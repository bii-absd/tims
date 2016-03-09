/*
 * Copyright @2016
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
 * GroupDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the grp table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 04-Mar-2016
 * 
 * Revision History
 * 04-Mar-2016 - Created with all the standard getters and setters.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 */

public abstract class GroupDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(GroupDB.class.getName());

    // Return true if this PI lead any group else return false.
    public static boolean isPILead(String piID) {
        Connection conn = null;
        String query = "SELECT grp_id FROM grp WHERE pi = ?";
        boolean result = Constants.NOT_OK;

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, piID);
            ResultSet rs = stm.executeQuery();            
            result = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve grp_id for " + piID);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return result;
    }
    
    // Return the HashMap of all the group IDs (with pi setup) in the system.
    public static LinkedHashMap<String, String> getGrpWithPIHash() {
        String query = "SELECT grp_id, grp_name FROM grp "
                     + "WHERE pi IS NOT NULL ORDER BY grp_id";

        return getGrpHash(query);
    }
    // Return the HashMap of all the group IDs setup in the system.
    public static LinkedHashMap<String, String> getAllGrpHash() {
        String query = "SELECT grp_id, grp_name FROM grp ORDER BY grp_id";
        
        return getGrpHash(query);
    }
    
    // Helper function to return the HashMap of group IDs setup in the system
    // using the query passed in.
    public static LinkedHashMap<String, String> getGrpHash(String query) {
        Connection conn = null;
        LinkedHashMap<String, String> allGrpHash = new LinkedHashMap<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                allGrpHash.put(rs.getString("grp_name"), rs.getString("grp_id"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve full list of group ID!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return allGrpHash;
    }
    
    // Return the list of Group setup in the system under this institution.
    public static List<Group> getGrpListByInst(String inst_id) {
        String query = "SELECT * FROM grp WHERE dept_id IN "
                     + "(SELECT dept_id FROM dept WHERE inst_id = \'" 
                     + inst_id + "\') ORDER BY dept_id, grp_id";
                
        return getGrpList(query);
    }
    // Return the list of Group setup in the system under this department.
    public static List<Group> getGrpListByDept(String dept_id) {
        String query = "SELECT * FROM grp WHERE dept_id = \'" 
                     + dept_id + "\' ORDER BY grp_id";
        
        return getGrpList(query);
    }    
    // Return the list of Group setup in the system. The list will be used in
    // the Group Management view.
    public static List<Group> getFullGrpList() {
        String query = "SELECT * FROM grp ORDER BY grp_id";
        
        return getGrpList(query);
    }
    
    // Helper function to retrieve the group list from the database using the 
    // query passed in.
    public static List<Group> getGrpList(String query) {
        Connection conn = null;
        List<Group> grpList = new ArrayList<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                Group grp = new Group(rs.getString("grp_id"),
                                      rs.getString("pi"),
                                      rs.getString("dept_id"),
                                      rs.getString("grp_name"));
                grpList.add(grp);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to build group list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return grpList;
    }
    
    // Return the list of group ID setup under this department.
    public static List<String> getGrpIDListByDept(String dept_id) {
        return new ArrayList<>(getGrpHashByDept(dept_id).values());
    }    
    // Return the HashMap of group ID setup under this department.
    public static LinkedHashMap<String, String> getGrpHashByDept(String dept_id) {
        Connection conn = null;
        LinkedHashMap<String, String> grpHash = new LinkedHashMap<>();
        String query = "SELECT grp_id FROM grp WHERE dept_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dept_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                grpHash.put(rs.getString("grp_id"), rs.getString("grp_id"));
            }
            
            stm.close();
            logger.debug("Group list for " + dept_id + ": " + grpHash.toString());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query group list for " + dept_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return grpHash;
    }
    
    // Insert the new group ID into database.
    public static boolean insertGroup(Group grp) {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "INSERT INTO grp(grp_id,pi,dept_id,grp_name) VALUES(?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, grp.getGrp_id());
            stm.setString(2, grp.getPi());
            stm.setString(3, grp.getDept_id());
            stm.setString(4, grp.getGrp_name());
            stm.executeUpdate();
            stm.close();
            logger.debug("New group ID inserted into database: " + grp.getGrp_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert new group " + grp.getGrp_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update the group information in the database.
    public static boolean updateGroup(Group grp) {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "UPDATE grp SET pi = ?, dept_id = ?, grp_name = ? "
                     + "WHERE grp_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, grp.getPi());
            stm.setString(2, grp.getDept_id());
            stm.setString(3, grp.getGrp_name());
            stm.setString(4, grp.getGrp_id());
            stm.executeUpdate();
            stm.close();
            logger.debug("Group " + grp.getGrp_id() + " updated.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update group " + grp.getGrp_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Retrieve the PI user ID for this group.
    public static String getGrpPIID(String grp_id) {
        return getGrpPropValue(grp_id, "pi");
    }
    // Retrieve the department ID that this group belongs to.
    public static String getGrpDeptID(String grp_id) {
        return getGrpPropValue(grp_id, "dept_id");
    }
    // Retrieve the group name for this group.
    public static String getGrpName(String grp_id) {
        return getGrpPropValue(grp_id, "grp_name");
    }
    
    // Helper function to retrieve one of the group's property value.
    public static String getGrpPropValue(String grp_id, String property) {
        Connection conn = null;
        String propValue = Constants.DATABASE_INVALID_STR;
        String query = "SELECT * FROM grp WHERE grp_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, grp_id);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                // Retrieve the requested property value.
                propValue = rs.getString(property);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve " + property + " for group " + grp_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return propValue;
    }
    
    // Retrieve the institution ID that this group belongs to.
    public static String getGrpInstID(String grp_id) {
        Connection conn = null;
        String inst_id = Constants.DATABASE_INVALID_STR;
        String query = "SELECT inst_id FROM inst_dept_grp WHERE grp_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, grp_id);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                inst_id = rs.getString("inst_id");
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve institution ID for group " + grp_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return inst_id;
    }
}
