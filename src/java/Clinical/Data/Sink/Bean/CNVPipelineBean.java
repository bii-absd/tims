/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import static Clinical.Data.Sink.Bean.ConfigBean.logger;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import java.sql.SQLException;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

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
 */

@ManagedBean (name="cnvPBean")
@ViewScoped
public class CNVPipelineBean extends GEXAffymetrixBean {
    private FileUploadBean ctrlFile;

    public CNVPipelineBean() {
        logger.debug("CNVPipelineBean created.");
    }
    
    @PostConstruct
    @Override
    public void initFiles() {
        init();
        if (haveNewData) {
            ctrlFile = new FileUploadBean(studyID, submitTimeInFilename);            
        }
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
        // For attributes type, normalization, probeFilter, StdLog2Ratio and 
        // region, set them to "NA". For probeSelect and sample_average, set 
        // them to false.
        // SubmittedJob(job_id, study_id, user_id, pipeline_name, status_id, 
        // submit_time, chip_type, input_path, normalization, probe_filtering, 
        // probe_select, phenotype_column, summarization, output_file, 
        // sample_average, standardization, region, report) 

        SubmittedJob newJob = 
                new SubmittedJob(0, getStudyID(), userName, pipelineName, 1,
                                 submitTimeInDB, "NA", getInputPath(), 
                                 "NA", "NA", false, getPhenotype(), 
                                 getSummarization(), outputFilePath, false, 
                                 "NA", "NA", reportFilePath);
        
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
    public void renameAnnotCtrlFiles() {
        ctrlFile.renameCtrlProbeFile();
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
