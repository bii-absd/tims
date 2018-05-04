/*
 * Copyright @2015-2018
 */
package TIMS.Database;

import TIMS.General.Constants;
// Libraries for Java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * SubjectDB is an abstract class and not mean to be instantiate, its main job 
 * is to perform SQL operations on the subject table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 10-Dec-2015
 * 
 * Revision History
 * 10-Dec-2015 - First baseline with 3 static methods, insertSubject, 
 * querySubject and clearSubList().
 * 14-Dec-2015 - Added new method, updateSubject.
 * 28-Dec-2015 - Added new method, isSubjectExistInDept.
 * 04-Jan-2016 - Fix the bug in updateSubject (i.e. to setup the 6th parameter).
 * 13-Jan-2016 - Removed all the static variables in Clinical Data Management
 * module.
 * 25-Feb-2016 - Implementation for database 3.0 (Part 2).
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 28-Mar-2016 - Added new method, buildStudySubjectMD() to retrieve and build
 * the study subject Meta data.
 * 30-Mar-2016 - Added the handling for 3 new attributes in subject
 * (i.e. remarks, event and event_date).
 * 31-Mar-2016 - getSubtDetailList() will return a list of SubjectDetail instead
 * of Subject. buildStudySubjectMD() will query from subject_detail view. 
 * Changes due to the movement of some attributes from Subject to StudySubject.
 * 04-Apr-2016 - Enhanced buildStudySubjectMD() method; to return the subject 
 * class, remarks, event and event date.
 * 17-Apr-2017 - Subject's meta data will now be own by study, and the study
 * will be own by group i.e. the direct link between group and subject's meta
 * data will be break off. Removed grp_id, and added study_id and subtype_code
 * in subject DB table.
 * 28-Apr-2017 - Added new method getSubjectIDHashMap() to retrieve the hashmap
 * of subject IDs that belong to a study.
 * 29-May-2017 - Changes due to change in Subject table (i.e. age_at_baseline
 * changed to float type.)
 * 06-Apr-2018 - Database version 2.0 changes. Added 3 new methods 
 * deleteAllSubjectsFromStudy, getSubject and getSubjectIDsList. Enhanced 
 * methods: insertSubject and updateSubject. Remove unused code.
 */

public abstract class SubjectDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubjectDB.class.getName());

    // Delete all the subjects belonging to this study.
    public static void deleteAllSubjectsFromStudy(String study_id) {
        Connection conn = null;
        String query = "DELETE FROM subject WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to delete subjects belonging to " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Return the subject object belonging to this study and subject id.
    public static Subject getSubject(String study_id, String subject_id) {
        Connection conn = null;
        Subject subjt = null;
        String query = "SELECT * FROM subject WHERE study_id = ? AND subject_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            stm.setString(2, subject_id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                subjt = new Subject(rs);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve subject for " 
                        + study_id + "-" + subject_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return subjt;
    }
    
    // Insert the new subject meta data into database
    public static Boolean insertSubject(Subject subject, Connection conn) {
        Boolean result = Constants.OK;
        String query = "INSERT INTO subject(subject_id,study_id,race,"
                + "gender,dob,casecontrol) VALUES(?,?,?,?,?,?)";
        
        try {
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject.getSubject_id());
            stm.setString(2, subject.getStudy_id());
            stm.setString(3, subject.getRace());
            stm.setString(4, subject.getGender());
            stm.setObject(5, subject.getDob(), Types.DATE);
            stm.setString(6, subject.getCasecontrol());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Subject ID: " + subject.getSubject_id() + 
                         " created under study " + subject.getStudy_id());
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert subject " + subject.getSubject_id());
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // For transaction that include update to many rows in the subject table, 
    // the transaction will only be committed if all the updates are successful.
    // For such cases, the caller will be passing in the connection (because
    // they will be controlling the time to commit at their ends.
    public static boolean updateSubt(Subject subt, Connection conn) {
        return updateSubject(subt, conn);
    }
    public static boolean updateSubt(Subject subt) {
        Connection conn = null;
        boolean result = Constants.OK;
        
        try {
            conn = DBHelper.getDSConn();
            result = updateSubject(subt, conn);
        } catch (SQLException|NamingException ex) {
            result = Constants.NOT_OK;
            logger.error(ex.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update subject meta data in database.
    // Only allow changes to gender, race, dob and casecontrol.
    private static boolean updateSubject(Subject subject, Connection conn) {
        boolean result = Constants.OK;
        String query = "UPDATE subject SET gender = ?, casecontrol = ?, "
                     + "race = ?, dob = ? WHERE subject_id = ? "
                     + "and study_id = ?";
        
        try {
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject.getGender());
            stm.setString(2, subject.getCasecontrol());
            stm.setString(3, subject.getRace());
            stm.setObject(4, subject.getDob(), Types.DATE);
            stm.setString(5, subject.getSubject_id());
            stm.setString(6, subject.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated meta data for subject " + 
                         subject.getSubject_id() + " under study " +
                         subject.getStudy_id());
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update meta data for subject " + 
                         subject.getSubject_id() + " under study " + 
                         subject.getStudy_id());
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Return the hashmap of subject ID belonging to this study. Exception
    // thrown here need to be handle by the caller.
    public static LinkedHashMap<String, String> 
        getSubjectIDHashMap(String study_id) 
        throws SQLException, NamingException 
    {
        Connection conn = DBHelper.getDSConn();
        LinkedHashMap<String, String> subtIDHash = new LinkedHashMap<>();
        String query = "SELECT * from subject WHERE study_id = ? ORDER BY subject_id";
        PreparedStatement stm = conn.prepareStatement(query);
        
        stm.setString(1, study_id);
        ResultSet rs = stm.executeQuery();

        while (rs.next()) {
            subtIDHash.put(rs.getString("subject_id"), rs.getString("subject_id"));
        }
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return subtIDHash;
    }
    
    // Return the list of subject ID belonging to this study.
    public static List<String> getSubjectIDsList(String study_id) {
        Connection conn = null;
        List<String> subtIDsList = new ArrayList<>();
        String query = "SELECT * from subject WHERE study_id = ? ORDER BY subject_id";

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                subtIDsList.add(rs.getString("subject_id"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to get subject IDs list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return subtIDsList;
    }
    
    // Return the list of subject details belonging to this study.
    // Exception thrown here need to be handle by the caller.
    public static List<SubjectDetail> getSubtDetailList(String study_id) 
            throws SQLException, NamingException 
    {
        Connection conn = DBHelper.getDSConn();
        List<SubjectDetail> subtDetailList = new ArrayList<>();
        String query = "SELECT * from subject_detail WHERE study_id = ? "
                     + "ORDER BY subject_id, record_date";
        PreparedStatement stm = conn.prepareStatement(query);
        
        stm.setString(1, study_id);
        ResultSet rs = stm.executeQuery();

        while (rs.next()) {
            subtDetailList.add(new SubjectDetail(rs));
        }
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return subtDetailList;
    }
    
    // Check whether the subject exists for this study.
    public static boolean isSubjectExistInStudy
                            (String subject_id, String study_id)
    {
        Connection conn = null;
        boolean isSubjectExist = Constants.NOT_OK;
        String query = "SELECT * FROM subject WHERE subject_id = ? AND "
                     + "study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject_id);
            stm.setString(2, study_id);
            ResultSet rs = stm.executeQuery();
            isSubjectExist = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
        
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to check for subject existence!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return isSubjectExist;
    }
}
