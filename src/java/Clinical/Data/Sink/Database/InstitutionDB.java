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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * InstitutionDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the institution table in the database.
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
 */

public abstract class InstitutionDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(InstitutionDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private final static LinkedHashMap<String, String> 
            instNameHash = new LinkedHashMap<>();
    private final static LinkedHashMap<String, String>
            instIDHash = new LinkedHashMap<>();
    private final static List<Institution> instList = new ArrayList<>();
    
    // Clear the institution list, so that it will be rebuild.
    private static void clearInstList() {
        instNameHash.clear();
        instIDHash.clear();
        instList.clear();
    }
    
    // Build the List and HashMap of Institution setup in the database.
    public static Boolean buildInstList() {
        Boolean status = Constants.OK;
        // Only build the institution list if it is empty.
        if (instList.isEmpty()) {
            ResultSet rs = DBHelper.
                runQuery("SELECT * from inst ORDER BY inst_name");

            try {
                while(rs.next()) {
                    Institution inst = new Institution
                                    (rs.getString("inst_id"),
                                     rs.getString("inst_name"));
                    // Build 2 Hash Map; One is Inst Name -> Inst ID,
                    // the other is Inst ID -> Inst Name.
                    instNameHash.put(inst.getInst_name(), inst.getInst_id());
                    instIDHash.put(inst.getInst_id(), inst.getInst_name());
                    instList.add(inst);    
                }
                rs.close();
                logger.debug("Institution List: " + instNameHash.toString());
            }
            catch (SQLException e) {
                logger.error("SQLException when query institution!");
                logger.error(e.getMessage());
                status = Constants.NOT_OK;
            }            
        }
        return status;
    }
    
    // Insert the new institution ID into database.
    public static Boolean insertInstitution(Institution inst) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO inst(inst_id,inst_name) VALUES(?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, inst.getInst_id());
            insertStm.setString(2, inst.getInst_name());
            
            insertStm.executeUpdate();
            // Clear and rebuild the institution list and hashmap.
            clearInstList();
            buildInstList();
            logger.debug("New institution ID inserted into database: " +
                    inst.getInst_id());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting new institution record!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Only allow update to the institution name.
    public static Boolean updateInstitution(Institution inst) {
        Boolean result = Constants.OK;
        String updateStr = "UPDATE inst SET inst_name = ? WHERE inst_id = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setString(1, inst.getInst_name());
            updateStm.setString(2, inst.getInst_id());
            
            updateStm.executeUpdate();
            // Clear and rebuild the institution list and hashmap.
            clearInstList();
            buildInstList();
            logger.debug("Institution " + inst.getInst_id() + " updated.");
        }
        catch (SQLException e) {
            logger.error("SQLException when updating institution!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Return the list of Institution in the database.
    public static List<Institution> getInstList() {
        return instList;
    }
    
    // Return the hashmap of Institution setup in the database.
    public static LinkedHashMap<String, String> getInstNameHash() {
        return instNameHash;
    }
    
    // Return the Institution Name for this instID.
    public static String getInstName(String instID) {
        return instIDHash.get(instID);
    }
}
