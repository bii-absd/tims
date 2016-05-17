/*
 * Copyright @2015-2016
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.DataDepositor;
import TIMS.Database.FinalizingJobEntry;
import TIMS.Database.StudyDB;
import TIMS.Database.StudySubjectDB;
import TIMS.Database.SubmittedJobDB;
import TIMS.General.Constants;
import TIMS.General.ResourceRetriever;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
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
 * 26-Jan-2016 - Implemented audit data capture module.
 * 27-Jan-2016 - Bug fixes: To handle the case whereby the output file is empty,
 * and the case whereby none of the subject meta data is available in the 
 * database.
 * 26-Feb-2016 - Bug fix: When preparing for finalization, need to check whether
 * job3 is selected too.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 04-Apr-2016 - When checking for subject Meta data availability, the system
 * will now check against the new study_subject table.
 * 13-May-2016 - Minor changes as the pipeline output file will now be zipped.
 */

@ManagedBean (name="finalizedBean")
@ViewScoped
public class FinalizeStudyBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FinalizeStudyBean.class.getName());
    private String study_id, grp_id;
    private LinkedHashMap<String, String> studyHash;
    // Store the status of the availability of subject meta data in the database.
    private String subMDAvailableStatus = null;
    private List<String> plList = new ArrayList<>();
    private FinalizingJobEntry selectedJob0, selectedJob1, selectedJob2, 
                               selectedJob3;
    // Store the list of selected jobs.
    private List<FinalizingJobEntry> selectedJobs = new ArrayList<>();
    private List<List<FinalizingJobEntry>> jobEntryLists = 
            new ArrayList<List<FinalizingJobEntry>>();
    private Boolean allowToProceed;
    // Store the user ID of the current PI.
    private final String userName;
        
    public FinalizeStudyBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        logger.debug("FinalizeStudyBean created.");
        logger.info(userName + ": access Finalize Study page.");
    }
    
    @PostConstruct
    public void init() {
        // Get the list of study (with completed jobs) from the group(s) that 
        // this PI is heading .
        studyHash = StudyDB.getFinalizableStudyHash(userName);
    }
    
    // A new study has been selected by the PI, need to build the job entries
    // lists.
    public void studyChange() {
        // Clear the lists before building them.
        clearLists();
        buildLists();
    }

    // PI has clicked on the Finalize button, need to prepare for the dialog
    // to display to PI; to seek for final confirmation to proceed.
    public String prepareForFinalization() {
        FacesContext fc = FacesContext.getCurrentInstance();
        allowToProceed = true;
        // Setup the group ID based on the Study's grp_id.
        grp_id = StudyDB.getStudyGrpID(study_id);
        logger.debug("Preparing for Study finalization.");
        // Check whether the user select any of the job.
        if ((selectedJob0==null) && (selectedJob1==null) && 
            (selectedJob2==null) && (selectedJob3==null)) {
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
            // String to store the subject ID that have and doesn't have meta data.
            StringBuilder subMetaDataNotFound = new StringBuilder();
            StringBuilder subMetaDataFound = new StringBuilder();
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
                                subMetaDataNotFound, subMetaDataFound);                    
                    }
                }
                catch (SQLException|IOException|NamingException e) {
                    // Error when checking for subject meta data. 
                    // Stop the finalization process and go to error page.
                    logger.error("FAIL to check for subject meta data availability!");
                    logger.error(e.getMessage());
                    return Constants.ERROR;
                }
            }

            // Need to handle the case whereby none of the subject meta data is
            // available.
            if (subMetaDataFound.toString().isEmpty()) {
                // None of the subject meta data is available, display error 
                // message and return to the same page.
                allowToProceed = false;
                subMDAvailableStatus = 
                    "None of the subject meta data is found in this study.\n" +
                    "\nFinalization will not proceed.\n";
                logger.debug("None of the subject meta data is found in this study.");
            }
            else {
                if (subMetaDataNotFound.toString().isEmpty()) {
                    subMDAvailableStatus = 
                        "All the subject's meta data are found in this study.\n" +
                        "\nPlease proceed with the finalization of this Study.\n";
                    logger.debug("All the subject meta data is found.");
                }
                else {
                    subMDAvailableStatus = 
                        "The following subject's meta data are not found in this study: " +
                        subMetaDataNotFound +
                        "\n\nPlease upload the subject's meta data to this study" + 
                        "\nbefore proceeding with the finalization of this Study.\n";
                    logger.debug("Subject meta data not found: " + subMetaDataNotFound);
                }
            }
        }
        
        return null;
    }
    
    // PI has clicked on the Proceed button in the dialog; proceed with the 
    // finalization of the Study.
    public String proceedForFinalization() {
        String nextpage = Constants.MAIN_PAGE;
        // Remove all the null ojects in the list before proceeding to insert
        // the finalized pipeline output.
        selectedJobs.removeAll(Collections.singleton(null));
        // Record this finalization of study into database.
        ActivityLogDB.recordUserActivity(userName, Constants.EXE_FIN, study_id);
        
        try {
            // Start a new thread to insert the finalized pipeline output into 
            // database.
            DataDepositor depositThread = new DataDepositor
                (userName, study_id, selectedJobs);
            // Update job status to finalizing
            for (FinalizingJobEntry job : selectedJobs) {
                SubmittedJobDB.updateJobStatusToFinalizing(job.getJob_id());
            }
            // Setup the filepath of the Astar and Bii logo.
            DataDepositor.setupLogo(
                    getServletContext().getRealPath("/resources/images/Astar.jpg"), 
                    getServletContext().getRealPath("/resources/images/BII.jpg"));
            // Start the finalization thread.
            depositThread.start();
            // Update study to finalized.
            StudyDB.updateStudyFinalizedStatus(study_id, true);
            logger.info(userName + " begin finalization process for " 
                        + study_id + ".");
        }
        catch (SQLException|NamingException e) {
            nextpage = Constants.ERROR;
            logger.error("FAIL to process with the finalization process!");
            logger.error(e.getMessage());
        }
        
        return nextpage;
    }
    
    // Check the database for subject meta data. Construct a string with all
    // those subject ID having no meta data in the database.
    private void checkSubMDAvailability(int jobID, 
            StringBuilder metaDataNotFound, StringBuilder metaDataFound) 
            throws SQLException, IOException, NamingException {
        BufferedReader br = new BufferedReader(new FileReader
                            (SubmittedJobDB.unzipOutputFile(jobID)));
        String subjectLine = br.readLine();
        
        // To handle the case whereby the output file is empty. Throw an
        // IOException if the input file is empty.
        if (subjectLine != null) {
            String[] subjectID = subjectLine.split("\t");
            // Ignore the first 2 strings (i.e. geneID and EntrezID); start at ndex 2.
            for (int i = 2; i < subjectID.length; i++) {
                // Check is subject meta data found in the database.
                if (!StudySubjectDB.isSSExist(subjectID[i], grp_id, study_id)) {
                    // Only want to store the unqiue subject ID that doesn't
                    // have meta data in the database.
                    if (!metaDataNotFound.toString().contains(subjectID[i])) {
                        metaDataNotFound.append(subjectID[i]).append(" ");                        
                    }
                }
                else {
                    if (!metaDataFound.toString().contains(subjectID[i])) {
                        metaDataFound.append(subjectID[i]).append(" ");                        
                    }                    
                }
            }
        }
        else {
            throw new IOException("The output file is empty!");
        }
    }

    // PI has clicked on the Cancel button in the dialog; do not proceed with
    // the finalization of the Study.
    public void cancelFinalization() {
        // Clear the selected jobs list so that it will be build again when 
        // the PI click on the Finalize button.
        selectedJobs.clear();
        logger.info(userName + ": decided not to proceed with the finalization.");
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
        plList = SubmittedJobDB.getPipelineExeInStudy(study_id);
        
        for (String pipeline : plList) {
            jobEntryLists.add(index++, SubmittedJobDB.
                    getCompletedPlJobsInStudy(study_id, pipeline));
        }
    }
    
    // Return the study ID hash map for this PI's group(s).
    public LinkedHashMap<String, String> getStudyHash() {
       return studyHash; 
    }
    
    // Proceed to select job(s) for finalization after a study has been selected.
    public Boolean getStudySelectedStatus() {
        return study_id != null;
    }
    
    // If the PI is able to select any Study ID, there is minimum one pipeline
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