/*
 * Copyright @2016
 */
package TIMS.Database;

import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for Trove
import gnu.trove.map.hash.THashMap;

/**
 * NationalityDB is used to perform SQL operations on the nationality table in 
 * the database.
 * 
 * Author: Tay Wei Hong
 * Date: 25-Feb-2016
 * 
 * Revision History
 * 25-Feb-2016 - Created with all the standard getters and setters.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 */

public class NationalityDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(NationalityDB.class.getName());
    private final static Map<String, String> nationalityCodeHash = new THashMap<>();
    private final static Map<String, String> nationalityNameHash = new THashMap<>();

    // Return the list of nationality code setup in the database.
    public Map<String, String> getNationalityCodeHash() {
        // We will only build the nationality code list once.
        if (nationalityCodeHash.isEmpty()) {
            Connection conn = null;
            
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement
                        ("SELECT * FROM nationality ORDER BY country_code");
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
