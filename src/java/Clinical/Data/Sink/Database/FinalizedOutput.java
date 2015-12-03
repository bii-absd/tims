/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

/**
 * FinalizedOutput is used to represent the finalized_output table in the 
 * database.
 * 
 * Author: Tay Wei Hong
 * Date: 02-Dec-2015
 * 
 * Revision History
 * 02-Dec-2015 - Created with all the standard getters and setters.
 * 03-Dec-2015 - Added in one new attribute annot_ver.
 */

public class FinalizedOutput {
    String annot_ver, subject_id;
    int array_index, job_id;

    // Machine generated code
    public FinalizedOutput(int array_index, String annot_ver, String subject_id, 
            int job_id) {
        this.array_index = array_index;
        this.annot_ver = annot_ver;
        this.subject_id = subject_id;
        this.job_id = job_id;
    }
    public int getArray_index() {
        return array_index;
    }
    public void setArray_index(int array_index) {
        this.array_index = array_index;
    }
    public String getAnnot_ver() {
        return annot_ver;
    }
    public void setAnnot_ver(String annot_ver) {
        this.annot_ver = annot_ver;
    }
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }
    public int getJob_id() {
        return job_id;
    }
    public void setJob_id(int job_id) {
        this.job_id = job_id;
    }
}
