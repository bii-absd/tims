/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.General.ExitListener;
import Clinical.Data.Sink.General.ProcessExitDetector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.ProcessBuilder.Redirect;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ArrayConfigBean is used as the backing bean for the array.xhtml and 
 arrayConfigReview.xhtml views.
 * 
 * Author: Tay Wei Hong
 * Date: 22-Sep-2015
 * 
 * Revision History
 * 22-Sep-2015 - Created with all the standard getters and setters.
 * 30-Sep-2015 - Added in the methods for reading in the user inputs for the
 * pipeline configuration.
 * 02-Oct-2015 - Added in 2 extra variables to store the submit time. Refactor
 * the methods involved in printing config, excute, and submit. Added in the
 * comments for the code.
 * 05-Oct-2015 - Create the directory for the output file if this is a new user.
 * Added in the temporary code to create the output file.
 * 06-Oct-2015 - To keep the job_id for all newly inserted job request (needed
 * for further processing). Added Log4j2 for this class. Fix a bug found when
 * testing whether inputFile is null or not.
 * 07-Oct-2015 - Added in 3 new variables (type, filterType & columnAverage) to
 * capture the user configuration inputs.
 * 08-Oct-2015 - Store the job_id of the inserted record. To handle the status
 * returned by the helper functions during executeJob. Removed qualityPlot and 
 * "Differential expression analysis" heading. Moved column to "Processing
 * Parameters".
 * 09-Oct-2015 - Added one new method executePipeline. Added in the code to
 * simulate the running of pipeline.
 * 12-Oct-2015 - Added one helper method createSystemDirectory to help create
 * the directory needed for storing system output file. Change the way the 
 * output file is being created (will created it based on the output from the 
 * process). Log the exception message. Implemented pipeline process exit 
 * detection module. Added job_id field during insertion into database.
 * 15-Oct-2015 - Critical error handling.
 * 21-Oct-2015 - Pipeline command(s) will be read from setup file. Changed the
 * system directory structure.
 * 22-Oct-2015 - Removed the Vendor variable from the config file. Will keep the
 * variable in this class for now.
 * 23-Oct-2015 - Removed the genReport,mpsFile & probeAntFile variables from the 
 * config file (will keep these variable in this class for now). Added 
 * pipelineReport to store the filepath of the pipeline report that will be 
 * generated. Pipeline report module.
 * 02-Nov-2015 - Full porting to JSF 2.2 completed.
 * 03-Nov-2015 - Fixed the Probe Filtering text issue.
 * 05-Nov-2015 - Use the sample annotation file uploaded to construct the 
 * selection list for 'Phenotype Column' and 'Sample Averaging'.
 */

@ManagedBean (name="arrayConfigBean")
@ViewScoped
public class ArrayConfigBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ArrayConfigBean.class.getName());
    // Input Parameters
    private String studyID, type;
    // Processing Parameters. ProbeFilter is use for Affymetrix, while
    // ProbeFilters is use for Illumina.
    private String probeFilter, normalization, phenotype;
    private List<String> probeFilters;
    private boolean probeSelect;
    // Further Processing
    private String sampleAverage, stdLog2Ratio;
    // Pipeline name
    private static String pipelineName;
    // Pipeline type
    private static String pipelineType;
    // To store the 3 kind of input files that will be uploaded by the users
    private FileUploadBean inputFile, sampleFile, ctrlFile;
    // Record the time this job was created
    private String submitTimeInDB, submitTimeInFilename;
    // job_id of the inserted record
    private int job_id;
    // Pipeline output, report and config filename
    private String pipelineOutput, pipelineReport, pipelineConfig;
    // Annotation list build from Sample Annotation file
    private LinkedHashMap<String,String> annotationList = new LinkedHashMap<>();

    public ArrayConfigBean() {
        logger.debug("ArrayConfigBean created.");
    }

    @PostConstruct
    public void initFiles() {
        // Create the time stamp for the pipeline job once the user enter
        // the page.
        createJobTimestamp();
        // Setup and create the input files directory for every entry to
        // GEX pipeline.
        if (!FileUploadBean.setFileDirectory(submitTimeInFilename)) {
            // System failed to create the input files directory for this job,
            // shouldn't allow the user to continue.
            logger.error("Failed to create the input files directory");            
        }
        
        if (pipelineType.compareTo(Constants.GEX_ILLUMINA) == 0) {
            // Only Illumina pipeline required the user to upload the 
            // control file
            ctrlFile = new FileUploadBean();            
        }
        inputFile = new FileUploadBean();
        sampleFile = new FileUploadBean();
    }
    
    // Read in the subject line (i.e. first line) from the uploaded sample
    // annotation file, and build the selection list for "Phenotype Column"
    // and "Sample Averaging".
    public LinkedHashMap<String,String> getAnnotationList() {
        // Only construct the selection list if the sample annotation file has
        // been uploaded by the user and the selection list has yet to be build.
        if (!sampleFile.filelistIsEmpty() && annotationList.isEmpty()) {
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
                logger.debug("Annotation List: " + annotationList.toString());
            }
            catch (IOException e) {
                logger.debug("IOException when reading the first line of annotation file.");
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
    // If any of the input files is not uploaded, user is not allowed to
    // submit the job for execution.
    public Boolean allowToSubmitJob() {
        if (pipelineType.compareTo(Constants.GEX_ILLUMINA) == 0) {
            return !(inputFile.filelistIsEmpty() ||
                     sampleFile.filelistIsEmpty() ||
                     ctrlFile.filelistIsEmpty());
        } else {
            return !(inputFile.filelistIsEmpty() ||
                     sampleFile.filelistIsEmpty());
        }
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
            createDummyFile(outputFilePath);
            createDummyFile(reportFilePath);
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
    
    // Create the config file that will be used by the pipeline during 
    // execution.
    private Boolean createConfigFile() {
        Boolean result = Constants.OK;
        String configDir = AuthenticationBean.getHomeDir() +
                Constants.getCONFIG_PATH();
        // configFile will be send to the pipeline during execution
        File configFile = new File(configDir + 
                Constants.getCONFIG_FILE_NAME() + submitTimeInFilename + 
                Constants.getCONFIG_FILE_EXT());

        pipelineConfig = configFile.getAbsolutePath();
        logger.debug("Pipeline config file: " + pipelineConfig);

        try (FileWriter fw = new FileWriter(configFile)) {
            String input = inputFile.getLocalDirectoryPath();
            String ctrl = " ";
            String sample = sampleFile.getLocalDirectoryPath() + 
                            sampleFile.getInputFilename();
            String probeFiltering;
            // Create the config file
            configFile.createNewFile();

            if (pipelineType.compareTo(Constants.GEX_ILLUMINA) == 0) {
                input += inputFile.getInputFilename();
                ctrl = ctrlFile.getLocalDirectoryPath() + 
                       ctrlFile.getInputFilename();
                
                if (probeFilters.size() == 0) {
                    probeFiltering = Constants.NONE;
                }
                else {
                    probeFiltering = probeFilters.get(0);
                    for (int i = 1; i < probeFilters.size(); i++) {
                        probeFiltering += ";";
                        probeFiltering += probeFilters.get(i);
                    }
                }
            }
            else {
                probeFiltering = probeFilter;
            }
            
            // Write to the config file according to the format needed 
            // by the pipeline.
            fw.write("### INPUT parameters\n" +
                     "STUDY_ID\t=\t" + getStudyID() + "_" + submitTimeInFilename +
                     "\nTYPE\t=\t" + getType() +
                     "\nINPUT_FILE\t=\t" + input +
                     "\nCTRL_FILE\t=\t" + ctrl +
                     "\nSAMPLES_ANNOT_FILE\t=\t" + sample + "\n\n");

            fw.write("### PROCESSING parameters\n" +
                     "NORMALIZATION\t=\t" + getNormalization() +
                     "\nPROBE_Filtering\t=\t" + probeFiltering +
                     "\nPROBE_SELECTION\t=\t" + booleanToYesNo(isProbeSelect()) +
                     "\nPHENOTYPE_COLUMN\t=\t" + getPhenotype() + "\n\n");

            fw.write("### Output file after normalization and processing\n" +
                     "OUTPUT\t=\t" + pipelineOutput + "\n\n");

            fw.write("### Further Processing\n" +
                     "SAMPLE_AVERAGING\t=\t" + getSampleAverage() +
                     "\nSTANDARDIZATION\t=\t" + getStdLog2Ratio() + "\n\n");

            fw.write("### Report Generation\n" +
                     "REP_FILENAME\t=\t" + pipelineReport + "\n\n");

            logger.debug("Pipeline config file created at " + pipelineConfig);
        }
        catch (IOException e) {
            logger.error(AuthenticationBean.getUserName() + 
                        ": encountered error when writing array config file.");

            result = Constants.NOT_OK;
        }
        
        return result;
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
        
        if (Constants.getCOMMAND1().compareTo("NA") != 0) {
            command.add(Constants.getCOMMAND1());
        }
        
        command.add(Constants.getCOMMAND2());
        command.add(pipelineConfig);
        
        logger.debug("Pipeline command: " + command.toString());
        
        ProcessBuilder pb = new ProcessBuilder(command);
        // This outputFilePath might be use as the execution log from the pipeline.
        File output = new File(pipelineOutput);
        // Merge the standard error and output stream, and always sent to
        // the same destination
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.to(output));
        Process process = null;
        
        try {
            // Start the pipeline
            process = pb.start();
        } catch (IOException ioe) {
            logger.error("IOException at executePipeline.");
            logger.error(ioe.getMessage());
            result = Constants.ERROR;
        }

        if (process != null) {
            // Start a listener thread to detect the complete status of 
            // the process
            try {
                ProcessExitDetector exitDetector = new ProcessExitDetector(
                                    job_id, process, new ExitListener());
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
    private String booleanToYesNo(boolean parameter) {
        if (parameter) {
            return "yes";
        }
        else {
            return "no";
        }
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

    // Machine generated getters and setters
    public FileUploadBean getCtrlFile() {
        return ctrlFile;
    }
    public void setCtrlFile(FileUploadBean ctrlFile) {
        this.ctrlFile = ctrlFile;
    }
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
        ArrayConfigBean.pipelineType = pipelineType;
    }
    public String getPipelineName() {
        return pipelineName;
    }    
    public static void setPipelineName(String pipelineName) {
        ArrayConfigBean.pipelineName = pipelineName;
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
    public String getProbeFilter() {
        return probeFilter;
    }
    public void setProbeFilter(String probeFilter) {
        this.probeFilter = probeFilter;
    }
    public List<String> getProbeFilters() {
        return probeFilters;
    }
    public void setProbeFilters(List<String> probeFilters) {
        this.probeFilters = probeFilters;
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
