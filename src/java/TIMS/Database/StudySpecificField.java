/*
 * Copyright @2018
 */
package TIMS.Database;

/**
 * StudySpecificField is used to represent the study_specific_fields table in 
 * the database.
 * 
 * Author: Tay Wei Hong
 * Date: 19-Jul-2018
 * 
 * Revision History
 * 19-Jul-2018 - Created with all the standard getters and setters.
 */

public class StudySpecificField {
    private String category, field;

    // Machine generated code.
    public StudySpecificField(String category, String field) {
        this.category = category;
        this.field = field;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}
