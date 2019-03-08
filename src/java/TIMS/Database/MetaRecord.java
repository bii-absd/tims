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
package TIMS.Database;

import TIMS.General.Constants;
import TIMS.General.MetaRecordStatusTracker.RecordStatusEnum;
// Libraries for Java
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class MetaRecord {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MetaRecord.class.getName());
    private String subject_id, race, casecontrol, height, weight, gender, 
                   age_at_baseline, msg;
    private LocalDate record_date, dob;
    private int index;
    private List<String> dat;
    private RecordStatusEnum record_status_enum;
    // Round off all the height and weight to 2 decimal places.
    public static final DecimalFormat heightWeightDF = new DecimalFormat("#.##");
    // Round off all the age to 1 decimal place.
    public static DecimalFormat ageDF = new DecimalFormat("#.#");
    // Read in the date in dd/MM/yy format.
    public static final SimpleDateFormat datef = new SimpleDateFormat("dd/MM/yyyy");
    public static final SimpleDateFormat dateTimef = new SimpleDateFormat("dd/MM/yyyy hh:mm a");
    public final DateTimeFormatter dtf_ddMMyyyy = DateTimeFormatter.ofPattern
        ("dd/MM/yyyy", Locale.ENGLISH);
    public final DateTimeFormatter dtf_yyyyMMdd = DateTimeFormatter.ofPattern
        ("yyyy-MM-dd", Locale.ENGLISH);
    private static final Pattern DATE_PATTERN = 
        Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}$");
    private static final Pattern HEIGHT_PATTERN = 
        Pattern.compile("(^[0-2]\\.?[0-9]*$)?");
    private static final Pattern WEIGHT_PATTERN = 
        Pattern.compile("(^[0-9]*\\.?[0-9]*$)?");
    private static final Pattern AGE_PATTERN = 
        Pattern.compile("(^[0-9]*\\.?[0-9]*$)?");
    
    public MetaRecord(String subject_id, String race, String casecontrol, 
            String height, String weight, String record_date, String dob, 
            String gender, String age_at_baseline, List<String> dat, int index) {
        this.subject_id = subject_id;
        this.race = race;
        this.casecontrol = casecontrol;
        this.height = height;
        this.weight = weight;
        this.gender = gender;
        this.age_at_baseline = age_at_baseline;
        this.dat = dat;
        this.index = index;
        this.record_status_enum = RecordStatusEnum.START;
        this.msg = "Record #" + index + " start.";
        datef.setLenient(false);
        ageDF.setRoundingMode(RoundingMode.UP);
        ageDF.setMinimumFractionDigits(1);
        
        if (!checkRecordDateValidity(record_date)) {
            this.record_status_enum = RecordStatusEnum.INVALID_DATE;
            this.msg = "Record #" + index + ": Visit date is invalid.";
            if (!record_date.isEmpty()) {
                logger.error("Invalid record date: " + record_date);
            }
        }
        else if (!checkDOBValidity(dob)) {
            this.record_status_enum = RecordStatusEnum.INVALID_DATE;
            this.msg = "Record #" + index + ": DOB is invalid.";
            if (!dob.isEmpty()) {
                logger.error("Invalid DOB: " + dob);
            }
        }
        else if (checkForNull()) {
            this.record_status_enum = RecordStatusEnum.MISSING_DATA;
            this.msg = "Record #" + index + ": Missing core data.";
        }
        else if (height != null && !height.isEmpty() && 
                !HEIGHT_PATTERN.matcher(height).matches()) {
            this.record_status_enum = RecordStatusEnum.INVALID;
        }
        else if (weight != null && !weight.isEmpty() && 
                !WEIGHT_PATTERN.matcher(weight).matches()) {
            this.record_status_enum = RecordStatusEnum.INVALID;
        }
        else if (age_at_baseline != null && !age_at_baseline.isEmpty() &&
                !AGE_PATTERN.matcher(age_at_baseline).matches()) {
            this.record_status_enum = RecordStatusEnum.INVALID;
        }
        
        // Round off height and weight to 2 decimal places.
        if (this.record_status_enum == RecordStatusEnum.START) {
            if (!height.isEmpty()) {
                this.height = heightWeightDF.format(Double.parseDouble(height));
            }
            if (!weight.isEmpty()) {
                this.weight = heightWeightDF.format(Double.parseDouble(weight));
            }
            if (!age_at_baseline.isEmpty()) {
                this.age_at_baseline = ageDF.format(Double.parseDouble(age_at_baseline));
            }
        }
    }
    
    // Helper functions to set the record status. Below are the possible stage
    // transition for record status:
    // START -> INVALID
    // START -> MISSING_DATA
    // START -> NEW_SUBJECT -> INVALID
    // START -> NEW_SUBJECT -> NEW_SUBJECT_COMPLETED
    // START -> VALID -> MISSING_VISIT
    // START -> VALID -> NEW_VISIT -> MISSING_VISIT
    // START -> VALID -> NEW_VISIT -> NEW_VISIT_COMPLETED
    // START -> VALID -> NEW_VISIT -> NEW_VISIT_COMPLETED -> NEW_VISIT_ERROR
    // START -> VALID -> NEW_VISIT -> DATA_INCONSISTENT
    // START -> VALID -> DATA_INCONSISTENT
    // START -> VALID -> DATA_CONSISTENT
    public void setRecordStatusAsNewSubject() {
        // Only START record can be updated to New Subject.
        if (record_status_enum == RecordStatusEnum.START) {
            record_status_enum = RecordStatusEnum.NEW_SUBJECT;
        }
    }
    // Update this existing subject record as a new visit.
    public void setRecordStatusAsNewVisit() {
        // Only allow valid record to be updated to New Visit.
        // i.e. NEW_SUBJECT_COMPLETED and INVALID records will remain status quo.
        if (record_status_enum == RecordStatusEnum.VALID) {
            record_status_enum = RecordStatusEnum.NEW_VISIT;
        }
    }
    // Update this new visit completed record as new visit error.
    public void setRecordStatusAsNewVisitError() {
        // Failed to insert this new visit into database, mark it as error.
        record_status_enum = RecordStatusEnum.NEW_VISIT_ERROR;
        msg = "Record #" + index + " failed to get inserted into database.";
    }
    // This record failed the validation test i.e. it's core data contain 
    // invalid value.
    public void setRecordStatusAsInvalid() {
        record_status_enum = RecordStatusEnum.INVALID;
        msg = "Record #" + index + " has invalid core data.";
    }
    // If this is a new subject record and it has passed the validation, it is
    // ready for insertion into the database i.e. completed else it will proceed
    // to Valid stage and wait for the next process.
    public void setRecordStatusAsValid() {
        if (record_status_enum == RecordStatusEnum.NEW_SUBJECT) {
            // If this is a new subject record, the check will stop at 
            // validation i.e. there is no need for consistency check.
            record_status_enum = RecordStatusEnum.NEW_SUBJECT_COMPLETED;
        }
        else {
            // From START to VALID.
            record_status_enum = RecordStatusEnum.VALID;
        }
    }
    // Once a missing visit has been found, all the record(s) under the same
    // subject ID will be mark as MISSING_VISIT.
    public void setRecordsStatusAsMissingVisit(String missingVisit) {
        record_status_enum = RecordStatusEnum.MISSING_VISIT;
        // Record down the missing visit date.
        msg = missingVisit;
    }
    // This record has failed the data consistency check, mark it as so.
    public void setRecordsStatusAsDataInconsistent() {
        record_status_enum = RecordStatusEnum.DATA_INCONSISTENT;
    }
    // This record has passed the data consistency check, mark it accordingly
    // to the visit status.
    public void setRecordsStatusAsDataConsistent() {
        if (record_status_enum == RecordStatusEnum.NEW_VISIT) {
            // A new subject_record will be inserted into the database.
            record_status_enum = RecordStatusEnum.NEW_VISIT_COMPLETED;
        }
        else {
            // This existing record has passed all the checks; nothing further
            // will be done.
            record_status_enum = RecordStatusEnum.DATA_CONSISTENT;
        }
    }
    
    // Return true if record is start.
    public boolean isStart() {
        return (record_status_enum == RecordStatusEnum.START);
    }
    // Return true if record is valid or new visit.
    public boolean isValidOrNewVisit() {
        return (record_status_enum == RecordStatusEnum.VALID) ||
               (record_status_enum == RecordStatusEnum.NEW_VISIT);
    }
    // Return true if record is valid.
    public boolean isValid() {
        return (record_status_enum == RecordStatusEnum.VALID);
    }
    // Return true if record is invalid or missing data.
    public boolean isInvalidOrMissingData() {
        return (record_status_enum == RecordStatusEnum.INVALID) || 
               (record_status_enum == RecordStatusEnum.INVALID_DATE) ||
               (record_status_enum == RecordStatusEnum.MISSING_DATA);
    }
    // Return true if record is invalid.
    public boolean isInvalid() {
        return (record_status_enum == RecordStatusEnum.INVALID) ||
               (record_status_enum == RecordStatusEnum.INVALID_DATE);
    }
    // Return true if record is new subject completed.
    public boolean isNewSubjectCompleted() {
        return (record_status_enum == RecordStatusEnum.NEW_SUBJECT_COMPLETED);
    }
    // Return true if record has missng data.
    public boolean isMissingData() {
        return (record_status_enum == RecordStatusEnum.MISSING_DATA);
    }
    // Return true if record is new visit.
    public boolean isNewVisit() {
        return (record_status_enum == RecordStatusEnum.NEW_VISIT);
    }
    // Return true if record is new visit completed.
    public boolean isNewVisitCompleted() {
        return (record_status_enum == RecordStatusEnum.NEW_VISIT_COMPLETED);
    }
    
    // Test the validity of the record date string.
    private boolean checkRecordDateValidity(String date_str) {
        try {
            Date date = datef.parse(date_str);
            record_date = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            // Further check to make sure the year is 4 digits.
            return DATE_PATTERN.matcher(date_str).matches();
        } catch (ParseException pe) {
            logger.error(pe.getMessage());
            return Constants.NOT_OK;
        }
    }
    // Test the validity of the date of birth string.
    private boolean checkDOBValidity(String date_str) {
        try {
            Date date = datef.parse(date_str);
            dob = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            // Further check to make sure the year is 4 digits.
            return DATE_PATTERN.matcher(date_str).matches();
        } catch (ParseException pe) {
            logger.error(pe.getMessage());
            return Constants.NOT_OK;
        }
    }
    // Check for missing core data.
    private boolean checkForNull() {
        return (race.isEmpty()) ||  (gender.isEmpty()) || 
               (subject_id.isEmpty() || casecontrol.isEmpty());
    }
    
    // Return the Prolog query that will be used to validate the meta record.
    public String getPrologQuery() {
        // partial_validate(Race,Gender)
        if (!height.isEmpty() && !weight.isEmpty()) {
            return "full_validate(" + race.toLowerCase() + "," + 
                   gender.toLowerCase() + "," + casecontrol.toLowerCase() + 
                   "," + height + "," + weight + ")";
        }
        else {
            return "partial_validate(" + race.toLowerCase() + "," + 
                    gender.toLowerCase() + "," + casecontrol.toLowerCase() + ")";
        }
    }
    
    // Return the consolidation core data as a string.
    public String getCoreData() {
        return index + ": " + race + " - " + gender + " - " + 
               casecontrol + " - " + height + " - " + weight;
    }
    
    // Return record date as a String in this format yyyy-MM-dd
    public String getRecordDateAsyyyyMMdd() {
        if (record_date != null) {
            return record_date.format(dtf_yyyyMMdd);
        }
        return "";        
    }
    // Return date as a String in this format dd/MM/yyyy
    public String getRecordDateAsddMMyyyy() {
        if (record_date != null) {
            return record_date.format(dtf_ddMMyyyy);
        }
        return "";
    }
    public String getDOBAsddMMyyyy() {
        if (dob != null) {
            return dob.format(dtf_ddMMyyyy);
        }
        return "";
    }
    
    // Machine generated getters and setters.
    public String getSubject_id() 
    {   return subject_id;  }

    public void setSubject_id(String subject_id) 
    {   this.subject_id = subject_id;   }

    public String getRace() 
    {   return race;    }

    public void setRace(String race) 
    {   this.race = race;   }

    public String getCasecontrol() 
    {   return casecontrol;    }

    public void setCasecontrol(String casecontrol) 
    {   this.casecontrol = casecontrol;   }

    public String getHeight() 
    {   return height;  }

    public void setHeight(String height) 
    {   this.height = height;   }

    public String getWeight() 
    {   return weight;  }

    public void setWeight(String weight) 
    {   this.weight = weight;   }

    public LocalDate getRecord_date() 
    {   return record_date;     }

    public void setRecord_date(LocalDate record_date) 
    {   this.record_date = record_date; }

    public LocalDate getDob() 
    {   return dob; }

    public void setDob(LocalDate dob) 
    {   this.dob = dob; }

    public String getGender() 
    {   return gender;  }

    public void setGender(String gender) 
    {   this.gender = gender;   }

    public String getAge_at_baseline() {
        return age_at_baseline;
    }

    public void setAge_at_baseline(String age_at_baseline) {
        this.age_at_baseline = age_at_baseline;
    }

    public List<String> getDat() 
    {   return dat; }

    public void setDat(List<String> dat) 
    {   this.dat = dat; }

    public int getIndex() 
    {   return index;   }

    public void setIndex(int index) 
    {   this.index = index; }

    public RecordStatusEnum getRecord_status_enum() 
    {   return record_status_enum;  }

    public String getMsg() 
    {   return msg; }
}
