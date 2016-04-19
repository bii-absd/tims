/*
 * Copyright @2016
 */
package TIMS.Database;

import TIMS.General.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ICD10DB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the icd table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 22-Mar-2016
 * 
 * Revision History
 * 22-Mar-2016 - Created with all the standard getters and setters.
 */

public abstract class ICD10DB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ICD10DB.class.getName());
    private final static LinkedHashMap<String, String> icdCodeHash = 
                                                    new LinkedHashMap<>();
    private final static LinkedHashMap<String, String> icdDescHash = 
                                                    new LinkedHashMap<>();
    
    // Return the list of ICD code setup in the system.
    public static LinkedHashMap<String, String> getICDCodeHash() {
        if (icdCodeHash.isEmpty()) {
            buildICDHashMaps();
        }
        
        return icdCodeHash;
    }
    // Return the list of ICD description setup in the system.
    public static LinkedHashMap<String, String> getICDDescHash() {
        if (icdDescHash.isEmpty()) {
            buildICDHashMaps();
        }
        
        return icdDescHash;
    }
    
    // Build the list of icd code and description setup in the database.
    public static void buildICDHashMaps() {
        // We will only build the icd code and description list once.
        if (icdCodeHash.isEmpty()) {
            Connection conn = null;
            String query = "SELECT * FROM icd ORDER BY icd_code";
            
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
                
                while (rs.next()) {
                    // ICD Description -> ICD Code
                    icdCodeHash.put(rs.getString("icd_desc"), 
                                    rs.getString("icd_code"));
                    // ICD Code -> ICD Description
                    icdDescHash.put(rs.getString("icd_code"), 
                                    rs.getString("icd_desc"));
                }
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to retrieve icd code!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
    }
    
    // Return the icd description for this icd code.
    public static String getICDDescription(String icd_code) {
        if (icdDescHash.isEmpty()) {
            return Constants.DATABASE_INVALID_STR;
        }
        return icdDescHash.get(icd_code);
    }
}
