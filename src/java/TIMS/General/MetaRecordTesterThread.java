/*
 * Copyright @2018
 */
package TIMS.General;

import TIMS.Database.DBHelper;
import TIMS.Database.MetaRecord;
import TIMS.Database.StudyDB;
import TIMS.Database.Subject;
import TIMS.Database.SubjectDB;
import TIMS.Database.SubjectRecord;
import TIMS.Database.SubjectRecordDB;
import static TIMS.General.MetaRecordStatusTracker.RecordStatusEnum.MISSING_VISIT;
// Libraries for Java
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * MetaRecordTesterThread check for consistency of the upload meta records with 
 * the records in the database. It also insert the new records (if any) into the 
 * database and generate the data quality report for the current upload. 
 * 
 * Author: Tay Wei Hong
 * Date: 16-Apr-2018
 * 
 * Revision History
 * 16-Apr-2018 - Created with the following methods: skipDataConsistencyCheck,
 * dataConsistencyCheck, updateRelevantMetaRecords and 
 * generateFinalDataQualityStats.
 */

public class MetaRecordTesterThread extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MetaRecordTesterThread.class.getName());
    private Connection conn = null;
    private final String study_id, user_name, missing_visits;
    private final List<String> sortedColNameL;
    private final Set<MetaRecord> recordsSet;
    private final boolean skipConsistencyCheck;

    public MetaRecordTesterThread(String study_id, String user_name, 
            String missing_visits, List<String> sortedColNameL, 
            Set<MetaRecord> recordsSet, boolean skipConsistencyCheck) {
        this.study_id = study_id;
        this.user_name = user_name;
        this.missing_visits = missing_visits;
        this.sortedColNameL = sortedColNameL;
        this.recordsSet = recordsSet;
        this.skipConsistencyCheck = skipConsistencyCheck;
    }

    @Override
    public void run() {
        // Check for data consistency for the existing and valid records.
        if (skipConsistencyCheck) {
            skipDataConsistencyCheck();
        }
        else {
            dataConsistencyCheck();
        }
        
        // Start to update the records in the database.
        try {
            if (sortedColNameL != null) {
                // Update the column name list for this study.
                StudyDB.updateStudyColumnNameList(study_id,
                        FileHelper.convertObjectToByteArray(sortedColNameL));
                logger.info("Column name list updated for study " + study_id);
            }
            // The following updates need to be group as a transaction.
            conn = DBHelper.getDSConn();
            conn.setAutoCommit(false);
            // Update the rows in subject and subject_record tables in database.
            updateRelevantMetaRecords(conn);
            // Only commit the transaction if all the update|insertion passed.
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException|NamingException e) {
            logger.error("FAIL to update meta records in database!");
            logger.error(e.getMessage());
        }
        finally {
            DBHelper.closeDSConn(conn);
        }
        Postman.sendMetaDataUploadStatusEmail(study_id, user_name, Constants.OK);
        generateFinalDataQualityStats();
    }
    
    // Skip data consistency check; mark all valid and new visit records as
    // data_consistent or new_visit_completed.
    private void skipDataConsistencyCheck() {
        for (MetaRecord rec : recordsSet) {
            if (rec.isValid() || rec.isNewVisit()) {
                rec.setRecordsStatusAsDataConsistent();
            }
        }
    }
    
    // Only records marked as VALID and NEW_VISIT will be checked for data 
    // consistency.
    // For NEW_VISIT record, only the dob, case_control, gender and race will be 
    // checked.
    // For VALID record, the dob, case_control, gender and race + height, 
    // weight and the full column data will be check against the column data 
    // currently in the database.
    private void dataConsistencyCheck() {
        for (MetaRecord rec : recordsSet) {
            if (rec.isValid() || rec.isNewVisit()) {
                // Compare the dob, case_control, gender and race with the 
                // values from database subject.
                Subject subjt = SubjectDB.getSubject
                                (study_id, rec.getSubject_id());
                if (subjt.getGender().equals(rec.getGender()) && 
                    subjt.getRace().equals(rec.getRace()) &&
                    subjt.getDob().equals(rec.getDob()) &&
                    subjt.getCasecontrol().equals(rec.getCasecontrol())) {
                    if (rec.isValid()) {
                        // This is an existing record; need to check that the 
                        // height, weight and uploaded data is consistent with 
                        // the data in the database.
                        SubjectRecord sr = SubjectRecordDB.getSubjectRecord
                            (study_id, rec.getSubject_id(), rec.getRecord_date());
                        List<String> dbColData = sr.getDataValueList();
                        if (dbColData.equals(rec.getDat()) && 
                            sr.getHeight().equals(rec.getHeight()) &&
                            sr.getWeight().equals(rec.getWeight())) {
                            rec.setRecordsStatusAsDataConsistent();
                        }
                        else {
                            rec.setRecordsStatusAsDataInconsistent();
                        }
                    }
                    else {
                        // For new visit, no further check is needed.
                        rec.setRecordsStatusAsDataConsistent();
                    }
                }
                else {
                    rec.setRecordsStatusAsDataInconsistent();
                }
            }
        }
    }
    
    // Update all the relevant meta records into database.
    // For consistency check:
    // NEW_SUBJECT_COMPLETED, insert new row into subject and subject_record
    // tables.
    // NEW_VISIT_COMPLETED, insert new row into subject_record table only.
    // For SKIP consistency check:
    // DATA_CONSISTENT: update existing row in subject and subject_record tables.
    private void updateRelevantMetaRecords(Connection conn) throws SQLException {
        // Track the number of unique new subject IDs that are being inserted
        // into subject table.
        List<String> newSubjectsL = new ArrayList<>();
        // Track the number of existing subject IDs that have been updated.
        List<String> exSubjectsL = new ArrayList<>();
        
        for (MetaRecord rec : recordsSet) {
            switch (rec.getRecord_status_enum()) {
                case NEW_SUBJECT_COMPLETED:
                    // Only insert new row into subject table if it has not 
                    // been inserted before.
                    if (!newSubjectsL.contains(rec.getSubject_id())) {
                        Subject newSubjt = new Subject(rec.getSubject_id(), study_id, 
                                        rec.getRace(), rec.getGender(), 
                                        rec.getDob(), rec.getCasecontrol());
                        SubjectDB.insertSubject(newSubjt, conn);
                        // Keep track of the subject IDs inserted.
                        newSubjectsL.add(rec.getSubject_id());
                    }
                    // Flow through to insert subject record.
                case NEW_VISIT_COMPLETED:
                    // Insert new row into subject_record.
                    SubjectRecord newSubjtRec = new SubjectRecord
                        (study_id, rec.getSubject_id(), rec.getRecord_date(), 
                         rec.getHeight(), rec.getWeight(), 
                         FileHelper.convertObjectToByteArray(rec.getDat()));
                    if (!SubjectRecordDB.insertSR(newSubjtRec, conn)) {
                        // Failed to insert record into database; mark this 
                        // record as error (most likely a duplicated record.)
                        rec.setRecordStatusAsNewVisitError();
                    }
                    break;
                case DATA_CONSISTENT:
                    if (skipConsistencyCheck) {
                        // When the user decided to skip consistency check, it
                        // means they wish to update the records in the database
                        // with the value uploaded through Excel.
                        if (!exSubjectsL.contains(rec.getSubject_id())) {
                            Subject exSubjt = new Subject(rec.getSubject_id(), 
                                              study_id, rec.getRace(), 
                                              rec.getGender(), rec.getDob(), 
                                              rec.getCasecontrol());
                            SubjectDB.updateSubt(exSubjt, conn);
                            // Keep track of the subject IDs updated.
                            exSubjectsL.add(rec.getSubject_id());
                        }
                        // Update the subject_record.
                        SubjectRecord exSubjtRec = new SubjectRecord
                            (study_id, rec.getSubject_id(), rec.getRecord_date(), 
                            rec.getHeight(), rec.getWeight(), 
                            FileHelper.convertObjectToByteArray(rec.getDat()));
                        SubjectRecordDB.updatePartialSubjectRecord(exSubjtRec, conn);
                    }
                    break;
                // Do nothing for the other record status.
            }
        }
    }
    
    // Generate the final statistics for the quality of data uploaded.
    private void generateFinalDataQualityStats() {
        String header = "Overview of the quality of data (Uploaded by " + user_name
                      + "@" + Constants.getStandardDT() + ")";
        MetaRecordStatusTracker finalTracker = 
                MetaRecordStatusTracker.createFinalTracker(recordsSet.size());
        // For MISSING_VISIT, we need to include the missing visits in the
        // message.
        if (!missing_visits.isEmpty()) {
            finalTracker.concatSubHeaderForStatus(MISSING_VISIT, missing_visits);
            finalTracker.concatSubHeaderForStatus(MISSING_VISIT, "\nAffected records: ");
        }
        
        for (MetaRecord rec : recordsSet) {
            finalTracker.incCountForStatus(rec.getRecord_status_enum());
            String msg = rec.getIndex() + ", ";
            finalTracker.concatMessageForStatus(rec.getRecord_status_enum(), msg);
        }
        
        finalTracker.generateQualityReport
                (Constants.getMETA_QUALITY_REPORT_PATH(study_id), header);
        StudyDB.updateMetaQualityReport
                (study_id, Constants.getMETA_QUALITY_REPORT_PATH(study_id));
    }
}
