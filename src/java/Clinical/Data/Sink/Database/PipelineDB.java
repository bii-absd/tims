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
 * PipelineDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the pipeline table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 5-Nov-2015
 * 
 * Revision History
 * 05-Nov-2015 - First baseline with two static methods (getCommand and 
 * getAllCommand) created.
 * 06-Nov-2015 - Added one new method updateCommand to update the pipeline
 * command in database.
 * 16-Nov-2015 - Added one new method insertPipelineCommand to insert a new
 * pipeline command into database. Updated the name for all methods i.e. from
 * Command to Pipeline.
 * 24-Nov-2015 - Changed variable name from command_id to name. Added one
 * variable tid (Technology ID).
 * 25-Nov-2015 - Added one new method getPipelineTechnology.
 * 01-Dec-2015 - Implementation for database 2.0
 * 10-Dec-2015 - Changed to abstract class.
 * 13-Jan-2016 - Removed all the static variables in Pipeline Management module.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 */

public abstract class PipelineDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(PipelineDB.class.getName());

    // Return the pipeline technology for this pipeline name.
    public static String getPipelineTechnology(String pipeline_name) {
        Connection conn = null;
        String query = "SELECT tid FROM pipeline WHERE name = ?";
        String tid = null;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, pipeline_name);
            ResultSet result = stm.executeQuery();
            
            if (result.next()) {
                tid = result.getString("tid");
            }
            
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve pipeline technology!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return tid;
    }
    
    // Return the pipeline object for this pipeline.
    public static Pipeline getPipeline(String pipeline_name) 
            throws SQLException, NamingException {
        Connection conn = null;
        Pipeline command = null;
        String query = 
                "SELECT tid, code, parameter FROM pipeline WHERE name = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        
        stm.setString(1, pipeline_name);
        ResultSet rs = stm.executeQuery();
        
        if (rs.next()) {
            command = new Pipeline(pipeline_name,
                            rs.getString("tid"),
                            rs.getString("code"),
                            rs.getString("parameter"));
            
            logger.debug(command.toString());
        }

        stm.close();
        DBHelper.closeDSConn(conn);
        
        return command;
    }
    
    // Return all the pipeline currently setup in the database.
    public static List<Pipeline> getAllPipeline() 
            throws SQLException, NamingException 
    {
        Connection conn = null;
        int index = 0;
        List<Pipeline> plList = new ArrayList<>();
        String query = "SELECT * FROM pipeline";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        ResultSet rs = stm.executeQuery();
            
        while (rs.next()) {
            Pipeline tmp = new Pipeline(
                                rs.getString("name"),
                                rs.getString("tid"),
                                rs.getString("code"),
                                rs.getString("parameter"));
            // Add the Pipeline to the command list
            plList.add(index++, tmp);
        }

        stm.close();
        DBHelper.closeDSConn(conn);
        
        return plList;
    }
    
    // Update the pipeline command in the database.
    public static Boolean updatePipeline(Pipeline cmd) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE pipeline SET code = ?, "
                     + "parameter = ?, tid = ? WHERE name = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, cmd.getCode());
            stm.setString(2, cmd.getParameter());
            stm.setString(3, cmd.getTid());
            stm.setString(4, cmd.getName());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated pipeline: " + cmd.getName());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update pipeline: " + cmd.getName());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Insert the new pipeline into database.
    public static Boolean insertPipeline(Pipeline cmd) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO pipeline"
                     + "(name,tid,code,parameter) VALUES(?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, cmd.getName());
            stm.setString(2, cmd.getTid());
            stm.setString(3, cmd.getCode());
            stm.setString(4, cmd.getParameter());            
            stm.executeUpdate();
            stm.close();
            
            logger.debug("New pipeline inserted into database: " + 
                    cmd.getName());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert pipeline!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
}
