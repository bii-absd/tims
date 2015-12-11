/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * JobStatusDB is an abstract class and not mean to be instantiate, its main 
 * job is to return the job status name based on the status_id.
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
 * 11-Dec-2015 - Changed to abstract class. Change class name from JobStatus to
 * JobStatusDB. Improve the method getJobStatusDef.
 */

public abstract class JobStatusDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(JobStatusDB.class.getName());    
    private static HashMap<Integer, String> job_status = new HashMap<>();

    // getStatusName will return the job status name based on the 
    // status_id passed in.
    public static String getStatusName(int status_id) {
        return job_status.get(status_id);
    }
    
    // Retrieve all the job status currently defined in job_status table and 
    // store them in the HashMap job_status.
    public static void getJobStatusDef() {
        // Only execute the query if the list is empty
        if (job_status.isEmpty()) {
            ResultSet result = DBHelper.runQuery(
                    "SELECT status_id, status_name FROM job_status");
            try {
                while (result.next()) {
                    job_status.put(result.getInt("status_id"), 
                               result.getString("status_name"));
                }
                logger.debug("Job Status Definition: " + job_status.values());                
            }
            catch (SQLException e) {
                logger.error("SQLException when query job status!");
                logger.error(e.getMessage());
            }
        }
    }    
}
