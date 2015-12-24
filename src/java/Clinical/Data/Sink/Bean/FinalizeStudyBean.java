/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.DataDepositor;
import Clinical.Data.Sink.Database.FinalizingJobEntry;
import Clinical.Data.Sink.Database.StudyDB;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * FinalizeStudyBean is the backing bean for the finalizestudy view.
 * 
 * Author: Tay Wei Hong
 * Date: 22-Dec-2015
 * 
 * Revision History
 * 22-Dec-2015 - Created with all the standard getters and setters. Implemented
 * the module for finalizing study.
 * 23-Dec-2015 - Added 3 group of methods for supporting the 3 datatable in the
 * finalizestudy view.
 */

@ManagedBean (name="finalizedBean")
@ViewScoped
public class FinalizeStudyBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FinalizeStudyBean.class.getName());
    private String study_id;
    private LinkedHashMap<String, String> studyHash;
    private List<String> TIDList = new ArrayList<>();
    private List<FinalizingJobEntry> selectedJobs = new ArrayList<>();
    private List<List<FinalizingJobEntry>> jobEntryLists = 
            new ArrayList<List<FinalizingJobEntry>>();
        
    public FinalizeStudyBean() {
        logger.debug("FinalizeStudyBean created.");
        logger.info(AuthenticationBean.getUserName() + 
                ": access Finalize Study page.");
    }
    
    @PostConstruct
    public void init() {
        // Get the list of study from the user's department that has 
        // completed job(s).
        studyHash = StudyDB.getFinalizeStudyHash
                    (AuthenticationBean.getUserName());
    }
    
    // A new study has been selected by user, need to build the job entries
    // lists.
    public void studyChange() {
        // Clear the lists before building them.
        clearLists();
        buildLists();
    }

    // User has clicked on the Finalize button, need to prepare for the dialog
    // to display to user; to seek for final confirmation to proceed.
    public String prepareForFinalization() {
        logger.info(AuthenticationBean.getUserName() + 
                " begin finalization process for " + study_id + ".");
        // Update job status to finalizing
        for (FinalizingJobEntry job : selectedJobs) {
            SubmittedJobDB.updateJobStatusToFinalizing(job.getJob_id());
        }
        // Start a new thread to insert the finalized pipeline output into 
        // database.
        DataDepositor depositThread = new DataDepositor(study_id, selectedJobs);
        depositThread.start();
        // Update study to completed
        StudyDB.updateStudyToCompleted(study_id);
        
        return Constants.MAIN_PAGE;
    }
    
    // Each time a new study is selected, the system need to clear the old 
    // lists and build new one.
    private void clearLists() {
        if (!TIDList.isEmpty()) {
            TIDList.clear();
        }
        
        if (!jobEntryLists.isEmpty()) {
            jobEntryLists.clear();
        }
    }
    
    // Build new lists based on the study_id selected by user.
    private void buildLists() {
        TIDList = SubmittedJobDB.queryTIDUsedInStudy(study_id);
        int index = 0;
        
        for (String tid : TIDList) {
            jobEntryLists.add(index++, SubmittedJobDB.
                    queryCompletedJobsInStudy(study_id, tid));
        }
    }
    
    // Return the study ID hash map for this user's department.
    public LinkedHashMap<String, String> getStudyHash() {
       return studyHash; 
    }
    
    // Proceed to select job(s) for finalization after a study has been selected.
    public Boolean getStudySelectedStatus() {
        return study_id != null;
    }
    
    // If the user is able to select any Study ID, there is minimum one pipeline
    // technology available.
    public List<FinalizingJobEntry> getJobList0() {
        return jobEntryLists.get(0);
    }
    // Return the pipeline technology name; to be use as data table header title.
    public String getTID0() {
        return TIDList.get(0) + " Technology";
    }
    // Job selected for the first pipeline technology.
    public void setSelectedJob0(FinalizingJobEntry job) {
        selectedJobs.add(0, job);
        logger.debug("Job ID " + job.getJob_id() + " selected for " + 
                     job.getTid() + " in study " + study_id);
    }
    public FinalizingJobEntry getSelectedJob0() {
        return selectedJobs.isEmpty()?null:selectedJobs.get(0);
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
    public String getTID1() {
        return TIDList.get(1) + " Technology";
    }
    public void setSelectedJob1(FinalizingJobEntry job) {
        if (getJobList1Status()) {
            selectedJobs.add(1, job);
            logger.debug("Job ID " + job.getJob_id() + " selected for " + 
                         job.getTid() + " in study " + study_id);
        }
    }
    public FinalizingJobEntry getSelectedJob1() {
        return selectedJobs.isEmpty()?null:selectedJobs.get(1);
    }

    // Do the same for the third data table.
    public Boolean getJobList2Status() {
        return jobEntryLists.size()>2;
    }
    public List<FinalizingJobEntry> getJobList2() {
        return jobEntryLists.get(2);
    }
    public String getTID2() {
        return TIDList.get(2) + " Technology";
    }
    public void setSelectedJob2(FinalizingJobEntry job) {
        if (getJobList2Status()) {
            selectedJobs.add(2, job);
            logger.debug("Job ID " + job.getJob_id() + " selected for " + 
                         job.getTid() + " in study " + study_id);
        }
    }
    public FinalizingJobEntry getSelectedJob2() {
        return selectedJobs.isEmpty()?null:selectedJobs.get(1);
    }
    
    // Machine generated getters and setters
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
}
