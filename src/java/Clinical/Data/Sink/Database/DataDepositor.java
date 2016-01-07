/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.Bean.AuthenticationBean;
import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * DataDepositor read in the processed data from the pipeline output and 
 * stored them into the database.
 * 
 * Author: Tay Wei Hong
 * Date: 19-Nov-2015
 * 
 * Revision History
 * 19-Nov-2015 - Created with the necessary methods implemented.
 * 24-Nov-2015 - DataDepositor will be run as a thread.
 * 03-Dec-2015 - Retrieve all the necessary info from the database.
 * 17-Dec-2015 - Group all the SQL statements as one transaction (i.e. 
 * autoCommit is set to off). Improve on the logic to insert gene value into
 * data array. Big improvement in the timing for inserting 22 records of 
 * 18,210/34,694 gene values; from 178 sec to 56 sec.
 * 23-Dec-2015 - Instead of receiving a single job_id, the constructor will
 * receive a list of job entries. This class will then process all those job_id
 * found in the list of job entries.
 * 28-Dec-2015 - Moved method subjectExistInDB to SubjectDB. Improve on the 
 * code in method insertFinalizedDataIntoDB().
 * 07-Jan-2016 - Continue to generate the consolidated output after the data
 * insertion has completed successfully. Generated the summary report after 
 * finalization.Removed unused code.
 */

public class DataDepositor extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataDepositor.class.getName());
    private final static Connection conn = DBHelper.getDBConn();
    private final String study_id, dept_id, annot_ver;
    private String fileUri, summaryReport;
    private int job_id, totalGene, processGene;
    // Categories of subject ID based on whether their meta data is found 
    // in the database or not.
    StringBuilder subjectNotFound, subjectFound;
    private List<FinalizingJobEntry> jobList = new ArrayList<>();
    
    public DataDepositor(String study_id, List<FinalizingJobEntry> jobList) {
        this.study_id = study_id;
        this.jobList = jobList;
        // Retrieve the value of dept_id and annot_ver from database.
        dept_id = UserAccountDB.getDeptID(AuthenticationBean.getUserName());
        annot_ver = StudyDB.getAnnotVer(study_id);
        summaryReport = Constants.getSYSTEM_PATH() + 
                        Constants.getFINALIZE_PATH() + study_id + 
                        Constants.getSUMMARY_FILE_NAME() + 
                        Constants.getSUMMARY_FILE_EXT();
        logger.debug("DataDepositor created for study: " + study_id);
    }
    
    @Override
    public void run() {
        Boolean finalizeStatus = Constants.OK;
        
        try {
            // All the SQL statements executed here will be treated as one 
            // big transaction.
            logger.debug("DataDepositor start - Set auto-commit to OFF.");
            conn.setAutoCommit(false);
            
            for (FinalizingJobEntry job : jobList) {
                // Retrieve the job ID and pipeline output file for this 
                // selected job.
                job_id = job.getJob_id();
                fileUri = SubmittedJobDB.getOutputPath(job_id);
                logger.debug("Data insertion for: " + study_id + " - " + 
                             job.getTid() + " - Job ID: " + job_id);
                
                if (!insertFinalizedDataIntoDB()) {
                    // Error occurred during data insertion, stop the transaction.
                    finalizeStatus = Constants.NOT_OK;
                    logger.error("DataDepositor - Hit error!");
                    break;
                }
            }
            
            if (finalizeStatus) {
                // All the SQL statements executed successfully, commit the changes.
                logger.debug("DataDepositor - Commit transaction.");
                conn.commit();
            }
            else {
                // Error occurred during data insertion, rollback the transaction.
                logger.error("DataDepositor - Rollback transaction.");
                conn.rollback();
            }
            
            conn.setAutoCommit(true);
            logger.debug("DataDepositor completed - Set auto-commit to ON.");
            if (finalizeStatus) {
                // Update job status to finalized
                for (FinalizingJobEntry job : jobList) {
                    SubmittedJobDB.updateJobStatusToFinalized(job.getJob_id());
                }
                // Generate the summary report for this study.
                generateSummaryReport();
                // Update the summary filepath in the study table.
                StudyDB.updateStudySummaryReport(study_id, summaryReport);
                // Generate the consolidated output for this study.
                DataRetriever retrieverThread = new DataRetriever(study_id);
                retrieverThread.start();
            }
            else {
                // Update study to uncompleted
                StudyDB.updateStudyCompletedStatus(study_id, false);
            }
        }
        catch (SQLException e) {
            logger.error("Falied to insert finalized data into database!");
            logger.error(e.getMessage());
        }
    }
    
    // Generate the summary report for the finalization of study.
    private void generateSummaryReport() {
        String[] subjects = subjectFound.toString().split("\\$");
        StringBuilder summary = new StringBuilder("Summary Report for Finalization of ");
        
        // Start to generate the summary report content
        summary.append(study_id).append("\n\n");
        summary.append("1. Technology employed - Pipeline executed - Date & Time").append("\n");
        for (FinalizingJobEntry job : jobList) {
            summary.append("\t").append(job.getTid()).append(" - ");
            summary.append(job.getPipeline_name()).append(" - ");
            summary.append(job.getSubmit_time()).append("\n");
        }
        summary.append("\n").append("2. Identified Subject ID").append("\n");
        for (String sub : subjects) {
            summary.append("\t").append(sub).append("\n");
        }
        summary.append("\n").append("3. Unidentified Subject ID").append("\n");
        summary.append("\t").append(subjectNotFound).append("\n");
        summary.append("\n").append("4. No of gene data available").append("\n");
        summary.append("\t").append(totalGene).append("\n");
        summary.append("\n").append("5. No of gene data stored").append("\n");
        summary.append("\t").append(processGene).append("\n\n");
        summary.append("Author: ").append(AuthenticationBean.getFullName()).append("\n");
        summary.append("Department: ").append(AuthenticationBean.getHeaderInstDept()).append("\n");
        summary.append("Date: ").append(Constants.getDateTime());

        // Start to produce the summary report.
        try {
            PrintStream ps = new PrintStream(new File(summaryReport));
            ps.print(summary);
        }
        catch (IOException ioe) {
            logger.error("IOException when writing summary report!");
            logger.error(ioe.getMessage());
        }
    }
    
    // Insert the gene value into the data array using the PreparedStatement
    // passed in.
    private void insertToDataArray(PreparedStatement stm, int array_index, 
            String value, String genename) throws SQLException {
        stm.setInt(1, array_index);
        stm.setString(2, value);
        stm.setString(3, genename);
        stm.executeUpdate();
    }
    
    // Insert a record into finalized_output table using the PreparedStatement
    // passed in.
    private void insertToFinalizedOutput(PreparedStatement stm, 
            FinalizedOutput record) throws SQLException {
        stm.setInt(1, record.getArray_index());
        stm.setString(2, record.getAnnot_ver());
        stm.setInt(3, record.getJob_id());
        stm.setString(4, record.getSubject_id());
        stm.executeUpdate();
            
        logger.debug("Output for " + record.getSubject_id() + 
                     " stored at index: " + record.getArray_index());        
    }
    
    // Return the next array index to be use for this record in 
    // finalized_output table.
    private int getNextArrayInd() {
        int count = Constants.DATABASE_INVALID_ID;
        String queryStr = "SELECT MAX(array_index) FROM finalized_output "
                        + "WHERE annot_ver = \'" + annot_ver + "\'";
        ResultSet rs = DBHelper.runQuery(queryStr);
        
        try{
            if (rs.next()) {
                count = rs.getInt(1) + 1;
            }
            rs.close();
        }
        catch (SQLException e) {
            logger.debug("SQLException when getting MAX(array_index)!");
            logger.debug(e.getMessage());
        }
        
        return count;
    }
    
    // Check whether genename exists using the PreparedStatement passed in.
    private Boolean checkGeneExistInDB(PreparedStatement stm, 
            String genename) throws SQLException {
        stm.setString(1, genename);
        ResultSet rs = stm.executeQuery();
        
        return rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
    }
    
    // Insert the finalized pipeline output into database.
    private Boolean insertFinalizedDataIntoDB() throws SQLException {
        Boolean result = Constants.OK;
        int[] arrayIndex;
        String[] values;
        // For record purpose
        int totalRecord, processedRecord;
        // To record the time taken to insert the processed data
        long startTime, elapsedTime;
        subjectNotFound = new StringBuilder();
        subjectFound = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(fileUri));
            //
            // **Subject line processing start here.
            //
            String lineRead = br.readLine();
            values = lineRead.split("\t");
            // Declare a integer array with size equal to the no of columns
            arrayIndex = new int[values.length];
            // No of subjects = total column - 2
            totalRecord = values.length - 2;
            processedRecord = 0;
            // INSERT statement to insert a record into finalized_output table.
            String insertStr = "INSERT INTO finalized_output(array_index,"
                             + "annot_ver,job_id,subject_id) VALUES(?,?,?,?)";
            
            try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
                // Ignore the first two strings (i.e. geneID and EntrezID); 
                // start at index 2. 
                for (int i = 2; i < values.length; i++) {
                    // Only store the pipeline output if the subject metadata is 
                    // available in the database.
                    try {
                        if (SubjectDB.isSubjectExistInDept(values[i], dept_id)) {
                            subjectFound.append(values[i]).append(" ");
                            processedRecord++;
                            arrayIndex[i] = getNextArrayInd();
                            FinalizedOutput record = new FinalizedOutput
                                (arrayIndex[i], annot_ver, values[i], job_id);
                            // Insert the finalized output record.
                            insertToFinalizedOutput(insertStm, record);                            
                            // At every 5th subject ID, place a marker '*'
                            if (processedRecord%5 == 0) {
                                subjectFound.append("$");
                            }
                        }
                        else {
                            subjectNotFound.append(values[i]).append(" ");
                            arrayIndex[i] = Constants.DATABASE_INVALID_ID;
                        }
                    } catch (SQLException e) {
                        // Error occurred, return to caller.
                        logger.error(e.getMessage());
                        return Constants.NOT_OK;
                    }
                }
                logger.info("Subject records processed: " + processedRecord + 
                            " out of " + totalRecord);
                // Record those subject ID not found; finalized data will not be stored.
                if (subjectNotFound.toString().isEmpty()) {
                    logger.debug("All the subject ID is found in database.");
                }
                else {
                    logger.debug("The following subject ID is not found in database " + 
                                 subjectNotFound);
                }
            }
            catch (SQLException e) {
                logger.error("SQLException when inserting finalized records!");
                logger.error(e.getMessage());
                // Error occurred, return to caller.
                return Constants.NOT_OK;
            }
            //
            // **Gene data processing start here.
            //
            String genename;
            totalGene = processGene = 0;
            // To record the total time taken to insert the finalized data.
            startTime = System.nanoTime();
            // UPDATE statement to update the data array in data_depository table.
            String updateStr = 
                    "UPDATE data_depository SET data[?] = ? WHERE " +
                    "genename = ? AND annot_ver = \'" + annot_ver + "\'";
            // SELECT statement to check the existence of gene in database.
            String queryGene = "SELECT 1 FROM data_depository "
                        + "WHERE genename = ? AND annot_ver = \'" 
                        + annot_ver + "\'";

            try (PreparedStatement updateStm = conn.prepareStatement(updateStr);
                 PreparedStatement queryGeneStm = conn.prepareStatement(queryGene)) 
            {
                while ((lineRead = br.readLine()) != null) {
                    totalGene++;
                    values = lineRead.split("\t");
                    // The first string is the gene symbol.
                    genename = values[0];
                    try {
                        // Check whether genename exist in data_depository.
                        if (checkGeneExistInDB(queryGeneStm,genename)) {
                            processGene++;
                            // Start reading in the data from 3rd string; 
                            // start from index 2.
                            for (int i = 2; i < values.length; i++) {
                                // Only process those data with valid PID
                                if (arrayIndex[i] != Constants.DATABASE_INVALID_ID) {
                                    // Insert gene value into data array.
                                    insertToDataArray(updateStm, arrayIndex[i],
                                            values[i],genename);                                    
                                }
                            }
                        }
                    } catch (SQLException e) {
                        // Error occurred, return to caller.
                        logger.error(e.getMessage());
                        return Constants.NOT_OK;
                    }
                }
                // Close the stream and releases any system resources associated
                // with it.
                br.close();
            } catch (SQLException e) {
                logger.error("SQLException when inserting into data array!");
                logger.error(e.getMessage());
                // Error occurred, return to caller.
                return Constants.NOT_OK;
            }
            // Record total time taken for the insertion.
            elapsedTime = System.nanoTime() - startTime;
            
            logger.debug("Total gene record processed: " + 
                    processGene + "/" + totalGene);
            logger.debug("Total time taken: " + (elapsedTime / 1000000000.0) +
                    " sec");
        }
        catch (IOException ioe) {
            logger.error("IOException when reading pipeline output file!");
            logger.error(ioe.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
}
