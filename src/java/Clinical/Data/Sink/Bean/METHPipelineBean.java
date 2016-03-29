/*
 * Copyright @2016
 */
package Clinical.Data.Sink.Bean;

import static Clinical.Data.Sink.Bean.ConfigBean.logger;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Java Extension
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.naming.NamingException;

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
 * 20-Jan-2016 - To streamline the navigation flow and passing of pipeline name
 * from main menu to pipeline configuration pages.
 * 18-Feb-2016 - To check the input files received with the filename listed in
 * the annotation file. List out the missing files (if any) and notice the user
 * during pipeline configuration review.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 24-Mar-2016 - Changes due to the new attribute (i.e. complete_time) added in
 * submitted_job table.
 * 29-Mar-2016 - Instead of storing the input path, the system will store the 
 * input SN.
 */

@ManagedBean (name="methPBean")
@ViewScoped
public class METHPipelineBean extends GEXAffymetrixBean {

    public METHPipelineBean() {
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
        // For complete_time, set to "waiting" for the start.
        // 
        // SubmittedJob(job_id, study_id, user_id, pipeline_name, status_id, 
        // submit_time, complete_time, chip_type, input_sn, normalization, 
        // probe_filtering, probe_select, phenotype_column, summarization, 
        // output_file, sample_average, standardization, region, report) 
        SubmittedJob newJob = 
                new SubmittedJob(0, getStudyID(), userName, pipelineName, 1,
                                 submitTimeInDB, "waiting", "NA", input_sn, 
                                 getNormalization(), "NA", isProbeSelect(), 
                                 getPhenotype(), "NA", outputFilePath, 
                                 false, "NA", "NA", reportFilePath);
        
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
    
    // Read in all the filename listed in the annotation file.
    @Override
    public List<String> getAllFilenameFromAnnot() {
        List<String> filenameList = new ArrayList<>();
        String[] content;
        
        try (BufferedReader br = new BufferedReader(
                                 new FileReader(sampleFile.getLocalDirectoryPath() + 
                                                sampleFile.getInputFilename())))
        {
            // First line is the header; not needed here.
            String lineRead = br.readLine();
            // Start processing from the second line.
            while ((lineRead = br.readLine()) != null) {
                content = lineRead.split("\t");
                // The second column contains the filename header; for
                // Methylation pipeline, each header will have 2 filenames.
                filenameList.add(content[1] + "_Grn.idat");
                filenameList.add(content[1] + "_Red.idat");
            }
            logger.debug("All filename read from annotation file.");
        }
        catch (IOException e) {
            logger.error("FAIL to read annotation file!");
            logger.error(e.getMessage());
        }
        
        return filenameList;
    }
}
