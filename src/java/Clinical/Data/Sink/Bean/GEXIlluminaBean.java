/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import static Clinical.Data.Sink.Bean.ConfigBean.studyID;
import Clinical.Data.Sink.Database.InputData;
import Clinical.Data.Sink.Database.InputDataDB;
import Clinical.Data.Sink.Database.PipelineDB;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.General.SelectOneMenuList;
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
 * 02-Dec-2015 - Streamline the createConfigFile method.
 * 16-Dec-2015 - Implemented the new abstract method saveSampleFileDetail(). 
 * Sample annotation file will be having the same common name for all pipelines.
 * 22-Dec-2015 - Control probe file will be having the same common name for all
 * pipelines.
 * 31-Dec-2015 - Implemented the module for reusing the input data.
 */

@ManagedBean (name="gexIlluminaBean")
@ViewScoped
public class GEXIlluminaBean extends ConfigBean {
    private FileUploadBean ctrlFile;
    private List<String> probeFilters;

    public GEXIlluminaBean() {
        pipelineName = Constants.GEX_ILLUMINA;
        pipelineTech = PipelineDB.getPipelineTechnology(pipelineName);

        logger.debug("GEXIlluminaBean created.");
    }
    
    @PostConstruct
    public void initFiles() {
        ctrlFile = new FileUploadBean();            
        init();
    }
    
    @Override
    public void updateJobSubmissionStatus() {
        if (!haveNewData) {
            // Only update the jobSubmissionStatus if data has been selected 
            // for reuse.
            if (selectedInput != null) {
                setJobSubmissionStatus(true);
                logger.debug("Data uploaded on " + selectedInput.getDate() + 
                             " has been selected for reuse.");
            }
            else {
                // No data is being selected for reuse, display error message.
                logger.debug("No data selected for reuse!");
            }
        }
        else {
            // Only update the jobSubmissionStatus if all the input files are 
            // uploaded.
            if (!(inputFile.isFilelistEmpty() || sampleFile.isFilelistEmpty() ||
                ctrlFile.isFilelistEmpty())) {
                setJobSubmissionStatus(true);            
            }
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
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting job.");
            logger.error(e.getMessage());
            result = Constants.NOT_OK;
        }
        // The insert operation will have failed if the control reaches here.
        return result;
    }

    @Override
    public void saveSampleFileDetail() {
        int sn;
        
        try {
            sn = InputDataDB.getNextSn(studyID);
            // For Affymetrix, we will only store the filepath.
            InputData newdata = new InputData(studyID, 
                    inputFile.getInputFilename(), 
                    inputFile.getLocalDirectoryPath(),
                    inputFileDesc, sn, submitTimeInDB);
            InputDataDB.insertInputData(newdata);
        }
        catch (SQLException e) {
            logger.error("Failed to insert input data detail!");
            logger.error(e.getMessage());
        }
    }

    @Override
    public void renameAnnotCtrlFiles() {
        ctrlFile.renameCtrlProbeFile();
        super.renameAnnotCtrlFiles();
    }
    
    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public Boolean createConfigFile() {
        if (haveNewData) {
            input = inputFile.getLocalDirectoryPath() + inputFile.getInputFilename();
            ctrl = ctrlFile.getLocalDirectoryPath() + 
                   Constants.getCONTROL_PROBE_FILE_NAME() +
                   Constants.getCONTROL_PROBE_FILE_EXT();
            sample = sampleFile.getLocalDirectoryPath() + 
                     Constants.getSAMPLE_ANNOT_FILE_NAME() + 
                     Constants.getSAMPLE_ANNOT_FILE_EXT();
        }
        else {
            input = selectedInput.getFilepath() + selectedInput.getFilename();
            ctrl = selectedInput.getFilepath() + 
                   Constants.getCONTROL_PROBE_FILE_NAME() +
                   Constants.getCONTROL_PROBE_FILE_EXT();
            sample = selectedInput.getFilepath() + 
                     Constants.getSAMPLE_ANNOT_FILE_NAME() + 
                     Constants.getSAMPLE_ANNOT_FILE_EXT();
        }
        probeFilter = Constants.NONE;

        if (probeFilters.size() > 0) {
            probeFilter = probeFilters.get(0);

            for (int i = 1; i < probeFilters.size(); i++) {
                probeFilter += ";";
                probeFilter += probeFilters.get(i);
            }
        }
        // Call the base class method to create the Config File.
        return super.createConfigFile();
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
