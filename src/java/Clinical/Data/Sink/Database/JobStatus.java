/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.Bean.AuthenticationBean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * JobStatus is not mean to be instantiate, its main job is to return the job
 * status name based on the status_id.
 * 
 * Author: Tay Wei Hong
 * Date: 2-Oct-2015
 * 
 * Revision History
 * 02-Oct-2015 - First baseline with two static methods (getJobStatusName and 
 * getJobStatusDef) created.
 * 06-Oct-2015 - Added a check at getJobStatusDef to prevent the query from 
 * being run multiple times. Added Log4j2 for this class.
 * 07-Oct-2015 - Changed to connection based for database access.
 */

public class JobStatus {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(JobStatus.class.getName());    
    private final static Connection conn = DBHelper.getDBConn();
    private static HashMap<Integer, String> job_status = new HashMap<>();

    // getStatusName will return the job status name based on the 
    // status_id passed in.
    public static String getStatusName(int status_id) {
        return job_status.get(status_id);
    }
    
    // getJobStatusDef will retrieve all the job status currently defined in 
    // job_status table and store them in the HashMap job_status.
    public static void getJobStatusDef() {
        // Only execute the query if the list is empty
        if (job_status.isEmpty()) {
            String queryStr = "SELECT status_id, status_name FROM job_status";
            ResultSet result;
        
            try(PreparedStatement queryJobStatus = conn.prepareStatement(queryStr))
            {
                result = queryJobStatus.executeQuery();
            
                while (result.next()) {
                    job_status.put(result.getInt("status_id"), 
                               result.getString("status_name"));
                }
            
                logger.debug("Job Status Definition: " + job_status.values());
            } catch (SQLException e) {
                logger.error(AuthenticationBean.getUserName() + 
                        ": encountered SQLException at getJobStatusDef");
                System.out.println(e.getMessage());
            }
        }
    }    
}
