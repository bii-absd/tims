/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.InputData;
import Clinical.Data.Sink.Database.InputDataDB;
import Clinical.Data.Sink.Database.PipelineDB;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import java.sql.SQLException;
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
 * 22-Dec-2015 - Control probe file will be having the same common name for all
 * pipelines.
 * 31-Dec-2015 - Implemented the module for reusing the input data.
 * 05-Jan-2015 - Changes in submitted_job table, removed ctrl_file and annot_
 * file fields. Added input_path field.
 * 06-Jan-2016 - Fixed the bug caused by the introduction of input_path field.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Jan-2016 - One new field user_id added in the input_data table; to 
 * identify the user who has uploaded this input data.
 * 14-Jan-2016 - Removed all the static variables in Pipeline Configuration
 * Management module.
 * 14-Jan-2016 - Deleted method getTypeList(). The pipeline type list will be 
 * retrieved from MenuBean.
 * 18-Jan-2016 - Changed the type of variable sample_average from String to
 * Boolean.
 * 20-Jan-2016 - To streamline the navigation flow and passing of pipeline name
 * from main menu to pipeline configuration pages.
 */

@ManagedBean (name="gexAffymetrixBean")
@ViewScoped
public class GEXAffymetrixBean extends ConfigBean {

    public GEXAffymetrixBean() {
        logger.debug("GEXAffymetrixBean created.");
    }
    
    @PostConstruct
    public void initFiles() {
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
            if (!(inputFile.isFilelistEmpty() || sampleFile.isFilelistEmpty())) {
                inputFile.createInputList();
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
        // SubmittedJob(job_id, study_id, user_id, pipeline_name, status_id, 
        // submit_time, chip_type, input_path, normalization, probe_filtering, 
        // probe_select, phenotype_column, summarization, output_file, 
        // sample_average, standardization, region, report) 
        SubmittedJob newJob = 
                new SubmittedJob(0, getStudyID(), userName, pipelineName, 1,
                                 submitTimeInDB, getType(), getInputPath(), 
                                 getNormalization(), probeFilter, 
                                 isProbeSelect(), getPhenotype(), "NA", 
                                 outputFilePath, isSampleAverage(), 
                                 getStdLog2Ratio(), "NA", reportFilePath);
        
        try {
            // Store the job_id of the inserted record
            job_id = SubmittedJobDB.insertJob(newJob);
        }
        catch (SQLException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert job!");
            logger.error(e.getMessage());
        }

        return result;
    }

    
    @Override
    public void saveSampleFileDetail() {
        int sn;
        
        try {
            sn = InputDataDB.getNextSn(studyID);
            // For Affymetrix, we will only store the filepath i.e. for 
            // filename, it is empty.
            InputData newdata = new InputData(studyID, userName, "", 
                    inputFile.getLocalDirectoryPath(),
                    inputFileDesc, sn, submitTimeInDB);
            InputDataDB.insertInputData(newdata);
        }
        catch (SQLException e) {
            logger.error("FAIL to insert input data detail!");
            logger.error(e.getMessage());
        }
    }
    
    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public Boolean createConfigFile() {
        if (haveNewData) {
            input = inputFile.getLocalDirectoryPath();
            sample = sampleFile.getLocalDirectoryPath() + 
                     Constants.getSAMPLE_ANNOT_FILE_NAME() + 
                     Constants.getSAMPLE_ANNOT_FILE_EXT();
        }
        else {
            input = selectedInput.getFilepath();
            sample = selectedInput.getFilepath() + 
                     Constants.getSAMPLE_ANNOT_FILE_NAME() + 
                     Constants.getSAMPLE_ANNOT_FILE_EXT();
        }
        // Call the base class method to create the Config File.        
        return super.createConfigFile();
    }
}
