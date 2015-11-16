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
 * InstitutionDB is not mean to be instantiate, its main job is to perform
 * SQL operations on the institution table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Created with all the standard getters and setters.
 * 16-nov-2015 - Added 3 new methods, clearInstList(), buildInstList() and
 * insertInstitution(inst).
 */

public class InstitutionDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(InstitutionDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private final static LinkedHashMap<String, String> instHashMap = 
                                                        new LinkedHashMap<>();
    private final static List<Institution> instList = new ArrayList<>();
    
    public InstitutionDB() {}
    
    // To clear the institution list, so that it will be rebuild again.
    private static void clearInstList() {
        instHashMap.clear();
        instList.clear();
    }
    
    // Build the List and HashMap of Institution setup in the database.
    public static Boolean buildInstList() {
        Boolean status = Constants.OK;
        // Only build the institution list if it is empty.
        if (instList.isEmpty()) {
            ResultSet result = DBHelper.
                runQuery("SELECT * from institution ORDER BY institution_name");

            try {
                while(result.next()) {
                    Institution inst = new Institution
                                    (result.getString("institution_code"),
                                     result.getString("institution_name"));
                
                    instHashMap.put(inst.getInstitution_name(),
                                    inst.getInstitution_code());
                    instList.add(inst);    
                }
                logger.debug("Institution List: " + instHashMap.toString());
            }
            catch (SQLException e) {
                logger.error("SQLException at buildInstList.");
                logger.error(e.getMessage());
                status = Constants.NOT_OK;
            }            
        }
        return status;
    }
    
    // Insert the new institution code into database.
    public static Boolean insertInstitution(Institution inst) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO institution"
                + "(institution_code,institution_name) VALUES(?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, inst.getInstitution_code());
            insertStm.setString(2, inst.getInstitution_name());
            
            insertStm.executeUpdate();
            // Clear and rebuild the institution list and hashmap.
            clearInstList();
            buildInstList();
            logger.debug("New institution code inserted into database: " +
                    inst.getInstitution_code());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting institution code.");
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
    public static LinkedHashMap<String, String> getInstHashMap() {
        return instHashMap;
    }
}
