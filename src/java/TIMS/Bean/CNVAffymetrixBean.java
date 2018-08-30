/*
 * Copyright @2017-2018
 */
package TIMS.Bean;

// Libraries for Java Extension
import javax.inject.Named;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.ViewScoped;

/**
 * CNVAffymetrixBean is used as the backing bean for the cnv-affymetrix view.
 * 
 * Author: Tay Wei Hong
 * Date: 08-Feb-2017
 * 
 * Revision History
 * 08-Feb-2017 - Initial creation.
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 */

//@ManagedBean (name="cnvAffyBean")
@Named("cnvAffyBean")
@ViewScoped
public class CNVAffymetrixBean extends GEXAffymetrixBean {
    
    public CNVAffymetrixBean() {
        logger.debug("CNVAffymetrixBean created.");
    }
}
