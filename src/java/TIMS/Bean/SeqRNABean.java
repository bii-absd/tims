/*
 * Copyright @2017-2019
 */
package TIMS.Bean;

import TIMS.General.Constants;
// Library for Java
import java.io.File;
// Libraries for Java Extension
import javax.inject.Named;
import javax.annotation.PostConstruct;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;


/**
 * SeqRNABean is used as the backing bean for the seq-rna view.
 * 
 * Author: Tay Wei Hong
 * Date: 06-Feb-2017
 * 
 * Revision History
 * 06-Feb-2017 - Initial creation by extending GEXAffymetrixBean. Override the
 * initFiles(), getAllFilenameFromAnnot() and retrieveRawDataFileList() methods.
 * 08-Feb-2017 - Changes due to change in method name at ConfigBean.
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 * 18-Jan-2018 - Removed method getAllFilenameFromAnnot(); no longer needed as
 * each subject will only have one sample file. Sample file extension changed
 * to bam. Added GTF file for RNA Seq pipeline.
 * 31-Jan-2019 - To use a common input directory for all newly uploaded raw data.
 */

//@ManagedBean (name="seqrnaBean")
@Named("seqrnaBean")
@ViewScoped
public class SeqRNABean extends GEXAffymetrixBean {
    private FileUploadBean gtfFile;
    
    public SeqRNABean() {
        logger.debug("SeqRNABean created.");
    }
    
    @Override
    @PostConstruct
    public void initFiles() {
        init();
        // Raw data file extension for RNA Sequencing pipeline.
        rdFileExt = "bam";
        
        if (haveNewData) {
            gtfFile = new FileUploadBean(inputDir);
        }
    }
    
    @Override
    public void renameAnnotCtrlFiles() {
        // Rename gtf file.
        gtfFile.renameFilename(Constants.getGTF_FILE_NAME() + 
                                Constants.getGTF_FILE_EXT());
        super.renameAnnotCtrlFiles();
    }

    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public boolean createConfigFile() {
        if (haveNewData) {
            gtf = gtfFile.getLocalDirectoryPath() + 
                  Constants.getGTF_FILE_NAME() + Constants.getGTF_FILE_EXT();
        }
        else {
            gtf = selectedInput.getFilepath() + 
                  Constants.getGTF_FILE_NAME() + Constants.getGTF_FILE_EXT();
        }
        // Call the base class method to create the Config File.        
        return super.createConfigFile();
    }

    @Override
    public void updateJobSubmissionStatus() {
        if (haveNewData && gtfFile.isFilelistEmpty()) {
            logger.debug("No GTF file uploaded!");
            setJobSubmissionStatus(false);
        }
        else {
            super.updateJobSubmissionStatus();
        }
    }

    // Call filterRawDataFileList() to exclude the annotation file from the raw
    // data file list.
    @Override
    public void retrieveRawDataFileList() {
        filterRawDataFileList();
    }
    
    // Machine generated getters and setters
    public FileUploadBean getGtfFile() {
        return gtfFile;
    }
    public void setGtfFile(FileUploadBean gtfFile) {
        this.gtfFile = gtfFile;
    }
}
