/*
 * Copyright @2015-2016
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
 * 13-Nov-2015 - Added one new variable userIDHash and one new method 
 * getAllUserID().
 * 30-Nov-2015 - Implementation for database 2.0
 * 14-Dec-2015 - Changed the class to abstract. Added new method, getDeptID.
 * 22-Dec-2015 - To close the ResultSet after use.
 * 11-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Jan-2016 - Removed all the static variables in Account Management module.
 * 18-Jan-2016 - Added new method, getAdminEmails() to retrieve all the 
 * administrator email addresses.
 */

public abstract class UserAccountDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(UserAccountDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    
    // Return the list of all the user ID currently in the system.
    public static LinkedHashMap<String,String> getUserIDHash() {
        LinkedHashMap<String,String> userIDHash = new LinkedHashMap<>();
        ResultSet rs = DBHelper.runQuery(
                "SELECT user_id FROM user_account ORDER BY user_id");
        
        try {
            while (rs.next()) {
                String user_id = rs.getString("user_id");
                userIDHash.put(user_id, user_id);
            }
            rs.close();
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve user ID!");
            logger.error(e.getMessage());
        }
        
        return userIDHash;
    }
    
    // Return the list of user accounts that are currently in the system.
    public static List<UserAccount> getAllUserAcct() {
        List<UserAccount> userAcctList = new ArrayList<>();
        String queryStr = "SELECT * FROM user_account u NATURAL JOIN dept d "
                        + "WHERE u.dept_id = d.dept_id ORDER BY u.user_id";
        ResultSet rs = DBHelper.runQuery(queryStr);
        
        try {
            while (rs.next()) {
                UserAccount user = new UserAccount(
                                        rs.getString("user_id"),
                                        rs.getInt("role_id"),
                                        rs.getString("first_name"),
                                        rs.getString("last_name"),
                                        rs.getString("email"),
                                        rs.getBoolean("active"),
                                        "password",
                                        rs.getString("dept_id"),
                                        rs.getString("inst_id"),
                                        rs.getString("last_login"));
                
                userAcctList.add(user);
            }
            rs.close();
            logger.debug("No of user account retrieved: " + userAcctList.size());
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve user accounts!");
            logger.error(e.getMessage());
        }
        
        return userAcctList;
    }
    
    // Return the user account that requested this job.
    public static UserAccount getJobRequestor(int jobID) {
        UserAccount user = null;
        String queryStr = "SELECT * FROM user_account u NATURAL JOIN dept d "
                + "WHERE u.dept_id = d.dept_id AND u.user_id = (SELECT "
                + "user_id FROM submitted_job WHERE job_id = "
                + jobID + ")";
        ResultSet rs = DBHelper.runQuery(queryStr);
        
        try {
            if (rs.next()) {
                user = new UserAccount(rs.getString("user_id"),
                                       rs.getInt("role_id"),
                                       rs.getString("first_name"),
                                       rs.getString("last_name"),
                                       rs.getString("email"),
                                       rs.getBoolean("active"),
                                       "password",
                                       rs.getString("dept_id"),
                                       rs.getString("inst_id"),
                                       rs.getString("last_login"));
                
            }
//            rs.close();
            logger.debug("For Job ID " + jobID + " User ID is " +
                    rs.getString("user_id"));
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve user account!");
            logger.error(e.getMessage());
        }

        return user;
    }
    
    // Return the email address of the user that requested this job.
    // Currently not in use.
    public static String getJobRequestorEmail(int jobID) {
        String queryStr = "SELECT email FROM user_account WHERE user_id = ("
                + "SELECT user_id FROM submitted_job WHERE job_id = "
                + jobID + ")";
        String email = null;
        ResultSet rs = DBHelper.runQuery(queryStr);
        
        try {
            if (rs.next()) {
                email = rs.getString("email");
            }
            rs.close();
            logger.debug("For Job ID " + jobID + " user email address is " +
                    email);
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve email address!");
            logger.error(e.getMessage());
        }
       
        return email;
    }
    
    // Return the list of administrator's email addresses i.e. email1,email2
    public static String getAdminEmails() {
        String emails = Constants.DATABASE_INVALID_STR;
        ResultSet rs = DBHelper.runQuery(
                "SELECT email FROM user_account WHERE role_id = 1");
        
        try {
            // The first administrator email.
            if (rs.next()) {
                emails = rs.getString("email");
            }
            // Subsequent adminstrator email(s).
            while (rs.next()) {
                emails += ",";
                emails += rs.getString("email");
            }
            rs.close();
            logger.debug("Retrieved adminstrator email addresses.");
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve administrator email addresses!");
            logger.error(e.getMessage());
        }
        
        return emails;
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
            logger.error("FAIL to check password!");
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
                + "active, dept_id) VALUES (?,?,?,?,?,?,?,?)";
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
            logger.error("FAIL to update last login!");
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
    
    // Update the account detail of this user. Any exception encountered here
    // will be throw and to be handled by the caller.
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
    
    // Retrieve the user account info for this user ID.
    public static UserAccount getUserAct(String userID) {
        UserAccount userAct = null;
        String queryStr = "SELECT * FROM user_account u NATURAL JOIN dept d "
                        + "WHERE user_id = ? AND u.dept_id = d.dept_id";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, userID);
            ResultSet rs = queryStm.executeQuery();
            
            if (rs.next()) {
                userAct = new UserAccount(rs.getString("user_id"),
                                          rs.getInt("role_id"),
                                          rs.getString("first_name"),
                                          rs.getString("last_name"),
                                          rs.getString("email"),
                                          rs.getBoolean("active"),
                                          "password",
                                          rs.getString("dept_id"),
                                          rs.getString("inst_id"),
                                          rs.getString("last_login"));
            }
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve user account info!");
            logger.error(e.getMessage());
        }
        
        return userAct;
    }
    
    // Return the department ID for this user.
    public static String getDeptID(String userID) {
        UserAccount tmp = getUserAct(userID);
        
        if (tmp != null) {
            return tmp.getDept_id();
        }

        return Constants.DATABASE_INVALID_STR;
    }
    
    // Return the role ID for this user.
    public static int getRoleID(String userID) {
        UserAccount tmp = getUserAct(userID);
        
        if (tmp != null) {
            return tmp.getRole_id();
        }
        
        return Constants.DATABASE_INVALID_ID;
    }
    
    // Return the full name of this user.
    public static String getFullName(String userID) {
        UserAccount tmp = getUserAct(userID);
        
        if (tmp != null) {
            return tmp.getFirst_name() + " " + tmp.getLast_name();
        }
        
        return Constants.DATABASE_INVALID_STR;
    }
    
    // Check whether this user ID belongs to a adminstrator/supervisor/clinical
    public static Boolean isAdministrator(String userID) {
        if (userID.compareTo("super") == 0) {
            return Constants.OK;
        }
        else {
            return getRoleID(userID) == 1;
        }
    }
    public static Boolean isSuperVisor(String userID) {
        return getRoleID(userID) <= 2;
    }
    public static Boolean isClinical(String userID) {
        return getRoleID(userID) <= 3;
    }
    
    // Return the institution name and department ID for this user.
    public static String getInstNameDeptID(String userID) {
        String instDept = Constants.DATABASE_INVALID_STR;
        String queryStr = "SELECT d.inst_name, u.dept_id FROM user_account u "
                        + "NATURAL JOIN (SELECT inst_name, dept_id FROM dept "
                        + "NATURAL JOIN inst) d WHERE u.user_id = ? "
                        + "AND u.dept_id = d.dept_id";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, userID);
            ResultSet rs = queryStm.executeQuery();
            
            if (rs.next()) {
                instDept = rs.getString("inst_name") + " - " 
                            + rs.getString("dept_id");
            }
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve institution name and department ID!");
            logger.error(e.getMessage());
        }
        
        return instDept;
    }
}
