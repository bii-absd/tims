/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.General;

import Clinical.Data.Sink.Bean.FileUploadBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
// Libraries for Java Extension
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Constants is a helper class that store all the constant variables used in the
 * system. The configuration file used to setup the variables will be passed
 * in the setup method.
 * 
 * Author: Tay Wei Hong
 * Date: 18-Sep-2015
 * 
 * Revision History
 * 18-Sep-2015 - Created with all the standard getters and setters.
 * 22-Sep-2015 - Added in the constants for input file, config file and 
 * database. Allow flexible configuration coming from setup file.
 * 28-Sep-2015 - Added in 2 new constants CHECK_VALID and CHECK_INVALID; to be
 * use by authentication process. Defined general constants for success and
 * failure.
 * 30-Sep-2015 - Reading in more constants for array pipeline.
 * 01-Oct-2015 - Added 2 new constants LOGOFF and OUTPUTFILE_PATH.
 * 06-Oct-2015 - Added in comments for the code. Added Log4j2 for this class.
 * 08-Oct-2015 - Changed the scope of this class to application, hence we only
 * need to load from the setup file once only. Added 2 new Boolean constants 
 * OK and NOT_OK.
 * 14-Oct-2015 - Using annotation for JSF. Added one new constant ERROR. Moved 
 * the initialization code out of the Constructor.
 * 15-Oct-2015 - Critical error handling.
 * 27-Oct-2015 - Ported to JSF 2.2
 * 30-Oct-2015 - Add 2 new String constants, TRUE and FALSE.
 * 02-Nov-2015 - Changes in the naming convention for all file related 
 * constants. Added one new constant, DIRECTORY_SEPARATOR.
 * 03-Nov-2015 - Added in one new method, getDateTime to return the current date
 * and time. Added one new constant NONE.
 * 04-Nov-2015 - Added one new constant, ACCOUNT_MANAGEMENT.
 * 06-Nov-2015 - Removed COMMAND and DIRECTORY_SEPARATOR constants.
 * 09-Nov-2015 - Added in faces-redirect = true for all the navigation strings.
 * Removed ILLUMINA and AFFYMETRIX constants.
 * 11-Nov-2015 - Added two new constants PAGES_DIR and ACCOUNT_MANAGEMENT_STAY. 
 * Changed the return type of setup method. Changed the value of LOGIN_PAGE.
 * 16-Nov-2015 - Added two new constants PIPELINE_COMMAND_MANAGEMENT and 
 * ITEM_LIST_MANAGEMENT, and deleted constant ACCOUNT_MANAGEMENT_STAY.
 * 01-Dec-2015 - Changed the value of DATABASE_INVALID_STR from null to "NOT
 * FOUND!".
 * 02-Dec-2015 - Implemented the changes in the input folder directory.
 * 07-Dec-2015 - Added one new constant, STUDY_MANAGEMENT. Deleted six 
 * constants, INSERT_SUCCESS, INSERT_FAIL, SUCCESS, FAILURE, CHECK_VALID and 
 * CHECK_INVALID.
 * 14-Dec-2015 - Added one new constant, CLINICAL_DATA_MANAGEMENT.
 * 16-Dec-2015 - Added two new constants, SAMPLE_ANNOT_FILE_NAME and
 * SAMPLE_ANNOT_FILE_EXT.
 * 22-Dec-2015 - Added two new constants, CONTROL_PROBE_FILE_NAME and 
 * CONTROL_PROBE_FILE_EXT.
 * 24-Dec-2015 - Added 3 new constants, FINALIZE_PATH, FINALIZE_FILE_NAME and
 * FINALIZE_FILE_EXT.
 * 28-Dec-2015 - Added 2 new constants, FINALIZE_STUDY and COMPLETED_STUDY_OUTPUT.
 * 06-Jan-2016 - Added one new constant, SUMMARY_FILE_EXT. Renamed 
 * FINALIZE_FILE_NAME to SUMMARY_FILE_NAME. Standardize the setup for all 
 * system paths.
 * 11-Jan-2016 - Added 2 new constants, METH_PIPELINE and METH_PIPELINE_PAGE.
 * 19-Jan-2016 - Added 2 new constants, CNV_PIPELINE and CNV_PIPELINE_PAGE.
 * 20-Jan-2016 - Removed all pipeline related constants.
 * 26-Jan-2016 - Added the constants for categories of activity.
 * 29-Jan-2016 - To use a common system setup file for both Windows and Linux OS.
 * 01-Feb-2016 - Added one new constant, JOB_STATUS.
 * 12-Feb-2016 - Added one new constant, EXE_UNFIN.
 * 19-Feb-2016 - Added one new constant, PIC_PATH.
 * 23-Feb-2016 - Changed constant name from ITEM_LIST_MANAGEMENT to 
 * GROUP_MANAGEMENT.
 * 24-Feb-2016 - Added one new constant, STUDIES_REVIEW for studies review page.
 * 01-Mar-2016 - All the system directories will be created during system
 * parameters setup (instead of during user login).
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 14-Mar-2016 - Added one new constant, EXE_CLSTUDY; activity code for closing
 * study.
 */

@ManagedBean (name = "constants")
@ApplicationScoped
public class Constants {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(Constants.class.getName());
    // Database return status
    public final static int DATABASE_INVALID_ID = -1;
    public final static String DATABASE_INVALID_STR = "NOT FOUND!";
    public final static Boolean OK = true;
    public final static Boolean NOT_OK = false;
    // General return status
    public final static String LOGOFF = "logoff";
    public final static String TRUE = "true";
    public final static String FALSE = "false";
    public final static String NONE = "none";
    // Categories of activity (LOG, EXE, CRE, CHG & DWL).
    public final static String LOG_IN = "Login";
    public final static String LOG_OFF = "Logoff";
    public final static String EXE_PL = "Run Pipeline";
    public final static String EXE_FIN = "Finalize Study";
    public final static String EXE_UNFIN = "Unfinalize Study";
    public final static String EXE_CLSTUDY = "Close Study";
    public final static String CRE_ID = "Create ID";
    public final static String CHG_PWD = "Change Password";
    public final static String CHG_ID = "Update ID";
    public final static String DWL_FIL = "Download File";
    // Navigation Strings
    public final static String PAGES_DIR = "restricted/";
    // For Login page, we shouldn't redirect because most of the time the 
    // system is going from Login back to Login.
    public final static String LOGIN_PAGE = "/login";
    public final static String MAIN_PAGE = "main?faces-redirect=true";
    public final static String ACCOUNT_MANAGEMENT = "accountmanagement";
    public final static String PIPELINE_MANAGEMENT = "pipelinemanagement";
    public final static String GROUP_MANAGEMENT = "groupmanagement";
    public final static String STUDY_MANAGEMENT = "studymanagement";
    public final static String CLINICAL_DATA_MANAGEMENT = "clinicaldatamanagement";
    public final static String JOB_STATUS = "jobstatus?faces-redirect=true";
    public final static String FINALIZE_STUDY = "finalizestudy";
    public final static String COMPLETED_STUDY_OUTPUT = "completedstudyoutput";
    public final static String STUDIES_REVIEW = "studiesreview?faces-redirect=true";
    public final static String NGS_PAGE = "ngs?faces-redirect=true";
    public final static String ERROR = "error?faces-redirect=true";
    // Constants read in from setup file.
    private static String SYSTEM_PATH = null;
    private static String USERS_PATH = null;
    private static String PIC_PATH = null;
    private static String OUTPUT_PATH = null;
    private static String INPUT_PATH = null;
    private static String CONFIG_PATH = null;
    private static String LOG_PATH = null;
    private static String FINALIZE_PATH = null;
    private static String OUTPUTFILE_NAME = null;
    private static String OUTPUTFILE_EXT = null;
    private static String REPORTFILE_NAME = null;
    private static String REPORTFILE_EXT = null;
    private static String LOGFILE_NAME = null;
    private static String LOGFILE_EXT = null;
    private static String CONFIG_FILE_NAME = null;
    private static String CONFIG_FILE_EXT = null;
    private static String SUMMARY_FILE_NAME = null;
    private static String SUMMARY_FILE_EXT = null;
    private static String FINALIZE_FILE_EXT = null;
    private static String SAMPLE_ANNOT_FILE_NAME = null;
    private static String SAMPLE_ANNOT_FILE_EXT = null;
    private static String CONTROL_PROBE_FILE_NAME = null;
    private static String CONTROL_PROBE_FILE_EXT = null;
    // Constants used in database setup
    private static String DATABASE_NAME = null;
    private static String DATABASE_DRIVER = null;

    public Constants() {}
    
    // No setters for the setup parameters will be provided.
    // Function setup will load the parameters value from the file passed in uri.
    public static Boolean setup(String uri, String root) {
        // The scope for this class is application, hence we can skip the 
        // loading from the setup file if it has already been done.
        if (DATABASE_DRIVER == null) {
            HashMap<String, String> setup = new HashMap<>();
        
            try (BufferedReader br = new BufferedReader(new FileReader(uri)))
            {
                String currentLine;
            
                while ((currentLine = br.readLine()) != null) {
                    if (!currentLine.startsWith("#")) {
                        // Read in the configuration pair one by one
                        String[] configPair = currentLine.split(" ");
                        if (configPair.length == 2) {
                            setup.put(configPair[0], configPair[1]);
                        }
                    }
                }
                logger.debug(uri + " loaded.");
            } catch (IOException|NullPointerException e) {
                logger.error("FAIL to load " + uri);
                logger.error(e.getMessage());
                return NOT_OK;
            }
        
            // Setup the config parameters
            SYSTEM_PATH = root + File.separator + setup.get("APP_NAME");
            USERS_PATH = 
                    File.separator + setup.get("USERS_PATH") + File.separator;
            PIC_PATH = 
                    File.separator + setup.get("PIC_PATH") + File.separator;
            OUTPUT_PATH = 
                    File.separator + setup.get("OUTPUT_PATH") + File.separator;
            INPUT_PATH = 
                    File.separator + setup.get("INPUT_PATH") + File.separator;
            CONFIG_PATH = 
                    File.separator + setup.get("CONFIG_PATH") + File.separator;
            LOG_PATH = 
                    File.separator + setup.get("LOG_PATH") + File.separator;
            FINALIZE_PATH = 
                    File.separator + setup.get("FINALIZE_PATH") + File.separator;
            OUTPUTFILE_NAME = setup.get("OUTPUTFILE_NAME");
            OUTPUTFILE_EXT = setup.get("OUTPUTFILE_EXT");
            REPORTFILE_NAME = setup.get("REPORTFILE_NAME");
            REPORTFILE_EXT = setup.get("REPORTFILE_EXT");
            LOGFILE_NAME = setup.get("LOGFILE_NAME");
            LOGFILE_EXT = setup.get("LOGFILE_EXT");
            CONFIG_FILE_NAME = setup.get("CONFIG_FILE_NAME");
            CONFIG_FILE_EXT = setup.get("CONFIG_FILE_EXT");
            SUMMARY_FILE_NAME = setup.get("SUMMARY_FILE_NAME");
            SUMMARY_FILE_EXT = setup.get("SUMMARY_FILE_EXT");
            FINALIZE_FILE_EXT = setup.get("FINALIZE_FILE_EXT");
            SAMPLE_ANNOT_FILE_NAME = setup.get("SAMPLE_ANNOT_FILE_NAME");
            SAMPLE_ANNOT_FILE_EXT = setup.get("SAMPLE_ANNOT_FILE_EXT");
            CONTROL_PROBE_FILE_NAME = setup.get("CONTROL_PROBE_FILE_NAME");
            CONTROL_PROBE_FILE_EXT = setup.get("CONTROL_PROBE_FILE_EXT");
            DATABASE_NAME = setup.get("DATABASE_NAME");
            DATABASE_DRIVER = setup.get("DATABASE_DRIVER");
            
            // Create system directories.
            if (!FileUploadBean.createSystemDirectories(SYSTEM_PATH)) {
                logger.error("FAIL to create system directories!");
                return NOT_OK;
            }
        }
        
        return OK;
    }
    
    // To display the date and time.
    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy hh:mmaa");
        return dateFormat.format(new Date());
    }
    
    // Display date and time at the views.
    public String getDisplayDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss aa");
        return dateFormat.format(new Date());        
    }
        
    // Machine generated getters
    public static String getSYSTEM_PATH() { return SYSTEM_PATH; }
    public static String getUSERS_PATH() { return USERS_PATH; }
    public static String getPIC_PATH() { return PIC_PATH; }
    public static String getOUTPUT_PATH() { return OUTPUT_PATH; }
    public static String getINPUT_PATH() { return INPUT_PATH; }
    public static String getCONFIG_PATH() { return CONFIG_PATH; }
    public static String getLOG_PATH() { return LOG_PATH; }
    public static String getFINALIZE_PATH() { return FINALIZE_PATH; }
    public static String getOUTPUTFILE_NAME() { return OUTPUTFILE_NAME; }
    public static String getOUTPUTFILE_EXT() { return OUTPUTFILE_EXT; }
    public static String getREPORTFILE_NAME() { return REPORTFILE_NAME; }
    public static String getREPORTFILE_EXT() { return REPORTFILE_EXT; }
    public static String getLOGFILE_NAME() { return LOGFILE_NAME; }
    public static String getLOGFILE_EXT() { return LOGFILE_EXT; }
    public static String getCONFIG_FILE_NAME() { return CONFIG_FILE_NAME; }
    public static String getCONFIG_FILE_EXT() { return CONFIG_FILE_EXT; }
    public static String getSUMMARY_FILE_NAME() { return SUMMARY_FILE_NAME; }
    public static String getSUMMARY_FILE_EXT() { return SUMMARY_FILE_EXT; }
    public static String getFINALIZE_FILE_EXT() { return FINALIZE_FILE_EXT; }
    public static String getSAMPLE_ANNOT_FILE_NAME() 
    { return SAMPLE_ANNOT_FILE_NAME; }
    public static String getSAMPLE_ANNOT_FILE_EXT() 
    { return SAMPLE_ANNOT_FILE_EXT; }
    public static String getCONTROL_PROBE_FILE_NAME() 
    { return CONTROL_PROBE_FILE_NAME; }
    public static String getCONTROL_PROBE_FILE_EXT() 
    { return CONTROL_PROBE_FILE_EXT; }
    public static String getDATABASE_NAME() { return DATABASE_NAME; }
    public static String getDATABASE_DRIVER() { return DATABASE_DRIVER; }
}
