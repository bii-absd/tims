/*
 * Copyright @2016-2018
 */
package TIMS.General;

import TIMS.Database.StudyDB;
import TIMS.Database.SubjectDB;
import TIMS.Database.SubjectDetail;
// Libraries for Java
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
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
 * is to perform general file operations.
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
 * 25-Apr-2017 - Added 1 static method, generateMetaDataList.
 * 12-Apr-2017 - Added 4 static methods; copyUploadedFileToLocalDirectory, 
 * convertByteArrayToList, convertObjectToByteArray and convertStrListToStr. 
 * Modified method generateMetaDataList.
 * 15-May-2018 - When generating the meta data list, the core data values will
 * be taken from the subject detail instead of the column data.
 * 10-Jul-2018 - Minor changes in method generateMetaDataList due to changes in
 * method SubjectDB.getSubtDetailList
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
    
    // Zip the file(s) in srcFiles to zipFile.
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
            StringBuilder oper = new StringBuilder(src).append(" moved to ").
                                                        append(dest);
            if (Files.exists(to)) {
                Files.move(from, to, REPLACE_EXISTING);
            }
            else {
                Files.move(from, to);
            }
//            logger.debug("File moved to " + dest);
            logger.info(oper);
        }
        catch (IOException ioe) {
            StringBuilder err = new StringBuilder("FAIL to move file. SRC: ").
                    append(src).append(" DEST: ").append(dest);
            result = Constants.NOT_OK;
//            logger.error("FAIL to move file. SRC: " + src + " DEST: " + dest);
            logger.error(err);
            logger.error(ioe.getMessage());
        }
        
        return result;
    }
    
    // Copy the uploaded file to a local directory.
    public static boolean copyUploadedFileToLocalDirectory
        (org.primefaces.model.UploadedFile src, String localDir) 
    {
        try {
            InputStream ipStream = src.getInputstream();
            OutputStream opStream = new FileOutputStream(new File(localDir));
            int len = 0;
            byte[] buffer = new byte[1024];
            while ( (len = ipStream.read(buffer)) > 0) {
                opStream.write(buffer, 0, len);
            }
        } catch (IOException ioe) {
            logger.error("Failed to copy uploaded file to local directory!");
            logger.error(ioe.getMessage());
            return Constants.NOT_OK;
        }
        return Constants.OK;
    }
    
    // Convert byte array to List<String> (after reading bytea from database).
    public static List<String> convertByteArrayToList(byte[] data) {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        
        try (ObjectInputStream ois = new ObjectInputStream(byteIn))
        {
            @SuppressWarnings("unchecked")
            List<String>list = (List<String>) ois.readObject();
            
            return list;
        } catch (IOException|ClassNotFoundException ex) {
            logger.error("FAIL to convert byte array to list!");
            logger.error(ex.getMessage());
        }
        return null;
    }
    
    // Convert the object to byte array (for writting to database).
    public static byte[] convertObjectToByteArray(Object obj) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        
        try (ObjectOutputStream oos = new ObjectOutputStream(byteOut))
        {
            oos.writeObject(obj);
        } catch (IOException ioe) {
            logger.error("FAIL to convert object to byte array!");
            logger.error(ioe.getMessage());
        }

        return byteOut.toByteArray();
    }
    
    // Convert the list of string to a string whereby each individual string
    // is separated by '|'
    private static String convertStrListToStr(List<String> strList) {
        StringBuilder strBuilder = new StringBuilder();
        
        for (String str : strList) {
            strBuilder.append(str).append("|");
        }
        
        return strBuilder.toString();
    }
    
    // Helper function to consolidate and generate the Meta data list for the
    // study ID passed in.
    public static boolean generateMetaDataList(String study_id, String filepath) {
        boolean status = Constants.NOT_OK;
        StringBuilder line = 
                new StringBuilder("Subject ID|Date of Birth|CaseControl|Gender|Race|Height|Weight|Age at Baseline|Record Date|");
        SubjectDB subjects = new SubjectDB(study_id);
        
        try {
            byte[] dat = StudyDB.getColumnNameList(study_id);
            if (dat != null) {
                List<String> dbColNameL = convertByteArrayToList(dat);
                List<SubjectDetail> subjectDetailList = 
                                                subjects.getSubtDetailList();
                PrintStream ps = new PrintStream(new File(filepath));
                // Include the column IDs.
                line.append(convertStrListToStr(dbColNameL));
                ps.println(line.toString());
                // Empty the string.
                line.delete(0, line.length());
                // Write the record for each subject.
                for (SubjectDetail subj : subjectDetailList) {
                    List<String> record = FileHelper.convertByteArrayToList(subj.getDat());
                    
                    line.append(subj.getSubject_id()).append("|").
                         append(subj.getDob()).append("|").
                         append(subj.getCasecontrol()).append("|").
                         append(subj.getGender()).append("|").
                         append(subj.getRace()).append("|").
                         append(subj.getHeight()).append("|").
                         append(subj.getWeight()).append("|").
                         append(subj.getAge_at_baseline()).append("|").
                         append(subj.getRecord_date()).append("|").
                         append(convertStrListToStr(record));
                    ps.println(line.toString());
                    // Empty the string after each subject Meta data.
                    line.delete(0, line.length());
                }
            
                ps.close();
                status = Constants.OK;
            }
        }
        catch (IOException ioe) {
            logger.error("FAIl to generate meta data list for study " + study_id);
            logger.error(ioe.getMessage());
        }
        
        return status;
    }
}
