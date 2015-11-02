/*
 * Copyright @2015
 */
package Clinical.Data.Sink.General;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
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
 */

@ManagedBean (name = "constants")
@ApplicationScoped
public class Constants {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(Constants.class.getName());
    // Database return status
    public final static int DATABASE_INVALID_ID = -1;
    public final static String DATABASE_INVALID_STR = null;
    public final static Boolean CHECK_VALID = true;
    public final static Boolean CHECK_INVALID = false;
    public final static Boolean INSERT_SUCCESS = true;
    public final static Boolean INSERT_FAIL = false;
    public final static Boolean OK = true;
    public final static Boolean NOT_OK = false;
    // General return status
    public final static String SUCCESS = "success";
    public final static String FAILURE = "failure";
    public final static String LOGOFF = "logoff";
    public final static String TRUE = "true";
    public final static String FALSE = "false";
    // Vendor
    public final static String ILLUMINA = "Illumina";
    public final static String AFFYMETRIX = "Affymetrix";
    // Navigation Strings
    public final static String LOGIN_PAGE = "login";
    public final static String MAIN_PAGE = "main";
    public final static String GEX_ILLUMINA = "gex-illumina";
    public final static String GEX_AFFYMETRIX = "gex-affymetrix";
    public final static String ARRAYCONFIG_PAGE = "arrayconfig";
    public final static String NGS_PAGE = "ngs";
    public final static String ERROR = "error";
    // Constants used in Array pipeline
    private static String SYSTEM_PATH = null;
    private static String OUTPUT_PATH = null;
    private static String INPUT_PATH = null;
    private static String CONFIG_PATH = null;
    private static String LOG_PATH = null;
    private static String OUTPUTFILE_NAME = null;
    private static String OUTPUTFILE_EXT = null;
    private static String REPORTFILE_NAME = null;
    private static String REPORTFILE_EXT = null;
    private static String LOGFILE_NAME = null;
    private static String LOGFILE_EXT = null;
    private static String CONFIG_FILE_NAME = null;
    private static String CONFIG_FILE_EXT = null;
    // Constants that identify the directory separator for OS
    private static String DIRECTORY_SEPARATOR = null;
    // Constants used in database setup
    private static String DATABASE_NAME = null;
    private static String DATABASE_DRIVER = null;
    // Commands for running pipeline
    private static String COMMAND1 = null;
    private static String COMMAND2 = null;

    public Constants() {}
    
    // No setters for the setup parameters will be provided.
    // Function setup will load the parameters value from the file passed in uri.
    public static String setup(String uri) {
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
            } catch (IOException e) {
                logger.error("IOException encountered while loading " + uri);
                logger.error(e.getMessage());
                return ERROR;
            }
        
            // Setup the config parameters
            SYSTEM_PATH = setup.get("SYSTEM_PATH");
            OUTPUT_PATH = setup.get("OUTPUT_PATH");
            INPUT_PATH = setup.get("INPUT_PATH");
            CONFIG_PATH = setup.get("CONFIG_PATH");
            LOG_PATH = setup.get("LOG_PATH");
            OUTPUTFILE_NAME = setup.get("OUTPUTFILE_NAME");
            OUTPUTFILE_EXT = setup.get("OUTPUTFILE_EXT");
            REPORTFILE_NAME = setup.get("REPORTFILE_NAME");
            REPORTFILE_EXT = setup.get("REPORTFILE_EXT");
            LOGFILE_NAME = setup.get("LOGFILE_NAME");
            LOGFILE_EXT = setup.get("LOGFILE_EXT");
            CONFIG_FILE_NAME = setup.get("CONFIG_FILE_NAME");
            CONFIG_FILE_EXT = setup.get("CONFIG_FILE_EXT");
            DIRECTORY_SEPARATOR = setup.get("DIRECTORY_SEPARATOR");
            DATABASE_NAME = setup.get("DATABASE_NAME");
            DATABASE_DRIVER = setup.get("DATABASE_DRIVER");
            COMMAND1 = setup.get("COMMAND1");
            COMMAND2 = setup.get("COMMAND2");
        }
        
        return SUCCESS;
    }
    
    // Machine generated getters
    public String getGEX_AFFYMETRIX() { return GEX_AFFYMETRIX; }
    public String getGEX_ILLUMINA() { return GEX_ILLUMINA; }
    public String getAFFYMETRIX() { return AFFYMETRIX; }
    public String getILLUMINA() { return ILLUMINA; }
    public static String getSYSTEM_PATH() { return SYSTEM_PATH; }
    public static String getOUTPUT_PATH() { return OUTPUT_PATH; }
    public static String getINPUT_PATH() { return INPUT_PATH; }
    public static String getCONFIG_PATH() { return CONFIG_PATH; }
    public static String getLOG_PATH() { return LOG_PATH; }
    public static String getOUTPUTFILE_NAME() { return OUTPUTFILE_NAME; }
    public static String getOUTPUTFILE_EXT() { return OUTPUTFILE_EXT; }
    public static String getREPORTFILE_NAME() { return REPORTFILE_NAME; }
    public static String getREPORTFILE_EXT() { return REPORTFILE_EXT; }
    public static String getLOGFILE_NAME() { return LOGFILE_NAME; }
    public static String getLOGFILE_EXT() { return LOGFILE_EXT; }
    public static String getCONFIG_FILE_NAME() { return CONFIG_FILE_NAME; }
    public static String getCONFIG_FILE_EXT() { return CONFIG_FILE_EXT; }
    public static String getDIRECTORY_SEPARATOR() { return DIRECTORY_SEPARATOR; }
    public static String getDATABASE_NAME() { return DATABASE_NAME; }
    public static String getDATABASE_DRIVER() { return DATABASE_DRIVER; }
    public static String getCOMMAND1() { return COMMAND1; }
    public static String getCOMMAND2() { return COMMAND2; }
}
