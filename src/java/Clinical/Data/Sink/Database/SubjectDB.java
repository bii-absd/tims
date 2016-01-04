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
 */

public abstract class SubjectDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubjectDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private static List<Subject> subList = new ArrayList<>();

    // Insert the new subject meta data into database
    public static Boolean insertSubject(Subject subject) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO subject(subject_id,dept_id,"
                + "age_at_diagnosis,gender,race,height,weight) "
                + "VALUES(?,?,?,?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, subject.getSubject_id());
            insertStm.setString(2, subject.getDept_id());
            insertStm.setInt(3, subject.getAge_at_diagnosis());
            insertStm.setString(4, String.valueOf(subject.getGender()));
            insertStm.setString(5, subject.getRace());
            insertStm.setFloat(6, subject.getHeight());
            insertStm.setFloat(7, subject.getWeight());
            
            insertStm.executeUpdate();
            // Clear the subject list as there is new meta data inserted into database.
            logger.debug("New Subject ID inserted into database: " +
                    subject.getSubject_id());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting subject!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        return result;
    }
    
    // Update subject meta data in database.
    // Only allow changes to age_at_diagnosis, gender, race, height and weight.
    public static Boolean updateSubject(Subject subject) {
        Boolean result = Constants.OK;
        String updateStr = "UPDATE subject SET age_at_diagnosis = ?, "
                         + "gender = ?, race = ?, height = ?, weight = ? "
                         + "WHERE subject_id = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setInt(1, subject.getAge_at_diagnosis());
            updateStm.setString(2, String.valueOf(subject.getGender()));
            updateStm.setString(3, subject.getRace());
            updateStm.setFloat(4, subject.getHeight());
            updateStm.setFloat(5, subject.getWeight());
            updateStm.setString(6, subject.getSubject_id());
            
            updateStm.executeUpdate();
            logger.debug("Updated subject meta data: " + subject.getSubject_id());
        }
        catch (SQLException e) {
            logger.error("Failed to update subject meta data!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Query the subject table using the deptID as a match condition.
    // Exception thrown here need to be handle by the caller.
    public static List<Subject> querySubject(String deptID) throws SQLException {
        String queryStr = "SELECT * from subject WHERE dept_id = ? ORDER BY subject_id";
        PreparedStatement queryStm = conn.prepareStatement(queryStr);
        queryStm.setString(1, deptID);
        ResultSet rs = queryStm.executeQuery();
        subList.clear();

        while (rs.next()) {
            Subject tmp = new Subject(
                            rs.getString("subject_id"),
                            rs.getInt("age_at_diagnosis"),
                            rs.getString("gender").charAt(0),
                            rs.getString("race"),
                            rs.getFloat("height"),
                            rs.getFloat("weight"),
                            rs.getString("dept_id"));
            // Add the object to the List.
            subList.add(tmp);
        }

        logger.debug("Query subject completed.");
        return subList;
    }
    
    // Check whether the subject meta data exists in the database.
    public static Boolean isSubjectExistInDept
        (String subject_id, String dept_id) throws SQLException {
        String queryStr = "SELECT * FROM subject WHERE subject_id = ? AND "
                        + "dept_id = ?";
        
        PreparedStatement queryStm = conn.prepareStatement(queryStr);
        queryStm.setString(1, subject_id);
        queryStm.setString(2, dept_id);
        ResultSet rs = queryStm.executeQuery();

        return rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
    }
    
    // Clear the subject list to make sure that the latest list of subject meta
    // data will be build.
    public static void clearSubList() {
        subList.clear();
    }
}
