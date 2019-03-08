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

import TIMS.General.Constants;
import TIMS.General.FileHelper;
// Libraries for Java
import java.io.File;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Study {
    // study table attributes
    private String study_id, title, owner_id, grp_id, annot_ver, description, 
                   background, grant_info, finalized_output, detail_files, 
                   summary, icd_code, cbio_url, meta_quality_report;
    private Date start_date, end_date;
    private Boolean finalized, closed;
    private byte[] data_col_name_list;
    // ICD10DB object to return ICD description.
    private static ICD10DB icd_db = new ICD10DB();
    
    // Construct the Study object directly using the result set returned from
    // the database query.
    public Study(ResultSet rs) throws SQLException {
        this.study_id = rs.getString("study_id");
        this.title = rs.getString("title");
        this.grp_id = rs.getString("grp_id");
        this.annot_ver = rs.getString("annot_ver");
        this.icd_code = rs.getString("icd_code");
        this.description = rs.getString("description");
        this.background = rs.getString("background");
        this.grant_info = rs.getString("grant_info");
        this.finalized_output = rs.getString("finalized_output");
        this.detail_files = rs.getString("detail_files");
        this.summary = rs.getString("summary");
        this.start_date = rs.getDate("start_date");
        this.end_date = rs.getDate("end_date");
        this.finalized = rs.getBoolean("finalized");
        this.closed = rs.getBoolean("closed");
        this.cbio_url = rs.getString("cbio_url");
        this.meta_quality_report = rs.getString("meta_quality_report");
        this.data_col_name_list = rs.getBytes("data_col_name_list");
        // Retrieve the PI ID using the Group ID that own this Study.
        this.owner_id = GroupDB.getGrpPIID(grp_id);
    }
    
    // This constructor is used for constructing new Study.
    // For every new Study created, the finalized_output, detail_files, 
    // cbio_url, meta_quality_report and summary will be empty, and closed 
    // status will be false (i.e. not closed).
    public Study(String study_id, String title, String grp_id, String annot_ver,
                 String icd_code, String description, String background, 
                 String grant_info, Date start_date, Date end_date, Boolean finalized) 
    {
        this.study_id = study_id;
        this.title = title;
        this.grp_id = grp_id;
        this.annot_ver = annot_ver;
        this.icd_code = icd_code;
        this.description = description;
        this.background = background;
        this.grant_info = grant_info;
        finalized_output = detail_files = summary = cbio_url = meta_quality_report = null;
        this.start_date = start_date;
        this.end_date = end_date;
        this.finalized = finalized;
        this.closed = false;
        // For new study, data column name is null.
        this.data_col_name_list = null;
    }
    
    // Return the list of data column name.
    public List<String> getDataColumnNameList() {
        List<String> nameList = new ArrayList<>();
        
        if (data_col_name_list != null) {
            nameList = FileHelper.convertByteArrayToList(data_col_name_list);
        }
        
        return nameList;
    }
    
    // If data has been exported to cBioPortal, don't disable the link (i.e.
    // return false.)
    public boolean getCBioDisableStatus() {
        if (cbio_url == null) {
            // Data has not been exported to cBioPortal, diable the link.
            return Constants.OK;
        }
        
        return Constants.NOT_OK;
    }
    
    // If finalized output is ready for download, don't disable the link (i.e.
    // return false.)
    public Boolean getOutputReadyStatus() {
        if (finalized_output != null) {
            return !checkFileReady(finalized_output);            
        }
        else {
            // Finalized output is not ready for download.
            return Constants.OK;
        }
    }
    
    // If summary is ready for download, don't disable the link (i.e. return
    // false).
    public Boolean getSummaryReadyStatus() {
        if (summary != null) {
            return !checkFileReady(summary);            
        }
        else {
            // Summary is not ready for download.
            return Constants.OK;
        }
    }
    
    // Check whether the file is ready for download by checking whether it 
    // exist or not.
    private Boolean checkFileReady(String filename) {
        File file = new File(filename);
        
        return file.exists()?Constants.OK:Constants.NOT_OK;
    }

    // Return the owner full name for this study.
    public String getOwnerFullName() {
        return UserAccountDB.getFullName(owner_id);
    }
    
    // Return the Group name for this study.
    public String getGroupName() {
        return GroupDB.getGrpName(grp_id);
    }
    
    // Return the ICD name for this study.
    public String getICDName() {
        return icd_db.getICDDescription(icd_code);
    }
    
    // Return the review title for each study.
    public String getReviewTitle() {
        String status;
        if (closed) {
            status = "Closed";
        }
        else {
            status = finalized?"Finalized":"Active";
        }
        
        return study_id + "  [PI: " + getOwnerFullName() + 
               "]  [Grant Duration: " + start_date + " to " + end_date + 
               "]  [Status: " + status + "]";
    }
    
    // Return the list of job detail that have exported to the visualizer.
    public List<String> getExportedJobsDetail() {
        List<String> plDetails = new ArrayList<>();
        List<SubmittedJob> jobs = SubmittedJobDB.getcBioExportedJobs(study_id);
        
        for (SubmittedJob job : jobs) {
            plDetails.add(job.getPipeline_name() + 
                          " - Date run: " + job.getSubmitTimeString() + 
                          " Request by: " + UserAccountDB.getFullName(job.getUser_id()));
        }
        
        return plDetails;
    }
    
    // Machine generated getters and setters
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getGrp_id() {
        return grp_id;
    }
    public void setGrp_id(String grp_id) {
        this.grp_id = grp_id;
    }
    public String getAnnot_ver() {
        return annot_ver;
    }
    public void setAnnot_ver(String annot_ver) {
        this.annot_ver = annot_ver;
    }
    public String getIcd_code() {
        return icd_code;
    }
    public void setIcd_code(String icd_code) {
        this.icd_code = icd_code;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getBackground() {
        return background;
    }
    public void setBackground(String background) {
        this.background = background;
    }
    public String getGrant_info() {
        return grant_info;
    }
    public void setGrant_info(String grant_info) {
        this.grant_info = grant_info;
    }
    public Date getStart_date() {
        return start_date;
    }
    public void setStart_date(Date start_date) {
        this.start_date = start_date;
    }
    public Date getEnd_date() {
        return end_date;
    }
    public void setEnd_date(Date end_date) {
        this.end_date = end_date;
    }
    public String getFinalized_output() {
        return finalized_output;
    }
    public void setFinalized_output(String finalized_output) {
        this.finalized_output = finalized_output;
    }
    public String getDetail_files() {
        return detail_files;
    }
    public void setDetail_files(String detail_files) {
        this.detail_files = detail_files;
    }
    public String getSummary() {
        return summary;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }
    public String getCbio_url() {
        return cbio_url;
    }
    public void setCbio_url(String cbio_url) {
        this.cbio_url = cbio_url;
    }
    public String getMeta_quality_report() {
        return meta_quality_report;
    }
    public Boolean getFinalized() {
        return finalized;
    }
    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }
    public Boolean getClosed() {
        return closed;
    }
    public void setClosed(Boolean closed) {
        this.closed = closed;
    }
}
