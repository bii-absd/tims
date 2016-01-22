/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.General.Constants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
// Libraries for PrimeFaces
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * FileUploadBean is the backing bean for all the uploaded file.
 * 
 * Author: Tay Wei Hong
 * Date: 27-Oct-2015
 * 
 * Revision History
 * 27-Oct-2015 - Created with the main function fileUploadListener, that is 
 * able to handle multiple uploaded files at one time.
 * 28-Oct-2015 - Changed to allow this class to handle both single and multiple
 * file upload.
 * 02-Nov-2015 - Added one new variable, localDirectoryPath and it's getter and 
 * setter methods. Added one new method, createAllSystemDirectories.
 * 05-Nov-2015 - Changed the localDirectoryPath to be static. Created individual
 * file upload listener for single and multiple files upload.
 * 06-Nov-2015 - Changed the way the localDirectoryPath is being setup, and 
 * display an error message when the file failed to get uploaded.
 * 11-Nov-2015 - The file directory will only be created after the user 
 * uploaded a file.
 * 02-Dec-2015 - Implemented the changes in the input folder directory.
 * 15-Dec-2015 - Added new method createStudyDirectory, to create level two
 * system directory for each Study. Modified method setFileDirectory to 
 * construct the directory name using the Study ID and Submission time.
 * 16-Dec-2015 - Added new method renameAnnotFile(), to rename the sample
 * annotation file to a common name.
 * 22-Dec-2015 - Added new method renameCtrlProbeFile(), to rename the control
 * probe file to a common name.
 * 24-Dec-2015 - Updated method createSystemDirectories, to create the 
 * directory for finalize_output too.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 14-Jan-2016 - Removed all the static variables in Pipeline Configuration
 * Management module.
 * 22-Jan-2016 - Added new method getFilesCount(), to return the no of input
 * files uploaded (i.e. as an indicator to the user during config review).
 */

public class FileUploadBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FileUploadBean.class.getName());
    private int fileCount = 0;
    private String fileDirectory = null;
    // To store the local path of the input files folder (to be use during
    // config file creation).
    private String localDirectoryPath = null;
    private List<String> inputList = null;
    private LinkedHashMap<Integer,String> fileList = null;

    public FileUploadBean(String study_id, String submitTime) {
        fileList = new LinkedHashMap<>();
        setFileDirectory(study_id, submitTime);
    }
    
    // Used for multiple files upload.
    public void multipleFileUploadListener(FileUploadEvent event) {
        fileList.put(++fileCount, fileUploadListener(event));
    }
    
    // Used for single file upload.
    public void singleFileUploadListener(FileUploadEvent event) {
        // For single file upload, we will always use the latest file.
        if (fileList.isEmpty()) {
            fileList.put(1, fileUploadListener(event));
        }
        else {
            fileList.replace(1, fileUploadListener(event));            
        }
    }
    
    // fileUploadListener will get call for each file uploaded.
    public String fileUploadListener(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        File file = new File(fileDirectory + uFile.getFileName());
        
        if (createSystemDirectory(fileDirectory)) {
            try (FileOutputStream fop = new FileOutputStream(file);
                InputStream filecontent = uFile.getInputstream(); ) {
                int bytesRead;
                byte[] content = new byte[4096];    // 4K buffer
            
                while ((bytesRead = filecontent.read(content)) != -1) {
                    fop.write(content, 0, bytesRead);
                }
        
                getFacesContext().addMessage(null, 
                        new FacesMessage(uFile.getFileName() + 
                                     " uploaded successfully."));            
            }
            catch (IOException ex) {
                logger.error("FAIL to upload file " + uFile.getFileName());
                logger.error(ex.getMessage());
                getFacesContext().addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                     uFile.getFileName() + 
                                     " failed to get uploaded.", ""));            
            }
        }
        else {
            // System failed to create the input files directory for this job,
            // shouldn't allow the user to continue.
            logger.error("FAIL to create input files directory!");
            getFacesContext().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "System failed to create input directory!\n"
                                + "Pipeline will not run!", ""));
        }
        
        return uFile.getFileName();
    }
    
    // Create level one system directories i.e. .../iCOMIC2S/users
    // .../iCOMIC2S/input .../iCOMIC2S/finalize_output
    public static Boolean createSystemDirectories(String systemDir) {
        Boolean result = 
                createSystemDirectory(systemDir + Constants.getUSERS_PATH()) &&
                createSystemDirectory(systemDir + Constants.getINPUT_PATH()) &&
                createSystemDirectory(systemDir + Constants.getFINALIZE_PATH());
        
        return result;
    }
    
    // Create level two system directories for each user i.e.
    // .../iCOMIC2S/users/whtay/output
    // .../iCOMIC2S/users/whtay/config
    // .../iCOMIC2S/users/whtay/log
    public static Boolean createUsersDirectories(String homeDir) {
        Boolean result = 
                createSystemDirectory(homeDir) && 
                createSystemDirectory(homeDir + Constants.getOUTPUT_PATH()) &&
                createSystemDirectory(homeDir + Constants.getCONFIG_PATH()) &&
                createSystemDirectory(homeDir + Constants.getLOG_PATH());
        
        return result;
    }
    
    // Create level two system directory for each Study i.e.
    // .../iCOMIC2S/input/Bayer
    public static Boolean createStudyDirectory(String study_id) {
        return createSystemDirectory(Constants.getSYSTEM_PATH() + 
                                     Constants.getINPUT_PATH() + study_id);
    }
    
    // Create the system directory used for storing input files.
    public static Boolean createSystemDirectory(String systemDir) {
        Boolean result = Constants.OK;
        File dir = new File(systemDir);
        
        if (!dir.exists()) {
            // System directory didn't exists
            if (dir.mkdir()) {
                logger.debug("Directory " + systemDir + " created.");
            }
            else {
                logger.error("FAIL to create directory " + systemDir);
                result = Constants.NOT_OK;
            }
        }
        
        return result;
    }
    
    // Setup and store the local path of the input files folder; to be use in
    // config file creation.
    private void setLocalDirectoryPath(String fullpath) {
        localDirectoryPath = fullpath + File.separator;
    }
    
    // Return the local path of the input files folder.
    public String getLocalDirectoryPath() {
        return localDirectoryPath;
    }
    
    // Return the list of input files that have been uploaded by the 
    // user; multiple files upload. 
    // This method will be called multiple times, hence the business logic to 
    // create the inputList have been moved to createInputList and this method
    // will only return the inputList.
    public List<String> getInputFileList() {
        return inputList;
    }

    // Return the no of input files uploaded.
    public String getFilesCount() {
        return "Sample File: " + fileCount;
    }
    
    // Create the inputList for job submission confirmation by the user; use
    // for multiple files upload.
    public void createInputList() {
        inputList = new ArrayList<>(fileList.values());
    }
    
    // Return the input files directory for this pipeline job.
    public String getFileDirectory() {
        return fileDirectory;
    }
    
    // Set the input files directory for this pipeline job i.e.
    // .../iCOMIC2S/input/Bayer/20151210_1502
    // This method will be called by the constructor.
    private void setFileDirectory(String study_id, String submitTime) {
        fileDirectory = Constants.getSYSTEM_PATH() + 
                        Constants.getINPUT_PATH() +
                        study_id + File.separator +
                        submitTime + File.separator;
        
        File local = new File(fileDirectory);
        // Set the local file path; to be use during config file creation.
        setLocalDirectoryPath(local.getAbsolutePath());
    }

    // Check whether any input file uploaded by the user.
    public Boolean isFilelistEmpty() {
        return fileList.isEmpty();
    }
    
    // Return the input filename that has been been uploaded by the 
    // user; single file upload.
    public String getInputFilename() {
        if (fileList.isEmpty()) {
            return "This field is required.";
        }
        else {
            return fileList.get(1);            
        }
    }
    
    // Rename the sample annotation file to a common filename 
    // (i.e. ANNOTATION.txt)
    public void renameAnnotFile() {
        Path from = FileSystems.getDefault().getPath
            (localDirectoryPath + getInputFilename());
        Path to = FileSystems.getDefault().getPath
            (localDirectoryPath + 
                Constants.getSAMPLE_ANNOT_FILE_NAME() + 
                Constants.getSAMPLE_ANNOT_FILE_EXT());
        // Rename the filename (from -> to).
        try {
            Files.move(from, to);
            logger.debug("Sample annotation file renamed.");
        }
        catch (IOException ioe) {
            logger.error("FAIL to rename sample annotation file!");
            logger.error(ioe.getMessage());
        }
    }
    
    // Rename the control probe file to a common filename 
    // (i.e. CONTROL_PROBE.txt)
    public void renameCtrlProbeFile() {
        Path from = FileSystems.getDefault().getPath
            (localDirectoryPath + getInputFilename());
        Path to = FileSystems.getDefault().getPath
            (localDirectoryPath +
                Constants.getCONTROL_PROBE_FILE_NAME() +
                Constants.getCONTROL_PROBE_FILE_EXT());
        // Rename the filename (from -> to).
        try {
            Files.move(from, to);
            logger.debug("Control probe file renamed.");
        }
        catch (IOException ioe) {
            logger.error("FAIL to rename control probe file!");
            logger.error(ioe.getMessage());
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
}
