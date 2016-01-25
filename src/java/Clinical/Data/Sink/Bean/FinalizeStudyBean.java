/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.DataDepositor;
import Clinical.Data.Sink.Database.FinalizingJobEntry;
import Clinical.Data.Sink.Database.StudyDB;
import Clinical.Data.Sink.Database.SubjectDB;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.General.ResourceRetriever;
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
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
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
 * 30-Dec-2015 - Added one new attribute allowToProceed, which set the condition
 * for allowing the user to proceed with the finalization or not. Improved
 * method prepareForFinalization; to check whether the user has selected at
 * least one job for finalization.
 * 05-Jan-2016 - Changes due to the change in method 
 * StudyDB.updateStudyCompletedStatus().
 * 08-Jan-2016 - To setup the Astar and Bii logo before starting the 
 * DataDepositor thread.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Jan-2016 - Removed all the static variables in Job Status module.
 * 20-Jan-2016 - Updated study table in database; added one new variable closed, 
 * and renamed completed to finalized.
 * 22-Jan-2016 - Study finalization logic change; finalization will be 
 * performed for each pipeline instead of each technology. Added one more
 * data table to support the 4th pipeline.
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
    private List<String> plList = new ArrayList<>();
    private FinalizingJobEntry selectedJob0, selectedJob1, selectedJob2, 
                               selectedJob3;
    // Store the list of selected jobs.
    private List<FinalizingJobEntry> selectedJobs = new ArrayList<>();
    private List<List<FinalizingJobEntry>> jobEntryLists = 
            new ArrayList<List<FinalizingJobEntry>>();
    private Boolean allowToProceed;
    // Store the user ID of the current user.
    private final String userName;
        
    public FinalizeStudyBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        dept_id = UserAccountDB.getDeptID(userName);
        logger.debug("FinalizeStudyBean created.");
        logger.info(userName + ": access Finalize Study page.");
    }
    
    @PostConstruct
    public void init() {
        // Get the list of study from the user's department that has 
        // completed job(s).
        studyHash = StudyDB.getFinalizableStudyHash(userName);
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
        FacesContext fc = FacesContext.getCurrentInstance();
        allowToProceed = true;
        logger.info("Preparing for Study finalization.");
        // Check whether the user select any of the job.
        if ((selectedJob0==null) && (selectedJob1==null) && (selectedJob2==null)) {
            // None of the job has been selected, display error message and
            // return to the same page.
            allowToProceed = false;
            subMDAvailableStatus = 
                    "No job has been selected for finalization.\n" +
                    "\nPlease select the job(s) to be finalize before proceeding.\n";
            logger.debug("No job has been selected for finalization.");
        }
        else {
            // At least one job has been selected, continue.
            // String to store the subject ID that doesn't have meta data.
            StringBuilder subMetaDataNotFound = new StringBuilder();
            selectedJobs.add(0, selectedJob0);
            selectedJobs.add(1, selectedJob1);
            selectedJobs.add(2, selectedJob2);
            selectedJobs.add(3, selectedJob3);
        
            // Check for subject meta data availability, and prepare the 
            // status update string.
            for (FinalizingJobEntry job : selectedJobs) {
                try {
                    if (job != null) {
                        checkSubMDAvailability(job.getJob_id(), 
                                subMetaDataNotFound);                    
                    }
                }
                catch (SQLException|IOException e) {
                    // Error when checking for subject meta data. 
                    // Stop the finalization process and go to error page.
                    logger.error("FAIL to check for subject meta data availability!");
                    logger.error(e.getMessage());
                    return Constants.ERROR;
                }
            }

            if (subMetaDataNotFound.toString().isEmpty()) {
                subMDAvailableStatus = 
                    "All the subject's meta data are found in the database.\n" +
                    "\nPlease proceed with the finalization of this Study.\n";
                logger.debug("All the subject meta data is found.");
            }
            else {
                subMDAvailableStatus = 
                    "The following subject's meta data are not found in the database: " +
                    subMetaDataNotFound +
                    "\n\nPlease upload the subject's meta data before proceeding" + 
                    "\nwith the finalization of this Study.\n";
                logger.debug("Subject meta data not found: " + subMetaDataNotFound);
            }
        }
        return null;
    }
    
    // User has clicked on the Proceed button in the dialog; proceed with the 
    // finalization of the Study.
    public String proceedForFinalization() {
        // Remove all the null ojects in the list before proceeding to insert
        // the finalized pipeline output.
        selectedJobs.removeAll(Collections.singleton(null));
        logger.info(userName + " begin finalization process for " 
                    + study_id + ".");
        // Update job status to finalizing
        for (FinalizingJobEntry job : selectedJobs) {
            SubmittedJobDB.updateJobStatusToFinalizing(job.getJob_id());
        }
        // Setup the filepath of the Astar and Bii logo.
        DataDepositor.setupLogo(
                getServletContext().getRealPath("/resources/images/Astar.jpg"), 
                getServletContext().getRealPath("/resources/images/BII.jpg"));
        // Start a new thread to insert the finalized pipeline output into 
        // database.
        DataDepositor depositThread = new DataDepositor(userName, study_id, selectedJobs);
        depositThread.start();
        // Update study to finalized.
        StudyDB.updateStudyFinalizedStatus(study_id, true);
        
        return Constants.MAIN_PAGE;        
    }
    
    // Check the database for subject meta data. Construct a string with all
    // those subject ID having no meta data in the database.
    private void checkSubMDAvailability(int jobID, StringBuilder metaDataNotFound) 
            throws SQLException, IOException {
        BufferedReader br = new BufferedReader(new FileReader
                            (SubmittedJobDB.getOutputPath(jobID)));
        String subjectLine = br.readLine();
        String[] subjectID = subjectLine.split("\t");
        // Ignore the first 2 strings (i.e. geneID and EntrezID); start at ndex 2.
        for (int i = 2; i < subjectID.length; i++) {
            // Check is subject meta data found in the database.
            if (!SubjectDB.isSubjectExistInDept(subjectID[i], dept_id)) {
                // Only want to store the unqiue subject ID that doesn't
                // have meta data in the database.
                if (!metaDataNotFound.toString().contains(subjectID[i])) {
                    metaDataNotFound.append(subjectID[i]).append(" ");                        
                }
            }
        }
    }

    // User has clicked on the Cancel button in the dialog; do not proceed with
    // the finalization of the Study.
    public void cancelFinalization() {
        // Clear the selected jobs list so that it will be build again when 
        // the user click on the Finalize button.
        selectedJobs.clear();
        logger.info(userName + ": decided not to proceed with the finalization.");
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
        plList = SubmittedJobDB.getPipelineExeInStudy(study_id);
        
        for (String pipeline : plList) {
            jobEntryLists.add(index++, SubmittedJobDB.
                    getCompletedPlJobsInStudy(study_id, pipeline));
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
    // Return the pipeline description; to be use as data table header title.
    public String getPl0() {
        return ResourceRetriever.getMsg(plList.get(0));
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
        return ResourceRetriever.getMsg(plList.get(1));
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
        return ResourceRetriever.getMsg(plList.get(2));
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
        return ResourceRetriever.getMsg(plList.get(3));
    }
    public void setSelectedJob3(FinalizingJobEntry job) {
        selectedJob3 = job;
    }
    public FinalizingJobEntry getSelectedJob3() {
        return selectedJob3;
    }
    
    // Retrieve the servlet context
    private ServletContext getServletContext() {
        return (ServletContext) FacesContext.getCurrentInstance().
                getExternalContext().getContext();
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
    public Boolean getAllowToProceed() {
        return allowToProceed;
    }
}
