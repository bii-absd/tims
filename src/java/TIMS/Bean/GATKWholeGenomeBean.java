/*
 * Copyright @2017-2019
 */
package TIMS.Bean;

import TIMS.Database.PipelineDB;
// Library for Java
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.inject.Named;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.ViewScoped;

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
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 * 18-Jan-2019 - Added method getAllFilenameFromAnnot(), as for Somatic 
 * pipeline, there are 2 sample files for each subject.
 */

@Named("GatkWGBean")
@ViewScoped
public class GATKWholeGenomeBean extends GEXAffymetrixBean {
    
    public GATKWholeGenomeBean() {
        logger.debug("GATKWholeGenomeBean created.");
    }
    
    @Override
    public List<String> getAllFilenameFromAnnot() {
        if (pipelineName.equals(PipelineDB.GATK_WG_SOMA)) {
            return getFilenamePairsFromAnnot();
        } else {
            return super.getAllFilenameFromAnnot();
        }
    }
    
    @Override
    @PostConstruct
    public void initFiles() {
        init();
        // Raw data file extension for GATK Whole-Genome Sequencing pipelines.
        rdFileExt = "bam";
        // Set some default values for readDepth and variantDepth.
        readDepth = 100;
        variantDepth = 10;
    }
}
