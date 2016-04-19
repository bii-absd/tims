/*
 * Copyright @2016
 */
package TIMS.Database;

import TIMS.General.Constants;
import java.io.Serializable;
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
 * NationalityDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the nationality table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 25-Feb-2016
 * 
 * Revision History
 * 25-Feb-2016 - Created with all the standard getters and setters.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 */

public abstract class NationalityDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(NationalityDB.class.getName());
    private final static LinkedHashMap<String, String> 
            nationalityCodeHash = new LinkedHashMap<>();
    private final static LinkedHashMap<String, String>
            nationalityNameHash = new LinkedHashMap<>();

    // Return the list of nationality code setup in the database.
    public static LinkedHashMap<String, String> getNationalityCodeHash() {
        // We will only build the nationality code list once.
        if (nationalityCodeHash.isEmpty()) {
            Connection conn = null;
            String query = "SELECT * FROM nationality ORDER BY country_code";
            
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
            
                while (rs.next()) {
                    // Country Name -> Country Code
                    nationalityCodeHash.put(rs.getString("country_name"), 
                                            rs.getString("country_code"));
                    // Country Code -> Country Name
                    nationalityNameHash.put(rs.getString("country_code"), 
                                            rs.getString("country_name"));
                }

                stm.close();
                logger.debug("Nationality code list retrieved.");
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to retrieve nationality code!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
        
        return nationalityCodeHash;
    }
    
    // Return the country name for this country code.
    public static String getCountryName(String country_code) {
        if (nationalityNameHash.isEmpty()) {
            return Constants.DATABASE_INVALID_STR;
        }
        return nationalityNameHash.get(country_code);
    }
}
