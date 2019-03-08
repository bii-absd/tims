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
import TIMS.General.FileHelper;
// Libraries for Java
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public abstract class StudyDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(StudyDB.class.getName());

    // Return the column name list used in this study.
    public static byte[] getColumnNameList(String studyID) {
        byte[] column_name_list = null;
        Connection conn = null;
        String query = "SELECT data_col_name_list FROM study WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                column_name_list = rs.getBytes("data_col_name_list");
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve data column name list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return column_name_list;
    }
    
    // Update study's column name list; call when the admin wish to delete the 
    // full subject meta data from the study, and during the first meta data 
    // upload.
    public static void updateStudyColumnNameList(String studyID, byte[] colNameList) {
        Connection conn = null;
        String query = "UPDATE study SET data_col_name_list = ? WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setBytes(1, colNameList);
            stm.setString(2, studyID);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update data column name list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }

    // Return the study object that has this study id.
    public static Study getStudyObject(String studyID) {
        Study study = null;
        Connection conn = null;
        String query = "SELECT * FROM study WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                study = new Study(rs);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve study " + studyID);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return study;
    }
    
    // Insert the new study into database. For every new study created, 
    // the finalized_output, detail_files and summary fields will be empty.
    public static boolean insertStudy(Study study) {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "INSERT INTO study(study_id,title,grp_id,annot_ver,"
                     + "icd_code,description,background,grant_info,start_date,"
                     + "end_date,finalized,closed) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study.getStudy_id());
            stm.setString(2, study.getTitle());
            stm.setString(3, study.getGrp_id());
            stm.setString(4, study.getAnnot_ver());
            stm.setString(5, study.getIcd_code());
            stm.setString(6, study.getDescription());
            stm.setString(7, study.getBackground());
            stm.setString(8, study.getGrant_info());
            stm.setDate(9, study.getStart_date());
            stm.setDate(10, study.getEnd_date());
            stm.setBoolean(11, study.getFinalized());
            stm.setBoolean(12, study.getClosed());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("New Study ID inserted into database: " + 
                    study.getStudy_id());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert study!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update study description, background or grant information (aka DBGI) in
    // database.
    public static boolean updateStudyDBGI(Study study) {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "UPDATE study SET description = ?, background = ?, "
                     + "grant_info = ? WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study.getDescription());
            stm.setString(2, study.getBackground());
            stm.setString(3, study.getGrant_info());
            stm.setString(4, study.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated study " + study.getStudy_id() + " DBGI.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update study " + study.getStudy_id() + " DBGI!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update study main info in database.
    public static boolean updateStudyMI(Study study) {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "UPDATE study SET title=?, grp_id = ?, icd_code = ?, "
                     + "start_date = ?, end_date = ? WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study.getTitle());
            stm.setString(2, study.getGrp_id());
            stm.setString(3, study.getIcd_code());
            stm.setDate(4, study.getStart_date());
            stm.setDate(5, study.getEnd_date());
            stm.setString(6, study.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated study " + study.getStudy_id() + " main info.");
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update study " + study.getStudy_id() + " main info!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Update study's visual time; the time it last export data for 
    // visualization.
    public static void updateStudyVisualTime(String studyID, Timestamp visual_time) {
        Connection conn = null;
        String query = "UPDATE study SET visual_time = ? WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setTimestamp(1, visual_time);
            stm.setString(2, studyID);
            stm.executeUpdate();
            stm.close();
            
            logger.debug(studyID + " visual time updated to " + visual_time.toString());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update study visual time!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Reset the study's cbio_url to default i.e. NULL
    public static void resetStudyCbioUrl(String studyID) {
        String query = "UPDATE study SET cbio_url = DEFAULT " 
                     + "WHERE study_id = \'" + studyID + "\'";
        
        logger.debug("Reset cbio_url for " + studyID);
        updateStudyField(query);
    }
    // Update study's cbio_url.
    public static void updateStudyCbioUrl(String studyID, String cbio_url) {
        String query = "UPDATE study SET cbio_url = \'" + cbio_url 
                     + "\' WHERE study_id = \'" + studyID + "\'";
        
        logger.debug("Updating cbio_url for " + studyID);
        updateStudyField(query);
    }
    // Update study's finalized status.
    public static void updateStudyFinalizedStatus(String studyID, Boolean status) {
        String query = "UPDATE study SET finalized = " + status 
                     + " WHERE study_id = \'" + studyID + "\'";
        
        logger.debug("Updating finalized status for " + studyID);
        updateStudyField(query);
    }
    // Update study's closed status.
    public static void updateStudyClosedStatus(String studyID, Boolean status) {
        String query = "UPDATE study SET closed = " + status 
                     + " WHERE study_id = \'" + studyID + "\'";
        
        logger.debug("Updating closed status for " + studyID);
        updateStudyField(query);
    }
    // Update the finalized_output field with the path of the output file.
    public static void updateStudyFinalizedFile(String studyID, String path) {
        String query = "UPDATE study SET finalized_output = \'" + path 
                     + "\' WHERE study_id = \'" + studyID + "\'";
        
        logger.debug("Updating finalized file path for " + studyID);
        updateStudyField(query);
    }
    // Update the summary field with the path of the summary report.
    public static void updateStudySummaryReport(String studyID, String path) {
        String query = "UPDATE study SET summary = \'" + path 
                     + "\' WHERE study_id = \'" + studyID + "\'";
        
        logger.debug("Updating summary report path for " + studyID);
        updateStudyField(query);
    }
    // Update the detail_files field with the path of the detail output file.
    public static void updateDetailOutputFiles(String studyID, String path) {
        String query = "UPDATE study SET detail_files = \'" + path 
                     + "\' WHERE study_id = \'" + studyID + "\'";
        
        logger.debug("Updating detail output path for " + studyID);
        updateStudyField(query);
    }
    // Update the meta_quality_report field with the path of quality report file.
    public static void updateMetaQualityReport(String studyID, String path) {
        String query = "UPDATE study SET meta_quality_report = \'" + path 
                     + "\' WHERE study_id = \'" + studyID + "\'";
        
        logger.debug("Updating meta quality report path for " + studyID);
        updateStudyField(query);        
    }
    // Null the path of quality report file.
    public static void nullMetaQualityReport(String studyID) {
        String query = "UPDATE study SET meta_quality_report = null "
                     + "WHERE study_id = \'" + studyID + "\'";
        
        logger.debug("Null the meta quality report path for " + studyID);
        updateStudyField(query);                
    }
    
    // Helper function to update study's field using the query passed in.
    private static void updateStudyField(String query) {
        Connection conn = null;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.executeUpdate();
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to update study!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Return the list of unclosed Study ID under the group(s) that this lead is
    // heading. The list of Study ID will be available for the lead to select
    // for pipeline execution.
    public static LinkedHashMap<String, String> getPIStudyHash(String piID) {
        String query = "SELECT study_id FROM study WHERE closed = false AND "
                     + "grp_id IN (SELECT grp_id FROM grp WHERE pi = \'"
                     + piID + "\') ORDER BY study_id";
        
        logger.debug("Retrieving study list for PI " + piID);
        return getStudyHash(query);
    }
    
    // Return the list of unclosed Study ID setup under the group that this 
    // user ID belongs to. The list of Study ID will be available for user to
    // select for pipeline execution.
    public static LinkedHashMap<String, String> getUserStudyHash(String userID) {
        String query = "SELECT study_id FROM study WHERE closed = false AND "
                     + "grp_id = (SELECT unit_id FROM user_account WHERE user_id = \'"
                     + userID + "\') ORDER BY study_id";

        logger.debug("Retrieving study list for user " + userID);
        return getStudyHash(query);
    }
    // Return all the unclosed Study ID setup in the system. This list of Study
    // ID will only be available to administrator to perform raw data uploading 
    // for the users.
    public static LinkedHashMap<String, String> getAllStudyHash() {
        String query = "SELECT study_id FROM study WHERE closed = false ORDER BY study_id";
        
        logger.debug("Retrieving full study list.");
        return getStudyHash(query);
    }
    // Only PI that lead a group can perform finalization.
    // Return the list of unfinalized Study ID that has completed job(s), and 
    // belongs to the group this PI is leading. This list of Study ID will be 
    // available for PI to select for finalization.
    public static LinkedHashMap<String, String> getFinalizableStudyHash(String piID) {
        String query = "SELECT DISTINCT study_id FROM study st "
                     + "NATURAL JOIN submitted_job sj WHERE sj.status_id = 3 "
                     + "AND st.grp_id IN (SELECT grp_id FROM grp WHERE pi = \'" 
                     + piID + "\') AND st.finalized = false "
                     + "AND st.closed = false ORDER BY study_id";

        logger.debug("Retrieving study list for finalization.");
        return getStudyHash(query);
    }
    // Return the list of unclosed Study ID that has completed job(s), and 
    // belongs to the groups that this user is heading (i.e. for 
    // Director|HOD|PI) or coming from (i.e. for Admin|User). Only allow the 
    // study to be export for visualization every one hour.
    public static LinkedHashMap<String, String> getVisualizableStudyHash(String grpQuery) {
        String query = "SELECT DISTINCT study_id FROM study st "
                     + "NATURAL JOIN submitted_job sj WHERE sj.status_id IN (3,5) "
                     + "AND st.grp_id IN (" + grpQuery 
                     + ") AND st.closed = false "
                     + "AND (st.visual_time < current_timestamp - interval \'1 hours\' "
                     + "OR st.visual_time IS NULL) ORDER BY study_id";

        logger.debug("Retrieving study list for visualization.");
        return getStudyHash(query);
    }

    // Return the list of Study ID (with Meta data uploaded) that belongs to 
    // this group.
    public static List<String> getDashboardStudyUnderGroup(String grp_id) {
        String query = "SELECT DISTINCT study_id FROM study st "
                     + "NATURAL JOIN subject sb WHERE st.grp_id = \'"
                     + grp_id + "\' AND st.closed = false ORDER BY study_id";
        
        logger.debug("Retrieving study IDs list for group ID " + grp_id);
        return new ArrayList<>(getStudyHash(query).values());
    }
    
    // Helper function to build the study list hash map based on the input query.
    private static LinkedHashMap<String, String> getStudyHash(String query) {
        Connection conn = null;
        LinkedHashMap<String, String> studyHash = new LinkedHashMap<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                studyHash.put(rs.getString("study_id"), rs.getString("study_id"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query study hash!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return studyHash;
    }
    
    // Return the list of studies (all status) that this user is allowed to view.
    public static List<Study> queryStudies(String groupQuery) {
        Connection conn = null;
        List<Study> deptStudies = new ArrayList<>();
        String query = "SELECT * FROM study WHERE grp_id IN (" + groupQuery 
                     + ") ORDER BY study_id";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();

            while (rs.next()) {
                deptStudies.add(new Study(rs));
            }
            stm.close();
            logger.debug("Studies review list retrieved.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve studies for " + groupQuery);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return deptStudies;
    }
    
    // Return the full list of finalized studies in the system. The list will
    // shown in the Unfinalize Study view (only accessible by Admin). Once the
    // study is closed, no unfinalize is allowed.
    public static List<Study> queryAllFinalizedStudies() {
        String query = "SELECT * FROM study WHERE finalized = true "
                     + "AND closed = false ORDER BY study_id";
        
        return queryFinalizedStudies(query);
    }
    // Return the list of finalized studies that belong to the group. The list
    // will be shown in the Completed Study Output view.
    public static List<Study> queryFinalizedStudiesByGrp(String grp_id) {
        String query = "SELECT * FROM study WHERE grp_id = \'" + grp_id 
                     + "\' AND finalized = true ORDER BY study_id";

        return queryFinalizedStudies(query);
    }
    // Return the list of finalized studies that belong to the group(s) that
    // this PI (i.e. Director|HOD|PI) is in charge of.
    public static List<Study> queryFinalizedStudiesByGrps(String pi_id) {
        String query = "SELECT * FROM study WHERE finalized = true AND "
                     + "grp_id IN (SELECT grp_id FROM grp WHERE pi = \'"
                     + pi_id + "\') ORDER BY study_id";
        
        return queryFinalizedStudies(query);
    }
    
    // Helper function to retrieve the list of finalized studies from the 
    // database using the query passed in.
    private static List<Study> queryFinalizedStudies(String query) {
        Connection conn = null;
        List<Study> finalizedStudies = new ArrayList<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                finalizedStudies.add(new Study(rs));
            }
            stm.close();
            logger.debug("Finalized studies retrieved.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve finalized studies!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return finalizedStudies;
    }
    
    // Return the list of Study ID setup in the system.
    // Note: Users will not have access to create Study ID i.e. only the 
    // administrator do.
    public static List<Study> queryStudy() {
        Connection conn = null;
        List<Study> studyList = new ArrayList<>();
        String query = "SELECT * FROM study ORDER BY study_id";

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
        
            while (rs.next()) {
                studyList.add(new Study(rs));
            }
            stm.close();
            logger.debug("Query study completed.");
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query study!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return studyList;
    }
    
    // Zip the consolidated output file of this study, and update the 
    // finalized_output to the zipped output filepath. The original consolidated
    // output filepath will be returned.
    public static String zipFinalizedOutput(String study_id) {
        String result = null;
        String foPath = getStudyObject(study_id).getFinalized_output();
        String[] srcFile = {foPath};
        // Remove the .txt extension from the filename, and replace it with .zip
        String zipPath = foPath.substring
                         (0, foPath.indexOf(Constants.getOUTPUTFILE_EXT()));
        zipPath += Constants.getZIPFILE_EXT();
        
        try {
            FileHelper.zipFiles(zipPath, srcFile);
            // Return the original finalized output filepath.
            result = foPath;
            logger.debug("Finalized output for Study ID " + study_id + " zipped.");
            // Added the below statement due to a bug in Java that prevent the
            // original output file for being deleted.
            System.gc();
            // Update the finalized output filepath to the zipped version.
            updateStudyFinalizedFile(study_id, zipPath);
        }
        catch (IOException e) {
            logger.error("FAIL to zip finalized output file!");
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Retrieve the annotation version used in this study.
    public static String getStudyAnnotVer(String study_id) {
        return getStudyPropValue(study_id, "annot_ver");
    }
    // Retrieve the group ID for this study.
    public static String getStudyGrpID(String study_id) {
        return getStudyPropValue(study_id, "grp_id");
    }
    // Retrieve the ICD code for this study.
    public static String getICDCode(String study_id) {
        return getStudyPropValue(study_id, "icd_code");
    }
    // Retrieve the cbio_url for this study.
    public static String getCbioURL(String study_id) {
        return getStudyPropValue(study_id, "cbio_url");
    }
    // Retrieve the meta quality report path for this study.
    public static String getMetaQualityReportPath(String study_id) {
        return getStudyPropValue(study_id, "meta_quality_report");
    }
    
    // Helper function to retrieve one of the study's property value.
    private static String getStudyPropValue(String study_id, String property) {
        Connection conn = null;
        String propValue = Constants.DATABASE_INVALID_STR;
        String query = "SELECT * FROM study WHERE study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                // Retrieve the requested property value.
                propValue = rs.getString(property);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve " + property + " for study " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return propValue;
    }
    
    // Return the list of annotation version setup in the system
    public static LinkedHashMap<String, String> getAnnotHash() {
        Connection conn = null;
        LinkedHashMap<String, String> annotHash = new LinkedHashMap<>();
        String query = "SELECT annot_ver FROM annotation";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
        
            while (rs.next()) {
                annotHash.put(rs.getString("annot_ver"), rs.getString("annot_ver"));
            }
            stm.close();
            logger.debug("Annotation Version: " + annotHash.toString());
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve annotation version!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return annotHash;
    }
}
