/*
 * Copyright @2016
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
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
import org.primefaces.event.FileUploadEvent;

/**
 * RawDataManagementBean is the backing bean for the rawdatamanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 23-August-2016
 * 
 * Revision History
 * 25-Aug-2016 - Implemented Raw Data Management Module Part I.
 * 30-Aug-2016 - Implemented Raw Data Management Module Part II.
 * 05-Sep-2016 - Changes due to change in constant name.
 * 20-Sep-2016 - Fix the bug whereby the filename in input_data table get 
 * corrupted when no sample data is being uploaded.
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
    // Sample file(s) list for new and replace.
    private List<String> newSampleFiles, replaceSampleFiles;
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
        // Clean up the temporary directory before using it.
        deleteTempDir();
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
        deleteTempDir();
    }
    
    // Delete the temporary directory.
    private void deleteTempDir() {
        File dir = new File(tmpDir);
        
        if (dir.exists()) {
            // Delete the temporary directory if it exists.
            try {
                FileHelper.deleteDirectory(tmpDir);
            }
            catch (IOException e) {
                logger.error("FAIL to delete temporary directory!");
                logger.error(e.getMessage());
            }
        }
    }

    // After reviewing the input data, the user decided not to proceed with the
    // changes.
    public void doNotProceed() {
        // Release the file lists memory.
        newSampleFiles = replaceSampleFiles = null;
        
        logger.debug(userName + ": did not confirm the changes to input data.");
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
        // Reset all the file beans.
        inputFile.resetFileBean();
        annotFile.resetFileBean();
        if (getControlFileStatus()) {
            ctrlFile.resetFileBean();
        }
        // Delete all the uploaded files.
        deleteTempDir();
    }
    
    // User click on Save Changes button, prepare the input data review content.
    public void preForReview() {
        // Initialise the file lists.
        newSampleFiles = new ArrayList<>();
        replaceSampleFiles = new ArrayList<>();
        // Create the sample file list.
        inputFile.createInputList();
        List<String> filenames = inputFile.getInputFileList();
        // Retrieve the file directory from the selected input data package.
        String dir = selectedInput.getFilepath();
        // Go through the list of sample files uploaded, and separate them into
        // new and replace file lists.
        for (String filename : filenames) {
            if (FileHelper.fileExist(dir + filename)) {
                replaceSampleFiles.add(filename);
            }
            else {
                newSampleFiles.add(filename);
            }
        }
    }
    
    // User confirmed the changes after reviewing them. Update the entry in 
    // input_data table, backup the replacement files, and move the newly 
    // uploaded files to input data directory.
    public String confirmChanges() {
        // Record the time when the changes happened, and save it into database.
        Date now = new Date();
        Timestamp update_time = new Timestamp(now.getTime());
        // Extension used in backup files.
        String ext = "." + Constants.getDT_yyyyMMdd_HHmm();
        // Input data directory.
        String destDir = selectedInput.getFilepath();
        // Record this raw data management activity into database.
        ActivityLogDB.recordUserActivity(userName, Constants.CHG_RD, 
                            studyID + " - Input SN " + selectedInput.getSn());
        // Update entry in input_data table.
        if (plName.compareTo(PipelineDB.GEX_ILLUMINA) == 0) {
            if (inputFile.isFilelistEmpty()) {
                // No raw data has being uploaded.
                InputDataDB.updateDescAfterEdit(studyID, selectedInput.getSn(), 
                        inputFileDesc, userName, update_time);
            }
            else {
                InputDataDB.updateDescFilenameAfterEdit(studyID, selectedInput.getSn(), 
                        inputFileDesc, userName, update_time, inputFile.getInputFilename());
                
                if (replaceSampleFiles.isEmpty()) {
                    // The uploaded raw data has a different filename.
                    // Need to backup the original sample file manually here.
                    replaceSampleFiles.add(selectedInput.getFilename());
                }
            }
        }
        else {
            InputDataDB.updateDescAfterEdit(studyID, selectedInput.getSn(), 
                    inputFileDesc, userName, update_time);
        }
        
        // For replacement sample file(s), backup the original sample file(s) 
        // using current datetime as extension.
        for (String repFile : replaceSampleFiles) {
            FileHelper.moveFile(destDir + repFile, 
                                destDir + repFile + ext);
        }
        // Move all the newly uploaded sample file(s) to input data directory 
        // (keep the filename).
        for (String newFile : inputFile.getInputFileList()) {
            FileHelper.moveFile(tmpDir + newFile, destDir + newFile);
        }
        // If a new control file has been uploaded, backup the original control
        // file, move and rename the new control file to the input data
        // directory.
        if (getControlFileStatus()) {
            if (!ctrlFile.isFilelistEmpty()) {
                String ctrlFilename = Constants.getCONTROL_FILE_NAME() 
                                    + Constants.getCONTROL_FILE_EXT();
                // Backup the original control file.
                FileHelper.moveFile(destDir + ctrlFilename, 
                                    destDir + ctrlFilename + ext);
                // Move and rename the new control file.
                FileHelper.moveFile(tmpDir + ctrlFile.getInputFilename(), 
                                    destDir + ctrlFilename);
                logger.debug("New control file saved.");
            }
        }
        // If a new annotation file has been uploaded, backup the original 
        // annotation file, move and rename the new annotation file to the input
        // data directory.
        if (!annotFile.isFilelistEmpty()) {
            String annotFilename = Constants.getANNOT_FILE_NAME()
                                 + Constants.getANNOT_FILE_EXT();
            // Backup the original annotation file.
            FileHelper.moveFile(destDir + annotFilename,
                                destDir + annotFilename + ext);
            // Move and rename the new annotation file.
            FileHelper.moveFile(tmpDir + annotFile.getInputFilename(), 
                                destDir + annotFilename);
            logger.debug("New annotation file saved.");
        }
        
        return Constants.MAIN_PAGE;
    }
    
    // Return the input file allowed types for each pipeline.
    public String getAllowTypes() {
        // Default file types for Illumina and CNV.
        String filter = "/(\\.|\\/)(txt)$/";

        switch (plName) {
            case PipelineDB.GEX_AFFYMETRIX:
                filter = "/(\\.|\\/)(CEL)$/";
                break;
            case PipelineDB.METHYLATION:
                filter = "/(\\.|\\/)(idat)$/";
                break;
        }

        return filter;
    }
    
    // Return true if this pipeline has control file, else return false.
    public final boolean getControlFileStatus() {
        return (plName.compareTo(PipelineDB.CNV) == 0 ) || 
               (plName.compareTo(PipelineDB.GEX_ILLUMINA) == 0);
    }
    
    // Called the respective listener based on the pipeline type.
    public void sampleFileUploadListener(FileUploadEvent event) {
        if (plName.compareTo(PipelineDB.GEX_ILLUMINA) == 0) {
            inputFile.singleFileUploadListener(event);
        }
        else {
            inputFile.multipleFileUploadListener(event);
        }
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
    public List<String> getNewSampleFiles() {
        return newSampleFiles;
    }
    public List<String> getReplaceSampleFiles() {
        return replaceSampleFiles;
    }
}
