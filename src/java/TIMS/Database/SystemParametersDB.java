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

// Libraries for Java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for Trove
import gnu.trove.map.hash.THashMap;

public abstract class SystemParametersDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SystemParametersDB.class.getName());
//    private static LinkedHashMap<String, String> spHash = new LinkedHashMap<>();
    private static Map<String, String> spHash = new THashMap<>();
    
    // Load all the system parameters defined in the system_parameters table
    // and store them in the spHash.
    public static void loadSystemParameters() {
        // We will only load the system parameters once i.e. everytime there is
        // any changes in the system parameters, TIMS need to restart in order 
        // for the changes to take effect.
        if (spHash.isEmpty()) {
            Connection conn = null;
            String query = "SELECT sys_para_name, sys_para_value FROM system_parameters";
            
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
                
                while (rs.next()) {
                    spHash.put(rs.getString("sys_para_name"), rs.getString("sys_para_value"));
                }
                stm.close();
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to load system parameters!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
    }
    
    // Return the cBioPortal URL setup in the system.
    public static String getcBioPortalUrl() {
        return spHash.get("CBIOPORTAL_URL");
    }
    
    // Return the Tomcat user id setup in the system.
    public static String getTomcatUID() {
        return spHash.get("TOMCAT_UID");
    }
    
    // Return the Tomcat password setup in the system.
    public static String getTomcatPWD() {
        return spHash.get("TOMCAT_PWD");
    }
}
