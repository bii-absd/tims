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
 */

public abstract class SubjectDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubjectDB.class.getName());

    // Insert the new subject meta data into database
    public static Boolean insertSubject(Subject subject) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO subject(subject_id,grp_id,"
                + "gender,country_code,race) VALUES(?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject.getSubject_id());
            stm.setString(2, subject.getGrp_id());
            stm.setString(3, String.valueOf(subject.getGender()));
            stm.setString(4, subject.getCountry_code());
            stm.setString(5, subject.getRace());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("New Subject ID " + subject.getSubject_id() + 
                         " created under group " + subject.getGrp_id());
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
                     + "race = ? WHERE subject_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, String.valueOf(subject.getGender()));
            stm.setString(2, subject.getCountry_code());
            stm.setString(3, subject.getRace());
            stm.setString(4, subject.getSubject_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated subject " + subject.getSubject_id() + 
                         "\'s meta data.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update subject " + subject.getSubject_id() + 
                         "\'s meta data!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Return the list of subjects belonging to this group and study.
    // Exception thrown here need to be handle by the caller.
    public static List<SubjectDetail> getSubtDetailList(String grpID, String studyID) 
            throws SQLException, NamingException 
    {
        Connection conn = null;
        List<SubjectDetail> subtDetailList = new ArrayList<>();
        String query = "SELECT * from subject_detail WHERE grp_id = ? "
                     + "AND study_id = ? ORDER BY subject_id";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, grpID);
        stm.setString(2, studyID);
        ResultSet rs = stm.executeQuery();

        while (rs.next()) {
            SubjectDetail tmp = new SubjectDetail(
                            rs.getString("grp_id"),
                            rs.getString("study_id"),
                            rs.getString("subject_id"),
                            rs.getString("country_code"),
                            rs.getString("race"),
                            rs.getString("subtype_code"),
                            rs.getString("remarks"),
                            rs.getString("event"),
                            rs.getInt("age_at_diagnosis"),
                            rs.getFloat("height"),
                            rs.getFloat("weight"),
                            rs.getDate("event_date"),
                            rs.getString("gender").charAt(0));

            subtDetailList.add(tmp);
        }
        logger.debug("Query subject detail completed.");
        
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return subtDetailList;
    }
    
    // Check whether the subject meta data exists in the database.
    // Exception thrown here need to be handle by the caller.
    public static boolean isSubjectExistInGrp
        (String subject_id, String grp_id) throws SQLException, NamingException 
    {
        Connection conn = null;
        String query = "SELECT * FROM subject WHERE subject_id = ? AND "
                     + "grp_id = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, subject_id);
        stm.setString(2, grp_id);
        ResultSet rs = stm.executeQuery();
        boolean isSubjectExist = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
        
        stm.close();
        DBHelper.closeDSConn(conn);

        return isSubjectExist;
    }
        
    // To retrieve and build the study subject Meta data (i.e. subject_id|age|
    // gender|race|height|weight|) for this subject under this study.
    public static String buildStudySubjectMD(String subject_id, String grp_id, 
            String study_id) 
    {
        Connection conn = null;
        StringBuilder metadata = new StringBuilder();
        String query = "SELECT  * FROM subject_detail WHERE subject_id = ? AND "
                     + "grp_id = ? AND study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject_id);
            stm.setString(2, grp_id);
            stm.setString(3, study_id);
            ResultSet rs = stm.executeQuery();

            if (rs.next()) {
                metadata.append(subject_id).append("|").
                        append(rs.getInt("age_at_diagnosis")).append("|").
                        append(rs.getString("gender").charAt(0)).append("|").
                        append(rs.getString("race")).append("|").
                        append(rs.getFloat("height")).append("|").
                        append(rs.getFloat("weight")).append("|");
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            metadata.append(Constants.DATABASE_INVALID_STR);
            logger.error("FAIL to build study subject meta data for  " + subject_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return metadata.toString();
    }
}
