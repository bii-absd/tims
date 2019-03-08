// Copyright (C) 2019 A*STAR
//
// TIMS (Translation Informatics Management System) is an software effort 
// by the ABSD (Analytics of Biological Sequence Data) team in the 
// Bioinformatics Institute (BII), Agency of Science, Technology and Research 
// (A*STAR), Singapore.
//

// This file is part of TIMS.
// 
// TIMS is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as 
// published by the Free Software Foundation, either version 3 of the 
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
package TIMS.Database;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
