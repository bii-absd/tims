/*
 * Copyright @2015-2017
 */
package TIMS.Database;

/**
 * OutputItems is used by DataRetriever to generate the consolidated pipeline
 * output during finalization.
 * 
 * Author: Tay Wei Hong
 * Date: 04-Dec-2015
 * 
 * Revision History
 * 04-Dec-2015 - Created with the necessary methods implemented.
 * 22-Jan-2016 - Changed the variable name from tid to pipeline.
 * 28-Mar-2016 - Added one new variable, grp_id.
 * 21-Apr-2017 - Subject Meta data management enhancement. Removed grp_id.
 */

public class OutputItems {
    private String subject_id, pipeline;
    private int array_index;

    public OutputItems(String subject_id, String pipeline, int array_index) 
    {
        this.subject_id = subject_id;
        this.pipeline = pipeline;
        this.array_index = array_index;
    }

    // Return a string representation of this object i.e. subject|pipeline|index
    public String toString() {
        return subject_id + "|" + pipeline + "|" + String.valueOf(array_index);
    }
    
    // Machine generated getters and setters
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
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
