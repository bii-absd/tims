/*
 * Copyright @2016-2017
 */
package TIMS.Bean;

import static TIMS.Bean.ConfigBean.logger;
import java.util.List;
// Libraries for Java Extension
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
 * boolean.
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
 * 11-Apr-2016 - Changes due to the removal of attributes (sample_average, 
 * standardization, region and probe_select) from submitted_job table.
 * 12-Apr-2016 - Changes due to the removal of attributes (probe_filtering and
 * phenotype_column) from submitted_job table.
 * 14-Apr-2016 - Changes due to the type change (i.e. to Timestamp) for 
 * submit_time and complete_time in submitted_job table.
 * 19-May-2016 - Changes due to the addition attribute (i.e. detail_output) in 
 * submitted_job table.
 * 01-Sep-2016 - Changes due to the addition attribute (i.e. input_desc) in 
 * submitted_job table.
 * 14-Sep-2016 - Implemented Raw Data Customization module. Removed method 
 * insertJob().
 * 19-Sep-2016 - Updated method getAllFilenameFromAnnot() to read in multiple
 * filename from the 2nd column of the annotation file. Removed unused code.
 * 06-Feb-2017 - Enhanced method getAllFilenameFromAnnot() to use the helper
 * function getFilenamePairs().
 */

@ManagedBean (name="methPBean")
@ViewScoped
public class METHPipelineBean extends GEXAffymetrixBean {

    public METHPipelineBean() {
        logger.debug("METHPipelineBean created.");
    }
    
    @Override
    public void initFiles() {
        init();
        // Raw data file extension for Methylation pipeline.
        rdFileExt = "idat";
    }
    
    // For Methylation pipeline, there will be 2 filenames for each sample 
    // input in the annotation file.
    @Override
    public List<String> getAllFilenameFromAnnot() {
        return getFilenamePairs(sampleFile.getLocalDirectoryPath() + 
                                sampleFile.getInputFilename());
    }
}
