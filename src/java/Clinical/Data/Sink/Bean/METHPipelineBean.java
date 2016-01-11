/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import static Clinical.Data.Sink.Bean.ConfigBean.pipelineName;
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
 */

@ManagedBean (name="methPBean")
@ViewScoped
public class METHPipelineBean extends GEXAffymetrixBean {

    public METHPipelineBean() {
        pipelineName = Constants.METH_PIPELINE;
        pipelineTech = PipelineDB.getPipelineTechnology(pipelineName);
        
        logger.debug("METHPipelineBean created.");
    }
    
    @Override
    public Boolean insertJob(String outputFilePath, String reportFilePath) {
        Boolean result = Constants.OK;        
        // job_id will not be used during insertion, just send in any value will
        // do e.g. 0
        // Insert the new job request into datbase; job status is 1 i.e. Waiting
        // For attributes type, probeFilter, sampleAverage, StdLog2Ratio, 
        // summarization and region, set them to "NA".
        SubmittedJob newJob = 
                new SubmittedJob(0, getStudyID(), pipelineName, 1,
                                 submitTimeInDB, "NA", getInputPath(), 
                                 getNormalization(), "NA", isProbeSelect(), 
                                 getPhenotype(), "NA", outputFilePath, 
                                 "NA", "NA", "NA", reportFilePath);
        
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
}
