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

import TIMS.Database.ActivityLogDB;
import TIMS.Database.MetaDataTagDB;
import TIMS.Database.MetaRecord;
import TIMS.Database.StudyDB;
import TIMS.Database.StudySpecificField;
import TIMS.Database.StudySpecificFieldDB;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;
import java.util.Collections;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
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
// Library for Trove
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TLinkedHashSet;
import gnu.trove.map.hash.TObjectIntHashMap;

@Named("MDMgntBean")
@ViewScoped
public class MetaDataManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MetaDataManagementBean.class.getName());
    // Store the study's subject record that we are managing.
    private List<SubjectDetail> subtDetailList, filteredSubtDetailList;
    private final String userName, study_id;
    // Keep the unsorted column name list for later processing.
    private List<String> unsortedColNameL = new ArrayList<>();
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
    private final SubjectDB subjects;
    private final StudySpecificFieldDB ss_fields;
    private final MetaDataTagDB md_tag;
    
    public MetaDataManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        study_id = (String) getFacesContext().getExternalContext().
                getSessionMap().get("study_id");
        subjects = new SubjectDB(study_id);
        ss_fields = new StudySpecificFieldDB(study_id);
        md_tag = new MetaDataTagDB(study_id);
        statsTracker = null;
        missingVisits = "";
        StringBuilder oper = new StringBuilder(userName).
                append(": access Meta Data Management for study ").
                append(study_id);
        logger.info(oper);
    }

    @PostConstruct
    public void init() {
        core_data_tag = md_tag.getMetaDataTag();
        refreshPageVariables();
        refreshStudySpecificFieldLists();
    }

    // Build the study specific field lists.
    private void refreshStudySpecificFieldLists() {
        ssf_lists = new ArrayList<>();
        categoryList = ss_fields.getSpecificFieldCategory();
        
        for (String category : categoryList) {
            // For each category, retrieve the list of fields that fall under it.
            List<String> field_list = ss_fields.
                    getSpecificFieldListFromCategory(category);
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
        
        subtDetailList = subjects.getSubtDetailList();
    }

    // Create the hashmap of meta data records using the values found in the
    // uploaded excel sheet.
    private void createMetaRecordsFromExcel(String xlsxFile) {
        // Load all the patient records into a LinkedHashSet.
        recordsLHS = new TLinkedHashSet<>();
        // Store the record's value into a TreeMap to get it sorted.
        TreeMap<String, String> recordTM = new TreeMap<>();
        
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
                // Clear the current record value before reading in the next record.
                recordTM.clear();
            }
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
            // Fail to process Excel file, terminate the upload and display
            // error message.
            throw new java.lang.RuntimeException("Fail to create meta records from Excel File!");
        }
        logger.debug("Total records uploaded: " + recordsLHS.size());
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
    // Clean up the column name list (i.e. removing the core data from the list.)
    private void cleanupColNameL() {
        unsortedColNameL.remove(core_data_tag.get("SubjectID"));
        unsortedColNameL.remove(core_data_tag.get("Race"));
        unsortedColNameL.remove(core_data_tag.get("CaseControl"));
        unsortedColNameL.remove(core_data_tag.get("Height"));
        unsortedColNameL.remove(core_data_tag.get("Weight"));
        unsortedColNameL.remove(core_data_tag.get("RecordDate"));
        unsortedColNameL.remove(core_data_tag.get("DateOfBirth"));
        unsortedColNameL.remove(core_data_tag.get("Gender"));
        unsortedColNameL.remove(core_data_tag.get("AgeAtBaseline"));
    }
    
    // Upload core data column ID tags using Excel file. Insert core data tag
    // into database.
    public void coreDataTagsUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        core_data_tag = new HashMap<>();
        StringBuilder localDir = new StringBuilder(Constants.getSYSTEM_PATH()).
                append(Constants.getTMP_PATH()).append(uFile.getFileName());
        
        try {
            // 1. Copy the uploaded Excel file to local directory.
            if (FileHelper.copyUploadedFileToLocalDirectory(uFile, localDir.toString())) {
                logger.info("Excel file copied to local directory.");
            }
            else {
                // Fail to copy, terminate the upload and display error message.
                throw new java.lang.RuntimeException("Fail to copy Excel File!");
            }
            
            exHelper = new ExcelHelper(localDir.toString(), "Data");
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
                md_tag.insertMetaDataTag((String) data.getKey(), 
                                         (String) data.getValue());
            }
            // Delete the temporary Excel file after use.
            FileHelper.delete(localDir.toString());
            // Record this activity.
            StringBuilder detail = new StringBuilder("Excel File ").
                    append(uFile.getFileName());
            ActivityLogDB.recordUserActivity(userName, Constants.UPL_CDT, detail.toString());
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
        StringBuilder localDir = new StringBuilder(Constants.getSYSTEM_PATH()).
                append(Constants.getTMP_PATH()).append(uFile.getFileName());

        try {
            // 1. Copy the uploaded Excel file to local directory.
            if (FileHelper.copyUploadedFileToLocalDirectory(uFile, localDir.toString())) {
                logger.info("Excel file copied to local directory.");
            }
            else {
                // Fail to copy, terminate the upload and display error message.
                throw new java.lang.RuntimeException("Fail to copy Excel File!");
            }
            
            exHelper = new ExcelHelper(localDir.toString(), "Data");
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
                ss_fields.updateSSField((String) data.getKey(), 
                        FileHelper.convertObjectToByteArray(data.getValue()));
            }
            // Delete the temporary Excel file after use.
            FileHelper.delete(localDir.toString());
            // Update the study specific field datalist.
            refreshStudySpecificFieldLists();
            // Record this activity.
            StringBuilder detail = new StringBuilder("Excel File ").
                                    append(uFile.getFileName());
            ActivityLogDB.recordUserActivity(userName, Constants.UPL_SSF, detail.toString());
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
        StringBuilder localDir = new StringBuilder(Constants.getSYSTEM_PATH()).
                append(Constants.getTMP_PATH()).append(uFile.getFileName());
        
        try {
            // 1. Copy the uploaded Excel file to local directory.
            if (FileHelper.copyUploadedFileToLocalDirectory(uFile, localDir.toString())) {
                logger.info("Excel file copied to local directory.");
            }
            else {
                // Fail to copy, terminate the upload and display error message.
                throw new java.lang.RuntimeException("Fail to copy Excel File!");
            }
            
            exHelper = new ExcelHelper(localDir.toString(), "Data");
            // 2. Get the column name from the first row of Excel sheet. The 
            // column will be used in the drop-down list for user to map the 
            // data of interests.
            unsortedColNameL = exHelper.readNextRow();
            // Check to make sure all the core data columns are available.
            if (!unsortedColNameL.containsAll(core_data_tag.values())) {
                throw new java.lang.RuntimeException("Missing core data columns!");
            }
            
            // 3. Create the Meta Records using data from the Excel sheet.
            createMetaRecordsFromExcel(localDir.toString());
            // Remove core data column ID from the list.
            cleanupColNameL();
            // AFTER HERE, UNSORTEDCOLNAMEL IS NO LONGER IN USE!

            // 4. Compare the column name from current upload with the one
            // stored in database.
            List<String> sortedColNameL = new ArrayList<>(unsortedColNameL);
            Collections.sort(sortedColNameL);
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
            List<String> dbSubtIDsL = subjects.getSubjectIDsList();
            if (dbSubtIDsL.isEmpty()) {
                // As of now, this study contains zero subject ID.
                FIRST_UPLOAD = true;
            }

            // MetaRecordTester will start to validate the subject record.
            MetaRecordTester tester = new MetaRecordTester(recordsLHS, study_id);

            // 5. Check for missing subject(s) if this is not the first data
            // upload for this study.
            if (!FIRST_UPLOAD) {
                TObjectIntHashMap<String> IDsHM = 
                        new TObjectIntHashMap<>(dbSubtIDsL.size());
                // Construct IDsHM, such that it contains all the subject IDs 
                // belonging to this study.
                for (String dbSubtID : dbSubtIDsL) {
                    IDsHM.put(dbSubtID, 0);
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
                THashSet<String> visitsHS = new THashSet<>(SRDList.size());
                // Construct visitsHM, such that it contains all the subject's 
                // visits belonging to this study.
                for (String srd : SRDList) {
                    visitsHS.add(srd);
                }
                missingVisits = tester.checkForMissingVisit(visitsHS);
                
                if (missingVisits.isEmpty()) {
                    logger.info("All the subject's visits in the last upload "
                              + "have been accounted for.");
                }
                else {
                    // There is missing visit(s) in the current upload.
                    logger.info("Missing visit(s) detected!");
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
        FileHelper.delete(localDir.toString());
        // Update the subject detail table.
        refreshPageVariables();
        // Record this activity.
        StringBuilder detail = new StringBuilder("Excel File ").
                append(uFile.getFileName());
        ActivityLogDB.recordUserActivity(userName, Constants.UPL_MD, detail.toString());
    }

    // After looking at the preliminary review of data quality, the user has 
    // decided to proceed with further check i.e. consistency check, new records
    // insertion into database, etc.
    public String proceedWithFurtherCheck() {
        List<String> sortedColNameL = null;
        
        if (FIRST_UPLOAD) {
            sortedColNameL = new ArrayList<>(unsortedColNameL);
            Collections.sort(sortedColNameL);
        }
        
        MetaRecordTesterThread testerThread = new MetaRecordTesterThread
            (study_id, userName, missingVisits, sortedColNameL, recordsLHS, SKIP_CONSISTENCY_CHECK);
        
        // Begin to check for data consistency for the existing and valid records.
        testerThread.start();
        // Record user activity.
        String detail = SKIP_CONSISTENCY_CHECK?
                "Skip Consistency Check":"Proceed with Consistency Check";
        ActivityLogDB.recordUserActivity(userName, Constants.UPD_MD, detail);
        StringBuilder oper = new StringBuilder(userName).
                append(": proceed with further check: ").append(detail);
        logger.info(oper);
        
        return Constants.MAIN_PAGE;
    }
    
    // User decided not to proceed with further check after viewing the 
    // preliminary overview of data quality. Generate the preliminary data
    // quality report.
    public String doNotProceed() {
        StringBuilder header = 
                new StringBuilder("Preliminary overview of the quality of data (Uploaded by ").
                        append(userName).append("@").
                        append(Constants.getStandardDT()).append(")");
        statsTracker.generateQualityReport
                    (Constants.getMETA_QUALITY_REPORT_PATH(study_id), header.toString());
        StudyDB.updateMetaQualityReport
                    (study_id, Constants.getMETA_QUALITY_REPORT_PATH(study_id));
        refreshPageVariables();
        logger.info(userName + ": did not proceed with data consistency check.");
        // Free up memory while waiting for the next upload.
        unsortedColNameL.clear();
        recordsLHS.clear();
        
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
//            String msg = rec.getIndex() + ", ";
            StringBuilder msg = new StringBuilder(String.valueOf(rec.getIndex())).append(", ");
            statsTracker.concatMessageForStatus(rec.getRecord_status_enum(), msg.toString());
        }
    }
    
    // Build the meta data list for the study; for user to download.
    public void downloadMetaDataList() {
        StringBuilder meta_file = new StringBuilder(Constants.getSYSTEM_PATH()).
                append(Constants.getTMP_PATH()).append(study_id).
                append("_meta_").append(Constants.getDT_yyyyMMdd_HHmm()).
                append(Constants.getOUTPUTFILE_EXT());
        if (FileHelper.generateMetaDataList(study_id, meta_file.toString())) {
            ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, 
                                    "Meta Data of " + study_id);
            FileHelper.download(meta_file.toString());
            // Delete the Meta Data List after download.
            FileHelper.delete(meta_file.toString());
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
        subjects.deleteAllSubjectsFromStudy();
        StringBuilder detail = new StringBuilder("All the subjects in study ").
                                    append(study_id);
        // Record user activity.
        ActivityLogDB.recordUserActivity(userName, Constants.DEL_MD, detail.toString());
        StringBuilder oper = new StringBuilder(userName).
                append(" deleted all the subject Meta data in ").
                append(study_id);
        logger.info(oper);
        // Update the subject list.
        refreshPageVariables();
    }

    // Used by admin to delete all the study specific fields.
    public void deleteStudySpecificFields() {
        ss_fields.deleteSpecificFields();
        // Record user activity.
        ActivityLogDB.recordUserActivity(userName, Constants.DEL_SSF, study_id);
        // Update the study specific field datalist.
        refreshStudySpecificFieldLists();
    }
    
    // Used by admin to delete all the study meta data tags. After this, no meta
    // data upload is allowed.
    public void deleteStudyMetaDataTag() {
        md_tag.deleteMetaDataTag();
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
