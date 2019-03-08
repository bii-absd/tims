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
import TIMS.General.ResourceRetriever;
// Libraries for Java
import java.io.BufferedReader;
import java.io.File;
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

public class DataDepositor extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DataDepositor.class.getName());
    private Connection conn = null;
    private final String study_id, annot_ver, summaryReportPath;
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
        // Retrieve the value of annot_ver from study table.
        Study study = StudyDB.getStudyObject(study_id);
        annot_ver = study.getAnnot_ver();
        summaryReportPath = Constants.getSYSTEM_PATH() + 
                            Constants.getSTUDIES_PATH() + 
                            study_id + File.separator + 
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
                                 Constants.getSTUDIES_PATH() + 
                                 study_id + File.separator +
                                 "finalized_detail" + 
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