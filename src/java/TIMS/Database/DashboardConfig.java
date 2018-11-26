/*
 * Copyright @2018
 */
package TIMS.Database;

// Libraries for Java
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * DashboardConfig is used to represent the dashboard_config table in the 
 * database.
 * 
 * Author: Tay Wei Hong
 * Date: 09-Nov-2018
 * 
 * Revision History
 * 09-Nov-2018 - Created with all the standard getters and setters.
 */

public class DashboardConfig {
    private String chart_id, study_id, title, data_source_x, data_source_y;
    private boolean inverted;
    private static final List<String> core_data_options = 
                            Arrays.asList("age","race","gender","casecontrol");
    
    // Construct the DashboardConfig object directly using the result set
    // returned from the database query.
    public DashboardConfig(ResultSet rs) throws SQLException {
        this.chart_id = rs.getString("chart_id");
        this.study_id = rs.getString("study_id");
        this.title = rs.getString("title");
        this.data_source_x = rs.getString("data_source_x");
        this.data_source_y = rs.getString("data_source_y");
        this.inverted = rs.getBoolean("inverted");
    }
    
    // Machine generated constructor
    public DashboardConfig(String chart_id, String study_id, String title, 
            String data_source_x, String data_source_y, boolean inverted) {
        this.chart_id = chart_id;
        this.study_id = study_id;
        this.title = title;
        this.data_source_x = data_source_x;
        this.data_source_y = data_source_y;
        this.inverted = inverted;
    }
    
    // Check for bar chart configuration type.
    public boolean isBarchart() {
        return (chart_id.equals("BARCL") || chart_id.equals("BARCR"));
    }
    
    // Check whether is data source x from core data.
    public boolean is_x_from_core_data() {
        return core_data_options.contains(data_source_x);
    }
    // Check whether is data source y from core data.
    public boolean is_y_from_core_data() {
        return core_data_options.contains(data_source_y);
    }
    
    // Return the label to be use for x-axis base on the data source for x.
    public String get_x_label() {
        String label = "";
        
        if (data_source_x.equals("race")) {
            label = "ETHNICITY";
        }
        else {
            label = data_source_x.toUpperCase();
        }
        
        return label;
    }
    
    // Machine generated getters and setters
    public String getChart_id() 
    {   return chart_id;    }
    public void setChart_id(String chart_id) 
    {   this.chart_id = chart_id;   }
    public String getStudy_id() 
    {   return study_id;    }
    public void setStudy_id(String study_id) 
    {   this.study_id = study_id;   }
    public String getTitle() 
    {   return title;       }
    public void setTitle(String title) 
    {   this.title = title; }
    public String getData_source_x() 
    {   return data_source_x;   }
    public void setData_source_x(String data_source_x) 
    {   this.data_source_x = data_source_x;     }
    public String getData_source_y() 
    {   return data_source_y;   }
    public void setData_source_y(String data_source_y) 
    {   this.data_source_y = data_source_y;     }
    public boolean isInverted() 
    {   return inverted;    }
    public void setInverted(boolean inverted) 
    {   this.inverted = inverted;   }
}
