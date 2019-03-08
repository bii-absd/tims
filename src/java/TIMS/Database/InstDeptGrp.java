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

public class InstDeptGrp {
    // inst_dept_grp view attributes.
    private final String inst_id, inst_name, dept_id, dept_name, 
                         grp_id, grp_name, pi;
    private final boolean active;
    
    // Machine generated constructor.
    public InstDeptGrp(String inst_id, String inst_name, String dept_id, 
                       String dept_name, String grp_id, String grp_name, 
                       String pi, boolean active) 
    {
        this.inst_id = inst_id;
        this.inst_name = inst_name;
        this.dept_id = dept_id;
        this.dept_name = dept_name;
        this.grp_id = grp_id;
        this.grp_name = grp_name;
        this.pi = pi;
        this.active = active;
    }

    // Return the group active status.
    public String getActiveStatus() {
        return active?"Active":"Inactive";
    }
    
    // Machine generated getters.
    public String getInst_id() {
        return inst_id;
    }
    public String getInst_name() {
        return inst_name;
    }
    public String getDept_id() {
        return dept_id;
    }
    public String getDept_name() {
        return dept_name;
    }
    public String getGrp_id() {
        return grp_id;
    }
    public String getGrp_name() {
        return grp_name;
    }
    public String getPi() {
        return pi;
    }
    public boolean isActive() {
        return active;
    }
}
