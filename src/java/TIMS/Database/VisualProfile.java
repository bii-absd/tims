/*
 * Copyright @2017
 */
package TIMS.Database;

import java.io.Serializable;

/**
 * VisualProfile is used to represent the visual_profile table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 9-Oct-2017
 * 
 * Revision History
 * 09-Oct-2017 - Created with all the standard getters and setters. 
 */

public class VisualProfile implements Serializable {
    private String vpid, vname, profile, description;
    
    // Machine generated constructor
    public VisualProfile(String vpid, String vname, String profile, 
            String description) {
        this.vpid = vpid;
        this.vname = vname;
        this.profile = profile;
        this.description = description;
    }

    // Machine generated getters and setters
    public String getVpid() {
        return vpid;
    }
    public void setVpid(String vpid) {
        this.vpid = vpid;
    }
    public String getVname() {
        return vname;
    }
    public void setVname(String vname) {
        this.vname = vname;
    }
    public String getProfile() {
        return profile;
    }
    public void setProfile(String profile) {
        this.profile = profile;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}
