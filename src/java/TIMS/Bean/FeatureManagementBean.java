/*
 * Copyright @2016-2018
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.Feature;
import TIMS.Database.FeatureDB;
import TIMS.General.Constants;
// Libraries for Java
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
 * FeatureManagementBean is the backing bean for the visualizermanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 18-Jul-2016
 * 
 * Revision History
 * 21-Jul-2016 - Created with all the standard getters and setters. Implemented
 * the edit function for feature active status.
 * 11-Jun-2018 - Changes due to update in feature table; replaced active 
 * (BOOLEAN) with status (TEXT).
 */

@ManagedBean (name="fteMgntBean")
@ViewScoped
public class FeatureManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FeatureManagementBean.class.getName());
    private final String userName;
    private List<Feature> fteList;
//    private String visualizer_status;
    private final FeatureDB featureDB;
    
    public FeatureManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        featureDB = new FeatureDB();
        logger.info(userName + ": access Visualizer Management page.");
    }
    
    @PostConstruct
    public void init() {
        fteList = featureDB.getAllFeatureStatus();
//        visualizer_status = featureDB.getFeatureStatus("Visualizer");
    }
    
    // Update the feature table in the database.
    public void onRowEdit(RowEditEvent event) {
        try {
            Feature fte = (Feature) event.getObject();
            featureDB.updateFeature(fte);
            // Record this feature setup activity into database.
            StringBuilder detail = new StringBuilder(fte.getFcode()).
                                       append(" - ").append(fte.getStatus());
//            String detail = fte.getFcode() + " - " + fte.getStatus();
            ActivityLogDB.recordUserActivity(userName, Constants.SET_FTE, detail.toString());
            StringBuilder oper = new StringBuilder(userName).
                    append(": updated ").append(fte.getFcode()).
                    append(" to ").append(fte.getStatus());
            logger.info(oper);
//            logger.info(userName + ": updated " + fte.getFcode() + " to " 
//                        + fte.getStatus());
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "Visualizer setting updated.", ""));
            // Update feature list.
            AuthenticationBean.setupFeatureList(featureDB.getAllFeatureStatusHash());
        }
        catch (SQLException|NamingException e) {
            logger.error("Fail to update visualizer setting!");
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "Failed to update visualizer setting!", ""));            
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Machine generated getters and setters.
    public List<Feature> getFteList() {
        return fteList;
    }
    /* NOT IN USE!
    public String getVisualizer_status() {
        return visualizer_status;
    }
    public void setVisualizer_status(String visualizer_status) {
        this.visualizer_status = visualizer_status;
    }
    */
}
