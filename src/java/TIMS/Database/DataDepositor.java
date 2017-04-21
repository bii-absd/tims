/*
 * Copyright @2015-2017
 */
package TIMS.Database;

import TIMS.General.Constants;
import TIMS.General.FileHelper;
import TIMS.General.Postman;
import TIMS.General.ResourceRetriever;
import java.io.BufferedReader;
import java.io.FileInputStream;
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
 * 22-Mar-2016 - Changes due to the addition field (i.e. icd_code) in the 
 * finalized_record table.
 * 24-Mar-2016 - To improve on the generation of summary report i.e. the ability
 * to generate multiple pages, have a set of gene available versus stored 
 * information for each pipeline, etc.
 * 28-Mar-2016 - Added in a checkpoint debug message before starting the gene
 * data processing; to make sure the thread is still alive.
 * 04-Apr-2016 - When checking for subject Meta data availability, the system
 * will now check against the new study_subject table. The system will now store
 * the study ID (instead of icd_code) into the finalized_record.
 * 13-Apr-2016 - Only send out the finalization status email here if the 
 * finalization has failed, else let DataRetriever send out the status email
 * after all the pipeline output have been consolidated.
 * 14-Apr-2016 - Changes due to the type change (i.e. to Timestamp) for 
 * submit_time and complete_time in submitted_job table.
 * 13-May-2016 - Bug fix: To ensure the summary report has enough space to hold 
 * the last section of the report. Minor changes as the pipeline output file 
 * will now be zipped.
 * 19-May-2016 - To delete those temporary files generated during finalization
 * of study. To zip all the detail output file(s) into a single zip file.
 * 03-Jun-2016 - Reduce the top spacing of the summary report (for page 2 and
 * beyond).
 * 22-Jul-2016 - Fix the bug where empty string get printed in section B when 
 * generating the summary report.
 * 10-Aug-2016 - In method insertFinalizedDataIntoDB(), use the 
 * try-with-resource statement to create the BufferedReader object. Performed
 * code refactoring on method insertFinalizedDataIntoDB().
 * 30-Aug-2016 - Changes due to change in method name in Constants class. 
 * Removed unused code.
 * 12-Dec-2016 - Add in the semaphore control during insertion of data into
 * data_depository table. Removed unused code.
 * 13-Feb-2017 - To include the parameters setup during pipeline execution into
 * the summary report.
 * 20-Feb-2017 - To revert all the submitted jobs and study status if during
 * finalization, exception occurred. Re-index the annotation index on data 
 * depositor table after successful finalization. To include the subject record
 * stored information in the summary report. Added 2 private methods 
 * revertStudyStatus() and reindexDataDepositoryIndex().
 * 19-Apr-2017 - Subject's meta data will now be own by study, and the study 
 * will be own by group i.e. the direct link between group and subject's meta 
 * data will be break off.
 */

public class DataDepositor extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataDepositor.class.getName());
    private Connection conn = null;
    private final String study_id, grp_id, annot_ver, summaryReportPath;
    private String fileUri;
    private int job_id, numSubjectNotFound, numSubjectFound;
    // Variables to be used during processing of pipeline output.
    private int totalRecord, processedRecord, totalGene, processedGene;
    private int[] arrayIndex;
    // Store the filepath of the Astar and Bii logo.
    private static String astarLogo, biiLogo;
    // Store the y-axis of the PDF content stream cursor.
    private float pageCursorYaxis;
    // Categories of subject ID based on whether their meta data is found 
    // in the database or not.
    private StringBuilder subjectNotFound, subjectFound, geneAvailableVsStored, 
                          subjectAvailableVsStored;
    private List<FinalizingJobEntry> jobList = new ArrayList<>();
    // Store the user ID of the current user.
    private final String userName;
    // No of subjects to print per line.
    private final int SUBJECTS_PER_LINE = 3;
    // Create a pdf document for the summary report.
    private final PDDocument summary_pdf = new PDDocument();
    private PDPage page;

    
    public DataDepositor(String userName, String study_id, 
            List<FinalizingJobEntry> jobList) 
            throws SQLException, NamingException
    {
        conn = DBHelper.getDSConn();
        page = new PDPage(PDPage.PAGE_SIZE_A4);
        subjectNotFound = new StringBuilder();
        subjectFound = new StringBuilder();
        geneAvailableVsStored = new StringBuilder();
        subjectAvailableVsStored = new StringBuilder();
        numSubjectNotFound = numSubjectFound = 0;
        this.userName = userName;
        this.study_id = study_id;
        this.jobList = jobList;
        // Retrieve the value of grp_id, annot_ver and icd_code from database.
        Study study = StudyDB.getStudyObject(study_id);
        grp_id = study.getGrp_id();
        annot_ver = study.getAnnot_ver();
        summaryReportPath = Constants.getSYSTEM_PATH() + 
                            Constants.getFINALIZE_PATH() + study_id + 
                            Constants.getSUMMARY_FILE_NAME() + 
                            Constants.getSUMMARY_FILE_EXT();
        logger.debug("DataDepositor created for study: " + study_id);
    }
    
    @Override
    public void run() {
        boolean finalizeStatus = Constants.OK;
        
        try {
            // Need to acquire the finalization token before proceeding.
            DBHelper.acquireFinalizeToken(userName);
            // All the SQL statements executed here will be treated as one 
            // big transaction.
            logger.debug("DataDepositor start - Set auto-commit to OFF.");
            conn.setAutoCommit(false);
            
            for (FinalizingJobEntry job : jobList) {
                // Retrieve the job ID and pipeline output file for this 
                // selected job.
                job_id = job.getJob_id();
                fileUri = SubmittedJobDB.unzipOutputFile(job_id);
                logger.debug("Data insertion for: " + study_id + " - " + 
                             job.getPipeline_name() + " - Job ID: " + job_id);
                
                if (!insertFinalizedDataIntoDB()) {
                    // Error occurred during data insertion, stop the transaction.
                    finalizeStatus = Constants.NOT_OK;
                    logger.error("DataDepositor - Hit error!");
                    break;
                }
                // Delete the temporary file here (i.e. fileUri)
                if (!FileHelper.delete(fileUri)) {
                    logger.error("FAIL to delete the temporary files generated "
                               + "during finalization of " + study_id);
                }
            }
            
            if (finalizeStatus) {
                // All the SQL statements executed successfully, commit the changes.
                logger.debug("DataDepositor - Commit transaction.");
                conn.commit();
                // Re-index the deposit_annot_ind.
                if (reindexDataDepositoryIndex()) {
                    logger.debug("Re-index of annotation index on data depository table passed.");
                }
                else {
                    logger.error("FAIL to re-index the annotation index on data depository table!");
                }
                conn.commit();
            }
            else {
                // Error occurred during data insertion, rollback the transaction.
                logger.error("DataDepositor - Rollback transaction.");
                conn.rollback();
            }
            
            conn.setAutoCommit(true);
            logger.debug("DataDepositor completed - Set auto-commit to ON.");
            // Release the finalization token.
            DBHelper.releaseFinalizeToken(userName);
            
            if (finalizeStatus) {
                String zipFile = Constants.getSYSTEM_PATH() + 
                                 Constants.getFINALIZE_PATH() + study_id + 
                                 Constants.getDETAIL_FILE_NAME() + 
                                 Constants.getZIPFILE_EXT();
                String[] srcFiles = new String[jobList.size()];
                int count = 0;
                // Update job status to finalized
                for (FinalizingJobEntry job : jobList) {
                    SubmittedJobDB.updateJobStatusToFinalized(job.getJob_id());
                    srcFiles[count++] = job.getDetail_output();
                }
                // Generate the summary report for this study.
                genPDFSummaryReport();
                // Update the summary filepath in the study table.
                StudyDB.updateStudySummaryReport(study_id, summaryReportPath);
                // Generate the consolidated output for this study.
                DataRetriever retrieverThread = new DataRetriever(study_id, userName);
                retrieverThread.start();
                // Proceed to zip the detail output files from all the selected
                // pipeline jobs.
                try {
                    FileHelper.zipFiles(zipFile, srcFiles);
                    logger.debug("Detail output for Study " + study_id + " zipped.");
                    StudyDB.updateDetailOutputFiles(study_id, zipFile);
                }
                catch (IOException e) {
                    logger.error("FAIL to zip detail output for Study " + study_id);
                    logger.error(e.getMessage());
                }
            }
            else {
                // Finalization failed. Revert all the submitted jobs and study
                // status.
                revertStudyStatus();
            }
        }
        catch (SQLException|NamingException|InterruptedException e) {
            logger.error("FAIL to insert finalized data!");
            logger.error(e.getMessage());
            // Finalization failed, release the token and revert all the
            // submitted jobs and study status.
            DBHelper.releaseFinalizeToken(userName);
            revertStudyStatus();
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
    }
    
    // Reindex the annotation index on data depository table after finalization.
    private boolean reindexDataDepositoryIndex() {
        boolean result = Constants.OK;
        String reindStr = "REINDEX INDEX deposit_annot_ind";
        
        try (PreparedStatement reindStm = conn.prepareStatement(reindStr)) {
            reindStm.execute();
        }
        catch (SQLException e) {
            logger.error("FAIL to reindex data depository table!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Revert submitted job(s) status back to completed, and study status back
    // to un-finalized if during finalization, some errors occurred. Send the
    // finalization status email to user.
    private void revertStudyStatus() {
        for (FinalizingJobEntry job : jobList) {
            SubmittedJobDB.updateJobStatusToCompleted(job.getJob_id());
        }
        // Revert study back to unfinalized.
        StudyDB.updateStudyFinalizedStatus(study_id, false);
        // Send finalization status (i.e. failed) email to the user.
        Postman.sendFinalizationStatusEmail(study_id, userName, Constants.NOT_OK);
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

    // Add a new page to the summary report and return the new content stream.
    private PDPageContentStream addNewPage() throws IOException {
        // Add a new page to the summary report.
        PDPage newPage = new PDPage(PDPage.PAGE_SIZE_A4);
        summary_pdf.addPage(newPage);
        // Shift the Y-axis to the starting position.
        pageCursorYaxis = getYaxisHeight() - 100;

        return new PDPageContentStream(summary_pdf, newPage);
    }
    
    // Return true if we have reached the end of the summary report page.
    private boolean checkEndOfPage() {
        return (pageCursorYaxis <= 70);
    }
    // Return true if the remaining page has enough space to hold the last 
    // section (i.e. last section is equal to 2 Header + 2 Line)
    private boolean enoughSpaceForLastSection() {
        return (pageCursorYaxis > (2 * 50 + 2 * 15));
    }
    
    // Get the Y-axis starting position.
    private float getYaxisHeight() {
        // rect can be used to get the page width and height.
        PDRectangle rect = page.getMediaBox();

        return rect.getHeight();
    }
    
    // Generate the PDF summary report for the finalization of study.
    private void genPDFSummaryReport() {
        // In the summary report, we print 3 subjects on every line.
        String[] foundSubjects = subjectFound.toString().split("\\$");
        String[] missingSubjects = subjectNotFound.toString().split("\\$");
        String[] geneInfo = geneAvailableVsStored.toString().split("\\$");
        String[] subjectInfo = subjectAvailableVsStored.toString().split("\\$");
        // x-axis position for each new subheader.
        float subheadX = 40;
        // x-axis position for each new content line.
        float lineX = 65;
        // Create some font objects by selecting one of the PDF base fonts.
        PDFont plainFont = PDType1Font.COURIER;
        PDFont boldFont = PDType1Font.COURIER_BOLD;
        PDFont italicFont = PDType1Font.COURIER_BOLD_OBLIQUE;

        try {
            int line = 0;
            // Add a new page and get the content stream to hold the created content.
            PDPageContentStream cs = addNewPage();
            // Create 2 image objects for Astar and BII logo.
            PDJpeg astarImg = new PDJpeg(summary_pdf, 
                    new FileInputStream(astarLogo));
            PDJpeg biiImg = new PDJpeg(summary_pdf, 
                    new FileInputStream(biiLogo));
            // Draw the Astar and BII logo.
            cs.drawImage(astarImg, subheadX, pageCursorYaxis);
            cs.drawImage(biiImg, subheadX+200, pageCursorYaxis);
            
            // Write the title in bold first.
            cs.beginText();
            cs.setFont(boldFont, 14);
            cs.moveTextPositionByAmount(subheadX, getNextHeaderYaxis());
            cs.drawString("Summary Report for " + study_id);
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
                                (String.valueOf(index)).append(". ");
                l1.append(ResourceRetriever.getMsg(job.getPipeline_name()));
                String l2 = "Parameters: " + job.getParameters();
                StringBuilder l3 = new StringBuilder
                                ("(Submitted by ").append(job.getUserName());
                l3.append(" @").append(job.getSubmitTimeString()).append(")");
                cs.beginText();
                cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
                cs.drawString(l1.toString());
                cs.endText();
                cs.beginText();
                cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
                cs.drawString(l2);
                cs.endText();
                cs.beginText();
                cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
                cs.drawString(l3.toString());
                cs.endText();
                cs.beginText();
                cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
                cs.drawString(subjectInfo[index-1]);
                cs.endText();
                cs.beginText();
                cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
                cs.drawString(geneInfo[index-1]);
                cs.endText();
                index++;
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
                // After printing each line, check whether we need another page.
                if (checkEndOfPage()) {
                    // Close the current content stream, and open another one.
                    cs.close();
                    cs = addNewPage();
                    cs.setFont(plainFont, 11);
                }
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
                // After printing each line, check whether we need another page.
                if (checkEndOfPage()) {
                    // Close the current content stream, and open another one.
                    cs.close();
                    cs = addNewPage();
                    cs.setFont(plainFont, 11);
                }
            }
            // Section D
            cs.beginText();
            cs.moveTextPositionByAmount(subheadX, getNextSubheaderYaxis());
            cs.drawString("D. HG19 Annotation Version");
            cs.endText();
            
            cs.beginText();
            cs.moveTextPositionByAmount(lineX, getNextLineYaxis());
            cs.drawString(annot_ver);
            cs.endText();            
            // Try to group the last section in one page, hence check in advance
            // whether we need another page.
            if (!enoughSpaceForLastSection()) {
                // Close the current content stream, and open another one.
                cs.close();
                cs = addNewPage();
                cs.setFont(plainFont, 11);
            }
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
            cs.drawString("Date: " + Constants.getStandardDT());
            cs.endText();
            
            // Application Signature
            cs.beginText();
            cs.setFont(italicFont, 12);
            cs.moveTextPositionByAmount(subheadX, getNextHeaderYaxis());
            cs.drawString("Generated by TIMS\u00a9");
            cs.endText();
            
            // Close the content stream.
            cs.close();
            // Save the result, and close the document.
            summary_pdf.save(summaryReportPath);
            summary_pdf.close();
        } 
        catch (IOException|COSVisitorException ex) {
            logger.error("FAIL to create summary report!");
            logger.error(ex.getMessage());
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
    
    // Insert a record into finalized_record table using the PreparedStatement
    // passed in.
    private void insertToFinalizedOutput(PreparedStatement stm, 
            FinalizedRecord record) throws SQLException {
        stm.setInt(1, record.getArray_index());
        stm.setString(2, record.getAnnot_ver());
        stm.setInt(3, record.getJob_id());
        stm.setString(4, record.getSubject_id());
        stm.setString(5, record.getStudy_id());
        stm.executeUpdate();
            
        logger.debug("Output for " + record.getSubject_id() + 
                     " stored at index: " + record.getArray_index());        
    }
    
    // Return the next array index to be use in finalized_record table.
    private int getNextArrayInd() {
        int count = Constants.DATABASE_INVALID_ID;
        String query = "SELECT MAX(array_index) FROM finalized_record "
                     + "WHERE annot_ver = \'" + annot_ver + "\'";
        
        try (PreparedStatement stm = conn.prepareStatement(query);
             ResultSet rs = stm.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1) + 1;
            }
        }
        catch (SQLException e) {
            logger.debug("FAIL to retrieve the next array index!");
            logger.debug(e.getMessage());
        }
        
        return count;
    }
    
    // Check whether genename exists using the PreparedStatement passed in.
    private boolean checkGeneExistInDB(PreparedStatement stm, 
            String genename) throws SQLException {
        stm.setString(1, genename);
        ResultSet rs = stm.executeQuery();
        
        return rs.isBeforeFirst()?Constants.OK:Constants.NOT_OK;
    }
    
    // Process the first line (i.e. subject line) of pipeline data file. Return
    // the processing status.
    private boolean procSubjectLine(String[] values) {
        boolean result = Constants.OK;
        // INSERT statement to insert a record into finalized_record table.
        String insertStr = "INSERT INTO finalized_record(array_index,"
                         + "annot_ver,job_id,subject_id,study_id) "
                         + "VALUES(?,?,?,?,?)";
            
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            // Ignore the first two strings (i.e. geneID and EntrezID).
            for (int i = 2; i < values.length; i++) {
                // Only store the pipeline data if the study record is 
                // available in the database.
                if (SubjectRecordDB.isSRExist(values[i], study_id)) {
                    if (!subjectFound.toString().contains(values[i])) {
                        // Only want to store the unqiue subject ID that
                        // have meta data in database.
                        subjectFound.append(values[i]).append(" ");
                        numSubjectFound++;
                        // At the end of each subject line, place a marker '$'
                        if (numSubjectFound%SUBJECTS_PER_LINE == 0) {
                            subjectFound.append("$");
                        }
                    }
                    processedRecord++;
                    arrayIndex[i] = getNextArrayInd();
                    FinalizedRecord record = new FinalizedRecord
                        (arrayIndex[i], annot_ver, values[i], job_id, study_id);
                    // Insert the finalized output record.
                    insertToFinalizedOutput(insertStm, record);
                }
                else {
                    if (!subjectNotFound.toString().contains(values[i])) {
                        // Only want to store the unqiue subject ID that
                        // do not have meta data in database.
                        subjectNotFound.append(values[i]).append(" ");
                        numSubjectNotFound++;
                        // At the end of each subject line, place a marker '$'
                        if (numSubjectNotFound%SUBJECTS_PER_LINE == 0) {
                            subjectNotFound.append("$");
                        }
                    }
                    arrayIndex[i] = Constants.DATABASE_INVALID_ID;
                }
            }
            // Print process status.
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
            logger.error("FAIL to create finalized records!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }

        return result;
    }
    
    // Process the gene data of pipeline output. Return the processing status.
    private boolean procGeneData(BufferedReader br) throws IOException {
        boolean result = Constants.OK;
        String genename, lineRead;
        String[] values;
        totalGene = processedGene = 0;
        // UPDATE statement to update the data array in data_depository table.
        String updateStr = "UPDATE data_depository SET data[?] = ? WHERE " 
                         + "genename = ? AND annot_ver = \'" 
                         + annot_ver + "\'";
        // SELECT statement to check the existence of gene in database.
        String queryGene = "SELECT 1 FROM data_depository "
                         + "WHERE genename = ? AND annot_ver = \'" 
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
                // Check whether genename exist in data_depository.
                if (checkGeneExistInDB(queryGeneStm,genename)) {
                    processedGene++;
                    // Gene data start from the 3rd column. 
                    for (int i = 2; i < values.length; i++) {
                        // Only process those data with valid PID
                        if (arrayIndex[i] != Constants.DATABASE_INVALID_ID) {
                            // Insert gene value into data array.
                            insertToDataArray(updateStm, arrayIndex[i],
                                              values[i],genename);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("FAIL to insert gene data into data array!");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Insert the finalized pipeline output into database.
    private boolean insertFinalizedDataIntoDB() throws SQLException {
        boolean result = Constants.OK;
        String[] values;
        // Reset global variables before start processing.
        totalRecord = processedRecord = 0;
        arrayIndex = null;
        // To record the time taken to insert the processed data
        long startTime, elapsedTime;

        try(BufferedReader br = new BufferedReader(new FileReader(fileUri))) {
            String lineRead = br.readLine();
            values = lineRead.split("\t");
            // Update the size of the integer array.
            arrayIndex = new int[values.length];
            // No of subjects = total column - 2
            totalRecord = values.length - 2;
            // Start subject line processing.
            if (!procSubjectLine(values)) {
                // Error occurred, return to caller.
                return Constants.NOT_OK;
            }
            // Record the subject record available versus stored information.
            subjectAvailableVsStored.append("Subject record available: ").append(totalRecord);
            subjectAvailableVsStored.append(", Subject record stored: ").append(processedRecord).append("$");
            // Only proceed with gene data processing if subject ID is found in
            // the database.
            if (processedRecord > 0) {
                startTime = System.nanoTime();
                // Start gene data processing.
                if (procGeneData(br)) {
                    // Record the time taken for the insertion.
                    elapsedTime = System.nanoTime() - startTime;
                    // Record the gene available versus stored information.
                    geneAvailableVsStored.append("Gene data available: ").append(totalGene);
                    geneAvailableVsStored.append(", Gene data stored: ").append(processedGene).append("$");
                    logger.debug("Gene record processed: " + processedGene + "/" + totalGene);
                    logger.debug("Time taken: " + (elapsedTime / 1000000000.0) + " sec");
                }
                else {
                    // Error occurred, return to caller.
                    return Constants.NOT_OK;
                }                
            }
            else {
                geneAvailableVsStored.append("Subject ID not found. Gene data will not be stored into database").append("$");
                logger.debug("None of the subject ID for this pipeline output is found. "
                        + "Gene data will not be stored into database.");
            }
        }
        catch (IOException ioe) {
            logger.error("FAIL to read pipeline output file!");
            logger.error(ioe.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
}