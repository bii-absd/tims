/*
 * Copyright @2016
 */
package TIMS.General;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
// Libraries for Java Extension
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * FileHelper is an abstract class and not mean to be instantiate, its main job
 * is to perform file uploading/downloading operations for the user at the 
 * client side.
 * 
 * Author: Tay Wei Hong
 * Date: 06-Jan-2016
 * 
 * Revision History
 * 06-Jan-2016 - Created with one method, download.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 19-May-2016 - Rename class name from FileLoader to FileHelper. Added 2 static
 * methods, delete and zipFiles.
 * 25-Aug-2016 - Added 2 static methods, fileExist and deleteDirectory.
 * 30-Aug-2016 - Added 2 static methods, moveFile and getFilesWithExt.
 */

public abstract class FileHelper {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FileHelper.class.getName());

    // Delete the file passed in.
    public static boolean delete(String filepath) {
        File delFile = new File(filepath);
        
        return delFile.delete();
    }
    
    // Zip the file(s) in srcFiles to zip file zipFile.
    public static void zipFiles(String zipFile, String[] srcFiles) 
            throws IOException {
        byte[] buffer = new byte[2048];
        FileOutputStream fos = new FileOutputStream(zipFile);
        
        try (ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (String srcFile : srcFiles) {
                File toZip = new File(srcFile);
                try (FileInputStream fis = new FileInputStream(toZip)) {
                    zos.putNextEntry(new ZipEntry(toZip.getName()));
                    int len;
                        
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                        
                    zos.closeEntry();
                }
            }
        }
    }
    
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
    
    // Return true if the file exist.
    public static boolean fileExist(String filepath) {
        File f = new File(filepath);
        
        return f.exists();
    }
    
    // Return all the file(s) in the directory that end with ext.
    public static File[] getFilesWithExt(String directory, String ext) {
        File dir = new File(directory);
        File[] matchedFile = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(ext);
            }
        });
        
        return matchedFile;
    }
    
    // Delete the directory recursively.
    public static void deleteDirectory(String dir) throws IOException {
        Path directory = Paths.get(dir);
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) 
                   throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) 
                   throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
           
        });
        
        logger.debug(dir + " deleted.");
    }
    
    // Move the file from src to dest.
    public static boolean moveFile(String src, String dest) {
        boolean result = Constants.OK;
        Path from = FileSystems.getDefault().getPath(src);
        Path to = FileSystems.getDefault().getPath(dest);
        // Move the file (from -> to), and replace existing file if found.
        try {
            if (Files.exists(to)) {
                Files.move(from, to, REPLACE_EXISTING);
            }
            else {
                Files.move(from, to);
            }
            logger.debug("File moved to " + dest);
        }
        catch (IOException ioe) {
            result = Constants.NOT_OK;
            logger.error("FAIL to move file. SRC: " + src + " DEST: " + dest);
            logger.error(ioe.getMessage());
        }
        
        return result;
    }
}
