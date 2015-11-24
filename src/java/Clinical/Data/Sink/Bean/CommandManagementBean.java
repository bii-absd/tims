/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.PipelineCommand;
import Clinical.Data.Sink.Database.PipelineCommandDB;
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
import org.primefaces.event.RowEditEvent;

/**
 * CommandManagementBean is the backing bean for the commandmanagement view.
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
 */

@ManagedBean (name="cmdMgntBean")
@ViewScoped
public class CommandManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(CommandManagementBean.class.getName());
    private String pipeline_name, tid, command_code, command_para;
    private List<PipelineCommand> cmdList;
    
    public CommandManagementBean() {
        logger.debug("CommandManagementBean created.");
        logger.info(AuthenticationBean.getUserName() +
                ": access Pipeline Command Management page.");
    }
    
    @PostConstruct
    public void init() {
        try {
            cmdList = PipelineCommandDB.getAllPipelineCommand();            
        }
        catch (SQLException e) {
            logger.error("SQLException when trying to query the pipeline commands.");
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "System failed to query the pipeline commands!", ""));
        }
    }

    // Create the new pipeline command.
    public String createNewPipelineCommand() {
       FacesContext fc = getFacesContext();
       PipelineCommand newCmd = new PipelineCommand(pipeline_name, tid, 
                                    command_code, command_para);
       
       if (PipelineCommandDB.insertPipelineCommand(newCmd)) {
           logger.info(AuthenticationBean.getUserName() + 
                   ": created new Pipeline Command: " + pipeline_name);
           fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "New pipeline command created.", ""));
       }
       else {
           logger.error("Failed to create new pipeline command: " + pipeline_name);
           fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Failed to create new pipeline command!", ""));
       }
       
       return Constants.PIPELINE_COMMAND_MANAGEMENT;
    }
    
    // Update the pipeline command in the database.
    public void onRowEdit(RowEditEvent event) {
        FacesContext fc = getFacesContext();
        
        if (PipelineCommandDB.updatePipelineCommand(
                (PipelineCommand) event.getObject())) {
            logger.info(AuthenticationBean.getUserName() + 
                    ": updated pipeline command " + 
                    ((PipelineCommand) event.getObject()).getPipeline_name());
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Pipeline command updated.", ""));
        }
        else {
            logger.error("Pipeline command update failed.");
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Failed to update pipeline command!", ""));
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
    public String getCommand_code() {
        return command_code;
    }
    public void setCommand_code(String command_code) {
        this.command_code = command_code;
    }
    public String getCommand_para() {
        return command_para;
    }
    public void setCommand_para(String command_para) {
        this.command_para = command_para;
    }
    public List<PipelineCommand> getCmdList() {
        return cmdList;
    }
    public void setCmdList(List<PipelineCommand> cmdList) {
        this.cmdList = cmdList;
    }
}
