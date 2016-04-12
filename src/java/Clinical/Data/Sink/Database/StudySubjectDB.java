/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * StudySubjectDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the study_subject table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 31-Mar-2016
 * 
 * Revision History
 * 31-Mar-2016 - First baseline with 4 static methods, insertSS, getSSList, 
 * updateSS and isSSExist.
 * 13-Apr-2016 - Added new method updatePartialSS(), to exclude event and
 * event date during updating of study_subject table.
 * 
 */

public abstract class StudySubjectDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudySubjectDB.class.getName());

    // Insert the subject meta data under this study into database.
    public static boolean insertSS(StudySubject ss) {
        boolean result = Constants.OK;
        Connection conn = null;
        String query = "INSERT INTO study_subject(subject_id,grp_id,study_id,"
                     + "subtype_code,age_at_diagnosis,height,weight,remarks,"
                     + "event,event_date) VALUES (?,?,?,?,?,?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, ss.getSubject_id());
            stm.setString(2, ss.getGrp_id());
            stm.setString(3, ss.getStudy_id());
            stm.setString(4, ss.getSubtype_code());
            stm.setInt(5, ss.getAge_at_diagnosis());
            stm.setFloat(6, ss.getHeight());
            stm.setFloat(7, ss.getWeight());
            stm.setString(8, ss.getRemarks());
            stm.setString(9, ss.getEvent());
            stm.setDate(10, ss.getEvent_date());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Inserted meta data for subject " + ss.getSubject_id() 
                       + " under study " + ss.getStudy_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert meta data for subject " + 
                         ss.getSubject_id() + " under study " + 
                         ss.getStudy_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return result;
    }
    
    // Return the list of study_subject belonging to this group + study.
    public static List<StudySubject> getSSList(String grp_id, String study_id) {
        Connection conn = null;
        List<StudySubject> ssList = new ArrayList<>();
        String query = "SELECT * FROM study_subject WHERE grp_id = ? "
                     + "AND study_id = ? ORDER BY subject_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, grp_id);
            stm.setString(2, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                StudySubject ss = new StudySubject(
                                    rs.getString("subject_id"),
                                    rs.getString("grp_id"),
                                    rs.getString("study_id"),
                                    rs.getString("subtype_code"),
                                    rs.getString("remarks"),
                                    rs.getString("event"),
                                    rs.getInt("age_at_diagnosis"),
                                    rs.getFloat("height"),
                                    rs.getFloat("weight"),
                                    rs.getDate("event_date"));
                
                ssList.add(ss);
            }
            logger.debug("Query meta data for study completed.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query meta data under study " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return ssList;
    }
    
    // Update subject meta data under this study in database. To be called 
    // during Meta data upload by batch.
    // Only allow changes to age_at_diagnosis, height, weight and remarks.
    public static boolean updatePartialSS(StudySubject ss) {
        boolean result = Constants.OK;
        Connection conn = null;
        String query = "UPDATE study_subject SET age_at_diagnosis = ?, "
                     + "height = ?, weight = ?, remarks = ? "
                     + "WHERE subject_id = ? AND grp_id = ? AND study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setInt(1, ss.getAge_at_diagnosis());
            stm.setFloat(2, ss.getHeight());
            stm.setFloat(3, ss.getWeight());
            stm.setString(4, ss.getRemarks());
            stm.setString(5, ss.getSubject_id());
            stm.setString(6, ss.getGrp_id());
            stm.setString(7, ss.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated partial meta data for subject "
                        + ss.getSubject_id() + " under group " 
                        + ss.getGrp_id() + " in study "
                        + ss.getStudy_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update partial meta data for subject " 
                        + ss.getSubject_id() + " under group " 
                        + ss.getGrp_id() + " in study "
                        + ss.getStudy_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;        
    }
    
    // Update subject meta data under this study in database.
    // Only allow changes to age_at_diagnosis, height, weight, remarks, event
    // and event_date.
    public static boolean updateSS(StudySubject ss) {
        boolean result = Constants.OK;
        Connection conn = null;
        String query = "UPDATE study_subject SET age_at_diagnosis = ?, "
                     + "height = ?, weight = ?, remarks = ?, event = ?, "
                     + "event_date = ? WHERE subject_id = ? AND grp_id = ? "
                     + "AND study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setInt(1, ss.getAge_at_diagnosis());
            stm.setFloat(2, ss.getHeight());
            stm.setFloat(3, ss.getWeight());
            stm.setString(4, ss.getRemarks());
            stm.setString(5, ss.getEvent());
            stm.setDate(6, ss.getEvent_date());
            stm.setString(7, ss.getSubject_id());
            stm.setString(8, ss.getGrp_id());
            stm.setString(9, ss.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated meta data for subject "
                        + ss.getSubject_id() + " under group " 
                        + ss.getGrp_id() + " in study "
                        + ss.getStudy_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update meta data for subject " 
                        + ss.getSubject_id() + " under group " 
                        + ss.getGrp_id() + " in study "
                        + ss.getStudy_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Check whether the subject meta data belonging to this group + study
    // exists in the database. Exception thrown here need to be handle by the
    // caller.
    public static boolean isSSExist(String subject_id, String grp_id, 
            String study_id) throws SQLException, NamingException
    {
        Connection conn = null;
        String query = "SELECT * FROM study_subject WHERE subject_id = ? AND "
                     + "grp_id = ? AND study_id = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, subject_id);
        stm.setString(2, grp_id);
        stm.setString(3, study_id);
        ResultSet rs = stm.executeQuery();
        boolean ssExist = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
        
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return ssExist;
    }
}
