/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

// Libraries for Log4j
import Clinical.Data.Sink.Database.PipelineCommand;
import Clinical.Data.Sink.Database.PipelineCommandDB;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.General.ExitListener;
import Clinical.Data.Sink.General.ProcessExitDetector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
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
 * 13-Nov-2015 - Inital creation by refactoring from ArrayConfigBean.
 * 
 */

public abstract class ConfigBean implements Serializable {
    // Get the logger for Log4j
    protected final static Logger logger = LogManager.
            getLogger(ArrayConfigBean.class.getName());
    // Input Parameters
    private String studyID, type;
    // Common Processing Parameters. 
    private String normalization, phenotype;
    private boolean probeSelect;
    // Further Processing
    private String sampleAverage, stdLog2Ratio;
    // Pipeline name
    private static String pipelineName;
    // Pipeline type
    protected static String pipelineType;
    // Common input files that will be uploaded by the users
    protected FileUploadBean inputFile, sampleFile;
    // Record the time this job was created
    protected String submitTimeInDB, submitTimeInFilename;
    // job_id of the inserted record
    private int job_id;
    // Pipeline output, report and config filename
    protected String pipelineOutput, pipelineReport, pipelineConfig;
    // Annotation list build from Sample Annotation file
    private LinkedHashMap<String,String> annotationList = new LinkedHashMap<>();

    // Methods to be implemented by the subclass.
    abstract Boolean allowToSubmitJob();
    // Create the config file that will be used during pipeline execution.
    abstract Boolean createConfigFile();

    public void init() {
        // Create the time stamp for the pipeline job once the user enter
        // the page.
        createJobTimestamp();
        // Setup the file directory for this pipeline job.
        FileUploadBean.setFileDirectory(submitTimeInFilename);
        
        inputFile = new FileUploadBean();
        sampleFile = new FileUploadBean();
    }
    
    // Read in the subject line (i.e. first line) from the uploaded sample
    // annotation file, and build the selection list for "Phenotype Column"
    // and "Sample Averaging".
    public LinkedHashMap<String,String> getAnnotationList() {
        // Only construct the selection list if the sample annotation file has
        // been uploaded by the user and the selection list has yet to be build.
        if (!sampleFile.isFilelistEmpty() && annotationList.isEmpty()) {
            // Retrieve the sample annotation file from local drive
            File file = new File(sampleFile.getLocalDirectoryPath() +
                                sampleFile.getInputFilename());
            
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedReader br = new BufferedReader(new InputStreamReader(fis));) 
            {
                // We are only interested in the first line i.e. subject line
                String line = br.readLine();
                // All the subjects need to be separated by the TAB key
                String[] annotList = line.split("\t");
                
                for (int i = 0; i < annotList.length; i++) {
                    annotationList.put(annotList[i], annotList[i]);
                }
                logger.debug("Annotation subject line: " + line);
                logger.debug("Annotation List: " + annotationList.toString());
            }
            catch (IOException e) {
                logger.debug("IOException when reading the first line of annotation file.");
                getFacesContext().addMessage(null, new FacesMessage(
                                FacesMessage.SEVERITY_ERROR,
                                "System failed to create annotation list!", ""));
            }
        }
        return annotationList;
    }
    
    // The enabled/disabled status of the 'Phenotype Column' and 'Sample
    // Averaging' will depend on whether the annotation list is constructed 
    // or not.
    public Boolean isAnnotationListReady() {
        return annotationList.isEmpty();
    }
    
    // After reviewing the configuration, if user decided to proceed with 
    // the pipeline execution and click on Confirm button, submitJob will be
    // called. A series of operations will then occur:
    // 1. Insert the new job request into the database
    // 2. Create the Config file
    // 3. Start the pipeline execution
    public String submitJob() {
        logger.info(AuthenticationBean.getUserName() + ": started " +
                    pipelineName);

        String result = Constants.MAIN_PAGE;
        String outputDir = AuthenticationBean.getHomeDir() + 
                Constants.getOUTPUT_PATH();
        String logDir = AuthenticationBean.getHomeDir() + 
                Constants.getLOG_PATH();
        // outputFilePath will store the full path name to the output file
        // that will be generated by the pipeline. The path will also be
        // stored in submitted_job table.
        String outputFilePath = outputDir + Constants.getOUTPUTFILE_NAME() + 
                                getStudyID() + "-" + submitTimeInFilename +
                                Constants.getOUTPUTFILE_EXT();
        String reportFilePath = outputDir + Constants.getREPORTFILE_NAME() + 
                                getStudyID() + "-" + submitTimeInFilename +
                                Constants.getREPORTFILE_EXT();
        // logFilePath will store the log of the pipeline execution (For debug
        // purpose).
        String logFilePath = logDir + Constants.getLOGFILE_NAME() +
                             getStudyID() + "-" + submitTimeInFilename;

        // The purpose of creating these File objects is to get the absolute
        // path of the output and report files for configuration input.
        File output = new File(outputFilePath);
        File report = new File(reportFilePath);
        pipelineOutput = output.getAbsolutePath();
        pipelineReport = report.getAbsolutePath();

        logger.debug("Pipeline output file: " + pipelineOutput);
        logger.debug("Pipeline report file: " + pipelineReport);

        // Step to follow for pipeline execution:
        // 1. Create Config file
        // 2. Insert new job request into database
        // 3. Update job status to in-progress
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
        if (createConfigFile() && insertJob(outputFilePath,reportFilePath)) {
            SubmittedJobDB.updateJobStatusToInprogress(job_id);
            logger.debug("Pipeline run and job status updated to in-progress. ID: " 
                        + job_id);
            // 3. Pipeline is ready to be run now
            result = executePipeline(logFilePath);
            // Create dummy pipeline files for user to download.
//            createDummyFile(outputFilePath);
//            createDummyFile(reportFilePath);
        }
        else {
            result = Constants.ERROR;
        }
        
        return result;
    }
    
    // After reviewing the configuration, user decided not to proceed with
    // the pipeline execution.
    public void cancelJob() {
        logger.info(AuthenticationBean.getUserName() + 
                ": decided not to proceed with " + pipelineName);
    }
    
    // Lock down and record the time when the user enter the GEX page.
    private void createJobTimestamp() {
        // inputFilesDir is used to create the directory for input files 
        // i.e. cannot have space in its format.
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        submitTimeInFilename = dateFormat.format(new Date());
        // submitTimeInDB will be stored in the submitted_job table and will
        // be display to the user in Job Status page.
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        submitTimeInDB = dateFormat.format(new Date());   
    }
    
    // Insert the current job request into the submitted_job table. The status
    // of the insertion operation will be return.
    private Boolean insertJob(String outputFilePath, String reportFilePath) {
        Boolean result = Constants.OK;
        // job_id will not be used during insertion, just send in any value will
        // do e.g. 0
        // Insert the new job request into datbase; job status is 1 i.e. Waiting
        SubmittedJob newJob = new SubmittedJob(0, 1, 
                                               getStudyID(), 
                                               submitTimeInDB, 
                                               outputFilePath, 
                                               reportFilePath);
        try {
            // Store the job_id of the inserted record
            job_id = SubmittedJobDB.insertJob(newJob);
            // insertJob will return the job_id of the inserted job request
            // NOTE: need to keep the job_id for further processing. KIV
            logger.info(AuthenticationBean.getUserName() + 
                    ": insert new job request into database. ID: " + job_id);
        }
        catch (SQLException e) {
            logger.error(AuthenticationBean.getUserName() + 
                    ": encountered SQLException at insertJob.");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        // The insert operation will have failed if the control reaches here.
        return result;
    }

    // Execute the pipeline together with the config file, the message 
    // generated by the pipeline will be directed to the pipelineOutput file.
    private String executePipeline(String pipelineOutput) {
        String result = Constants.MAIN_PAGE;
        // Build the pipeline command
        List<String> command = new ArrayList<>();
        PipelineCommand cmd = null;
        
        try {
            // Retrieve the pipeline command and it's parameter from database.
            cmd = PipelineCommandDB.getCommand(pipelineType);

            logger.debug("Pipeline from database: " + cmd.toString());
        }
        catch (SQLException e) {
            logger.error("SQLException while retriving pipeline command " +
                    pipelineType);
            logger.error(e.getMessage());
            // Something is wrong, shouldn't let the user continue.
            return Constants.ERROR;
        }
        
        command.add(cmd.getCommand_code());
        command.add(cmd.getCommand_para());
        command.add(pipelineConfig);
        
        logger.debug("Full pipeline command: " + command.toString());
        
        ProcessBuilder pb = new ProcessBuilder(command);
        // This outputFilePath might be use as the execution log from the pipeline.
        File output = new File(pipelineOutput);
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
            logger.error("IOException at executePipeline.");
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
                logger.error("Pipeline completed before detector is create.");
            }            
        } else {
            logger.error("Unable to start Pipeline process.");
            result = Constants.ERROR;
        }
        
        return result;
    }

    // booleanToYesNo helped to convert boolean (i.e. true/false) to yes/no
    protected String booleanToYesNo(boolean parameter) {
        return parameter?"yes":"no";
    }

    // Function to create the output/report file for testing purposes.
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
            logger.error("IOException at createDummyFile.");
            logger.error(e.getMessage());
        }
    }

    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
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
    public static String getPipelineType() {
        return pipelineType;
    }
    public static void setPipelineType(String pipelineType) {
        ConfigBean.pipelineType = pipelineType;
    }
    public String getPipelineName() {
        return pipelineName;
    }    
    public static void setPipelineName(String pipelineName) {
        ConfigBean.pipelineName = pipelineName;
    }
    public String getStudyID() {
        return studyID;
    }
    public void setStudyID(String studyID) {
        this.studyID = studyID;
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
    public String getPhenotype() {
        return phenotype;
    }
    public void setPhenotype(String phenotype) {
        this.phenotype = phenotype;
    }
    public boolean isProbeSelect() {
        return probeSelect;
    }
    public void setProbeSelect(boolean probeSelect) {
        this.probeSelect = probeSelect;
    }
    public String getSampleAverage() {
        return sampleAverage;
    }
    public void setSampleAverage(String sampleAverage) {
        this.sampleAverage = sampleAverage;
    }
    public String getStdLog2Ratio() {
        return stdLog2Ratio;
    }
    public void setStdLog2Ratio(String stdLog2Ratio) {
        this.stdLog2Ratio = stdLog2Ratio;
    }
}
