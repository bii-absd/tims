/*
 * Copyright @2016-2018
 */
package TIMS.Bean;

import static TIMS.Bean.ConfigBean.logger;
import TIMS.General.Constants;
// Library for Java
import java.io.File;
// Libraries for Java Extension
import javax.inject.Named;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;
//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.ViewScoped;

/**
 * CNVIlluminaBean is used as the backing bean for the cnv-pipeline view.
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
 * 14-Apr-2016 - Changes due to the type change (i.e. to Timestamp) for 
 * submit_time and complete_time in submitted_job table.
 * 19-May-2016 - Changes due to the addition attribute (i.e. detail_output) in 
 * submitted_job table.
 * 25-Aug-2016 - Changes due to method name (i.e. getCreateTimeString) change 
 * in InputData class.
 * 01-Sep-2016 - Changes due to the addition attribute (i.e. input_desc) in 
 * submitted_job table.
 * 05-Sep-2016 - Changes due to change in constant name.
 * 14-Sep-2016 - Implemented Raw Data Customization module. Removed method 
 * insertJob().
 * 21-Sep-2016 - Enhanced method retrieveRawDataFileList() to make sure the 
 * filename is sorted before storing them into the file list. Removed unused
 * code.
 * 06-Feb-2017 - Enhanced method retrieveRawDataFileList() to use the helper
 * function filterRawDataFileList().
 * 08-Feb-2017 - Renamed from CNVPipelineBean to CNVIlluminaBean.
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 */

//@ManagedBean (name="cnvIlluBean")
@Named("cnvIlluBean")
@ViewScoped
public class CNVIlluminaBean extends GEXAffymetrixBean {
    private FileUploadBean ctrlFile;

    public CNVIlluminaBean() {
        logger.debug("CNVIlluminaBean created.");
    }

    @Override
    public void initFiles() {
        init();
        // Set the raw data file extension for this pipeline.
        rdFileExt = "txt";
        
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
    public void renameAnnotCtrlFiles() {
        // Rename control probe file.
        ctrlFile.renameFilename(Constants.getCONTROL_FILE_NAME() + 
                                Constants.getCONTROL_FILE_EXT());
        super.renameAnnotCtrlFiles();
    }
    
    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public boolean createConfigFile() {
        if (haveNewData) {
            ctrl = ctrlFile.getLocalDirectoryPath() + 
                   Constants.getCONTROL_FILE_NAME() +
                   Constants.getCONTROL_FILE_EXT();
        }
        else {
            ctrl = selectedInput.getFilepath() + 
                   Constants.getCONTROL_FILE_NAME() +
                   Constants.getCONTROL_FILE_EXT();
        }
        // Call the base class method to create the Config File.        
        return super.createConfigFile();
    }

    // Call filterRawDataFileList() to exclude the annotation and control files 
    // from the raw data file list.
    @Override
    public void retrieveRawDataFileList() {
        filterRawDataFileList();
    }

    // Machine generated getters and setters
    public FileUploadBean getCtrlFile() {
        return ctrlFile;
    }
    public void setCtrlFile(FileUploadBean ctrlFile) {
        this.ctrlFile = ctrlFile;
    }
}
