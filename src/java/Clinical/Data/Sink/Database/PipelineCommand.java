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
 * 24-Nov-2015 - Changed variable name from command_id to pipeline_name. Added
 * one variable tid (Technology ID).
 */

public class PipelineCommand implements Serializable {
    private String pipeline_name;
    private String tid;
    private String command_code;
    private String command_para;

    // Machine generated constructor
    public PipelineCommand(String pipeline_name, String tid, 
            String command_code, String command_para) {
        this.pipeline_name = pipeline_name;
        this.tid = tid;
        this.command_code = command_code;
        this.command_para = command_para;
    }

    // Return the String representation of the pipeline command 
    // i.e. pipeline_name - command parameter
    public String toString() {
        return pipeline_name + " - " + command_code + " " + command_para;
    }
    
    // Machine generated getters and setters
    public String getPipeline_name() {
        return pipeline_name;
    }
    public void setPipeline_name(String pipeline_name) {
        this.pipeline_name = pipeline_name;
    }
    public String getTid() {
        return tid;
    }
    public void setTid(String tid) {
        this.tid = tid;
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
