/*
 * Copyright @2015-2016
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.Pipeline;
import TIMS.Database.PipelineDB;
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
 * PipelineManagementBean is the backing bean for the pipelinemanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 06-Nov-2015
 * 
 * Revision History
 * 06-Nov-2015 - Created with all the standard getters and setters. Added
 * method onRowEdit to handle pipeline command updating.
 * 16-Nov-2015 - Added one new method createNewPipelineCommand, to allow user
 * to create new pipeline command.
 * 24-Nov-2015 - Changed variable name from command_id to plName. Added
 * one variable tid (Technology ID).
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Jan-2016 - Removed all the static variables in Pipeline Management module.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 25-Aug-2016 - Implementation for database 3.6 Part I.
 */

@ManagedBean (name="plMgntBean")
@ViewScoped
public class PipelineManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(PipelineManagementBean.class.getName());
    private String plName, plDesc, tid, command, parameter;
    private boolean editable;
    private List<Pipeline> plList;
    // Store the user ID of the current user.
    private final String userName;

    public PipelineManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        logger.debug("PipelineManagementBean created.");
        logger.info(userName + ": access Pipeline Management page.");
    }
    
    @PostConstruct
    public void init() {
        try {
            plList = PipelineDB.getAllPipeline();            
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve pipeline info!");
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "System failed to retrieve pipeline from database!", ""));
        }
    }

    // Create the new pipeline.
    public String createNewPipeline() {
       FacesContext fc = getFacesContext();
       Pipeline newCmd = new Pipeline(plName, plDesc, tid, command, parameter, editable);
       
       if (PipelineDB.insertPipeline(newCmd)) {
           // Record this pipeline creation activity into database.
           String detail = "Pipeline " + plName + " for technology " 
                         + tid;
           ActivityLogDB.recordUserActivity(userName, Constants.CRE_ID, detail);
           logger.info(userName + ": created " + detail);
           fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "New pipeline created.", ""));
       }
       else {
           logger.error("FAIL to create new pipeline: " + plName);
           fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Failed to create new pipeline!", ""));
       }
       
       return Constants.PIPELINE_MANAGEMENT;
    }
    
    // Update the pipeline table in database.
    public void onRowEdit(RowEditEvent event) {
        FacesContext fc = getFacesContext();
        
        if (PipelineDB.updatePipeline((Pipeline) event.getObject())) {
            // Record this pipeline update activity into database.
            String detail = "Pipeline " + ((Pipeline) event.getObject()).getName();
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail);
            logger.info(userName + ": updated " + detail);
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Pipeline updated.", ""));
        }
        else {
            logger.error("FAIL to update pipeline!");
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Failed to update pipeline!", ""));
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Machine generated getters and setters
    public String getPlName() {
        return plName;
    }
    public void setPlName(String plName) {
        this.plName = plName;
    }
    public String getPlDesc() {
        return plDesc;
    }
    public void setPlDesc(String plDesc) {
        this.plDesc = plDesc;
    }
    public String getTid() {
        return tid;
    }
    public void setTid(String tid) {
        this.tid = tid;
    }
    public String getCommand() {
        return command;
    }
    public void setCommand(String command) {
        this.command = command;
    }
    public String getParameter() {
        return parameter;
    }
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
    public boolean isEditable() {
        return editable;
    }
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
    public List<Pipeline> getPlList() {
        return plList;
    }
    public void setPlList(List<Pipeline> plList) {
        this.plList = plList;
    }
}
