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

public class Department {
    // dept table fields
    private String inst_id, dept_id, dept_name;
    // Additional attribute to store the institution name
//    private final String inst_name;

    public Department(String inst_id, String dept_id, 
            String dept_name) {
        this.inst_id = inst_id;
        this.dept_id = dept_id;
        this.dept_name = dept_name;
//        inst_name = InstitutionDB.getInstName(inst_id);
    }

    // Return the institution name for this inst_id.
//    public String getInst_name() {
//        return inst_name;
//    }
    
    // Machine generated getters and setters
    public String getInst_id() {
        return inst_id;
    }
    public void setInst_id(String inst_id) {
        this.inst_id = inst_id;
    }
    public String getDept_id() {
        return dept_id;
    }
    public void setDept_id(String dept_id) {
        this.dept_id = dept_id;
    }
    public String getDept_name() {
        return dept_name;
    }
    public void setDept_name(String dept_name) {
        this.dept_name = dept_name;
    }   
}
