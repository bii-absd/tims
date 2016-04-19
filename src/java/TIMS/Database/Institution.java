/*
 * Copyright @2015
 */
package TIMS.Database;

/**
 * Institution is used to represent the institution table in the database.
 * 
 * Author: Tay Wei Hong
 * Date: 13-Nov-2015
 * 
 * Revision History
 * 13-Nov-2015 - Created with all the standard getters and setters.
 * 01-Dec-2015 - Implementation for database 2.0
 */

public class Institution {
    // institution table fields
    private String inst_id, inst_name;

    public Institution(String inst_id, String inst_name) {
        this.inst_id = inst_id;
        this.inst_name = inst_name;
    }

    // Machine generated getters and setters
    public String getInst_id() 
    {   return inst_id;    }
    public void setInst_id(String inst_id) 
    {   this.inst_id = inst_id;   }
    public String getInst_name() 
    {   return inst_name;    }
    public void setInst_name(String inst_name) 
    {   this.inst_name = inst_name;   }
}
