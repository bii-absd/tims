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

public class UserAccount {
    // user_account table fields
    private String user_id, first_name, last_name, photo, email, pwd;
    private Boolean active;
    private int role_id;
    private String unit_id, last_login;

    public UserAccount(String user_id, int role_id, String first_name, 
                       String last_name, String photo, String email, 
                       Boolean active, String pwd, String unit_id, 
                       String last_login) {
        this.user_id = user_id;
        this.role_id = role_id;
        this.first_name = first_name;
        this.last_name = last_name;
        this.photo = photo;
        this.email = email;
        this.active = active;
        this.pwd = pwd;
        this.unit_id = unit_id;
        this.last_login = last_login;
    }

    // Return the Active status.
    public String getActiveStatus() { return active?"Enabled":"Disabled"; }
    // Return the Role Name.
    public String getRoleName() { return UserRoleDB.getRoleNameFromHash(role_id); }
    
    //Machine generated setters
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setFirst_name(String first_name) { this.first_name = first_name; }
    public void setLast_name(String last_name) { this.last_name = last_name; }
    public void setPhoto(String photo) { this.photo = photo; }
    public void setEmail(String email) { this.email = email; }
    public void setActive(Boolean active) { this.active = active; }
    public void setPwd(String pwd) { this.pwd = pwd; }
    public void setRole_id(int role_id) { this.role_id = role_id; }
    public void setUnit_id(String unit_id) { this.unit_id = unit_id; }
    public void setLast_login(String last_login) { this.last_login = last_login; }
    // Machine generated getters
    public String getUser_id() { return user_id; }
    public String getFirst_name() { return first_name; }
    public String getLast_name() { return last_name; }
    public String getPhoto() { return photo; }
    public String getEmail() { return email; }
    public Boolean getActive() { return active; }
    public String getPwd() { return pwd; }
    public int getRole_id() { return role_id; }
    public String getUnit_id() { return unit_id; }
    public String getLast_login() { return last_login; }
}
