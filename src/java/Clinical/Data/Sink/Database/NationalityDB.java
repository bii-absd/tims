/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
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
            ResultSet rs = DBHelper.runQuery
                    ("SELECT * FROM nationality ORDER BY country_code");
            try {
                while (rs.next()) {
                    // Country Name -> Country Code
                    nationalityCodeHash.put(rs.getString("country_name"), 
                                            rs.getString("country_code"));
                    // Country Code -> Country Name
                    nationalityNameHash.put(rs.getString("country_code"), 
                                            rs.getString("country_name"));
                }
                rs.close();
                logger.debug("Nationality code list retrieved.");
            }
            catch (SQLException e) {
                logger.error("FAIL to retrieve nationality code!");
                logger.error(e.getMessage());
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
