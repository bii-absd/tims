/*
 * Copyright @2015-2016
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
 * 23-Dec-2015 - To close the ResultSet after use. Added 3 new methods, 
 * updateStudyToCompleted, updateStudyFinalizedFile and getFinalizableStudyHash.
 * 30-Dec-2015 - Updated the query in method getStudyHash, to return only
 * uncompleted study.
 * 05-Jan-2016 - Minor changes to method updateStudyCompletedStatus.
 * 07-Jan-2016 - Changes due to 2 addition attributes in Study class. Added
 * new method queryCompletedStudy. Implemented the module for downloading of
 * study's consolidated output and finalized summary.
 * 13-Dec-2016 - Removed all the static variables in Study and ItemList
 * management modules.
 * 19-Jan-2016 - To cater for adhoc study creation i.e. where the study is 
 * created with completed flag set to true.
 * 20-Jan-2016 - Updated study table in database; added one new variable closed, 
 * and renamed completed to finalized.
 * 23-Feb-2016 - Implementation for database 3.0 (Part 1).
 * 24-Feb-2016 - Implemented studies review module.
 */

public abstract class StudyDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudyDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();

    // Insert the new study into database. For every new study created, 
    // the finalized_output and summary fields will be empty.
    public static Boolean insertStudy(Study study) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO study(study_id,owner_id,dept_id,annot_ver,"
                         + "description,background,grant_info,start_date,"
                         + "end_date,finalized,closed) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, study.getStudy_id());
            insertStm.setString(2, study.getOwner_id());
            insertStm.setString(3, study.getDept_id());
            insertStm.setString(4, study.getAnnot_ver());
            insertStm.setString(5, study.getDescription());
            insertStm.setString(6, study.getBackground());
            insertStm.setString(7, study.getGrant_info());
            insertStm.setDate(8, study.getStart_date());
            insertStm.setDate(9, study.getEnd_date());
            insertStm.setBoolean(10, study.getFinalized());
            insertStm.setBoolean(11, study.getClosed());

            insertStm.executeUpdate();
            logger.debug("New Study ID inserted into database: " + 
                    study.getStudy_id());
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert study!");
            logger.error(e.getMessage());
        }
        return result;
    }
    
    // Update the study information in the database.
    public static Boolean updateStudy(Study study) {
        Boolean result = Constants.OK;
        String updateStr = "UPDATE study SET owner_id = ?, dept_id = ?, "
                         + "description = ?, background = ?, grant_info = ?, "
                         + "start_date = ?, end_date = ?, closed = ? WHERE study_id = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setString(1, study.getOwner_id());
            updateStm.setString(2, study.getDept_id());
            updateStm.setString(3, study.getDescription());
            updateStm.setString(4, study.getBackground());
            updateStm.setString(5, study.getGrant_info());
            updateStm.setDate(6, study.getStart_date());
            updateStm.setDate(7, study.getEnd_date());
            updateStm.setBoolean(8, study.getClosed());
            updateStm.setString(9, study.getStudy_id());
            
            updateStm.executeUpdate();
            logger.debug("Updated study: " + study.getStudy_id());
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update study!");
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Update the study finalized status.
    public static void updateStudyFinalizedStatus(String studyID, Boolean status) {
        String updateStr = "UPDATE study SET finalized = " + status + 
                           " WHERE study_id = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setString(1, studyID);
            updateStm.executeUpdate();
            logger.debug(studyID + " finalized status updated to " + status);
        }
        catch (SQLException e) {
            logger.error("FAIL to update study to finalized!");
            logger.error(e.getMessage());
        }
    }
    
    // Update the study closed status.
    public static void updateStudyClosedStatus(String studyID, Boolean status) {
        String updateStr = "UPDATE study SET closed = " + status + 
                           " WHERE study_id = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setString(1, studyID);
            updateStm.executeUpdate();
            logger.debug(studyID + " closed status updated to " + status);
        }
        catch (SQLException e) {
            logger.error("FAIL to update study to closed!");
            logger.error(e.getMessage());
        }        
    }
    
    // Update the finalized_output with the file path of the output file.
    public static void updateStudyFinalizedFile(String studyID, String path) {
        String updateStr = "UPDATE study SET finalized_output = ? WHERE study_id = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(updateStr)) {
            queryStm.setString(1, path);
            queryStm.setString(2, studyID);
            queryStm.executeUpdate();
            logger.debug(studyID + " finalized file path updated to " + path);
        }
        catch (SQLException e) {
            logger.error("FAIL to update study's finalized file path!");
            logger.error(e.getMessage());
        }
    }
    
    // Updated the summary with the file path of the summary report.
    public static void updateStudySummaryReport(String studyID, String path) {
        String updateStr = "UPDATE study SET summary = ? WHERE study_id = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(updateStr)) {
            queryStm.setString(1, path);
            queryStm.setString(2, studyID);
            queryStm.executeUpdate();
            logger.debug(studyID + " summary report path updated to " + path);
        }
        catch (SQLException e) {
            logger.error("FAIL to update study's summary report path!");
            logger.error(e.getMessage());
        }        
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
            logger.error("FAIL to retrieve annotation version!");
            logger.error(e.getMessage());
        }
        
        return annotHash;
    }
    
    // Return the list of unclosed Study ID setup under the department that this 
    // user ID belongs to. This list of Study ID will be available for users to
    // select for pipeline execution.
    public static LinkedHashMap<String, String> getStudyHash(String userID) {
        LinkedHashMap<String, String> studyHash = new LinkedHashMap<>();
        String queryStr = "SELECT study_id FROM study WHERE closed = false AND "
                        + "dept_id = (SELECT dept_id FROM user_account WHERE user_id = ?) "
                        + "ORDER BY study_id";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, userID);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                studyHash.put(rs.getString("study_id"), rs.getString("study_id"));
            }
            logger.debug("Study list for " + userID + "'s department retrieved.");
        }
        catch (SQLException e) {
            logger.error("FAIL to query study!");
            logger.error(e.getMessage());
        }
        return studyHash;
    }
    
    // Return the list of unfinalized Study ID that has completed job(s), and 
    // belongs to the department that this user ID come from. This list of 
    // Study ID will be available for users to select for finalization.
    public static LinkedHashMap<String, String> getFinalizableStudyHash(String userID) {
        LinkedHashMap<String, String> finStudyHash = new LinkedHashMap<>();
        String queryStr = "SELECT DISTINCT study_id FROM study st "
                        + "NATURAL JOIN submitted_job sj WHERE sj.status_id = 3 "
                        + "AND st.dept_id = (SELECT dept_id FROM user_account "
                        + "WHERE user_id =?) AND st.finalized = false ORDER BY study_id";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, userID);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                finStudyHash.put(rs.getString("study_id"), rs.getString("study_id"));
            }
            logger.debug("Study list available for finalization retrieved.");
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve study that could be finalize!");
            logger.error(e.getMessage());
        }
        
        return finStudyHash;
    }
    
    // Return the list of studies (all status) that belong to this institution.
    public static List<Study> queryInstStudies(String inst_id) {
        List<Study> instStudies = new ArrayList<>();
        
        return instStudies;
    }
    
    // Return the list of studies (all status) that belong to this department.
    public static List<Study> queryDeptStudies(String dept_id) {
        List<Study> deptStudies = new ArrayList<>();
        String queryStr = "SELECT * FROM study WHERE dept_id = \'" + dept_id 
                        + "\' ORDER BY study_id";
        ResultSet rs = DBHelper.runQuery(queryStr);
        
        try {
            while (rs.next()) {
                Study tmp = new Study(
                            rs.getString("study_id"),
                            rs.getString("owner_id"),
                            rs.getString("dept_id"),
                            rs.getString("annot_ver"),
                            rs.getString("description"),
                            rs.getString("background"),
                            rs.getString("grant_info"),
                            rs.getString("finalized_output"),
                            rs.getString("summary"),
                            rs.getDate("start_date"),
                            rs.getDate("end_date"),
                            rs.getBoolean("finalized"),
                            rs.getBoolean("closed"));
                
                deptStudies.add(tmp);
            }
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve studies for " + dept_id);
            logger.error(e.getMessage());
        }
        
        return deptStudies;
    }
    
    // Return the list of studies (all status) that belong to this group.
    public static List<Study> queryGrpStudies(String grp_id) {
        List<Study> grpStudies = new ArrayList<>();
        
        return grpStudies;
    }
    
    // Return the list of finalized studies that belong to the department. This
    // list of study objects will be shown in the datatable in summary of study
    // view.
    public static List<Study> queryFinalizedStudies(String dept_id) {
        List<Study> finalizedStudies = new ArrayList<>();
        String queryStr = "SELECT * FROM study WHERE dept_id = ? "
                        + "AND finalized = true ORDER BY study_id";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, dept_id);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                Study tmp = new Study(
                            rs.getString("study_id"),
                            rs.getString("owner_id"),
                            rs.getString("dept_id"),
                            rs.getString("annot_ver"),
                            rs.getString("description"),
                            rs.getString("background"),
                            rs.getString("grant_info"),
                            rs.getString("finalized_output"),
                            rs.getString("summary"),
                            rs.getDate("start_date"),
                            rs.getDate("end_date"),
                            rs.getBoolean("finalized"),
                            rs.getBoolean("closed"));
                
                finalizedStudies.add(tmp);
            }
            logger.debug("Query finalized studies for " + dept_id + " completed.");
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve finalized studies!");
            logger.error(e.getMessage());
        }
        
        return finalizedStudies;
    }
    
    // Return the list of Study ID setup in the system.
    // Note: Users will not have access to create Study ID i.e. only the 
    // administrator do.
    public static List<Study> queryStudy() {
        List<Study> studyList = new ArrayList<>();
        ResultSet rs = DBHelper.runQuery("SELECT * FROM study ORDER BY study_id");
            
        try {
            while (rs.next()) {
                Study tmp = new Study(
                            rs.getString("study_id"),
                            rs.getString("owner_id"),
                            rs.getString("dept_id"),
                            rs.getString("annot_ver"),
                            rs.getString("description"),
                            rs.getString("background"),
                            rs.getString("grant_info"),
                            rs.getString("finalized_output"),
                            rs.getString("summary"),
                            rs.getDate("start_date"),
                            rs.getDate("end_date"),
                            rs.getBoolean("finalized"),
                            rs.getBoolean("closed"));
                    
                studyList.add(tmp);
            }
            rs.close();
            logger.debug("Query study completed.");
        }
        catch (SQLException e) {
            logger.error("FAIL to query study!");
            logger.error(e.getMessage());
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
                logger.debug("Annotation version used in study: " + studyID + 
                             " is " + annot_ver);
            }
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve annotation version from study!");
            logger.error(e.getMessage());
        }
        
        return annot_ver;
    }
}
