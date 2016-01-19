/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.PipelineDB;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import java.sql.SQLException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * METHPipelineBean is used as the backing bean for the meth-pipeline view.
 * 
 * Author: Tay Wei Hong
 * Date: 11-Jan-2016
 * 
 * Revision History
 * 11-Jan-2016 - Initial creation by extending GEXAffymetrixBean. Override the
 * insertJob method.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 14-Jan-2016 - Removed all the static variables in Pipeline Configuration
 * Management module.
 * 18-Jan-2016 - Changed the type of variable sample_average from String to
 * Boolean.
 */

@ManagedBean (name="methPBean")
@ViewScoped
public class METHPipelineBean extends GEXAffymetrixBean {

    public METHPipelineBean() {
        pipelineName = Constants.METH_PIPELINE;
        pipelineTech = PipelineDB.getPipelineTechnology(pipelineName);
        commandLink  = "run-meth-pipeline";
        
        logger.debug("METHPipelineBean created.");
    }
    
    @Override
    public Boolean insertJob(String outputFilePath, String reportFilePath) {
        Boolean result = Constants.OK;        
        // job_id will not be used during insertion, just send in any value will
        // do e.g. 0
        // Insert the new job request into datbase; job status is 1 i.e. Waiting
        // For attributes type, probeFilter, StdLog2Ratio, summarization and 
        // region, set them to "NA". For sample_average, set it to false.
        // SubmittedJob(job_id, study_id, user_id, pipeline_name, status_id, 
        // submit_time, chip_type, input_path, normalization, probe_filtering, 
        // probe_select, phenotype_column, summarization, output_file, 
        // sample_average, standardization, region, report) 
        SubmittedJob newJob = 
                new SubmittedJob(0, getStudyID(), userName, pipelineName, 1,
                                 submitTimeInDB, "NA", getInputPath(), 
                                 getNormalization(), "NA", isProbeSelect(), 
                                 getPhenotype(), "NA", outputFilePath, 
                                 false, "NA", "NA", reportFilePath);
        
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
}
