/*
 * Copyright @2017
 */
package TIMS.Bean;

import static TIMS.Bean.ConfigBean.logger;
import java.util.List;
// Libraries for Java Extension
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;


/**
 * SeqRNABean is used as the backing bean for the seq-rna view.
 * 
 * Author: Tay Wei Hong
 * Date: 06-Feb-2017
 * 
 * Revision History
 * 06-Feb-2017 - Initial creation by extending GEXAffymetrixBean. Override the
 * initFiles(), getAllFilenameFromAnnot() and retrieveRawDataFileList() methods.
 */

@ManagedBean (name="seqrnaBean")
@ViewScoped
public class SeqRNABean extends GEXAffymetrixBean {
    
    public SeqRNABean() {
        logger.debug("SeqRNABean created.");
    }
    
    @Override
    public void initFiles() {
        init();
        // Raw data file extension for RNA Sequencing pipeline.
        rdFileExt = "txt";
    }
    
    // For RNA Sequencing pipeline, there will be 2 filenames for each sample 
    // input in the annotation file.
    @Override
    public List<String> getAllFilenameFromAnnot() {
        return getFilenamePairs(sampleFile.getLocalDirectoryPath() + 
                                sampleFile.getInputFilename());
    }
    
    // Call filterRawDataFileList() to exclude the annotation file from the raw
    // data file list.
    @Override
    public void retrieveRawDataFileList() {
        filterRawDataFileList();
    }
}
