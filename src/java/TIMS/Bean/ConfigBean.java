/*
 * Copyright @2015-2016
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.InputData;
import TIMS.Database.InputDataDB;
import TIMS.Database.Pipeline;
import TIMS.Database.PipelineDB;
import TIMS.Database.SubmittedJobDB;
import TIMS.Database.UserAccountDB;
import TIMS.Database.UserRoleDB;
import TIMS.General.Constants;
import TIMS.General.ExitListener;
import TIMS.General.Postman;
import TIMS.General.ProcessExitDetector;
import TIMS.General.ResourceRetriever;
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
 */

public abstract class ConfigBean implements Serializable {
    // Get the logger for Log4j
    protected final static Logger logger = LogManager.
            getLogger(ConfigBean.class.getName());
    protected String studyID, commandLink;
    // Common Processing Parameters. 
    protected String type, normalization, summarization;
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
    // Pipeline input, control and sample annotation filename
    protected String input, ctrl, sample;
    // List of input data belonging to the study, that are available for reuse.
    private List<InputData> inputDataList = new ArrayList<>();
    protected InputData selectedInput;
    // Store the user ID and home directory of the current user.
    protected String userName, homeDir;

    // This method will only be trigger if all the inputs validation have 
    // passed after the user clicked on Submit. As a result of this behaviour 
    // (i.e. all the validations need to pass), we will check whether all the 
    // input files have been uploaded and update the jobSubmissionStatus here.
    abstract void updateJobSubmissionStatus();
    // Insert the current job request into the submitted_job table. The status
    // of the insertion operation will be return.
    abstract boolean insertJob();
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
        // Setup local variables using the setting retrieved from session map.
        pipelineTech = PipelineDB.getPipelineTechnology(pipelineName);
        commandLink = ResourceRetriever.getMsg(pipelineName);
        homeDir = Constants.getSYSTEM_PATH() + Constants.getUSERS_PATH() + userName;
        jobSubmissionStatus = false;
        input_sn = Constants.DATABASE_INVALID_ID;
        // Create the time stamp for the pipeline job.
        createJobTimestamp();
        
        if (haveNewData) {
            String dir = Constants.getSYSTEM_PATH() + Constants.getINPUT_PATH() 
                       + studyID + File.separator 
                       + submitTimeInFilename + File.separator;
            inputFile = new FileUploadBean(dir);
            sampleFile = new FileUploadBean(dir);
        }
        else {
            inputDataList = InputDataDB.getIpList(studyID, pipelineName);
        }
        logger.debug(studyID + " ConfigBean - init().");
    }
    
    // Rename the sample annotation file (and control probe file) to a common 
    // name for future use.
    public void renameAnnotCtrlFiles() {
        // Rename sample annotation file.
        sampleFile.renameFilename(Constants.getSAMPLE_ANNOT_FILE_NAME() + 
                                  Constants.getSAMPLE_ANNOT_FILE_EXT());
    }
    
    // Read in all the filename listed in the annotation file.
    public List<String> getAllFilenameFromAnnot() {
        List<String> filenameList = new ArrayList<>();
        String[] content;
        
        try (BufferedReader br = new BufferedReader(
                                 new FileReader(sampleFile.getLocalDirectoryPath() + 
                                                sampleFile.getInputFilename())))
        {
            // First line is the header; not needed here.
            String lineRead = br.readLine();
            // Start processing from the second line.
            while ((lineRead = br.readLine()) != null) {
                content = lineRead.split("\t");
                // The second column is the filename, store it.
                filenameList.add(content[1]);
            }
            logger.debug("All filename read from annotation file.");
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
                // Only perform the following steps if these are new data.
                // Save the Sample File detail into database
                saveSampleFileDetail();
                // Rename sample annotation file (and control probe file) to a
                // common name for future use.
                renameAnnotCtrlFiles();
                // Update the input SN for this job.
                SubmittedJobDB.updateJobInputSN(job_id, input_sn);
            }
            // 4. Pipeline is ready to be run now
            result = executePipeline(logFilePath);
            // Create dummy pipeline files for user to download.
            createDummyFile(pipelineOutput);
            createDummyFile(detailOutput);
            
            // Record this pipeline execution activity into database.
            ActivityLogDB.recordUserActivity(userName, Constants.EXE_PL, 
                                             pipelineName);
        }
        else {
            result = Constants.ERROR;
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
        Pipeline cmd;
        
        try {
            // Retrieve the pipeline command and it's parameter from database.
            cmd = PipelineDB.getPipeline(pipelineName);

            logger.debug("Pipeline from database: " + cmd.toString());
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
        
        logger.debug("Full pipeline command: " + command.toString());
        
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
        } else {
            logger.error("FAIL to start Pipeline process!");
            result = Constants.ERROR;
        }
        
        return result;
    }

    // Create the config file that will be used during pipeline execution.
    protected boolean createConfigFile() {
        boolean result = Constants.OK;
        String configDir = homeDir + Constants.getCONFIG_PATH();
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
                     "\nTYPE\t=\t" + getType() +
                     "\nINPUT\t=\t" + input +
                     "\nCTRL_FILE\t=\t" + ctrl +
                     "\nSAMPLES_ANNOT_FILE\t=\t" + sample + "\n\n");

            fw.write("### PROCESSING parameters\n" +
                     "NORMALIZATION\t=\t" + getNormalization() +
                     "\nSUMMARIZATION\t=\t" + getSummarization() + "\n\n");

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
    
    // The setter for the following few fields will not be available for use.
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
}
