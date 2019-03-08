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
package TIMS.Bean;

import TIMS.Database.ActivityLog;
import TIMS.Database.ActivityLogDB;
import TIMS.Database.UserAccountDB;
// Libraries for Java
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.faces.context.FacesContext;
import javax.inject.Named;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("actiBean")
@ViewScoped
public class ActivityTrackingBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ActivityTrackingBean.class.getName());
    private List<ActivityLog> activityLog;
    private final LinkedHashMap<String,String> userIDHash;
    private LinkedHashMap<String,String> activityList;
    private String trackUser, trackActi;
    private Date from, to;
    // Store the user ID of the current user.
    private final String userName;
    private final ActivityLogDB activityDB;
    
    public ActivityTrackingBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        activityDB = new ActivityLogDB();
        activityList = activityDB.getActivityList();
        // Currently only allow director and administrator to use this module.
        if (UserAccountDB.isDirector(userName)) {
            // Director can only see the activities of all users under his/her
            // institution.
            userIDHash = UserAccountDB.getInstUserIDHash(
                            UserAccountDB.getUnitID(userName));
        }
        else {
            // Administrator is allowed to see the activities of all users.
            userIDHash = UserAccountDB.getUserIDHash();
        }
        
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
        else {
            // Director is only allowed to see the activities from the users
            // coming from his/her institution.
            if (UserAccountDB.isDirector(userName)) {
                para.add("user_id IN (" + 
                    getInstUserIDQuery(UserAccountDB.getUnitID(userName)) + ")");
            }
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

        activityLog = activityDB.retrieveActivityLog(para);
    }
    
    // Return the query that can be used to retrieve the list of users that 
    // belong to this institution.
    private String getInstUserIDQuery(String instID) {
//        String query 
//            = "SELECT user_id FROM user_account WHERE unit_id IN ("
//            + "(SELECT inst_id AS unit_id FROM inst_dept_grp WHERE inst_id = \'" + instID + "\') UNION "
//            + "(SELECT dept_id AS unit_id FROM inst_dept_grp WHERE inst_id = \'" + instID + "\') UNION "
//            + "(SELECT grp_id AS unit_id FROM inst_dept_grp WHERE inst_id = \'" + instID + "\'))";
        StringBuilder query = new 
            StringBuilder("SELECT user_id FROM user_account WHERE unit_id IN (").
                append("(SELECT inst_id AS unit_id FROM inst_dept_grp WHERE inst_id = \'").
                append(instID).append("\') UNION ").
                append("(SELECT dept_id AS unit_id FROM inst_dept_grp WHERE inst_id = \'").
                append(instID).append("\') UNION ").
                append("(SELECT grp_id AS unit_id FROM inst_dept_grp WHERE inst_id = \'").
                append(instID).append("\'))");
        
        return query.toString();
    }
    
    // Return the list of activity currently available in the database.
    public LinkedHashMap<String, String> getActivityList() {    
        return activityList;
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
