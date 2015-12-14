/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
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
// Library for password hashing
import org.mindrot.jbcrypt.BCrypt;

/**
 * UserAccountDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the user_account table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 09-Oct-2015
 * 
 * Revision History
 * 09-Oct-2015 - First baseline with two static methods (checkPwd and 
 * insertAccount) created.
 * 13-Oct-2015 - Added new method getEmailAddress that return the email address
 * of the job requestor.
 * 04-Nov-2015 - Added the following new methods:
 * I. updateLastLogin to update the last login of the user.
 * II. updatePassword to allow user to change his/her password.
 * 05-Nov-2015 - Added new method getUser that return the user account of the
 * job requestor.
 * 06-Nov-2015 - Added the following methods to support the 'update user 
 * account' module:
 * 1. getAllUser()
 * 2. updateAccount(UserAccount user)
 * 13-Nov-2015 - Added one new variable userIDList and one new method 
 * getAllUserID(). Changed getAllUser() method to build up the userIDList.
 * 30-Nov-2015 - Implementation for database 2.0
 * 14-Dec-2015 - Changed the class to abstract. Added new method, getDeptID.
 */

public abstract class UserAccountDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(UserAccountDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private static List<UserAccount> userList = new ArrayList<>();
    private static LinkedHashMap<String,String> userIDList = new LinkedHashMap<>();
    
    UserAccountDB() {};
    
    // Return the list of all the user ID currently in the system.
    // This method should be called after getAllUser().
    public static LinkedHashMap<String,String> getAllUserID() {
        if (userList.isEmpty()) {
            getAllUser();
        }
        
        return userIDList;
    }
    
    // Return the list of user accounts that are currently in the system, and
    // build the list of user ID.
    public static List<UserAccount> getAllUser() {
        // Empty the current user list
        userList.clear();
        String queryStr = "SELECT * FROM user_account u NATURAL JOIN dept d "
                + "WHERE u.dept_id = d.dept_id ORDER BY u.user_id";
        ResultSet result = DBHelper.runQuery(queryStr);
        
        try {
            while (result.next()) {
                String user_id = result.getString("user_id");
                UserAccount user = new UserAccount(
                                        user_id,
                                        result.getInt("role_id"),
                                        result.getString("first_name"),
                                        result.getString("last_name"),
                                        result.getString("email"),
                                        result.getBoolean("active"),
                                        "password",
                                        result.getString("dept_id"),
                                        result.getString("inst_id"),
                                        result.getString("last_login"));
                
                userList.add(user);
                userIDList.put(user_id, user_id);
            }
            logger.debug("No of user account retrieved: " + userList.size());
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving user accounts from DB!");
            logger.error(e.getMessage());
        }
        
        return userList;
    }
    
    // Return the user account that requested this job.
    public static UserAccount getUser(int jobID) {
        UserAccount user = null;
        String queryStr = "SELECT * FROM user_account u NATURAL JOIN dept d "
                + "WHERE u.dept_id = d.dept_id AND u.user_id = (SELECT "
                + "user_id FROM submitted_job WHERE job_id = "
                + jobID + ")";
        ResultSet result = DBHelper.runQuery(queryStr);
        
        try {
            if (result.next()) {
                user = new UserAccount(result.getString("user_id"),
                                       result.getInt("role_id"),
                                       result.getString("first_name"),
                                       result.getString("last_name"),
                                       result.getString("email"),
                                       result.getBoolean("active"),
                                       "password",
                                       result.getString("dept_id"),
                                       result.getString("inst_id"),
                                       result.getString("last_login"));
                
            }
           
            logger.debug("For Job ID " + jobID + " User ID is " +
                    result.getString("user_id"));
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving user account for Job ID: " 
                        + jobID);
            logger.error(e.getMessage());
        }

        return user;
    }
    
    // Return the email address of the user that requested this job.
    public static String getEmailAddress(int jobID) {
        String queryStr = "SELECT email FROM user_account WHERE user_id = ("
                + "SELECT user_id FROM submitted_job WHERE job_id = "
                + jobID + ")";
        String email = null;
        ResultSet result = DBHelper.runQuery(queryStr);
        
        try {
            if (result.next()) {
                email = result.getString("email");
            }
           
            logger.debug("For Job ID " + jobID + " user email address is " +
                    email);
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving email address for Job ID: " 
                        + jobID);
            logger.error(e.getMessage());
        }
       
        return email;
    }
    
    // Check the password entered by the user, and if the password is valid, 
    // a UserAccount object will be return.
    public static UserAccount checkPwd(String user_id, String pwd) {
        String queryStr = "SELECT * FROM user_account u NATURAL JOIN dept d "
                + "WHERE u.dept_id = d.dept_id AND u.user_id = ?";
        String pwd_hash = null;
        UserAccount acct = null;

        try (PreparedStatement queryPwd = conn.prepareStatement(queryStr)) 
        {
            // Build the query condition using the parameter user_id.
            queryPwd.setString(1, user_id);
            ResultSet queryResult = queryPwd.executeQuery();
            
            if (queryResult.next()) {
                pwd_hash = queryResult.getString("pwd");
            }

            if (pwd_hash != null) {
                if (BCrypt.checkpw(pwd, pwd_hash)) {
                    logger.info(user_id + ": password valid.");
                    // Construct a UserAccount object based on the return result
                    // BUT set the password and last_login to default value.
                    acct = new UserAccount(queryResult.getString("user_id"),
                                        queryResult.getInt("role_id"),
                                        queryResult.getString("first_name"),
                                        queryResult.getString("last_name"),
                                        queryResult.getString("email"),
                                        queryResult.getBoolean("active"),
                                        "password",
                                        queryResult.getString("dept_id"),
                                        queryResult.getString("inst_id"),
                                        "last-login");
                }
            }
        } catch (SQLException e) {
            logger.error("SQLException at checkPwd.");
            logger.error(e.getMessage());
        } 
        
        // If acct is null, either password don't match or account
        // doesn't exists.
        if (acct == null) {
            logger.info(user_id + ": password invalid or account don't exist.");            
        }
        
        return acct;
    }
    
    // Insert the UserAccount object into the user_account table. Any exception 
    // encountered here will be throw and to be handled by the caller.
    public static void insertAccount(UserAccount newAcct) throws SQLException {
        // Hash the password using BCrypt before storing it into the database.
        String pwd_hash = BCrypt.hashpw(newAcct.getPwd(), BCrypt.gensalt());

        String insertStr = "INSERT INTO user_account"
                + "(user_id, role_id, first_name, last_name, email, pwd, "
                + "active, dept_id)" 
                + "VALUES (?,?,?,?,?,?,?,?)";
        PreparedStatement insertStm = conn.prepareStatement(insertStr);
        
        // Build the INSERT statement using the values from the current
        // UserAccount object.
        insertStm.setString(1, newAcct.getUser_id());
        insertStm.setInt(2, newAcct.getRole_id());
        insertStm.setString(3, newAcct.getFirst_name());
        insertStm.setString(4, newAcct.getLast_name());
        insertStm.setString(5, newAcct.getEmail());
        insertStm.setString(6, pwd_hash);
        insertStm.setBoolean(7, newAcct.getActive());
        insertStm.setString(8, newAcct.getDept_id());
        // Execute the INSERT statement
        insertStm.executeUpdate();
    }
    
    // Update the last login of this user.
    public static void updateLastLogin(String user_id, String last_login) {
        String updateStr = "UPDATE user_account SET last_login = ? WHERE "
                + "user_id = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setString(1, last_login);
            updateStm.setString(2, user_id);
            // Execute the UPDATE statement
            updateStm.executeUpdate();
        }
        catch (SQLException e) {
            logger.error("SQLException when trying to update last login of " +
                    user_id);
            logger.error(e.getMessage());
        }
    }
    
    // Update the password of this user. Any exception encountered here will 
    // be throw and to be handled by the caller.
    public static void updatePassword(String user_id, String new_pwd) 
            throws SQLException  {
        // Hash the password using BCrypt before storing it into the database.
        String pwd_hash = BCrypt.hashpw(new_pwd, BCrypt.gensalt());
        String updateStr = "UPDATE user_account SET pwd = ? WHERE "
                + "user_id = ?";
        
        PreparedStatement updateStm = conn.prepareStatement(updateStr);
        updateStm.setString(1, pwd_hash);
        updateStm.setString(2, user_id);
        // Execute the UPDATE statement
        updateStm.executeUpdate();
    }
    
    // Update the account detail of this user.
    public static void updateAccount(UserAccount user) throws SQLException {
        String updateStr = "UPDATE user_account SET dept_id = ?, "
                           + "first_name = ?, last_name = ?, "
                           + "email = ?, active = ?, role_id = ? WHERE "
                           + "user_id = ?";
        
        PreparedStatement updateStm = conn.prepareStatement(updateStr);
        updateStm.setString(1, user.getDept_id());
        updateStm.setString(2, user.getFirst_name());
        updateStm.setString(3, user.getLast_name());
        updateStm.setString(4, user.getEmail());
        updateStm.setBoolean(5, user.getActive());
        updateStm.setInt(6, user.getRole_id());
        updateStm.setString(7, user.getUser_id());
        // Excute the UPDATE statement
        updateStm.executeUpdate();
    }
    
    // Return the department ID for this user.
    public static String getDeptID(String userID) {
        String result = Constants.DATABASE_INVALID_STR;
        String queryStr = "SELECT dept_id FROM user_account WHERE user_id = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, userID);
            ResultSet rs = queryStm.executeQuery();
            
            if (rs.next()) {
                result = rs.getString("dept_id");
                logger.debug("Department ID for " + userID + " is " + result);
            }
        }
        catch (SQLException e) {
            logger.error("SQLException when query user_account for dept_id!");
            logger.error(e.getMessage());
        }
        
        return result;
    }
}
