// Copyright (C) 2019 A*STAR
//
// TIMS (Translation Informatics Management System) is an software effort 
// by the ABSD (Analytics of Biological Sequence Data) team in the 
// Bioinformatics Institute (BII), Agency of Science, Technology and Research 
// (A*STAR), Singapore.
//

// This file is part of TIMS.
// 
// TIMS is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as 
// published by the Free Software Foundation, either version 3 of the 
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
package TIMS.Database;

// Libraries for Java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ActivityLogDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ActivityLogDB.class.getName());
    
    // Insert this activity into the database. There are 5 categories of 
    // activity: Login, Execute, Create, Change and Download.
    public static void recordUserActivity(String user_id, String activity, 
            String detail)
    {
        // Do not record the activities of the "super" user.
        if (user_id.compareTo("super") != 0) {
            Connection conn = null;
            String query = "INSERT INTO activity_log(user_id, activity, "
                         + "detail, time) VALUES(?,?,?,?)";
            Timestamp now = new Timestamp(Calendar.getInstance().
                                            getTime().getTime());
        
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement(query);
            
                stm.setString(1, user_id);
                stm.setString(2, activity);
                stm.setString(3, detail);
                stm.setTimestamp(4, now);
                stm.executeUpdate();
                stm.close();
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to record user activity!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
    }

    // Retrieve the list of activity currently available in the database.
    public LinkedHashMap<String, String> getActivityList() {
        Connection conn = null;
        LinkedHashMap<String, String> activityList = new LinkedHashMap<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                ("SELECT DISTINCT activity FROM activity_log ORDER BY activity");
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                String activity = rs.getString("activity");
                activityList.put(activity, activity);
            }
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve activity list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return activityList;
    }
    
    // Build and return the activity log based on the query passed in.
    private List<ActivityLog> buildActivityLog(String query) {
        Connection conn = null;
        List<ActivityLog> logs = new ArrayList<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                ActivityLog tmp = new ActivityLog(rs.getString("user_id"),
                                                  rs.getString("activity"),
                                                  rs.getString("detail"),
                                                  rs.getTimestamp("time"));
                logs.add(tmp);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve activity log!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return logs;
    }
    
    // Build the query string based on the parameters selected by the user.
    public List<ActivityLog> retrieveActivityLog(List<String> paraList) {
//        String query = "SELECT * FROM activity_log";
        StringBuilder query = new StringBuilder("SELECT * FROM activity_log");
        
        for (int ind = 0; ind < paraList.size(); ind++) {
            // Check that this is the first parameter.
            if (ind == 0) {
//                query += " WHERE ";
                query.append(" WHERE ");
            }
            
//            query += paraList.get(ind);
            query.append(paraList.get(ind));
            // Check that this is not the last parameter.
            if (ind < paraList.size()-1) {
//                query += " AND ";
                query.append(" AND ");
            }
        }
//        query += " ORDER BY sn DESC";
        query.append(" ORDER BY sn DESC");
        logger.info("Query activity log table: " + query);
        
        return buildActivityLog(query.toString());
    }
}
