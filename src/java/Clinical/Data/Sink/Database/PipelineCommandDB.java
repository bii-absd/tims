/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

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

 */

public class PipelineCommandDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(PipelineCommandDB.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private final static List<PipelineCommand> commandList = new ArrayList<>();

    public PipelineCommandDB() {}
    
    // Return the command_code and command_para for this command_id.
    public static PipelineCommand getCommand(String command_id) 
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
    public static List<PipelineCommand> getAllCommand() throws SQLException {
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
}
