/*
 * Copyright @2015-2017
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
 */

public abstract class SubjectDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubjectDB.class.getName());

    // Insert the new subject meta data into database
    public static Boolean insertSubject(Subject subject) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO subject(subject_id,study_id,"
                + "gender,country_code,race,subtype_code,age_at_baseline) "
                + "VALUES(?,?,?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject.getSubject_id());
            stm.setString(2, subject.getStudy_id());
            stm.setString(3, String.valueOf(subject.getGender()));
            stm.setString(4, subject.getCountry_code());
            stm.setString(5, subject.getRace());
            stm.setString(6, subject.getSubtype_code());
            stm.setInt(7, subject.getAge_at_baseline());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("New Subject ID " + subject.getSubject_id() + 
                         " created under study " + subject.getStudy_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert subject " + subject.getSubject_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update subject meta data in database.
    // Only allow changes to gender, nationality, race.
    public static Boolean updateSubject(Subject subject) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE subject SET gender = ?, country_code = ?, "
                     + "race = ?, age_at_baseline = ? WHERE subject_id = ? "
                     + "and study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, String.valueOf(subject.getGender()));
            stm.setString(2, subject.getCountry_code());
            stm.setString(3, subject.getRace());
            stm.setInt(4, subject.getAge_at_baseline());
            stm.setString(5, subject.getSubject_id());
            stm.setString(6, subject.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated meta data for subject " + 
                         subject.getSubject_id() + " under study " +
                         subject.getStudy_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update meta data for subject " + 
                         subject.getSubject_id() + " under study " + 
                         subject.getStudy_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Return the hashmap of subject ID belonging to this study. Exception
    // thrown here need to be handle by the caller.
    public static LinkedHashMap<String, String> 
        getSubjectIDHashMap(String study_id) 
        throws SQLException, NamingException 
    {
        Connection conn = null;
        LinkedHashMap<String, String> subtIDHash = new LinkedHashMap<>();
        String query = "SELECT * from subject WHERE study_id = ? ORDER BY subject_id";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, study_id);
        ResultSet rs = stm.executeQuery();

        while (rs.next()) {
            subtIDHash.put(rs.getString("subject_id"), rs.getString("subject_id"));
        }
        logger.debug("Query subject IDs completed.");
        
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return subtIDHash;
    }
    
    // Return the list of subject details belonging to this study.
    // Exception thrown here need to be handle by the caller.
    public static List<SubjectDetail> getSubtDetailList(String study_id) 
            throws SQLException, NamingException 
    {
        Connection conn = null;
        List<SubjectDetail> subtDetailList = new ArrayList<>();
        String query = "SELECT * from subject_detail WHERE study_id = ? "
                     + "ORDER BY subject_id, record_date";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, study_id);
        ResultSet rs = stm.executeQuery();

        while (rs.next()) {
            subtDetailList.add(new SubjectDetail(rs));
        }
        logger.debug("Query subject detail completed.");
        
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return subtDetailList;
    }
    
    // Check whether the subject exists for this study.
    // Exception thrown here need to be handle by the caller.
    public static boolean isSubjectExistInStudy
        (String subject_id, String study_id) throws SQLException, NamingException 
    {
        Connection conn = null;
        String query = "SELECT * FROM subject WHERE subject_id = ? AND "
                     + "study_id = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, subject_id);
        stm.setString(2, study_id);
        ResultSet rs = stm.executeQuery();
        boolean isSubjectExist = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
        
        stm.close();
        DBHelper.closeDSConn(conn);

        return isSubjectExist;
    }
    
    // NOT IN USE ANYMORE!
    // To retrieve and build the subject Meta data (i.e. subject_id|age|gender|
    // race|height|weight|subjectclass|remarks|event|event_date) for this
    // subject under this study.
    public static String buildStudySubjectMD(String subject_id, String study_id) 
    {
        Connection conn = null;
        StringBuilder metadata = new StringBuilder();
        String query = "SELECT  * FROM subject_detail WHERE subject_id = ? AND "
                     + "study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject_id);
            stm.setString(2, study_id);
            ResultSet rs = stm.executeQuery();

            if (rs.next()) {
                metadata.append(subject_id).append("|").
                        append(rs.getInt("age_at_baseline")).append("|").
                        append(rs.getString("gender").charAt(0)).append("|").
                        append(rs.getString("race")).append("|").
                        append(rs.getFloat("height")).append("|").
                        append(rs.getFloat("weight")).append("|").
                        append(rs.getString("subtype_code")).append("|").
                        append(rs.getString("remarks")).append("|").
                        append(rs.getString("event")).append("|").
                        append(rs.getDate("event_date")).append("|");
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            metadata.append(Constants.DATABASE_INVALID_STR);
            logger.error("FAIL to build study subject meta data for  " + 
                         subject_id + " under study " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return metadata.toString();
    }
}
