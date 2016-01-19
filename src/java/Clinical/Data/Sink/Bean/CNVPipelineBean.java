/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import static Clinical.Data.Sink.Bean.ConfigBean.logger;
import Clinical.Data.Sink.Database.PipelineDB;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import java.sql.SQLException;
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
 */

@ManagedBean (name="cnvPBean")
@ViewScoped
public class CNVPipelineBean extends GEXIlluminaBean {

    public CNVPipelineBean() {
        pipelineName = Constants.CNV_PIPELINE;
        pipelineTech = PipelineDB.getPipelineTechnology(pipelineName);
        commandLink = "run-cnv-pipeline";

        logger.debug("CNVPipelineBean created.");
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
}
