/*
 * Copyright @2015-2018
 */
package TIMS.Database;

import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for Trove
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * UserRoleDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the user_role table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 23-Sep-2015
 * 
 * Revision History
 * 23-Sep-2015 - Created with all the standard getters and setters.
 * 25-Sep-2015 - Added in two methods (getRoleID and getRole).
 * 07-Oct-2015 - Changed dbHandle to static. Added comment for the code. 
 * Added Log4j2 for this class. Changed to connection based for database access.
 * 23-Oct-2015 - Added new function that return the list of Role setup in the
 * database. Added 2 Hash Map to store the list of Role and Role ID settings.
 * 04-Nov-2015 - Port to JSF 2.2
 * 06-Nov-2015 - Updated the query statement for getRoleList.
 * 13-Nov-2015 - Changes the class name from UserRole to UserRoleDB.
 * 30-Nov-2015 - Implementation for database 2.0
 * 11-Dec-2015 - Changed to abstract class. Removed unused code.
 * 22-Dec-2015 - To close the ResultSet after use.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 07-Apr-2016 - Added new method, isLead() to check whether the role ID passed
 * in belong to a lead or not.
 * 26-Nov-2018 - Added a new role 'Guest'.
 */

public abstract class UserRoleDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(UserRoleDB.class.getName());
//    private final static LinkedHashMap<String, Integer> 
//            roleNameHash = new LinkedHashMap<>();
    private final static TObjectIntHashMap<String> roleNameHash = 
            new TObjectIntHashMap<String>();
//    private final static LinkedHashMap<Integer, String> 
//            roleIDHash = new LinkedHashMap<>();
    private final static TIntObjectHashMap<String> roleIDHash = 
            new TIntObjectHashMap<String>();
    
    // Return the list of Role setup in the database
//    public static LinkedHashMap<String, Integer> getRoleNameHash() {
    public static TObjectIntHashMap<String> getRoleNameHash() {
        // We will only build the roleList once
        if (roleNameHash.isEmpty()) {
            Connection conn = null;
            String query = "SELECT * from user_role ORDER BY role_id";
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
            
                while (rs.next()) {
                    // Build the 2 Hash Map; One is Role ID -> Role Name, 
                    // the other is Role Name -> Role ID.
                    roleNameHash.put(rs.getString("role_name"),
                                     rs.getInt("role_id"));
                    roleIDHash.put(rs.getInt("role_id"), 
                                   rs.getString("role_name"));
                }
                stm.close();
            } 
            catch (SQLException|NamingException e) {
                logger.error("FAIL to query user role!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
        
        return roleNameHash;
    }
    
    // Helper function to return the Role ID from the role name map.
    private static int getRoleIDFromHash(String roleName) {
        if (roleNameHash.isEmpty()) {
            return Constants.DATABASE_INVALID_ID;
        }
        return roleNameHash.get(roleName);            
    }
    
    // Return the Role using the value stored in HashMap roleIDHash.
    public static String getRoleNameFromHash(int roleID) {
        if (roleIDHash.isEmpty()) {
            return Constants.DATABASE_INVALID_STR;
        }
        return roleIDHash.get(roleID);
    }
    
    // Return the role ID for each role defined in the system.
    public static int admin() {
        return getRoleIDFromHash("Admin");
    }
    public static int director() {
        return getRoleIDFromHash("Director");
    }
    public static int hod() {
        return getRoleIDFromHash("HOD");
    }
    public static int pi() {
        return getRoleIDFromHash("PI");
    }
    public static int user() {
        return getRoleIDFromHash("User");
    }
    public static int guest() {
        return getRoleIDFromHash("Guest");
    }
    
    // Return true if the role ID passed in is a Director/HOD/PI.
    public static boolean isLead(int roleID) {
        return (roleID == director()) || (roleID == hod()) || (roleID == pi());
    }
}
