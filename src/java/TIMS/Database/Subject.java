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
import java.time.LocalDate;

public class Subject {
    private String subject_id, study_id, race, gender, casecontrol, 
                   age_at_baseline;
    private LocalDate dob;

    // Construct the Subject object directly using the result set returned 
    // from the database query.
    public Subject(ResultSet rs) throws SQLException {
        this.subject_id = rs.getString("subject_id");
        this.study_id = rs.getString("study_id");
        this.race = rs.getString("race");
        this.gender = rs.getString("gender");
        this.dob = rs.getDate("dob").toLocalDate();
        this.casecontrol = rs.getString("casecontrol");
        this.age_at_baseline =rs.getString("age_at_baseline");
    }
    
    // Machine generated constructor
    public Subject(String subject_id, String study_id, String race, 
            String gender, LocalDate dob, String casecontrol, 
            String age_at_baseline) {
        this.subject_id = subject_id;
        this.study_id = study_id;
        this.race = race;
        this.gender = gender;
        this.dob = dob;
        this.casecontrol = casecontrol;
        this.age_at_baseline = age_at_baseline;
    }

    // Return the string representation of this subject in the format of:
    // Study_ID|Subject_ID|Race|Gender|DOB
    @Override
    public String toString() {
        return study_id + "|" + subject_id + "|" + race + "|" + gender 
                + "|" + dob;
    }
    
    // Machine generated getters and setters
    public String getSubject_id() 
    {   return subject_id;  }
    public String getStudy_id() 
    {   return study_id;    }
    public String getRace() 
    {   return race;    }
    public void setRace(String race) 
    {   this.race = race;   }
    public String getGender() 
    {   return gender;  }
    public void setGender(String gender) 
    {   this.gender = gender;   }
    public LocalDate getDob() 
    {   return dob; }
    public void setDob(LocalDate dob) 
    {   this.dob = dob; }
    public String getCasecontrol() 
    {   return casecontrol;    }
    public void setCasecontrol(String casecontrol) 
    {   this.casecontrol = casecontrol;   }
    public String getAge_at_baseline() {
        return age_at_baseline;
    }
    public void setAge_at_baseline(String age_at_baseline) {
        this.age_at_baseline = age_at_baseline;
    }
}
