/*
 * Copyright @2015-2016
 */
package TIMS.Database;

import TIMS.General.Constants;
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
 * InstitutionDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the inst table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Created with all the standard getters and setters.
 * 16-nov-2015 - Added 3 new methods, clearInstList(), buildInstList() and
 * insertInstitution(inst).
 * 18-Nov-2015 - Added one new method, updateInstitution(inst).
 * 30-Nov-2015 - Implementation for database 2.0
 * 09-Dec-2015 - To clear and rebuild the institution list and hashmap after 
 * update. Will not allow updating of inst_id through UI.
 * 16-Dec-2015 - Changed to abstract class.
 * 22-Dec-2015 - To close the ResultSet after use.
 * 13-Dec-2016 - Removed all the static variables in Study and ItemList
 * management modules.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 08-Apr-2016 - Enhanced the getInstNameHash method to retrieve the hashmap 
 * for all or specific institution. Added a static HashMap for storing 
 * Institution ID -> Institution Name mapping, and the methods to build and
 * retrieve institution name from it.
 */

public abstract class InstitutionDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(InstitutionDB.class.getName());
    private static LinkedHashMap<String, String> instIDHash = new LinkedHashMap<>();
    
    // Insert the new institution ID into database.
    public static Boolean insertInstitution(Institution inst) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO inst(inst_id,inst_name) VALUES(?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, inst.getInst_id());
            stm.setString(2, inst.getInst_name());
            stm.executeUpdate();
            stm.close();
            // Rebuild the Institution ID HashMap after every insertion.
            instIDHash.clear();
            buildInstIDHash();
            
            logger.debug("New institution ID inserted into database: " +
                    inst.getInst_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert new institution ID " + inst.getInst_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Only allow update to the institution name.
    public static Boolean updateInstitution(Institution inst) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE inst SET inst_name = ? WHERE inst_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, inst.getInst_name());
            stm.setString(2, inst.getInst_id());            
            stm.executeUpdate();
            stm.close();
            // Rebuild the Institution ID HashMap after every update.
            instIDHash.clear();
            buildInstIDHash();
            
            logger.debug("Institution " + inst.getInst_id() + " updated.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update institution " + inst.getInst_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Return the list of Institution in the database.
    public static List<Institution> getInstList() {
        Connection conn = null;
        String query = "SELECT * from inst ORDER BY inst_name";
        List<Institution> instList = new ArrayList<>();

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while(rs.next()) {
                Institution inst = new Institution
                                    (rs.getString("inst_id"),
                                     rs.getString("inst_name"));
                instList.add(inst);    
            }
            
            stm.close();
            logger.debug("Full institution list built.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to build full institution list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return instList;
    }
    
    // Build the HashMap of Institution ID -> Institution Name.
    public static void buildInstIDHash() {
        if (instIDHash.isEmpty()) {
            Connection conn = null;
            String query = "SELECT * FROM inst ORDER BY inst_name";

            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
            
                while (rs.next()) {
                    instIDHash.put(rs.getString("inst_id"), 
                                   rs.getString("inst_name"));
                }

                stm.close();
                logger.debug("Institution ID HashMap built.");
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to build institution ID HashMap!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
    }
    // Return the name of the institution that has this ID.
    public static String getInstNameFromHash(String instID) {
        return instIDHash.get(instID);
    }

    // Return the hashmap of the institution setup with the specific ID.
    public static LinkedHashMap<String, String> getSingleInstNameHash(String instID) 
    {
        String query = "SELECT * FROM inst WHERE inst_id = \'" + instID 
                     + "\' ORDER BY inst_name";
        
        return getInstNameHash(query);
    }    
    // Return the hashmap of all the institution setup in the database.
    public static LinkedHashMap<String, String> getAllInstNameHash() {
        String query = "SELECT * FROM inst ORDER BY inst_name";
        
        return getInstNameHash(query);
    }
    // Helper function to return the hashmap of the institution setup.
    public static LinkedHashMap<String, String> getInstNameHash(String query) {
        Connection conn = null;
        LinkedHashMap<String, String> instNameHash = new LinkedHashMap<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                instNameHash.put(rs.getString("inst_name"), 
                                 rs.getString("inst_id"));
            }

            stm.close();
            logger.debug("Institution name hash built.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to build institution name hash!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return instNameHash;
    }
    
    // Return the ID of the institution where this unit ID belongs to.
    public static String getInstID(String unitID) {
        String query = "SELECT inst_id FROM inst_dept_grp WHERE inst_id = ? "
                     + "OR dept_id = ? OR grp_id = ?";
        
        return getInstProperty(query, unitID, "inst_id");        
    }
    // Return the name of the institution where this unit ID belongs to.
    public static String getInstName(String unitID) {
        String query = "SELECT inst_name FROM inst_dept_grp WHERE inst_id = ? "
                     + "OR dept_id = ? OR grp_id = ?";
        
        return getInstProperty(query, unitID, "inst_name");
    }
    // Helper function to retrieve the institution ID or name where this unit
    // ID belongs to.
    public static String getInstProperty(String query, String unitID, 
            String property) 
    {
        Connection conn = null;
        String instName = Constants.DATABASE_INVALID_STR;

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, unitID);
            stm.setString(2, unitID);
            stm.setString(3, unitID);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                instName = rs.getString(property);
            }
            
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve " + property + " for unit " + unitID);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return instName;
    }
}
