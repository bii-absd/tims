/*
 * Copyright @2017
 */
package TIMS.Database;

import TIMS.General.Constants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * VisualProfileDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the visual_profile table and 
 * visual_profile_detail view in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 9-Oct-2017
 * 
 * Revision History
 * 09-Oct-2017 - First baseline with 3 static methods (getProfileDescription,
 * getProfileListForVisualiser and getPipelineListForVisualProfile).
 */

public abstract class VisualProfileDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(VisualProfileDB.class.getName());

    // Return all the visual_profile currently setup in the database.
    // TO BE IMPLEMENTED WHEN NEEDED!
    public static List<VisualProfile> getAllVisualProfile() {
        List<VisualProfile> vpList = new ArrayList<>();
        String query = "SELECT * FROM visual_profile_detail ORDER BY vname, profile";
        
        return vpList;
    }
    
    // Return the description for this profile that is defined for this
    // visualiser.
    public static String getProfileDescription(String vname, String profile) {
        Connection conn = null;
        String profDesc = Constants.DATABASE_INVALID_STR;
        String query = "SELECT description FROM visual_profile_detail "
                     + "WHERE vname = ? AND profile = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, vname);
            stm.setString(2, profile);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                profDesc = rs.getString("description");
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve description for profile " + profile 
                        + " defined in " + vname);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return profDesc;
    }
    
    // Return the list of profiles that are defined for this visualiser.
    public static List<String> getProfileListForVisualiser(String vname) {
        Connection conn = null;
        String query = "SELECT profile FROM visual_profile WHERE vname = ?";
        List<String> profileList = new ArrayList<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, vname);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                profileList.add(rs.getString("profile"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve profile list for visualiser " + vname);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return profileList;
    }
    
    // Return the list of pipelines that are group under this profile in this
    // visualiser.
    public static List<String> getPipelineListForVisualProfile
        (String vname, String profile) {
        Connection conn = null;
        String query = "SELECT pipeline_name FROM visual_profile_detail "
                     + "WHERE vname = ? AND profile = ?";
        List<String> plList = new ArrayList<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, vname);
            stm.setString(2, profile);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                plList.add(rs.getString("pipeline_name"));
            }            
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve pipeline list for profile " + 
                         profile + " in visualiser " + vname);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return plList;        
    } 
}
