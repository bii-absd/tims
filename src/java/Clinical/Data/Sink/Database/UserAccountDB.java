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
// Libraries for Java Extension
import javax.naming.NamingException;
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
 * 19-Feb-2016 - To support user account with picture uploaded.
 * 23-Feb-2016 - Added new method getPiIDHash(), to return the user ID of all
 * the PIs in the system.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 */

public abstract class UserAccountDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(UserAccountDB.class.getName());
    
    // If no user account has been setup, return false (i.e. the system has just
    // been setup) else return true.
    public static boolean isUserAccountSetup() {
        Connection conn = null;
        boolean isUserAcctSetup = false;
        String query = "SELECT COUNT(*) FROM user_account";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    isUserAcctSetup = true;
                }
            }
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve user account count!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return isUserAcctSetup;
    }
    
    // Temporay method retrieve all the user ID of PI.
    // In future, this method will be moved to grp Class.
    // Return the list of user ID that has been registered as the PI of the group.
    // DIFFERENT FROM THIS!
    
    // Return the list of user ID that can be PI (i.e. Director, HOD and PI).
    public static LinkedHashMap<String, String> getPiIDHash() {
        Connection conn = null;
        LinkedHashMap<String,String> piIDHash = new LinkedHashMap<>();
        String query = "SELECT first_name, user_id FROM user_account "
                     + "WHERE role_id IN (2,3,4) ORDER BY user_id";

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                piIDHash.put(rs.getString("first_name"), rs.getString("user_id"));
            }            
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve PI ID!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return piIDHash;
    }
    
    // Return the list of all the user ID currently in the system.
    public static LinkedHashMap<String,String> getUserIDHash() {
        Connection conn = null;
        LinkedHashMap<String,String> userIDHash = new LinkedHashMap<>();
        String query = "SELECT user_id FROM user_account ORDER BY user_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                String user_id = rs.getString("user_id");
                userIDHash.put(user_id, user_id);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve user ID!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return userIDHash;
    }
    
    // Return the list of user accounts that are currently in the system.
    public static List<UserAccount> getAllUserAcct() {
        Connection conn = null;
        List<UserAccount> userAcctList = new ArrayList<>();
//        String query = "SELECT * FROM user_account u NATURAL JOIN dept d "
//                     + "WHERE u.dept_id = d.dept_id ORDER BY u.user_id";
        String query = "SELECT * FROM user_account ORDER BY user_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
        
            while (rs.next()) {
                UserAccount user = new UserAccount(
                                        rs.getString("user_id"),
                                        rs.getInt("role_id"),
                                        rs.getString("first_name"),
                                        rs.getString("last_name"),
                                        rs.getString("photo"),
                                        rs.getString("email"),
                                        rs.getBoolean("active"),
                                        "password",
                                        rs.getString("unit_id"),
                                        rs.getString("last_login"));
                
                userAcctList.add(user);
            }
            stm.close();
            logger.debug("No of user account retrieved: " + userAcctList.size());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve user accounts!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return userAcctList;
    }
    
    // Return the user account that requested this job.
    public static UserAccount getJobRequestor(int jobID) {
        Connection conn = null;
        UserAccount user = null;
//        String query = "SELECT * FROM user_account u NATURAL JOIN dept d "
//                + "WHERE u.dept_id = d.dept_id AND u.user_id = (SELECT "
//                + "user_id FROM submitted_job WHERE job_id = "
//                + jobID + ")";
        String query = "SELECT * FROM user_account WHERE user_id = "
                     + "(SELECT user_id FROM submitted_job WHERE job_id = "
                     + jobID + ")";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
        
            if (rs.next()) {
                user = new UserAccount(rs.getString("user_id"),
                                       rs.getInt("role_id"),
                                       rs.getString("first_name"),
                                       rs.getString("last_name"),
                                       rs.getString("photo"),
                                       rs.getString("email"),
                                       rs.getBoolean("active"),
                                       "password",
                                       rs.getString("unit_id"),
                                       rs.getString("last_login"));
                
            }
            logger.debug("For Job ID " + jobID + " User ID is " +
                    rs.getString("user_id"));
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve user account!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return user;
    }
    
    // Return the email address of the user that requested this job.
    // Currently not in use.
    public static String getJobRequestorEmail(int jobID) {
        Connection conn = null;
        String query = "SELECT email FROM user_account WHERE user_id = ("
                + "SELECT user_id FROM submitted_job WHERE job_id = "
                + jobID + ")";
        String email = null;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                email = rs.getString("email");
            }
            stm.close();
            logger.debug("For Job ID " + jobID + " user email address is " +
                    email);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve email address!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
       
        return email;
    }
    
    // Return the list of administrator's email addresses i.e. email1,email2
    public static String getAdminEmails() {
        Connection conn = null;
        String query = "SELECT email FROM user_account WHERE role_id = 1";
        String emails = Constants.DATABASE_INVALID_STR;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            // The first administrator email.
            if (rs.next()) {
                emails = rs.getString("email");
            }
            // Subsequent adminstrator email(s).
            while (rs.next()) {
                emails += ",";
                emails += rs.getString("email");
            }
            stm.close();
            logger.debug("Retrieved adminstrator email addresses.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve administrator email addresses!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return emails;
    }
    
    // Check the password entered by the user, and if the password is valid, 
    // a UserAccount object will be return.
    public static UserAccount checkPwd(String user_id, String pwd) {
        Connection conn = null;
//        String query = "SELECT * FROM user_account u NATURAL JOIN dept d "
//                        + "WHERE u.dept_id = d.dept_id AND u.user_id = ?";
        String query = "SELECT * FROM user_account WHERE user_id = ?";
        String pwd_hash = null;
        UserAccount acct = null;

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            // Build the query condition using the parameter user_id.
            stm.setString(1, user_id);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                pwd_hash = rs.getString("pwd");
            }

            if (pwd_hash != null) {
                if (BCrypt.checkpw(pwd, pwd_hash)) {
                    logger.info(user_id + ": password valid.");
                    // Construct a UserAccount object based on the return result
                    // BUT set the password and last_login to default value.
                    acct = new UserAccount(rs.getString("user_id"),
                                           rs.getInt("role_id"),
                                           rs.getString("first_name"),
                                           rs.getString("last_name"),
                                           rs.getString("photo"),
                                           rs.getString("email"),
                                           rs.getBoolean("active"),
                                           "password",
                                           rs.getString("unit_id"),
                                           "last-login");
                }
            }
            stm.close();
        } 
        catch (SQLException|NamingException e) {
            logger.error("FAIL to check password!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
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
    public static void insertAccount(UserAccount newAcct) 
            throws SQLException, NamingException 
    {
        Connection conn = null;
        // Hash the password using BCrypt before storing it into the database.
        String pwd_hash = BCrypt.hashpw(newAcct.getPwd(), BCrypt.gensalt());
        String query = "INSERT INTO user_account"
                + "(user_id, role_id, first_name, last_name, photo, email, pwd, "
                + "active, unit_id) VALUES (?,?,?,?,?,?,?,?,?)";
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        
        // Build the INSERT statement using the values from the current
        // UserAccount object.
        stm.setString(1, newAcct.getUser_id());
        stm.setInt(2, newAcct.getRole_id());
        stm.setString(3, newAcct.getFirst_name());
        stm.setString(4, newAcct.getLast_name());
        stm.setString(5, newAcct.getPhoto());
        stm.setString(6, newAcct.getEmail());
        stm.setString(7, pwd_hash);
        stm.setBoolean(8, newAcct.getActive());
        stm.setString(9, newAcct.getUnit_id());
        // Execute the INSERT statement
        stm.executeUpdate();
        stm.close();
        
        DBHelper.closeDSConn(conn);
    }
    
    // Update the last login of this user.
    public static void updateLastLogin(String user_id, String last_login) {
        Connection conn = null;
        String query = "UPDATE user_account SET last_login = ? WHERE "
                     + "user_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, last_login);
            stm.setString(2, user_id);
            // Execute the UPDATE statement
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update last login!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Update the password of this user. Any exception encountered here will 
    // be throw and to be handled by the caller.
    public static void updatePassword(String user_id, String new_pwd) 
            throws SQLException, NamingException  
    {
        Connection conn = null;
        // Hash the password using BCrypt before storing it into the database.
        String pwd_hash = BCrypt.hashpw(new_pwd, BCrypt.gensalt());
        String query = "UPDATE user_account SET pwd = ? WHERE "
                     + "user_id = ?";
        conn = DBHelper.getDSConn();
        
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, pwd_hash);
        stm.setString(2, user_id);
        // Execute the UPDATE statement
        stm.executeUpdate();
        stm.close();
        
        DBHelper.closeDSConn(conn);
    }
    
    // Update the account detail of this user. Any exception encountered here
    // will be throw and to be handled by the caller.
    public static void updateAccount(UserAccount user) 
            throws SQLException, NamingException 
    {
        Connection conn = null;
        String query = "UPDATE user_account SET unit_id = ?, "
                     + "first_name = ?, last_name = ?, photo = ?, "
                     + "email = ?, active = ?, role_id = ? WHERE "
                     + "user_id = ?";
        conn = DBHelper.getDSConn();
        
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, user.getUnit_id());
        stm.setString(2, user.getFirst_name());
        stm.setString(3, user.getLast_name());
        stm.setString(4, user.getPhoto());
        stm.setString(5, user.getEmail());
        stm.setBoolean(6, user.getActive());
        stm.setInt(7, user.getRole_id());
        stm.setString(8, user.getUser_id());
        // Excute the UPDATE statement
        stm.executeUpdate();
        stm.close();
        
        DBHelper.closeDSConn(conn);
    }
    
    // Retrieve the user account info for this user ID.
    public static UserAccount getUserAct(String userID) {
        Connection conn = null;
        UserAccount userAct = null;
        String query = "SELECT * FROM user_account WHERE user_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, userID);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                userAct = new UserAccount(rs.getString("user_id"),
                                          rs.getInt("role_id"),
                                          rs.getString("first_name"),
                                          rs.getString("last_name"),
                                          rs.getString("photo"),
                                          rs.getString("email"),
                                          rs.getBoolean("active"),
                                          "password",
                                          rs.getString("unit_id"),
                                          rs.getString("last_login"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve user account info!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return userAct;
    }
    
    // Return the unit ID for this user.
    public static String getUnitID(String userID) {
        UserAccount tmp = getUserAct(userID);
        
        if (tmp != null) {
            return tmp.getUnit_id();
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
    public static boolean isAdministrator(String userID) {
        if (userID.compareTo("super") == 0) {
            return Constants.OK;
        }
        else {
            return getRoleID(userID) == 1;
        }
    }
    public static boolean isDirector(String userID) {
        return getRoleID(userID) == 2;
    }
    public static boolean isHOD(String userID) {
        return getRoleID(userID) == 3;
    }
    public static boolean isPI(String userID) {
        return getRoleID(userID) == 4;
    }
    
    // Return the institution name and unit ID for this user.
    // If user is a director, return the institution name.
    // For other users, return institution name - unit ID.
    public static String getInstNameUnitID(String userID) {
        Connection conn = null;
        int role_id = getRoleID(userID);
        String instUnit = Constants.DATABASE_INVALID_STR;
        String query;
        
        // Director
        if (role_id == 2) {
            query = "SELECT DISTINCT inst_name FROM inst_dept_grp WHERE "
                  + "inst_id = (SELECT unit_id FROM user_account WHERE user_id = ?)";
        }
        // HOD
        else if (role_id == 3) {
            query = "SELECT DISTINCT x.inst_name, y.unit_id FROM inst_dept_grp x "
                  + "WHERE x.dept_id = "
                  + "(SELECT unit_id FROM user_account WHERE user_id = ?) y";
        }
        // Admin, PI & User
        else {
            query = "SELECT DISTINCT x.inst_name, y.unit_id FROM inst_dept_grp x "
                  + "WHERE x.grp_id = "
                  + "(SELECT unit_id FROM user_account WHERE user_id = ?) y";
        }
        
//        query = "SELECT d.inst_name, u.unit_id FROM user_account u "
//              + "NATURAL JOIN (SELECT inst_name, dept_id FROM dept "
//              + "NATURAL JOIN inst) d WHERE u.user_id = ? "
//              + "AND u.dept_id = d.dept_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, userID);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                if (role_id == 2) {
                    instUnit = rs.getString("inst_name");
                }
                else {
                    instUnit = rs.getString("inst_name") + " - " 
                             + rs.getString("unit_id");
                }
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve institution name and unit ID!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return instUnit;
    }
}
