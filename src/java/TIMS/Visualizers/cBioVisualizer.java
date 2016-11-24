/*
 * Copyright @2016
 */
package TIMS.Visualizers;

import TIMS.Bean.FileUploadBean;
import TIMS.Database.FinalizingJobEntry;
import TIMS.Database.PipelineDB;
import TIMS.Database.Study;
import TIMS.Database.StudyDB;
import TIMS.Database.SubmittedJobDB;
import TIMS.General.Constants;
import TIMS.General.FileHelper;
import TIMS.General.Postman;
import TIMS.General.Statistics;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.math.RoundingMode;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for password hashing
import org.mindrot.jbcrypt.BCrypt;

/**
 * cBioVisualizer export the pipeline data from the selected job(s) to 
 * cBioPortal database.
 * 
 * Author: Tay Wei Hong
 * Date: 22-Jun-2016
 * 
 * Revision History
 * 04-Jul-2015 - Implemented the integration with cBioPortal application.
 * 10-Aug-2016 - Fixed the following bugs reported in UAT:
 * 1. The study failed to get created at cBioPortal if the study description 
 * is more than 1024 characters.
 * 2. CNV column offset should be 2 instead of 4.
 * 3. Only one thread can restart the tomcat application server at one time.
 * 4. The stable_id in the meta file need to end in pre-defined code.
 * 5. To convert the data values from Affymetrix pipeline to z-score format.
 * 12-Aug-2016 - To make sure the Affymetrix z-score result is exactly 2 
 * decimal places.
 * 30-Aug-2016 - Enhanced method recordVisualTime(), to call the helper function
 * in Constants class.
 * 01-Sep-2016 - Changes due to change in method name in FinalizingJobEntry 
 * class.
 * 27-Sep-2016 - Minor update to method createCbioUrl(), due to changes in the
 * IP address of TIMS server.
 * 07-Oct-2016 - Minor update to method createCbioUrl(), due to changes in the
 * IP address of TIMS server.
 * 23-Nov-2016 - To read in the system environment variables for tomcat and 
 * cbioportal. To retrieve the cbioportal url from system config file.
 */

public class cBioVisualizer extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(cBioVisualizer.class.getName());
    // Only one thread can restart the tomcat application server at one time.
    private static Semaphore tcCtrl = new Semaphore(1);
    // Use for formating the z-score to 2 decimals.
    private static DecimalFormat df = new DecimalFormat("0.00");
    // Commands for cBioPortal.
    private final static String IMPORT_STUDY = "import-study";
    private final static String REMOVE_STUDY = "remove-study";
    private final static String IMPORT_CASE_LIST = "import-case-list";
    private final static String IMPORT_DATA = "import-study-data";
    private final static String META_FILENAME = "--meta-filename";
    private final static String DATA_FILENAME = "--data-filename";
    // These strings are used to store the meta data parameters.
    private String alteration_type, datatype, profile_desc, profile_name;
    private final List<FinalizingJobEntry> selectedJobs;
    private final String userName, studyID;
    private final Study study;
    private String folder_name, dir, case_dir, meta_study_txt;
    private Timestamp visual_time;
    // cBioPortal import command.
    private List<String> CMD = new ArrayList<>();
    // To store the subjects ID from all the pipeline output.
    StringBuilder casesAllList = new StringBuilder();
    
    // Commands to stop and start tomcat server.
    // To be moved later!
    private List<String> TCSTOP = new ArrayList<>();
    private List<String> TCSTART = new ArrayList<>();

    public cBioVisualizer(String userName, String study_id, 
            List<FinalizingJobEntry> selectedJobs) 
    {
        df.setRoundingMode(RoundingMode.CEILING);
        this.studyID = study_id;
        this.userName = userName;
        this.selectedJobs = selectedJobs;
        this.study = StudyDB.getStudyObject(study_id);
        dir = Constants.getSYSTEM_PATH() + Constants.getCBIO_PATH()
            + study_id + File.separator;
        // Define the file path of the meta_study.txt file
        meta_study_txt = dir + "meta_study.txt";
        // Record the time this export job is requested, and used the return
        // string as the name of the folder used for storing the meta files.
        folder_name = recordVisualTime();
        dir += folder_name + File.separator;
        case_dir = dir + Constants.getCBIO_CASE_DIR();
        // Save the visual time into database.
        StudyDB.updateStudyVisualTime(study_id, visual_time);
        // Build the cBioPortal import command.
        // Addition parameter needed for Window OS.
        if (System.getProperty("os.name").startsWith("Windows")) {
            CMD.add("python");
        }
        
        CMD.add(System.getenv("PORTAL_HOME") + File.separator + "cbioportalImporter.py");
        CMD.add("--command");
        logger.debug("cBioPortal Import command: " + CMD.toString());
        
        // Create tomcat commands.
        createTomcatCommands();
        logger.debug("cBioVisualizer created for study: " + study_id);
    }
    
    @Override
    public void run() {
        boolean status = Constants.OK;
        String logFileName;
        // Create the directory and sub-directory for storing the meta files
        // and the case files.
        if (!(FileUploadBean.createSystemDirectory(dir) && 
              FileUploadBean.createSystemDirectory(case_dir))) {
            // Fail to create system directory for cBioPortal, no point to continue.
            logger.error("FAIL to create system directory for cBioPortal!");
            logger.error("Aborting data export for study " + studyID);
            return;
        }
        
        // Check whether has this study been exported for visualization before.
        if (StudyDB.getCbioURL(studyID) == null) {
            // Create the meta_study file.
            createMetaStudyFile();
        }
        else {
            List<String> removeStudyCMD = remStudyCommand();
            // Prepare log file.
            logFileName = createLogFile(dir, "rem_" + studyID);
            // Remove the study.
            if (executeImportScript(removeStudyCMD, logFileName)) {
                logger.debug("Study from last import removed.");
            }
            else {
                // Failed to remove previous study.
                status = Constants.NOT_OK;
            }
        }
        
        List<String> addStudyCMD = addStudyCommand();
        // Prepare log file.
        logFileName = createLogFile(dir, "add_" + studyID);
        // Import the study.
        if (executeImportScript(addStudyCMD, logFileName)) {
            logger.debug("Imported study.");
        }
        else {
            // Failed to import the study, no point to continue.
            logger.error("FAIL to import study in cBioPortal!");
            logger.error("Aborting data export for study " + studyID);
            return;
        }
        
        // For each job, need to create the meta_data and case list files, 
        // unzip the pipeline data to the tmp directory, and run the cBioPortal 
        // script to import pipeline data.
        for (FinalizingJobEntry job : selectedJobs) {
            // Unzip the pipeline data file to the tmp directory; this file 
            // will be deleted after use.
            String data_file = SubmittedJobDB.unzipOutputFile(job.getJob_id());
            // Store the list of subject IDs; to be use in the case_list.
            StringBuilder case_list_ids = new StringBuilder();
            // To store the code needed at the end of each stable ID.
            String stableID_code = null;
            
            // Setup the meta file parameters for each pipeline.
            switch (job.getPipeline_name()) {
                case PipelineDB.GEX_AFFYMETRIX:
                    alteration_type = "MRNA_EXPRESSION";
                    datatype = "Z-SCORE";
                    profile_desc = "Expression levels (Affymetrix microarray)";
                    profile_name = "mRNA expression (Affymetrix microarray)";
                    stableID_code = studyID + "_affy_mrna";
                    case_list_ids = createSubjectsList(data_file, 2);
                    // Convert pipeline output to z-score format.
                    convert2zScoreFile(data_file, "affymetrix");
                    break;
                case PipelineDB.GEX_ILLUMINA:
                    alteration_type = "MRNA_EXPRESSION";
                    datatype = "Z-SCORE";
                    profile_desc = "Expression levels (Illumina microarray)";
                    profile_name = "mRNA expression (Illumina microarray)";
                    stableID_code = studyID + "_illu_mrna";
                    case_list_ids = createSubjectsList(data_file, 2);
                    break;
                case PipelineDB.METHYLATION:
                    alteration_type = "METHYLATION";
                    datatype = "CONTINUOUS";
                    profile_desc = "Methylation beta-values";
                    profile_name = "Methylation";
                    stableID_code = studyID + "_methylation";
                    case_list_ids = createSubjectsList(data_file, 2);
                    break;
                case PipelineDB.CNV:
                    alteration_type = "COPY_NUMBER_ALTERATION";
                    datatype = "DISCRETE";
                    profile_desc = "Putative copy-number from GISTIC 2.0. "
                                 + "Values: -2 = homozygous deletion; "
                                 + "-1 = hemizygous deletion; "
                                 + "0 = neutral|no change; 1 = gain; "
                                 + "2 = high level amplification.";
                    profile_name = "Putative copy-number alterations from GISTIC";
                    stableID_code = studyID + "_gistic";
                    case_list_ids = createSubjectsList(data_file, 2);
                    break;
                default:
                    // Unlikely for control to reach here.
                    logger.error("Received invalid pipeline job during exporting of data: " 
                            + job.getPipeline_name());
                    break;
            }
            // Create the meta file and case list file for each pipeline.
            String meta_file = createMetaPLFile(job.getPipeline_name(), stableID_code);
            String case_file = createCaseListFile(job.getPipeline_name(), 
                                job.getInput_desc(), case_list_ids, stableID_code);
            
            logger.debug("Meta file created: " + meta_file);
            logger.debug("Case list file created: " + case_file);
            List<String> importDataCMD = importDataCommand(meta_file, data_file);
            // Prepare log file.
            logFileName = createLogFile(dir, job.getPipeline_name());
            // Import pipeline data.
            if (executeImportScript(importDataCMD, logFileName)) {
                logger.debug("Data imported for pipeline " + job.getPipeline_name());
            }
            else {
                status = Constants.NOT_OK;
                logger.error("FAIL to import data for pipeline " + job.getPipeline_name());
            }
            
            try {
                // Added the below statement due to a bug in Java that prevent the
                // temporary output file for being deleted.
                System.gc();
                // Sleep for 1 sec before deleting the temporary file.
                sleep(1000);
                // Delete the temporary file here.
                if (!FileHelper.delete(data_file)) {
                    logger.error("FAIL to delete the temporary data file!");
                }
            }
            catch (InterruptedException ie) {
                logger.error("FAIL to sleep before deleting temp working file!");
                logger.error(ie.getMessage());
            }
        }
        // Create the case_all file for this study.
        createCasesAllFile();
        List<String> importCaseListCMD = importCaseListCommand();
        // Prepare log file.
        logFileName = createLogFile(dir, "case_lists");
        // Import the case list for all pipeline.
        if (executeImportScript(importCaseListCMD, logFileName)) {
            logger.debug("Imported case list.");
        }
        else {
            status = Constants.NOT_OK;
            logger.error("FAIL to import case list for study " + studyID);
        }

        if (status) {
            // Create and save the cBioPortal URL into database.
            StudyDB.updateStudyCbioUrl(studyID, createCbioUrl());
            // Export completed!
            logger.debug(studyID + " exported to cBioPortal.");
        }
        else {
            logger.error("FAIL to export " + studyID + " to cBioPortal!");
        }
        
        // Used the executeImportScript to run the commands to restart the 
        // tomcat server.
        try {
            logger.debug(userName + ": trying to acquire tomcat control.");
            tcCtrl.acquire();
            logger.debug(userName + ": restarting tomcat application server.");
            executeImportScript(TCSTOP, createLogFile(dir, "stop_tomcat"));
            executeImportScript(TCSTART, createLogFile(dir, "start_tomcat"));
            logger.debug(userName + ": tomcat application server restarted.");
            // Wait a minute before releasing the control key i.e. giving
            // the tomcat time to complete it's reset cycle.
            sleep(60000);
        }
        catch (InterruptedException ie) {
            logger.error("FAIL to acquire tomcat control!");
            logger.error(ie.getMessage());
        }
        finally {
            tcCtrl.release();
        }
        
        // Send notification email to usr.
        Postman.sendExportDataStatusEmail(studyID, userName, status);
    }
    
    // Convert the content of the datafile to z-score value.
    private String convert2zScoreFile(String datafile, String plName) {
        String zScoreFile = dir + plName + "_zscore.txt";
        
        try (PrintStream ps = new PrintStream(new File(zScoreFile));
             BufferedReader br = new BufferedReader(new FileReader(datafile)))
        {
            // No processing needed for file header; just read and write.
            String lineRead = br.readLine();
            ps.println(lineRead);
            // Process the remaining gene data.
            while ((lineRead = br.readLine()) != null) {
                String[] values = lineRead.split("\t");
                // Create the DescriptiveStatistics object using the values 
                // found in the datafile; ignoring the first 2 columns.
                DescriptiveStatistics stats = Statistics.createStatsInstance(values, 2);
                double mean = stats.getMean();
                double sd = stats.getStandardDeviation();
                // Process the converted data line by line.
                StringBuilder zsLine = new StringBuilder();
                zsLine.append(values[0]).append("\t").append(values[1]).append("\t");
                // Convert the data (one at a time) into it's z-score value.
                for (int i = 2; i < values.length; i++) {
                    // If standard deviation is zero, z-score is undefined; 
                    // will store as 0.0 here.
                    if (sd == 0.0) {
                        zsLine.append("0.00").append("\t");
                    }
                    else {
                        Double zs = Statistics.zScore(Double.parseDouble(values[i]), mean, sd);
                        zsLine.append(df.format(zs)).append("\t");                        
                    }
                }
                // Write the converted data into the z-score file.
                ps.println(zsLine);
            }
            logger.debug("z-score file created at: " + zScoreFile);
        }
        catch (IOException ioe) {
            logger.error("FAIl to convert " + plName + " data file to z-score format!");
            logger.error(ioe.getMessage());
        }

        // Copy and replace the datafile with the converted z-score file.
        try {
            Path from = FileSystems.getDefault().getPath(zScoreFile);
            Path to = FileSystems.getDefault().getPath(datafile);
            Files.copy(from, to, REPLACE_EXISTING);
            logger.debug("z-score file copied to temp directory.");
        }
        catch (IOException ioe) {
            logger.error("FAIl to copy z-score file to temp directory!");
            logger.error(ioe.getMessage());
        }

        // Return the path to the converted z-score file.
        return zScoreFile;
    }
    
    // Create the commands to stop and start tomcat server.
    private void createTomcatCommands() {
        String tcCommand;
        
        // Window Version
        if (System.getProperty("os.name").startsWith("Windows")) {
            tcCommand = System.getenv("CATALINA_HOME") + File.separator + "bin" + File.separator + "catalina.bat";
        }
        else {
            tcCommand = System.getenv("CATALINA_HOME") + File.separator + "bin" + File.separator + "catalina.sh";
        }
        
        TCSTOP.add(tcCommand);
        TCSTOP.add("stop");
        TCSTART.add(tcCommand);
        TCSTART.add("start");
        logger.debug("Tomcat START command: " + TCSTART.toString());
        logger.debug("Tomcat STOP command: " + TCSTOP.toString());
    }
    
    // Construct and return the remove study command for cBioPortal.
    private List<String> remStudyCommand() {
        // In order to have a new copy of the List object, we need to perform 
        // a 'deep-copy' of the List object here i.e. purely assignment will 
        // not work.
        List<String> command = new ArrayList<>(CMD);
        // Build the command to remove the study from last import.
        command.add(REMOVE_STUDY);
        command.add(META_FILENAME);
        command.add(meta_study_txt);
    
        return command;
    }
    
    // Construct and return the add study command for cBioPortal.
    private List<String> addStudyCommand() {
        List<String> command = new ArrayList<>(CMD);
        // Build the command to import the study for this import.
        command.add(IMPORT_STUDY);
        command.add(META_FILENAME);
        command.add(meta_study_txt);
        
        return command;
    }

    // Construct and return the import data command for cBioPortal.
    private List<String> importDataCommand(String meta_file, String data_file) {
        List<String> command = new ArrayList<>(CMD);
        // Build the command to import pipeline data.
        command.add(IMPORT_DATA);
        command.add(META_FILENAME);
        command.add(meta_file);
        command.add(DATA_FILENAME);
        command.add(data_file);

        return command;
    }
    
    // Construct and return the import case lists command for cBioportal.
    private List<String> importCaseListCommand() {
        List<String> command = new ArrayList<>(CMD);
        // Build the command to import the case list for all the pipeline.
        command.add(IMPORT_CASE_LIST);
        command.add(META_FILENAME);
        command.add(case_dir);

        return command;
    }
    
    // Helper function to create the cBioPortal URL for this study.
    private String createCbioUrl() {
        // Create a random 60 characters string.
        int dummyNum = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE/2, Integer.MAX_VALUE);
        String dummyStr = BCrypt.hashpw(String.valueOf(dummyNum), BCrypt.gensalt());

        // Need to replace the special character '/' as this string will form
        // part of an url.
        dummyStr = dummyStr.replace("/", "0");
        String target = dummyStr.substring(0, 30) + studyID.substring(0, 5) 
                      + dummyStr.substring(30) + studyID.substring(5);

        if (System.getProperty("os.name").startsWith("Windows")) {
            return "http://localhost:8080/cbioportal/?sid=" + target;
        }
        else {
            return Constants.getCBIOPORTAL_URL() + "?sid=" + target;
        }
    }
    
    // Helper function to create the log file.
    private String createLogFile(String dir, String filename) {
        return dir + Constants.getLOGFILE_NAME() + 
               filename + Constants.getLOGFILE_EXT();
    }
    
    // Execute the cBioPortal Python script to import study meta data into
    // cBioPortal database.
    private boolean executeImportScript(List<String> command, String logFileName) {
        boolean status = Constants.NOT_OK;
        ProcessBuilder pb = new ProcessBuilder(command);
        // The execution log from the cBioPortal script will be written to 
        // the log file.
        File logFile = new File(logFileName);
        // Merge the standard error and output stream, and always sent to the
        // same destination.
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(logFile));
        logger.debug("Executing cBioPortal command: " + command);
        
        try {
            // Start the import.
            Process process = pb.start();
            // Wait for the import to finish.
            int result = process.waitFor();
            if (result == 0) {
                // Any result other than 0 is considered not ok.
                status = Constants.OK;
            }
            logger.debug("cBioPortal import completed with status: " + result);
        }
        catch (IOException ioe) {
            logger.error("FAIL to start the CBioPortal import script!");
            logger.error(ioe.getMessage());
        }
        catch (InterruptedException ie) {
            logger.error("FAIL to complete the import of data into cBioPortal!");
            logger.error(ie.getMessage());            
        }
        
        return status;
    }
    
    // Lock down and record the time when this job started started. Return a
    // string to be used as the name of the folder for storing the meta
    // files.
    private String recordVisualTime() {
        Date now = new Date();
        // visual_time will be stored in the study table.
        visual_time = new Timestamp(now.getTime());

        return Constants.getDT_yyyyMMdd_HHmm();
    }
    
    // Trim the target string (if needed) to less than or equal to length, and
    // return the trimmed string. 
    private String trimString(String target, int len) {
        return target.substring(0, Math.min(target.length(), len-1));
    }
    
    // Create the meta_study file for this study; only need to create once per
    // study. Return the absolute path of the meta_study file.
    private String createMetaStudyFile() {
        String result = Constants.FAILED;
        File meta_study = new File(meta_study_txt);
        
        try (FileWriter fw = new FileWriter(meta_study)) {
            // Create meta_study file.
            meta_study.createNewFile();
            // Write to the meta_study file according to the format needed by
            // cbioportalImporter Python script.
            fw.write("type_of_cancer: " + study.getIcd_code() + "\n");
            fw.write("cancer_study_identifier: " + studyID + "\n");
            // Need to trim the title to be less than or equal to 255 characters.
            fw.write("name: " + trimString(study.getTitle(), 255) + "\n");
            fw.write("short_name: " + studyID + "\n");
            // Need to trim the description to less than or equal to 1024 characters.
            fw.write("description: " + trimString(study.getDescription(), 1024) + "\n");
            fw.write("groups: ");
            // Update the result with the absolute path of the meta_study file.
            result = meta_study.getAbsolutePath();
        }
        catch (IOException ioe) {
            logger.error("FAIL to create meta_study file!");
            logger.error(ioe.getMessage());
        }

        return result;
    }
    
    // Create the meta file for each pipeline. Return the absolute path of the
    // meta file.
    private String createMetaPLFile(String pipeline, String stableIDCode) {
        String result = Constants.FAILED;
        File meta_file = new File(dir + "meta_" + pipeline + ".txt");
        
        try (FileWriter fw = new FileWriter(meta_file)) {
            meta_file.createNewFile();
            // Write to the meta file according to the format needed by
            // cbioportalImporter Python script.
            fw.write("cancer_study_identifier: " + studyID + "\n");
            fw.write("genetic_alteration_type: " + alteration_type + "\n");
            fw.write("datatype: " + datatype + "\n");
            fw.write("stable_id: " + stableIDCode + "\n");
            fw.write("show_profile_in_analysis_tab: true\n");
            fw.write("profile_description: " + profile_desc + "\n");
            fw.write("profile_name: " + profile_name + "\n");
            // Update the result with the absolute path of the meta file.
            result = meta_file.getAbsolutePath();
        }
        catch (IOException ioe) {
            logger.error("FAIL to create meta file for pipeline " + pipeline);
            logger.error(ioe.getMessage());
        }

        return result;
    }
    
    // Create the cases_all file. Return the absolute path of the cases_all file.
    private String createCasesAllFile() {
        String result = Constants.FAILED;
        File casesAllFile = new File(case_dir + "cases_all.txt");
        
        try (FileWriter fw = new FileWriter(casesAllFile)) {
            casesAllFile.createNewFile();
            // Write to the all case list file according to the format needed by
            // cbioportalImporter Python script.
            fw.write("cancer_study_identifier: " + studyID + "\n");
            fw.write("stable_id: " + studyID + "_all\n");
            fw.write("case_list_name: All Cases\n");
            fw.write("case_list_description: All Cases\n");
            fw.write("case_list_ids: " + casesAllList.toString() + "\n");
            // Update the result with the absolute path of the case list file.
            result = casesAllFile.getAbsolutePath();
        }
        catch (IOException ioe) {
            logger.error("FAIL to create cases_all file!");
            logger.error(ioe.getMessage());
        }
        
        return result;
    }
    
    // Create the case list file for each pipeline. Return the absolute path
    // of the case list file.
    private String createCaseListFile(String pipeline, String description, 
            StringBuilder case_list, String stableIDCode) {
        String result = Constants.FAILED;
        File case_file = new File(case_dir + "cases_" + pipeline + ".txt");
        
        try (FileWriter fw = new FileWriter(case_file)) {
            case_file.createNewFile();
            // Write to the case list file according to the format needed by
            // cbioportalImporter Python script.
            fw.write("cancer_study_identifier: " + studyID + "\n");
            fw.write("stable_id: " + stableIDCode + "\n");
            fw.write("case_list_name: " + studyID + " " + pipeline + " Cases\n");
            fw.write("case_list_description: " + description + "\n");
            fw.write("case_list_ids: " + case_list.toString() + "\n");
            // Update the result with the absolute path of the case list file.
            result = case_file.getAbsolutePath();
        }
        catch (IOException e) {
            logger.error("FAIL to create case list file for pipeline " + pipeline);
        }
        
        return result;
    }
    
    // Create the list of subject IDs from the first line of pipeline output; 
    // to be use in the case_list. Parameter offset will tell how many columns
    // to ignore.
    private StringBuilder createSubjectsList(String filename, int offset) {
        StringBuilder subjectsList = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String header = br.readLine();
            String[] subjectID = header.split("\t");
            for (int i = offset; i < subjectID.length; i++) {
                subjectsList.append(subjectID[i]).append("\t");
                // Only store those unique subjects ID in the all case list.
                if (!casesAllList.toString().contains(subjectID[i])) {
                    casesAllList.append(subjectID[i]).append("\t");
                }
            }
        }
        catch (IOException e) {
            logger.error("FAIL to create subject list!");
            logger.error(e.getMessage());
        }
        
        return subjectsList;
    }
}
