/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * DepartmentDB is not mean to be instantiate, its main job is to perform
 * SQL operations on the department table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Created with all the standard getters and setters.
 */

public class DepartmentDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DepartmentDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();

    public DepartmentDB() {}
    
    // Return the list of departments setup under the institution
    public static LinkedHashMap<String, String> getDeparmentList
        (String institution_code) 
    {
        LinkedHashMap<String, String> departmentList = new LinkedHashMap<>();
        String queryStr = "SELECT department_code, department_name FROM "
                    + "department WHERE institution_code = ?";

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, institution_code);
            ResultSet result = queryStm.executeQuery();
            
            while (result.next()) {
                departmentList.put(result.getString("department_code"), 
                                   result.getString("department_name"));
            }
            logger.debug("Department list for " + institution_code + ": " +
                    departmentList.toString());
        }
        catch (SQLException e) {
            logger.error("SQLException at getDepartmentList: " + institution_code);
            logger.error(e.getMessage());
        }
        
        return departmentList;
    }
}
