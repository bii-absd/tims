/*
 * Copyright @2016-2018
 */
package TIMS.Database;

// Libraries for Java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for Trove
import gnu.trove.map.hash.THashMap;

/**
 * FeatureDB is an abstract class and not mean to be instantiate, its main 
 * job is to return the feature active status based on the feature code.
 * 
 * Author: Tay Wei Hong
 * Date: 18-Jul-2016
 * 
 * Revision History
 * 21-Jul-2016 - Created with 4 static methods, getFeatureActiveStatus(),
 * getAllFeatureStatusHash(), getAllFeatureStatus() and updateFeature().
 * 11-Jun-2018 - Changes due to update in feature table; replaced active
 * (BOOLEAN) with status (TEXT).
 */

public class FeatureDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FeatureDB.class.getName());

    /* NOT IN USE!
    // Return the feature status based on the fcode passed in.
    public String getFeatureStatus(String fcode) {
        String status = Constants.DATABASE_INVALID_STR;
        Connection conn = null;
        String query = "SELECT status FROM feature WHERE fcode = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, fcode);
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                status = rs.getString("status");
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve feature status!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    
        return status;
    }
    */
    
    // Return all the feature settings defined in the database as a hash map.
    public Map<String, String> getAllFeatureStatusHash() {
        Map<String, String> fteHash = new THashMap<>();
        Connection conn = null;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                                    ("SELECT * FROM feature ORDER BY fcode");
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                fteHash.put(rs.getString("fcode"), rs.getString("status"));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query feature database!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return fteHash;
    }
    
    // Return all the feature settings defined in the database.
    public List<Feature> getAllFeatureStatus() {
        Connection conn = null;
        List<Feature> fList = new ArrayList<>();
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                                    ("SELECT * FROM feature ORDER BY fcode");
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                fList.add(new Feature(rs));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve feature status list!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        return fList;
    }
    
    // Update the feature setup. Any exception encountered here will be throw
    // and to be handled by the caller.
    public void updateFeature(Feature fte) throws SQLException, NamingException 
    {
        String query = "UPDATE feature SET status = ? WHERE fcode = ?";
        Connection conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        
        stm.setString(1, fte.getStatus());
        stm.setString(2, fte.getFcode());
        // Execute the update statement.
        stm.executeUpdate();
        stm.close();
        
        DBHelper.closeDSConn(conn);
    }
}
