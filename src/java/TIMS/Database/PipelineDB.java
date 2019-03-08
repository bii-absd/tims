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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class PipelineDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(PipelineDB.class.getName());
    private String plName;
    // Define a common name for each pipeline here.
    public static final String METHYLATION = "meth-pipeline";
    public static final String GEX_AFFYMETRIX = "gex-affymetrix";
    public static final String GEX_ILLUMINA = "gex-illumina";
    public static final String CNV_ILLUMINA = "cnv-illumina";
    public static final String CNV_AFFYMETRIX = "cnv-affymetrix";
    public static final String SEQ_RNA = "seq-rna";
    public static final String SEQ_DNA = "seq-dna";
    public static final String GATK_WG_GERM = "gatk-wg-germ";
    public static final String GATK_WG_SOMA = "gatk-wg-soma";
    public static final String GATK_TAR_GERM = "gatk-tar-germ";
    public static final String GATK_TAR_SOMA = "gatk-tar-soma";
    
    // Machine generated constructor.
    public PipelineDB(String plName) {
        this.plName = plName;
    }
    public PipelineDB() {}

    // Return true if this pipeline belongs to the GATK Sequencing Pipeline
    // family.
    public static boolean isGATKPipeline(String pipeline_name) {
        if (pipeline_name.equals(GATK_WG_GERM) ||
                pipeline_name.equals(GATK_TAR_GERM) ||
                pipeline_name.equals(GATK_WG_SOMA) ||
                pipeline_name.equals(GATK_TAR_SOMA) )
            return true;
        return false;
    }
    
    // Return the editable pipeline hash map (pipeline name -> description).
    public LinkedHashMap<String, String> getEditablePlHash() {
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
            logger.error("FAIL to retrieve editable pipeline!");
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
    private static String getPlAttribute(String pipeline_name, String attr) {
        Connection conn = null;
        String query = "SELECT * FROM pipeline WHERE name = ?";
        String attribute = null;
        
        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement(query);
            stm.setString(1, pipeline_name);
            ResultSet result = stm.executeQuery();
            
            if (result.next()) {
                attribute = result.getString(attr);
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            StringBuilder err = new StringBuilder("FAIL to retrieve ").
                    append(attr).append(" for pipeline ").append(pipeline_name);
            logger.error(err);
//            logger.error("FAIL to retrieve " + attr + " for pipeline " + plName);
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return attribute;
    }
    
    // Return the pipeline object for this pipeline.
//    public static Pipeline getPipeline(String pipeline_name) 
    public Pipeline getPipeline() throws SQLException, NamingException {
        Connection conn = null;
        Pipeline command = null;
        String query = "SELECT * FROM pipeline WHERE name = ?";
        
        conn = DBHelper.getDSConn();
        PreparedStatement stm = conn.prepareStatement(query);
        
        stm.setString(1, plName);
        ResultSet rs = stm.executeQuery();
        
        if (rs.next()) {
            command = new Pipeline(rs);
        }

        stm.close();
        DBHelper.closeDSConn(conn);
        
        return command;
    }
    
    // Return all the pipeline currently setup in the database.
    public List<Pipeline> getAllPipeline() {
        Connection conn = null;
        List<Pipeline> plList = new ArrayList<>();

        try {
            conn = DBHelper.getDSConn();
            PreparedStatement stm = conn.prepareStatement
                                    ("SELECT * FROM pipeline ORDER BY tid, name");
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                plList.add(new Pipeline(rs));
            }
            stm.close();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve pipeline info!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return plList;
    }
    
    // Update the pipeline command in the database.
    public boolean updatePipeline(Pipeline cmd) {
        Connection conn = null;
        boolean result = Constants.OK;
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
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            StringBuilder err = new StringBuilder("FAIL to update ").
                                append(cmd.getName());
            logger.error(err);
//            logger.error("FAIL to update " + cmd.getName());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
    
    // Insert the new pipeline into database.
    public boolean insertPipeline(Pipeline cmd) {
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
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            StringBuilder err = new StringBuilder("FAIL to insert ").
                                append(cmd.getName());
            logger.error(err);
//            logger.error("FAIL to insert " + cmd.getName());
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        
        return result;
    }
}
