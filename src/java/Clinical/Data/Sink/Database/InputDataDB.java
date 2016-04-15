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
 * InputDataDB is an abstract class and not mean to be instantiate, its main job 
 * is to perform SQL operations on the input_data table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 16-Dec-2015
 * 
 * Revision History
 * 16-Dec-2015 - First baseline with three static methods, insertInputData, 
 * getIpList and getNextSn.
 * 30-Dec-2015 - Updated getIpList to handle any SQLException thrown by
 * the query.
 * 31-Dec-2015 - Removed attribute ipList, and updated getIpList to always 
 * query the database for the input_data details.
 * 08-Jan-2016 - To sort the list of input data in descending order.
 * 13-Jan-2016 - One new field user_id added in the input_data table; to 
 * identify the user who has uploaded this input data.
 * 21-Jan-2016 - Added one new field pipeline_name in the input_data table; to
 * associate this input_data with the respective pipeline.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 29-Mar-2016 - Added one new method, getInputDescription() to return the
 * input data description.
 * 14-Apr-2016 - Changes due to the type change (i.e. to Timestamp) for date
 * in submitted_job table.
 */

public abstract class InputDataDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(InputDataDB.class.getName());
    
    // Insert the new input data detail into database.
    public static Boolean insertInputData(InputData idata) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO input_data(study_id,sn,user_id,"
                     + "pipeline_name,filename,filepath,description,date) "
                     + "VALUES(?,?,?,?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, idata.getStudy_id());
            stm.setInt(2, idata.getSn());
            stm.setString(3, idata.getUser_id());
            stm.setString(4, idata.getPipeline_name());
            stm.setString(5, idata.getFilename());
            stm.setString(6, idata.getFilepath());
            stm.setString(7, idata.getDescription());
            stm.setTimestamp(8, idata.getDate());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("New input data detail inserted into database: " +
                        idata.getStudy_id() + " - SN: " + idata.getSn());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert input data!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Return the list of input data that belong to this study ID and pipeline.
    public static List<InputData> getIpList(String studyID, String plName) {
        Connection conn = null;
        List<InputData> ipList = new ArrayList<>();
        String query = "SELECT * FROM input_data WHERE study_id = ? AND "
                     + "pipeline_name = ? ORDER BY sn DESC";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            stm.setString(2, plName);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                InputData tmp = new InputData(rs.getString("study_id"),
                                              rs.getString("user_id"),
                                              rs.getString("pipeline_name"),
                                              rs.getString("filename"),
                                              rs.getString("filepath"),
                                              rs.getString("description"),
                                              rs.getInt("sn"),
                                              rs.getTimestamp("date"));
                ipList.add(tmp);
            }
            
            stm.close();
            logger.debug("Query input data completed.");
        }
        catch (SQLException|NamingException e) {
            logger.debug("FAIL to query input data!");
            logger.debug(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return ipList;
    }
    
    // Return the next sn for the input data detail for this study.
    public static int getNextSn(String studyID) throws SQLException, NamingException 
    {
        Connection conn = null;
        int nextSn = Constants.DATABASE_INVALID_ID;
        String query = "SELECT MAX(sn) FROM input_data WHERE study_id = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, studyID);
        ResultSet rs = stm.executeQuery();
        
        if (rs.next()) {
            // To get the next sn, add 1 to the largest sn for this study_id.
            nextSn = rs.getInt(1) + 1;
        }

        stm.close();
        DBHelper.closeDSConn(conn);
        
        return nextSn;
    }
    
    // Return the description for this input data.
    public static String getInputDescription(String study_id, int sn) {
        Connection conn = null;
        String inputDesc = Constants.DATABASE_INVALID_STR;
        String query = "SELECT description FROM input_data WHERE study_id = ? "
                     + "AND sn = " + sn;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                inputDesc = rs.getString("description");
            }
        }
        catch (SQLException|NamingException e) {
            logger.debug("FAIL to query input data description!");
            logger.debug(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return inputDesc;
    }
}
