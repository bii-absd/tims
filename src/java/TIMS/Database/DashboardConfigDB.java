/*
 * Copyright @2018
 */
package TIMS.Database;

import TIMS.General.Constants;
// Libraries for Java
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
 * DashboardConfigDB is used to perform SQL operations on the 
 * dashboard_config table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 09-Nov-2018
 * 
 * Revision History
 * 09-Nov-2018 - First baseline with 4 static methods, getDashboardConfigList(),
 * getDBCForChartID(chart_id), insertDBCsForNewStudy() and updateDBC(dbc).
 */

public class DashboardConfigDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DashboardConfigDB.class.getName());
    private final String study_id;
    // Machine generated constructor
    public DashboardConfigDB(String study_id) {
        this.study_id = study_id;
    }

    // Return the list of dashboard config that belong to this study.
    public List<DashboardConfig> getDashboardConfigList() {
        Connection conn = null;
        List<DashboardConfig> dbConfigList = new ArrayList<>();
        String query = "SELECT * FROM dashboard_config WHERE study_id = ? "
                     + "ORDER BY chart_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                dbConfigList.add(new DashboardConfig(rs));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve the dashboard config for " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return dbConfigList;
    }
    
    // Return the dashboard config for this chart ID defined under this study.
    public DashboardConfig getDBCForChartID(String chart_id) {
        DashboardConfig dbc = null;
        Connection conn = null;
        String query = "SELECT * FROM dashboard_config WHERE chart_id = ? AND "
                     + "study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, chart_id);
            stm.setString(2, study_id);
            ResultSet rs = stm.executeQuery();

            if (rs.next()) {
                dbc = new DashboardConfig(rs);
            }
            stm.close();            
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve the dashboard config for " + chart_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return dbc;
    }
    
    // Insert the new dashboard configuration into database.
    public void insertDBCsForNewStudy() {
        Connection conn = null;
        String query = "INSERT INTO dashboard_config(chart_id,data_source_x,data_source_y,title,inverted,study_id) "
                     + "VALUES(\'PIECL\',\'age\',\'\',\'Age Group Breakdown Chart\',false,\'" + study_id 
                     + "\'),(\'PIECR\',\'race\',\'\',\'Ethnicity Group Breakdown Chart\',false,\'" + study_id
                     + "\'),(\'BARCL\',\'race\',\'gender\',\'Ethnicity Breakdown by Gender Chart\',false,\'" + study_id
                     + "\'),(\'BARCR\',\'race\',\'casecontrol\',\'Ethnicity Breakdown by Case Control Chart\',false,\'" + study_id + "\')";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to create dashboard config for " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Update title, data_source_x, data_source_y and inverted in database.
    public boolean updateDBC(DashboardConfig dbc) {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "UPDATE dashboard_config SET title = ?, "
                     + "data_source_x = ?, data_source_y = ?, inverted = ? "
                     + "WHERE chart_id = ? AND study_id = ?";
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dbc.getTitle());
            stm.setString(2, dbc.getData_source_x());
            stm.setString(3, dbc.getData_source_y());
            stm.setBoolean(4, dbc.isInverted());
            stm.setString(5, dbc.getChart_id());
            stm.setString(6, dbc.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.info("Updated dashboard config for " + dbc.getStudy_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update dashboard config for " + dbc.getStudy_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
}
