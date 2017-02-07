/*
 * Copyright @2015-2017
 */
package TIMS.Database;

import TIMS.General.Constants;
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

/**
 * PipelineDB is an abstract class and not mean to be instantiate, its main 
 * job is to perform SQL operations on the pipeline table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 5-Nov-2015
 * 
 * Revision History
 * 05-Nov-2015 - First baseline with two static methods (getCommand and 
 * getAllCommand) created.
 * 06-Nov-2015 - Added one new method updateCommand to update the pipeline
 * command in database.
 * 16-Nov-2015 - Added one new method insertPipelineCommand to insert a new
 * pipeline command into database. Updated the name for all methods i.e. from
 * Command to Pipeline.
 * 24-Nov-2015 - Changed variable name from command_id to name. Added one
 * variable tid (Technology ID).
 * 25-Nov-2015 - Added one new method getPipelineTechnology.
 * 01-Dec-2015 - Implementation for database 2.0
 * 10-Dec-2015 - Changed to abstract class.
 * 13-Jan-2016 - Removed all the static variables in Pipeline Management module.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 04-Jul-2016 - Added 4 constant strings to store the name of the pipelines.
 * 25-Aug-2016 - Passed the ResultSet directly to the Pipeline constructor
 * during creation of Pipeline object. Implementation for database 3.6 Part I.
 * Added 3 new methods getEditablePlHash, getPipelineDescription & 
 * getPlAttribute.
 * 06-Feb-2017 - Added 2 new constants strings for RNA and DNA Sequencing 
 * pipelines.
 */

public abstract class PipelineDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(PipelineDB.class.getName());
    // Define a common name for each pipeline here.
    public static final String METHYLATION = "meth-pipeline";
    public static final String GEX_AFFYMETRIX = "gex-affymetrix";
    public static final String GEX_ILLUMINA = "gex-illumina";
    public static final String CNV = "cnv-pipeline";
    public static final String SEQ_RNA = "seq-rna";
    public static final String SEQ_DNA = "seq-dna";
    
    // Return the editable pipeline hash map (pipeline name -> description).
    public static LinkedHashMap<String, String> getEditablePlHash() {
        Connection conn = null;
        LinkedHashMap<String, String> plHash = new LinkedHashMap<>();
        String query = "SELECT name, description FROM pipeline "
                     + "WHERE editable = true ORDER BY description";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                plHash.put(rs.getString("description"), rs.getString("name"));
            }
            
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to query editable pipeline hash!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return plHash;
    }
    
    // Return the pipeline technology for this pipeline.
    public static String getPipelineTechnology(String pipeline_name) {
        return getPlAttribute(pipeline_name, "tid");
    }
    // Return the pipeline description for this pipeline.
    public static String getPipelineDescription(String pipeline_name) {
        return getPlAttribute(pipeline_name, "description");
    }
    
    // Helper function to retrieve one of the pipeline's attribute.
    private static String getPlAttribute(String plName, String attr) {
        Connection conn = null;
        String query = "SELECT * FROM pipeline WHERE name = ?";
        String attribute = null;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, plName);
            ResultSet result = stm.executeQuery();
            
            if (result.next()) {
                attribute = result.getString(attr);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve " + attr + " for pipeline " + plName);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return attribute;
    }
    
    // Return the pipeline object for this pipeline.
    public static Pipeline getPipeline(String pipeline_name) 
            throws SQLException, NamingException {
        Connection conn = null;
        Pipeline command = null;
        String query = "SELECT * FROM pipeline WHERE name = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        
        stm.setString(1, pipeline_name);
        ResultSet rs = stm.executeQuery();
        
        if (rs.next()) {
            command = new Pipeline(rs);
        }

        stm.close();
        DBHelper.closeDSConn(conn);
        
        return command;
    }
    
    // Return all the pipeline currently setup in the database.
    public static List<Pipeline> getAllPipeline() 
            throws SQLException, NamingException 
    {
        Connection conn = null;
        int index = 0;
        List<Pipeline> plList = new ArrayList<>();
        String query = "SELECT * FROM pipeline ORDER BY name";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        ResultSet rs = stm.executeQuery();
            
        while (rs.next()) {
            plList.add(index++, new Pipeline(rs));
        }

        stm.close();
        DBHelper.closeDSConn(conn);
        
        return plList;
    }
    
    // Update the pipeline command in the database.
    public static Boolean updatePipeline(Pipeline cmd) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "UPDATE pipeline SET description = ?, command = ?, "
                     + "parameter = ?, tid = ?, editable = ? WHERE name = ?";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, cmd.getDescription());
            stm.setString(2, cmd.getCommand());
            stm.setString(3, cmd.getParameter());
            stm.setString(4, cmd.getTid());
            stm.setBoolean(5, cmd.isEditable());
            stm.setString(6, cmd.getName());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("Updated pipeline: " + cmd.getName());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to update pipeline: " + cmd.getName());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Insert the new pipeline into database.
    public static Boolean insertPipeline(Pipeline cmd) {
        Connection conn = null;
        Boolean result = Constants.OK;
        String query = "INSERT INTO pipeline"
                     + "(name,description,tid,command,parameter,editable) "
                     + "VALUES(?,?,?,?,?,?)";
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, cmd.getName());
            stm.setString(2, cmd.getDescription());
            stm.setString(3, cmd.getTid());
            stm.setString(4, cmd.getCommand());
            stm.setString(5, cmd.getParameter());
            stm.setBoolean(6, cmd.isEditable());
            stm.executeUpdate();
            stm.close();
            
            logger.debug("New pipeline inserted into database: " + 
                    cmd.getName());
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert pipeline!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
}
