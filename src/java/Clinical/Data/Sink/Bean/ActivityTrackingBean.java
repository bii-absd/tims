/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.ActivityLog;
import Clinical.Data.Sink.Database.ActivityLogDB;
import Clinical.Data.Sink.Database.UserAccountDB;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ActivityTrackingBean is the backing bean for the activitytracking view.
 * 
 * Author: Tay Wei Hong
 * Date: 26-Jan-2016
 * 
 * Revision History
 * 26-Jan-2016 - Implemented the module for tracking user activities, and 
 * specific activity.
 * 29-Jan-2016 - Joined the methods for tracking user activities and specific
 * activity into one method.
 * 01-Feb-2016 - Further enhanced the query logic by combing the 4 query methods
 * into one, and to include the time (from and/or to) for user selection.
 */

@ManagedBean (name = "actiBean")
@ViewScoped
public class ActivityTrackingBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ActivityTrackingBean.class.getName());
    private List<ActivityLog> activityLog;
    private final LinkedHashMap<String,String> userIDHash;
    private String trackUser, trackActi;
    private Date from, to;
    // Store the user ID of the current user.
    private final String userName;
    
    public ActivityTrackingBean() {
        userIDHash = UserAccountDB.getUserIDHash();
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        logger.debug("ActivityTrackingBean created.");
        logger.info(userName + ": access Activity Tracking page.");
    }

    // Proceed to retrieve the activity log based on user selection.
    public void retrieveActivity() {
        List<String> para = new ArrayList<>();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String fromStr = null;
        String toStr = null;

        if (trackUser != null) {
            para.add("user_id = \'" + trackUser + "\'");
        }
        if (trackActi != null) {
            para.add("activity = \'" + trackActi + "\'");
        }
        if (from != null) {
            fromStr = df.format(from);
            para.add("time >= \'" + fromStr + "\'");
        }
        if (to != null) {
            toStr = df.format(to);
            para.add("time <= \'" + toStr + "\'");
        }

        activityLog = ActivityLogDB.retrieveActivityLog(para);
    }
    
    // Machine generated getters.
    public LinkedHashMap<String, String> getUserIDHash() {
        return userIDHash;
    }
    public String getTrackUser() {
        return trackUser;
    }
    public void setTrackUser(String trackUser) {
        this.trackUser = trackUser;
    }
    public String getTrackActi() {
        return trackActi;
    }
    public void setTrackActi(String trackActi) {
        this.trackActi = trackActi;
    }
    public List<ActivityLog> getActivityLog() {
        return activityLog;
    }
    public Date getFrom() {
        return from;
    }
    public void setFrom(Date from) {
        this.from = from;
    }
    public Date getTo() {
        return to;
    }
    public void setTo(Date to) {
        this.to = to;
    }
}
