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

public class DepartmentDB implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DepartmentDB.class.getName());

    // Return the full list of Department setup in the system.
    public List<Department> getDeptList() {
        Connection conn = null;
        String query = "SELECT * FROM dept ORDER BY inst_id, dept_id";
        List<Department> deptList = new ArrayList<>();

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                Department dept = new Department(
                                        rs.getString("inst_id"),
                                        rs.getString("dept_id"),
                                        rs.getString("dept_name"));
                deptList.add(dept);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to build department list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return deptList;
    }
    
    // Insert the new department ID into database.
    public boolean insertDepartment(Department dept) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO dept(dept_id,inst_id,dept_name) VALUES(?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dept.getDept_id());
            stm.setString(2, dept.getInst_id());
            stm.setString(3, dept.getDept_name());
            stm.executeUpdate();
            stm.close();
            logger.info("Department ID inserted into database: " + 
                    dept.getDept_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert new department " + dept.getDept_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return result;
    }
    
    // Update the department name in the database.
    public boolean updateDepartment(Department dept) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE dept SET dept_name = ? WHERE dept_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dept.getDept_name());
            stm.setString(2, dept.getDept_id());
            stm.executeUpdate();
            stm.close();
            logger.info(dept.getDept_id() + " updated.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update department " + dept.getDept_id());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Return the list of department ID setup under this institution.
    public List<String> getDeptIDList(String inst_id) {
        return new ArrayList<>(getDeptHashForInst(inst_id).values());
    }
    
    // Return the HashMap of department ID setup for this institution.
    public LinkedHashMap<String, String> getDeptHashForInst(String inst_id) {
        String query = "SELECT dept_id, dept_name FROM dept "
                     + "WHERE inst_id = \'" + inst_id + "\' ORDER BY dept_id";

        return getDeptHash(query);
    }
    // Return the HashMap of all the department IDs setup in the system.
    public LinkedHashMap<String, String> getAllDeptHash() {
        String query = "SELECT dept_id, dept_name FROM dept ORDER BY dept_id";
        
        return getDeptHash(query);
    }
    // Helper function to build the hashmap of the department IDs setup in the
    // system using the query passed in.
    private LinkedHashMap<String, String> getDeptHash(String query) {
        Connection conn = null;
        LinkedHashMap<String, String> deptHash = new LinkedHashMap<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                deptHash.put(rs.getString("dept_name"), 
                             rs.getString("dept_id"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to build department hashmap!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return deptHash;
    }
    
    /*
    // Retrieve the institution ID that this department belongs to.
    public String getDeptInstID(String dept_id) {
        return getDeptPropValue(dept_id, "inst_id");
    }
    */
    
    // Return the name for this department.
    public static String getDeptName(String dept_id) {
        return getDeptPropValue(dept_id, "dept_name");
    }
    // Helper function to retrieve one of the department's property value.
    private static String getDeptPropValue(String dept_id, String property) {
        Connection conn = null;
        String propValue = Constants.DATABASE_INVALID_STR;
        String query = "SELECT * FROM dept WHERE dept_id = ?";

        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, dept_id);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                // Retrieve the requested property value.
                propValue = rs.getString(property);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            StringBuilder err = new StringBuilder("FAIL to retrieve ").
                    append(property).append(" for department ").append(dept_id);
//            logger.error("FAIL to retrieve " + property + "for department " + dept_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return propValue;
    }
}
