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

import TIMS.General.Constants;
import TIMS.General.FileHelper;
// Libraries for Java
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
// Libraries for PrimeFaces
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
    private LinkedHashMap<Integer,String> fileList = new LinkedHashMap<>();
    private boolean inputDir = Constants.NOT_OK;

    // File uploaded will be stored into the directory.
    public FileUploadBean(String directory) {
        fileDirectory = directory;

        setFileDirectory();
    }
    
    // This function is called in Raw Data Management module when user click 
    // on another input data package i.e. need to reset all the changes that
    // has been done so far.
    public void resetFileBean() {
        fileCount = 0;
        fileList.clear();
        if (inputList != null){
            inputList.clear();
        }
    }
    
    // Create the input files directory before the uploading started. This 
    // method is triggered for multiple files upload only.
    public void createInputDirectory() {
        inputDir = createSystemDirectory(fileDirectory);
    }
    
    // Used for multiple files upload.
    public void multipleFileUploadListener(FileUploadEvent event) {
        fileList.put(++fileCount, fileUploadListener(event));
    }
    
    // Used for single file upload.
    public void singleFileUploadListener(FileUploadEvent event) {
        // Create the input files directory first.
        inputDir = createSystemDirectory(fileDirectory);
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

        if (inputDir) {
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
            logger.error("FAIL to create input directory for " + 
                         uFile.getFileName());
            getFacesContext().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "System failed to create directory!\n"
                                + "Task will not execute!", ""));
        }
        
        return uFile.getFileName();
    }
    
    // Create TIMS system directories i.e. /var/TIMS/ .../TIMS/users 
    // .../TIMS/images .../TIMS/input .../TIMS/studies etc.
    public static boolean createSystemDirectories(String systemDir) {
        boolean result = 
                createSystemDirectory(systemDir + File.separator) &&
                createSystemDirectory(systemDir + Constants.getUSERS_PATH()) &&
                createSystemDirectory(systemDir + Constants.getPIC_PATH()) &&
                createSystemDirectory(systemDir + Constants.getINPUT_PATH()) &&
                createSystemDirectory(systemDir + Constants.getSTUDIES_PATH()) &&
                createSystemDirectory(systemDir + Constants.getTMP_PATH()) &&
                createSystemDirectory(systemDir + Constants.getCBIO_PATH());
        
        return result;
    }
    
    // Create user system directories i.e. .../TIMS/users/whtay/output
    // .../TIMS/users/whtay/config
    // .../TIMS/users/whtay/log
    public static boolean createUsersDirectories(String homeDir) {
        boolean result = 
                createSystemDirectory(homeDir) && 
                createSystemDirectory(homeDir + Constants.getOUTPUT_PATH()) &&
                createSystemDirectory(homeDir + Constants.getCONFIG_PATH()) &&
                createSystemDirectory(homeDir + Constants.getLOG_PATH());
        
        return result;
    }
    
    // Create study system directory i.e. .../TIMS/input/Bayer 
    // .../TIMS/cbio/Bayer .../TIMS/studies/Bayer
    public static boolean createStudyDirectory(String study_id) {
        boolean result = 
            createSystemDirectory(Constants.getSYSTEM_PATH() + Constants.getINPUT_PATH() + study_id) &&
            createSystemDirectory(Constants.getSYSTEM_PATH() + Constants.getCBIO_PATH() + study_id) &&
            createSystemDirectory(Constants.getSYSTEM_PATH() + Constants.getSTUDIES_PATH() + study_id);
                
        return result;
    }
    
    // Helper function to create system directory.
    public static boolean createSystemDirectory(String systemDir) {
        boolean result = Constants.OK;
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
    
    // Compare the filename listed in the annotation file with the list of 
    // files received by the application. Return the list of filenames not
    // received by the application.
    public List<String> compareFileList(List<String> annotList) {
        boolean received = false;
        List<String> missingList = new ArrayList<>();
        
        // Compare all the filename listed in the annotation file with the 
        // files received.
        for (String filename : annotList) {
            for (int i = 0; i < inputList.size(); i++) {
                if (filename.compareToIgnoreCase(inputList.get(i)) == 0) {
                    // Input file is received by the application.
                    received = true;
                    break;
                }
            }

            if (received) {
                received = false;
            }
            else {
                // Add the filename to the missing list as it is not received
                // by the application.
                missingList.add(filename);
            }
        }
        
        return missingList;
    }
    
    // Setup and store the local path of the file directory.
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
    
    // Set the file directory for this upload. This method will be called by 
    // the constructor.
    private void setFileDirectory() {
        File local = new File(fileDirectory);
        // Set the local file path; to be use during config file creation.
        setLocalDirectoryPath(local.getAbsolutePath());
    }

    // Check whether any input file uploaded by the user.
    public boolean isFilelistEmpty() {
        return fileList.isEmpty();
    }
    
    // This function is called by the pipeline configuration pages. Return the 
    // filename of the single file uploaded.
    public String getInputFilename() {
        return getFilename("This field is required.");
    }
    // This function is called by Raw Data Management page. Return the filename
    // of the single file uploaded.
    public String getInputFilenameForRDM() {
        return getFilename("None");
    }
    // Helper function to return the filename of the single file uploaded. The
    // default string will be returned if no file has been uploaded.
    private String getFilename(String noFile) {
        if (fileList.isEmpty()) {
            return noFile;
        }
        else {
            return fileList.get(1);            
        }
    }
    
    // Rename the single file name to the new filename.
    public void renameFilename(String newFilename) {
        if (FileHelper.moveFile(localDirectoryPath + getInputFilename(), 
                                localDirectoryPath + newFilename)) 
        {
            logger.debug(getInputFilename() + " renamed to " + newFilename);
            // Update file list to new filename.
            fileList.replace(1, newFilename);
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
}