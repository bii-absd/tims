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
 */

public abstract class PipelineDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(PipelineDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();

    // Return the pipeline technology for this pipeline name.
    public static String getPipelineTechnology(String pipeline_name) {
        String queryStr = "SELECT tid FROM pipeline WHERE name = ?";
        String tid = null;
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, pipeline_name);
            ResultSet result = queryStm.executeQuery();
            
            if (result.next()) {
                tid = result.getString("tid");
            }
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve pipeline technology!");
            logger.error(e.getMessage());
        }
        
        return tid;
    }
    
    // Return the pipeline object for this pipeline.
    public static Pipeline getPipeline(String pipeline_name) 
            throws SQLException {
        Pipeline command = null;
        String queryStr = 
                "SELECT tid, code, parameter FROM pipeline WHERE name = ?";
        PreparedStatement queryStm = conn.prepareStatement(queryStr);
        
        queryStm.setString(1, pipeline_name);
        ResultSet result = queryStm.executeQuery();
        
        if (result.next()) {
            command = new Pipeline(pipeline_name,
                            result.getString("tid"),
                            result.getString("code"),
                            result.getString("parameter"));
            
            logger.debug(command.toString());
        }

        return command;
    }
    
    // Return all the pipeline currently setup in the database.
    public static List<Pipeline> getAllPipeline() throws SQLException {
        List<Pipeline> plList = new ArrayList<>();
        ResultSet rs = DBHelper.runQuery("SELECT * FROM pipeline");
        int index = 0;
            
        while (rs.next()) {
            Pipeline tmp = new Pipeline(
                                rs.getString("name"),
                                rs.getString("tid"),
                                rs.getString("code"),
                                rs.getString("parameter"));
            // Add the Pipeline to the command list
            plList.add(index++, tmp);
        }

        return plList;
    }
    
    // Update the pipeline command in the database.
    public static Boolean updatePipeline(Pipeline cmd) {
        Boolean result = Constants.OK;
        String updateStr = "UPDATE pipeline SET code = ?, "
                + "parameter = ?, tid = ? WHERE name = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setString(1, cmd.getCode());
            updateStm.setString(2, cmd.getParameter());
            updateStm.setString(3, cmd.getTid());
            updateStm.setString(4, cmd.getName());
        
            updateStm.executeUpdate();
            logger.debug("Updated pipeline: " + cmd.getName());
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update pipeline: " + cmd.getName());
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Insert the new pipeline into database.
    public static Boolean insertPipeline(Pipeline cmd) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO pipeline"
                + "(name,tid,code,parameter) VALUES(?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, cmd.getName());
            insertStm.setString(2, cmd.getTid());
            insertStm.setString(3, cmd.getCode());
            insertStm.setString(4, cmd.getParameter());
            
            insertStm.executeUpdate();
            // Clear the command list, so that it will be rebuild again.
//            pipelineList.clear();
            logger.debug("New pipeline inserted into database: " + 
                    cmd.getName());
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert pipeline!");
            logger.error(e.getMessage());
        }
        
        return result;
    }
}
