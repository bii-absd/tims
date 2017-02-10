/*
 * Copyright @2017
 */
package TIMS.Bean;

// Libraries for Java Extension
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * CNVAffymetrixBean is used as the backing bean for the cnv-affymetrix view.
 * 
 * Author: Tay Wei Hong
 * Date: 08-Feb-2017
 * 
 * Revision History
 * 08-Feb-2017 - Initial creation.
 */

@ManagedBean (name="cnvAffyBean")
@ViewScoped
public class CNVAffymetrixBean extends GEXAffymetrixBean {
    
    public CNVAffymetrixBean() {
        logger.debug("CNVAffymetrixBean created.");
    }
}
