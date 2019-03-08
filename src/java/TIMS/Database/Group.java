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

public class Group {
    // grp table fields
    private String grp_id, pi, dept_id, grp_name;
    private boolean active;
    // Additional attribute to store the institution, department and PI name.
    private String inst_name, dept_name, pi_name;

    public Group(String grp_id, String pi, String dept_id, String grp_name, 
                 boolean active) 
    {
        this.grp_id = grp_id;
        this.pi = pi;
        this.dept_id = dept_id;
        this.grp_name = grp_name;
        this.active = active;
        dept_name = DepartmentDB.getDeptName(dept_id);
        inst_name = InstitutionDB.getInstName(dept_id);
        if (pi != null) {
            pi_name = UserAccountDB.getFullName(pi);
        }
        else {
            pi_name = "NA";
        }
    }
    
    // Return the institution name that this group belongs to.
    public String getInst_name() {
        return inst_name;
    }
    // Return the department name for this dept_id.
    public String getDept_name() {
        return dept_name;
    }
    // Return the name of the PI incharge of this group.
    public String getPi_name() {
        return pi_name;
    }
    // Return the active status.
    public String getActiveStatus() {
        return active?"Active":"Inactive";
    }
    
    // Machine generated getters and setters.
    public String getGrp_id() {
        return grp_id;
    }
    public void setGrp_id(String grp_id) {
        this.grp_id = grp_id;
    }
    public String getPi() {
        return pi;
    }
    public void setPi(String pi) {
        this.pi = pi;
    }
    public String getDept_id() {
        return dept_id;
    }
    public void setDept_id(String dept_id) {
        this.dept_id = dept_id;
    }
    public String getGrp_name() {
        return grp_name;
    }
    public void setGrp_name(String grp_name) {
        this.grp_name = grp_name;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
}
