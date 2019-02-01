/*
 * Copyright @2015-2019
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.InputData;
import TIMS.Database.InputDataDB;
import TIMS.Database.Pipeline;
import TIMS.Database.PipelineDB;
import TIMS.Database.StudyDB;
import TIMS.Database.SubmittedJob;
import TIMS.Database.SubmittedJobDB;
import TIMS.Database.UserAccountDB;
import TIMS.Database.UserRoleDB;
import TIMS.General.Constants;
import TIMS.General.ExitListener;
import TIMS.General.FileHelper;
import TIMS.General.Postman;
import TIMS.General.ProcessExitDetector;
import TIMS.General.ResourceRetriever;
// Libraries for Java
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
// Libraries for Java Extension
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ConfigBean is an abstract class, and it will be extended by all the pipeline
 * configuration beans.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Initial creation by refactoring from ArrayConfigBean.
 * 18-Nov-2015 - Removed one abstract method allowToSubmitJob(), added one 
 * variable jobSubmissionStatus, and one abstract method 
 * updateJobSubmissionStatus() to resolve the issues surrounding the job
 * submission's readiness status.
 * 25-Nov-2015 - Renamed pipelineType to pipelineTech. Implementation for 
 * database 2.0
 * 02-Dec-2015 - Streamline the createConfigFile method. Implemented the changes
 * in the input folder directory.
 * 07-Dec-2015 - Recreate the annotationList every time a new sample annotation
 * file is uploaded.
 * 11-Dec-2015 - Added method getStudyList that return the list of Study ID 
 * setup for the user's department.
 * 15-Dec-2015 - Shifted method getStudyList to Class MenuSelectionBean. 
 * Removed the submission time from study id (in the config file). Implemented 
 * the new workflow (i.e. User to select Study ID before proceeding to pipeline 
 * configuration.
 * 16-Dec-2015 - Added one attribute inputFileDesc, and one abstract method
 * saveSampleFileDetail(). To save the sample file detail to database, and
 * rename the sample annotation file before executing the pipeline.
 * 22-Dec-2015 - Added one abstract method renameAnnotCtrlFiles(), to rename
 * the sample annotation file (and control probe file) to a common name for all
 * pipelines. Added one attribute, haveNewData.
 * 31-Dec-2015 - Implemented the module for reusing the input data.
 * 06-Jan-2016 - Added new method, getInputPath().
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 12-Jan-2016 - To include the study ID into the config filename.
 * 14-Jan-2016 - Removed all the static variables in Pipeline Configuration
 * Management module.
 * 18-Jan-2016 - Changed the type of variable sample_average from String to
 * boolean.
 * 20-Jan-2016 - To streamline the navigation flow and passing of pipeline name
 * from main menu to pipeline configuration pages.
 * 21-Jan-2016 - Added one new field pipeline_name in the input_data table; to
 * associate this input_data with the respective pipeline.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 28-Jan-2016 - Added the pipeline name into the config file content and 
 * filename. Added the seconds timing into the filename.
 * 18-Feb-2016 - To check the input files received with the filename listed in
 * the annotation file. List out the missing files (if any) and notice the user
 * during pipeline configuration review.
 * 19-Feb-2016 - To use the new generic method renameFilename in FileUploadBean
 * class when renaming annotation and control files. To use the new generic
 * constructor in FileUploadBean class when creating new object.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 29-Mar-2016 - Instead of storing the input path, the system will store the 
 * input SN.
 * 11-Apr-2016 - Changes due to the removal of attributes (sample_average, 
 * standardization, region and probe_select) from submitted_job table.
 * 12-Apr-2016 - Changes due to the removal of attributes (probe_filtering and
 * phenotype_column) from submitted_job table. Allow administrator to upload 
 * raw data through the UI.
 * 14-Apr-2016 - Changes due to the type change (i.e. to Timestamp) for 
 * submit_time and complete_time in submitted_job table.
 * 25-Apr-2016 - Commented out some unnecessary comments.
 * 13-May-2016 - Minor changes; to rename some of the variables.
 * 19-May-2016 - Changes due to the addition attribute (i.e. detail_output) in 
 * submitted_job table.
 * 25-Aug-2016 - Changes due to attribute name change in Pipeline table, and 
 * method name (i.e. getCreateTimeString) change in InputData class. Removed 
 * unused code.
 * 05-Sep-2016 - Added method downloadAnnot, to allow user to download the
 * annotation file from the input package.
 * 14-Sep-2016 - Implemented Raw Data Customization module. Defined nested 
 * class ExcludeFileName. Code refactoring for the method insertJob().
 * 20-Sep-2016 - Enhanced method retrieveRawDataFileList() to make sure the 
 * filename is sorted before storing them into the file list.
 * 22-Sep-2016 - To record the raw data customization activity into the 
 * database.
 * 29-Nov-2016 - To include the annotation version in the config file.
 * 08-Dec-2016 - To record user activity after uploading of new raw data. To 
 * check the correctness of the annotation file format.
 * 06-Feb-2017 - Added helper functions getFilenamePairs() and 
 * filterRawDataFileList().
 * 08-Feb-2017 - Enhanced getAllFilenameFromAnnot(), so that it could handle
 * empty line in annotation file. Renamed method getFilenamePairs() to 
 * getFilenamePairsFromAnnot().
 * 13-Feb-2017 - To consolidate all the pipeline parameters (i.e. chip_type, 
 * normalization and summarization) into one field (i.e. parameters) in the 
 * database.
 * 13-Jul-2017 - Changes due to the addition of GATK Sequencing Pipelines.
 * 25-Sep-2017 - If the pipeline process failed to start, need to update the
 * job status to failed.
 * 15-May-2018 - Don't create the dummy files for pipeline output and detail
 * after submitting jobs.
 * 24-Jan-2019 - Added GTF file for RNA Seq pipeline.
 * 31-Jan-2019 - To use a common input directory for all newly uploaded raw data.
 */

public abstract class ConfigBean implements Serializable {
    // Get the logger for Log4j
    protected final static Logger logger = LogManager.
            getLogger(ConfigBean.class.getName());
    protected String studyID, commandLink;
    // Common Processing Parameters. 
    protected String type, normalization, summarization;
    protected Integer readDepth, variantDepth;
    protected boolean excludeDB;
    // Indicator of whether user have new data to upload or not.
    protected boolean haveNewData;
    // Pipeline name and technology
    protected String pipelineName, pipelineTech;
    // Common input files that will be uploaded by the users
    protected FileUploadBean inputFile, sampleFile;
    // Brief description of the input file
    protected String inputFileDesc;
    // Record the time this job was created
    protected String submitTimeInFilename;
    protected Timestamp submitTimeInDB;
    // job_id of the inserted record. sn of the input data. role ID of the 
    // current user.
    protected int job_id, input_sn, roleID;
    // jobSubmissionStatus will keep track of whether has all the input files
    // been uploaded, all the required parameters been filled up, etc.
    private boolean jobSubmissionStatus;
    // Pipeline output, report and config filename
    protected String pipelineOutput, detailOutput, pipelineReport, pipelineConfig;
    // Pipeline input, control, interval, gtf and sample annotation filename
    protected String input, ctrl, interval, gtf, sample;
    // Store the input directory for the newly uploaded raw data.
    protected String inputDir;
    // List of input data belonging to the study, that are available for reuse.
    private List<InputData> inputDataList = new ArrayList<>();
    protected InputData selectedInput;
    // Store the user ID and home directory of the current user.
    protected String userName, homeDir;
    // Raw data file list.
    protected List<ExcludeFileName> fileList = new ArrayList<>();
    // Exclusion date file list.
    protected List<ExcludeFileName> exclList;
    // Excluded file list string builder.
    protected StringBuilder exclFileSB = new StringBuilder();
    // Excluded file list array.
    protected List<String> exclFileList = new ArrayList<>();
    // Raw data file extension.
    protected String rdFileExt;
    // Custom description.
    protected String custDesc;
    // Customization status i.e. true == saved.
    protected boolean custStatus;
    protected InputDataDB inputDB;

    // This method will only be trigger if all the inputs validation have 
    // passed after the user clicked on Submit. As a result of this behaviour 
    // (i.e. all the validations need to pass), we will check whether all the 
    // input files have been uploaded and update the jobSubmissionStatus here.
    abstract void updateJobSubmissionStatus();
    // Save the input data detail into the database.
    abstract void saveSampleFileDetail();
    
    protected void init() {
        // Retrieve the setting from the session map.
        userName = (String) getFacesContext().getExternalContext().
                    getSessionMap().get("User");
        roleID = UserAccountDB.getRoleID(userName);
        studyID = (String) getFacesContext().getExternalContext().
                    getSessionMap().get("study_id");
        haveNewData = (boolean) getFacesContext().getExternalContext().
                    getSessionMap().get("haveNewData");
        pipelineName = (String) getFacesContext().getExternalContext().
                    getSessionMap().get("pipeline");
        inputDB = new InputDataDB();
        // Setup local variables using the setting retrieved from session map.
        pipelineTech = PipelineDB.getPipelineTechnology(pipelineName);
        commandLink = ResourceRetriever.getMsg(pipelineName);
        homeDir = Constants.getSYSTEM_PATH() + Constants.getUSERS_PATH() + userName;
        jobSubmissionStatus = false;
        input_sn = Constants.DATABASE_INVALID_ID;
        custStatus = false;
        // Create the time stamp for the pipeline job.
        createJobTimestamp();
        
        if (haveNewData) {
            inputDir = Constants.getSYSTEM_PATH() + Constants.getINPUT_PATH() 
                       + studyID + File.separator 
                       + pipelineName + "_"
                       + submitTimeInFilename + File.separator;
            inputFile = new FileUploadBean(inputDir);
            sampleFile = new FileUploadBean(inputDir);
        }
        else {
            inputDataList = inputDB.getIpList(studyID, pipelineName);
        }
        logger.debug(studyID + " ConfigBean - init().");
    }
    
    // Rename the sample annotation file (and control probe or interval file) to 
    // a common name for future use.
    public void renameAnnotCtrlFiles() {
        // Rename sample annotation file.
        sampleFile.renameFilename(Constants.getANNOT_FILE_NAME() + 
                                  Constants.getANNOT_FILE_EXT());
    }
    
    // Read in all the sample input filename listed in the annotation file.
    public List<String> getAllFilenameFromAnnot() {
        List<String> filenameList = new ArrayList<>();
        List<String> content = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(
                                 new FileReader(sampleFile.getLocalDirectoryPath() + 
                                                sampleFile.getInputFilename())))
        {
            // First line is the header; not needed here.
            String lineRead = br.readLine();
            // Start processing from the second line.
            while ((lineRead = br.readLine()) != null) {
                content = Arrays.asList(lineRead.split("\t"));
                // Check that the annotation file format is correct.
                if (content.size() > 1) {
                    // The second column is the filename, store it.
                    filenameList.add(content.get(1));
                }
            }
            
            // Check to make sure the filename list has been retrieved.
            if (filenameList.size() > 0) {
                logger.debug("All filename read from annotation file.");
            }
            else {
                // The format of the annotation file is incorrect.
                logger.error("Incorrect annotation file format detected in " + sampleFile.getInputFilename());
                // Return a error message in the filename list; to be displayed to the user.
                filenameList.add("INCORRECT ANNOTATION FILE FORMAT!");
            }
        }
        catch (IOException e) {
            logger.error("FAIL to read annotation file!");
            logger.error(e.getMessage());
        }
        
        return filenameList;
    }
    
    // The administrator uploading raw data for this pipeline in this study.
    public String uploadRawData() {
        logger.info(userName + ": uploaded raw data for pipeline " + 
                    pipelineName + " under study " + studyID);
        // Create an input_data record for the raw data uploaded.
        saveSampleFileDetail();
        // Rename sample annotation file (and control probe file) to a
        // common name for future use.
        renameAnnotCtrlFiles();
        // Send the input data path to the administrator.
        Postman.sendDataUploadedEmail(userName, studyID, pipelineName, 
                                      inputFile.getLocalDirectoryPath());
        // Record user activity.
        ActivityLogDB.recordUserActivity(userName, Constants.UPL_RD, 
                                         studyID + " - " + pipelineName + " - " + input_sn);

        return Constants.MAIN_PAGE;
    }
    
    // After reviewing the configuration, if user decided to proceed with 
    // the pipeline execution and click on Confirm button, submitJob will be
    // called. A series of operations will then occur:
    // 1. Insert the new job request into the database
    // 2. Create the Config file
    // 3. Start the pipeline execution
    public String submitJob() {
        logger.info(userName + ": started " + pipelineName);

        String result = Constants.MAIN_PAGE;
        String outputDir = homeDir + Constants.getOUTPUT_PATH();
        String logDir = homeDir + Constants.getLOG_PATH();
        // Build the full path name for the pipeline output, detail output and
        // report files that will be generated by the pipeline. These paths 
        // will also be stored in submitted_job table.
        String outputPath = outputDir + Constants.getOUTPUTFILE_NAME() + 
                            getStudyID() + "_" + submitTimeInFilename;
        pipelineOutput = outputPath + Constants.getOUTPUTFILE_EXT();
        detailOutput = outputPath + Constants.getDETAIL_FILE_NAME() 
                     + Constants.getOUTPUTFILE_EXT();
        pipelineReport = outputDir + Constants.getREPORTFILE_NAME() + 
                         getStudyID() + "_" + submitTimeInFilename +
                         Constants.getREPORTFILE_EXT();
        // logFilePath will store the log of the pipeline execution (For debug
        // purpose).
        String logFilePath = logDir + Constants.getLOGFILE_NAME() +
                             getStudyID() + "_" + submitTimeInFilename;

        // Step to follow for pipeline execution:
        // 1. Create Config file
        // 2. Insert new job request into database
        // 3. Update job status to in-progress, save sample file detail and 
        // rename sample annotation file.
        // 4. Execute the pipeline
        //      4.1 Create the process to run the pipeline using the 
        //      ProcessBuilder.
        //      4.2 Start the process, and spawn a thread to detect the
        //      completion of execution at the background.
        //      4.3 Once execution is completed, listener thread will update
        //      the job status of the job according to the return status from
        //      the process.
        
        // 1. Create the config file; to be use by the pipeline during execution.
        // 2. Insert this new job request into the submitted_job table
        if (createConfigFile() && insertJob()) {
            // 3. Update job status to in-progress
            SubmittedJobDB.updateJobStatusToInprogress(job_id);
            logger.debug("Pipeline run and job status updated to in-progress. ID: " 
                        + job_id);
            if (haveNewData) {
                // Save the Sample File detail into database
                saveSampleFileDetail();
                // Rename sample annotation file (and control probe file) to a
                // common name for future use.
                renameAnnotCtrlFiles();
                // Update the input SN for this job.
                SubmittedJobDB.updateJobInputSN(job_id, input_sn);
                // Record user activity for raw data upload.
                ActivityLogDB.recordUserActivity(userName, Constants.UPL_RD, 
                                                 studyID + " - " + pipelineName + " - " + input_sn);
            }
            // 4. Pipeline is ready to be run now
            result = executePipeline(logFilePath);
            // Create dummy pipeline files for user to download.
//            createDummyFile(pipelineOutput);
//            createDummyFile(detailOutput);
            
            // Record this pipeline execution activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.EXE_PL, 
                                             pipelineName);
        }
        else {
            result = Constants.ERROR;
        }
        
        if (result.equals(Constants.ERROR)) {
            // Job has failed, update status accordingly.
            SubmittedJobDB.updateJobStatusToFailed(job_id);
        }
        
        return result;
    }
    
    // After reviewing the configuration, user decided not to proceed with
    // the pipeline execution. 
    public void cancelJob() {
        // Reset the jobSubmissionStatus.
        jobSubmissionStatus = false;
        logger.debug(userName + ": cancel " + pipelineName);
    }
    
    // Lock down and record the time when the user enter the GEX page.
    private void createJobTimestamp() {
        // inputFilesDir is used to create the directory for input files 
        // i.e. cannot have space in its format.
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm_ss");
        submitTimeInFilename = dateFormat.format(new Date());
        // submitTimeInDB will be stored in the submitted_job table and will
        // be display to the user in Job Status page.
        Date now = new Date();
        submitTimeInDB = new Timestamp(now.getTime());
    }
    
    // Execute the pipeline together with the config file, the log generated 
    // by the pipeline will be directed to the pipelineLog file.
    private String executePipeline(String pipelineLog) {
        String result = Constants.MAIN_PAGE;
        // Build the pipeline command
        List<String> command = new ArrayList<>();
        PipelineDB plDB = new PipelineDB(pipelineName);
        Pipeline cmd;
        
        try {
            // Retrieve the pipeline command and it's parameter from database.
            cmd = plDB.getPipeline();
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve pipeline command " +
                    pipelineName);
            logger.error(e.getMessage());
            // Something is wrong, shouldn't let the user continue.
            return Constants.ERROR;
        }
        
        command.add(cmd.getCommand());
        command.add(cmd.getParameter());
        command.add(pipelineConfig);
        logger.info("Full pipeline command: " + command.toString());
        
        ProcessBuilder pb = new ProcessBuilder(command);
        // The execution log from the pipeline will be written to the 
        // pipelineLog file.
        File output = new File(pipelineLog);
        // Merge the standard error and output stream, and always sent to
        // the same destination
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(output));
        Process process = null;
        
        try {
            // Start the pipeline
            process = pb.start();
        } 
        catch (IOException ioe) {
            logger.error("FAIL to start pipeline process!");
            logger.error(ioe.getMessage());
            result = Constants.ERROR;
        }

        if (process != null) {
            // Start a listener thread to detect the complete status of 
            // the process
            try {
                ProcessExitDetector exitDetector = new ProcessExitDetector(
                                    job_id, studyID, process, new ExitListener());
                // If the pipeline has completed, an IllegalArgumentException
                // will be thrown and the exit detector will not get to run.
                exitDetector.start();
            }
            catch (IllegalArgumentException e) {
                logger.error("Pipeline completed before detector is create!");
            }
        }
        
        return result;
    }

    // Create the config file that will be used during pipeline execution.
    protected boolean createConfigFile() {
        boolean result = Constants.OK;
        String configDir = homeDir + Constants.getCONFIG_PATH();
        String exDB = null;
        // Parameter EXCLUDE_DB only apply for GATK pipelines.
        if (PipelineDB.isGATKPipeline(pipelineName)) {
            exDB = isExcludeDB()?"YES":"NO";
        }
        // Config File will be send to the pipeline during execution
        File configFile = new File(configDir + 
                Constants.getCONFIG_FILE_NAME() + getStudyID() + "_" + 
                pipelineName + "_" + submitTimeInFilename + 
                Constants.getCONFIG_FILE_EXT());

        pipelineConfig = configFile.getAbsolutePath();

        try (FileWriter fw = new FileWriter(configFile)) {
            // Create config file
            configFile.createNewFile();
            // Write to the config file according to the format needed 
            // by the pipeline. Sample annotation file will be having the same
            // common name for all pipelines.
            fw.write("### INPUT parameters for " + pipelineName + "\n" +
                     "STUDY_ID\t=\t" + getStudyID() +
                     "\nANNOTATION\t=\t" + StudyDB.getStudyAnnotVer(studyID) +
                     "\nTYPE\t=\t" + getType() +
                     "\nINPUT\t=\t" + input +
                     "\nCTRL_FILE\t=\t" + ctrl +
                     "\nSAMPLES_ANNOT_FILE\t=\t" + sample + 
                     "\nINTERVAL_FILE\t=\t" + interval +
                     "\nGTF_FILE\t=\t" + gtf +
                     "\nEXCLUDE_FILES\t=\t" + exclFileSB.toString() + "\n\n");

            fw.write("### PROCESSING parameters\n" +
                     "NORMALIZATION\t=\t" + getNormalization() +
                     "\nSUMMARIZATION\t=\t" + getSummarization() + 
                     "\nREAD_DEPTH\t=\t" + getReadDepth() +
                     "\nVARIANT_DEPTH\t=\t" + getVariantDepth() +
                     "\nEXCLUDE_DB\t=\t" + exDB + "\n\n");

            fw.write("### Output file after normalization and processing\n" +
                     "OUTPUT\t=\t" + pipelineOutput + "\n\n");

            fw.write("### Report Generation\n" +
                     "REP_FILENAME\t=\t" + pipelineReport + "\n\n");
        }
        catch (IOException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to create pipeline config file!");
        }

        return result;
    }

    // Insert the current job request into the submitted_job table. The status
    // of the insertion operation will be return.
    private boolean insertJob() {
        boolean result = Constants.OK;
        // If new raw data has been uploaded, input_desc will follow the 
        // description that the user has entered.
        String input_desc = inputFileDesc;
        if (!haveNewData) {
            // Reusing raw data.
            if (custStatus) {
                // Customized raw data.
                input_desc = custDesc;
            }
            else {
                input_desc = selectedInput.getDescription();
            }
        }
        
        // Insert the new job request into datbase; job status is 1 i.e. Waiting
        // For complete_time, set to null for the start.
        SubmittedJob newJob = 
                new SubmittedJob(getStudyID(), userName, pipelineName, 1,
                                 submitTimeInDB, null, input_sn, input_desc,
                                 buildParametersStr(), pipelineOutput, 
                                 detailOutput, pipelineReport);
        
        try {
            // Store the job_id of the inserted record
            job_id = SubmittedJobDB.insertJob(newJob);
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert job!");
            logger.error(e.getMessage());
        }

        return result;
    }
    
    // Build the pipeline parameters string based on the value of common 
    // processing parameters.
    private String buildParametersStr() {
        String parameters = "";
        
        if (getType() != null) {
            parameters = "Type:" + getType() + " ";
        }
        
        if (getNormalization() != null) {
            parameters += "Norm:" + getNormalization() + " ";
        }
        
        if (getSummarization() != null) {
            parameters += "Sum:" + getSummarization() + " ";
        }
        
        // The processing parameters (Read depth, variant depth and exclude DB)
        // are introduced by the GATK Sequencing pipelines.
        if (PipelineDB.isGATKPipeline(pipelineName)) {
            String exDB = isExcludeDB()?"YES":"NO";
            parameters += "RD:" + getReadDepth() + " ";
            parameters += "VD:" + getVariantDepth() + " ";
            parameters += "ExDB:" + exDB + " ";
        }
        
        return parameters;
    }
    
    // User has selected a new input package, need to rebuild the variables.
    public void inputSelectionChange() {
        // Reset all the variables that are related to Raw Data Customization.
        custStatus = false;
        custDesc = selectedInput.getDescription();
        exclFileList.clear();
        if (exclList != null) {
            // User has selected some raw data to exclude in the previously
            // input package, need to reset the selection.
            exclList.clear();
        }
        
        if (exclFileSB.length() > 0) {
            exclFileSB.delete(0, exclFileSB.length());
        }
        
        logger.debug("Study " + selectedInput.getStudy_id() + 
                     " Serial No " + selectedInput.getSn() + " Input Selected.");
    }
    
    // Retrieve the raw data file list from directory.
    public void retrieveRawDataFileList() {
        File[] fList = FileHelper.getFilesWithExt(selectedInput.getFilepath(), rdFileExt);
        List<String> fNameList = new ArrayList<>();
        // Clear the existing file list before building the new file list.
        fileList.clear();
        int index = 0;
        
        for (File rd : fList) {
            fNameList.add(rd.getName());
        }
        // Sort the filename list first before storing them into fileList.
        Collections.sort(fNameList);
        
        for (String filename : fNameList) {
            fileList.add(new ExcludeFileName(index++, filename));
        }
        
        logger.debug("Total number of files retrieved: " + index);
    }

    // In the Raw Data Customization dialog, the user click on cancel to return
    // to pipeline config screen.
    public void cancelRawDataCust() {
        logger.debug(userName + " cancel Raw Data Customization.");
    }
    
    // Save the customization the user has made to the raw data package.
    public void saveCust() {
        String activity = studyID + " - " + pipelineName;
        // Update customization status to true.
        custStatus = true;
        // Clear the exclusion file list and string builder, before rebuilding
        // them.
        exclFileList.clear();
        if (exclFileSB.length() > 0) {
            exclFileSB.delete(0, exclFileSB.length());
        }
        // Build the exclusion file list and string builder.
        for (ExcludeFileName exclFile : exclList) {
            exclFileSB.append(exclFile.getFilename()).append(",");
            exclFileList.add(exclFile.getFilename());
        }
        
        // Make sure there are files excluded before removing the last character.
        if (exclFileSB.length() > 0) {
            // Remove the last ','
            exclFileSB.deleteCharAt(exclFileSB.length()-1);
            activity += ", excluded files: " + exclFileSB.toString();
        }
        else {
            activity += ", no file excluded.";
        }
        
        // Record this raw data customization activity into database.
        ActivityLogDB.recordUserActivity(userName, Constants.CUS_RD, activity);
        logger.debug("Raw Data Customization, excluded files: " + exclFileSB.toString());
    }
    
    // Convert boolean (i.e. true/false) to yes/no
    protected String booleanToYesNo(boolean parameter) {
        return parameter?"yes":"no";
    }

    // Create the output/report file for testing purposes.
    private void createDummyFile(String dummyFile) {
        File file = new File(dummyFile);
        
        try {
            // Create output file with the content set to the outputFilePath
            file.createNewFile();
            FileOutputStream fop = new FileOutputStream(file);
            byte[] contents = dummyFile.getBytes();
            
            fop.write(contents);
            fop.flush();
        }
        catch (IOException e) {
            logger.error("FAIL to create Dummy File!");
            logger.error(e.getMessage());
        }
    }

    // Return the input path depending on whether there is any new data uploaded.
    public String getInputPath() {
        if (haveNewData) {
            return inputFile.getLocalDirectoryPath();
        }
        else {
            return selectedInput.getFilepath();
        }
    }
    
    // Download the annotation file for user.
    public void downloadAnnot(InputData input) {
        String detail = "Annotation file of " + input.getStudy_id() 
                      + " - Serial No. " + input.getSn();
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, detail);
        FileHelper.download(input.getFilepath() + 
                            Constants.getANNOT_FILE_NAME() + 
                            Constants.getANNOT_FILE_EXT());
    }
    
    // Return the wording to be display at the link under the BreadCrumb in the 
    // pipeline configuration page.
    public String getBreadCrumbLink() {
        return commandLink + "  Study: " + studyID;
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Return the date the reused input data is being uploaded.
    public String getReuseInputDate() {
        return (selectedInput != null)?selectedInput.getCreateTimeString():
                "Please select a input to reuse";
    }
    
    // Only allow administrator to upload raw data.
    public boolean isUploadDataOnly() {
        return jobSubmissionStatus && haveNewData && (roleID == UserRoleDB.admin());
    }
    
    // Return true if no input package has been selected.
    public boolean getSelectedInputStatus() {
        return (selectedInput == null);
    }
    
    // Helper function to exclude the annotation, control, interval and gtf 
    // files from the raw data file list.
    protected void filterRawDataFileList() {
        File[] fList = FileHelper.getFilesWithExt(selectedInput.getFilepath(), rdFileExt);
        List<String> fNameList = new ArrayList<>();
        // Clear the existing file list before building the new file list.
        fileList.clear();
        int index = 0;
        
        for (File rd : fList) {
            fNameList.add(rd.getName());
        }
        // Sort the filename list first before storing them into fileList.
        Collections.sort(fNameList);
        
        for (String filename : fNameList) {
            // As long as the file belongs to Annotation, Control or interval 
            // files, we don't add them.
            if (!(filename.equals(Constants.getANNOT_FILE_NAME() + 
                                  Constants.getANNOT_FILE_EXT()) || 
                filename.equals(Constants.getCONTROL_FILE_NAME() + 
                                Constants.getCONTROL_FILE_EXT()) ||
                filename.equals(Constants.getINTERVAL_FILE_NAME() +
                                Constants.getINTERVAL_FILE_EXT()) ||
                filename.equals(Constants.getGTF_FILE_NAME() + 
                                Constants.getGTF_FILE_EXT())) ) {
                fileList.add(new ExcludeFileName(index++, filename));
            }
        }
        
        logger.debug("Total number of files retrieved: " + index);
    }
    
    // Read in all the sample input filename pair (separated by ',') listed in
    // the annotation file.
    protected List<String> getFilenamePairsFromAnnot() {
        List<String> filenameList = new ArrayList<>();
        List<String> content = new ArrayList<>();
        String[] fn;
        
        try (BufferedReader br = new BufferedReader(
                                 new FileReader(sampleFile.getLocalDirectoryPath() + 
                                                sampleFile.getInputFilename())))
        {
            // First line is the header; not needed here.
            String lineRead = br.readLine();
            // Start processing from the second line.
            while ((lineRead = br.readLine()) != null) {
                content = Arrays.asList(lineRead.split("\t"));
                // Check that the annotation file format is correct.
                if (content.size() > 1) {
                    // The second column contains the filename header; each header 
                    // will have 2 filenames separated by ','.
                    fn = content.get(1).split(",");
                    // Add the filenames to the list; can handle more than 2 
                    // filenames.
                    filenameList.addAll(Arrays.asList(fn));
                }
            }
            
            // Check to make sure the filename list has been retrieved.
            if (filenameList.size() > 0) {
                logger.debug("All filename read from annotation file.");
            }
            else {
                // The format of the annotation file is incorrect.
                logger.error("Incorrect annotation file format detected in " + sampleFile.getInputFilename());
                // Return a error message in the filename list; to be displayed to the user.
                filenameList.add("INCORRECT ANNOTATION FILE FORMAT!");
            }
        }
        catch (IOException e) {
            logger.error("FAIL to read annotation file!");
            logger.error(e.getMessage());
        }
        
        return filenameList;
    }
    
    // Machine generated getters and setters
    public FileUploadBean getSampleFile() {
        return sampleFile;
    }
    public void setSampleFile(FileUploadBean sampleFile) {
        this.sampleFile = sampleFile;
    }
    public FileUploadBean getInputFile() {
        return inputFile;
    }
    public void setInputFile(FileUploadBean inputFile) {
        this.inputFile = inputFile;
    }
    public String getInputFileDesc() {
        return inputFileDesc;
    }
    public void setInputFileDesc(String inputFileDesc) {
        this.inputFileDesc = inputFileDesc;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getNormalization() {
        return normalization;
    }
    public void setNormalization(String normalization) {
        this.normalization = normalization;
    }
    public String getSummarization() {
        return summarization;
    }
    public void setSummarization(String summarization) {
        this.summarization = summarization;
    }
    public Integer getReadDepth() {
        return readDepth;
    }
    public void setReadDepth(Integer readDepth) {
        this.readDepth = readDepth;
    }
    public Integer getVariantDepth() {
        return variantDepth;
    }
    public void setVariantDepth(Integer variantDepth) {
        this.variantDepth = variantDepth;
    }
    public boolean isExcludeDB() {
        return excludeDB;
    }
    public void setExcludeDB(boolean excludeDB) {
        this.excludeDB = excludeDB;
    }
    public boolean getJobSubmissionStatus() {
        return jobSubmissionStatus;
    }
    public void setJobSubmissionStatus(boolean jobSubmissionStatus) {
        this.jobSubmissionStatus = jobSubmissionStatus;
    }
    public InputData getSelectedInput() {        
        return selectedInput;
    }
    public void setSelectedInput(InputData selectedInput) {
        this.selectedInput = selectedInput;
    }
    public List<ExcludeFileName> getExclList() {
        return exclList;
    }
    public void setExclList(List<ExcludeFileName> exclList) {
        this.exclList = exclList;
    }
    public String getCustDesc() {
        return custDesc;
    }
    public void setCustDesc(String custDesc) {
        this.custDesc = custDesc;
    }

    // The setter for the following fields will not be provided.
    public String getPipelineTech() {
        return pipelineTech;
    }
    public String getCommandLink() {
        return commandLink;
    }
    public boolean getHaveNewData() {
        return haveNewData;
    }
    public String getPipelineName() {
        return pipelineName;
    }
    public String getStudyID() {
        return studyID;
    }
    public List<InputData> getInputDataList() {
        return inputDataList;
    }
    public List<ExcludeFileName> getFileList() {
        return fileList;
    }
    public List<String> getExclFileList() {
        return exclFileList;
    }
    public boolean isCustStatus() {
        return custStatus;
    }

    // Nested class created to store the raw data exclusion file list.
    public class ExcludeFileName {
        private int index;
        private String filename;
        // Machine generated code.
        public ExcludeFileName(int index, String filename) {
            this.index = index;
            this.filename = filename;
        }
        public int getIndex() {
            return index;
        }
        public void setIndex(int index) {
            this.index = index;
        }
        public String getFilename() {
            return filename;
        }
        public void setFilename(String filename) {
            this.filename = filename;
        }
    }
}
