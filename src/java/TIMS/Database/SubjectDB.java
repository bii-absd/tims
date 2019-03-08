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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for Trove
import gnu.trove.map.hash.TObjectIntHashMap;

public class SubjectDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubjectDB.class.getName());
    private final String study_id;

    public SubjectDB(String study_id) {
        this.study_id = study_id;
    }
    
    // Delete all subjects.
    public void deleteAllSubjectsFromStudy() {
        Connection conn = null;
        String query = "DELETE FROM subject WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to delete subjects belonging to " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // This express get method is used during consistency check in meta data 
    // upload.
    public Subject getSubject(PreparedStatement stm, String subject_id) 
            throws SQLException 
    {
        Subject subjt = null;
        stm.setString(1, study_id);
        stm.setString(2, subject_id);
        ResultSet rs = stm.executeQuery();
        if (rs.next()) {
            subjt = new Subject(rs);
        }
        return subjt;
    }
    
    // Return the subject object belonging to this subject id.
    public Subject getSubject(String subject_id) {
        Connection conn = null;
        Subject subjt = null;
        String query = "SELECT * FROM subject WHERE study_id = ? AND subject_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            stm.setString(2, subject_id);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                subjt = new Subject(rs);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve subject for " 
                        + study_id + "-" + subject_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return subjt;
    }
    
    // Insert the new subject meta data into database
    public boolean insertSubject(Subject subject, Connection conn) {
        boolean result = Constants.OK;
        String query = "INSERT INTO subject(subject_id,study_id,race,"
                + "gender,dob,casecontrol,age_at_baseline) VALUES(?,?,?,?,?,?,?)";
        
        try {
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject.getSubject_id());
            stm.setString(2, subject.getStudy_id());
            stm.setString(3, subject.getRace());
            stm.setString(4, subject.getGender());
            stm.setObject(5, subject.getDob(), Types.DATE);
            stm.setString(6, subject.getCasecontrol());
            stm.setString(7, subject.getAge_at_baseline());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Subject ID: " + subject.getSubject_id() + 
                         " created under study " + subject.getStudy_id());
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert subject " + subject.getSubject_id());
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // For transaction that include update to many rows in the subject table, 
    // the transaction will only be committed if all the updates are successful.
    // For such cases, the caller will be passing in the connection (because
    // they will be controlling the time to commit at their ends.
    public boolean updateSubt(Subject subt, Connection conn) {
        return updateSubject(subt, conn);
    }
    public boolean updateSubt(Subject subt) {
        Connection conn = null;
        boolean result = Constants.OK;
        
        try {
            conn = DBHelper.getDSConn();
            result = updateSubject(subt, conn);
        } catch (SQLException|NamingException ex) {
            result = Constants.NOT_OK;
            logger.error(ex.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update subject meta data in database.
    // Only allow changes to gender, race, dob, casecontrol and age_at_baseline.
    private boolean updateSubject(Subject subject, Connection conn) {
        boolean result = Constants.OK;
        String query = "UPDATE subject SET gender = ?, casecontrol = ?, "
                     + "race = ?, dob = ?, age_at_baseline = ? WHERE subject_id = ? "
                     + "and study_id = ?";
        
        try {
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject.getGender());
            stm.setString(2, subject.getCasecontrol());
            stm.setString(3, subject.getRace());
            stm.setObject(4, subject.getDob(), Types.DATE);
            stm.setString(5, subject.getAge_at_baseline());
            stm.setString(6, subject.getSubject_id());
            stm.setString(7, subject.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated meta data for subject " + 
                         subject.getSubject_id() + " under study " +
                         subject.getStudy_id());
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update meta data for subject " + 
                         subject.getSubject_id() + " under study " + 
                         subject.getStudy_id());
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    /* NO LONGER IN USE!
    // Return the hashmap of subject ID. Exception thrown here need to be 
    // handle by the caller.
    public LinkedHashMap<String, String> getSubjectIDHashMap() 
        throws SQLException, NamingException 
    {
        Connection conn = DBHelper.getDSConn();
        LinkedHashMap<String, String> subtIDHash = new LinkedHashMap<>();
        String query = "SELECT * from subject WHERE study_id = ? ORDER BY subject_id";
        PreparedStatement stm = conn.prepareStatement(query);
        
        stm.setString(1, study_id);
        ResultSet rs = stm.executeQuery();

        while (rs.next()) {
            subtIDHash.put(rs.getString("subject_id"), rs.getString("subject_id"));
        }
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return subtIDHash;
    }
    */
    
    // Return the list of subject ID.
    public List<String> getSubjectIDsList() {
        Connection conn = null;
        List<String> subtIDsList = new ArrayList<>();
        String query = "SELECT * from subject WHERE study_id = ? ORDER BY subject_id";

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                subtIDsList.add(rs.getString("subject_id"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to get subject IDs list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return subtIDsList;
    }
    
    // Return the list of subject details.
    public List<SubjectDetail> getSubtDetailList() {
        Connection conn = null;
        List<SubjectDetail> subtDetailList = new ArrayList<>();
        String query = "SELECT * FROM subject_detail WHERE study_id = ? "
                     + "ORDER BY subject_id, record_date";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                subtDetailList.add(new SubjectDetail(rs));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve subject detail for " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return subtDetailList;
    }
    
    /* NO LONGER IN USE!
    // Return the list of subject.
    public List<Subject> getSubjectList() {
        Connection conn = null;
        List<Subject> subjectList = new ArrayList<>();
        String query = "SELECT * from subject WHERE study_id = ? ";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                subjectList.add(new Subject(rs));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve list of subjects!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return subjectList;
    }
    */
    
    // Retrieve the list of column X values where column Y is having y_value.
    public List<String> getColXBasedOnColYValue(String col_x, 
            String col_y, String y_value) {
        Connection conn = null;
        List<String> list_colx = new ArrayList<>();
        String query = "SELECT " + col_x + " FROM subject WHERE study_id = ?"
                     + " AND " + col_y + " = ?";
        
        try {
            conn =DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            stm.setString(2, y_value);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                list_colx.add(rs.getString(col_x));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve the list of " + col_x + 
                         " where " + col_y + " is " + y_value);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return list_colx;
    }
    
    /* NO LONGER IN USE!
    // Check whether the subject exists.
    public boolean isSubjectExistInStudy(String subject_id)
    {
        Connection conn = null;
        boolean isSubjectExist = Constants.NOT_OK;
        String query = "SELECT * FROM subject WHERE subject_id = ? AND "
                     + "study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, subject_id);
            stm.setString(2, study_id);
            ResultSet rs = stm.executeQuery();
            isSubjectExist = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
        
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to check for subject existence!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return isSubjectExist;
    }
    */
    
    // Return the count of each distinct value in this column.
    public TObjectIntHashMap<String> getDistinctValueCountInColumn(String column_name) {
        Connection conn = null;
        TObjectIntHashMap<String> distinct_count_hashmap = new TObjectIntHashMap<>();
        String query = "SELECT " + column_name 
                     + ", COUNT(1) as tally FROM subject WHERE study_id = ? "
                     + "GROUP BY " + column_name;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                distinct_count_hashmap.put(rs.getString(column_name), rs.getInt("tally"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to get distinct value count in column " + column_name);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return distinct_count_hashmap;
    }
    
    // Return the list of distinct(s) value found in this column.
    public List<String> getDistinctValueInColumn(String column_name) {
        Connection conn = null;
        List<String> distinct_value = new ArrayList<>();
        String query = "SELECT DISTINCT " + column_name 
                     + " FROM subject WHERE study_id = ? ORDER BY " 
                     + column_name;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                distinct_value.add(rs.getString(column_name));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to get list of distinct " + column_name);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return distinct_value;
    }
    
    // Return the list of age_at_baseline (converted to float).
    public List<Float> getAgeAtBaselineList() {
        List<Float> age_at_baseline_list = new ArrayList<>();
        Connection conn = null;
        String query = "SELECT age_at_baseline FROM subject WHERE study_id = ? "
                     + "ORDER BY length(age_at_baseline), age_at_baseline";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                if (rs.getString("age_at_baseline").length() > 0) {
                    try {
                        age_at_baseline_list.add(Float.valueOf
                            (rs.getString("age_at_baseline")));
                    }
                    catch (NumberFormatException nfe) {
                        // Not a float; do nothing.
                    }
                }
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to get age_at_baseline list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return age_at_baseline_list;
    }
}
