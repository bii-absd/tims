/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.ActivityLogDB;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.General.FileLoader;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * JobStatusBean is the backing bean for the jobstatus view.
 * 
 * Author: Tay Wei Hong
 * Date: 02-Oct-2015
 * 
 * Revision History
 * 02-Oct-2015 - Created with all the standard getters and setters. Added
 * getJobSubmissions method, and comments for the code.
 * 05-Oct-2015 - Allow user to download the output file. Allow user to sort the
 * job submission table by Study ID or Submission Date.
 * 06-Oct-2015 - Setup the query order by for each entry. Added Log4j2 for this
 * class.
 * 12-Oct-2015 - Added method getAvailable that enable/disable the download
 * link based on the job status. Log the exception message.
 * 23-Oct-2015 - Pipeline report module.
 * 30-Oct-2015 - Ported from JSF1.1 to JSF2.2 + PrimeFaces. Added PostConstruct
 * function init(), and 3 new functions to handle pipeline output/report
 * downloading for the users.
 * 02-Nov-2015 - Passing in the current submittedJob as method argument for 
 * downloadOuptut and downloadReport methods.
 * 25-Nov-2015 - Comment out unused code. Implementation for database 2.0
 * 23-Dec-2015 - Moved the retrieval of the job status definition to the
 * AuthenticationBean class.
 * 06-Jan-2016 - Moved the common function download() to FileLoader class.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Jan-2016 - Removed all the static variables in Job Status module.
 * 21-Jan-2016 - To allow the users to view the pipeline parameters setup at
 * the job status page through a context menu.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 01-Feb-2016 - When retrieving submitted jobs, there are now 2 options 
 * available i.e. to retrieve for single user or all users (enable for 
 * administrator only).
 */

@ManagedBean(name = "jsBean")
@ViewScoped
public class JobStatusBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(JobStatusBean.class.getName());
    // For JSF 2.2, since we are using @ViewScoped we cannot bind any UIComponent.
//    private transient UIData jobStatusTable;
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
        logger.info(userName + ": access Job Status page.");
        // Reset the single user mode to false.
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("singleUser", false);
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
//        String output = getExternalContext().getRequestParameterMap().get("output");
        String detail = "Output " + job.getOutput_file();
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, detail);
        FileLoader.download(job.getOutput_file());
    }
    
    // Download the pipeline report for user.
    public void downloadReport(SubmittedJob job) {
        String detail = "Report " + job.getReport();
        ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, detail);
        FileLoader.download(job.getReport());
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
    
    /* No longer in use.
    // Return the file to be downloaded.
    public StreamedContent getFile() throws FileNotFoundException {
        // Temporary hardcored to "text/plain"; should be using variable type
        return new DefaultStreamedContent(new FileInputStream(file),
                                          "text/plain",file.getName());
    }
    */    
}