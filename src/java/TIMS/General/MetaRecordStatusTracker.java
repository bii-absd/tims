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
        StringBuilder oper = new StringBuilder();
        
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
        
        oper.append(count).append("/").append(total_records).append(" (").
             append(nf.format((double)count/total_records)).append(")");
        return oper.toString();
    }
    
    // Return the statistics of this status (with respect to the total_records.)
    public String getStatsForStatus(RecordStatusEnum status) {
        StringBuilder oper = new StringBuilder();
        
        if (statistics.containsKey(status)) {
            int count = statistics.get(status).getNum_of_records();
            oper.append(count).append("/").append(total_records).append(" (").
                 append(nf.format((double)count/total_records)).append(")");
        }
        return oper.toString();
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
        private StringBuilder message, sub_header;

        public StatusCounter(String sub_header, String message) {
            this.num_of_records = 0;
            this.message = new StringBuilder(message);
            this.sub_header = new StringBuilder(sub_header);
        }
        // Increment the num_of_record by one.
        public void incNum_of_records() {
            num_of_records++;
        }
        // Concatenate msg to message.
        public void concatMessage(String msg) {
            message.append(msg);
        }
        // Concatenate msg to sub_header.
        public void concatSubHeader(String msg) {
            sub_header.append(msg);
        }
        // Machine generated code.
        public int getNum_of_records() {
            return num_of_records;
        }
        public void setNum_of_records(int num_of_records) {
            this.num_of_records = num_of_records;
        }
        public String getMessage() {
            return message.toString();
        }
        public void setMessage(String message) {
            this.message = new StringBuilder(message);
        }
        public String getSub_header() {
            return sub_header.toString();
        }
        public void setSub_header(String sub_header) {
            this.sub_header = new StringBuilder(sub_header);
        }
    }
}
