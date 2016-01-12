/*
 * Copyright @2016
 */
package Clinical.Data.Sink.General;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * FileLoader is an abstract class and not mean to be instantiate, its main job
 * is to perform file uploading/downloading operations for the user at the 
 * client side.
 * 
 * Author: Tay Wei Hong
 * Date: 06-Jan-2016
 * 
 * Revision History
 * 06-Jan-2016 - Created with one method, download.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 */

public abstract class FileLoader {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FileLoader.class.getName());

    // Download the file from the filepath.
    public static void download(String filepath) {
       // Get ready the file for user to download
        File file = new File(filepath);
        String filename = file.getName();
        int contentLength = (int) file.length();
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        // Some JSF component library or some filter might have set some headers
        // in the buffer beforehand. We want to clear them, else they may collide.
        ec.responseReset();
        // Auto-detect the media-types based on filename
        ec.setResponseContentType(ec.getMimeType(filename));
        // Set the file size, so that the download progress will be known.
        ec.setResponseContentLength(contentLength);
        // Create the Sava As popup
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\""
                            + filename + "\"");
        
        try (FileInputStream fis = new FileInputStream(file)){
            OutputStream os = ec.getResponseOutputStream();
            byte[] buffer = new byte[2048]; // 2K byte-buffer
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer,0,bytesRead);
            }
        } catch (IOException ex) {
            logger.error("FAIL to download " + filename);
            logger.error(ex.getMessage());
        }
        
        // Important! Otherwise JSF will attempt to render the response which
        // will fail since it's already written with a file and closed.
        FacesContext.getCurrentInstance().responseComplete();
        logger.info(filename + " downloaded.");
    }
}
