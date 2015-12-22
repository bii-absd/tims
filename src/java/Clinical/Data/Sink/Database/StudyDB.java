/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
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
 * StudyDB is an abstract class and not mean to be instantiate, its main job 
 * is to perform SQL operations on the study table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 07-Dec-2015
 * 
 * Revision History
 * 07-Dec-2015 - First baseline with two static methods, insertStudy and 
 * getAnnotHashMap.
 * 11-Dec-2015 - Changed to abstract class. Added 4 methods, updateStudy, 
 * getStudyList, queryStudy and clearStudyList().
 * 17-Dec-2015 - Added new method getAnnotVer, to return the Annotation Version
 * used in the study.
 * 22-Dec-2015 - To close the ResultSet after use.
 */

public abstract class StudyDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudyDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private static List<Study> studyList = new ArrayList<>();

    // Insert the new study into database
    public static Boolean insertStudy(Study study) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO study(study_id,dept_id,user_id,"
                         + "annot_ver,description,date,completed) "
                         + "VALUES(?,?,?,?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, study.getStudy_id());
            insertStm.setString(2, study.getDept_id());
            insertStm.setString(3, study.getUser_id());
            insertStm.setString(4, study.getAnnot_ver());
            insertStm.setString(5, study.getDescription());
            insertStm.setDate(6, study.getSqlDate());
            insertStm.setBoolean(7, study.getCompleted());

            insertStm.executeUpdate();
            // Clear the study list, so that it will be rebuild again.
            clearStudyList();
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
    
    // Update the study information in the database
    public static Boolean updateStudy(Study study) {
        Boolean result = Constants.OK;
        String updateStr = "UPDATE study SET dept_id = ?, description = ?, "
                         + "date = ? WHERE study_id = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setString(1, study.getDept_id());
            updateStm.setString(2, study.getDescription());
            updateStm.setDate(3, study.getSqlDate());
            updateStm.setString(4, study.getStudy_id());
            
            updateStm.executeUpdate();
            logger.debug("Updated study: " + study.getStudy_id());
        }
        catch (SQLException e) {
            logger.error("SQLException when updating study!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Return the list of annotation version setup in the system
    public static LinkedHashMap<String, String> getAnnotHash() {
        LinkedHashMap<String, String> annotHash = new LinkedHashMap<>();
        ResultSet rs = DBHelper.runQuery("SELECT annot_ver FROM annotation");
        
        try {
            while (rs.next()) {
                annotHash.put(rs.getString("annot_ver"), rs.getString("annot_ver"));
            }
            rs.close();
            logger.debug("Annotation Version: " + annotHash.toString());
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving annotation version!");
            logger.error(e.getMessage());
        }
        
        return annotHash;
    }
    
    // Return the list of Study ID setup for the department that this
    // user ID belongs to.
    public static LinkedHashMap<String, String> getStudyHash(String userID) {
        LinkedHashMap<String, String> studyHash = new LinkedHashMap<>();
        String queryStr = "SELECT study_id FROM study WHERE dept_id = "
                        + "(SELECT dept_id FROM user_account WHERE user_id = ?)";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, userID);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                studyHash.put(rs.getString("study_id"), rs.getString("study_id"));
            }
            logger.debug("Study list for " + userID + "'s department retrieved.");
        }
        catch (SQLException e) {
            logger.error("SQLException when query study!");
            logger.error(e.getMessage());
        }
        return studyHash;
    }
    
    // Return the list of Study ID setup in the system.
    // Note: Users will not have access to create Study ID i.e. only the 
    // administrator do.
    public static List<Study> queryStudy() {
        // Only execute the query if the list is empty.
        if (studyList.isEmpty()) {
            ResultSet rs = DBHelper.runQuery("SELECT * FROM study ORDER BY date DESC");
            
            try {
                while (rs.next()) {
                    Study tmp = new Study(
                            rs.getString("study_id"),
                            rs.getString("dept_id"),
                            rs.getString("user_id"),
                            rs.getString("annot_ver"),
                            rs.getString("description"),
                            rs.getDate("date"),
                            rs.getBoolean("completed"));
                    
                    studyList.add(tmp);
                }
                rs.close();
                logger.debug("Query study completed.");
            }
            catch (SQLException e) {
                logger.error("SQLException when query study!");
                logger.error(e.getMessage());
                // Exception has occurred, return back a empty list.
                return new ArrayList<>();
            }
        }
        
        return studyList;
    }
    
    // Return the annotation version used in this study.
    public static String getAnnotVer(String studyID) {
        String annot_ver = Constants.DATABASE_INVALID_STR;
        String queryStr = "SELECT annot_ver FROM study WHERE study_id = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, studyID);
            ResultSet rs = queryStm.executeQuery();
            
            if (rs.next()) {
                annot_ver = rs.getString("annot_ver");
            }
        }
        catch (SQLException e) {
            logger.error("SQLException when retrieving annot_ver from study!");
            logger.error(e.getMessage());
        }
        
        return annot_ver;
    }
    
    // Clear the study list, so that the query to the database get to run again.
    public static void clearStudyList() {
        studyList.clear();
    }
}
