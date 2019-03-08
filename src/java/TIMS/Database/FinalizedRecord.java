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

public class FinalizedRecord {
    String annot_ver, subject_id, study_id;
    int array_index, job_id;

    // Machine generated code
    public FinalizedRecord(int array_index, String annot_ver, String subject_id, 
                           int job_id, String study_id) {
        this.array_index = array_index;
        this.annot_ver = annot_ver;
        this.subject_id = subject_id;
        this.job_id = job_id;
        this.study_id = study_id;
    }
    public int getArray_index() {
        return array_index;
    }
    public void setArray_index(int array_index) {
        this.array_index = array_index;
    }
    public String getAnnot_ver() {
        return annot_ver;
    }
    public void setAnnot_ver(String annot_ver) {
        this.annot_ver = annot_ver;
    }
    public String getSubject_id() {
        return subject_id;
    }
    public void setSubject_id(String subject_id) {
        this.subject_id = subject_id;
    }
    public int getJob_id() {
        return job_id;
    }
    public void setJob_id(int job_id) {
        this.job_id = job_id;
    }
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
}
