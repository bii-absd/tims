/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.ActivityLog;
import Clinical.Data.Sink.Database.ActivityLogDB;
import Clinical.Data.Sink.Database.UserAccountDB;
import java.io.Serializable;
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
 */

@ManagedBean (name = "actiBean")
@ViewScoped
public class ActivityTrackingBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ActivityTrackingBean.class.getName());
    private List<ActivityLog> userActivities;
    private List<ActivityLog> actiLog;
    private final LinkedHashMap<String,String> userIDHash;
    private String trackUser, trackActi;
    // Store the user ID of the current user.
    private final String userName;
    
    public ActivityTrackingBean() {
        userIDHash = UserAccountDB.getUserIDHash();
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        logger.debug("ActivityTrackingBean created.");
        logger.info(userName + ": access Activity Tracking page.");
    }

    // A useer or activity has been selected, proceed to build the list of 
    // activity log.
    public void trackUserChange() {
        userActivities = ActivityLogDB.retrieveUserActivities(trackUser);
    }
    public void trackActiChange() {
        actiLog = ActivityLogDB.retrieveActivityRecords(trackActi);
    }
    
    // A user or activity has been selected, proceed to display the activities 
    // or log.
    public Boolean getUserSelectedStatus() {
        return trackUser != null;
    }
    public Boolean getActiSelectedStatus() {
        return trackActi != null;
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
    public List<ActivityLog> getUserActivities() {
        return userActivities;
    }
    public List<ActivityLog> getActiLog() {
        return actiLog;
    }
}
