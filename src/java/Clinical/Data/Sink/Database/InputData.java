/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import java.io.Serializable;

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
 */

public class InputData implements Serializable {
    // input_data table attributes
    private String study_id, filename, filepath, description, date;
    private int sn;

    public InputData(String study_id, String filename, String filepath, 
            String description, int sn, String date) {
        this.study_id = study_id;
        this.filename = filename;
        this.filepath = filepath;
        this.description = description;
        this.sn = sn;
        this.date = date;
    }
    
    // Machine generated getters and setters.
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
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
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
}
