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
 */

public class FinalizedOutput {
    String subject_id;
    int array_index, job_id;

    // Machine generated code
    public FinalizedOutput(String subject_id, int job_id) {
        this.subject_id = subject_id;
        this.job_id = job_id;
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
