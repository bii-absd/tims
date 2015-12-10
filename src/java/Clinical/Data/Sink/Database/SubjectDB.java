/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

// Libraries for Log4j
import Clinical.Data.Sink.General.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
 */

public abstract class SubjectDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubjectDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private static List<Subject> subList = new ArrayList<>();

    // Insert the new subject into database
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
    
    // Query the subject table using the deptID as a match condition.
    public static List<Subject> querySubject(String deptID) {
        // Only execute the query if the list is empty.
        // This is to prevent the query from being run multiple times.
        if (subList.isEmpty()) {
            String queryStr = "SELECT * from subject WHERE dept_id = ?";
            
            try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
                queryStm.setString(1, deptID);
                ResultSet rs = queryStm.executeQuery();
                
                while (rs.next()) {
                    Subject tmp = new Subject(
                            rs.getString("subject_id"),
                            rs.getString("dept_id"),
                            rs.getString("race"),
                            rs.getString("gender").charAt(0),
                            rs.getInt("age_at_diagnosis"),
                            rs.getFloat("height"),
                            rs.getFloat("weight"));
                    // Add the object to the List.
                    subList.add(tmp);
                }
                logger.debug("Query subject completed.");
            }
            catch (SQLException e) {
                logger.error("SQLException when query subject!");
                logger.error(e.getMessage());
                // Exception has occurred, return back a empty list.
                return new ArrayList<>(0);
            }
        }
        
        return subList;
    }
    
    // Clear the subject list, so that the query to the database get to run again.
    public static void clearSubList() {
        subList.clear();
    }
}
