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
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.SubmittedJob;
import TIMS.Database.SubmittedJobDB;
import TIMS.General.Constants;
import TIMS.General.FileHelper;
// Libraries for Java
import java.io.Serializable;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.inject.Named;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("jsBean")
@ViewScoped
public class JobStatusBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(JobStatusBean.class.getName());
    private List<SubmittedJob> jobSubmission;
    private SubmittedJob selectedJob;
    // Store the user ID of the current user.
    private final String userName;
    // Indicator for retrieving and displaying the list of jobs for single 
    // user or all users?
    private boolean singleUser;
    
    public JobStatusBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        singleUser = (boolean) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("singleUser");
        logger.debug("JobStatusBean created.");
        logger.info(userName + ": access Job Status page. Single: " + singleUser);
    }

    @PostConstruct
    public void init() {
        // Need to assign the jobSubmission here, else the sorting will not work.
        if (singleUser) {
            jobSubmission = SubmittedJobDB.getUserJobs(userName);
        }
        else {
            // The administrator is accessing the submitted jobs from all the users.
            jobSubmission = SubmittedJobDB.getAllUsersJobs();
        }
    }
    
    // Download the pipeline output for user.
    public void downloadOutput(SubmittedJob job) {
        String detail = "Output " + job.getOutput_file();
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, detail);
        FileHelper.download(job.getOutput_file());
    }
    
    // Download the pipeline detail output for user.
    public void downloadDetailOutput(SubmittedJob job) {
        String detail = "Detail Output " + job.getOutput_file();
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, detail);
        FileHelper.download(job.getDetail_output());
    }
    
    // Download the pipeline report for user.
    public void downloadReport(SubmittedJob job) {
        String detail = "Report " + job.getReport();
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, detail);
        FileHelper.download(job.getReport());
    }
    
    // Return the list of SubmittedJob objects that belong to the current user.
    // NOTE: This function will get called multiple times, hence should'nt
    // include any business logic into it (i.e. performance issue). The 
    // initialisation of jobSubmission will be done in PostConstruct function
    // init() instead.
    public List<SubmittedJob> getJobSubmission() {
        return jobSubmission;
    }

    // Machine generated getters and setters
    public SubmittedJob getSelectedJob() {
        return selectedJob;
    }
    public void setSelectedJob(SubmittedJob selectedJob) {
        this.selectedJob = selectedJob;
    }
    public boolean isSingleUser() {
        return singleUser;
    }
    
    /* @ViewScoped will breaks when any UIComponent is bound to the bean
       using binding attribute or when using JSTL tags in the view; the bean
       will behave like a request scoped one.
    public UIData getJobStatusTable() { return jobStatusTable; }
    public void setJobStatusTable(UIData jobStatusTable) {
        this.jobStatusTable = jobStatusTable;
    }
    */
}