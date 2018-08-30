/*
 * Copyright @2015-2018
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.MetaRecord;
import TIMS.Database.StudyDB;
import TIMS.Database.StudySpecificField;
import TIMS.Database.SubjectRecordDB;
import TIMS.Database.SubjectDB;
import TIMS.Database.SubjectDetail;
import TIMS.General.Constants;
import TIMS.General.ExcelHelper;
import TIMS.General.FileHelper;
import TIMS.General.MetaRecordStatusTracker;
import static TIMS.General.MetaRecordStatusTracker.RecordStatusEnum.INVALID;
import static TIMS.General.MetaRecordStatusTracker.RecordStatusEnum.INVALID_DATE;
import static TIMS.General.MetaRecordStatusTracker.RecordStatusEnum.MISSING_DATA;
import static TIMS.General.MetaRecordStatusTracker.RecordStatusEnum.MISSING_VISIT;
import TIMS.General.MetaRecordTester;
import TIMS.General.MetaRecordTesterThread;
// Libraries for Java
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for primefaces
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.primefaces.context.RequestContext;
// Libraries for Apache POI
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
// Library for stream reader
import com.monitorjbl.xlsx.StreamingReader;
// Library for omniface
import org.omnifaces.cdi.ViewScoped;

/**
 * MetaDataManagementBean is the backing bean for the metadatamanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 14-Dec-2015
 *
 * Revision History
 * 14-Dec-2015 - Created with all the standard getters and setters. Added 3 new
 * methods insertMetaData(), metaDataUpload and onRowEdit.
 * 04-Jan-2016 - Added new method buildSubjectList(). Improved the method 
 * metaDataUpload.
 * 13-Jan-2016 - Removed all the static variables in Clinical Data Management
 * module.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 25-Feb-2016 - Implementation for database 3.0 (Part 2). Bug fix: To check
 * that the strings read in from the meta data uploaded represent valid integer
 * and float values.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 30-Mar-2016 - Changed the class name from ClinicalDataManagementBean to 
 * MetaDataManagementBean. Added the handling for 3 new attributes in subject
 * (i.e. remarks, event and event_date).
 * 31-Mar-2016 - Subject meta data will be split into 2 tables (i.e. Subject 
 * and SubjectRecord), to handle the scenario whereby the subjects are involved
 * in more than one study and their attributes (i.e. height, weight, etc) might
 * have change across those studies. During data upload/entry, perform database
 * insert/update according to whether the subject exist in database or not.
 * 04-Apr-2016 - For new subject record entry, only allow new record insertion
 * i.e. no update allowed.
 * 12-Apr-2016 - To include remarks field during Meta data upload. To refine 
 * the logic for new subject record entry; allow update to subject record BUT 
 * only allow insertion for study_subject record.
 * 14-Apr-2016 - For remarks and event fields, if there are empty, store them
 * as null value.
 * 19-Apr-2016 - Bug Fix: Subject(s) uploaded or created should be pack under
 * the owner of the study.
 * 12-Apr-2017 - The fixed attributes (i.e. gender, race and nationality) can 
 * only be changed through UI manually (not through meta file upload). Log the 
 * user who change the fixed attributes. Subject's meta data will now be own 
 * by study, and the study will be own by group i.e. the direct link between 
 * group and subject's meta data will be break off.
 * 25-Apr-2017 - Added the functionality for user to download the Meta data list
 * for the respective study.
 * 28-Apr-2017 - Renamed method insertMetaData() to insertNewMeasurement().
 * Fixed the bugs found during demo.
 * 19-May-2017 - To display error message if the date format is incorrect
 * during Meta data file upload. During Meta data file upload or insert new
 * measurement, only check for the special character '|' in the event and 
 * remark fields.
 * 29-May-2017 - Changes due to change in Subject table (i.e. age_at_baseline
 * changed to float type.)
 * 06-Apr-2018 - Database version 2.0 changes. Implemented the module for 
 * uploading of meta data through Excel file, and the validation and consistency
 * checks on the uploaded data.
 * 15-May-2018 - Removed the core data from the data column value; the core data
 * will be stored and displayed separately. Add in a check to make sure all the
 * core data columns are available before proceeding to create the meta records.
 * 17-Jul-2018 - Commented out unused code. Implemented the Dashboard module. 
 * Tagging of core data fields to column ID can be done through Excel file;
 * instead of hard coding the column ID.
 * 14-Aug-2018 - Removed unused code. Remove Height, Weight and DateOfBirth data
 * in method removeCoreData().
 * 23-Aug-2018 - To allow update to study specific fields.
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 */

//@ManagedBean (name="MDMgntBean")
@Named("MDMgntBean")
@ViewScoped
public class MetaDataManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MetaDataManagementBean.class.getName());
    // Store the study's subject record that we are managing.
    private List<SubjectDetail> subtDetailList, filteredSubtDetailList;
    private final String userName, study_id;
    // Store the Excel sheet column name into colNameTM to get it sorted.
    private TreeMap<String, String> colNameTM = new TreeMap<>();
    // Keep the unsorted column name list for later processing.
    List<String> unsortedColNameL = new ArrayList<>();
    // Tracker for data quality status during data upload.
    private MetaRecordStatusTracker statsTracker;
    private String missingVisits;
    private ExcelHelper exHelper;
    // Booleans used during records uploading through excel sheet.
    private boolean FIRST_UPLOAD, SKIP_CONSISTENCY_CHECK, quality_report;
    // The set of records that user are uploading through excel sheet.
    private Set<MetaRecord> recordsLHS;
    // Category of the study specific fields.
    private List<String> categoryList;
    // Core data to Excel column ID tagging.
    private HashMap<String, String> core_data_tag;
    private List<List<StudySpecificField>> ssf_lists;
    
    public MetaDataManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        study_id = (String) getFacesContext().getExternalContext().
                getSessionMap().get("study_id");
        statsTracker = null;
        missingVisits = "";
        logger.info(userName + ": access Meta Data Management for study " 
                    + study_id);
    }

    @PostConstruct
    public void init() {
        core_data_tag = StudyDB.getMetaDataTagForStudy(study_id);
        refreshPageVariables();
        refreshStudySpecificFieldLists();
    }

    // Build the study specific field lists.
    private void refreshStudySpecificFieldLists() {
        ssf_lists = new ArrayList<>();
        categoryList = StudyDB.getSpecificFieldCategoryFromStudy(study_id);
        
        for (String category : categoryList) {
            // For each category, retrieve the list of fields that fall under it.
            List<String> field_list = StudyDB.
                    getSpecificFieldListFromStudyCategory(study_id, category);
            List<StudySpecificField> list_of_ssf = new ArrayList<>();
            for (String field : field_list) {
                // For each field, construct a StudySpecificField object and
                // add it to the list of study specific field.
                StudySpecificField tmp = new StudySpecificField(category, field);
                list_of_ssf.add(tmp);
            }
            ssf_lists.add(list_of_ssf);
        }
    }
    
    // Build the subjects detail list.
    private void refreshPageVariables() {
        if (StudyDB.getMetaQualityReportPath(study_id) == null) {
            quality_report = false;
        }
        else {
            quality_report = true;
        }
        
        subtDetailList = SubjectDB.getSubtDetailList(study_id);
    }

    /*
    private int getNoOfRowsInExcel(String xlsxFile) {
        File excel = new File(xlsxFile);
        FileInputStream fis;
        XSSFWorkbook wb;
        XSSFSheet sh;
        
        try {
            fis = new FileInputStream(excel);
            wb = new XSSFWorkbook(fis);
            sh = wb.getSheet("Data");
            logger.info("No of rows in " + xlsxFile + " is " + sh.getLastRowNum() + 1);
            
            return sh.getLastRowNum() + 1;
        } catch (IOException ex) {
            logger.error(ex.getMessage());
            return 0;
        }
    }
    */
    
    // Create the hashmap of meta data records using the values found in the
    // uploaded excel sheet.
    private void createMetaRecordsFromExcel(String xlsxFile) {
        // Load all the patient records into a LinkedHashSet.
        recordsLHS = new LinkedHashSet<>();
        
        try (Workbook wb = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(new File(xlsxFile))) 
        {
            Sheet dataSheet = wb.getSheet("Data");
            int row = 0;
            // Retrieve the column value from the second row of Excel sheet.
            for (Row values : dataSheet) {
                row++;
                // Skip the column name row (i.e. 1st row).
                if (row == 1)
                    continue;
                // Store the record's value into a TreeMap to get it sorted.
                TreeMap<String, String> recordTM = new TreeMap<>();
                List<String> colDataL = ExcelHelper.convertRowToStrList(values);
                // Join the column name and column data into a TreeMap, so
                // that we could extract the data of interests more efficiently.
                Iterator<String> colDataItr = colDataL.iterator();
                for (int i = 0; i < unsortedColNameL.size(); i++) {
                    // Because the excel sheet might have empty value for some
                    // of the columns, hence need to handle them here.
                    if (colDataItr.hasNext()) {
                        recordTM.put(unsortedColNameL.get(i), colDataItr.next());
                    }
                    else {
                        // This column has empty value; need to put in ""
                        recordTM.put(unsortedColNameL.get(i), "");
                    }
                }
                
                // Construct the Meta record for this subject.
                // For now, we will hard-code the mapping for the data of interests.
                MetaRecord record = new MetaRecord(
                        recordTM.get(core_data_tag.get("SubjectID")),
                        recordTM.get(core_data_tag.get("Race")),
                        recordTM.get(core_data_tag.get("CaseControl")),
                        recordTM.get(core_data_tag.get("Height")),
                        recordTM.get(core_data_tag.get("Weight")),
                        recordTM.get(core_data_tag.get("RecordDate")),
                        recordTM.get(core_data_tag.get("DateOfBirth")),
                        recordTM.get(core_data_tag.get("Gender")),
                        recordTM.get(core_data_tag.get("AgeAtBaseline")),
                        // Set the colum data value as null first.
                        null,
                        row);
                // Update the column data after removing the core data.
                record.setDat(new ArrayList<>(removeCoreData(recordTM).values()));
                recordsLHS.add(record);
            }
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
            // Fail to process Excel file, terminate the upload and display
            // error message.
            throw new java.lang.RuntimeException("Fail to create meta records from Excel File!");
        }
    }
    
    // Core data will be stored and display separately; remove them from the 
    // record.
    private TreeMap<String, String> removeCoreData(TreeMap<String, String> rec) {
        rec.remove(core_data_tag.get("SubjectID"));
        rec.remove(core_data_tag.get("Race"));
        rec.remove(core_data_tag.get("CaseControl"));
        rec.remove(core_data_tag.get("Height"));
        rec.remove(core_data_tag.get("Weight"));
        rec.remove(core_data_tag.get("RecordDate"));
        rec.remove(core_data_tag.get("DateOfBirth"));
        rec.remove(core_data_tag.get("Gender"));
        rec.remove(core_data_tag.get("AgeAtBaseline"));

        return rec;
    }
    
    // Upload core data column ID tags using Excel file. Insert core data tag
    // into database.
    public void coreDataTagsUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        core_data_tag = new HashMap<>();
        String localDir = Constants.getSYSTEM_PATH() 
                        + Constants.getTMP_PATH() 
                        + uFile.getFileName();
        
        try {
            // 1. Copy the uploaded Excel file to local directory.
            if (FileHelper.copyUploadedFileToLocalDirectory(uFile, localDir)) {
                logger.info("Excel file copied to local directory: " + localDir);
            }
            else {
                // Fail to copy, terminate the upload and display error message.
                throw new java.lang.RuntimeException("Fail to copy Excel File!");
            }
            
            exHelper = new ExcelHelper(localDir, "Data");
            // Read in the first row of field data [CATEGORY|FIELD]
            List<String> field = exHelper.readNextRow();
            while (field != null) {
                if (field.size() >= 2) {
                    // Store the core data and it's column ID tag.
                    core_data_tag.put(field.get(0), field.get(1));
                }
                field = exHelper.readNextRow();
            }
            
            for (Map.Entry data : core_data_tag.entrySet()) {
                StudyDB.insertMetaDataTag(study_id, (String) data.getKey(), 
                                         (String) data.getValue());
            }
            // Delete the temporary Excel file after use.
            FileHelper.delete(localDir);
            // Record this activity.
            String detail = "Excel File " + uFile.getFileName();
            ActivityLogDB.recordUserActivity(userName, Constants.UPL_CDT, detail);
            // Post the success message to user.
            getFacesContext().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO, "Core data tag uploaded.", ""));
        } catch (RuntimeException rte) {
            logger.error(rte.getMessage());
            // Catch the runtime exception generated locally and display the 
            // respective error message to the user.
            getFacesContext().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_ERROR, rte.getMessage(), ""));
        }
    }
    
    // Upload study specific fields using Excel file. Insert uploaded specific
    // fields into database.
    public void ssFieldsUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        LinkedHashMap<String, List<String>> ssFields_hashmap = new LinkedHashMap<>();
        String localDir = Constants.getSYSTEM_PATH() 
                        + Constants.getTMP_PATH() 
                        + uFile.getFileName();
        
        try {
            // 1. Copy the uploaded Excel file to local directory.
            if (FileHelper.copyUploadedFileToLocalDirectory(uFile, localDir)) {
                logger.info("Excel file copied to local directory: " + localDir);
            }
            else {
                // Fail to copy, terminate the upload and display error message.
                throw new java.lang.RuntimeException("Fail to copy Excel File!");
            }
            
            exHelper = new ExcelHelper(localDir, "Data");
            // Read in the first row of field data [CATEGORY|FIELD]
            List<String> field = exHelper.readNextRow();
            while (field != null) {
                if (field.size() >= 2) {
                    // Group the fields under each category.
                    if (ssFields_hashmap.get(field.get(0)) == null) {
                        // New category.
                        List<String> tmp = new ArrayList<>();
                        tmp.add(field.get(1));
                        ssFields_hashmap.put(field.get(0), tmp);
                    }
                    else {
                    // Existing category, add the field to the existing list of 
                    // string.
                    ssFields_hashmap.get(field.get(0)).add(field.get(1));
                    }
                }
                field = exHelper.readNextRow();
            }
            
            for (Map.Entry data : ssFields_hashmap.entrySet()) {
                StudyDB.updateSSField(study_id, (String) data.getKey(), 
                        FileHelper.convertObjectToByteArray(data.getValue()));
            }
            // Delete the temporary Excel file after use.
            FileHelper.delete(localDir);
            // Update the study specific field datalist.
            refreshStudySpecificFieldLists();
            // Record this activity.
            String detail = "Excel File " + uFile.getFileName();
            ActivityLogDB.recordUserActivity(userName, Constants.UPL_SSF, detail);
            // Post the success message to user.
            getFacesContext().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO, "Study specific fields uploaded.", ""));
        } catch (RuntimeException rte) {
            logger.error(rte.getMessage());
            // Catch the runtime exception generated locally and display the 
            // respective error message to the user.
            getFacesContext().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_ERROR, rte.getMessage(), ""));
        }
    }
    
    // Upload subject meta data using Excel file.
    public void metaDataUpload(FileUploadEvent event) {
        FIRST_UPLOAD = false;
        UploadedFile uFile = event.getFile();
        String localDir = Constants.getSYSTEM_PATH() 
                        + Constants.getTMP_PATH() 
                        + uFile.getFileName();
        
        try {
            // 1. Copy the uploaded Excel file to local directory.
            if (FileHelper.copyUploadedFileToLocalDirectory(uFile, localDir)) {
                logger.info("Excel file copied to local directory: " + localDir);
            }
            else {
                // Fail to copy, terminate the upload and display error message.
                throw new java.lang.RuntimeException("Fail to copy Excel File!");
            }
            
            exHelper = new ExcelHelper(localDir, "Data");
            // 2. Get the column name from the first row of Excel sheet. The 
            // column will be used in the drop-down list for user to map the 
            // data of interests.
            unsortedColNameL = exHelper.readNextRow();
            // Check to make sure all the core data columns are available.
            if (!unsortedColNameL.containsAll(core_data_tag.values())) {
                throw new java.lang.RuntimeException("Missing core data columns!");
            }
            
            // Reset colNameTM before constructing.
            colNameTM.clear();
            // Store the column name as sorted, so that the order of the column
            // will not matter.
            for (String colData : unsortedColNameL) {
                colNameTM.put(colData, colData);
            }
            
            // 3. Create the Meta Records using data from the Excel sheet.
            createMetaRecordsFromExcel(localDir);

            // Remove core data from the column name since they will be stored 
            // and display separately.
            colNameTM = removeCoreData(colNameTM);

            // 4. Compare the column name from current upload with the one
            // stored in database.
            List<String> sortedColNameL = new ArrayList<>(colNameTM.keySet());
            // Retrieve the column name list from database.
            byte[] dat = StudyDB.getColumnNameList(study_id);
            if (dat == null) {
                // Column name list is empty i.e. first upload for this study.
                // Need to update the column name list for this study using 
                // sortedColNameL.
                FIRST_UPLOAD = true;
            }
            else {
                // Store the column name list locally into a list.
                List<String> dbColNameL = FileHelper.convertByteArrayToList(dat);
                // Compare the uploaded column name list with the list from database.
                if (!sortedColNameL.equals(dbColNameL)) {
                    // Column name is different, terminate the data upload and 
                    // display error message.
                    throw new java.lang.RuntimeException
                            ("Column name is different from last upload!");
                }
            }

            // Retrieve from the database, the subject IDs belonging to this study.
            List<String> dbSubtIDsL = SubjectDB.getSubjectIDsList(study_id);
            if (dbSubtIDsL.isEmpty()) {
                // As of now, this study contains zero subject ID.
                FIRST_UPLOAD = true;
            }

            // MetaRecordTester will start to validate the subject record.
            MetaRecordTester tester = new MetaRecordTester(recordsLHS, study_id);

            // 5. Check for missing subject(s) if this is not the first data
            // upload for this study.
            if (!FIRST_UPLOAD) {
                HashMap<String, Boolean> IDsHM = new HashMap<>();
                // Construct IDsHM, such that it contains all the subject IDs 
                // belonging to this study.
                for (String dbSubtID : dbSubtIDsL) {
                    IDsHM.put(dbSubtID, Boolean.FALSE);
                }

                if (tester.checkForMissingSubject(IDsHM)) {
                    logger.info("All the subjects in the last upload have been "
                              + "accounted for.");                    
                }
                else {
                    // There is missing subject(s) in the current upload,
                    // terminate the data upload and display error message.
                    throw new java.lang.RuntimeException
                        ("Mising subject detected in this upload!");
                }
            }
            else {
                // Since this is the first upload for this study, all the valid
                // meta record will be updated to NEW_SUBJECT.
                tester.setAllMetaRecordsStatusToNewSubject();
            }
        
            // 6. Proceed with the data validation check; this check is
            // compulsory for all record (new and old).
            tester.validateMetaRecords();
            // 7. Check for missing visit(s) if this is not the first data upload
            // for this study.
            if (!FIRST_UPLOAD) {
                List<String> SRDList = SubjectRecordDB.getSubjectRecordDateList(study_id);
                LinkedHashMap<String, Boolean> visitsHM = new LinkedHashMap<>();
                // Construct visitsHM, such that it contains all the subject's 
                // visits belonging to this study.
                for (String srd : SRDList) {
                    visitsHM.put(srd, Boolean.FALSE);
                }
                missingVisits = tester.checkForMissingVisit(visitsHM);
                
                if (missingVisits.isEmpty()) {
                    logger.info("All the subject's visits in the last upload "
                              + "have been accounted for.");
                }
                else {
                    // There is missing visit(s) in the current upload.
                }
            }
            // At this stage, the system will have the following informations:
            // 1. Invalid|missing dob and record date
            // 2. Missing subject ID, race or gender
            // 3. Invalid race or gender
            // 4. Missing visits
            generatePreliminaryDataQualityStats(tester.getRecordsSet(), missingVisits);
            // Updated the records set for further processing.
            recordsLHS = tester.getRecordsSet();
            // Display the preliminary overview of data quality to user.
            getRequestContext().execute("PF('dataQDlg').show();");
        } catch (RuntimeException rte) {
            logger.error(rte.getMessage());
            // Catch the runtime exception generated locally and display the 
            // respective error message to the user.
            getFacesContext().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_ERROR, rte.getMessage(), ""));
        }
        // Delete the temporary Excel file after use.
        FileHelper.delete(localDir);
        // Update the subject detail table.
        refreshPageVariables();
        // Record this activity.
        String detail = "Excel File " + uFile.getFileName();
        ActivityLogDB.recordUserActivity(userName, Constants.UPL_MD, detail);
    }

    // After looking at the preliminary review of data quality, the user has 
    // decided to proceed with further check i.e. consistency check, new records
    // insertion into database, etc.
    public String proceedWithFurtherCheck() {
        List<String> sortedColNameL = null;
        
        if (FIRST_UPLOAD) {
            sortedColNameL = new ArrayList<>(colNameTM.keySet());
        }
        
        MetaRecordTesterThread testerThread = new MetaRecordTesterThread
            (study_id, userName, missingVisits, sortedColNameL, recordsLHS, SKIP_CONSISTENCY_CHECK);
        
        // Begin to check for data consistency for the existing and valid records.
        testerThread.start();
        // Record user activity.
        String detail = SKIP_CONSISTENCY_CHECK?
                "Skip Consistency Check":"Proceed with Consistency Check";
        ActivityLogDB.recordUserActivity(userName, Constants.UPD_MD, detail);
        logger.info(userName + ": proceed with further check: " + detail);
        
        return Constants.MAIN_PAGE;
    }
    
    // User decided not to proceed with further check after viewing the 
    // preliminary overview of data quality. Generate the preliminary data
    // quality report.
    public String doNotProceed() {
        String header = 
            "Preliminary overview of the quality of data (Uploaded by " + 
            userName + "@" + Constants.getStandardDT() + ")";
        statsTracker.generateQualityReport
                    (Constants.getMETA_QUALITY_REPORT_PATH(study_id), header);
        StudyDB.updateMetaQualityReport
                    (study_id, Constants.getMETA_QUALITY_REPORT_PATH(study_id));
        refreshPageVariables();
        logger.info(userName + ": did not proceed with data consistency check.");
        
        return Constants.META_DATA_MANAGEMENT;
    }

    // Generate the data quality messages to be display at the Preliminary
    // Overview of Data Quality dialog.
    private void generatePreliminaryDataQualityStats(Set<MetaRecord> recordsSet, 
            String missing_visits) {
        statsTracker = MetaRecordStatusTracker.createPreliminaryTracker
                        (recordsSet.size());

        if (!missingVisits.isEmpty()) {
            // For missing visits, we need to print the missing visits in the
            // sub-header.
            statsTracker.concatSubHeaderForStatus(MISSING_VISIT, missing_visits);
            statsTracker.concatSubHeaderForStatus(MISSING_VISIT, "\nAffected records: ");
        }
        
        for (MetaRecord rec : recordsSet) {
            statsTracker.incCountForStatus(rec.getRecord_status_enum());
            String msg = rec.getIndex() + ", ";
            statsTracker.concatMessageForStatus(rec.getRecord_status_enum(), msg);
        }
    }
    
    // Build the meta data list for the study; for user to download.
    public void downloadMetaDataList() {
        String meta_file = Constants.getSYSTEM_PATH() + 
                           Constants.getTMP_PATH() + 
                           study_id + "_meta_" + 
                           Constants.getDT_yyyyMMdd_HHmm() + 
                           Constants.getOUTPUTFILE_EXT();
        
        if (FileHelper.generateMetaDataList(study_id, meta_file)) {
            ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, 
                                    "Meta Data of " + study_id);
            FileHelper.download(meta_file);
            // Delete the Meta Data List after download.
            FileHelper.delete(meta_file);
        }
    }
    
    // Allow user to download the data quality report for the last upload.
    public void downloadDataQualityReport() {
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, 
                                    "Data Quality Report of " + study_id);
        FileHelper.download(StudyDB.getMetaQualityReportPath(study_id));
    }
    
    // Used by admin to delete all the subjects and subject records belonging
    // to the study, and to update the column data name to null.
    public void deleteAllSubjectMetaData() {
        StudyDB.updateStudyColumnNameList(study_id, null);
        StudyDB.nullMetaQualityReport(study_id);
        SubjectRecordDB.deleteAllSubjectRecordsFromStudy(study_id);
        SubjectDB.deleteAllSubjectsFromStudy(study_id);
        String detail = "All the subjects in study " + study_id;
        // Record user activity.
        ActivityLogDB.recordUserActivity(userName, Constants.DEL_MD, detail);
        logger.info(userName + " deleted all the subject Meta data in " + study_id);
        // Update the subject list.
        refreshPageVariables();
    }

    // Used by admin to delete all the study specific fields.
    public void deleteStudySpecificFields() {
        StudyDB.deleteStudySpecificFields(study_id);
        // Record user activity.
        ActivityLogDB.recordUserActivity(userName, Constants.DEL_SSF, study_id);
        // Update the study specific field datalist.
        refreshStudySpecificFieldLists();
    }
    
    // Used by admin to delete all the study meta data tags. After this, no meta
    // data upload is allowed.
    public void deleteStudyMetaDataTag() {
        StudyDB.deleteMetaDataTagForStudy(study_id);
        // Record user activity.
        ActivityLogDB.recordUserActivity(userName, Constants.DEL_CDT, study_id);
        // Reset core data tag.
        core_data_tag = new HashMap<>();        
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    // Retrieve the request context
    private RequestContext getRequestContext() {
        return RequestContext.getCurrentInstance();
    }
    
    // Return true if the string represent a number, else return false.
    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        }
        catch (NumberFormatException nfe) {
            return false;
        }
    }
    
    // Return true if the string represent a float, else return false.
    private boolean isFloat(String str) {
        try {
            Float.parseFloat(str);
            return true;
        }
        catch (NumberFormatException nfe) {
            return false;
        }        
    }
    
    // Return the wording to be display at the link under the BreadCrumb in the
    // Clinical Data Management page.
    public String getBreadCrumbLink() {
        return "Meta Data Management for " + study_id;
    }
    
    // Check whether the subject detail list is empty; use to control the 
    // rendering of the Generate Meta Data List button.
    public boolean isMetaListEmpty() {
        return subtDetailList.isEmpty();
    }
    
    // Check whether the study specific category list is empty; use to control
    // the rendering of the Delete All Study Specific Fields button.
    public boolean isCategoryListEmpty() {
        return categoryList.isEmpty();
    }
    
    // Check whether the core data column ID tag has been setup; use to control
    // the rendering of the Upload Meta Data button.
    public boolean isCoreDataTagEmpty() {
        return core_data_tag.isEmpty();
    }
    
    // Return the list of study specific fields under each category; will only
    // allow 3 categories for now.
    public List<StudySpecificField> getSsfList1() {
        return (ssf_lists.size() >= 1)?ssf_lists.get(0):null;
    }
    public List<StudySpecificField> getSsfList2() {
        return (ssf_lists.size() >= 2)?ssf_lists.get(1):null;
    }
    public List<StudySpecificField> getSsfList3() {
        return (ssf_lists.size() >= 3)?ssf_lists.get(2):null;
    }
    // Return the name of category; will only allow 3 categories for now.
    public String getSsc1() {
        return (categoryList.size() >= 1)?categoryList.get(0):"";
    }
    public String getSsc2() {
        return (categoryList.size() >= 2)?categoryList.get(1):"";
    }
    public String getSsc3() {
        return (categoryList.size() >= 3)?categoryList.get(2):"";
    }
    
    // Return the list of meta data belonging to the same department ID as
    // the user.
    public List<SubjectDetail> getSubtDetailList() {
        return subtDetailList;
    }

    // Return the messages for the preliminary overview of data quality.
    public String getInvalidCoreMsg() {
        return (statsTracker == null)?
                "":statsTracker.getStatsForStatus(INVALID);
    }
    public String getMissingCoreMsg() {
        return (statsTracker == null)?
                "":statsTracker.getStatsForStatus(MISSING_DATA);
    }
    public String getMissingVisitMsg() {
        return (statsTracker == null)?
                "":statsTracker.getStatsForStatus(MISSING_VISIT);
    }
    public String getInvalidDateMsg() {
        return (statsTracker == null)?
                "":statsTracker.getStatsForStatus(INVALID_DATE);
    }
    public String getReadyMsg() {
        return (statsTracker == null)?
                "":statsTracker.getStatsForPassedRecords();
    }
    
    public List<SubjectDetail> getFilteredSubtDetailList() {
        return filteredSubtDetailList;
    }
    public void setFilteredSubtDetailList(List<SubjectDetail> filteredSubtDetailList) {
        this.filteredSubtDetailList = filteredSubtDetailList;
    }
    public boolean isSKIP_CONSISTENCY_CHECK() {
        return SKIP_CONSISTENCY_CHECK;
    }
    public void setSKIP_CONSISTENCY_CHECK(boolean SKIP_CONSISTENCY_CHECK) {
        this.SKIP_CONSISTENCY_CHECK = SKIP_CONSISTENCY_CHECK;
    }
    public boolean isQuality_report() {
        return quality_report;
    }
}
