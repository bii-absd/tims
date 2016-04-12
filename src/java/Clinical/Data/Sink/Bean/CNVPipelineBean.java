/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import static Clinical.Data.Sink.Bean.ConfigBean.logger;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import java.io.File;
import java.sql.SQLException;
// Libraries for Java Extension
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.naming.NamingException;

/**
 * CNVPipelineBean is used as the backing bean for the cnv-pipeline view.
 * 
 * Author: Tay Wei Hong
 * Date: 19-Jan-2016
 * 
 * Revision History
 * 19-Jan-2016 - Initial creation by extending GEXIlluminaBean. Override the
 * insertJob method.
 * 20-Jan-2016 - Changed from extending GEXIlluminaBean to GEXAffymetrixBean
 * because CNV need to support multiple input files upload.
 * 20-Jan-2016 - To streamline the navigation flow and passing of pipeline name
 * from main menu to pipeline configuration pages.
 * 18-Feb-2016 - To check the input files received with the filename listed in
 * the annotation file. List out the missing files (if any) and notice the user
 * during pipeline configuration review.
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
 */

@ManagedBean (name="cnvPBean")
@ViewScoped
public class CNVPipelineBean extends GEXAffymetrixBean {
    private FileUploadBean ctrlFile;

    public CNVPipelineBean() {
        logger.debug("CNVPipelineBean created.");
    }

    @Override
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
    public Boolean insertJob(String outputFilePath, String reportFilePath) {
        Boolean result = Constants.OK;
        // job_id will not be used during insertion, just send in any value will
        // do e.g. 0
        // Insert the new job request into datbase; job status is 1 i.e. Waiting
        // For attributes type and normalization, set them to "NA". 
        // For complete_time, set to "waiting" for the start.
        // 
        // SubmittedJob(job_id, study_id, user_id, pipeline_name, status_id, 
        // submit_time, complete_time, chip_type, input_sn, normalization, 
        // summarization, output_file, report)
        SubmittedJob newJob = 
                new SubmittedJob(0, getStudyID(), userName, pipelineName, 1,
                                 submitTimeInDB, "waiting", "NA", input_sn, 
                                 "NA", getSummarization(), outputFilePath, reportFilePath);
        
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
    public void renameAnnotCtrlFiles() {
        // Rename control probe file.
        ctrlFile.renameFilename(Constants.getCONTROL_PROBE_FILE_NAME() + 
                                Constants.getCONTROL_PROBE_FILE_EXT());
        super.renameAnnotCtrlFiles();
    }
    
    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public Boolean createConfigFile() {
        if (haveNewData) {
            ctrl = ctrlFile.getLocalDirectoryPath() + 
                   Constants.getCONTROL_PROBE_FILE_NAME() +
                   Constants.getCONTROL_PROBE_FILE_EXT();
        }
        else {
            ctrl = selectedInput.getFilepath() + 
                   Constants.getCONTROL_PROBE_FILE_NAME() +
                   Constants.getCONTROL_PROBE_FILE_EXT();
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
