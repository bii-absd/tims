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

public class DataVoid extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataVoid.class.getName());
    private Connection conn = null;
    private List<Integer> jobIDList = new ArrayList<>();
    private List<Integer> arrayIndList = new ArrayList<>();
    private final String userName, study_id, annot_ver;
    
    public DataVoid(String userName, String study_id) 
            throws SQLException, NamingException
    {
        conn = DBHelper.getDSConn();
        this.userName = userName;
        this.study_id = study_id;
        annot_ver = StudyDB.getStudyAnnotVer(study_id);
        // Retrieve the list of job IDs that have been finalized for this study.
        jobIDList = SubmittedJobDB.getFinalizedJobIDs(study_id);
    }
    
    @Override
    public void run() {
        boolean unfinResult = Constants.OK;
        // To record the time taken to unfinalize the study.
        long startTime, elapsedTime;

        for (Integer jobID : jobIDList) {
            arrayIndList.addAll(getArrayIndexes(jobID));
        }
        logger.debug("Job IDs: " + jobIDList.toString());
        logger.debug("Indexes: " + arrayIndList.toString());
        
        startTime = System.nanoTime();
        try {
            // All the SQL statements executed here will be treated as one
            // big transaction.
            logger.debug("DataVoid start - Set auto-commit to OFF.");
            conn.setAutoCommit(false);
            
            if (!voidAllData()) {
                logger.error("DataVoid - Hit error when trying to void the data!");
                unfinResult = Constants.NOT_OK;
            }
            else {
                // All the data in data depository have been voided, now 
                // proceed to void all the records in finalized output table.
                if (!voidAllFinalizedRecords()) {
                    logger.error("DataVoid - Hit error when trying to void the finalized record!");
                    unfinResult = Constants.NOT_OK;
                }
            }
            
            if (unfinResult) {
                // Revert study back to unfinalized.
                StudyDB.updateStudyFinalizedStatus(study_id, false);
                // All the SQL statements executed successfully, commit the changes.
                logger.debug("DataVoid - Commit transaction.");
                conn.commit();
                // Delete the consolidated output, detail output, and report 
                // summary from the application folder.
                Study tmp = StudyDB.getStudyObject(study_id);
                if (!FileHelper.delete(tmp.getFinalized_output())) {
                    logger.error("FAIL to delete consolidated output of " + study_id);
                }
                if (!FileHelper.delete(tmp.getSummary())) {
                    logger.error("FAIL to delete finalized summary report of " + study_id);
                }
                if (!FileHelper.delete(tmp.getDetail_files())) {
                    logger.error("FAIL to delete zipped detail output of " + study_id);
                }
            }
            else {
                // Error occurred during unfinalization, rollback the transaction.
                logger.debug("DataVoid - Rollback transaction.");
                conn.rollback();
            }
            
            conn.setAutoCommit(true);
            logger.debug("DataVoid completed - Set auto-commit to ON");
        }
        catch (SQLException e) {
            logger.error("FAIL to unfinalize study!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }

        elapsedTime = System.nanoTime() - startTime;
        logger.debug("Total time taken to unfinalize " + study_id + 
                " is " + (elapsedTime / 1000000000.0) + " sec.");
        // Send un-finalization status email to the user.
        Postman.sendUnFinalizationStatusEmail(study_id, userName, unfinResult);
    }
    
    // Retrieve all the array indexes that belong to this job ID.
    private List<Integer> getArrayIndexes(int job_id) {
        List<Integer> indexList = new ArrayList<>();
        String query = "SELECT array_index FROM finalized_record WHERE "
                     + "annot_ver = \'" + annot_ver + "\' AND job_id = " 
                     + job_id;
        
        try (PreparedStatement stm = conn.prepareStatement(query);
             ResultSet rs = stm.executeQuery()) {
            while (rs.next()) {
                indexList.add(rs.getInt("array_index"));
            }
            logger.debug("Array indexes retrieved for job ID " + job_id);
        }
        catch (SQLException e) {
            logger.error("FAIL to retrieve array indexes!");
            logger.error(e.getMessage());
        }

        return indexList;
    }
    
    // Set all the related data at data depository table to VOID.
    private boolean voidAllData() {
        boolean result = Constants.OK;
        // To record the time taken to void the data.
        long startTime, elapsedTime;
        String query = "UPDATE data_depository SET data[?] = \'VOID\' "
                     + "WHERE annot_ver = \'" + annot_ver + "\'";
        
        try (PreparedStatement stm = conn.prepareStatement(query)) {            
            for (Integer index : arrayIndList) {
                startTime = System.nanoTime();
                voidData(stm, index);
                elapsedTime = System.nanoTime() - startTime;
                logger.debug("Void data for index " + index + " took " + 
                            (elapsedTime / 1000000000.0) + " sec.");
            }
            logger.debug("All data voided.");
        }
        catch (SQLException e) {
            logger.error("FAIL to void data!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }

        return result;
    }
    
    // Set the data at data depository table to VOID.
    private void voidData(PreparedStatement stm, int index) 
            throws SQLException {
        stm.setInt(1, index);
        stm.executeUpdate();
    }
    
    // Void all the related records in finalized output table i.e. by setting
    // job_id to 0 and subject_id to VOID.
    private boolean voidAllFinalizedRecords() {
        boolean result = Constants.OK;
        String query = "UPDATE finalized_record SET job_id = 0, "
                     + "subject_id = \'VOID\' WHERE annot_ver = \'" 
                     + annot_ver + "\' AND job_id = ?";
        
        try (PreparedStatement stm = conn.prepareStatement(query)) {
            for (Integer jobID : jobIDList) {
                stm.setInt(1, jobID);
                stm.executeUpdate();
                // Revert job status to completed.
                SubmittedJobDB.updateJobStatusToCompleted(jobID);
            }
        }
        catch (SQLException e) {
            logger.error("FAIL to void finalized record!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
}
