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
 * PipelineCommandDB is not mean to be instantiate, its main job is to perform
 * SQL operations on the pipeline_command table in the database.
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
 * Command to PipelineCommand.
 */

public class PipelineCommandDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(PipelineCommandDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private final static List<PipelineCommand> commandList = new ArrayList<>();

    public PipelineCommandDB() {}
    
    // Return the command_code and command_para for this command_id.
    public static PipelineCommand getPipelineCommand(String command_id) 
            throws SQLException {
        PipelineCommand command = null;
        String queryStr = 
                "SELECT command_code, command_para FROM pipeline_command "
                + "WHERE command_id = ?";
        PreparedStatement queryStm = conn.prepareStatement(queryStr);
        
        queryStm.setString(1, command_id);
        ResultSet result = queryStm.executeQuery();
        
        if (result.next()) {
            command = new PipelineCommand(command_id,
                            result.getString("command_code"),
                            result.getString("command_para"));
            
            logger.debug(command.toString());
        }

        return command;
    }
    
    // Return all the pipeline command currently setup in the database.
    public static List<PipelineCommand> getAllPipelineCommand() 
            throws SQLException {
        // Only execute the query if the list is empty
        // To prevent the query from being run multiple times.
        if (commandList.isEmpty()) {
            String queryStr = "SELECT * FROM pipeline_command";
            PreparedStatement queryStm = conn.prepareStatement(queryStr);
            ResultSet result = queryStm.executeQuery();
            int index = 0;
            
            while (result.next()) {
                PipelineCommand tmp = new PipelineCommand(
                                        result.getString("command_id"),
                                        result.getString("command_code"),
                                        result.getString("command_para"));
                // Add the PipelineCommand to the command list
                commandList.add(index++, tmp);
            }
        }
        return commandList;
    }
    
    // Update the pipeline command in the database.
    public static Boolean updatePipelineCommand(PipelineCommand cmd) {
        Boolean result = Constants.OK;
        String updateStr = "UPDATE pipeline_command SET command_code = ?, "
                + "command_para = ? WHERE command_id = ?";
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr)) {
            updateStm.setString(1, cmd.getCommand_code());
            updateStm.setString(2, cmd.getCommand_para());
            updateStm.setString(3, cmd.getCommand_id());
        
            updateStm.executeUpdate();            
        }
        catch (SQLException e) {
            logger.error("SQLException when updating pipeline command: "
                    + cmd.getCommand_id());
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Insert the new pipeline command into database.
    public static Boolean insertPipelineCommand(PipelineCommand cmd) {
        Boolean result = Constants.OK;
        String insertStr = "INSERT INTO pipeline_command"
                + "(command_id,command_code,command_para) VALUES(?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, cmd.getCommand_id());
            insertStm.setString(2, cmd.getCommand_code());
            insertStm.setString(3, cmd.getCommand_para());
            
            insertStm.executeUpdate();
            // Clear the command list, so that it will be rebuild again.
            commandList.clear();
            logger.debug("New pipeline command inserted into database: " + 
                    cmd.getCommand_id());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting pipeline command.");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
}
