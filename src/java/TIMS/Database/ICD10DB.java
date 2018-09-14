/*
 * Copyright @2016-2018
 */
package TIMS.Database;

// Libraries for Java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for Trove
import gnu.trove.map.hash.THashMap;

/**
 * ICD10DB is used to perform SQL operations on the icd table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 22-Mar-2016
 * 
 * Revision History
 * 22-Mar-2016 - Created with all the standard getters and setters.
 */

public class ICD10DB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ICD10DB.class.getName());
    private final static LinkedHashMap<String, String> icdCodeHash = 
                                                    new LinkedHashMap<>();
    private final static Map<String, String> icdDescHash = new THashMap<>();
//    private final static LinkedHashMap<String, String> icdDescHash = 
//                                                    new LinkedHashMap<>();
    
    // Return the list of ICD code setup in the system.
    public LinkedHashMap<String, String> getICDCodeHash() {
        if (icdCodeHash.isEmpty()) {
            buildICDHashMaps();
        }
        return icdCodeHash;
    }
    // Return the list of ICD description setup in the system.
    public Map<String, String> getICDDescHash() {
        if (icdDescHash.isEmpty()) {
            buildICDHashMaps();
        }
        return icdDescHash;
    }
    
    // Build the list of icd code and description setup in the database.
    private void buildICDHashMaps() {
        Connection conn = null;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                                    ("SELECT * FROM icd ORDER BY icd_code");
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
    
    // Return the icd description for this icd code.
    public String getICDDescription(String icd_code) {
        if (icdDescHash.isEmpty()) {
            buildICDHashMaps();
            logger.info("ICD10 Map is empty! Building now!");
        }
        return icdDescHash.get(icd_code);
    }
}
