/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Database;

/**
 * OutputItems is used to represent the subject_id, grp_id, pipeline and 
 * array_index that will be retrieved when the system is consolidating the 
 * finalized data from database, and outputting them to file.
 * 
 * Author: Tay Wei Hong
 * Date: 04-Dec-2015
 * 
 * Revision History
 * 04-Dec-2015 - Created with the necessary methods implemented.
 * 22-Jan-2016 - Changed the variable name from tid to pipeline.
 * 28-Mar-2016 - Added one new variable, grp_id.
 */

public class OutputItems {
    private String subject_id, grp_id, pipeline;
    private int array_index;

    public OutputItems(String subject_id, String grp_id, String pipeline, 
            int array_index) 
    {
        this.subject_id = subject_id;
        this.grp_id = grp_id;
        this.pipeline = pipeline;
        this.array_index = array_index;
    }

    // Return a string representation of this object i.e. subject|pipeline|index
    public String toString() {
        return subject_id + "|" + grp_id + "|" + 
               pipeline + "|" + String.valueOf(array_index);
    }
    
    // Machine generated getters and setters
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }
    public String getGrp_id() {
        return grp_id;
    }
    public void setGrp_id(String grp_id) {
        this.grp_id = grp_id;
    }
    public String getPipeline() {
        return pipeline;
    }
    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }
    public int getArray_index() {
        return array_index;
    }
    public void setArray_index(int array_index) {
        this.array_index = array_index;
    }
}
