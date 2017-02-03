/*
 * Copyright @2017
 */
package TIMS.Database;

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
 * SystemParametersDB is an abstract class and not mean to be instantiate, its
 * main job is to return the system parameter based on function call.
 * 
 * Author: Tay Wei Hong
 * Date: 02-Feb-2017
 * 
 * Revision History
 * 02-Feb-2017 - Initial creation with two static methods (loadSystemParameters
 * and getcBioPortalUrl) created.
 */

public abstract class SystemParametersDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SystemParametersDB.class.getName());
    private static LinkedHashMap<String, String> spHash = new LinkedHashMap<>();
    
    // Load all the system parameters defined in the system_parameters table
    // and store them in the spHash.
    public static void loadSystemParameters() {
        // We will only load the system parameters once.
        if (spHash.isEmpty()) {
            Connection conn = null;
            String query = "SELECT sys_para_name, sys_para_value FROM system_parameters";
            
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
                
                while (rs.next()) {
                    spHash.put(rs.getString("sys_para_name"), rs.getString("sys_para_value"));
                }
                stm.close();
                logger.debug("System parameters loaded: " + spHash.toString());
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to load system parameters!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
    }
    
    // Return the cBioPortal URL setup in the system.
    public static String getcBioPortalUrl() {
        return spHash.get("CBIOPORTAL_URL");
    }
}
