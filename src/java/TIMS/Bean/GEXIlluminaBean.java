/*
 * Copyright @2015-2016
 */
package TIMS.Bean;

import TIMS.Database.InputData;
import TIMS.Database.InputDataDB;
import TIMS.Database.SubmittedJob;
import TIMS.Database.SubmittedJobDB;
import TIMS.General.Constants;
import java.io.File;
import java.sql.SQLException;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.naming.NamingException;

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
 * 19-Jan-2016 - Initialize the variable probeFilters in the constructor. 
 * 20-Jan-2016 - To streamline the navigation flow and passing of pipeline name
 * from main menu to pipeline configuration pages.
 * 21-Jan-2016 - Added one new field pipeline_name in the input_data table; to
 * associate this input_data with the respective pipeline.
 * 19-Feb-2016 - To use the new generic method renameFilename in FileUploadBean
 * class when renaming annotation and control files. To use the new generic
 * constructor in FileUploadBean class when creating new object.
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
 */

@ManagedBean (name="gexIlluBean")
@ViewScoped
public class GEXIlluminaBean extends ConfigBean {
    private FileUploadBean ctrlFile;

    public GEXIlluminaBean() {
        logger.debug("GEXIlluminaBean created.");
    }
    
    @PostConstruct
    public void initFiles() {
        init();
        if (haveNewData) {
            String dir = Constants.getSYSTEM_PATH() + Constants.getINPUT_PATH() 
                       + studyID + File.separator 
                       + submitTimeInFilename + File.separator;
            ctrlFile = new FileUploadBean(dir);            
        }
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
            if (!(inputFile.isFilelistEmpty() || sampleFile.isFilelistEmpty() ||
                ctrlFile.isFilelistEmpty())) {
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
            // Insert a new record into input_data table.
            InputData newdata = new InputData(studyID, userName, pipelineName,
                    inputFile.getInputFilename(), 
                    inputFile.getLocalDirectoryPath(),
                    inputFileDesc, input_sn, submitTimeInDB);
            InputDataDB.insertInputData(newdata);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to insert input data detail!");
            logger.error(e.getMessage());
        }
    }

    @Override
    public void renameAnnotCtrlFiles() {
        // Rename control probe file.
        ctrlFile.renameFilename(Constants.getCONTROL_PROBE_FILE_NAME() + 
                                Constants.getCONTROL_PROBE_FILE_EXT());
        super.renameAnnotCtrlFiles();
    }
    
    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public boolean createConfigFile() {
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

        // Call the base class method to create the Config File.
        return super.createConfigFile();
    }

    // Machine generated getters and setters
    public FileUploadBean getCtrlFile() {
        return ctrlFile;
    }
    public void setCtrlFile(FileUploadBean ctrlFile) {
        this.ctrlFile = ctrlFile;
    }
}
