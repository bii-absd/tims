/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.General.Postman;
import Clinical.Data.Sink.General.ResourceRetriever;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
// Libraries for Apache PDFBox
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;

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
 * 08-Jan-2016 - Generate the PDF version of the summary report.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 20-Jan-2016 - Updated study table in database; added one new variable closed, 
 * and renamed completed to finalized.
 * 22-Jan-2016 - Study finalization logic change; finalization will be 
 * performed for each pipeline instead of each technology. Changed the format
 * of the summary report.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 27-Jan-2016 - To revert the submitted job status back to completed if 
 * finalization failed.
 * 26-Feb-2016 - Implementation for database 3.0 (Part 3).
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 14-Mar-2016 - Minor changes to the summary report.
 */

public class DataDepositor extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataDepositor.class.getName());
    private Connection conn = null;
    private final String study_id, grp_id, annot_ver, summaryReport;
    private String fileUri;
    private int job_id, totalGene, processedGene;
    // Store the filepath of the Astar and Bii logo.
    private static String astarLogo, biiLogo;
    // Store the y-axis of the PDF content stream cursor.
    float pageCursorYaxis;
    // Categories of subject ID based on whether their meta data is found 
    // in the database or not.
    StringBuilder subjectNotFound, subjectFound;
    private List<FinalizingJobEntry> jobList = new ArrayList<>();
    // Store the user ID of the current user.
    private final String userName;
    
    public DataDepositor(String userName, String study_id, 
            List<FinalizingJobEntry> jobList) 
            throws SQLException, NamingException
    {
        conn = DBHelper.getDSConn();
        this.userName = userName;
        this.study_id = study_id;
        this.jobList = jobList;
        // Retrieve the value of grp_id and annot_ver from database.
        grp_id = StudyDB.getStudyGrpID(study_id);
        annot_ver = StudyDB.getStudyAnnotVer(study_id);
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
                             job.getPipeline_name() + " - Job ID: " + job_id);
                
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
            // Send finalization status email to the user.
            Postman.sendFinalizationStatusEmail(study_id, userName, finalizeStatus);
            
            if (finalizeStatus) {
                // Update job status to finalized
                for (FinalizingJobEntry job : jobList) {
                    SubmittedJobDB.updateJobStatusToFinalized(job.getJob_id());
                }
                // Generate the summary report for this study.
                genPDFSummaryReport();
                // Update the summary filepath in the study table.
                StudyDB.updateStudySummaryReport(study_id, summaryReport);
                // Generate the consolidated output for this study.
                DataRetriever retrieverThread = new DataRetriever(study_id);
                retrieverThread.start();
            }
            else {
                // Finalization failed.
                // Revert job status back to completed.
                for (FinalizingJobEntry job : jobList) {
                    SubmittedJobDB.updateJobStatusToCompleted(job.getJob_id());
                }
                // Revert study back to unfinalized.
                StudyDB.updateStudyFinalizedStatus(study_id, false);
            }
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to insert finalized data!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Call by FinalizeStudyBean to setup the filepath of the Astar and Bii 
    // logo before starting the finalization process.
    public static void setupLogo(String astar, String bii) {
        astarLogo = astar;
        biiLogo = bii;
    }
    
    // Update the cursor y-axis to the next line, and return the new position.
    private float getNextLineYaxis() {
        pageCursorYaxis -= 15;
        return pageCursorYaxis;
    }
    // Update the cursor y-axis to the next sub-header, and return the new position.
    private float getNextSubheaderYaxis() {
        pageCursorYaxis -= 30;
        return pageCursorYaxis;
    }
    // Update the cursor y-axis to the next header, and return the new position.
    private float getNextHeaderYaxis() {
        pageCursorYaxis -= 50;
        return pageCursorYaxis;
    }
    
    // Generate the PDF summary report for the finalization of study.
    private void genPDFSummaryReport() {
        // In the summary report, we print 5 subjects on every line.
        String[] foundSubjects = subjectFound.toString().split("\\$");
        String[] missingSubjects = subjectNotFound.toString().split("\\$");
        // Create a PDF document and add a page to it.
        PDDocument doc = new PDDocument();
        PDPage page1 = new PDPage(PDPage.PAGE_SIZE_A4);
        doc.addPage(page1);
        // rect can be used to get the page width and height.
        PDRectangle rect = page1.getMediaBox();
        pageCursorYaxis = rect.getHeight();
        // x-axis position for each new subheader.
        float subheadX = 40;
        // x-axis position for each new content line.
        float lineX = 65;
        // Create some font objects by selecting one of the PDF base fonts.
        PDFont plainFont = PDType1Font.COURIER;
        PDFont boldFont = PDType1Font.COURIER_BOLD;
        PDFont italicFont = PDType1Font.COURIER_BOLD_OBLIQUE;

        // Start a new content stream to hold the created content.
        try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
            int line = 0;
            // Create 2 image objects for Astar and BII logo.
            PDJpeg astarImg = new PDJpeg(doc, 
                    new FileInputStream(astarLogo));
            PDJpeg biiImg = new PDJpeg(doc, 
                    new FileInputStream(biiLogo));
            // Draw the Astar and BII logo.
            cs.drawImage(astarImg, subheadX, pageCursorYaxis-100);
            cs.drawImage(biiImg, subheadX+200, pageCursorYaxis-100);
            pageCursorYaxis -= 100;
            
            // Write the title in bold first.
            cs.beginText();
            cs.setFont(boldFont, 14);
            cs.moveTextPositionByAmount(subheadX, getNextHeaderYaxis());
            cs.drawString("Summary Report for Finalization of " + study_id);
            cs.endText();
            
            // Write the body in plain.
            cs.beginText();
            cs.setFont(plainFont, 11);
            // Section A
            cs.moveTextPositionByAmount(subheadX, getNextHeaderYaxis());
            cs.drawString("A. Pipeline job(s) selected for finalization");
            cs.endText();
            int index = 1;
            
            for (FinalizingJobEntry job : jobList) {
                StringBuilder l1 = new StringBuilder
                                (String.valueOf(index++)).append(". ");
                l1.append(ResourceRetriever.getMsg(job.getPipeline_name()));
                StringBuilder l2 = new StringBuilder
                                ("(Submitted by ").append(job.getUserName());
                l2.append(" @").append(job.getSubmit_time()).append(")");
                cs.beginText();
                cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
                cs.drawString(l1.toString());
                cs.endText();
                cs.beginText();
                cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
                cs.drawString(l2.toString());
                cs.endText();                
            }
            // Section B
            cs.beginText();
            cs.moveTextPositionByAmount(subheadX, getNextSubheaderYaxis());
            cs.drawString("B. Identified Subject ID");
            cs.endText();
            
            for (String sub : foundSubjects) {
                cs.beginText();
                cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
                cs.drawString(sub);
                cs.endText();
            }
            // Section C
            cs.beginText();
            cs.moveTextPositionByAmount(subheadX, getNextSubheaderYaxis());
            cs.drawString("C. Unidentified Subject ID");
            cs.endText();
            
            for (String sub : missingSubjects) {
                cs.beginText();
                cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
                cs.drawString(sub);
                cs.endText();
            }
            // Section D
            cs.beginText();
            cs.moveTextPositionByAmount(subheadX, getNextSubheaderYaxis());
            cs.drawString("D. No of gene data available");
            cs.endText();
            
            cs.beginText();
            cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
            cs.drawString(String.valueOf(totalGene));
            cs.endText();
            // Section E
            cs.beginText();
            cs.moveTextPositionByAmount(subheadX, getNextSubheaderYaxis());
            cs.drawString("E. No of gene data stored");
            cs.endText();
            
            cs.beginText();
            cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
            cs.drawString(String.valueOf(processedGene));
            cs.endText();
            // Section F
            cs.beginText();
            cs.moveTextPositionByAmount(subheadX, getNextSubheaderYaxis());
            cs.drawString("F. HG19 Annotation Version");
            cs.endText();
            
            cs.beginText();
            cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
            cs.drawString(annot_ver);
            cs.endText();            
            // Ending section
            cs.beginText();
            cs.moveTextPositionByAmount(subheadX, getNextHeaderYaxis());
            cs.drawString("Author: " + UserAccountDB.getFullName(userName));
            cs.endText();
            
            cs.beginText();
            cs.moveTextPositionByAmount(subheadX, getNextLineYaxis());
            cs.drawString("From: " + UserAccountDB.getInstNameUnitID(userName));
            cs.endText();
            
            cs.beginText();
            cs.moveTextPositionByAmount(subheadX, getNextLineYaxis());
            cs.drawString("Date: " + Constants.getDateTime());
            cs.endText();
            
            // Application Signature
            cs.beginText();
            cs.setFont(italicFont, 12);
            cs.moveTextPositionByAmount(subheadX, getNextHeaderYaxis());
            cs.drawString("Generated by TIMS");
            cs.endText();
            
            // Close the content stream.
            cs.close();
            // Save the result, and close the document.
            doc.save(summaryReport);
            doc.close();
        } 
        catch (IOException|COSVisitorException ex) {
            logger.error("FAIL to create summary report!");
            logger.error(ex.getMessage());
        }
    }
    
    // Generate the summary report (.txt) for the finalization of study.
    private void genTxtSummaryReport() {
        String[] subjects = subjectFound.toString().split("\\$");
        StringBuilder summary = new StringBuilder("Summary Report for Finalization of ");
        
        // Start to generate the summary report content
        summary.append(study_id).append("\n\n");
        summary.append("1. Technology - Pipeline - Requestor - Date & Time").append("\n");
        for (FinalizingJobEntry job : jobList) {
            summary.append("\t").append(job.getTid()).append(" - ");
            summary.append(job.getPipeline_name()).append(" - ");
            summary.append(job.getUserName()).append(" - ");
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
        summary.append("\t").append(processedGene).append("\n\n");
        summary.append("Author: ").append(UserAccountDB.getFullName(userName)).append("\n");
        summary.append("From: ").append(UserAccountDB.getInstNameUnitID(userName)).append("\n");
        summary.append("Date: ").append(Constants.getDateTime());

        // Start to produce the summary report.
        try {
            PrintStream ps = new PrintStream(new File(summaryReport));
            ps.print(summary);
        }
        catch (IOException ioe) {
            logger.error("FAIL to create summary report!");
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
        stm.setString(5, record.getGrp_id());
        stm.executeUpdate();
            
        logger.debug("Output for " + record.getSubject_id() + 
                     " stored at index: " + record.getArray_index());        
    }
    
    // Return the next array index to be use for this record in 
    // finalized_output table.
    private int getNextArrayInd() {
        int count = Constants.DATABASE_INVALID_ID;
        String query = "SELECT MAX(array_index) FROM finalized_output "
                     + "WHERE annot_ver = \'" + annot_ver + "\'";
        
        try (PreparedStatement stm = conn.prepareStatement(query)) {
            ResultSet rs = stm.executeQuery();
            
            if (rs.next()) {
                count = rs.getInt(1) + 1;
            }
            rs.close();
        }
        catch (SQLException e) {
            logger.debug("FAIL to retrieve MAX(array_index)!");
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
        int totalRecord, processedRecord, unprocessedRecord;
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
            processedRecord = unprocessedRecord = 0;
            // INSERT statement to insert a record into finalized_output table.
            String insertStr = "INSERT INTO finalized_output(array_index,"
                             + "annot_ver,job_id,subject_id,grp_id) "
                             + "VALUES(?,?,?,?,?)";
            
            try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
                // Ignore the first two strings (i.e. geneID and EntrezID); 
                // start at index 2. 
                for (int i = 2; i < values.length; i++) {
                    // Only store the pipeline output if the subject metadata is 
                    // available in the database.
                    try {
                        if (SubjectDB.isSubjectExistInGrp(values[i], grp_id)) {
                            subjectFound.append(values[i]).append(" ");
                            processedRecord++;
                            arrayIndex[i] = getNextArrayInd();
                            FinalizedOutput record = new FinalizedOutput
                                (arrayIndex[i], annot_ver, values[i], grp_id, job_id);
                            // Insert the finalized output record.
                            insertToFinalizedOutput(insertStm, record);                            
                            // At every 5th subject ID, place a marker '$'
                            if (processedRecord%5 == 0) {
                                subjectFound.append("$");
                            }
                        }
                        else {
                            subjectNotFound.append(values[i]).append(" ");
                            unprocessedRecord++;
                            arrayIndex[i] = Constants.DATABASE_INVALID_ID;
                            // At every 5th subject ID, place a marker '$'
                            if (unprocessedRecord%5 == 0) {
                                subjectNotFound.append("$");
                            }
                        }
                    } catch (SQLException e) {
                        // Error occurred, return to caller.
                        logger.error(e.getMessage());
                        return Constants.NOT_OK;
                    }
                }
                logger.debug("Subject records processed: " + processedRecord + 
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
            catch (SQLException|NamingException e) {
                logger.error("FAIL to insert finalized records!");
                logger.error(e.getMessage());
                // Error occurred, return to caller.
                return Constants.NOT_OK;
            }
            //
            // **Gene data processing start here.
            //
            String genename;
            totalGene = processedGene = 0;
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
                            processedGene++;
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
                logger.error("FAIL to insert into data array!");
                logger.error(e.getMessage());
                // Error occurred, return to caller.
                return Constants.NOT_OK;
            }
            // Record total time taken for the insertion.
            elapsedTime = System.nanoTime() - startTime;
            
            logger.debug("Total gene record processed: " + 
                    processedGene + "/" + totalGene);
            logger.debug("Total time taken: " + (elapsedTime / 1000000000.0) +
                    " sec");
        }
        catch (IOException ioe) {
            logger.error("FAIL to read pipeline output file!");
            logger.error(ioe.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
}