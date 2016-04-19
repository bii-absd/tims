/*
 * Copyright @2015-2016
 */
package TIMS.General;

import TIMS.Database.SubmittedJobDB;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EventListener;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ProcessListener is used to update the job status once the pipeline has 
 * completed it's execution.
 * 
 * Author: Tay Wei Hong
 * Date: 12-Oct-2015
 * 
 * Revision History
 * 12-Oct-2015 - Created with one method processFinished.
 * 13-Oct-2015 - Added new method sendEmail that send a email to notify the 
 * user of the completion of pipeline execution.
 * 14-Oct-2015 - For failed case, CC the email to the support team for 
 * investigation.
 * 02-Nov-2015 - Port to JSF 2.2
 * 05-Nov-2015 - To receive the study ID of this job, and to retrieve the user
 * account of the job requestor. Personalize the email sent to the user.
 * 11-Jan-2015 - To include the pipeline name in the subject title of the email.
 * 13-Jan-2016 - Removed all the static variables in Account Management module.
 * 18-Jan-2016 - Moved the sendMail method to Postman class.
 * 24-Mar-2016 - To record into database, the pipeline execution completion time.
 * 14-Apr-2016 - Changes due to the type change (i.e. to Timestamp) for 
 * submit_time and complete_time in submitted_job table.
 */

public class ExitListener implements EventListener {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ExitListener.class.getName());    

    public ExitListener() {}
    
    // The pipeline has completed execution, update the job status according
    // to the process return status.
    public void processFinished(int job_id, String study_id, int result) {
        if (result == 0) {
            SubmittedJobDB.updateJobStatusToCompleted(job_id);
            logger.debug("Job status updated to completed. ID: " + job_id);
            Postman.sendJobStatusEmail(job_id, study_id, Constants.OK);
        }
        else {
            SubmittedJobDB.updateJobStatusToFailed(job_id);
            logger.debug("Job status updated to failed. ID: " + job_id);
            Postman.sendJobStatusEmail(job_id, study_id, Constants.NOT_OK);
        }
        // Record into database, the completion time of this job.
        Date now = new Date();
        Timestamp complete_time = new Timestamp(now.getTime());
        SubmittedJobDB.updateJobCompleteTime(job_id, complete_time);
    }
}
