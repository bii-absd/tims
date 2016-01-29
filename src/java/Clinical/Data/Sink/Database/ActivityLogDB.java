/*
 * Copyright @2016
 */

package Clinical.Data.Sink.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ActivityLogDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the activity_log table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 26-Jan-2016
 * 
 * Revision History
 * 26-Jan-2016 - First baseline with three static methods, recordUserActivity,
 * retrieveUserActivities and retrieveActivityRecords.
 * 29-Jan-2016 - Added 2 new query methods, retrieveAllActivities and 
 * retrieveActivities. Enhanced the query methods to reuse the common code used 
 * in retrieving the log from database.
 */

public abstract class ActivityLogDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ActivityLogDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    
    // Insert this activity into the database. There are 5 categories of 
    // activity: Login, Execute, Create, Change and Download.
    public static void recordUserActivity(String user_id, String activity, 
            String detail)
    {
        String insertStr = "INSERT INTO activity_log(user_id, activity, "
                         + "detail, time) VALUES(?,?,?,?)";
        Timestamp now = new Timestamp(Calendar.getInstance().getTime().getTime());
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, user_id);
            insertStm.setString(2, activity);
            insertStm.setString(3, detail);
            insertStm.setTimestamp(4, now);
            insertStm.executeUpdate();
        }
        catch (SQLException e) {
            logger.error("FAIL to record user activity!");
            logger.error(e.getMessage());
        }
    }

    // Build and return the activity log based on the query passed in.
    private static List<ActivityLog> buildActivityLog(String query) {
        List<ActivityLog> logs = new ArrayList<>();
        ResultSet rs = DBHelper.runQuery(query);
        
        try {
            while (rs.next()) {
                ActivityLog tmp = new ActivityLog(rs.getString("user_id"),
                                                  rs.getString("activity"),
                                                  rs.getString("detail"),
                                                  rs.getTimestamp("time"));
                logs.add(tmp);
            }
            logger.debug("Retrieved activity log.");
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve activity log!");
            logger.error(e.getMessage());
        }

        return logs;
    }
    
    // Retrieve all the activity log.
    public static List<ActivityLog> retrieveAllActivities() {
        String query = "SELECT * FROM activity_log ORDER BY sn DESC";
        
        return buildActivityLog(query);
    }
    
    // Retrieve the log for this user performing this activity.
    public static List<ActivityLog> retrieveActivities(String user_id, 
            String activity) {
        String query = "SELECT * FROM activity_log WHERE user_id = \'" 
                     + user_id + "\' AND activity = \'" 
                     + activity + "\' ORDER BY sn DESC";
        
        return buildActivityLog(query);
    }
    
    // Retrieve all the activties for this user.
    public static List<ActivityLog> retrieveUserActivities(String user_id) {
        String query = "SELECT * FROM activity_log WHERE user_id = \'" 
                     + user_id + "\' ORDER BY sn DESC";

        return buildActivityLog(query);
    }
    
    // Retrieve all the log for this activity.
    public static List<ActivityLog> retrieveActivityRecords(String activity) {
        String query = "SELECT * FROM activity_log WHERE activity = \'" 
                        + activity + "\' ORDER BY sn DESC";
        
        return buildActivityLog(query);
    }
}
