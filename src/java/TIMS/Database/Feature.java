/*
 * Copyright @2016-2018
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
 * 11-Jun-2018 - Changes due to update in feature table; replaced active 
 * (BOOLEAN) with status (TEXT).
 */

public class Feature implements Serializable {
    private String fcode, status;
    
    // Construct the Feature object directly using the result set returned from
    // the database query.
    public Feature(ResultSet rs) throws SQLException {
        this.fcode = rs.getString("fcode");
        this.status = rs.getString("status");
    }
    
    // Machine generated getters and setters.
    public String getFcode() {
        return fcode;
    }
    public void setFcode(String fcode) {
        this.fcode = fcode;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
