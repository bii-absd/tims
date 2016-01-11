/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.General;

import java.util.EventListener;
import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.Database.UserAccount;
import Clinical.Data.Sink.Database.UserAccountDB;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
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
            sendEmail(job_id, study_id, Constants.OK);
        }
        else {
            SubmittedJobDB.updateJobStatusToFailed(job_id);
            logger.debug("Job status updated to failed. ID: " + job_id);
            sendEmail(job_id, study_id, Constants.NOT_OK);
        }
    }
    
    // Send a email to notify the user that the pipeline has completed 
    // execution. The success/failure of the execution will be indicated on 
    // the Subject line.
    private void sendEmail(int job_id, String study_id, Boolean status) {
        // Retrieve the user account of the job requestor.
        UserAccount user = UserAccountDB.getUser(job_id);
        // Retrieve the pipeline executed in this job.
        String plName = SubmittedJobDB.getPipelineName(job_id);
        String from = "datasink@bii.a-star.edu.sg";
        // Sending email from localhost
        String host = "localhost";
        // Get system properties
        Properties properties = System.getProperties();
        // Setup mail server
        properties.setProperty("mail.smtp.host", host);
        // Get the default session object
        Session session = Session.getDefaultInstance(properties);
        
        try {
            // Create a default MimeMessage object
            MimeMessage message = new MimeMessage(session);
            // Set From: header field
            message.setFrom(new InternetAddress(from));
            // Set To: header field
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(user.getEmail()));
            if (status) {
                // Set the Subject and message content according to execution 
                // return status.
                message.setSubject("Pipeline " + plName + " execution for " + 
                                   study_id + " successfully completed.");
                message.setText(
                    "Dear " + user.getFirst_name() + ",\n\n" +
                    "Pipeline " + plName + " execution has completed.\n\n" +
                    "Output and report files are ready for download at Job Status page.\n\n\n" +
                    "Please do not reply to this message.");
            }
            else {
                // For failed case, CC the email to the support team also.
                // Temporarily hardcoded the support email to me
                message.addRecipient(Message.RecipientType.CC, 
                    new InternetAddress("taywh@bii.a-star.edu.sg"));
                message.setSubject("Pipeline " + plName + " execution for " + 
                                   study_id + " failed to complete.");
                message.setText(
                    "Dear " + user.getFirst_name() + ",\n\n" +
                    "Pipeline " + plName + " execution failed to complete.\n\n" +
                    "The team is looking at the root cause now.\n\n" +
                    "We will get back to you once we have any finding.\n\n" +
                    "Sorry for the inconvenience caused.\n\n\n" +
                    "Please do not reply to this message.");                
            }
            // Send the message
            Transport.send(message);
            
            logger.debug("Email successfully sent to: " + user.getEmail());
        }
        catch (MessagingException me) {
            logger.error("MessagingException encountered while trying to send "
                    + "email to: " + user.getEmail());
            logger.error(me.getMessage());
        }
    }
}
