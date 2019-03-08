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
import java.util.LinkedHashMap;
import java.util.Map;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for Trove
import gnu.trove.map.hash.THashMap;

public class ICD10DB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ICD10DB.class.getName());
    private final static LinkedHashMap<String, String> icdCodeHash = 
                                                    new LinkedHashMap<>();
    private final static Map<String, String> icdDescHash = new THashMap<>();
//    private final static LinkedHashMap<String, String> icdDescHash = 
//                                                    new LinkedHashMap<>();
    
    // Return the list of ICD code setup in the system.
    public LinkedHashMap<String, String> getICDCodeHash() {
        if (icdCodeHash.isEmpty()) {
            buildICDHashMaps();
        }
        return icdCodeHash;
    }
    // Return the list of ICD description setup in the system.
    public Map<String, String> getICDDescHash() {
        if (icdDescHash.isEmpty()) {
            buildICDHashMaps();
        }
        return icdDescHash;
    }
    
    // Build the list of icd code and description setup in the database.
    private void buildICDHashMaps() {
        Connection conn = null;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                                    ("SELECT * FROM icd ORDER BY icd_code");
            ResultSet rs = stm.executeQuery();
                
            while (rs.next()) {
                // ICD Description -> ICD Code
                icdCodeHash.put(rs.getString("icd_desc"), 
                                rs.getString("icd_code"));
                // ICD Code -> ICD Description
                icdDescHash.put(rs.getString("icd_code"), 
                                rs.getString("icd_desc"));
            }
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve icd code!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Return the icd description for this icd code.
    public String getICDDescription(String icd_code) {
        if (icdDescHash.isEmpty()) {
            buildICDHashMaps();
            logger.info("ICD10 Map is empty! Building now!");
        }
        return icdDescHash.get(icd_code);
    }
}
