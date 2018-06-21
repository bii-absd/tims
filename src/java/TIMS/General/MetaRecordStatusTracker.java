/*
 * Copyright @2018
 */
package TIMS.General;

// Libraries for Java
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;
// Libraries for Log4j
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MetaRecordStatusTracker is used to track the status of the uploaded records.
 * It will hold the record count and message associated with each record status.
 * 
 * Author: Tay Wei Hong
 * Date: 26-Apr-2018
 * 
 * Revision History
 * 26-Apr-2018 - Created with the following methods: createFinalTracker,
 * createPreliminaryTracker, createCommonCounter, generateQualityReport,
 * getStatsForPassedRecords, etc. And a static class StatusCounter.
 */

public class MetaRecordStatusTracker {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MetaRecordStatusTracker.class.getName());
    private Map<RecordStatusEnum, StatusCounter> statistics = new LinkedHashMap<>();
    private final int total_records;
    private final boolean preliminary;
    private final NumberFormat nf = NumberFormat.getPercentInstance();
    // All the available record status
    public static enum RecordStatusEnum {START, VALID, INVALID, INVALID_DATE,
                                         MISSING_DATA, MISSING_VISIT, 
                                         DATA_CONSISTENT, DATA_INCONSISTENT, 
                                         NEW_SUBJECT, NEW_SUBJECT_COMPLETED, 
                                         NEW_VISIT, NEW_VISIT_COMPLETED, 
                                         NEW_VISIT_ERROR};

    // This is a private constructor i.e. object of this class can only be 
    // created through the static methods defined below.
    private MetaRecordStatusTracker(Map<RecordStatusEnum, StatusCounter> statistics, 
            int total_records, boolean preliminary) {
        this.statistics = statistics;
        this.total_records = total_records;
        this.preliminary = preliminary;
        nf.setMaximumFractionDigits(1);
    }
    
    // Used to construct the tracker for final data quality check.
    public static MetaRecordStatusTracker createFinalTracker(int total_records) {
        Map<RecordStatusEnum, StatusCounter> stat = createCommonCounter();
        // In final report, we only track the following status:
        // MISSING_DATA, INVALID_DATE, MISSING_VISIT, INVALID, VALID,
        // NEW_SUBJECT_COMPLETED, NEW_VISIT_COMPLETED, NEW_VISIT_ERROR,
        // DATA_CONSISTENT and DATA_INCONSISTENT.
        stat.put(RecordStatusEnum.NEW_VISIT_COMPLETED, 
                new StatusCounter("Verified new visit records: ", ""));
        stat.put(RecordStatusEnum.NEW_VISIT_ERROR, 
                new StatusCounter("Duplicated visit records: ", ""));
        stat.put(RecordStatusEnum.DATA_INCONSISTENT, 
                new StatusCounter("Records with inconsistent data: ", ""));
        stat.put(RecordStatusEnum.DATA_CONSISTENT, 
                new StatusCounter("Records with consistent data: ", ""));
        
        return new MetaRecordStatusTracker(stat, total_records, false);
    }

    // Used to construct the tracker for preliminary data quality check.
    public static MetaRecordStatusTracker createPreliminaryTracker(int total_records) {
        Map<RecordStatusEnum, StatusCounter> stat = createCommonCounter();
        // In preliminary report, we only track the following status:
        // MISSING_DATA, INVALID_DATE, MISSING_VISIT, INVALID, VALID, 
        // NEW_SUBJECT_COMPLETED and NEW_VISIT.
        stat.put(RecordStatusEnum.NEW_VISIT, 
                new StatusCounter("New visit records: ", ""));
        
        return new MetaRecordStatusTracker(stat, total_records, true);
    }
    
    // Create the common status counters for both the preliminary and final
    // tracker.
    private static Map<RecordStatusEnum, StatusCounter> createCommonCounter() {
        Map<RecordStatusEnum, StatusCounter> stat = new LinkedHashMap<>();
        // The following 6 status are common.
        stat.put(RecordStatusEnum.INVALID_DATE, 
                new StatusCounter("Records with invalid date: ", ""));
        stat.put(RecordStatusEnum.MISSING_DATA, 
                new StatusCounter("Records with missing data: ", ""));
        // For missing visits, we need to print the missing visits in the
        // sub-header.
        stat.put(RecordStatusEnum.MISSING_VISIT, 
                new StatusCounter("Missing visits detected: \n", ""));
        stat.put(RecordStatusEnum.INVALID, 
                new StatusCounter("Records with invalid data: ", ""));
        stat.put(RecordStatusEnum.VALID, 
                new StatusCounter("Validated records: ", ""));
        stat.put(RecordStatusEnum.NEW_SUBJECT_COMPLETED, 
                new StatusCounter("Verified new subject records: ", ""));
        
        return stat;
    }
    
    // Generate the quality report of the data provided in this upload .
    public void generateQualityReport(String filename, String header) {
        try (PrintStream ps = new PrintStream(new File(filename))) {
            ps.println(header);
            ps.println();
            for (Map.Entry<RecordStatusEnum, StatusCounter> stat :
                    statistics.entrySet()) {
                if (stat.getValue().getNum_of_records() > 0) {
                    ps.print(stat.getValue().getSub_header());
                    ps.println(getStatsForStatus(stat.getKey()));
                    if ((stat.getKey() != RecordStatusEnum.NEW_SUBJECT_COMPLETED) 
                     && (stat.getKey() != RecordStatusEnum.DATA_CONSISTENT) ) {
                        // The list for NEW_SUBJECT_COMPLETED and 
                        // DATA_CONSISTENT is too long, so we don't print them.
                        ps.println(stat.getValue().getMessage());                        
                    }
                    ps.println();
                }
            }
        } catch (FileNotFoundException fnf) {
            logger.error(fnf.getMessage());
        }
    }
    
    // During preliminary, records with status VALID, NEW_VISIT and 
    // NEW_SUBJECT_COMPLETED are considered as passed. After all the checks
    // have been preformed, records with status NEW_SUBJECT_COMPLETED, 
    // NEW_VISIT_COMPLETED and DATA_CONSISTENT are considered as passed.
    public String getStatsForPassedRecords() {
        int count = 0;
        
        if (preliminary) {
            count = statistics.get(RecordStatusEnum.VALID).getNum_of_records()
                  + statistics.get(RecordStatusEnum.NEW_VISIT).getNum_of_records()
                  + statistics.get(RecordStatusEnum.NEW_SUBJECT_COMPLETED).getNum_of_records();
        }
        else {
            count = statistics.get(RecordStatusEnum.NEW_SUBJECT_COMPLETED).getNum_of_records()
                  + statistics.get(RecordStatusEnum.NEW_VISIT_COMPLETED).getNum_of_records()
                  + statistics.get(RecordStatusEnum.DATA_CONSISTENT).getNum_of_records();
        }
        
        return  count + "/" + total_records + " (" +
                nf.format((double)count/total_records) + ")";        
    }
    
    // Return the statistics of this status (with respect to the total_records.)
    public String getStatsForStatus(RecordStatusEnum status) {
        if (statistics.containsKey(status)) {
            int count = statistics.get(status).getNum_of_records();
            return  count + "/" + total_records + " (" +
                    nf.format((double)count/total_records) + ")";
        }
        return "";        
    }
    
    // Return the message associated with this status.
    public String getMessageForStatus(RecordStatusEnum status) {
        if (statistics.containsKey(status)) {
            return statistics.get(status).getMessage();
        }
        return "";
    }
    
    // Return the number of records having this status.
    public int getNumOfRecordsForStatus(RecordStatusEnum status) {
        if (statistics.containsKey(status)) {
            return statistics.get(status).getNum_of_records();
        }
        return 0;
    }
    
    // Increment the num_of_records for this status by one.
    public void incCountForStatus(RecordStatusEnum status) {
        if (statistics.containsKey(status)) {
            statistics.get(status).incNum_of_records();
        }
    }
    
    // Concatenate msg to the message for this status.
    public void concatMessageForStatus(RecordStatusEnum status, String msg) {
        if (statistics.containsKey(status)) {
            statistics.get(status).concatMessage(msg);
        }
    }
    
    // Concatenate msg to the sub-header for this status.
    public void concatSubHeaderForStatus(RecordStatusEnum status, String msg) {
        if (statistics.containsKey(status)) {
            statistics.get(status).concatSubHeader(msg);
        }
    }
    
    // static nested class.
    private static class StatusCounter {
        private int num_of_records;
        private String message, sub_header;

        public StatusCounter(String sub_header, String message) {
            this.num_of_records = 0;
            this.message = message;
            this.sub_header = sub_header;
        }
        // Increment the num_of_record by one.
        public void incNum_of_records() {
            num_of_records++;
        }
        // Concatenate msg to message.
        public void concatMessage(String msg) {
            message += msg;
        }
        // Concatenate msg to sub_header.
        public void concatSubHeader(String msg) {
            sub_header += msg;
        }
        // Machine generated code.
        public int getNum_of_records() {
            return num_of_records;
        }
        public void setNum_of_records(int num_of_records) {
            this.num_of_records = num_of_records;
        }
        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
        public String getSub_header() {
            return sub_header;
        }
        public void setSub_header(String sub_header) {
            this.sub_header = sub_header;
        }
    }
}