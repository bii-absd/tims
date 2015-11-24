/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import java.sql.Date;

/**
 * PipelineRecord is used to represent the pipeline_record table in the 
 * database.
 * 
 * Author: Tay Wei Hong
 * Date: 20-Nov-2015
 * 
 * Revision History
 * 20-Nov-2015 - Created with all the standard getters and setters.
 * 24-Nov-2015 - Added one variable pipeline_name;
 */

public class PipelineRecord {
    String tid, pid, study_id, pipeline_name;
    int array_index;
    Date rdate;

    // Machine generated code
    public PipelineRecord(String tid, String pid, String study_id, 
            String pipeline_name, int array_index, Date rdate) {
        this.tid = tid;
        this.pid = pid;
        this.study_id = study_id;
        this.pipeline_name = pipeline_name;
        this.array_index = array_index;
        this.rdate = rdate;
    }

    public String getTid() {
        return tid;
    }
    public void setTid(String tid) {
        this.tid = tid;
    }
    public String getPid() {
        return pid;
    }
    public void setPid(String pid) {
        this.pid = pid;
    }
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getPipeline_name() {
        return pipeline_name;
    }
    public void setPipeline_name(String pipeline_name) {
        this.pipeline_name = pipeline_name;
    }
    public int getArray_index() {
        return array_index;
    }
    public void setArray_index(int array_index) {
        this.array_index = array_index;
    }
    public Date getRdate() {
        return rdate;
    }
    public void setRdate(Date rdate) {
        this.rdate = rdate;
    }
}
