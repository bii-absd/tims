/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

/**
 * Institution is used to represent the institution table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Created with all the standard getters and setters.
 */

public class Institution {
    // institution table fields
    private String institution_code, institution_name;

    public Institution(String institution_code, String institution_name) {
        this.institution_code = institution_code;
        this.institution_name = institution_name;
    }

    // Machine generated getters and setters
    public String getInstitution_code() 
    {   return institution_code;    }
    public void setInstitution_code(String institution_code) 
    {   this.institution_code = institution_code;   }
    public String getInstitution_name() 
    {   return institution_name;    }
    public void setInstitution_name(String institution_name) 
    {   this.institution_name = institution_name;   }
}
