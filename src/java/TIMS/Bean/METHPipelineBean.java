/*
 * Copyright @2016
 */
package TIMS.Bean;

import static TIMS.Bean.ConfigBean.logger;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
        // Set the raw data file extension for this pipeline.
        rdFileExt = "idat";
    }
    
    // Read in all the filename listed in the annotation file.
    @Override
    public List<String> getAllFilenameFromAnnot() {
        List<String> filenameList = new ArrayList<>();
        String[] content, fn;
        
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
                // Methylation pipeline, each header will have 2 filenames 
                // separated by ','.
                fn = content[1].split(",");
                // Add the filenames to the list; can handle more than 2 
                // filenames.
                filenameList.addAll(Arrays.asList(fn));
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
