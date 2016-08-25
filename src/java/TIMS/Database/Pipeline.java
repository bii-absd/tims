/*
 * Copyright @2015-2016
 */
package TIMS.Database;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Pipeline is used to represent the pipeline table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 5-Nov-2015
 * 
 * Revision History
 * 05-Nov-2015 - Created with all the standard getters and setters. Added one 
 * method toString.
 * 24-Nov-2015 - Changed variable name from command_id to name. Added one
 * variable tid (Technology ID).
 * 01-Dec-2015 - Implementation for database 2.0
 * 25-Aug-2016 - Update the constructor to receive the Pipeline parameters 
 * directly from the database. Implementation for database 3.6 Part I.
 */

public class Pipeline implements Serializable {
    private String name, description, tid, command, parameter;
    private boolean editable;

    // Machine generated constructor
    public Pipeline(String name, String description, String tid, String command, 
            String parameter, boolean editable) {
        this.name = name;
        this.description = description;
        this.tid = tid;
        this.command = command;
        this.parameter = parameter;
        this.editable = editable;
    }
    
    // Construct the Pipeline object directly using the result set returned
    // from the database query.
    public Pipeline(ResultSet rs) throws SQLException {
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.tid = rs.getString("tid");
        this.command = rs.getString("command");
        this.parameter = rs.getString("parameter");
        this.editable = rs.getBoolean("editable");
    }

    // Return the String representation of the pipeline command 
    // i.e. name - command parameter
    @Override
    public String toString() {
        return name + " - " + command + " " + parameter;
    }
    
    // Machine generated getters and setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getTid() {
        return tid;
    }
    public void setTid(String tid) {
        this.tid = tid;
    }
    public String getCommand() {
        return command;
    }
    public void setCommand(String command) {
        this.command = command;
    }
    public String getParameter() {
        return parameter;
    }
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
    public boolean isEditable() {
        return editable;
    }
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
