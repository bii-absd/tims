/*
 * Copyright @2015
 */
package TIMS.General;

// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
/**
 * ProcessExitDetector is used to detect the completion of the process
 * execution. It will invoke ProcessListener method to update the job status
 * once the process has finished.
 * 
 * Author: Tay Wei Hong
 * Date: 12-Oct-2015
 * 
 * Revision History
 * 12-Oct-2015 - Created with one constructor, and two methods (getProcess and
 * run).
 * 02-Nov-2015 - Port to JSF 2.2
 * 05-Nov-2015 - To receive and pass on the study ID of this job.
 */

public class ProcessExitDetector extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ProcessExitDetector.class.getName());    
    // The process which we want to detect the end
    private Process process;
    // The associated listener to be invoked at the end of the process
    private ExitListener listener;
    // The job ID that this process is associated with
    private int jobID;
    // The Study ID of this job
    private String studyID;
    
    public ProcessExitDetector(int jobID, String studyID, Process process, 
            ExitListener listener) {
        try {
            // Test if the process is finished. exitValue will throw
            // IllegalThreadStateException if the process has not yet ended.
            int result = process.exitValue();

            // If exitValue return, this means the process has already ended.
            logger.error("Process already ended before creating detector.");
            // Update the job status according to the process exit value.
            listener.processFinished(jobID, studyID, result);
            // Throw an exception that will propagates beyond the run method
            throw new IllegalArgumentException(
                    "Process already ended before creating detector.");
        } catch (IllegalThreadStateException e) {
            // This mean that the process has not yet complete i.e. we need to
            // detect it's end.
            this.jobID = jobID;
            this.studyID = studyID;
            this.process = process;
            this.listener = listener;
            logger.debug("Process exit detector created.");
        }
    }
    
    // Return the process currently being monitor
    public Process getProcess() {
        return process;
    }
    
    @Override
    public void run() {
        try {
            // Wait for the process to finish
            int result = process.waitFor();
            // Invoke the listener
            logger.debug("Pipeline for job ID " + jobID + 
                    " has completed with exit status: " + result);
            listener.processFinished(jobID, studyID, result);
        }
        catch (InterruptedException e) {
            logger.error("InterruptedException encountered while monitoring "
                    + "the pipeline execution for job ID: " + jobID);
        }
    }
}
