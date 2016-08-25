/*
 * Copyright @2016
 */
package TIMS.Bean;

import TIMS.Database.InputData;
import TIMS.Database.InputDataDB;
import TIMS.Database.PipelineDB;
import TIMS.General.Constants;
import TIMS.General.FileHelper;
import TIMS.General.ResourceRetriever;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * RawDataManagementBean is the backing bean for the rawdatamanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 23-August-2016
 * 
 * Revision History
 * 25-Aug-2016 - Implemented Raw Data Management Module Part I.
 */

@ManagedBean (name="RDMgntBean")
@ViewScoped
public class RawDataManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(RawDataManagementBean.class.getName());
    private final String userName, plName, studyID;
    // List of input data belonging to this pipeline under this study.
    private List<InputData> inputDataList = new ArrayList<>();
    private InputData selectedInput = null;
    // Input files that will be uploaded by the users.
    private final FileUploadBean inputFile, ctrlFile, annotFile;
    // Input file description.
    private String inputFileDesc;
    // Temporary directory to store the input files.
    String tmpDir;
    
    public RawDataManagementBean() {
        plName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("pipeline");
        studyID = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("study_id");
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        // Setup the temporary directory to store the input files.
        tmpDir = Constants.getSYSTEM_PATH() + Constants.getUSERS_PATH() 
               + userName + Constants.getTMP_PATH();
        // Initialise the file upload beans.
        inputFile = new FileUploadBean(tmpDir);
        annotFile = new FileUploadBean(tmpDir);
        if (getControlFileStatus()) {
            ctrlFile = new FileUploadBean(tmpDir);
        }
        else {
            ctrlFile = null;
        }
        
        logger.debug(userName + ": access Raw Data Management page.");
    }
    
    @PostConstruct
    public void init() {
        // Retrieve the input data list for this pipeline under this study.
        inputDataList = InputDataDB.getIpList(studyID, plName);
    }
    
    @PreDestroy
    public void cleanUp() {
        // Perform some house cleaning before exiting this page.
        File dir = new File(tmpDir);
        
        if (dir.exists()) {
            // Delete the temporary directory before exit.
            try {
                logger.debug("Delete temporary directory before exit.");
                FileHelper.deleteDirectory(tmpDir);
            }
            catch (IOException e) {
                logger.error("FAIL to delete temporary directory!");
                logger.error(e.getMessage());
            }
        }
    }
    
    // Return the wording to be display at the link under the BreadCrumb in the
    // Raw Data Management page.
    public String getBreadCrumbLink() {
        return "Study: " + studyID + "  Pipeline: " + ResourceRetriever.getMsg(plName);
    }

    // Return true if input package has been selected.
    public boolean getInputSelectedStatus() {
        return (selectedInput != null);
    }
    
    // A new input package has been selected by the user, need to rebuild the
    // variables.
    public void inputSelectionChange() {
        // Retrieve the current input file description.
        inputFileDesc = selectedInput.getDescription();
    }
    
    // The Save Changes button has been clicked.
    public String confirmChanges() {
        // Record the time when the changes happened, and save it into database.
        Date now = new Date();
        Timestamp update_time = new Timestamp(now.getTime());
        // Update entry in input_data table.
        InputDataDB.updateFieldsAfterEdit(studyID, selectedInput.getSn(), 
                inputFileDesc, userName, update_time);
        
        return Constants.MAIN_PAGE;
    }
    
    // Return the input file allowed types for each pipeline.
    public String getAllowTypes() {
        String filter = "/(\\.|\\/)(txt)$/";

        switch (plName) {
            case PipelineDB.GEX_AFFYMETRIX:
                filter = "/(\\.|\\/)(CEL)$/";
                break;
            case PipelineDB.METHYLATION:
                filter = "/(\\.|\\/)(idat)$/";
                break;
            case PipelineDB.CNV:
                filter = "/(\\.|\\/)(txt)$/";
        }

        return filter;
    }
    
    // Return true if this pipeline has control file, else return false.
    public final boolean getControlFileStatus() {
        return (plName.compareTo(PipelineDB.CNV) == 0 );
    }
    
    // Machine generated code.
    public List<InputData> getInputDataList() {
        return inputDataList;
    }
    public String getInputFileDesc() {
        return inputFileDesc;
    }
    public void setInputFileDesc(String inputFileDesc) {
        this.inputFileDesc = inputFileDesc;
    }
    public InputData getSelectedInput() {
        return selectedInput;
    }
    public void setSelectedInput(InputData selectedInput) {
        this.selectedInput = selectedInput;
    }
    public FileUploadBean getInputFile() {
        return inputFile;
    }
    public FileUploadBean getCtrlFile() {
        return ctrlFile;
    }
    public FileUploadBean getAnnotFile() {
        return annotFile;
    }
}
