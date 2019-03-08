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

import TIMS.General.Constants;
import TIMS.General.ResourceRetriever;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class SubmittedJob implements Serializable {
    private int job_id, status_id, input_sn;
    private String study_id, user_id, pipeline_name, input_desc, 
                   parameters, output_file, detail_output, report;
    private Timestamp submit_time, complete_time;
    private boolean cbio_target;
    // status_name will be used by the job status page
    private String status_name;
    private final static DateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mmaa");

    // Full constructor to be used during database query.
    public SubmittedJob(ResultSet rs) throws SQLException {
        this.job_id = rs.getInt("job_id");
        this.study_id = rs.getString("study_id");
        this.user_id = rs.getString("user_id");
        this.pipeline_name = rs.getString("pipeline_name");
        this.status_id = rs.getInt("status_id");
        this.submit_time = rs.getTimestamp("submit_time");
        this.complete_time = rs.getTimestamp("complete_time");
        this.input_sn = rs.getInt("input_sn");
        this.input_desc = rs.getString("input_desc");
        this.parameters = rs.getString("parameters");
        this.output_file = rs.getString("output_file");
        this.detail_output = rs.getString("detail_output");
        this.report = rs.getString("report");
        this.cbio_target = rs.getBoolean("cbio_target");
    }
    
    // Full constructor to be used by pipeline beans.
    public SubmittedJob(String study_id, String user_id,
            String pipeline_name, int status_id, Timestamp submit_time, 
            Timestamp complete_time, int input_sn, String input_desc, 
            String parameters, String output_file, String detail_output, 
            String report) 
    {
        this.job_id = 0;
        this.study_id = study_id;
        this.user_id = user_id;
        this.pipeline_name = pipeline_name;
        this.status_id = status_id;
        this.submit_time = submit_time;
        this.complete_time = complete_time;
        this.input_sn = input_sn;
        this.input_desc = input_desc;
        this.parameters = parameters;
        this.output_file = output_file;
        this.detail_output = detail_output;
        this.report = report;
    }

    // Return the job status name of this submitted job.
    public String getStatus_name() {
        return JobStatusDB.getJobStatusName(status_id);
    }
    
    // Based on the job status, the download link for output at jobstatus.xhtml 
    // will be enabled or disabled accordingly.
    public String getOutputReady() {
        if ( (status_id == JobStatusDB.completed()) ||
             (status_id == JobStatusDB.finalized()) )
        {
            return Constants.FALSE;
        }
        else {
            return Constants.TRUE;
        }
    }
    // Download link for report will also be enabled for failed job.
    public String getReportReady() {
        if ( (status_id == JobStatusDB.completed()) || 
             (status_id == JobStatusDB.finalized()) ||
             (status_id == JobStatusDB.failed()) )
        {
            return Constants.FALSE;
        }
        else {
            return Constants.TRUE;
        }        
    }
    
    // Retrieve and return the text description for this pipeline from the 
    // resource bundle.
    public String getPlDescription() {
        return ResourceRetriever.getMsg(pipeline_name);
    }
    
    // Return the submit_time and complete_time in format "dd-MMM-yyyy hh:mmaa"
    // for showing in pipeline job status page.
    public String getSubmitTimeString() {
        return df.format(submit_time);
    }
    public String getCompleteTimeString() {
        if (complete_time != null) {
            return df.format(complete_time);
        }
        else {
            return "In-progress";
        }
    }
    
    // Machine generated getters and setters
    public int getJob_id() {
        return job_id;
    }
    public void setJob_id(int job_id) {
        this.job_id = job_id;
    }
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
    public int getStatus_id() {
        return status_id;
    }
    public void setStatus_id(int status_id) {
        this.status_id = status_id;
    }
    public Timestamp getSubmit_time() {
        return submit_time;
    }
    public void setSubmit_time(Timestamp submit_time) {
        this.submit_time = submit_time;
    }
    public Timestamp getComplete_time() {
        return complete_time;
    }
    public void setComplete_time(Timestamp complete_time) {
        this.complete_time = complete_time;
    }
    public int getInput_sn() {
        return input_sn;
    }
    public void setInput_sn(int input_sn) {
        this.input_sn = input_sn;
    }
    public String getInput_desc() {
        return input_desc;
    }
    public void setInput_desc(String input_desc) {
        this.input_desc = input_desc;
    }
    public String getParameters() {
        return parameters;
    }
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    public String getOutput_file() {
        return output_file;
    }
    public void setOutput_file(String output_file) {
        this.output_file = output_file;
    }
    public String getDetail_output() {
        return detail_output;
    }
    public void setDetail_output(String detail_output) {
        this.detail_output = detail_output;
    }
    public String getReport() {
        return report;
    }
    public void setReport(String report) {
        this.report = report;
    }
    public boolean isCbio_target() {
        return cbio_target;
    }
    public void setCbio_target(boolean cbio_target) {
        this.cbio_target = cbio_target;
    }
}
