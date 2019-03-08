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

public class Pipeline implements Serializable {
    private String name, description, tid, command, parameter;
    private boolean editable;

    // Machine generated constructor
    public Pipeline(String name, String description, String tid, String command, 
            String parameter, boolean editable) {
        this.name = name;
        this.description = description;
        this.tid = tid;
        this.command = command;
        this.parameter = parameter;
        this.editable = editable;
    }
    
    // Construct the Pipeline object directly using the result set returned
    // from the database query.
    public Pipeline(ResultSet rs) throws SQLException {
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.tid = rs.getString("tid");
        this.command = rs.getString("command");
        this.parameter = rs.getString("parameter");
        this.editable = rs.getBoolean("editable");
    }

    // Return the String representation of the pipeline command 
    // i.e. name - command parameter
    @Override
    public String toString() {
        return name + " - " + command + " " + parameter;
    }
    
    // Machine generated getters and setters
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getTid() {
        return tid;
    }
    public void setTid(String tid) {
        this.tid = tid;
    }
    public String getCommand() {
        return command;
    }
    public void setCommand(String command) {
        this.command = command;
    }
    public String getParameter() {
        return parameter;
    }
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
    public boolean isEditable() {
        return editable;
    }
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
