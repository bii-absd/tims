/*
 * Copyright @2016-2017
 */
package TIMS.Database;

import TIMS.General.Constants;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * SubjectRecordDB is an abstract class and not mean to be instantiate, its main 
 job is to perform SQL operations on the subject_record table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 31-Mar-2016
 * 
 * Revision History
 * 31-Mar-2016 - First baseline with 4 static methods, insertSS, getSSList, 
 * updateSS and isSSExist.
 * 13-Apr-2016 - Added new method updatePartialSS(), to exclude event and
 * event date during updating of subject_record table.
 * 17-Apr-2017 - Subject's meta data will now be own by study, and the study
 * will be own by group i.e. the direct link between group and subject's meta
 * data will be break off. Rename to SubjectRecordDB.
 * 28-Apr-2017 - Changes due to removal of age_at_diagnosis from subject_record 
 * database table.
 */

public abstract class SubjectRecordDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SubjectRecordDB.class.getName());

    // Insert the subject record for this study into database.
    public static boolean insertSR(SubjectRecord sr) {
        boolean result = Constants.OK;
        String detail = sr.getStudy_id() + " - " + sr.getSubject_id() 
                      + " - " + sr.getRecord_date();
        Connection conn = null;
        String query = "INSERT INTO subject_record(subject_id,study_id,"
                     + "record_date,height,weight,remarks,event,event_date) "
                     + "VALUES (?,?,?,?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, sr.getSubject_id());
            stm.setString(2, sr.getStudy_id());
            stm.setObject(3, sr.getRecord_date(), Types.DATE);
            stm.setFloat(4, sr.getHeight());
            stm.setFloat(5, sr.getWeight());
            stm.setString(6, sr.getRemarks());
            stm.setString(7, sr.getEvent());
            stm.setObject(8, sr.getEvent_date(), Types.DATE);
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Insert subject record: " + detail);
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert subject record: " + detail);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return result;
    }
    
    // Return the list of subject records belonging to this study.
    public static List<SubjectRecord> getSSList(String study_id) {
        Connection conn = null;
        List<SubjectRecord> srList = new ArrayList<>();
        String query = "SELECT * FROM subject_record WHERE study_id = ? "
                     + "ORDER BY subject_id, record_date";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                SubjectRecord sr = new SubjectRecord(
                                    rs.getString("subject_id"),
                                    rs.getString("study_id"),
                                    rs.getObject("record_date", LocalDate.class),
                                    rs.getString("remarks"),
                                    rs.getString("event"),
                                    rs.getFloat("height"),
                                    rs.getFloat("weight"),
                                    rs.getObject("event_date", LocalDate.class));
                
                srList.add(sr);
            }
            logger.debug("Subject records retrieved from study: " + study_id);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve subject records from study: " + study_id);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return srList;
    }
    
    // Update subject record from this study in database. To be called 
    // during Meta data upload by batch.
    // Only allow changes to height, weight and remarks.
    public static boolean updatePartialSR(SubjectRecord sr) {
        boolean result = Constants.OK;
        String detail = sr.getStudy_id() + " - " + sr.getSubject_id() 
                      + " - " + sr.getRecord_date();
        Connection conn = null;
        String query = "UPDATE subject_record SET height = ?, weight = ?, "
                     + "remarks = ? WHERE subject_id = ? AND record_date = ? "
                     + "AND study_id = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setFloat(1, sr.getHeight());
            stm.setFloat(2, sr.getWeight());
            stm.setString(3, sr.getRemarks());
            stm.setString(4, sr.getSubject_id());
            stm.setObject(5, sr.getRecord_date(), Types.DATE);
            stm.setString(6, sr.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Update subject record: " + detail);
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update subject record: " + detail);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;        
    }
    
    // Update subject record from this study in database.
    // Only allow changes to height, weight, remarks, record _date, event and 
    // event_date.
    public static boolean updateSR(SubjectRecord sr, LocalDate new_record_date) {
        boolean result = Constants.OK;
        Date rec_date = null;
        Date new_rec_date = null;
        Date eve_date = null;
        String detail = sr.getStudy_id() + " - " + sr.getSubject_id() 
                      + " - " + new_record_date;
        Connection conn = null;
        String query = "UPDATE subject_record SET height = ?, weight = ?, "
                     + "remarks = ?, event = ?, event_date = ?, record_date = ? "
                     + "WHERE subject_id = ? AND record_date = ? AND study_id = ?";
        
        rec_date = java.sql.Date.valueOf(sr.getRecord_date());
        new_rec_date = java.sql.Date.valueOf(new_record_date);
        if (sr.getEvent_date() != null) {
            eve_date = java.sql.Date.valueOf(sr.getEvent_date());
        }
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setFloat(1, sr.getHeight());
            stm.setFloat(2, sr.getWeight());
            stm.setString(3, sr.getRemarks());
            stm.setString(4, sr.getEvent());
            stm.setDate(5, eve_date);
            stm.setDate(6, new_rec_date);
            stm.setString(7, sr.getSubject_id());
            stm.setDate(8, rec_date);
            stm.setString(9, sr.getStudy_id());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Update subject record: " + detail);
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update subject record: " + detail);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Check whether the subject record exists in the database. Exception 
    // thrown here need to be handle by the caller.
    public static boolean isSRExist(String subject_id, String study_id) 
            throws SQLException, NamingException
    {
        Connection conn = null;
        String query = "SELECT * FROM subject_record WHERE subject_id = ? AND "
                     + "study_id = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, subject_id);
        stm.setString(2, study_id);
        ResultSet rs = stm.executeQuery();
        boolean srExist = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
        
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return srExist;
    }
    
    // Check whether the specific subject record (with record date) exists in 
    // the database. Exception thrown here need to be handle by the caller.
    public static boolean isSRExist(String subject_id, String study_id, LocalDate rec_date) 
            throws SQLException, NamingException
    {
        Connection conn = null;
        String query = "SELECT * FROM subject_record WHERE subject_id = ? AND "
                     + "study_id = ? AND record_date = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        stm.setString(1, subject_id);
        stm.setString(2, study_id);
        stm.setObject(3, rec_date, Types.DATE);
        ResultSet rs = stm.executeQuery();
        boolean srExist = rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
        
        stm.close();
        DBHelper.closeDSConn(conn);
        
        return srExist;
    }
}
