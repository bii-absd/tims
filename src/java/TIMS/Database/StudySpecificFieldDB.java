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

import TIMS.General.FileHelper;
// Libraries for Java
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StudySpecificFieldDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudySpecificFieldDB.class.getName());
    private String study_id;
    // Machine generated constructor.
    public StudySpecificFieldDB(String study_id) {
        this.study_id = study_id;
    }
    
    // Insert or update the study specific field into database.
    public void updateSSField(String category, byte[] fields) {
        Connection conn = null;
        PreparedStatement stm = null;
        StringBuilder oper = new StringBuilder(study_id).append(" - ").append(category);
        
        try {
            conn = DBHelper.getDSConn();
            // Check whether is this a existing category.
            stm = conn.prepareStatement
                ("SELECT 1 FROM study_specific_fields WHERE study_id = ? AND category = ?");
            stm.setString(1, study_id);
            stm.setString(2, category);
            ResultSet rs = stm.executeQuery();
            
            if (rs.isBeforeFirst()) {
                // Existing category; update.
                stm = conn.prepareStatement
                    ("UPDATE study_specific_fields SET fields = ? WHERE study_id = ? AND category = ?");
                stm.setBytes(1, fields);
                stm.setString(2, study_id);
                stm.setString(3, category);
                oper.append(": Updated.");
            }
            else {
                // New category; insert.
                stm = conn.prepareStatement
                    ("INSERT INTO study_specific_fields(study_id,category,fields) VALUES(?,?,?)");
                stm.setString(1, study_id);
                stm.setString(2, category);
                stm.setBytes(3, fields);
                oper.append(": Inserted.");
            }
            stm.executeUpdate();
            stm.close();
            
            logger.info(oper);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update study specific fields!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Return the list of specific field category (limit to 3).
    public List<String> getSpecificFieldCategory() {
        List<String> categories = new ArrayList<>();
        Connection conn = null;
        StringBuilder err = new StringBuilder
            ("FAIL to retrieve specific field catergory belonging to ").
                append(study_id);
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                ("SELECT category FROM study_specific_fields WHERE study_id = ? ORDER BY id LIMIT 3");
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error(err);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return categories;
    }
    
    // Return the hashmap of all the specific fields defined for this study.
    public LinkedHashMap<String, String> getSpecificFieldHashMap() {
        Connection conn = null;
        LinkedHashMap<String, String> field_hashmap = new LinkedHashMap<>();
        String query = "SELECT fields FROM study_specific_fields WHERE study_id = ? ORDER BY category";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                for (String field : FileHelper.convertByteArrayToList(rs.getBytes("fields"))) {
                    field_hashmap.put(field, field);
                }
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query study specific fields!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return field_hashmap;
    }
    
    // Return the list of specific fields under this category.
    public List<String> getSpecificFieldListFromCategory(String category) {
        List<String> field_list = new ArrayList<>();
        Connection conn = null;
        StringBuilder err = new StringBuilder
            ("FAIL to retrieve specific field list belonging to ").
                append(study_id).append(" - ").append(category);
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                ("SELECT fields FROM study_specific_fields WHERE study_id = ? AND category = ?");
            stm.setString(1, study_id);
            stm.setString(2, category);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                field_list = FileHelper.convertByteArrayToList
                                        (rs.getBytes("fields"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error(err);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return field_list;
    }

    // Delete all the specific fields.
    public void deleteSpecificFields() {
        Connection conn = null;
        StringBuilder err = new StringBuilder
            ("FAIL to delete study specific fields belonging to ").
                append(study_id);
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                ("DELETE FROM study_specific_fields WHERE study_id = ?");
            stm.setString(1, study_id);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error(err);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
}
