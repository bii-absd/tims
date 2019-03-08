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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class InstitutionDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(InstitutionDB.class.getName());
//    private static Map<String, String> instIDHash = new HashMap<>();
    
    // Insert the new institution ID into database.
    public boolean insertInstitution(Institution inst) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO inst(inst_id,inst_name) VALUES(?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, inst.getInst_id());
            stm.setString(2, inst.getInst_name());
            stm.executeUpdate();
            stm.close();
            // Rebuild the Institution ID HashMap after every insertion.
//            instIDHash.clear();
//            buildInstIDHash();
            
            logger.debug("New institution ID inserted into database: " +
                    inst.getInst_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert new institution ID " + inst.getInst_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update institution name.
    public boolean updateInstitution(Institution inst) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE inst SET inst_name = ? WHERE inst_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, inst.getInst_name());
            stm.setString(2, inst.getInst_id());            
            stm.executeUpdate();
            stm.close();
            // Rebuild the Institution ID HashMap after every update to 
            // institution name.
//            instIDHash.clear();
//            buildInstIDHash();
            
            logger.debug(inst.getInst_id() + " updated.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update " + inst.getInst_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Return the list of Institution in the database.
    public List<Institution> getInstList() {
        Connection conn = null;
        String query = "SELECT * from inst ORDER BY inst_name";
        List<Institution> instList = new ArrayList<>();

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while(rs.next()) {
                Institution inst = new Institution
                                    (rs.getString("inst_id"),
                                     rs.getString("inst_name"));
                instList.add(inst);    
            }
            stm.close();
            logger.debug("Full institution list built.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to build full institution list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return instList;
    }
    
    /*
    // Build the HashMap of Institution ID -> Institution Name.
    public static void buildInstIDHash() {
        if (instIDHash.isEmpty()) {
            Connection conn = null;
            String query = "SELECT * FROM inst";

            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
            
                while (rs.next()) {
                    instIDHash.put(rs.getString("inst_id"), 
                                   rs.getString("inst_name"));
                }
                stm.close();
                logger.debug("Institution ID HashMap built.");
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to build institution ID HashMap!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
    }
    // Return the name of the institution that has this ID.
    public static String getInstNameFromHash(String instID) {
        return instIDHash.get(instID);
    }
    */
    
    // Return the hashmap of the institution setup with the specific ID.
    public LinkedHashMap<String, String> getSingleInstNameHash(String instID) 
    {
        String query = "SELECT * FROM inst WHERE inst_id = \'" + instID + "\'";
        
        return getInstNameHash(query);
    }    
    // Return the hashmap of all the institution setup in the database.
    public LinkedHashMap<String, String> getAllInstNameHash() {
        String query = "SELECT * FROM inst ORDER BY inst_name";
        
        return getInstNameHash(query);
    }
    // Helper function to return the hashmap of the institution setup.
    private LinkedHashMap<String, String> getInstNameHash(String query) {
        Connection conn = null;
        LinkedHashMap<String, String> instNameHash = new LinkedHashMap<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                instNameHash.put(rs.getString("inst_name"), 
                                 rs.getString("inst_id"));
            }
            stm.close();
            logger.debug("Institution name hash built.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to build institution name hash!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return instNameHash;
    }
    
    // Return the ID of the institution where this unit ID belongs to.
    public static String getInstID(String unitID) {
        String query = "SELECT inst_id FROM inst_dept_grp WHERE inst_id = ? "
                     + "OR dept_id = ? OR grp_id = ?";
        
        return getInstProperty(query, unitID, "inst_id");        
    }
    // Return the name of the institution where this unit ID belongs to.
    public static String getInstName(String unitID) {
        String query = "SELECT inst_name FROM inst_dept_grp WHERE inst_id = ? "
                     + "OR dept_id = ? OR grp_id = ?";
        
        return getInstProperty(query, unitID, "inst_name");
    }
    // Helper function to retrieve the institution ID or name where this unit
    // ID belongs to.
    private static String getInstProperty(String query, String unitID, 
            String property) 
    {
        Connection conn = null;
        String instName = Constants.DATABASE_INVALID_STR;

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, unitID);
            stm.setString(2, unitID);
            stm.setString(3, unitID);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                instName = rs.getString(property);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            StringBuilder err = new StringBuilder("FAIL to retrieve ").
                    append(property).append(" for unit ").append(unitID);
//            logger.error("FAIL to retrieve " + property + " for unit " + unitID);
            logger.error(err);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return instName;
    }
}
