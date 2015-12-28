/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.DataDepositor;
import Clinical.Data.Sink.Database.FinalizingJobEntry;
import Clinical.Data.Sink.Database.StudyDB;
import Clinical.Data.Sink.Database.SubjectDB;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
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
 * 28-Dec-2015 - Completed the module for finalizing study. Added 5 new 
 * attributes dept_id, subMDAvailableStatus, selectedJob0, selectedJob1 & 
 * selectedJob2. Added 3 new methods proceedForFinalization, 
 * checkSubMDAvailability & cancelFinalization.
 */

@ManagedBean (name="finalizedBean")
@ViewScoped
public class FinalizeStudyBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FinalizeStudyBean.class.getName());
    private String study_id, dept_id;
    private LinkedHashMap<String, String> studyHash;
    // Store the status of the availability of subject meta data in the database.
    private String subMDAvailableStatus = null;
    private List<String> TIDList = new ArrayList<>();
    private FinalizingJobEntry selectedJob0, selectedJob1, selectedJob2;
    // Store the list of selected jobs.
    private List<FinalizingJobEntry> selectedJobs = new ArrayList<>();
    private List<List<FinalizingJobEntry>> jobEntryLists = 
            new ArrayList<List<FinalizingJobEntry>>();
        
    public FinalizeStudyBean() {
        dept_id = UserAccountDB.getDeptID(AuthenticationBean.getUserName());
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
    public void prepareForFinalization() {
        // Store the subject ID that doesn't have meta data in the database.
        StringBuilder subMetaDataNotFound = new StringBuilder();
        selectedJobs.add(0, selectedJob0);
        selectedJobs.add(1, selectedJob1);
        selectedJobs.add(2, selectedJob2);
        
        logger.info("Preparing for Study finalization.");
        // Check for subject meta data availability, and prepare the status
        // update string.
        for (FinalizingJobEntry job : selectedJobs) {
            try {
                if (job != null) {
                    checkSubMDAvailability(job.getJob_id(), subMetaDataNotFound);                    
                }
            }
            catch (SQLException e) {
                // Error in checking the database for subject meta data, need 
                // to stop the user from proceeding.
                logger.error("SQLException when checking for subject meta data availability!");
                logger.error(e.getMessage());
                //return Constants.ERROR;
            }
        }

        if (subMetaDataNotFound.toString().isEmpty()) {
            subMDAvailableStatus = 
                    "All the subject's meta data are found in the database.\n" +
                    "Please proceed with the finalization of this Study.";
            logger.debug("All the subject meta data is found.");
        }
        else {
            subMDAvailableStatus = 
                    "The following subject's meta data are not found in the database:\n" +
                    subMetaDataNotFound +
                    "\nPlease upload the subject's meta data before proceeding with the" + 
                    "\nfinalization of this Study.";
            logger.debug("Subject meta data not found: " + subMetaDataNotFound);
        }
    }
    
    // User has clicked on the Proceed button in the dialog; proceed with the 
    // finalization of the Study.
    public String proceedForFinalization() {
        // Remove all the null ojects in the list before proceeding to insert
        // the finalized pipeline output.
        selectedJobs.removeAll(Collections.singleton(null));
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
    
    // Check the database for subject meta data. Construct a string with all
    // those subject ID having no meta data in the database.
    private void checkSubMDAvailability(int jobID, StringBuilder metaDataNotFound) 
            throws SQLException {
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(SubmittedJobDB.getOutputPath(jobID)));
            String subjectLine = br.readLine();
            String[] subjectID = subjectLine.split("\t");
            // Ignore the first 2 strings (i.e. geneID and EntrezID); start at ndex 2.
            for (int i = 2; i < subjectID.length; i++) {
                // If subject meta data is not found in the database.
                if (!SubjectDB.isSubjectExistInDept(subjectID[i], dept_id)) {
                    // Only want to store the unqiue subject ID that doesn't
                    // have meta data in the database.
                    if (!metaDataNotFound.toString().contains(subjectID[i])) {
                        metaDataNotFound.append(subjectID[i]).append(" ");                        
                    }
                }
            }
        }
        catch (IOException ioe) {
            logger.error("Failed to read pipeline output file!");
            logger.error(ioe.getMessage());
        }
    }

    // User has clicked on the Cancel button in the dialog; do not proceed with
    // the finalization of the Study.
    public void cancelFinalization() {
        // Clear the selected jobs list so that it will be build again when 
        // the user click on the Finalize button.
        selectedJobs.clear();
        logger.info(AuthenticationBean.getUserName() +
                ": decided not to proceed with the finalization.");
    }
    
    // Each time a new study is selected, the system need to clear the old 
    // lists and build new one.
    private void clearLists() {
        TIDList.clear();
        jobEntryLists.clear();
        selectedJobs.clear();
    }
    
    // Build new lists based on the study_id selected by user.
    private void buildLists() {
        int index = 0;
        TIDList = SubmittedJobDB.queryTIDUsedInStudy(study_id);
        
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
    public String getTID1() {
        return TIDList.get(1) + " Technology";
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
    public String getTID2() {
        return TIDList.get(2) + " Technology";
    }
    public void setSelectedJob2(FinalizingJobEntry job) {
        selectedJob2 = job;
    }
    public FinalizingJobEntry getSelectedJob2() {
        return selectedJob2;
    }
    
    // Machine generated getters and setters
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public String getSubMDAvailableStatus() {
        return subMDAvailableStatus;
    }
    public void setSubMDAvailableStatus(String subMDAvailableStatus) {
        this.subMDAvailableStatus = subMDAvailableStatus;
    }
}
