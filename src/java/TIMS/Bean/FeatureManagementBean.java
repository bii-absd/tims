/*
 * Copyright @2016
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.Feature;
import TIMS.Database.FeatureDB;
import TIMS.General.Constants;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for PrimeFaces
import org.primefaces.event.RowEditEvent;

/**
 * FeatureManagementBean is the backing bean for the featuresetup view.
 * 
 * Author: Tay Wei Hong
 * Date: 18-Jul-2016
 * 
 * Revision History
 * 21-Jul-2016 - Created with all the standard getters and setters. Implemented
 * the edit function for feature active status.
 */

@ManagedBean (name="fteMgntBean")
@ViewScoped
public class FeatureManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FeatureManagementBean.class.getName());
    private final String userName;
    private List<Feature> fteList;
    
    public FeatureManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        logger.debug("FeatureManagementBean created.");
        logger.info(userName + ": access Feature Management page.");
    }
    
    @PostConstruct
    public void init() {
        fteList = FeatureDB.getAllFeatureStatus();
    }
    
    // Update the feature table in the database.
    public void onRowEdit(RowEditEvent event) {
        try {
            Feature fte = (Feature) event.getObject();
            FeatureDB.updateFeature(fte);
            // Record this feature setup activity into database.
            String detail = fte.getFcode() + " - " + fte.getActiveStatus();
            ActivityLogDB.recordUserActivity(userName, Constants.SET_FTE, detail);
            logger.info(userName + ": updated " + fte.getFcode() + " to " 
                        + fte.getActiveStatus());
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "Feature updated.", ""));
        }
        catch (SQLException|NamingException e) {
            logger.error("Fail to update feature!");
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "Failed to update feature!", ""));            
        }
    }
    
    // User cancel the edit, display message.
    public void onRowEditCancel(RowEditEvent event) {
        getFacesContext().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO, "Cancel Feature Update.", ""));
    }

    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Machine generated getters and setters.
    public List<Feature> getFteList() {
        return fteList;
    }
}
