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

// Libraries for Java
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class DashboardConfig {
    private String chart_id, study_id, title, label_x, data_source_x, data_source_y;
    private static final List<String> core_data_options = 
                            Arrays.asList("age","race","gender","casecontrol");
    
    // Construct the DashboardConfig object directly using the result set
    // returned from the database query.
    public DashboardConfig(ResultSet rs) throws SQLException {
        this.chart_id = rs.getString("chart_id");
        this.study_id = rs.getString("study_id");
        this.title = rs.getString("title");
        this.label_x = rs.getString("label_x");
        this.data_source_x = rs.getString("data_source_x");
        this.data_source_y = rs.getString("data_source_y");
    }
    
    // Machine generated constructor
    public DashboardConfig(String chart_id, String study_id, String label_x, 
            String data_source_x, String data_source_y) {    
        this.chart_id = chart_id;
        this.study_id = study_id;
        this.label_x = label_x;
        this.data_source_x = data_source_x;
        this.data_source_y = data_source_y;
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
    public String getLabel_x() 
    {   return label_x;     }
    public void setLabel_x(String label_x) 
    {   this.label_x = label_x; }
    public String getData_source_x() 
    {   return data_source_x;   }
    public void setData_source_x(String data_source_x) 
    {   this.data_source_x = data_source_x;     }
    public String getData_source_y() 
    {   return data_source_y;   }
    public void setData_source_y(String data_source_y) 
    {   this.data_source_y = data_source_y;     }
}
