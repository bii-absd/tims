/*
 * Copyright @2015-2016
 */
package TIMS.Database;

import TIMS.General.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
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
 * 17-Dec-2015 - Added new method getStudyAnnotVer, to return the Annotation Version
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
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 01-Mar-2016 - Changes due to one addition attribute (i.e. title) in Study
 * class.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 14-Mar-2016 - Do not allow user to change the study's closed status directly.
 * Changed method queryAllFinalizedStudies(), to return only those studies that  
 * are finalized and not closed.
 * 22-Mar-2016 - Changes due to the addition field (i.e. icd_code) in the study
 * table.
 * 23-Mar-2016 - Separated the update function for study record; one for the
 * main info and one for the description|background|grant information.
 * 04-Apr-2016 - Implemented one helper function that build the studies hash map
 * for pipeline execution and subject meta data management.
 * 07-Apr-2016 - For subject meta data management, the method used to build the
 * studies hash map will be split into 2 (one for users one for PI).
 * 12-Apr-2016 - Added new method getAllStudyHash(), to return all the unclosed
 * Study ID for administrator selection during raw data upload.
 */

public abstract class StudyDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudyDB.class.getName());

    // Return the study object that has this study id.
    public static Study getStudyObject(String studyID) {
        Study study = null;
        Connection conn = null;
        String query = "SELECT * FROM study WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                study = new Study(
                            rs.getString("study_id"),
                            rs.getString("title"),
                            rs.getString("grp_id"),
                            rs.getString("annot_ver"),
                            rs.getString("icd_code"),
                            rs.getString("description"),
                            rs.getString("background"),
                            rs.getString("grant_info"),
                            rs.getString("finalized_output"),
                            rs.getString("summary"),
                            rs.getDate("start_date"),
                            rs.getDate("end_date"),
                            rs.getBoolean("finalized"),
                            rs.getBoolean("closed"));
            }
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve study " + studyID);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return study;
    }
    
    // Insert the new study into database. For every new study created, 
    // the finalized_output and summary fields will be empty.
    public static Boolean insertStudy(Study study) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO study(study_id,title,grp_id,annot_ver,"
                     + "icd_code,description,background,grant_info,start_date,"
                     + "end_date,finalized,closed) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study.getStudy_id());
            stm.setString(2, study.getTitle());
            stm.setString(3, study.getGrp_id());
            stm.setString(4, study.getAnnot_ver());
            stm.setString(5, study.getIcd_code());
            stm.setString(6, study.getDescription());
            stm.setString(7, study.getBackground());
            stm.setString(8, study.getGrant_info());
            stm.setDate(9, study.getStart_date());
            stm.setDate(10, study.getEnd_date());
            stm.setBoolean(11, study.getFinalized());
            stm.setBoolean(12, study.getClosed());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("New Study ID inserted into database: " + 
                    study.getStudy_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert study!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update study description, background or grant information (aka DBGI) in
    // database.
    public static Boolean updateStudyDBGI(Study study) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE study SET description = ?, background = ?, "
                     + "grant_info = ? WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study.getDescription());
            stm.setString(2, study.getBackground());
            stm.setString(3, study.getGrant_info());
            stm.setString(4, study.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated study " + study.getStudy_id() + " DBGI.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update study " + study.getStudy_id() + " DBGI!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update study main info in database.
    public static Boolean updateStudyMI(Study study) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE study SET title=?, grp_id = ?, icd_code = ?, "
                     + "start_date = ?, end_date = ? WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study.getTitle());
            stm.setString(2, study.getGrp_id());
            stm.setString(3, study.getIcd_code());
            stm.setDate(4, study.getStart_date());
            stm.setDate(5, study.getEnd_date());
            stm.setString(6, study.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated study " + study.getStudy_id() + " main info.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update study " + study.getStudy_id() + " main info!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update study's finalized status.
    public static void updateStudyFinalizedStatus(String studyID, Boolean status) {
        Connection conn = null;
        String query = "UPDATE study SET finalized = " + status 
                     + " WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            stm.executeUpdate();
            stm.close();
            
            logger.debug(studyID + " finalized status updated to " + status);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update study to finalized!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Update study's closed status.
    public static void updateStudyClosedStatus(String studyID, Boolean status) {
        Connection conn = null;
        String query = "UPDATE study SET closed = " + status 
                     + " WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            stm.executeUpdate();
            stm.close();
            
            logger.debug(studyID + " closed status updated to " + status);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update study's closed status!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Update the finalized_output with the file path of the output file.
    public static void updateStudyFinalizedFile(String studyID, String path) {
        Connection conn = null;
        String query = "UPDATE study SET finalized_output = ? WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, path);
            stm.setString(2, studyID);
            stm.executeUpdate();
            stm.close();
            
            logger.debug(studyID + " finalized file path updated to " + path);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update study's finalized file path!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Updated the summary with the file path of the summary report.
    public static void updateStudySummaryReport(String studyID, String path) {
        Connection conn = null;
        String query = "UPDATE study SET summary = ? WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, path);
            stm.setString(2, studyID);
            stm.executeUpdate();
            stm.close();
            
            logger.debug(studyID + " summary report path updated to " + path);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update study's summary report path!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Return the list of 'opened' (i.e. not finalized yet) Study ID setup under
    // the group(s) that this lead is heading. The list of Study ID will be
    // available for the lead to select for subject Meta data management.
    public static LinkedHashMap<String, String> getPIOpenStudyHash(String piID) {
        String query = "SELECT study_id FROM study WHERE finalized = false AND "
                     + "grp_id IN (SELECT grp_id FROM grp WHERE pi = \'"
                     + piID + "\') ORDER BY study_id";

        logger.debug("Retrieving open study list for PI " + piID);
        return getStudyHash(query);
    }    
    // Return the list of unclosed Study ID under the group(s) that this lead is
    // heading. The list of Study ID will be available for the lead to select
    // for pipeline execution.
    public static LinkedHashMap<String, String> getPIStudyHash(String piID) {
        String query = "SELECT study_id FROM study WHERE closed = false AND "
                     + "grp_id IN (SELECT grp_id FROM grp WHERE pi = \'"
                     + piID + "\') ORDER BY study_id";
        
        logger.debug("Retrieving study list for PI " + piID);
        return getStudyHash(query);
    }
    // Return the list of 'opened' (i.e. not finalized yet) Study ID setup under
    // the group that this user ID belongs to. The list of Study ID will be
    // available for user to select for subject Meta data management.
    public static LinkedHashMap<String, String> getUserOpenStudyHash(String userID) {
        String query = "SELECT study_id FROM study WHERE finalized = false AND "
                     + "grp_id = (SELECT unit_id FROM user_account WHERE user_id = \'"
                     + userID + "\') ORDER BY study_id";

        logger.debug("Retrieving open study list for user " + userID);
        return getStudyHash(query);
    }    
    // Return the list of unclosed Study ID setup under the group that this 
    // user ID belongs to. The list of Study ID will be available for user to
    // select for pipeline execution.
    public static LinkedHashMap<String, String> getUserStudyHash(String userID) {
        String query = "SELECT study_id FROM study WHERE closed = false AND "
                     + "grp_id = (SELECT unit_id FROM user_account WHERE user_id = \'"
                     + userID + "\') ORDER BY study_id";

        logger.debug("Retrieving study list for user " + userID);
        return getStudyHash(query);
    }
    // Return all the unclosed Study ID setup in the system. This list of Study
    // ID will only be available to administrator to perform raw data uploading 
    // for the users.
    public static LinkedHashMap<String, String> getAllStudyHash() {
        String query = "SELECT study_id FROM study WHERE closed = false ORDER BY study_id";
        
        logger.debug("Retrieving full study list.");
        return getStudyHash(query);
    }
    
    // Helper function to build the study list hash map based on the input query.
    public static LinkedHashMap<String, String> getStudyHash(String query) {
        Connection conn = null;
        LinkedHashMap<String, String> studyHash = new LinkedHashMap<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                studyHash.put(rs.getString("study_id"), rs.getString("study_id"));
            }
            
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query study hash!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return studyHash;
    }
    
    // Only PI that lead a group can perform finalization.
    // Return the list of unfinalized Study ID that has completed job(s), and 
    // belongs to the group this PI is leading. This list of Study ID will be 
    // available for PI to select for finalization.
    public static LinkedHashMap<String, String> getFinalizableStudyHash(String piID) {
        Connection conn = null;
        LinkedHashMap<String, String> finStudyHash = new LinkedHashMap<>();
        String query = "SELECT DISTINCT study_id FROM study st "
                     + "NATURAL JOIN submitted_job sj WHERE sj.status_id = 3 "
                     + "AND st.grp_id IN (SELECT grp_id FROM grp WHERE pi =?) "
                     + "AND st.finalized = false AND st.closed = false "
                     + "ORDER BY study_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, piID);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                finStudyHash.put(rs.getString("study_id"), rs.getString("study_id"));
            }
            
            stm.close();
            logger.debug("Study list available for finalization retrieved.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve study that could be finalize!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return finStudyHash;
    }
    
    // Return the list of studies (all status) that belong to this institution.
    // NOT IMPLEMENTED YET UNTIL THERE IS A USE!
    public static List<Study> queryInstStudies(String inst_id) {
        List<Study> instStudies = new ArrayList<>();
        
        return instStudies;
    }
    
    // Return the list of studies (all status) that belong to this group.
    // NOT IMPLEMENTED YET UNTIL THERE IS A USE!
    public static List<Study> queryGrpStudies(String grp_id) {
        List<Study> grpStudies = new ArrayList<>();
        
        return grpStudies;
    }
    
    // Return the list of studies (all status) that this user is allowed to view.
    public static List<Study> queryStudies(String groupQuery) {
        Connection conn = null;
        List<Study> deptStudies = new ArrayList<>();
        String query = "SELECT * FROM study WHERE grp_id IN (" + groupQuery 
                     + ") ORDER BY study_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                Study tmp = new Study(
                            rs.getString("study_id"),
                            rs.getString("title"),
                            rs.getString("grp_id"),
                            rs.getString("annot_ver"),
                            rs.getString("icd_code"),
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
            
            stm.close();
            logger.debug("Studies review list retrieved.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve studies for " + groupQuery);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return deptStudies;
    }
    
    // Return the full list of finalized studies in the system. The list will
    // shown in the Unfinalize Study view (only accessible by Admin). Once the
    // study is closed, no unfinalize is allowed.
    public static List<Study> queryAllFinalizedStudies() {
        String query = "SELECT * FROM study WHERE finalized = true "
                     + "AND closed = false ORDER BY study_id";
        
        return queryFinalizedStudies(query);
    }
    // Return the list of finalized studies that belong to the group. The list
    // will be shown in the Completed Study Output view.
    public static List<Study> queryFinalizedStudiesByGrp(String grp_id) {
        String query = "SELECT * FROM study WHERE grp_id = \'" + grp_id 
                     + "\' AND finalized = true ORDER BY study_id";

        return queryFinalizedStudies(query);
    }
    // Return the list of finalized studies that belong to the group(s) that
    // this PI (i.e. Director|HOD|PI) is in charge of.
    public static List<Study> queryFinalizedStudiesByGrps(String pi_id) {
        String query = "SELECT * FROM study WHERE finalized = true AND "
                     + "grp_id IN (SELECT grp_id FROM grp WHERE pi = \'"
                     + pi_id + "\') ORDER BY study_id";
        
        return queryFinalizedStudies(query);
    }
    
    // Helper function to retrieve the list of finalized studies from the 
    // database using the query passed in.
    public static List<Study> queryFinalizedStudies(String query) {
        Connection conn = null;
        List<Study> finalizedStudies = new ArrayList<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                Study tmp = new Study(
                            rs.getString("study_id"),
                            rs.getString("title"),
                            rs.getString("grp_id"),
                            rs.getString("annot_ver"),
                            rs.getString("icd_code"),
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
            
            stm.close();
            logger.debug("Finalized studies retrieved.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve finalized studies!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return finalizedStudies;
    }
    
    // Return the list of Study ID setup in the system.
    // Note: Users will not have access to create Study ID i.e. only the 
    // administrator do.
    public static List<Study> queryStudy() {
        Connection conn = null;
        List<Study> studyList = new ArrayList<>();
        String query = "SELECT * FROM study ORDER BY study_id";

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
        
            while (rs.next()) {
                Study tmp = new Study(
                            rs.getString("study_id"),
                            rs.getString("title"),
                            rs.getString("grp_id"),
                            rs.getString("annot_ver"),
                            rs.getString("icd_code"),
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
            
            stm.close();
            logger.debug("Query study completed.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query study!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return studyList;
    }
    
    // Retrieve the annotation version used in this study.
    public static String getStudyAnnotVer(String study_id) {
        return getStudyPropValue(study_id, "annot_ver");
    }
    // Retrieve the group ID for this study.
    public static String getStudyGrpID(String study_id) {
        return getStudyPropValue(study_id, "grp_id");
    }
    // Retrieve the ICD code for this study.
    public static String getICDCode(String study_id) {
        return getStudyPropValue(study_id, "icd_code");
    }
    
    // Helper function to retrieve one of the study's property value.
    public static String getStudyPropValue(String study_id, String property) {
        Connection conn = null;
        String propValue = Constants.DATABASE_INVALID_STR;
        String query = "SELECT * FROM study WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                // Retrieve the requested property value.
                propValue = rs.getString(property);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve " + property + " for study " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return propValue;
    }
    
    // Return the list of annotation version setup in the system
    public static LinkedHashMap<String, String> getAnnotHash() {
        Connection conn = null;
        LinkedHashMap<String, String> annotHash = new LinkedHashMap<>();
        String query = "SELECT annot_ver FROM annotation";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
        
            while (rs.next()) {
                annotHash.put(rs.getString("annot_ver"), rs.getString("annot_ver"));
            }

            stm.close();
            logger.debug("Annotation Version: " + annotHash.toString());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve annotation version!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return annotHash;
    }    
}
