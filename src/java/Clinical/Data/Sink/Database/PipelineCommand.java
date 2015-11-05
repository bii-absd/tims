/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import java.io.Serializable;

/**
 * PipelineCommand is used to represent the pipeline_command table in the 
 * database.
 * 
 * Author: Tay Wei Hong
 * Date: 5-Nov-2015
 * 
 * Revision History
 * 05-Nov-2015 - Created with all the standard getters and setters. Added one 
 * method toString.
 */

public class PipelineCommand implements Serializable {
    private String command_id;
    private String command_code;
    private String command_para;

    // Machine generated constructor
    public PipelineCommand(String command_id, String command_code, 
            String command_para) {
        this.command_id = command_id;
        this.command_code = command_code;
        this.command_para = command_para;
    }

    // Return the String representation of the pipeline command 
    // i.e. command_id - command parameter
    public String toString() {
        return command_id + " - " + command_code + " " + command_para;
    }
    
    // Machine generated getters and setters
    public String getCommand_id() {
        return command_id;
    }
    public void setCommand_id(String command_id) {
        this.command_id = command_id;
    }
    public String getCommand_code() {
        return command_code;
    }
    public void setCommand_code(String command_code) {
        this.command_code = command_code;
    }
    public String getCommand_para() {
        return command_para;
    }
    public void setCommand_para(String command_para) {
        this.command_para = command_para;
    }
}
