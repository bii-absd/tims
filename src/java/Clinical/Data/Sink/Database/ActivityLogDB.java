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
 * 01-Feb-2016 - Further enhanced the query logic by combing the 4 query methods
 * into one, and to include the time (from and/or to) for user selection.
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
    
    // Build the query string based on the parameters selected by the user.
    public static List<ActivityLog> retrieveActivityLog(List<String> paraList) {
        String query = "SELECT * FROM activity_log";
        
        for (int ind = 0; ind < paraList.size(); ind++) {
            // Check that this is the first parameter.
            if (ind == 0) {
                query += " WHERE ";
            }
            
            query += paraList.get(ind);
            // Check that this is not the last parameter.
            if (ind < paraList.size()-1) {
                query += " AND ";
            }
        }
        
        query += " ORDER BY sn DESC";
  
        logger.debug("Query activity log table: " + query);
        
        return buildActivityLog(query);
    }
}
