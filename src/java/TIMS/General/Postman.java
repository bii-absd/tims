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
package TIMS.General;

import TIMS.Database.SubmittedJobDB;
import TIMS.Database.UserAccount;
import TIMS.Database.UserAccountDB;
// Libraries for Java
import java.util.Properties;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

public abstract class Postman {
    private static final String from = "TIMS@bii.a-star.edu.sg";
    // Sending email from localhost
    private static final String host = "localhost";
    // Get system properties
    private static final Properties properties = System.getProperties();
    private static Session session;
    private static String ipAddress;
    
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(Postman.class.getName());

    // Setup email server.
    private static void setupMailServer() {
        // Setup mail server
        properties.setProperty("mail.smtp.host", host);
        // Get the default session object
        session = Session.getDefaultInstance(properties);
        try {
            // Get the IP address of the current system.
            InetAddress ip = InetAddress.getLocalHost();
            ipAddress = ip.getHostAddress();
        } catch (UnknownHostException ex) {
            logger.error("FAIL to get the IP address of the system.");
            logger.error(ex.getMessage());
            // Set the IP address to 'localhost'.
            ipAddress = "localhost";
        }
    }
    
    // Send a data uploaded status email to notify the admin that the raw data
    // has been uploaded; include the input data path in the email so that the
    // admin could upload those huge raw data files (i.e. >1GB) directly into 
    // the path located in the server.
    public static void sendDataUploadedEmail(String adminID, String studyID, 
            String plName, String inputPath) 
    {
        setupMailServer();
        UserAccount admin = UserAccountDB.getUserAct(adminID);

        try {
            // Create a default MimeMessage object
            MimeMessage message = new MimeMessage(session);
            // Set From: header field
            message.setFrom(new InternetAddress(from));
            // Set To: header field
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(admin.getEmail()));
            message.setSubject("TIMS - Raw data for " + studyID + 
                               " - " + ResourceRetriever.getMsg(plName) + " uploaded.");
            message.setText(
                    "Raw data for " + ResourceRetriever.getMsg(plName) + 
                    " under study " + studyID +
                    " has been successfully uploaded to the following path:\n\n" + inputPath +
                    "\n\n\nTIMS @" + ipAddress);
            // Send the message
            Transport.send(message);
            logger.debug("Data uploaded email sent.");
        }
        catch (MessagingException me) {
            logger.error("FAIL to send data uploaded email!");
            logger.error(me.getMessage());
        }
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
                    + "please help to take a look. Thank you!"
                    + "\n\nTIMS @" + ipAddress);
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
        // Retrieve the pipeline description executed in this job.
        String plDesc = ResourceRetriever.getMsg(SubmittedJobDB.getPipelineName(job_id));
        
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
                message.setSubject(plDesc + " execution for " + 
                                   study_id + " successfully completed.");
                message.setText(
                    "Dear " + user.getFirst_name() + ",\n\n" +
                    plDesc + " execution has completed.\n\n" +
                    "Output and report files are ready for download at Job Status page.\n\n\n" +
                    "Please do not reply to this message." +
                    "\n\n\nTIMS @" + ipAddress);
            }
            else {
                // For failed case, BCC the email to the administrator(s).
                String adminEmails = UserAccountDB.getAdminEmails();
                message.addRecipients(Message.RecipientType.BCC, adminEmails);
                message.setSubject(plDesc + " execution for " + 
                                   study_id + " failed to complete.");
                message.setText(
                    "Dear " + user.getFirst_name() + ",\n\n" +
                    plDesc + " execution failed to complete.\n\n" +
                    "The team is looking at the root cause now.\n\n" +
                    "We will get back to you once we have any finding.\n\n" +
                    "Sorry for the inconvenience caused.\n\n\n" +
                    "Please do not reply to this message." +
                    "\n\n\nTIMS @" + ipAddress);
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

    // Send a meta data upload status email to notify the user of the status.
    public static void sendMetaDataUploadStatusEmail(String study_id, 
            String userName, boolean status) {
        sendTaskStatusEmail(study_id, userName, status, "meta data upload", 
                "\n\nData quality report is ready for download at Meta Data Management page.\n\n");
    }
    // Send a export data status email to notify the user of the status.
    public static void sendExportDataStatusEmail(String study_id, 
            String userName, boolean status)
    {
        sendTaskStatusEmail(study_id, userName, status, "export data", 
                "\n\nPlease login to TIMS to visualize your data.\n\n");
    }
    // Send a finalization status email to notify the user of the status.
    public static void sendFinalizationStatusEmail(String study_id,
            String userName, boolean status)
    {
        sendTaskStatusEmail(study_id, userName, status, "finalization", 
                "\n\nConsolidated output, detail output and finalized summary are ready"
                + "\nfor download at Completed Study Output page.\n\n");
    }
    // Send a unfinalization status email to notify the user of the status.
    public static void sendUnFinalizationStatusEmail(String study_id, 
            String userName, boolean status) 
    {
        sendTaskStatusEmail(study_id, userName, status, "un-finalization", "\n\n\n");
    }
    // Send a closure status email to notify the user of the status.
    public static void sendStudyClosureStatusEmail(String study_id, 
            String userName, boolean status) 
    {
        sendTaskStatusEmail(study_id, userName, status, "closure", "\n\n\n");
    }
    // Helper function to send out the status email for each task.
    public static void sendTaskStatusEmail(String study_id, String userName, 
            boolean status, String task, String content) {
        setupMailServer();
        UserAccount user = UserAccountDB.getUserAct(userName);
        
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.addRecipient(Message.RecipientType.TO, 
                    new InternetAddress(user.getEmail()));
            if (status) {
                msg.setSubject("Study " + study_id + " " + task + " completed successfully.");
                msg.setText(
                    "Dear " + user.getFirst_name() + ",\n\n" +
                    "Study " + study_id + " " + task + " has completed successfully." +
                    content + "Please do not reply to this message." +
                    "\n\n\nTIMS @" + ipAddress);
            }
            else {
                msg.setSubject("Study " + study_id + " " + task + " failed to complete.");
                msg.setText(
                    "Dear " + user.getFirst_name() + ",\n\n" +
                    "Study " + study_id + " " + task + " failed to complete.\n\n\n" +
                    "Please do not reply to this message." +
                    "\n\n\nTIMS @" + ipAddress);
            }
            // Send the message.
            Transport.send(msg);
            logger.debug("Email sent to: " + user.getEmail());
        }
        catch (MessagingException me) {
            logger.error("FAIL to send email to: " + user.getEmail());
            logger.error(me.getMessage());
        }
    }
}
