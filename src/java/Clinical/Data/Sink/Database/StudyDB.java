/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * StudyDB is not mean to be instantiate, its main job is to perform
 * SQL operations on the study table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 07-Dec-2015
 * 
 * Revision History
 * 07-Dec-2015 - First baseline with two static methods, insertStudy and 
 * getAnnotHashMap.
 */

public class StudyDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudyDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();

    public StudyDB() {}
    
    // Insert the new study into database
    public static Boolean insertStudy(Study study) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO study(study_id,user_id,annot_ver,"
                         + "description,date,completed) VALUES(?,?,?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, study.getStudy_id());
            insertStm.setString(2, study.getUser_id());
            insertStm.setString(3, study.getAnnot_ver());
            insertStm.setString(4, study.getDescription());
            insertStm.setDate(5, study.getSqlDate());
            insertStm.setBoolean(6, study.getCompleted());
            insertStm.executeUpdate();
            
            logger.debug("New Study ID inserted into database: " + 
                    study.getStudy_id());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting study!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        return result;
    }
    
    // Return the list of annotation version setup in the system
    public static LinkedHashMap<String, String> getAnnotHashMap() {
        LinkedHashMap<String, String> annotList = new LinkedHashMap<>();
        ResultSet rs = DBHelper.runQuery("SELECT annot_ver FROM annotation");
        
        try {
            while (rs.next()) {
                annotList.put(rs.getString("annot_ver"), rs.getString("annot_ver"));
            }
            logger.debug("Annotation Version: " + annotList.toString());
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving annotation version!");
            logger.error(e.getMessage());
        }
        
        return annotList;
    }
}
