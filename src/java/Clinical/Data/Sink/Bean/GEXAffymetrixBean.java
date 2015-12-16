/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.InputData;
import Clinical.Data.Sink.Database.InputDataDB;
import Clinical.Data.Sink.Database.PipelineDB;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.General.SelectOneMenuList;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * GEXAffymetrixBean is used as the backing bean for the gex-affymetrix view.
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
 */

@ManagedBean (name="gexAffymetrixBean")
@ViewScoped
public class GEXAffymetrixBean extends ConfigBean {

    public GEXAffymetrixBean() {
        pipelineName = Constants.GEX_AFFYMETRIX;
        pipelineTech = PipelineDB.getPipelineTechnology(pipelineName);
        
        logger.debug("GEXAffymetrixBean created.");
    }
    
    @PostConstruct
    public void initFiles() {
        init();
    }

    @Override
    public void updateJobSubmissionStatus() {
        // Only update the jobSubmissionStatus if all the input files are 
        // uploaded.
        if (!(inputFile.isFilelistEmpty() || sampleFile.isFilelistEmpty())) {
            inputFile.createInputList();
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
            logger.info("New job request inserted. ID: " + job_id);
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
            // Need to include a description field for input.
            InputData newdata = new InputData(studyID, "", 
                    inputFile.getLocalDirectoryPath(),
                    inputFileDesc, sn, submitTimeInDB);
            InputDataDB.insertInputData(newdata);
        }
        catch (SQLException e) {
            logger.error("Failed to insert input data detail!");
            logger.error(e.getMessage());
        }
    }

    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public Boolean createConfigFile() {
        input = inputFile.getLocalDirectoryPath();
        sample = sampleFile.getLocalDirectoryPath() + 
                 Constants.getSAMPLE_ANNOT_FILE_NAME() + 
                 Constants.getSAMPLE_ANNOT_FILE_EXT();
        // Call the base class method to create the Config File.        
        return super.createConfigFile();
    }
    
    // Return the list of Affymetrix type.
    public LinkedHashMap<String,String> getTypeList() {
        return SelectOneMenuList.getAffymetrixTypeList();
    }
}
