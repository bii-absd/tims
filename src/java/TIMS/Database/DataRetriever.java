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
import TIMS.General.Postman;
// Libraries for Java
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class DataRetriever extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataRetriever.class.getName());
    private Connection conn = null;
    private final String study_id, annot_ver, finalize_file, 
                         finalize_meta, userName;
    private final List<OutputItems> opItemsList;
    private final List<String> geneList;
    private StringBuilder opHeader = new StringBuilder();
    
    // Using the study_id received, DataRetriever will retrieve the finalized
    // data from the database.
    public DataRetriever(String study_id, String userName) 
            throws SQLException, NamingException
    {
        this.study_id = study_id;
        this.userName = userName;
        finalize_file = Constants.getSYSTEM_PATH() + 
                        Constants.getSTUDIES_PATH() + 
                        study_id + File.separator + 
                        "output" + Constants.getFINALIZE_FILE_EXT();
        finalize_meta = Constants.getSYSTEM_PATH() + 
                        Constants.getSTUDIES_PATH() + 
                        study_id + File.separator + 
                        "meta" + Constants.getFINALIZE_FILE_EXT();
        annot_ver = StudyDB.getStudyAnnotVer(study_id);
        // Get a data source connection for this thread.
        conn = DBHelper.getDSConn();
        
        // Subject ID|Pipeline Name|Pipeline output
        opHeader.append("Subject|Pipeline");
        geneList = getGeneList();
        // Retrieve the list of OutputItems (i.e. Subject ID|Pipeline Name|Index)
        opItemsList = getOpItemsList();
        logger.debug("DataRetriever created for study_id: " + study_id);
    }
    
    @Override
    public void run() {
        logger.debug("DataRetriever start running.");
        consolidateFinalizedData();
        FileHelper.generateMetaDataList(study_id, finalize_meta);
        // Close the data source connection after use.
        DBHelper.closeDSConn(conn);
        // Zip the finalized output file and meta data file into one package.
        String[] srcFiles = {finalize_file, finalize_meta};
        String zipFile = Constants.getSYSTEM_PATH() + 
                         Constants.getSTUDIES_PATH() + 
                         study_id + File.separator +
                         "finalized_output" + Constants.getZIPFILE_EXT();
        try {
            FileHelper.zipFiles(zipFile, srcFiles);
            logger.debug("Output and Meta data files for Study " + study_id + " zipped.");
            // Update the study with the zipped file path.
            StudyDB.updateStudyFinalizedFile(study_id, zipFile);
            // Delete the original output and Meta data files to free up memory space.
            FileHelper.delete(finalize_file);
            FileHelper.delete(finalize_meta);
        }
        catch (IOException e) {
            logger.error("FAIL to zip output and Meta data files for Study " + study_id);
            logger.error(e.getMessage());
        }

        // Send success finalization status email to the user.
        Postman.sendFinalizationStatusEmail(study_id, userName, Constants.OK);
    }
    
    // To build the subject line for the output file.
    private String getSubjectData(OutputItems item) {
        // For time logging purpose.
        long elapsedTime;
        long startTime = System.nanoTime();
        StringBuilder data = new StringBuilder();
        String query = "SELECT data[?] FROM data_depository " 
                     + "WHERE annot_ver = ? ORDER BY genename";
        
        data.append(item.getSubject_id()).append("|").append(item.getPipeline());
        
        try (PreparedStatement stm = conn.prepareStatement(query)) {
            stm.setInt(1, item.getArray_index());
            stm.setString(2, annot_ver);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                data.append("|").append(rs.getString(1));
            }
            
            elapsedTime = System.nanoTime() - startTime;
            logger.debug(item.getSubject_id() + ": " + 
                    (elapsedTime / 1000000.0) + " msec");
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve subject data!");
            logger.error(e.getMessage());
        }

        return data.toString();
    }
    
    // Retrieve the finalized data from the database, consolidate and output
    // them to a text file.
    private void consolidateFinalizedData() {
        // For time logging purpose.
        long elapsedTime;
        long startTime = System.nanoTime();

        try (PrintStream ps = new PrintStream(new File(finalize_file))) {
            // Write the header/subject line first
            ps.println(opHeader);
            // Loop through the output items and write the subject output one
            // at a time.
            opItemsList.stream().forEach((item) -> {
                ps.println(getSubjectData(item));
            });
            elapsedTime = System.nanoTime() - startTime;
            logger.debug("Total time taken to write output: " + 
                    (elapsedTime / 1000000000.0) + " sec.");
        }
        catch (IOException ioe) {
            logger.error("FAIl to write output to file!");
            logger.error(ioe.getMessage());
        }
    }
    
    // Retrieve the list of genename that is relevant to the annotation 
    // version used in the study.
    private List<String> getGeneList() {
        List<String> gene = new ArrayList<>();
        // For time logging purpose.
        long elapsedTime;
        long startTime = System.nanoTime();
        String query = "SELECT genename FROM data_depository " 
                     + "WHERE annot_ver = ? ORDER BY genename";
        // Retrieve all the gene that appeared in this HG19 annotation version.
        try (PreparedStatement stm = conn.prepareStatement(query)) {
            stm.setString(1, annot_ver);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                gene.add(rs.getString("genename"));
                opHeader.append("|").append(rs.getString("genename"));
            }
            
            elapsedTime = System.nanoTime() - startTime;
            logger.debug("No of genename retrieved: " + gene.size());
            logger.debug("Time taken: " + (elapsedTime / 1000000.0) + " msec.");
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve genename!");
            logger.error(e.getMessage());
        }

        return gene;
    }
    
    // Retrieve the output row information (i.e. subject_id|pipeline|array_index 
    // where gene's values are stored) from the database.
    private List<OutputItems> getOpItemsList() {
        List<OutputItems> opList = new ArrayList<>();
        String query = 
                "SELECT y.subject_id, x.pipeline_name, y.array_index "
                + "FROM (SELECT job_id, pipeline_name FROM submitted_job WHERE "
                + "study_id = ? AND status_id = 5) x NATURAL JOIN " +
                "(SELECT * FROM finalized_record) y WHERE job_id = x.job_id " +
                "ORDER BY y.subject_id, y.array_index";
        
        try (PreparedStatement stm = conn.prepareStatement(query)) {
            stm.setString(1, study_id);
            ResultSet rs = stm.executeQuery();
            
            while (rs.next()) {
                OutputItems item = new OutputItems(
                                    rs.getString("subject_id"),
                                    rs.getString("pipeline_name"),
                                    rs.getInt("array_index"));
                opList.add(item);                
            }
            
            logger.debug("Total output row retrieved: " + opList.size());
        }
        catch (SQLException e) {
            logger.error("FAIL to build output list!");
            logger.error(e.getMessage());
        }

        return opList;
    }

    /*
    // NOTE: When using System.out.println the last character CANNOT be a "|"
    // else weird message will be printed.
    // e.g. [#|2015-12-04T11:42:02.340+0800|INFO|glassfish 4.1||_ThreadID=933;
    System.out.println("Subject|Technology|Array_index");
    for (OutputItems item : opList) {
        System.out.println(item.toString());
    }
    */
}
