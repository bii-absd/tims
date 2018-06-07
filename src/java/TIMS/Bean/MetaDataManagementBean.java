/*
 * Copyright @2015-2018
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.MetaRecord;
import TIMS.Database.StudyDB;
import TIMS.Database.SubjectRecord;
import TIMS.Database.SubjectRecordDB;
import TIMS.Database.Subject;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for primefaces
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.UploadedFile;
import org.primefaces.context.RequestContext;
// Libraries for Apache POI
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
// Library for stream reader
import com.monitorjbl.xlsx.StreamingReader;
import java.util.Arrays;

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
 */

@ManagedBean (name="MDMgntBean")
@ViewScoped
public class MetaDataManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MetaDataManagementBean.class.getName());
    private String subject_id, country_code, race, remarks, event;
    private char gender;
    private float height, weight, age_at_baseline;
    private java.util.Date util_event_date, util_record_date;
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
        refreshPageVariables();
    }

    // Build the subjects detail list.
    private void refreshPageVariables() {
        if (StudyDB.getMetaQualityReportPath(study_id) == null) {
            quality_report = false;
        }
        else {
            quality_report = true;
        }
        
        try {
            subtDetailList = SubjectDB.getSubtDetailList(study_id);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to retrieve subject records for study: " + study_id);
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "System failed to retrieve subject records from database!", ""));
        }
    }

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
                        recordTM.get("PID"),
                        recordTM.get("Race"),
                        recordTM.get("casecontrol"),
                        recordTM.get("Visit__exam_Height_in_metres"),
                        recordTM.get("Visit__exam_Weight"),
                        recordTM.get("Date"),
                        recordTM.get("DateOfBirth"),
                        recordTM.get("Gender"),
                        // Set the colum data value as null first.
                        null,
                        row);
                // Update the column data after removing the core data.
                record.setDat(removeCoreData(recordTM));
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
    private List<String> removeCoreData(TreeMap<String, String> rec) {
        rec.remove("PID");
        rec.remove("Race");
        rec.remove("casecontrol");
        rec.remove("Visit__exam_Height_in_metres");
        rec.remove("Visit__exam_Weight");
        rec.remove("Date");
        rec.remove("DateOfBirth");
        rec.remove("Gender");
        
        return new ArrayList<>(rec.values());
    }
    private void removeCoreDataColumn() {
        colNameTM.remove("PID");
        colNameTM.remove("Race");
        colNameTM.remove("casecontrol");
        colNameTM.remove("Visit__exam_Height_in_metres");
        colNameTM.remove("Visit__exam_Weight");
        colNameTM.remove("Date");
        colNameTM.remove("DateOfBirth");
        colNameTM.remove("Gender");
    }
    
    // Upload subject meta data using Excel file.
    public void xlsxDataUpload(FileUploadEvent event) {
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
            // This step can be REMOVED once the feature for user to map the
            // data of interests is available.
            List<String> tmp = Arrays.asList("PID","Race","casecontrol",
                    "Visit__exam_Height_in_metres","Visit__exam_Weight",
                    "Date","DateOfBirth","Gender");
            if (!unsortedColNameL.containsAll(tmp)) {
                throw new java.lang.RuntimeException("Missing core data columns!");
            }
            
            // Reset colNameTM before constructing.
            colNameTM.clear();
            for (String colData : unsortedColNameL) {
                colNameTM.put(colData, colData);
            }
            
            // 3. Create the Meta Records using data from the Excel sheet.
            createMetaRecordsFromExcel(localDir);

            // Remove the Subject ID column from the record since it will be
            // stored and display separately.
            // FOR NOW HARD CODE THE SUBJECT ID COLUMN NAME i.e PID.
            removeCoreDataColumn();

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
                    // TODO: Generate quality report!
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
        String detail = "Using Excel File " + uFile.getFileName();
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
        
    // Update the subject meta data in database
    public void onRowEdit(RowEditEvent event) {
        LocalDate new_record_date = null;
        FacesContext fc = getFacesContext();
        SubjectDetail subtDetail = (SubjectDetail) event.getObject();
        // Because the system is receiving the date as java.util.Date hence
        // we need to perform a conversion here before storing it into database.
        if (util_event_date != null) {
            subtDetail.setEvent_date(util_event_date.toInstant().
                    atZone(ZoneId.systemDefault()).toLocalDate());
        }
        if (util_record_date != null) {
            new_record_date = util_record_date.toInstant().
                    atZone(ZoneId.systemDefault()).toLocalDate();
        }
        else {
            // No change in the record date.
            new_record_date = subtDetail.getRecord_date();
        }
        // Build the Subject and SubjectRecord objects using the selected 
        // SubjectDetail object.
        Subject subt = new Subject(subtDetail.getSubject_id(),
                                   subtDetail.getStudy_id(),
                                   subtDetail.getRace(),
                                   subtDetail.getGender(),
                                   subtDetail.getDob(),
                                   subtDetail.getCasecontrol());
        SubjectRecord sr = new SubjectRecord(subtDetail.getSubject_id(),
                                           subtDetail.getStudy_id(),
                                           subtDetail.getRecord_date(),
                                           subtDetail.getRemarks(),
                                           subtDetail.getEvent(),
                                           subtDetail.getHeight(),
                                           subtDetail.getWeight(),
                                           subtDetail.getEvent_date(),
                                           // For now, initialise as null.
                                           null
                                           );
        
        if (SubjectDB.updateSubt(subt) && SubjectRecordDB.updateSR(sr, new_record_date)) {
            // Record this subject meta data update into database.
            String detail = "Subject " + subt.getSubject_id() 
                          + " in Study " + study_id 
                          + ". Gender: " + subt.getGender() 
                          + " Race: " + subt.getRace()
                          + " DOB: " + subt.getDob()
                          + " Case or Control: " + subt.getCasecontrol();
            ActivityLogDB.recordUserActivity(userName, Constants.CHG_ID, detail);
            logger.info(userName + ": updated " + detail);
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO, "Meta data updated.", ""));
            // Update the subject list.
            refreshPageVariables();
        }
        else {
            logger.error("FAIL to update meta data!");
            fc.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR, "Failed to update meta data!", ""));
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
    
    // Machine generated getters and setters.
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }
    public float getAge_at_baseline() {
        return age_at_baseline;
    }
    public void setAge_at_baseline(float age_at_baseline) {
        this.age_at_baseline = age_at_baseline;
    }
    public char getGender() {
        return gender;
    }
    public void setGender(char gender) {
        this.gender = gender;
    }
    public String getCountry_code() {
        return country_code;
    }
    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }
    public String getRace() {
        return race;
    }
    public void setRace(String race) {
        this.race = race;
    }
    public float getHeight() {
        return height;
    }
    public void setHeight(float height) {
        this.height = height;
    }
    public float getWeight() {
        return weight;
    }
    public void setWeight(float weight) {
        this.weight = weight;
    }
    public String getRemarks() {
        return remarks;
    }
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    public String getEvent() {
        return event;
    }
    public void setEvent(String event) {
        this.event = event;
    }
    public java.util.Date getUtil_event_date() {
        return util_event_date;
    }
    public void setUtil_event_date(java.util.Date util_event_date) {
        this.util_event_date = util_event_date;
    }
    public java.util.Date getUtil_record_date() {
        return util_record_date;
    }
    public void setUtil_record_date(java.util.Date util_record_date) {
        this.util_record_date = util_record_date;
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
