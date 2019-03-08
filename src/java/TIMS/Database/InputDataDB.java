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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class InputDataDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(InputDataDB.class.getName());
    
    // Insert the new input data detail into database.
    public boolean insertInputData(InputData idata) {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "INSERT INTO input_data(study_id,sn,create_uid,"
                     + "pipeline_name,filename,filepath,description,create_time) "
                     + "VALUES(?,?,?,?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, idata.getStudy_id());
            stm.setInt(2, idata.getSn());
            stm.setString(3, idata.getCreate_uid());
            stm.setString(4, idata.getPipeline_name());
            stm.setString(5, idata.getFilename());
            stm.setString(6, idata.getFilepath());
            stm.setString(7, idata.getDescription());
            stm.setTimestamp(8, idata.getCreate_time());
            stm.executeUpdate();
            stm.close();
            StringBuilder oper = new 
                StringBuilder("New input data detail inserted into database: ").
                    append(idata.getStudy_id()).append(" - SN: ").
                    append(idata.getSn());
            logger.info(oper);
//            logger.debug("New input data detail inserted into database: " +
//                        idata.getStudy_id() + " - SN: " + idata.getSn());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert input data!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Return the list of input data that belong to this study ID and pipeline.
    public List<InputData> getIpList(String studyID, String plName) {
        Connection conn = null;
        List<InputData> ipList = new ArrayList<>();
        String query = "SELECT * FROM input_data WHERE study_id = ? AND "
                     + "pipeline_name = ? ORDER BY sn DESC";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, studyID);
            stm.setString(2, plName);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                ipList.add(new InputData(rs));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.debug("FAIL to retrieve input data list!");
            logger.debug(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return ipList;
    }
    
    // Update description field in the input_data table after making changes in
    // the Raw Data Management page.
    public boolean updateDescAfterEdit(String study_id, int sn, 
            String desc, String update_uid, Timestamp update_time) 
    {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "UPDATE input_data SET update_uid = ?, update_time = ?, "
                     + "description = ? WHERE study_id = ? AND sn = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, update_uid);
            stm.setTimestamp(2, update_time);
            stm.setString(3, desc);
            stm.setString(4, study_id);
            stm.setInt(5, sn);
            stm.executeUpdate();
            stm.close();
            StringBuilder oper = new 
                StringBuilder("Updated input data for study ").
                    append(study_id).append(" serial no ").append(sn);
            logger.info(oper);
//            logger.debug("Updated input data for study " + study_id + " serial no " + sn);
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            StringBuilder err = new 
                StringBuilder("FAIL to update input data for study ").
                    append(study_id).append(" serial no ").append(sn);
            logger.error(err);
//            logger.error("FAIL to update input data for study " + study_id + " serial no " + sn);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return result;
    }
    
    // Update the description and filename fields in the input_data table after 
    // making changes in the Raw Data Management page.
    public boolean updateDescFilenameAfterEdit(String study_id, int sn, 
            String desc, String update_uid, Timestamp update_time, String filename) 
    {
        Connection conn = null;
        boolean result = Constants.OK;
        String query = "UPDATE input_data SET update_uid = ?, update_time = ?, "
                     + "description = ?, filename = ? WHERE study_id = ? AND sn = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, update_uid);
            stm.setTimestamp(2, update_time);
            stm.setString(3, desc);
            stm.setString(4, filename);
            stm.setString(5, study_id);
            stm.setInt(6, sn);
            stm.executeUpdate();
            stm.close();
            StringBuilder oper = new 
                StringBuilder("Updated input data for study ").
                    append(study_id).append(" serial no ").append(sn);
            logger.info(oper);
//            logger.debug("Updated input data for study " + study_id + " serial no " + sn);
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            StringBuilder err = new 
                StringBuilder("FAIL to update input data for study ").
                    append(study_id).append(" serial no ").append(sn);
            logger.error(err);
//            logger.error("FAIL to update input data for study " + study_id + " serial no " + sn);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return result;
    }
    
    // Return the next sn for the input data detail for this study.
    public int getNextSn(String studyID) throws SQLException, NamingException 
    {
        Connection conn = null;
        int nextSn = Constants.DATABASE_INVALID_ID;
        String query = "SELECT MAX(sn) FROM input_data WHERE study_id = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, studyID);
        ResultSet rs = stm.executeQuery();
        
        if (rs.next()) {
            // To get the next sn, add 1 to the largest sn for this study_id.
            nextSn = rs.getInt(1) + 1;
        }

        stm.close();
        DBHelper.closeDSConn(conn);
        
        return nextSn;
    }
    
    /* NOT IN USE!
    // Return the description for this input data.
    public static String getInputDescription(String study_id, int sn) {
        Connection conn = null;
        String inputDesc = Constants.DATABASE_INVALID_STR;
        String query = "SELECT description FROM input_data WHERE study_id = ? "
                     + "AND sn = " + sn;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                inputDesc = rs.getString("description");
            }
        }
        catch (SQLException|NamingException e) {
            logger.debug("FAIL to query input data description!");
            logger.debug(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return inputDesc;
    }
    */
}
