/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.PipelineCommandDB;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.General.SelectOneMenuList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * GEXIlluminaBean is used as the backing bean for the gex-illumina view.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Initial creation by refactoring from ArrayConfigBean.
 * 18-Nov-2015 - override the abstract method updateJobSubmissionStatus(), and 
 * removed the abstract method allowToSubmitJob().
 * 25-Nov-2015 - Renamed pipelineType to pipelineTech. Implementation for 
 * database 2.0
 */

@ManagedBean (name="gexIlluminaBean")
@ViewScoped
public class GEXIlluminaBean extends ConfigBean {
    private FileUploadBean ctrlFile;
    private List<String> probeFilters;
    private String probeFilter;

    public GEXIlluminaBean() {
        pipelineName = Constants.GEX_ILLUMINA;
        pipelineTech = PipelineCommandDB.getPipelineTechnology(pipelineName);

        logger.debug("GEXIlluminaBean created.");
    }
    
    @PostConstruct
    public void initFiles() {
        ctrlFile = new FileUploadBean();            
        init();
    }
    
    @Override
    public Boolean createConfigFile() {
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
            input = inputFile.getLocalDirectoryPath() +
                    inputFile.getInputFilename();
            ctrl = ctrlFile.getLocalDirectoryPath() + 
                   ctrlFile.getInputFilename();
            sample = sampleFile.getLocalDirectoryPath() + 
                     sampleFile.getInputFilename();
            probeFilter = Constants.NONE;
            // Create config file
            configFile.createNewFile();

            if (probeFilters.size() > 0) {
                probeFilter = probeFilters.get(0);
                for (int i = 1; i < probeFilters.size(); i++) {
                    probeFilter += ";";
                    probeFilter += probeFilters.get(i);
                }
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
                     "\nPROBE_Filtering\t=\t" + probeFilter +
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
                        ": encountered error when writing pipeline config file.");

            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    @Override
    public void updateJobSubmissionStatus() {
        // Only update the jobSubmissionStatus if all the input files are 
        // uploaded.        
        if (!(inputFile.isFilelistEmpty() || sampleFile.isFilelistEmpty() ||
              ctrlFile.isFilelistEmpty())) {
            setJobSubmissionStatus(true);            
        }
    }

    @Override
    public Boolean insertJob(String outputFilePath, String reportFilePath) {
        Boolean result = Constants.OK;
        // job_id will not be used during insertion, just send in any value will
        // do e.g. 0
        // Insert the new job request into datbase; job status is 1 i.e. Waiting
        // DB 2.0 - For attributes summarization and region, set them to "NA".
        SubmittedJob newJob = 
                new SubmittedJob(0, getStudyID(), pipelineName, 1,
                                 submitTimeInDB, getType(), ctrl, sample, 
                                 getNormalization(), probeFilter, 
                                 isProbeSelect(), getPhenotype(), "NA", 
                                 outputFilePath, getSampleAverage(), 
                                 getStdLog2Ratio(), "NA", reportFilePath);
        
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

    // Return the list of Illumina type.
    public LinkedHashMap<String,String> getTypeList() {
        return SelectOneMenuList.getIlluminaTypeList();
    }
    
    // Machine generated getters and setters
    public FileUploadBean getCtrlFile() {
        return ctrlFile;
    }
    public void setCtrlFile(FileUploadBean ctrlFile) {
        this.ctrlFile = ctrlFile;
    }
    public List<String> getProbeFilters() {
        return probeFilters;
    }
    public void setProbeFilters(List<String> probeFilters) {
        this.probeFilters = probeFilters;
    }
}
