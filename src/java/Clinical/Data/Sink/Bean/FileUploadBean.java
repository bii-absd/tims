/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.General.Constants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
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
 * 27-Oct-2015 - Created with the main function fileUploadListener, that is able
 * to handle multiple uploaded files at one time.
 * 28-Oct-2015 - Changed to allow this class to handle both single and multiple
 * file upload.
 */

@ManagedBean (name="fileUploadBean")
@ViewScoped
public class FileUploadBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FileUploadBean.class.getName());
    private int fileCount = 0;
    private static String fileDirectory = null;
    private LinkedHashMap<Integer,String> fileList = new LinkedHashMap();

    public FileUploadBean() {}
    
    // fileUploadListener will get call for each file uploaded.
    public void fileUploadListener(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        // Store the uploaded filename for configuration review later.
        fileList.put(++fileCount, uFile.getFileName());
        File file = new File(getFileDirectory() + uFile.getFileName());
        
        try (FileOutputStream fop = new FileOutputStream(file);
             InputStream filecontent = uFile.getInputstream(); ) {
            int bytesRead = 0;
            byte[] content = new byte[4096];    // 4K buffer
            
            while ((bytesRead = filecontent.read(content)) != -1) {
                fop.write(content, 0, bytesRead);
            }
        
            getFacesContext().addMessage(null, 
                    new FacesMessage(uFile.getFileName() + " uploaded successfully."));
            
        }
        catch (IOException ex) {
            logger.error(AuthenticationBean.getUserName() +
                         ": encountered error in uploading file " +
                         uFile.getFileName());
            logger.error(ex.getMessage());
        }
        
        logger.debug("File list: " + fileList.toString());
    }
    
    // Return the full path of the input folder for this pipeline job.
    private String getFileDirectory() {
        if (fileDirectory == null) {
            DateFormat dateFormat = new SimpleDateFormat("ddMMM_HHmm");
            fileDirectory = Constants.getSYSTEM_PATH() + 
                            AuthenticationBean.getUserName() +
                            Constants.getINPUTFILE_PATH() +
                            dateFormat.format(new Date()) + "//";
            createSystemDirectory(fileDirectory);
        }
        
        return fileDirectory;
    }
    
    // Helper function to create the system directory used for storing input files.
    public static String createSystemDirectory(String systemDir) {
        String result = Constants.SUCCESS;
        File dir = new File(systemDir);
        
        if (!dir.exists()) {
            // System directory didn't exists
            if (dir.mkdir()) {
                logger.debug("System directory " + systemDir + " created.");
            }
            else {
                logger.error("Failed to create system directory " +
                        systemDir);
                result = Constants.ERROR;
            }
        }
        
        return result;
    }
    
    // Return the list of input files that have been uploaded by the 
    // user; multiple files upload.
    public List<String> getInputFileList() {
        List<String> inputList = new ArrayList<String>(fileList.values());

        System.out.println("Filename: " + fileList.values());
        System.out.println("File count: " + fileCount);

        return inputList;
    }
    
    // Check whether any input file uploaded by the user.
    public Boolean checkFileIsEmpty() {
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
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
}
