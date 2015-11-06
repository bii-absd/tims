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
import java.util.LinkedHashMap;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * UserRole is used to represent the user_role table in the database.
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
 */

public class UserRole implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(UserRole.class.getName());
    private final static LinkedHashMap<String, Integer> roleList = 
                                                        new LinkedHashMap<>();
    private final static LinkedHashMap<Integer, String> roleIDList = 
                                                        new LinkedHashMap<>();
    private final static Connection conn = DBHelper.getDBConn();
    
    public UserRole() {}

    // Return the list of Role setup in the database
    public static LinkedHashMap<String, Integer> getRoleList() {
        // We will only build the roleList once
        if (roleList.isEmpty()) {
            String queryStr = "SELECT * from user_role ORDER BY role_id";
            
            try (PreparedStatement queryRole = conn.prepareStatement(queryStr))
            {
                ResultSet queryResult = queryRole.executeQuery();
                
                while (queryResult.next()) {
                    // Build the 2 Hash Map; One is Role ID -> Role, the other
                    // is Role -> Role ID.
                    roleList.put(queryResult.getString("role"),
                            queryResult.getInt("role_id"));
                    roleIDList.put(queryResult.getInt("role_id"), 
                            queryResult.getString("role"));
                }
                logger.debug("Role List: " + roleList.toString());
            } catch (SQLException ex) {
                logger.error("SQLException at getRoleList.");
                logger.error(ex.getMessage());
            }
        }
        
        return roleList;
    }
    
    // Return the Role ID using the value stored in HashMap roleList (Don't 
    // need to access the database).
    public static int getRoleIDFromHash(String role) {
        if (roleList.isEmpty()) {
            return Constants.DATABASE_INVALID_ID;
        }
        return roleList.get(role);            
    }
    // Return the Role using the value stored in HashMap roleIDList (Don't 
    // need to access the database).
    public static String getRoleFromHash(int roleID) {
        if (roleIDList.isEmpty()) {
            return Constants.DATABASE_INVALID_STR;
        }
        return roleIDList.get(roleID);
    }
    
    // Return the role_id for the role passed in.
    public static int getRoleID(String role) throws SQLException {
        String queryStr = "SELECT role_id from user_role WHERE role = ?";
        ResultSet queryResult = null;
        
        try {
            PreparedStatement queryRoleID = conn.prepareStatement(queryStr);
            queryRoleID.setString(1, role);
            queryResult = queryRoleID.executeQuery();
            
            if (queryResult.next()) {
                return queryResult.getInt("role_id");
            }
        } catch (SQLException ex) {
            logger.error("SQLException at getRoleID.");
            logger.error(ex.getMessage());
        } finally {
            if (queryResult != null) {
                queryResult.close();
            }
        }
        
        logger.debug("Role " + role + " is not found in the database.");
        return Constants.DATABASE_INVALID_ID;
    }
    
    // Return the role for the roleID passed in.
    public static String getRole(int roleID) throws SQLException {
        String queryStr = "SELECT role from user_role WHERE role_id = ?";
        ResultSet queryResult = null;
        
        try {
            PreparedStatement queryRoleID = conn.prepareStatement(queryStr);
            queryRoleID.setInt(1, roleID);
            queryResult = queryRoleID.executeQuery();
            
            if (queryResult.next()) {
                return queryResult.getString("role");
            }            
        } catch (SQLException ex) {
            logger.error("SQLException at getRoleID.");
            logger.error(ex.getMessage());
        } finally {
            if (queryResult != null) {
                queryResult.close();
            }
        }
        logger.debug("Role ID " + String.valueOf(roleID) + 
                " is not found in the database.");
        return Constants.DATABASE_INVALID_STR;
    }
}