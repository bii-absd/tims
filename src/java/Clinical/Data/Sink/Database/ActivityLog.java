/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Database;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * ActivityLog is used to represent the activity_log table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 26-Jan-2016
 * 
 * Revision History
 * 26-Jan-2016 - Created with all the standard getters and setters. Added one
 * method getTimeString() to return the timestamp in a specific format.
 */

public class ActivityLog implements Serializable {
    // activity_log table attributes
    private String user_id, activity, detail;
    private Timestamp now;
    private final static DateFormat df = 
            new SimpleDateFormat("dd-MMM-yyyy hh:mmaa");

    public ActivityLog(String user_id, String activity, String detail, 
            Timestamp now) {
        this.user_id = user_id;
        this.activity = activity;
        this.detail = detail;
        this.now = now;
    }

    // Return the timestamp in format "dd-MMM-yyyy hh:mmaa" for showing in 
    // the activity tracking page.
    public String getTimeString() {
        return df.format(now);
    }
    
    // Machine generated getters and setters.
    public String getUser_id() {
        return user_id;
    }
    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
    public String getActivity() {
        return activity;
    }
    public void setActivity(String activity) {
        this.activity = activity;
    }
    public String getDetail() {
        return detail;
    }
    public void setDetail(String detail) {
        this.detail = detail;
    }
    public Timestamp getNow() {
        return now;
    }
    public void setNow(Timestamp now) {
        this.now = now;
    }
}
