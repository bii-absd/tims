/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
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
 */

public class InstitutionDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(InstitutionDB.class.getName());
    private final static LinkedHashMap<String, String> institutionList = 
                                                        new LinkedHashMap<>();
    
    public InstitutionDB() {}
    
    // Return the list of Institution setup in the database.
    public static LinkedHashMap<String, String> getInstitutionList() {
        // We will only build the institutionList once until the list is reset
        if (institutionList.isEmpty()) {
            ResultSet result = DBHelper.
                runQuery("SELECT * from institution ORDER BY institution_name");

            try {
                while(result.next()) {
                    institutionList.put(result.getString("institution_name"),
                                        result.getString("institution_code"));
                    
                }
                logger.debug("Institution List: " + institutionList.toString());
            }
            catch (SQLException e) {
                logger.error("SQLException at getInstitutionList.");
                logger.error(e.getMessage());
            }
        }
        
        return institutionList;
    }
}
