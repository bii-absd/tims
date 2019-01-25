/*
 * Copyright @2017-2019
 */
package TIMS.Bean;

import TIMS.Database.PipelineDB;
import TIMS.General.Constants;
import TIMS.General.ResourceRetriever;
// Library for Java
import java.io.File;
import java.util.List;
// Libraries for Java Extension
import javax.inject.Named;
import javax.annotation.PostConstruct;
//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.ViewScoped;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

/**
 * GATKTargetedBean is used as the backing bean for the gatk-targeted-seq view.
 * 
 * Author: Tay Wei Hong
 * Date: 10-Jul-2017
 * 
 * Revision History
 * 10-Jul-2017 - Initial creation by extending GEXAffymetrixBean. Override the
 * initFiles(), renameAnnotCtrlFiles(), createConfigFile(), and 
 * retrieveRawDataFileList() methods, and created a new method getIntFileName().
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 * 18-Jan-2019 - Added method getAllFilenameFromAnnot(), as for Somatic 
 * pipeline, there are 2 sample files for each subject.
 */

@Named("GatkTarBean")
@ViewScoped
public class GATKTargetedBean extends GEXAffymetrixBean {
    private FileUploadBean intFile;
    
    public GATKTargetedBean() {
        logger.debug("GATKTargetedBean created.");
    }
    
    @Override
    public List<String> getAllFilenameFromAnnot() {
        if (pipelineName.equals(PipelineDB.GATK_TAR_SOMA)) {
            return getFilenamePairsFromAnnot();
        } else {
            return super.getAllFilenameFromAnnot();
        }
    }

    @Override
    @PostConstruct
    public void initFiles() {
        init();
        // Raw data file extension for GATK Targeted Sequencing.
        rdFileExt = "bam";
        readDepth = 100;
        variantDepth = 10;
        
        if (haveNewData) {
            String dir = Constants.getSYSTEM_PATH() + Constants.getINPUT_PATH() 
                       + studyID + File.separator 
                       + submitTimeInFilename + File.separator;
            intFile = new FileUploadBean(dir);
        }
    }
    
    @Override
    public void renameAnnotCtrlFiles() {
        // Rename interval file.
        intFile.renameFilename(Constants.getINTERVAL_FILE_NAME() + 
                                Constants.getINTERVAL_FILE_EXT());
        super.renameAnnotCtrlFiles();
    }
    
    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public boolean createConfigFile() {
        if (haveNewData) {
            // Check to make sure the user did upload the interval file.
            if (!intFile.isFilelistEmpty()) {
                interval = intFile.getLocalDirectoryPath() + 
                           Constants.getINTERVAL_FILE_NAME() +
                           Constants.getINTERVAL_FILE_EXT();
            }
        }
        else {
            interval = selectedInput.getFilepath() + 
                       Constants.getINTERVAL_FILE_NAME() +
                       Constants.getINTERVAL_FILE_EXT();
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

    // If the user didn't upload the interval file, let them proceed but display
    // the warning message below.
    public String getIntFileName() {
        if (intFile.isFilelistEmpty()) {
            return ResourceRetriever.getMsg("warn-int-file");
        }
        else {
            return intFile.getInputFilename();
        }
    }
    
    // Machine generated getters and setters
    public FileUploadBean getIntFile() {
        return intFile;
    }
    public void setIntFile(FileUploadBean intFile) {
        this.intFile = intFile;
    }
}
