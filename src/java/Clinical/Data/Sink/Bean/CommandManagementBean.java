/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.PipelineCommand;
import Clinical.Data.Sink.Database.PipelineCommandDB;
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
 */

@ManagedBean (name="cmdMgntBean")
@ViewScoped
public class CommandManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(CommandManagementBean.class.getName());
    private List<PipelineCommand> cmdList;
    
    public CommandManagementBean() {
        logger.debug("CommandManagementBean created.");
        logger.debug(AuthenticationBean.getUserName() +
                ": access Pipeline Command Management page.");
    }
    
    @PostConstruct
    public void init() {
        try {
            cmdList = PipelineCommandDB.getAllCommand();            
        }
        catch (SQLException e) {
            logger.error("SQLException when trying to query the pipeline commands.");
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "System failed to query the pipeline commands!", ""));
        }
    }

    // Update the pipeline command in the database too.
    public void onRowEdit(RowEditEvent event) {
        if (PipelineCommandDB.updateCommand(
                (PipelineCommand) event.getObject())) {
            logger.info(AuthenticationBean.getUserName() + 
                    ": updated pipeline command " + 
                    ((PipelineCommand) event.getObject()).getCommand_id());
            getFacesContext().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Pipeline command updated.", ""));
        }
        else {
            logger.error("Pipeline command update failed.");
            getFacesContext().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Failed to update pipeline command!", ""));
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }

    // Machine generated getters and setters
    public List<PipelineCommand> getCmdList() {
        return cmdList;
    }
    public void setCmdList(List<PipelineCommand> cmdList) {
        this.cmdList = cmdList;
    }
}
