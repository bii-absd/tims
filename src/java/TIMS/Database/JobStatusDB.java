/*
 * Copyright @2015-2018
 */
package TIMS.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for Trove
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

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
 * 22-Dec-2015 - To close the ResultSet after use.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 */

public abstract class JobStatusDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(JobStatusDB.class.getName());
    private static final TIntObjectHashMap<String> jsIDHash = 
            new TIntObjectHashMap<String>();
//    private static LinkedHashMap<Integer, String> jsIDHash = new LinkedHashMap<>();
    private static final TObjectIntHashMap<String> jsNameHash = 
            new TObjectIntHashMap<String>();
//    private static LinkedHashMap<String, Integer> jsNameHash = new LinkedHashMap<>();

    // Return the job status name based on the status_id passed in.
    public static String getJobStatusName(int status_id) {
        return jsIDHash.get(status_id);
    }
    
    // Return the job status ID based on the status_name passed in.
    private static int getJobStatusID(String status_name) {
        return jsNameHash.get(status_name);
    }
    
    // Return the job status ID for each job status defined in the system.
    public static int waiting() {
        return getJobStatusID("Waiting");
    }
    public static int inprogress() {
        return getJobStatusID("In-progress");
    }
    public static int completed() {
        return getJobStatusID("Completed");
    }
    public static int finalizing() {
        return getJobStatusID("Finalizing");
    }
    public static int finalized() {
        return getJobStatusID("Finalized");
    }
    public static int failed() {
        return getJobStatusID("Failed");
    }
    
    // Retrieve all the job status defined in job_status table and store them
    // in the jsIDHash and jsNameHash.
    public static void buildJobStatusDef() {
        // We will only build the job status list once.
        if (jsIDHash.isEmpty()) {
            Connection conn = null;
            
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement
                            ("SELECT status_id, status_name FROM job_status");
                ResultSet rs = stm.executeQuery();
                
                while (rs.next()) {
                    // Build the 2 Hash Map; One is Status ID -> Status Name,
                    // the other is Status Name -> Status ID.
                    jsIDHash.put(rs.getInt("status_id"), rs.getString("status_name"));
                    jsNameHash.put(rs.getString("status_name"), rs.getInt("status_id"));
                }
                stm.close();
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to retrieve job status!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
    }    
}
