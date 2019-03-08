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
import TIMS.Database.DataDepositor;
import TIMS.Database.FinalizingJobEntry;
import TIMS.Database.PipelineDB;
import TIMS.Database.StudyDB;
import TIMS.Database.SubjectRecordDB;
import TIMS.Database.SubmittedJobDB;
import TIMS.General.Constants;
import TIMS.General.ResourceRetriever;
// Libraries for Java
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.omnifaces.cdi.ViewScoped;

@Named("finalizedBean")
@ViewScoped
public class FinalizeStudyBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FinalizeStudyBean.class.getName());
    private String study_id;
    private LinkedHashMap<String, String> studyHash;
    // Store the status of the availability of subject meta data in the database.
    private String subMDAvailableStatus = null;
    private List<String> plList = new ArrayList<>();
    private FinalizingJobEntry selectedJob0, selectedJob1, selectedJob2, 
                               selectedJob3, selectedJob4;
    // Store the list of selected jobs.
    private List<FinalizingJobEntry> selectedJobs = new ArrayList<>();
    private List<List<FinalizingJobEntry>> jobEntryLists = 
            new ArrayList<List<FinalizingJobEntry>>();
    private Boolean allowToProceed;
    // Store the user ID of the current PI.
    private final String userName;
    // GATK pipelines.
    private final List<String> gatk_pipelines = Arrays.asList
        (PipelineDB.GATK_TAR_GERM, PipelineDB.GATK_TAR_SOMA, 
         PipelineDB.GATK_WG_GERM, PipelineDB.GATK_WG_SOMA);
    
    public FinalizeStudyBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
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
        logger.debug("Preparing for Study finalization.");
        // Check whether the user select any of the job.
        if ((selectedJob0==null) && (selectedJob1==null) && 
            (selectedJob2==null) && (selectedJob3==null) && 
            (selectedJob4==null)) {
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
            selectedJobs.add(4, selectedJob4);
        
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
                    // Error when checking for subject record.
                    // Stop the finalization process and go to error page.
                    logger.error("FAIL to check for subject record availability!");
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
                // Check is subject record found in the database.
                if (!SubjectRecordDB.isSRExist(subjectID[i], study_id)) {
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
        plList = SubmittedJobDB.getCompletedPlNameInStudy(study_id);
        // GATK pipelines output are not ready for finalizing yet; remove them.
        plList.removeAll(gatk_pipelines);
        logger.debug("Filter pipeline list: " + plList.toString());
        
        for (String pipeline : plList) {
            jobEntryLists.add(index++, SubmittedJobDB.
                    getCompletedPlJobsInStudy(study_id, pipeline));
        }
    }
    
    // Return the study ID hash map for this PI's group(s).
    public LinkedHashMap<String, String> getStudyHash() {
       return studyHash; 
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
    
    // Do the same for the fifth data table.
    public Boolean getJobList4Status() {
        return jobEntryLists.size()>4;
    }
    public List<FinalizingJobEntry> getJobList4() {
        return jobEntryLists.get(4);
    }
    public String getPl4() {
        return ResourceRetriever.getMsg(plList.get(4));
    }
    public void setSelectedJob4(FinalizingJobEntry job) {
        selectedJob4 = job;
    }
    public FinalizingJobEntry getSelectedJob4() {
        return selectedJob4;
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