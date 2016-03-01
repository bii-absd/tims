/*
 * Copyright @2016
 */
package Clinical.Data.Sink.General;

import Clinical.Data.Sink.Database.SubmittedJobDB;
import Clinical.Data.Sink.Database.UserAccount;
import Clinical.Data.Sink.Database.UserAccountDB;

import java.util.Properties;
// Libraries for Java Extension
import javax.faces.context.FacesContext;
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
 * Postman is an abstract class and not mean to be instantiate, its main job 
 * is to send job status email and exception email to the users and 
 * administrators.
 *
 * Author: Tay Wei Hong
 * Date: 18-Jan-2016
 * 
 * Revision History
 * 18-Jan-2016 - Initial creation with 3 methods, setupMailServer(), 
 * sendJobStatusEmail and sendExceptionEmail().
 * 01-Mar-2016 - Change in from: email address, due to change in application
 * name.
 */

public abstract class Postman {
    private static final String from = "TIMS@bii.a-star.edu.sg";
    // Sending email from localhost
    private static final String host = "localhost";
    // Get system properties
    private static final Properties properties = System.getProperties();
    private static Session session;
    
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(Postman.class.getName());

    // Setup email server.
    private static void setupMailServer() {
        // Setup mail server
        properties.setProperty("mail.smtp.host", host);
        // Get the default session object
        session = Session.getDefaultInstance(properties);
    }
    
    // Send a exception email to the administrator to ask for help.
    public static void sendExceptionEmail() {
        setupMailServer();
        String userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        String adminEmails = UserAccountDB.getAdminEmails();
        
        try {
            // Create a default MimeMessage object
            MimeMessage message = new MimeMessage(session);
            // Set From: header field
            message.setFrom(new InternetAddress(from));
            // Set To: header field
            message.addRecipients(Message.RecipientType.TO, adminEmails);
            message.setSubject("TIMS - User encountered Error.");
            message.setText(userName + " hit the Error page, "
                    + "please help to take a look. Thank you!");
            // Send the message
            Transport.send(message);
            
            logger.debug("Exception email sent.");
        }
        catch (MessagingException me) {
            logger.error("FAIL to send exception email!");
            logger.error(me.getMessage());
        }
        
    }

    // Send a job status email to notify the user that the pipeline has 
    // completed execution. The success/failure of the execution will be 
    // indicated on the Subject line.
    public static void sendJobStatusEmail(int job_id, String study_id, Boolean status) {
        setupMailServer();
        // Retrieve the user account of the job requestor.
        UserAccount user = UserAccountDB.getJobRequestor(job_id);
        // Retrieve the pipeline executed in this job.
        String plName = SubmittedJobDB.getPipelineName(job_id);
        
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
                // For failed case, BCC the email to the administrator(s).
                String adminEmails = UserAccountDB.getAdminEmails();
                message.addRecipients(Message.RecipientType.BCC, adminEmails);
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
            
            logger.debug("Email sent to: " + user.getEmail());
        }
        catch (MessagingException me) {
            logger.error("FAIL to send email to: " + user.getEmail());
            logger.error(me.getMessage());
        }
    }
}
