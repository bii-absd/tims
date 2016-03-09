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
                + "age_at_diagnosis,gender,country_code,race,height,weight) "
                + "VALUES(?,?,?,?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject.getSubject_id());
            stm.setString(2, subject.getGrp_id());
            stm.setInt(3, subject.getAge_at_diagnosis());
            stm.setString(4, String.valueOf(subject.getGender()));
            stm.setString(5, subject.getCountry_code());
            stm.setString(6, subject.getRace());
            stm.setFloat(7, subject.getHeight());
            stm.setFloat(8, subject.getWeight());
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
    // Only allow changes to age_at_diagnosis, gender, nationality, race, 
    // height and weight.
    public static Boolean updateSubject(Subject subject) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE subject SET age_at_diagnosis = ?, "
                     + "gender = ?, country_code = ?, race = ?, height = ?, "
                     + "weight = ? WHERE subject_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setInt(1, subject.getAge_at_diagnosis());
            stm.setString(2, String.valueOf(subject.getGender()));
            stm.setString(3, subject.getCountry_code());
            stm.setString(4, subject.getRace());
            stm.setFloat(5, subject.getHeight());
            stm.setFloat(6, subject.getWeight());
            stm.setString(7, subject.getSubject_id());
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
    
    // Return the list of subjects belonging to this group.
    // Exception thrown here need to be handle by the caller.
    public static List<Subject> getSubjectList(String grpID) 
            throws SQLException, NamingException 
    {
        Connection conn = null;
        List<Subject> subjectList = new ArrayList<>();
        String query = "SELECT * from subject WHERE grp_id = ? ORDER BY subject_id";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, grpID);
        ResultSet rs = stm.executeQuery();

        while (rs.next()) {
            Subject tmp = new Subject(
                            rs.getString("subject_id"),
                            rs.getInt("age_at_diagnosis"),
                            rs.getString("gender").charAt(0),
                            rs.getString("country_code"),
                            rs.getString("race"),
                            rs.getFloat("height"),
                            rs.getFloat("weight"),
                            rs.getString("grp_id"));

            subjectList.add(tmp);
        }
        logger.debug("Query subject completed.");
        
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return subjectList;
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
}
