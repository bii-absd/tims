/*
 * Copyright @2018
 */
package TIMS.General;

import TIMS.Database.MetaRecord;
import TIMS.Database.SubjectRecordDB;
// Libraries for Java
import java.util.Set;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for Trove
import gnu.trove.set.hash.THashSet;
import gnu.trove.iterator.hash.TObjectHashIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * MetaRecordTester is used to test the validity of the Meta records that have 
 * been uploaded by the user, and to display the preliminary data quality of
 * the uploaded data.
 * 
 * Author: Tay Wei Hong
 * Date: 27-Mar-2018
 * 
 * Revision History
 * 06-Apr-2018 - Created with the following methods: checkForMissingVisit,
 * checkForMissingSubject and validateMetaRecords.
 * 21-May-2018 - Minor changes in method validateMetaRecords().
 */

public class MetaRecordTester {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MetaRecordTester.class.getName());
    private final Set<MetaRecord> recordsSet;
    private final String study_id;

    // Machine generated code.
    public MetaRecordTester(Set<MetaRecord> recordsSet, String study_id) {
        this.recordsSet = recordsSet;
        this.study_id = study_id;
    }

    // Tally the uploaded visits with the existing visits found in
    // the database; to make sure there is no missing visit in this upload.
    // Return the list of missing visits (i.e. Subject ID=Date) if any.
    public String checkForMissingVisit(THashSet<String> visitsHS) {
        StringBuilder missing_visits = new StringBuilder();
        // Make sure all the visits are found in the current upload.
        for (MetaRecord record : recordsSet) {
            // Key is computed by joining subject ID and record date.
            StringBuilder key = new StringBuilder(record.getSubject_id()).
                                append(SubjectRecordDB.joinStr).
                                append(record.getRecordDateAsyyyyMMdd());
            if (visitsHS.contains(key.toString())) 
            {
                // Existing visit, remove this entry.
                visitsHS.remove(key.toString());
            }
            else {
                // New visit for this subject.
                record.setRecordStatusAsNewVisit();
            }
        }
        
        if (!visitsHS.isEmpty()) {
            // There is missing visit(s) in the current upload. Set all the
            // valid record(s) under the same subject ID to MISSING_VISIT.
            TObjectHashIterator<String> itr = visitsHS.iterator();
            while (itr.hasNext()) {
                String key = itr.next();
                // Split the key (i.e. SubjectID=RecordDate)
                String[] srd = key.split(SubjectRecordDB.joinStr);
                String subjectID = srd[0];
                for (MetaRecord record : recordsSet) {
                    // Only interested in valid and new visit records.
                    if (record.isValidOrNewVisit() && 
                        record.getSubject_id().equals(subjectID)) {
                        record.setRecordsStatusAsMissingVisit(srd[1]);
                    }
                }
                // Store the list of missing visits (i.e. Subject ID=Date)
                missing_visits.append(key).append(", ");
            }
        }
        
        if (missing_visits.length() > 0) {
            // Remove the last ',' before returning the string.
            missing_visits.deleteCharAt(missing_visits.lastIndexOf(", "));
        }
        
        return missing_visits.toString();
    }
    
    // Tally the uploaded subject IDs with the existing subject IDs found in 
    // the database; to make sure there is no missing subject in this upload.
    public boolean checkForMissingSubject(TObjectIntHashMap<String> IDsHM) {
        boolean result = Constants.OK;
        // Make sure all the subjects are found in the current upload.
        for (MetaRecord record : recordsSet) {
            if (IDsHM.containsKey(record.getSubject_id())) {
                // Existing subject ID, set status to true i.e. 1
                IDsHM.put(record.getSubject_id(), 1);
            }
            else {
                // New subject ID.
                record.setRecordStatusAsNewSubject();
            }
        }
        
        if (IDsHM.containsValue(0)) {
            // There is missing subject(s) in the current upload.
            result = Constants.NOT_OK;
        }
        
        return result;
    }
    
    // Set all the Meta record status to NEW_SUBJECT.
    public void setAllMetaRecordsStatusToNewSubject() {
        for (MetaRecord record : recordsSet) {
            record.setRecordStatusAsNewSubject();
        }
    }
    
    // Validate the Meta record's core fields using Prolog engine.
    public void validateMetaRecords() {
        // Create the Prolog database for core data validation.
        if (PrologHelper.createMetaRecordValidationDatabase()) {
            logger.info("Prolog database created.");
        }
        else {
            throw new java.lang.RuntimeException("FAIL to create Prolog database!");
        }
        
        for (MetaRecord rec : recordsSet) {
            // All records (i.e. START and NEW_SUBJECT) need to be tested.
            if (!rec.isInvalidOrMissingData()) {
                if (PrologHelper.queryPrologDatabase(rec.getPrologQuery())) {
                    rec.setRecordStatusAsValid();
                }
                else {
                    logger.info("Invalid record #" + rec.getCoreData());
                    rec.setRecordStatusAsInvalid();
                }
            }
        }
        
        // Delete the Prolog database for core data validation.
        if (PrologHelper.deleteMetaRecordValidationDatabase()) {
            logger.info("Prolog database deleted.");
        }
        else {
            throw new java.lang.RuntimeException("FAIL to delete Prolog database!");
        }        
    }
    
    // Machine generated code.
    public Set<MetaRecord> getRecordsSet() 
    {   return recordsSet;  }
}
