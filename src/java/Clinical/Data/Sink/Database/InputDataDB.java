/*
 * Copyright @2015
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
 * InputDataDB is an abstract class and not mean to be instantiate, its main job 
 * is to perform SQL operations on the input_data table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 16-Dec-2015
 * 
 * Revision History
 * 16-Dec-2015 - First baseline with three static methods, insertInputData, 
 * getIpList and getNextSn.
 */

public abstract class InputDataDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(InputDataDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private static List<InputData> ipList = new ArrayList<>();
    
    // Insert the new input data detail into database.
    public static Boolean insertInputData(InputData idata) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO input_data(study_id,sn,filename,"
                         + "filepath,description,date) VALUES(?,?,?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, idata.getStudy_id());
            insertStm.setInt(2, idata.getSn());
            insertStm.setString(3, idata.getFilename());
            insertStm.setString(4, idata.getFilepath());
            insertStm.setString(5, idata.getDescription());
            insertStm.setString(6, idata.getDate());
            
            insertStm.executeUpdate();
            // Clear the input data detail list, so that it will be rebuild.
            ipList.clear();
            logger.debug("New input data detail inserted into database: " +
                        idata.getStudy_id() + " - SN: " + idata.getSn());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting input data!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Return the list of input data details that belong to this study.
    // Let the caller handle any exception that might be thrown here.
    public static List<InputData> getIpList(String studyID) throws SQLException {
        // Only execute the query if the list is empty.
        if (ipList.isEmpty()) {
            String queryStr = "SELECT * FROM input_data WHERE study_id = ? "
                            + "ORDER BY sn";
            PreparedStatement queryStm = conn.prepareStatement(queryStr);
            queryStm.setString(1, studyID);
            ResultSet rs = queryStm.executeQuery();
            
            while (rs.next()) {
                InputData tmp = new InputData(rs.getString("study_id"),
                                              rs.getString("filename"),
                                              rs.getString("filepath"),
                                              rs.getString("description"),
                                              rs.getInt("sn"),
                                              rs.getString("date"));
                ipList.add(tmp);
            }
            logger.debug("Query input data completed.");
        }
        
        return ipList;
    }
    
    // Return the next sn for the input data detail for this study.
    public static int getNextSn(String studyID) throws SQLException {
        int nextSn = Constants.DATABASE_INVALID_ID;
        String queryStr = "SELECT MAX(sn) FROM input_data WHERE study_id = ?";
        PreparedStatement queryStm = conn.prepareStatement(queryStr);
        queryStm.setString(1, studyID);
        ResultSet rs = queryStm.executeQuery();
        
        if (rs.next()) {
            // To get the next sn, add 1 to the largest sn for this study_id.
            nextSn = rs.getInt(1) + 1;
        }
        
        return nextSn;
    }
}