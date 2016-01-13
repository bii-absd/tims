/*
 * Copyright @2015-2016
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
 * 13-Dec-2016 - Removed all the static variables in Study and ItemList
 * management modules.
 */

public abstract class InstitutionDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(InstitutionDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    
    // Insert the new institution ID into database.
    public static Boolean insertInstitution(Institution inst) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO inst(inst_id,inst_name) VALUES(?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, inst.getInst_id());
            insertStm.setString(2, inst.getInst_name());
            insertStm.executeUpdate();
            logger.debug("New institution ID inserted into database: " +
                    inst.getInst_id());
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert new institution!");
            logger.error(e.getMessage());
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
            logger.debug("Institution " + inst.getInst_id() + " updated.");
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update institution!");
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Return the list of Institution in the database.
    public static List<Institution> getInstList() {
        List<Institution> instList = new ArrayList<>();
        ResultSet rs = DBHelper.runQuery
                       ("SELECT * from inst ORDER BY inst_name");

        try {
            while(rs.next()) {
                Institution inst = new Institution
                                    (rs.getString("inst_id"),
                                     rs.getString("inst_name"));
                instList.add(inst);    
            }
            rs.close();
            logger.debug("Institution list built.");
        }
        catch (SQLException e) {
            logger.error("FAIL to build institution list!");
            logger.error(e.getMessage());
        }
        
        return instList;
    }
    
    // Return the hashmap of institution setup in the database.
    public static LinkedHashMap<String, String> getInstNameHash() {
        LinkedHashMap<String, String> instNameHash = new LinkedHashMap<>();
        ResultSet rs = DBHelper.runQuery
                       ("SELECT * FROM inst ORDER BY inst_name");
        
        try {
            while (rs.next()) {
                instNameHash.put(rs.getString("inst_name"), 
                                 rs.getString("inst_id"));
            }
            rs.close();
            logger.debug("Institution name hash built.");
        }
        catch (SQLException e) {
            logger.error("FAIL to build institution name hash!");
            logger.error(e.getMessage());
        }
        
        return instNameHash;
    }
    
    // Return the name for this institution.
    public static String getInstName(String instID) {
        String instName = Constants.DATABASE_INVALID_STR;
        String queryStr = "SELECT inst_name FROM inst WHERE inst_id = ?";

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, instID);
            ResultSet rs = queryStm.executeQuery();
            
            if (rs.next()) {
                instName = rs.getString("inst_name");
            }
            logger.debug("Institution name for " + instID + " is " + instName);
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve institution name!");
            logger.error(e.getMessage());
        }
        
        return instName;
    }
}
