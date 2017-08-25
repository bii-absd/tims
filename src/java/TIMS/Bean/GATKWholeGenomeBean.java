/*
 * Copyright @2017
 */
package TIMS.Bean;

// Libraries for Java Extension
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * GATKWholeGenomeBean is used as the backing bean for the gatk-whole-genome-seq 
 * view.
 * 
 * Author: Tay Wei Hong
 * Date: 10-Jul-2017
 * 
 * Revision History
 * 10-Jul-2017 - Initial creation by extending GEXAffymetrixBean. Override the
 * initFiles() method.
 */

@ManagedBean (name="GatkWGBean")
@ViewScoped
public class GATKWholeGenomeBean extends GEXAffymetrixBean {
    
    public GATKWholeGenomeBean() {
        logger.debug("GATKWholeGenomeBean created.");
    }
    
    @Override
    public void initFiles() {
        init();
        // Raw data file extension for GATK Whole-Genome Sequencing pipelines.
        rdFileExt = "bam";
        // Set some default values for readDepth and variantDepth.
        readDepth = 100;
        variantDepth = 10;
    }
}
