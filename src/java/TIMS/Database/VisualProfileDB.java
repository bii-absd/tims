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
package TIMS.Database;

import TIMS.General.Constants;
// Libraries for Java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class VisualProfileDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(VisualProfileDB.class.getName());
    private final String vname;
    
    // Machine generated constructor.
    public VisualProfileDB(String vname) {
        this.vname = vname;
    }
    
    // Return the description for this profile that is defined for this
    // visualiser.
    public String getProfileDescription(String profile) {
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
            StringBuilder err = new StringBuilder
                ("FAIL to retrieve description for profile ").append(profile).
                append(" defined in ").append(vname);
            logger.error(err);
//            logger.error("FAIL to retrieve description for profile " + profile 
//                        + " defined in " + vname);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return profDesc;
    }
    
    // Return the list of profiles that are defined for this visualiser.
    public List<String> getProfileListForVisualiser() {
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
            logger.error("FAIL to retrieve profile list for visualiser!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return profileList;
    }
    
    /* NOT IN USE!
    // Return the list of pipelines that are group under this profile in this
    // visualiser.
    public List<String> getPipelineListForVisualProfile
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
            StringBuilder err = new StringBuilder
                ("FAIL to retrieve pipeline list for profile ").
                    append(profile).append(" in visualiser ").append(vname);
            logger.error(err);
//            logger.error("FAIL to retrieve pipeline list for profile " + 
//                         profile + " in visualiser " + vname);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return plList;        
    }
    */
}
