// Copyright (C) 2019 A*STAR
//
// TIMS (Translation Informatics Management System) is an software effort 
// by the ABSD (Analytics of Biological Sequence Data) team in the 
// Bioinformatics Institute (BII), Agency of Science, Technology and Research 
// (A*STAR), Singapore.
//

// This file is part of TIMS.
// 
// TIMS is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as 
// published by the Free Software Foundation, either version 3 of the 
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.Pipeline;
import TIMS.Database.PipelineDB;
import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.omnifaces.cdi.ViewScoped;
// Libraries for PrimeFaces
import org.primefaces.event.RowEditEvent;

@Named("plMgntBean")
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
    private final PipelineDB plDB;

    public PipelineManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        plDB = new PipelineDB();
        logger.info(userName + ": access Pipeline Management page.");
    }
    
    @PostConstruct
    public void init() {
        plList = plDB.getAllPipeline();
        
        if (plList.isEmpty()) {
            getFacesContext().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "System failed to retrieve pipeline info from database!", ""));
        }
    }

    // Create the new pipeline.
    public String createNewPipeline() {
       FacesContext fc = getFacesContext();
       Pipeline newCmd = new Pipeline(plName, plDesc, tid, command, parameter, editable);
       
       if (plDB.insertPipeline(newCmd)) {
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
        
        if (plDB.updatePipeline((Pipeline) event.getObject())) {
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
