/*
 * Copyright @2015-2016
 */
package TIMS.Database;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * 25-Aug-2016 - Added new method getUpdateTimeString(), and renamed method 
 * getDateString() to getCreateTimeString(). To construct the InputData object
 * directly from the ResultSet returned from the database. Implementation for 
 * database 3.6 Part I.
 */

public class InputData implements Serializable {
    // input_data table attributes
    private String study_id, create_uid, update_uid, pipeline_name, filename, 
                   filepath, description;
    private Timestamp create_time, update_time;
    private int sn;
    private final static DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mmaa");
    
    // This constructor is used for constructing new InputData.
    public InputData(String study_id, String create_uid, String pipeline_name, 
            String filename, String filepath, String description, int sn, 
            Timestamp create_time) {
        this.study_id = study_id;
        this.create_uid = create_uid;
        this.pipeline_name = pipeline_name;
        this.filename = filename;
        this.filepath = filepath;
        this.description = description;
        this.sn = sn;
        this.create_time = create_time;
    }
    
    // Construct the InputData object directly using the result set returned 
    // from the database query.
    public InputData(ResultSet rs) throws SQLException {
        this.study_id = rs.getString("study_id");
        this.create_uid = rs.getString("create_uid");
        this.update_uid = rs.getString("update_uid");
        this.pipeline_name = rs.getString("pipeline_name");
        this.filename = rs.getString("filename");
        this.filepath = rs.getString("filepath");
        this.description = rs.getString("description");
        this.create_time = rs.getTimestamp("create_time");
        this.update_time = rs.getTimestamp("update_time");
        this.sn = rs.getInt("sn");
    }

    // Return the creation timestamp in format "dd-MMM-yyyy hh:mmaa" for 
    // showing in pipeline config pages.
    public String getCreateTimeString() {
        return df.format(create_time);
    }
    // Return the last update timestamp in format "dd-MMM-yyyy hh:mmaa".
    public String getUpdateTimeString() {
        if (update_time != null) {
            return df.format(update_time);
        }
        // Return a empty string if this input data has not been updated before.
        return "";
    }
    
    // Machine generated getters and setters.
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getCreate_uid() {
        return create_uid;
    }
    public void setCreate_uid(String create_uid) {
        this.create_uid = create_uid;
    }
    public String getUpdate_uid() {
        return update_uid;
    }
    public void setUpdate_uid(String update_uid) {
        this.update_uid = update_uid;
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
    public Timestamp getCreate_time() {
        return create_time;
    }
    public void setCreate_time(Timestamp create_time) {
        this.create_time = create_time;
    }
    public Timestamp getUpdate_time() {
        return update_time;
    }
    public void setUpdate_time(Timestamp update_time) {
        this.update_time = update_time;
    }
}
