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
package TIMS.Visualizers;

import TIMS.Bean.FileUploadBean;
import TIMS.Database.FinalizingJobEntry;
import TIMS.Database.PipelineDB;
import TIMS.Database.Study;
import TIMS.Database.StudyDB;
import TIMS.Database.SubmittedJobDB;
import TIMS.Database.SystemParametersDB;
import TIMS.General.Constants;
import TIMS.General.FileHelper;
import TIMS.General.Postman;
import TIMS.General.ResourceRetriever;
import TIMS.General.Statistics;
// Libraries for Java
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for password hashing
import org.mindrot.jbcrypt.BCrypt;

public class cBioVisualizer extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(cBioVisualizer.class.getName());
    // Only one thread can restart the tomcat application server at one time.
    private static Semaphore tcCtrl = new Semaphore(1);
    // Use for formating the z-score to 2 decimals.
    private static DecimalFormat df = new DecimalFormat("0.00");
    // These strings are used to store the meta data parameters.
    private String alteration_type, datatype, profile_desc, profile_name;
    private final List<FinalizingJobEntry> selectedJobs;
    private final String userName, studyID;
    private final Study study;
    private String folder_name, dir, case_dir, meta_study_txt, 
                   meta_clinical_samples, meta_cancer_type, 
                   data_clinical_samples, cancer_type;
    // Color code for the different cancer type in cBioPortal.
    public final String[] color_code = new String[] 
        {"White","PeachPuff","Red","Gray","Green","LightSkyBlue","HotPink",
         "SaddleBrown","MediumSeaGreen","LightBlue","Black","Yellow",
         "LightYellow","LimeGreen","Purple","LightSalmon","Teal","Cyan",
         "Gainsboro","DarkRed","Orange","Blue"};
    private Timestamp visual_time;
    // cBioPortal import command.
    private List<String> CMD = new ArrayList<>();
    // To store the subjects ID from all the pipeline output.
    Set<String> casesAllList = new HashSet<String>();
    // Commands to stop and start the cBioPortal application.
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
        // Record the time this export job is requested, and used the return
        // string as the name of the folder used for storing the meta files.
        folder_name = recordVisualTime();
        dir = Constants.getSYSTEM_PATH() + Constants.getCBIO_PATH()
            + study_id + File.separator + folder_name + File.separator;
        // Define the file path for all the meta and data files.
        meta_study_txt = dir + "meta_study.txt";
        meta_clinical_samples = dir + "meta_clinical_samples.txt";
        data_clinical_samples = dir + "data_clinical_samples.txt";
        meta_cancer_type = dir + "meta_cancer_type.txt";
        cancer_type = dir + "cancer_type.txt";
        case_dir = dir + Constants.getCBIO_CASE_DIR();
        // Build the cBioPortal import command.
        // Addition parameter needed for Window OS.
        if (System.getProperty("os.name").startsWith("Windows")) {
            CMD.add("python");
        }
        CMD.add(System.getenv("PORTAL_HOME") + File.separator + "metaImport.py");
        CMD.add("-s");
        logger.debug("cBioPortal Meta Import command: " + CMD.toString());
        
        // Create tomcat commands to stop and start the cBioPortal application.
        TCSTOP.add("curl");
        TCSTOP.add("--user");
        // Retrieve the Tomcat user id and password.
        String tomcat = SystemParametersDB.getTomcatUID() + ":" 
                      + SystemParametersDB.getTomcatPWD();
        TCSTOP.add(tomcat);
        TCSTART.add("curl");
        TCSTART.add("--user");
        TCSTART.add(tomcat);
        createTomcatCommands();
        logger.debug("cBioVisualizer created for study: " + study_id);
    }
    
    @Override
    public void run() {
        // To store the list of pipeline data files; files to be deleted after 
        // export.
        StringBuilder datafiles = new StringBuilder();
        // Create the directory and sub-directory for storing the meta files
        // and the case files.
        if (!(FileUploadBean.createSystemDirectory(dir) && 
              FileUploadBean.createSystemDirectory(case_dir))) {
            // Fail to create system directories for cBioPortal, no point 
            // to continue.
            logger.error("FAIL to create system directories for cBioPortal!");
            logger.error("Aborting data export for study " + studyID);
            // Send the failed notification email to user.
            Postman.sendExportDataStatusEmail(studyID, userName, 
                                              Constants.NOT_OK);
            return;
        }

        // Create the meta files for study, cancer type, and clinical samples.
        createMetaStudyFile();
        createMetaCancerTypeFile();
        createMetaClinicalSamplesFile();
        // For each job, need to create the meta and case list files, unzip the
        // pipeline data to the tmp directory, and store it's absolute path in
        // the meta file.
        for (FinalizingJobEntry job : selectedJobs) {
            // Unzip the pipeline data file to the tmp directory; this file 
            // will be deleted after use.
            String data_file = SubmittedJobDB.unzipOutputFile(job.getJob_id());
            // Store the list of subject IDs; to be use in the case_list.
            Set<String> case_list_ids = new HashSet<String>();
            // To store the stable ID.
            String stable_id = null;
            
            // Fill in the meta file parameters for each pipeline.
            switch (job.getPipeline_name()) {
                // For mutation pipelines, the stable ID needs to end in 
                // "mutations" or else the mutation tab will be missing at
                // cBioPortal.
                case PipelineDB.GATK_TAR_GERM:
                case PipelineDB.GATK_TAR_SOMA:
                case PipelineDB.GATK_WG_GERM:
                case PipelineDB.GATK_WG_SOMA:
                    alteration_type = "MUTATION_EXTENDED";
                    datatype = "MAF";
                    profile_name = "Mutations";
                    // The subject ID is at the 10th columns for mutation 
                    // pipeline output.
                    case_list_ids = createSubjectsListForMAF(data_file, 9);
                    stable_id = "mutations";
                    if (job.getPipeline_name().equals(PipelineDB.GATK_TAR_GERM)) {
                        profile_desc = "Mutation data from GATK Targeted Germline Sequencing";
                    }
                    else if (job.getPipeline_name().equals(PipelineDB.GATK_TAR_SOMA)) {
                        profile_desc = "Mutation data from GATK Targeted Somatic Sequencing";
                    }
                    else if (job.getPipeline_name().equals(PipelineDB.GATK_WG_GERM)) {
                        profile_desc = "Mutation data from GATK Whole-Genome Germline Sequencing";
                    }
                    else {
                        profile_desc = "Mutation data from GATK Whole-Genome Somatic Sequencing";
                    }
                    break;
                case PipelineDB.SEQ_RNA:
                    alteration_type = "MRNA_EXPRESSION";
                    datatype = "Z-SCORE";
                    profile_desc = "RNA-seq data";
                    profile_name = "mRNA expression z-Scores (RNA Seq)";
                    stable_id = "rna_seq_mrna_median_Zscores";
                    case_list_ids = createSubjectsList(data_file, 2);
                    // Convert pipeline output to z-score format.
                    data_file = convert2zScoreFile(data_file, "seq_rna");
                    break;
                case PipelineDB.GEX_AFFYMETRIX:
                    alteration_type = "MRNA_EXPRESSION";
                    datatype = "Z-SCORE";
                    profile_desc = "mRNA data";
                    profile_name = "mRNA expression (Affymetrix microarray)";
                    stable_id = "mrna_median_Zscores";
                    case_list_ids = createSubjectsList(data_file, 2);
                    // Convert pipeline output to z-score format.
                    data_file = convert2zScoreFile(data_file, "affymetrix");
                    break;
                case PipelineDB.GEX_ILLUMINA:
                    alteration_type = "MRNA_EXPRESSION";
                    datatype = "Z-SCORE";
                    profile_desc = "mRNA data";
                    profile_name = "mRNA expression (Illumina microarray)";
                    stable_id = "mrna_median_Zscores";
                    case_list_ids = createSubjectsList(data_file, 2);
                    // Convert pipeline output to z-score format.
                    data_file = convert2zScoreFile(data_file, "illumina");
                    break;
                case PipelineDB.METHYLATION:
                    alteration_type = "METHYLATION";
                    datatype = "CONTINUOUS";
                    profile_desc = "Methylation beta-values";
                    profile_name = "Methylation (HM450)";
                    stable_id = "methylation_hm450";
                    case_list_ids = createSubjectsList(data_file, 2);
                    break;
                case PipelineDB.CNV_ILLUMINA:
                case PipelineDB.CNV_AFFYMETRIX:
                    alteration_type = "COPY_NUMBER_ALTERATION";
                    datatype = "DISCRETE";
                    profile_desc = "Putative copy-number from GISTIC 2.0. "
                                 + "Values: -2 = homozygous deletion; "
                                 + "-1 = hemizygous deletion; "
                                 + "0 = neutral|no change; 1 = gain; "
                                 + "2 = high level amplification.";
                    stable_id = "gistic";
                    if (job.getPipeline_name().equals(PipelineDB.CNV_ILLUMINA)) {
                        profile_name = "Putative copy-number (Illumina) alterations from GISTIC";
                    }
                    else {
                        profile_name = "Putative copy-number (Affymetrix) alterations from GISTIC";
                    }
                    case_list_ids = createSubjectsList(data_file, 2);
                    break;
                default:
                    // Unlikely for control to reach here.
                    logger.error("Received invalid pipeline job during exporting of data: " 
                            + job.getPipeline_name());
                    break;
            }
            // Create the meta file and case list file for each pipeline.
            logger.debug("Meta file created: " + createMetaPLFile
                        (job.getPipeline_name(), stable_id, data_file));
            logger.debug("Case list file created: " + createCaseListFile
                        (job.getPipeline_name(), job.getInput_desc(), 
                        case_list_ids, stable_id));
            // Store the path of this pipeline data file; to be deleted after
            // export.
            datafiles.append(data_file).append("\t");            
        }
        
        createDataCancerType();
        createDataClinicalSamplesFile();
        // Import the study into cBioPortal.
        List<String> importStudyCMD = importStudyCommand();
        // Prepare log file.
        String logFileName = createLogFile(dir, "import_" + studyID);
        // Import the study.
        if (executeImportScript(importStudyCMD, logFileName)) {
            logger.debug("Study exported.");
        }
        else {
            // Failed to import the study, no point to continue.
            logger.error("FAIL to export study!");
            logger.error("Aborting data export for study " + studyID);
            // Send the failed notification email to user.
            Postman.sendExportDataStatusEmail(studyID, userName, Constants.NOT_OK);
            return;
        }
        
        // Create and save the cBioPortal URL into database.
        StudyDB.updateStudyCbioUrl(studyID, createCbioUrl());
        // Save the visual time into database.
        StudyDB.updateStudyVisualTime(studyID, visual_time);
        // Export completed!
        logger.debug(studyID + " exported to cBioPortal.");
        // Delete all the temporary pipeline data files.
        for (String tmp_file : datafiles.toString().split("\t")) {
            if (!FileHelper.delete(tmp_file)) {
                logger.error("FAIL to delete temporary data file: " + tmp_file);
            }
        }
        // Used the executeImportScript to run the commands to restart the 
        // cBioPortal application.
        try {
            logger.debug(userName + ": trying to acquire tomcat control.");
            tcCtrl.acquire();
            logger.debug(userName + ": restarting cBioPortal.");
            executeImportScript(TCSTOP, createLogFile(dir, "stop_cBioPortal"));
            executeImportScript(TCSTART, createLogFile(dir, "start_cBioPortal"));
            logger.debug(userName + ": cBioPortal restarted.");
            // Wait a minute before releasing the control key i.e. giving
            // the application time to complete it's reset cycle.
            sleep(60000);
        }
        catch (InterruptedException ie) {
            logger.error("FAIL to acquire tomcat control!");
            logger.error(ie.getMessage());
        }
        finally {
            tcCtrl.release();
        }
        // Send notification email to user.
        Postman.sendExportDataStatusEmail(studyID, userName, Constants.OK);
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
                // Remove the last tab.
                zsLine.setLength(zsLine.length() - 1);
                // Write the converted data into the z-score file.
                ps.println(zsLine);
            }
            logger.debug("z-score file created at: " + zScoreFile);
        }
        catch (IOException ioe) {
            logger.error("FAIl to convert " + plName + " data file to z-score format!");
            logger.error(ioe.getMessage());
        }

        /* Instead of replacing the original datafile with the converted z-score
        // file, we will update the datafile with the path of the converted
        // z-score file.
        // Move and replace the datafile with the converted z-score file.
        try {
            Path from = FileSystems.getDefault().getPath(zScoreFile);
            Path to = FileSystems.getDefault().getPath(datafile);
            Files.move(from, to, REPLACE_EXISTING);
            logger.debug("z-score file moved to temp directory.");
        }
        catch (IOException ioe) {
            logger.error("FAIl to copy z-score file to temp directory!");
            logger.error(ioe.getMessage());
        }
        */
        // Return the path to the converted z-score file.
        return zScoreFile;
    }
    
    // Create the commands to stop and start tomcat server.
    private void createTomcatCommands() {
        TCSTOP.add("http://localhost:8080/manager/text/stop?path=/cbioportal");
        TCSTART.add("http://localhost:8080/manager/text/start?path=/cbioportal");
//        TCSTOP.add("http://192.168.142.20:8080/manager/text/stop?path=/cbioportal");
//        TCSTART.add("http://192.168.142.20:8080/manager/text/start?path=/cbioportal");
        logger.debug("Tomcat START command: " + TCSTART.toString());
        logger.debug("Tomcat STOP command: " + TCSTOP.toString());
    }
    
    // Construct and return the import study command for cBioPortal.
    private List<String> importStudyCommand() {
        List<String> command = new ArrayList<>(CMD);
        // Build the command to import the study.
        command.add(dir);
        command.add("-n");
        command.add("-o");
        
        return command;
    }

    // Create the cBioPortal URL for this study.
    private String createCbioUrl() {
        String ipAddress;
        try {
            // Get the IP address of the current system.
            InetAddress ip = InetAddress.getLocalHost();
            ipAddress = ip.getHostAddress();
        } catch (UnknownHostException ex) {
            logger.error("FAIL to get the IP address of the system.");
            logger.error(ex.getMessage());
            // Set the IP address to 'localhost'.
            ipAddress = "localhost";
        }

        // Create a random 60 characters string.
        int dummyNum = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE/2, Integer.MAX_VALUE);
        String dummyStr = BCrypt.hashpw(String.valueOf(dummyNum), BCrypt.gensalt());
        // Need to replace the special character '/' as this string will form
        // part of an url.
        dummyStr = dummyStr.replace("/", "0");
        String target = dummyStr.substring(0, 30) + studyID.substring(0, 5) 
                      + dummyStr.substring(30) + studyID.substring(5);
        
//        return "http://" + ipAddress + ":8080/cbioportal/?sid=" + target;
        return target;
    }
    
    // Create the log file.
    private String createLogFile(String dir, String filename) {
        return dir + Constants.getLOGFILE_NAME() + 
               filename + Constants.getLOGFILE_EXT();
    }
    
    // Execute the command and write the output to the log file.
    private boolean executeImportScript(List<String> command, String logFileName) {
        boolean status = Constants.NOT_OK;
        ProcessBuilder pb = new ProcessBuilder(command);
        // The execution log will be written to the log file.
        File logFile = new File(logFileName);
        // Merge the standard error and output stream, and always sent to the
        // same destination.
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(logFile));
        logger.debug("Executing command: " + command);
        
        try {
            Process process = pb.start();
            // Wait for the command to complete.
            int result = process.waitFor();
            if (result == 0) {
                // Any result other than 0 is considered not ok.
                status = Constants.OK;
            }
            logger.debug("Command completed with status: " + result);
        }
        catch (IOException ioe) {
            logger.error("FAIL to execute command!");
            logger.error(ioe.getMessage());
        }
        catch (InterruptedException ie) {
            logger.error("FAIL to complete execution of command!");
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
    
    // Create the meta_study file for this study. Return the absolute path of 
    // the meta_study file.
    private String createMetaStudyFile() {
        String result = Constants.FAILED;
        File meta_study = new File(meta_study_txt);
        
        try (FileWriter fw = new FileWriter(meta_study)) {
            // Create meta_study file.
            meta_study.createNewFile();
            // Write to the meta_study file according to the format needed by
            // metaImport Python script.
            fw.write("type_of_cancer: " + study.getIcd_code() + "\n");
            fw.write("cancer_study_identifier: " + studyID + "\n");
            // Need to trim the title to be less than or equal to 255 characters.
            fw.write("name: " + trimString(study.getTitle(), 255) + "\n");
            fw.write("short_name: " + studyID + "\n");
            fw.write("description: " + study.getTitle() + "\n");
            fw.write("add_global_case_list: true");
            // Update the result with the absolute path of the meta_study file.
            result = meta_study.getAbsolutePath();
        }
        catch (IOException ioe) {
            logger.error("FAIL to create meta file for study!");
            logger.error(ioe.getMessage());
        }

        return result;
    }
    
    // Create the meta file for the cancer type mentioned in this study. Return
    // the absolute path of the meta file.
    private String createMetaCancerTypeFile() {
        String result = Constants.FAILED;
        File meta_file = new File(meta_cancer_type);
        
        try (FileWriter fw = new FileWriter(meta_file)) {
            meta_file.createNewFile();
            // Write to the meta file according to the format needed by
            // metaImport Python script.
            fw.write("genetic_alteration_type: CANCER_TYPE\n");
            fw.write("datatype: CANCER_TYPE\n");
            fw.write("data_filename: " + cancer_type);
            // Update the result with the absolute path of the meta file.
            result = meta_file.getAbsolutePath();
        }
        catch (IOException ioe) {
            logger.error("FAIL to create meta file for cancer type!");
            logger.error(ioe.getMessage());
        }
        
        return result;
    }
    
    // Create the meta file for the clinical samples used in this study. Return
    // absolute path of the meta file.
    private String createMetaClinicalSamplesFile() {
        String result = Constants.FAILED;
        File meta_file = new File(meta_clinical_samples);
        
        try (FileWriter fw = new FileWriter(meta_file)) {
            meta_file.createNewFile();
            // Write to the meta file according to the format needed by
            // metaImport Python script.
            fw.write("cancer_study_identifier: " + studyID + "\n");
            fw.write("genetic_alteration_type: CLINICAL\n");
            fw.write("datatype: SAMPLE_ATTRIBUTES\n");
            fw.write("data_filename: " + data_clinical_samples);
            // Update the result with the absolute path of the meta file.
            result = meta_file.getAbsolutePath();
        }
        catch (IOException ioe) {
            logger.error("FAIL to create meta file for clinical samples!");
            logger.error(ioe.getMessage());
        }
        
        return result;
    }
    
    // Create the meta file for each pipeline. Return the absolute path of the
    // meta file.
    private String createMetaPLFile(String pipeline, String stable_id, 
            String data_file) {
        String result = Constants.FAILED;
        File meta_file = new File(dir + "meta_" + pipeline + ".txt");
        
        try (FileWriter fw = new FileWriter(meta_file)) {
            meta_file.createNewFile();
            // Write to the meta file according to the format needed by
            // metaImport Python script.
            fw.write("cancer_study_identifier: " + studyID + "\n");
            fw.write("genetic_alteration_type: " + alteration_type + "\n");
            fw.write("datatype: " + datatype + "\n");
            fw.write("stable_id: " + stable_id + "\n");
            fw.write("show_profile_in_analysis_tab: true\n");
            fw.write("profile_description: " + profile_desc + "\n");
            fw.write("profile_name: " + profile_name + "\n");
            fw.write("data_filename: " + data_file + "\n");
            if (PipelineDB.isGATKPipeline(pipeline)) {
                fw.write("swissprot_identifier: accession\n");
            }
            // Update the result with the absolute path of the meta file.
            result = meta_file.getAbsolutePath();
        }
        catch (IOException ioe) {
            logger.error("FAIL to create meta file for pipeline " + pipeline);
            logger.error(ioe.getMessage());
        }

        return result;
    }
    
    // Create the case list file for each pipeline. Return the absolute path
    // of the case list file.
    private String createCaseListFile(String pipeline, String description, 
            Set<String> case_list, String stable_id) {
        String result = Constants.FAILED;
        File case_file = new File(case_dir + "cases_" + pipeline + ".txt");
        
        try (FileWriter fw = new FileWriter(case_file)) {
            case_file.createNewFile();
            // Write to the case list file according to the format needed by
            // metaImport Python script.
            fw.write("cancer_study_identifier: " + studyID + "\n");
            fw.write("stable_id: " + studyID + "_" + stable_id + "\n");
            fw.write("case_list_name: " + ResourceRetriever.getMsg(pipeline) + " Cases\n");
            fw.write("case_list_description: " + description + "\n");
            fw.write("case_list_ids: ");
            for (String case_id : case_list) {
                fw.write(case_id + "\t");
            }
            fw.write("\n");
            // Update the result with the absolute path of the case list file.
            result = case_file.getAbsolutePath();
        }
        catch (IOException e) {
            logger.error("FAIL to create case list file for pipeline " + pipeline);
        }
        
        return result;
    }
    
    // Create the data file for cancer type.
    private void createDataCancerType() {
        // Select a random color code to be use for this cancer type.
        String color = color_code
                [ThreadLocalRandom.current().nextInt(0, color_code.length)];
        File data_file = new File(cancer_type);
        
        try (FileWriter fw = new FileWriter(data_file)) {
            data_file.createNewFile();
            // Create the data file for cancer type according to the format
            // needed by metaImport Python script.
            fw.write(study.getIcd_code() + "\t");
            fw.write(study.getICDName() + "\t");
            fw.write(study.getICDName() + "\t");
            fw.write(color + "\t");
            // All newly created cancer type will fall under 'other' for now.
            fw.write("other\n");
        }
        catch (IOException e) {
            logger.error("FAIL to create the data file for cancer type!");
        }        
    }
    
    // Create the data file for the clinical samples.
    private void createDataClinicalSamplesFile() {
        File data_file = new File(data_clinical_samples);
        
        try (FileWriter fw = new FileWriter(data_file)) {
            data_file.createNewFile();
            // Create the data file for clinical samples according to the format 
            // needed by metaImport Python script.
            fw.write("#Patient Identifier\tSample Identifier\n");
            fw.write("#Patient Identifier\tSample Identifier\n");
            fw.write("#STRING\tSTRING\n");
            fw.write("#1\t1\n");
            fw.write("PATIENT_ID\tSAMPLE_ID\n");
            // Write all the unique subject ID into this data file.
            for (String subject : casesAllList) {
                fw.write(subject + "\t" + subject + "\n");
            }
        }
        catch (IOException e) {
            logger.error("FAIL to create the data file for clinical samples!");
        }
    }
    
    // Create the list of subject IDs from the first line of pipeline output; 
    // to be use in the case_list. Parameter offset will tell how many columns
    // to ignore.
    private Set<String> createSubjectsList(String filename, int offset) {
        Set<String> subjectsList = new HashSet<String>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String header = br.readLine();
            String[] subjectID = header.split("\t");
            for (int i = offset; i < subjectID.length; i++) {
                subjectsList.add(subjectID[i]);
                casesAllList.add(subjectID[i]);
            }
        }
        catch (IOException e) {
            logger.error("FAIL to create subject list!");
            logger.error(e.getMessage());
        }
        
        return subjectsList;
    }
    
    // Create the list of subject IDs from the xth column of the pipeline
    // output; to be use in the case_list. Parameter offset will tell us which
    // column contains the subject ID.
    private Set<String> createSubjectsListForMAF(String filename, int offset) {
        Set<String> subjectsList = new HashSet<String>();
        String lineRead;
        String[] columns;
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            // Skip the first 2 lines.
            for (int i = 0; i <= 1; i++)
                br.readLine();
            
            while ((lineRead = br.readLine()) != null) {
                columns = lineRead.split("\t");
                if (columns.length <= offset) {
                    // Invalid data file has been passed in, break out of the 
                    // while loop.
                    subjectsList.add("INVALID_DATAFILE");
                    break;
                } else {
                    subjectsList.add(columns[offset]);
                    casesAllList.add(columns[offset]);
                }
            }
        }
        catch (IOException e) {
            logger.error("FAIL to create subject list for MAF!");
            logger.error(e.getMessage());
        }
        
        return subjectsList;
    }
}
