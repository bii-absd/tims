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
 */

public abstract class GroupDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(GroupDB.class.getName());

    // Return the list of Group setup in the system.
    public static List<Group> getFullGrpList() {
        Connection conn = null;
        String query = "SELECT * FROM grp ORDER BY grp_id";
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
            logger.error("FAIL to build full group list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return grpList;
    }
    
    // Return the list of Group setup in the system under this department.
    public static List<Group> getGrpListByDept(String dept_id) {
        Connection conn = null;
        List<Group> grpList = new ArrayList<>();
        
        
        return grpList;
    }
    
    // Return the list of Group setup in the system under this institution.
    public static List<Group> getGrpListByInst(String inst_id) {
        Connection conn = null;
        List<Group> grpList = new ArrayList<>();
        
        return grpList;
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
    
    // Retrieve the department ID that this group belongs to.
    public static String getDeptID(String grp_id) {
        Connection conn = null;
        String dept_id = Constants.DATABASE_INVALID_STR;
        
        return dept_id;
    }
}
