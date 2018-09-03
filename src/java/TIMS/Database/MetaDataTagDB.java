/*
 * Copyright @2018
 */
package TIMS.Database;

// Libraries for Java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MetaDataTagDB is use to perform SQL operations on the 
 * meta_data_tag table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 31-Aug-2018
 * 
 * Revision History
 * 31-Aug-2018 - First baseline with 3 methods insertMetaDataTag, 
 * getMetaDataTag and deleteMetaDataTag.
 */

public class MetaDataTagDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MetaDataTagDB.class.getName());
    private String study_id;
    // Machine generated constructor.
    public MetaDataTagDB(String study_id) {
        this.study_id = study_id;
    }
    
    // Insert the new meta data tag into database.
    public void insertMetaDataTag(String core_data, String column_id) {
        Connection conn = null;
        StringBuilder oper = new StringBuilder
            ("New meta data tag inserted into database: ").append(study_id).
                append(" - ").append(core_data).append(" - ").append(column_id);
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                ("INSERT INTO meta_data_tag(study_id,core_data,column_id) VALUES(?,?,?)");
            stm.setString(1, study_id);
            stm.setString(2, core_data);
            stm.setString(3, column_id);
            stm.executeUpdate();
            stm.close();
            
            logger.info(oper);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to insert meta data tag!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Return the meta data tag.
    public HashMap<String,String> getMetaDataTag() {
        HashMap<String, String> meta_data_tag = new HashMap<>();
        Connection conn = null;
        StringBuilder err = new StringBuilder
            ("FAIL to retrieve meta data tag from ").append(study_id);
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                ("SELECT core_data, column_id FROM meta_data_tag WHERE study_id = ?");
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                meta_data_tag.put(rs.getString("core_data"), rs.getString("column_id"));
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

        return meta_data_tag;
    }
    
    // Delete all the meta data tag.
    public void deleteMetaDataTag() {
        Connection conn = null;
        StringBuilder err = new StringBuilder
            ("FAIL to delete meta data tag belonging to ").append(study_id);
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                ("DELETE FROM meta_data_tag WHERE study_id = ?");
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
