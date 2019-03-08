// Copyright (C) 2019 A*STAR
//
// TIMS (Translation Informatics Management System) is an software effort 
// by the ABSD (Analytics of Biological Sequence Data) team in the 
// Bioinformatics Institute (BII), Agency of Science, Technology and Research 
// (A*STAR), Singapore.
//

// This file is part of TIMS.
// 
// TIMS is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as 
// published by the Free Software Foundation, either version 3 of the 
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
package TIMS.Database;

import TIMS.General.Constants;
// Libraries for Java.
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

public class GroupDB implements Serializable {
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
    
    // Return the HashMap of all the group IDs (which are active and with pi 
    // setup) in the system. Used in Study Management Edit function.
    public LinkedHashMap<String, String> getActiveGrpHashWithPI() {
        String query = "SELECT grp_id, grp_name FROM grp "
                     + "WHERE pi IS NOT NULL AND active = true ORDER BY grp_id";

        return getGrpHash(query);
    }
    /* NOT IN USE!
    // Return the HashMap of all the group IDs setup in the system.
    public static LinkedHashMap<String, String> getAllGrpHash() {
        String query = "SELECT grp_id, grp_name FROM grp ORDER BY grp_id";
        
        return getGrpHash(query);
    }
    */
    // Return the HashMap of all the group IDs setup under this institution in
    // the system.
    public LinkedHashMap<String, String> getGrpHashForInst(String instID) {
        String query = "SELECT grp_id, grp_name FROM inst_dept_grp "
                     + "WHERE inst_id = \'" + instID + "\' ORDER BY grp_id";
        
        return getGrpHash(query);
    }
    // Helper function to return the HashMap of group IDs setup in the system
    // using the query passed in.
    private LinkedHashMap<String, String> getGrpHash(String query) {
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
            logger.error("FAIL to retrieve group ID map!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return allGrpHash;
    }
    
    // Return the list of group IDs setup in the system using the query passed in.
    public static List<String> getGrpIDList(String query) {
        List<String> grpIDList = new ArrayList<>();
        Connection conn = null;

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                grpIDList.add(rs.getString(1));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve list of group ID!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return grpIDList;
    }
    
    /* NOT IN USE!
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
    */
    
    // Return the list of Group setup in the system. The list will be used in
    // the Group Management view.
    public List<Group> getFullGrpList() {
        String query = "SELECT * FROM grp ORDER BY grp_id";
        
        return getGrpList(query);
    }
    // Return the Group for this group ID. Used in Account Management view.
    public Group getGrpByGrpID(String grp_id) {
        String query = "SELECT * FROM grp WHERE grp_id = \'" + grp_id + "\'";
        // There should only be one item in the list returned.
        return getGrpList(query).get(0);
    }
    // Helper function to retrieve the group list from the database using the 
    // query passed in.
    private List<Group> getGrpList(String query) {
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
                                      rs.getString("grp_name"),
                                      rs.getBoolean("active"));
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
    
    /* NOT IN USE!
    // Return the list of group hierarchy structure (together with its leading
    // PI).
    public static List<InstDeptGrp> getInstDeptGrpList() {
        List<InstDeptGrp> hierarchyList = new ArrayList<>();
        Connection conn = null;
        String query = "SELECT * FROM inst_dept_grp";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                InstDeptGrp tmp = new InstDeptGrp(rs.getString("inst_id"),
                                                  rs.getString("inst_name"),
                                                  rs.getString("dept_id"),
                                                  rs.getString("dept_name"),
                                                  rs.getString("grp_id"),
                                                  rs.getString("grp_name"),
                                                  rs.getString("pi"),
                                                  rs.getBoolean("active"));
                
                hierarchyList.add(tmp);
            }
            logger.info("Retrieved group hierarchy list.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve the group hierarchy list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return hierarchyList;
    }
    */
    
    // Return the list of active group ID (with pi setup) under this department.
    public List<String> getActiveGrpIDListByDept(String dept_id) {
        return new ArrayList<>(getActiveGrpHashByDept(dept_id).values());
    }
    // Return the HashMap of active group ID (with pi setup) under this department.
    public LinkedHashMap<String, String> getActiveGrpHashByDept(String dept_id) 
    {
        Connection conn = null;
        LinkedHashMap<String, String> grpHash = new LinkedHashMap<>();
        String query = "SELECT grp_id FROM grp WHERE pi IS NOT NULL "
                     + "AND active = true AND dept_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dept_id);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                grpHash.put(rs.getString("grp_id"), rs.getString("grp_id"));
            }
            stm.close();
            StringBuilder oper = new StringBuilder("Active group list for ").
                    append(dept_id).append(": ").append(grpHash.toString());
            logger.info(oper);
//            logger.debug("Active group list for " + dept_id + ": " + grpHash.toString());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query active group list for " + dept_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return grpHash;
    }
    
    // Insert the new group ID into database.
    public boolean insertGroup(Group grp) {
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
            logger.info("Group ID inserted into database: " + grp.getGrp_id());
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
    public boolean updateGroup(Group grp) {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "UPDATE grp SET pi = ?, dept_id = ?, grp_name = ?, "
                     + "active = ? WHERE grp_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, grp.getPi());
            stm.setString(2, grp.getDept_id());
            stm.setString(3, grp.getGrp_name());
            stm.setBoolean(4, grp.isActive());
            stm.setString(5, grp.getGrp_id());
            stm.executeUpdate();
            stm.close();
            logger.info(grp.getGrp_id() + " updated.");
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
    private static String getGrpPropValue(String grp_id, String property) {
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
            StringBuilder err = new StringBuilder("FAIL to retrieve ").
                    append(property).append(" for group ").append(grp_id);
            logger.error(err);
//            logger.error("FAIL to retrieve " + property + " for group " + grp_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return propValue;
    }
    
    // Retrieve the institution ID that this group belongs to.
    public String getInstIDForGrp(String grp_id) {
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
