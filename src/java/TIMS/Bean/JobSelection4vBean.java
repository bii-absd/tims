/*
 * Copyright @2016
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.FinalizingJobEntry;
import TIMS.Database.StudyDB;
import TIMS.Database.SubmittedJobDB;
import TIMS.Database.UserAccount;
import TIMS.Database.UserAccountDB;
import TIMS.General.Constants;
import TIMS.General.QueryStringGenerator;
import TIMS.General.ResourceRetriever;
import TIMS.Visualizers.cBioVisualizer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JobSelection4vBean is the backing bean for the jobselection4v view.
 * 
 * Author: Tay Wei Hong
 * Date: 20-Jun-2016
 * 
 * Revision History
 * 22-Jun-2016 - Created with all the standard getters and setters. Implemented
 * the UI for Job Selection during Exporting in the Visualization Module.
 * 04-Jul-2016 - Implemented the integration with cBioPortal application.
 * 07-Jul-2016 - To reset the cbio_url to NULL before starting the export.
 */

@ManagedBean (name="js4vBean")
@ViewScoped
public class JobSelection4vBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(JobSelection4vBean.class.getName());
    private String study_id, exportStatus;
    // Store the user ID of the current user.
    private final String userName;
    private final UserAccount user;
    private boolean allowToProceed;
    private LinkedHashMap<String, String> studyHash;
    private List<String> plNameList = new ArrayList<>();
    private FinalizingJobEntry selectedJob0, selectedJob1, selectedJob2, 
                               selectedJob3;
    // Store the list of selected jobs.
    private List<FinalizingJobEntry> selectedJobs = new ArrayList<>();
    private List<List<FinalizingJobEntry>> jobEntryLists = 
            new ArrayList<List<FinalizingJobEntry>>();

    public JobSelection4vBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        user = UserAccountDB.getUserAct(userName);
        logger.debug("JobSelection4vBean created.");
        logger.info(userName + " access Job Selection for Visualization page.");
    }
    
    @PostConstruct
    public void init() {
        String groupQuery = QueryStringGenerator.genGrpQuery4Visualize(user);
        studyHash = StudyDB.getVisualizableStudyHash(groupQuery);
    }
    
    // User has clicked on the Export button, need to prepare for the dialog
    // to display to user; to seek for final confirmation to proceed.
    public String prepare2ExportData() {
        FacesContext fc = FacesContext.getCurrentInstance();
        allowToProceed = true;
        logger.debug("Preparing to export data for visualization.");
        // Check whether the user select any of the job.
        if ((selectedJob0==null) && (selectedJob1==null) && 
            (selectedJob2==null) && (selectedJob3==null)) {
            // None of the job has been selected, display error message and
            // return to the same page.
            allowToProceed = false;
            exportStatus = 
                    "No job has been selected for export.\n" +
                    "\nPlease select the job(s) to be export before proceeding.\n";
            logger.debug("No job has been selected for export.");
        }
        else {
            exportStatus = "The following job(s) has been selected for export:\n";
            // At least one job has been selected, continue.
            selectedJobs.add(0, selectedJob0);
            selectedJobs.add(1, selectedJob1);
            selectedJobs.add(2, selectedJob2);
            selectedJobs.add(3, selectedJob3);
            // Remove all the null objects in the list.
            selectedJobs.removeAll(Collections.singleton(null));
            int index = 1;
            for (FinalizingJobEntry job : selectedJobs) {
                String jobDescription = index + ". " + job.getTid() + " - " 
                                      + job.getUserName() + " - "
                                      + job.getSubmitTimeString() + ".\n";
                exportStatus += jobDescription;
                index++;
            }
        }
        
        return null;
    }

    // User has clicked on the Proceed button in the dialog; proceed with the 
    // exporting of data for visualization.
    public String proceed2ExportData() {
        String nextpage = Constants.MAIN_PAGE;

        // Update cbio_target for all the jobs to false.
        SubmittedJobDB.resetCbioTarget4Study(study_id);
        // Update cbio_target for the selected jobs to true.
        for (FinalizingJobEntry job : selectedJobs) {
            SubmittedJobDB.setCbioTarget4Job(job.getJob_id());
        }
        
        // Ready for export to cBioPortal.
        cBioVisualizer cbio = new cBioVisualizer(userName, study_id, selectedJobs);
        // Record this export activity into database.
        ActivityLogDB.recordUserActivity(userName, Constants.EXP_DAT, study_id);
        // Reset the cBioPortal URL to null in the database.
        StudyDB.resetStudyCbioUrl(study_id);
        // Start the exporting of pipeline data to cBioPortal.
        cbio.start();
        
        return nextpage;
    }

    // User has clicked on the Cancel button in the dialog; do not proceed with
    // the exporting of data.
    public void cancelExportData() {
        // Clear the selected jobs list so that it will be build again when 
        // the User click on the Export button.
        selectedJobs.clear();
        logger.info(userName + ": decided not to proceed with the exporting of data.");
    }
    
    // A new study has been selected by the user, need to build the job entries
    // lists.
    public void studyChange() {
        // Clear the lists before building them.
        clearLists();
        buildLists();
    }

    // Each time a new study is selected, the system need to clear the old 
    // lists and build new one.
    private void clearLists() {
        jobEntryLists.clear();
        selectedJobs.clear();
    }
    
    // Build new lists based on the study_id selected by user.
    private void buildLists() {
        int index = 0;
        // Retrieve all the pipeline that has completed jobs in this study.
        plNameList = SubmittedJobDB.getCompletedPlNameInStudy(study_id);
        
        // For each pipeline, retrieve the list of jobs that belong to it.
        for (String plName : plNameList) {
            jobEntryLists.add(index++, SubmittedJobDB.
                    getCompletedPlJobsInStudy(study_id, plName));
        }
    }

    // A study has been selected if it is not equal to null and "0".
    public Boolean getStudySelectedStatus() {
        if (study_id != null) {
            if (study_id.compareTo("0") != 0) {
                // A study is selected.
                return true;
            }
        }
        
        return false;
    }

    // If the user is able to select any Study ID, there is minimum one pipeline
    // job available.
    public List<FinalizingJobEntry> getJobList0() {
        return jobEntryLists.get(0);
    }
    // Return the pipeline description; to be use as data table header title.
    public String getPl0() {
        return ResourceRetriever.getMsg(plNameList.get(0));
    }
    // Job selected for the first pipeline technology.
    public void setSelectedJob0(FinalizingJobEntry job) {
        selectedJob0 = job;
    }
    public FinalizingJobEntry getSelectedJob0() {
        return selectedJob0;
    }
    
    // Render the second data table if there is more than one pipeline 
    // technology used in this study.
    public Boolean getJobList1Status() {
        return jobEntryLists.size()>1;            
    }
    // Return the list of completed jobs for this pipeline technology.
    public List<FinalizingJobEntry> getJobList1() {
        return jobEntryLists.get(1);
    }
    public String getPl1() {
        return ResourceRetriever.getMsg(plNameList.get(1));
    }
    public void setSelectedJob1(FinalizingJobEntry job) {
        selectedJob1 = job;
    }
    public FinalizingJobEntry getSelectedJob1() {
        return selectedJob1;
    }

    // Do the same for the third data table.
    public Boolean getJobList2Status() {
        return jobEntryLists.size()>2;
    }
    public List<FinalizingJobEntry> getJobList2() {
        return jobEntryLists.get(2);
    }
    public String getPl2() {
        return ResourceRetriever.getMsg(plNameList.get(2));
    }
    public void setSelectedJob2(FinalizingJobEntry job) {
        selectedJob2 = job;
    }
    public FinalizingJobEntry getSelectedJob2() {
        return selectedJob2;
    }
    
    // Do the same for the fourth data table.
    public Boolean getJobList3Status() {
        return jobEntryLists.size()>3;
    }
    public List<FinalizingJobEntry> getJobList3() {
        return jobEntryLists.get(3);
    }
    public String getPl3() {
        return ResourceRetriever.getMsg(plNameList.get(3));
    }
    public void setSelectedJob3(FinalizingJobEntry job) {
        selectedJob3 = job;
    }
    public FinalizingJobEntry getSelectedJob3() {
        return selectedJob3;
    }
    
    // Machine generated getters and setters.
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public LinkedHashMap<String, String> getStudyHash() {
        return studyHash;
    }
    public String getExportStatus() {
        return exportStatus;
    }
    public void setExportStatus(String exportStatus) {
        this.exportStatus = exportStatus;
    }
    public boolean isAllowToProceed() {
        return allowToProceed;
    }    
}