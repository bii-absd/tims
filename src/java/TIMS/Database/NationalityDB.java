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
import java.util.Map;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for Trove
import gnu.trove.map.hash.THashMap;

public class NationalityDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(NationalityDB.class.getName());
    private final static Map<String, String> nationalityCodeHash = new THashMap<>();
    private final static Map<String, String> nationalityNameHash = new THashMap<>();

    // Return the list of nationality code setup in the database.
    public Map<String, String> getNationalityCodeHash() {
        // We will only build the nationality code list once.
        if (nationalityCodeHash.isEmpty()) {
            Connection conn = null;
            
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement
                        ("SELECT * FROM nationality ORDER BY country_code");
                ResultSet rs = stm.executeQuery();
            
                while (rs.next()) {
                    // Country Name -> Country Code
                    nationalityCodeHash.put(rs.getString("country_name"), 
                                            rs.getString("country_code"));
                    // Country Code -> Country Name
                    nationalityNameHash.put(rs.getString("country_code"), 
                                            rs.getString("country_name"));
                }
                stm.close();
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to retrieve nationality code!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
        
        return nationalityCodeHash;
    }
    
    // Return the country name for this country code.
    public static String getCountryName(String country_code) {
        if (nationalityNameHash.isEmpty()) {
            return Constants.DATABASE_INVALID_STR;
        }
        return nationalityNameHash.get(country_code);
    }
}
