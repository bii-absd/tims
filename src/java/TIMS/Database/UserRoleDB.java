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
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for Trove
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public abstract class UserRoleDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(UserRoleDB.class.getName());
//    private final static LinkedHashMap<String, Integer> 
//            roleNameHash = new LinkedHashMap<>();
    private final static TObjectIntHashMap<String> roleNameHash = 
            new TObjectIntHashMap<String>();
//    private final static LinkedHashMap<Integer, String> 
//            roleIDHash = new LinkedHashMap<>();
    private final static TIntObjectHashMap<String> roleIDHash = 
            new TIntObjectHashMap<String>();
    
    // Return the list of Role setup in the database
//    public static LinkedHashMap<String, Integer> getRoleNameHash() {
    public static TObjectIntHashMap<String> getRoleNameHash() {
        // We will only build the roleList once
        if (roleNameHash.isEmpty()) {
            Connection conn = null;
            String query = "SELECT * from user_role ORDER BY role_id";
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
            
                while (rs.next()) {
                    // Build the 2 Hash Map; One is Role ID -> Role Name, 
                    // the other is Role Name -> Role ID.
                    roleNameHash.put(rs.getString("role_name"),
                                     rs.getInt("role_id"));
                    roleIDHash.put(rs.getInt("role_id"), 
                                   rs.getString("role_name"));
                }
                stm.close();
            } 
            catch (SQLException|NamingException e) {
                logger.error("FAIL to query user role!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
        
        return roleNameHash;
    }
    
    // Helper function to return the Role ID from the role name map.
    private static int getRoleIDFromHash(String roleName) {
        if (roleNameHash.isEmpty()) {
            return Constants.DATABASE_INVALID_ID;
        }
        return roleNameHash.get(roleName);            
    }
    
    // Return the Role using the value stored in HashMap roleIDHash.
    public static String getRoleNameFromHash(int roleID) {
        if (roleIDHash.isEmpty()) {
            return Constants.DATABASE_INVALID_STR;
        }
        return roleIDHash.get(roleID);
    }
    
    // Return the role ID for each role defined in the system.
    public static int admin() {
        return getRoleIDFromHash("Admin");
    }
    public static int director() {
        return getRoleIDFromHash("Director");
    }
    public static int hod() {
        return getRoleIDFromHash("HOD");
    }
    public static int pi() {
        return getRoleIDFromHash("PI");
    }
    public static int user() {
        return getRoleIDFromHash("User");
    }
    public static int guest() {
        return getRoleIDFromHash("Guest");
    }
    
    // Return true if the role ID passed in is a Director/HOD/PI.
    public static boolean isLead(int roleID) {
        return (roleID == director()) || (roleID == hod()) || (roleID == pi());
    }
}
