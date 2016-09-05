/*
 * Copyright @2015-2016
 */
package TIMS.Bean;

import TIMS.Database.InputData;
import TIMS.Database.InputDataDB;
import TIMS.Database.SubmittedJob;
import TIMS.Database.SubmittedJobDB;
import TIMS.General.Constants;
import java.sql.SQLException;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.naming.NamingException;

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
 * boolean.
 * 20-Jan-2016 - To streamline the navigation flow and passing of pipeline name
 * from main menu to pipeline configuration pages.
 * 21-Jan-2016 - Added one new field pipeline_name in the input_data table; to
 * associate this input_data with the respective pipeline.
 * 18-Feb-2016 - To check the input files received with the filename listed in
 * the annotation file. List out the missing files (if any) and notice the user
 * during pipeline configuration review.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 24-Mar-2016 - Changes due to the new attribute (i.e. complete_time) added in
 * submitted_job table.
 * 29-Mar-2016 - Instead of storing the input path, the system will store the 
 * input SN.
 * 11-Apr-2016 - Changes due to the removal of attributes (sample_average, 
 * standardization, region and probe_select) from submitted_job table.
 * 12-Apr-2016 - Changes due to the removal of attributes (probe_filtering and
 * phenotype_column) from submitted_job table.
 * 14-Apr-2016 - Changes due to the type change (i.e. to Timestamp) for 
 * submit_time and complete_time in submitted_job table.
 * 19-May-2016 - Changes due to the addition attribute (i.e. detail_output) in 
 * submitted_job table.
 * 25-Aug-2016 - Changes due to method name (i.e. getCreateTimeString) change 
 * in InputData class.
 * 01-Sep-2016 - Changes due to the addition attribute (i.e. input_desc) in 
 * submitted_job table.
 * 05-Sep-2016 - Changes due to change in constant name.
 */

@ManagedBean (name="gexAffyBean")
@ViewScoped
public class GEXAffymetrixBean extends ConfigBean {
    protected List<String> missingFiles = null;

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
                input_sn = selectedInput.getSn();
                logger.debug("Data uploaded on " + selectedInput.getCreateTimeString() + 
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
                // Create the input filename list (Need to do first).
                inputFile.createInputList();
                // New input files are being uploaded, need to make sure the
                // application received all the input files as listed in the 
                // annotation file.
                missingFiles = inputFile.compareFileList(getAllFilenameFromAnnot());
                
                if (!missingFiles.isEmpty()) {
                    logger.debug("File(s) not received: " + missingFiles.toString());
                }
                setJobSubmissionStatus(true);
            }
        }
    }
    
    @Override
    public boolean insertJob() {
        boolean result = Constants.OK;
        // If new raw data has been uploaded, input_desc will follow the 
        // description that the user has entered.
        String input_desc = inputFileDesc;
        if (!haveNewData) {
            input_desc = selectedInput.getDescription();
        }
        // job_id will not be used during insertion, just send in any value will
        // do e.g. 0
        // Insert the new job request into datbase; job status is 1 i.e. Waiting
        // DB 2.0 - For attribute summarization, set it to "NA".
        // For complete_time, set to null for the start.
        // 
        // SubmittedJob(job_id, study_id, user_id, pipeline_name, status_id, 
        // submit_time, complete_time, chip_type, input_sn, input_desc, 
        // normalization, summarization, output_file, detail_output, report)
        SubmittedJob newJob = 
                new SubmittedJob(0, getStudyID(), userName, pipelineName, 1,
                                 submitTimeInDB, null, getType(), input_sn, 
                                 input_desc, getNormalization(), "NA", 
                                 pipelineOutput, detailOutput, pipelineReport);
        
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

    
    @Override
    public void saveSampleFileDetail() {
        try {
            input_sn = InputDataDB.getNextSn(studyID);
            // For Affymetrix, we will only store the filepath i.e. for 
            // filename, it is empty.
            InputData newdata = new InputData(studyID, userName, pipelineName,
                    "", inputFile.getLocalDirectoryPath(), inputFileDesc, 
                    input_sn, submitTimeInDB);
            InputDataDB.insertInputData(newdata);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to insert input data detail!");
            logger.error(e.getMessage());
        }
    }
    
    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public boolean createConfigFile() {
        if (haveNewData) {
            input = inputFile.getLocalDirectoryPath();
            sample = sampleFile.getLocalDirectoryPath() + 
                     Constants.getANNOT_FILE_NAME() + 
                     Constants.getANNOT_FILE_EXT();
        }
        else {
            input = selectedInput.getFilepath();
            sample = selectedInput.getFilepath() + 
                     Constants.getANNOT_FILE_NAME() + 
                     Constants.getANNOT_FILE_EXT();
        }
        // Call the base class method to create the Config File.        
        return super.createConfigFile();
    }
    
    // Return true if missing files information is available (i.e. new input
    // files uploaded and there are files not received by the system).
    public boolean getMissingFilesStatus() {
        if (missingFiles != null) {
            return !missingFiles.isEmpty();
        }
        
        return Constants.NOT_OK;
    }
    
    // Machine generated getters and setters.
    public List<String> getMissingFiles() {
        return missingFiles;
    }
    public void setMissingFiles(List<String> missingFiles) {
        this.missingFiles = missingFiles;
    }
}
