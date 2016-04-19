/*
 * Copyright @2015
 */
package TIMS.Database;

import java.io.Serializable;

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
 */

public class Pipeline implements Serializable {
    private String name;
    private String tid;
    private String code;
    private String parameter;

    // Machine generated constructor
    public Pipeline(String name, String tid, String code, String parameter) {
        this.name = name;
        this.tid = tid;
        this.code = code;
        this.parameter = parameter;
    }

    // Return the String representation of the pipeline command 
    // i.e. name - command parameter
    public String toString() {
        return name + " - " + code + " " + parameter;
    }
    
    // Machine generated getters and setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getTid() {
        return tid;
    }
    public void setTid(String tid) {
        this.tid = tid;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getParameter() {
        return parameter;
    }
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
}
