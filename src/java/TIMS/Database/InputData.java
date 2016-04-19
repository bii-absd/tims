/*
 * Copyright @2015-2016
 */
package TIMS.Database;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * InputData is used to represent the input_data table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 16-Dec-2015
 * 
 * Revision History
 * 16-Dec-2015 - Created with all the standard getters and setters.
 * 30-Dec-2015 - To implement Serializable else will encounter 
 * IllegalStateException : InstantiationException.
 * 13-Jan-2016 - One new field user_id added in the input_data table; to 
 * identify the user who has uploaded this input data.
 * 21-Jan-2016 - Added one new field pipeline_name in the input_data table; to
 * associate this input_data with the respective pipeline.
 * 14-Apr-2016 - Change type for date to Timestamp.
 */

public class InputData implements Serializable {
    // input_data table attributes
    private String study_id, user_id, pipeline_name, filename, 
                   filepath, description;
    private Timestamp date;
    private int sn;
    private final static DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mmaa");
    
    public InputData(String study_id, String user_id, String pipeline_name, 
            String filename, String filepath, String description, int sn, 
            Timestamp date) {
        this.study_id = study_id;
        this.user_id = user_id;
        this.pipeline_name = pipeline_name;
        this.filename = filename;
        this.filepath = filepath;
        this.description = description;
        this.sn = sn;
        this.date = date;
    }
    
    // Return the timestamp in format "dd-MMM-yyyy hh:mmaa" for showing in 
    // pipeline config pages.
    public String getDateString() {
        return df.format(date);
    }
    
    // Machine generated getters and setters.
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getUser_id() {
        return user_id;
    }
    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
    public String getPipeline_name() {
        return pipeline_name;
    }
    public void setPipeline_name(String pipeline_name) {
        this.pipeline_name = pipeline_name;
    }
    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public String getFilepath() {
        return filepath;
    }
    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public int getSn() {
        return sn;
    }
    public void setSn(int sn) {
        this.sn = sn;
    }
    public Timestamp getDate() {
        return date;
    }
    public void setDate(Timestamp date) {
        this.date = date;
    }
}
