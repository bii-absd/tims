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
package TIMS.General;

import TIMS.Bean.FileUploadBean;
// Libraries for Java
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

@ManagedBean (name = "constants")
@ApplicationScoped
public class Constants {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(Constants.class.getName());
    // Database return status
    public final static int DATABASE_INVALID_ID = -1;
    public final static String DATABASE_INVALID_STR = "NOT FOUND!";
    public final static boolean OK = true;
    public final static boolean NOT_OK = false;
    // General return status
    public final static String LOGOFF = "logoff";
    public final static String TRUE = "true";
    public final static String FALSE = "false";
    public final static String NONE = "none";
    public final static String FAILED = "FAILED";
    public final static String EMPTY_STR = "EMPTY";
    // Categories of activity.
    public final static String LOG_IN = "Login";
    public final static String LOG_OFF = "Logoff";
    public final static String EXE_PL = "Run Pipeline";
    public final static String EXE_FIN = "Finalize Study";
    public final static String EXE_UNFIN = "Unfinalize Study";
    public final static String EXE_CLSTUDY = "Close Study";
    public final static String CRE_ID = "Create ID";
    public final static String CHG_PWD = "Change Password";
    public final static String CHG_ID = "Update ID";
    public final static String UPD_DS = "Update Data Source";
    public final static String UPL_MD = "Upload Meta Data";
    public final static String UPD_MD = "Update Meta Data";
    public final static String DEL_MD = "Delete Meta Data";
    public final static String UPL_SSF = "Upload Study Specific Fields";
    public final static String UPL_CDT = "Upload Core Data Tags";
    public final static String DEL_SSF = "Delete Study Specific Fields";
    public final static String DEL_CDT = "Delete Core Data Tags";
    public final static String CHG_RD = "Update Raw Data";
    public final static String CUS_RD = "Customize Raw Data";
    public final static String DWL_FIL = "Download File";
    public final static String UPL_RD = "Upload Raw Data";
    public final static String EXP_DAT = "Export Data";
    public final static String SET_FTE = "Setup Feature";
    public final static String VIS_DAT = "Visualize Study Data";
    // Navigation Strings
    public final static String PAGES_DIR = "restricted/";
    // For Login page, we shouldn't redirect because most of the time the 
    // system is going from Login back to Login.
    public final static String LOGIN_PAGE = "/login";
    public final static String MAIN_PAGE = "main?faces-redirect=true";
    public final static String ACCOUNT_MANAGEMENT = "accountmanagement";
    public final static String PIPELINE_MANAGEMENT = "pipelinemanagement";
    public final static String WORKUNIT_MANAGEMENT = "workunitmanagement";
    public final static String STUDY_MANAGEMENT = "studymanagement";
    public final static String META_DATA_MANAGEMENT = "metadatamanagement";
    public final static String RAW_DATA_MANAGEMENT = "rawdatamanagement";
    public final static String JOB_STATUS = "jobstatus?faces-redirect=true";
    public final static String FINALIZE_STUDY = "finalizestudy";
    public final static String COMPLETED_STUDY_OUTPUT = "completedstudyoutput";
    public final static String STUDIES_REVIEW = "studiesreview?faces-redirect=true";
    public final static String DASHBOARD = "dashboard?faces-redirect=true";
    public final static String JOB_SELECTION_4V = "jobselection4v";
    public final static String ERROR = "error?faces-redirect=true";
    // Constants read in from setup file.
    private static String SYSTEM_PATH = null;
    private static String USERS_PATH = null;
    private static String PIC_PATH = null;
    private static String OUTPUT_PATH = null;
    private static String INPUT_PATH = null;
    private static String CONFIG_PATH = null;
    private static String LOG_PATH = null;
    private static String STUDIES_PATH = null;
    private static String TMP_PATH = null;
    private static String CBIO_PATH = null;
    private static String CBIO_CASE_DIR = null;
    private static String OUTPUTFILE_NAME = null;
    private static String OUTPUTFILE_EXT = null;
    private static String ZIPFILE_EXT = null;
    private static String REPORTFILE_NAME = null;
    private static String REPORTFILE_EXT = null;
    private static String LOGFILE_NAME = null;
    private static String LOGFILE_EXT = null;
    private static String CONFIG_FILE_NAME = null;
    private static String CONFIG_FILE_EXT = null;
    private static String SUMMARY_FILE_NAME = null;
    private static String SUMMARY_FILE_EXT = null;
    private static String DETAIL_FILE_NAME = null;
    private static String FINALIZE_FILE_EXT = null;
    private static String META_QUALITY_FILE_NAME = null;
    private static String META_QUALITY_FILE_EXT = null;
    private static String ANNOT_FILE_NAME = null;
    private static String ANNOT_FILE_EXT = null;
    private static String CONTROL_FILE_NAME = null;
    private static String CONTROL_FILE_EXT = null;
    private static String INTERVAL_FILE_NAME = null;
    private static String INTERVAL_FILE_EXT = null;
    private static String GTF_FILE_NAME = null;
    private static String GTF_FILE_EXT = null;
    // Constants used in server and database setup
    private static String SERVER_NAME = null;
    private static String DATABASE_NAME = null;
    private static String DATABASE_DRIVER = null;
    
    public Constants() {}
    
    // No setters for the setup parameters will be provided.
    // Function setup will load the parameters value from the file passed in uri.
    public static boolean setup(String uri, String root) {
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
            STUDIES_PATH = 
                    File.separator + setup.get("STUDIES_PATH") + File.separator;
            TMP_PATH = File.separator + setup.get("TMP_PATH") + File.separator;
            CBIO_PATH = File.separator + setup.get("CBIO_PATH") + File.separator;
            CBIO_CASE_DIR = setup.get("CBIO_CASE_DIR") + File.separator;
            OUTPUTFILE_NAME = setup.get("OUTPUTFILE_NAME");
            OUTPUTFILE_EXT = setup.get("OUTPUTFILE_EXT");
            ZIPFILE_EXT = setup.get("ZIPFILE_EXT");
            REPORTFILE_NAME = setup.get("REPORTFILE_NAME");
            REPORTFILE_EXT = setup.get("REPORTFILE_EXT");
            LOGFILE_NAME = setup.get("LOGFILE_NAME");
            LOGFILE_EXT = setup.get("LOGFILE_EXT");
            CONFIG_FILE_NAME = setup.get("CONFIG_FILE_NAME");
            CONFIG_FILE_EXT = setup.get("CONFIG_FILE_EXT");
            SUMMARY_FILE_NAME = setup.get("SUMMARY_FILE_NAME");
            SUMMARY_FILE_EXT = setup.get("SUMMARY_FILE_EXT");
            DETAIL_FILE_NAME = setup.get("DETAIL_FILE_NAME");
            FINALIZE_FILE_EXT = setup.get("FINALIZE_FILE_EXT");
            META_QUALITY_FILE_NAME = setup.get("META_QUALITY_FILE_NAME");
            META_QUALITY_FILE_EXT = setup.get("META_QUALITY_FILE_EXT");
            ANNOT_FILE_NAME = setup.get("ANNOT_FILE_NAME");
            ANNOT_FILE_EXT = setup.get("ANNOT_FILE_EXT");
            CONTROL_FILE_NAME = setup.get("CONTROL_FILE_NAME");
            CONTROL_FILE_EXT = setup.get("CONTROL_FILE_EXT");
            INTERVAL_FILE_NAME = setup.get("INTERVAL_FILE_NAME");
            INTERVAL_FILE_EXT = setup.get("INTERVAL_FILE_EXT");
            GTF_FILE_NAME = setup.get("GTF_FILE_NAME");
            GTF_FILE_EXT = setup.get("GTF_FILE_EXT");
            SERVER_NAME = setup.get("SERVER_NAME");
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
    
    // To get the current datetime in different format.
    public static String getStandardDT() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy hh:mmaa");
        return dateFormat.format(new Date());
    }
    public static String getDT_yyyyMMdd_HHmm() {
        DateFormat dt = new SimpleDateFormat("yyyyMMdd_HHmm");
        return dt.format(new Date());
    }
    
    // Display date and time at the views.
    public String getDisplayDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss aa");
        return dateFormat.format(new Date());        
    }
    
    // Return the path where the meta quality report for this study is to be
    // stored.
    public static String getMETA_QUALITY_REPORT_PATH(String study_id) {
        return Constants.getSYSTEM_PATH() + Constants.getSTUDIES_PATH() + 
               study_id + File.separator + Constants.getMETA_QUALITY_FILE_NAME() +
               Constants.getMETA_QUALITY_FILE_EXT();
    }
    
    // Machine generated getters
    public static String getSYSTEM_PATH() { return SYSTEM_PATH; }
    public static String getUSERS_PATH() { return USERS_PATH; }
    public static String getPIC_PATH() { return PIC_PATH; }
    public static String getOUTPUT_PATH() { return OUTPUT_PATH; }
    public static String getINPUT_PATH() { return INPUT_PATH; }
    public static String getCONFIG_PATH() { return CONFIG_PATH; }
    public static String getLOG_PATH() { return LOG_PATH; }
    public static String getSTUDIES_PATH() { return STUDIES_PATH; }
    public static String getTMP_PATH() { return TMP_PATH; }
    public static String getCBIO_PATH() { return CBIO_PATH; }
    public static String getCBIO_CASE_DIR() { return CBIO_CASE_DIR; }
    public static String getOUTPUTFILE_NAME() { return OUTPUTFILE_NAME; }
    public static String getOUTPUTFILE_EXT() { return OUTPUTFILE_EXT; }
    public static String getZIPFILE_EXT() { return ZIPFILE_EXT; }
    public static String getREPORTFILE_NAME() { return REPORTFILE_NAME; }
    public static String getREPORTFILE_EXT() { return REPORTFILE_EXT; }
    public static String getLOGFILE_NAME() { return LOGFILE_NAME; }
    public static String getLOGFILE_EXT() { return LOGFILE_EXT; }
    public static String getCONFIG_FILE_NAME() { return CONFIG_FILE_NAME; }
    public static String getCONFIG_FILE_EXT() { return CONFIG_FILE_EXT; }
    public static String getSUMMARY_FILE_NAME() { return SUMMARY_FILE_NAME; }
    public static String getSUMMARY_FILE_EXT() { return SUMMARY_FILE_EXT; }
    public static String getDETAIL_FILE_NAME() { return DETAIL_FILE_NAME; }
    public static String getFINALIZE_FILE_EXT() { return FINALIZE_FILE_EXT; }
    public static String getMETA_QUALITY_FILE_NAME() { return META_QUALITY_FILE_NAME; }
    public static String getMETA_QUALITY_FILE_EXT() { return META_QUALITY_FILE_EXT; }
    public static String getANNOT_FILE_NAME() { return ANNOT_FILE_NAME; }
    public static String getANNOT_FILE_EXT() { return ANNOT_FILE_EXT; }
    public static String getCONTROL_FILE_NAME() { return CONTROL_FILE_NAME; }
    public static String getCONTROL_FILE_EXT() { return CONTROL_FILE_EXT; }
    public static String getINTERVAL_FILE_NAME() { return INTERVAL_FILE_NAME; }
    public static String getINTERVAL_FILE_EXT() { return INTERVAL_FILE_EXT; }
    public static String getGTF_FILE_NAME() { return GTF_FILE_NAME; }
    public static String getGTF_FILE_EXT() { return GTF_FILE_EXT; }
    public static String getSERVER_NAME() { return SERVER_NAME; }
    public static String getDATABASE_NAME() { return DATABASE_NAME; }
    public static String getDATABASE_DRIVER() { return DATABASE_DRIVER; }
}
