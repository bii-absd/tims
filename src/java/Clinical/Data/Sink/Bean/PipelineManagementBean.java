/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.Pipeline;
import Clinical.Data.Sink.Database.PipelineDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
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
 * 24-Nov-2015 - Changed variable name from command_id to pipeline_name. Added
 * one variable tid (Technology ID).
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Jan-2016 - Removed all the static variables in Pipeline Management module.
 */

@ManagedBean (name="plMgntBean")
@ViewScoped
public class PipelineManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(PipelineManagementBean.class.getName());
    private String pipeline_name, tid, code, parameter;
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
        catch (SQLException e) {
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
       Pipeline newCmd = new Pipeline(pipeline_name, tid, code, parameter);
       
       if (PipelineDB.insertPipeline(newCmd)) {
           logger.info(userName + ": created new Pipeline: " + pipeline_name);
           fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "New pipeline created.", ""));
       }
       else {
           logger.error("FAIL to create new pipeline: " + pipeline_name);
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
            logger.info(userName + ": updated pipeline " + 
                    ((Pipeline) event.getObject()).getName());
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
    public String getPipeline_name() {
        return pipeline_name;
    }
    public void setPipeline_name(String pipeline_name) {
        this.pipeline_name = pipeline_name;
    }
    public String getTid() {
        return tid;
    }
    public void setTid(String tid) {
        this.tid = tid;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getParameter() {
        return parameter;
    }
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
    public List<Pipeline> getPlList() {
        return plList;
    }
    public void setPlList(List<Pipeline> plList) {
        this.plList = plList;
    }
}
