/*
 * Copyright @2016-2017
 */
package TIMS.Database;

import TIMS.General.Constants;
import TIMS.General.FileHelper;
import TIMS.General.Postman;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

/**
 * VaultKeeper read in the processed data from the pipeline output and 
 * stored them into the vault.
 * 
 * Author: Tay Wei Hong
 * Date: 14-Mar-2016
 * 
 * Revision History
 * 14-Mar-2016 - Created with the necessary methods implemented. Implemented 
 * the module to close study.
 * 22-Mar-2016 - Changes due to the addition field (i.e. icd_code) in the 
 * vault_record table.
 * 04-Apr-2016 - When checking for subject Meta data availability, the system
 * will now check against the new study_subject table. The system will now store
 * the study ID (instead of icd_code) into the finalized_record.
 * 13-May-2016 - Minor changes as the pipeline output file will now be zipped.
 * 19-May-2016 - To delete those temporary files generated during closure
 * of study.
 * 10-Aug-2016 - In method storePlDataIntoVault(), use the try-with-resource
 * statement to create the BufferedReader object. Performed code refactoring on
 * method storePlDataIntoVault().
 * 12-Dec-2016 - Add in the semaphore control during insertion of data into 
 * vault_data table. Removed unused code.
 * 19-Apr-2017 - Subject's meta data will now be own by study, and the study 
 * will be own by group i.e. the direct link between group and subject's meta 
 * data will be break off.
 */

public class VaultKeeper extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(VaultKeeper.class.getName());
    private Connection conn = null;
    private List<Integer> jobList = new ArrayList<>();
    private String fileUri;
    private final String study_id, grp_id, annot_ver, userName;
    // Variables to be used during processing of pipeline output.
    private int totalRecord, processedRecord, totalGene, processedGene;
    private int[] vaultIndex;

    public VaultKeeper(String userName, String study_id) 
            throws SQLException, NamingException 
    {
        conn = DBHelper.getDSConn();
        this.userName = userName;
        this.study_id = study_id;
        jobList = SubmittedJobDB.getFinalizedJobIDs(study_id);
        // Retrieve the value of grp_id, annot_ver and icd_code from database.
        Study study = StudyDB.getStudyObject(study_id);
        grp_id = study.getGrp_id();
        annot_ver = study.getAnnot_ver();
        
        logger.debug("VaultKeeper created for study " + study_id);
    }
    
    @Override
    public void run() {
        boolean closureStatus = Constants.OK;
        
        try {
            // Need to acquire the closure token before proceeding.
            DBHelper.acquireClosureToken(userName);
            // All the SQL statements executed here will be treated as one
            // big transaction.
            logger.debug("VaultKeeper start - Set auto-commit to OFF.");
            conn.setAutoCommit(false);
            
            for (Integer job_id : jobList) {
                fileUri = SubmittedJobDB.unzipOutputFile(job_id);
                logger.debug("Storing data for: " + study_id + " - job ID " + job_id);
                
                if (!storePlDataIntoVault(job_id)) {
                    // Error occurred when storing data into vault, stop the 
                    // transaction.
                    closureStatus = Constants.NOT_OK;
                    logger.error("VaultKeeper - Hit error!");
                    break;
                }
                // Delete the temporary file here (i.e. fileUri)
                if (!FileHelper.delete(fileUri)) {
                    logger.error("FAIL to delete the temporary files generated "
                               + "during closing of " + study_id);
                }
            }
                
            if (closureStatus) {
                // All the SQL statements executed successfully, commit the changes.
                logger.debug("VaultKeeper - Commit transaction.");
                conn.commit();
            }
            else {
                // Error occurred when storing data, rollback the transaction.
                logger.error("VaultKeeper - Rollback transaction.");
                conn.rollback();
                // Revert study status to not close.
                StudyDB.updateStudyClosedStatus(study_id, false);
            }
            
            conn.setAutoCommit(true);
            logger.debug("VaultKeeper - Set auto-commit to ON.");
            // Release the closure token.
            DBHelper.releaseClosureToken(userName);
            // Send closure status email to the user.
            Postman.sendStudyClosureStatusEmail(study_id, userName, closureStatus);
        }
        catch (SQLException|InterruptedException e) {
            logger.error("FAIL to store data into the vault!");
            logger.error(e.getMessage());
            // Closure failed, release the token.
            DBHelper.releaseClosureToken(userName);
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Helper function to process the first line (i.e. subject line) of 
    // pipeline data file. Return the processing status.
    private boolean procSubjectLine(String[] values, int job_id) {
        boolean result = Constants.OK;
        StringBuilder subjectNotFound = new StringBuilder();
        // INSERT statement to insert a record into vault_record table.
        String insertStr = "INSERT INTO vault_record(array_index,"
                         + "annot_ver,job_id,subject_id,study_id) "
                         + "VALUES(?,?,?,?,?)";

        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            // Ignore the first two strings (i.e. geneID and EntrezID).
            for (int i = 2; i < values.length; i++) {
                // Only store the pipeline data if the subject record is 
                // available in the database.
                if (SubjectRecordDB.isSRExist(values[i], study_id)) {
                    processedRecord++;
                    vaultIndex[i] = getNextVaultInd();
                    FinalizedRecord record = new FinalizedRecord
                        (vaultIndex[i], annot_ver, values[i], job_id, study_id);
                    // Create an vault record.
                    createVaultRecord(insertStm, record);
                }
                else {
                    subjectNotFound.append(values[i]).append(" ");
                    vaultIndex[i] = Constants.DATABASE_INVALID_ID;
                }
            }
            logger.debug("Records processed: " + processedRecord + 
                         " out of " + totalRecord);
            // Some of the subject IDs are not found for this pipeline.
            // Display the consolidated subject IDs that are not found.
            if (totalRecord > processedRecord) {
                logger.debug("The following subject IDs are not found " + 
                             subjectNotFound);
            }
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to create vault records!");
            logger.error(e.getMessage());
            // Error occurred, return to caller.
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Helper function to process the gene data of pipeline output. Return the 
    // processing status.
    private boolean procGeneData(BufferedReader br, int job_id) throws IOException {
        boolean result = Constants.OK;
        String genename, lineRead;
        String[] values;
        totalGene = processedGene = 0;
        // UPDATE statement to update the data array in vault_data table.
        String updateStr = "UPDATE vault_data SET data[?] = ? WHERE " 
                         + "genename = ? AND annot_ver = \'" 
                         + annot_ver + "\'";
        // SELECT statement to check the existence of gene in database.
        String queryGene = "SELECT 1 FROM vault_data WHERE "
                         + "genename = ? AND annot_ver = \'" 
                         + annot_ver + "\'";
        // This debug message serve as a check point.
        logger.debug("Start gene data processing for job " + job_id);
        
        try (PreparedStatement updateStm = conn.prepareStatement(updateStr);
             PreparedStatement queryGeneStm = conn.prepareStatement(queryGene)) 
        {
            while ((lineRead = br.readLine()) != null) {
                totalGene++;
                values = lineRead.split("\t");
                // The first string is the gene symbol.
                genename = values[0];
                // Only store the data if the genename exist in vault_data.
                if (isGeneExistInVault(queryGeneStm,genename)) {
                    processedGene++;
                    // Gene data start from the 3rd column. 
                    for (int i = 2; i < values.length; i++) {
                        // Only process those data with valid PID
                        if (vaultIndex[i] != Constants.DATABASE_INVALID_ID) {
                            // Store gene data into vault.
                            storeGeneDataIntoVault(updateStm, vaultIndex[i],
                                                   values[i],genename);                                    
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("FAIL to store gene data into vault!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }

        return result;
    }
    
    // Store the subject's pipeline data into the vault.
    private boolean storePlDataIntoVault(int job_id) {
        boolean result = Constants.OK;
        String[] values;
        // Reset global variables before start processing.
        totalRecord = processedRecord = 0;
        vaultIndex = null;
        // To record the time taken to store the finalized data.
        long startTime, elapsedTime;

        try (BufferedReader br = new BufferedReader(new FileReader(fileUri))) {
            String lineRead = br.readLine();
            values = lineRead.split("\t");
            // Update the size of the integer array.
            vaultIndex = new int[values.length];
            // No of records = total column - 2
            totalRecord = values.length - 2;
            // Start subject line processing.
            if (!procSubjectLine(values, job_id)) {
                // Error occurred, return to caller.
                return Constants.NOT_OK;
            }

            // Only proceed with gene data processing if subject ID is found in
            // the database.
            if (processedRecord > 0) {
                startTime = System.nanoTime();
                if (procGeneData(br, job_id)) {
                    // Record the time taken for storing the data into vault.
                    elapsedTime = System.nanoTime() - startTime;
                    logger.debug("Gene record processed: " + processedGene + "/" + totalGene);
                    logger.debug("Time taken: " + (elapsedTime / 1000000000.0) + " sec");

                }
                else {
                    // Error occurred, return to caller.
                    return Constants.NOT_OK;
                }
            }
            else {
                logger.debug("None of the subject ID for this pipeline output is found. "
                        + "Gene data will not be stored into database.");
            }
        }
        catch (IOException e) {
            logger.error("FAIL to read pipeline output file for job ID " + job_id);
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Return the next vault index to be use for this record.
    private int getNextVaultInd() {
        int count = Constants.DATABASE_INVALID_ID;
        String query = "SELECT MAX(array_index) FROM vault_record "
                     + "WHERE annot_ver = \'" + annot_ver + "\'";
        
        try (PreparedStatement stm = conn.prepareStatement(query);
             ResultSet rs = stm.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1) + 1;
            }
        }
        catch (SQLException e) {
            logger.debug("FAIL to retrieve the next vault index!");
            logger.debug(e.getMessage());
        }
        
        return count;
    }
    
    // Create an vault record which serve as a key to link the subject with
    // the gene data stored in the vault.
    private void createVaultRecord(PreparedStatement stm, FinalizedRecord record) 
            throws SQLException {
        stm.setInt(1, record.getArray_index());
        stm.setString(2, record.getAnnot_ver());
        stm.setInt(3, record.getJob_id());
        stm.setString(4, record.getSubject_id());
        stm.setString(5, record.getStudy_id());
        stm.executeUpdate();

        logger.debug("Vault record for " + record.getSubject_id() + 
                     " created with index " + record.getArray_index());        
    }

    // Check whether genename exists.
    private Boolean isGeneExistInVault(PreparedStatement stm, 
            String genename) throws SQLException {
        stm.setString(1, genename);
        ResultSet rs = stm.executeQuery();
        
        return rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
    }
    
    // Store the gene data into the vault.
    private void storeGeneDataIntoVault(PreparedStatement stm, int array_index, 
            String value, String genename) throws SQLException {
        stm.setInt(1, array_index);
        stm.setString(2, value);
        stm.setString(3, genename);
        stm.executeUpdate();
    }
}
