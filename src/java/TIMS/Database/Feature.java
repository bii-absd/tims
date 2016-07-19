/*
 * Copyright @2016
 */
package TIMS.Database;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Feature is used to represent the feature table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 18-Jul-2016
 * 
 * Revision History
 * 21-Jul-2016 - Created with all the standard getters and setters. Implemented
 * method, getActiveStatus() to return ON|OFF based on the active status.
 */

public class Feature implements Serializable {
    private String fcode;
    private boolean active;
    
    // Construct the Feature object directly using the result set returned from
    // the database query.
    public Feature(ResultSet rs) throws SQLException {
        this.fcode = rs.getString("fcode");
        this.active = rs.getBoolean("active");
    }
    
    // Return active status.
    public String getActiveStatus() {
        return active?"ON":"OFF";
    }
    
    // Machine generated getters and setters.
    public String getFcode() {
        return fcode;
    }
    public void setFcode(String fcode) {
        this.fcode = fcode;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}
