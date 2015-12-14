/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
 */

public abstract class UserRoleDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(UserRoleDB.class.getName());
    private final static LinkedHashMap<String, Integer> 
            roleNameList = new LinkedHashMap<>();
    private final static LinkedHashMap<Integer, String> 
            roleIDList = new LinkedHashMap<>();
    
    public UserRoleDB() {}

    // Return the list of Role setup in the database
    public static LinkedHashMap<String, Integer> getRoleNameList() {
        // We will only build the roleList once
        if (roleNameList.isEmpty()) {
            ResultSet result = DBHelper.runQuery
                    ("SELECT * from user_role ORDER BY role_id");
            try {
                while (result.next()) {
                    // Build the 2 Hash Map; One is Role ID -> Role Name, 
                    // the other is Role Name -> Role ID.
                    roleNameList.put(result.getString("role_name"),
                            result.getInt("role_id"));
                    roleIDList.put(result.getInt("role_id"), 
                            result.getString("role_name"));
                }
                logger.debug("Role List: " + roleNameList.toString());
            } catch (SQLException e) {
                logger.error("SQLException when retrieving from user_role table!");
                logger.error(e.getMessage());
            }
        }
        
        return roleNameList;
    }
    
    // Return the Role ID using the value stored in HashMap roleList.
    public static int getRoleIDFromHash(String roleName) {
        if (roleNameList.isEmpty()) {
            return Constants.DATABASE_INVALID_ID;
        }
        return roleNameList.get(roleName);            
    }
    
    // Return the Role using the value stored in HashMap roleIDList.
    public static String getRoleNameFromHash(int roleID) {
        if (roleIDList.isEmpty()) {
            return Constants.DATABASE_INVALID_STR;
        }
        return roleIDList.get(roleID);
    }    
}
