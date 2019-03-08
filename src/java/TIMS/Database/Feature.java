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

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;

public class Feature implements Serializable {
    private String fcode, status;
    private LinkedHashMap<String, String> options_hashmap;
    
    // Construct the Feature object directly using the result set returned from
    // the database query.
    public Feature(ResultSet rs) throws SQLException {
        this.fcode = rs.getString("fcode");
        this.status = rs.getString("status");
        this.options_hashmap = new LinkedHashMap<>();
        // Build the options hashmap from the options string stored in database.
        String[] options = rs.getString("options").split("\\|");
        for (String option : options) {
            options_hashmap.put(option, option);
        }
    }
    
    // Machine generated getters and setters.
    public String getFcode() {
        return fcode;
    }
    public void setFcode(String fcode) {
        this.fcode = fcode;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public LinkedHashMap<String, String> getOptions_hashmap() {
        return options_hashmap;
    }
}
