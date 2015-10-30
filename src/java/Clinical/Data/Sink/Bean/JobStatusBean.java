/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.JobStatus;
import Clinical.Data.Sink.Database.SubmittedJob;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIData;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * JobStatusBean is the backing bean for the jobstatus.xhtml view.
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
 */

@ManagedBean(name = "jobStatusBean")
@ViewScoped
public class JobStatusBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(JobStatusBean.class.getName());
    // For JSF 2.2, since we are using @ViewScoped we cannot bind any UIComponent.
//    private transient UIData jobStatusTable;
    private List<SubmittedJob> jobSubmission;
    // These 2 variables are used during downloading of output and report
    private File file = null;
    private String type;
    
    public JobStatusBean() {
        logger.info(AuthenticationBean.getUserName() + 
                ": access Job Status page.");
        // Retrieve the job status definition from database
        JobStatus.getJobStatusDef();
        // Setup the default query condition everytime we enter the job status page
        SubmittedJobDB.setQueryOrderBy();
    }

    @PostConstruct
    public void init() {
        // Need to assign the jobSubmission here, else the sorting will not work.
        jobSubmission = SubmittedJobDB.querySubmittedJob
                        (AuthenticationBean.getUserName());
    }
    
    // No longer in use; will be using the sortBy function provided by 
    // PrimeFaces instead.
    public void sort(ActionEvent actionEvent) {
        // Order the query result according to the column selected by the user.
        // For Submission Date, order by job_id will be used.
        SubmittedJobDB.setQueryOrderBy(actionEvent.getComponent().getId());
    }
    
    // Download the pipeline output for user.
    public void downloadOutput() {
        String output = getExternalContext().getRequestParameterMap().get("output");
        download(output);
    }
    
    // Download the pipeline report for user.
    public void downloadReport() {
        String report = getExternalContext().getRequestParameterMap().get("report");
        download(report);
    }
    
    // Setup the variables file and type according to the pipeline output.
    public void preOutput() {
        String output = getExternalContext().getRequestParameterMap().get("output");
        file = new File(output);
        type = getFacesContext().getExternalContext().getMimeType(file.getName());
    }
    
    // Setup the variables file and type according to the pipeline report.
    public void preReport() {
        String report = getExternalContext().getRequestParameterMap().get("report");        
        file = new File(report);
        type = getFacesContext().getExternalContext().getMimeType(file.getName());
    }
    
    // Return the file to be downloaded.
    public StreamedContent getFile() throws FileNotFoundException {
        logger.info(AuthenticationBean.getUserName() + ": downloaded " +
                    file.getName());
        // Temporary hardcored to "text/plain"; should be using variable type
        return new DefaultStreamedContent(new FileInputStream(file),
                                          "text/plain",file.getName());
    }
    
    // download let the user download the output file after the pipeline has
    // completed execution
    public void download(String downloadFile) {
        // Get ready the pipeline file for user to download
        File file = new File(downloadFile);
        String filename = file.getName();
        int contentLength = (int) file.length();
        ExternalContext ec = getFacesContext().getExternalContext();
        // Some JSF component library or some filter might have set some headers
        // in the buffer beforehand. We want to clear them, else they may collide.
        ec.responseReset();
        // Auto-detect the media-types based on filename
        ec.setResponseContentType(ec.getMimeType(filename));
        // Set the file size, so that the download progress will be known.
        ec.setResponseContentLength(contentLength);
        // Create the Sava As popup
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\""
                            + filename + "\"");
        
        try (FileInputStream fis = new FileInputStream(file)){
            OutputStream os = ec.getResponseOutputStream();
            byte[] buffer = new byte[2048]; // 2K byte-buffer
            int bytesRead = 0;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer,0,bytesRead);
            }
        } catch (IOException ex) {
            logger.error(AuthenticationBean.getUserName() + 
                    ": encountered IOException during download.");
            logger.error(ex.getMessage());
        }
        
        // Important! Otherwise JSF will attempt to render the response which
        // will fail since it's already written with a file and closed.
        getFacesContext().responseComplete();
        logger.info(AuthenticationBean.getUserName() + 
                ": downloaded pipeline file " + downloadFile);
    }
    
    // Return the list of SubmittedJob objects that belong to the current user.
    // NOTE: This function will get called multiple times, hence should'nt
    // include any business logic into it (i.e. performance issue).
    public List getJobSubmission() {
        return jobSubmission;
    }

    /* No longer in use, the same function will be provided by SubmittedJob.
    // getAvailable will enable/disable the download link based on the job 
    // status i.e. if job status is "Completed" enable, else disable.
    public String getAvailable() {
        // Get the job selected
        SubmittedJob job = (SubmittedJob) jobStatusTable.getRowData();

        if (status.compareTo("Completed") == 0)
        {
            return "false";
        }
        else {
            return "true";
        }
    }
    */
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    // Retrieve the external context
    private ExternalContext getExternalContext() {
        return getFacesContext().getExternalContext();
    }
    // Retrieve the servlet context
    private ServletContext getServletContext() {
        return (ServletContext) getExternalContext().getContext();
    }

    // Machine generated getters and setters
    /* @ViewScoped will breaks when any UIComponent is bound to the bean
       using binding attribute or when using JSTL tags in the view; the bean
       will behave like a request scoped one.
    public UIData getJobStatusTable() { return jobStatusTable; }
    public void setJobStatusTable(UIData jobStatusTable) {
        this.jobStatusTable = jobStatusTable;
    }
    */
}