/*
 * Copyright @2015-2016
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.DBHelper;
import TIMS.Database.FeatureDB;
import TIMS.Database.GroupDB;
import TIMS.Database.ICD10DB;
import TIMS.Database.InstitutionDB;
import TIMS.Database.JobStatusDB;
import TIMS.Database.UserAccount;
import TIMS.Database.UserAccountDB;
import TIMS.Database.UserRoleDB;
import TIMS.General.Constants;
import java.io.File;
import java.io.Serializable;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
// Libraries for Java Extension
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ManagedBean;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * AuthenticationBean is the backing bean for the login view.
 * 
 * Author: Tay Wei Hong
 * Created on: 18-Sep-2015
 * 
 * Revision History
 * 18-Sep-2015 - Created with all the standard getters and setters.
 * 23-Sep-2015 - Moved the configuration setting outside of application.
 * 25-Sep-2015 - Read in the setup file from the context-param setup in web.xml
 * 28-Sep-2015 - Added the code for authentication in login method. to use
 * general constants for success and failure.
 * 29-Sep-2015 - Added the logout method.
 * 01-Oct-2015 - Changed the return value of the logout method.
 * 06-Oct-2015 - Added comment for the code. Added Log4j2 for this class. 
 * 07-Oct-2015 - Changed to connection based for database access; only one
 * DBHelper will be created for the whole system. Classes that need to access
 * the database will get a connection from the DBHelper class.
 * 09-Oct-2015 - Added 2 new methods (getHeaderInstDept and getHeaderFullName)
 * to support the header.jsp view. Added a new variable userAcct. To use the
 * new class UserAccountDB for checking of user password.
 * 12-Oct-2015 - Added support for super user.
 * 13-Oct-2015 - Added new method getAdminRight, to provide basic access control
 * to command/link.
 * 15-Oct-2015 - Critical error handling.
 * 21-Oct-2015 - To create user directory once successfully login.
 * 22-Oct-2015 - Added one String variable to store the user home directory.
 * 23-Oct-2015 - To create system directory once successfully login.
 * 27-Oct-2015 - Created 2 new functions setupConstants and setupMenuList, to
 * handle the setting up of systems constants and parameters.
 * 02-Nov-2015 - To create all the user system directories once successfully 
 * login.
 * 03-Nov-2015 - Setup will be loaded base on the Operating System the 
 * application is hosted on.
 * 04-Nov-2015 - To update the last login time of the user once he/she 
 * successfully login to the system.
 * 09-Nov-2015 - Added one static method isAdministrator() to check whether is
 * the current user a administrator.
 * 11-Nov-2015 - To add the credential upon user successful login, and to 
 * remove the credential upon user logout. Changed the return type of 
 * setupConstants() and setupMenuList() methods. To have a common exit point
 * for login() method.
 * 16-Nov-2015 - To retrieve institution list from database after login.
 * 01-Dec-2015 - Implementation for database 2.0
 * 02-Dec-2015 - Implemented the changes in the input folder directory.
 * 28-Dec-2015 - Added 2 new methods, isSupervisor() and isClinical().
 * 07-Jan-2016 - Added one new method, getFullName() to be used during 
 * generation of study's summary report.
 * 12-Jan-2016 - Fix the static variable issues in AuthenticationBean.
 * 13-Dec-2016 - Removed all the static variables in Study and ItemList
 * management modules.
 * 14-Jan-2016 - Deleted method setupMenuList. The menu item list will be setup
 * in MenuBean.
 * 15-Jan-2016 - Enhanced the error handling during login.
 * 26-Jan-2016 - Implemented audit data capture module.
 * 29-Jan-2016 - To use a common system setup file for both Windows and Linux OS.
 * 17-Feb-2016 - To alert the user one minute before session timeout, and to 
 * allow the user to extend the session.
 * 18-Feb-2016 - Added 2 new methods, allUsersJobStatus() and 
 * singleUserJobStatus(), to set the single user mode in the session map.
 * 19-Feb-2016 - To support user account with picture uploaded.
 * 23-Feb-2016 - Implementation for database 3.0 (Part 1).
 * 24-Feb-2016 - To direct user to different page based on their role. Fix the 
 * bug where the application crashed because the user's photo has been removed 
 * from the directory.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 01-Mar-2016 - System and user working directories will only be created during
 * system parameters setup and user account creation (instead of during user 
 * login).
 * 09-Mar-2016 - Implementation for database 3.0 (final). User role expanded
 * (Admin - Director - HOD - PI - User). Grouping hierarchy expanded 
 * (Institution - Department - Group).
 * 14-Mar-2016 - Added one new method, isLead() to check whether the user is a
 * Director|HOD|PI.
 * 14-Mar-2016 - Fix the null pointer exception when the system is first setup.
 * 22-Mar-2016 - To build the ICD code HashMap once user login.
 * 08-Apr-2016 - To build the Institution ID HashMap once user login.
 * 21-Jul-2016 - To retrieve and build the feature active status list from 
 * the database. Added 2 new methods, isCBioPortalON() and isVisualizationON().
 */

@ManagedBean (name="authBean")
@SessionScoped
public class AuthenticationBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(AuthenticationBean.class.getName());
    private String loginName, password, instName;
    private UserAccount userAcct;
    private LinkedHashMap<String, Boolean> featureList;
    
    public AuthenticationBean() {
        logger.debug("AuthenticationBean created.");
    }
    
    // Retrieve and setup the feature active status list from the database.
    private void setupFeatureList() {
        featureList = FeatureDB.getAllFeatureStatusHash();
    }
    
    // Setup the database configuration, input and config file path according 
    // to the OS the application is hosted on.
    private Boolean setupConstants(ServletContext context) {
        String setupFile, root;
        String OS = System.getProperty("os.name");
        // Load the system setup file location from context-param.
        setupFile = context.getInitParameter("setup");
        // Setup the application root directory accordingly to the Operating 
        // System the application is hosted on, 
        if (OS.startsWith("Windows")) {
            root = "C:";
        }
        else {
            root = File.separator + "var";
        }

        logger.debug("Application is hosted on: " + OS);
        // Setup the constants using the parameters defined in setup file.
        return Constants.setup(context.getRealPath(setupFile), root);
    }
    
    // Setup the system, check the user login and password against the 
    // database before letting the user use the system.
    public String login()
    {
        // Next page to proceed to
        String result = Constants.LOGIN_PAGE;
        ServletContext context = getServletContext();
        
        // Setting up the database configuration, input, config file path, etc
        if (!setupConstants(context))
        {
            // Constant variables cannot be loaded, shouldn't let the user proceed.
            getFacesContext().addMessage("global", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "System failure. Please contact the administrator.", ""));
            logger.error("FAIL to load/create system parameters!");
            // Return to login page.
            return Constants.LOGIN_PAGE;
        }

        // Initialise the data source for TIMS.
        DBHelper.initDataSource();        
        // Retrieve the job status definition from database
        JobStatusDB.buildJobStatusDef();
        // Build the role list.
        UserRoleDB.getRoleNameHash();
        // Build the ICD10 Code HashMap.
        ICD10DB.buildICDHashMaps();
        // Build the Institution ID HashMap.
        InstitutionDB.buildInstIDHash();
        // Setup the feature active status list.
        setupFeatureList();
        
        // Temporary hack to allow me to enter to create user when the 
        // application is first deployed.
        if ((loginName.compareTo("super")==0) && 
            (password.compareTo("super")==0)) {
            // "super" user is only allowed when the user account is not setup.
            if (!UserAccountDB.isUserAccountSetup()) {
                getFacesContext().getExternalContext().getSessionMap().
                        put("User", "super");
                // "Super" user, no further check required. Proceed from 
                // login to /restricted folder
                return Constants.PAGES_DIR + Constants.MAIN_PAGE;                
            }
        }
        else {
            userAcct = UserAccountDB.checkPwd(loginName, password);
        }
        
        if (userAcct != null) {
            // Check is account enabled.
            if (userAcct.getActive()) {
                // Record login success into database.
                ActivityLogDB.recordUserActivity(loginName, Constants.LOG_IN, 
                                                 "Success");
                logger.info(loginName + ": login as " + userAcct.getRoleName());
                // User home directory where all it's pipeline output will be stored.
                String homeDir = Constants.getSYSTEM_PATH() + 
                                 Constants.getUSERS_PATH() + loginName;
                // Update the last login of this user            
                UserAccountDB.updateLastLogin(loginName, Constants.getDateTime());
                // Save the user ID in the session map.
                getFacesContext().getExternalContext().getSessionMap().
                                    put("User", loginName);
                // Save the institution name where this user belongs to.
                instName = InstitutionDB.getInstName(userAcct.getUnit_id());
                // Proceed from login to the next view depending on user role.
                if ( (userAcct.getRoleName().compareTo("Director") == 0) || 
                     (userAcct.getRoleName().compareTo("HOD") == 0) ) {
                    // For director and HOD, direct them to the studies review page.
                    result = Constants.PAGES_DIR + Constants.STUDIES_REVIEW;
                }
                else {
                    // For other users, direct them to the main page.
                    result =  Constants.PAGES_DIR + Constants.MAIN_PAGE;
                }
            }
            else {
                // Account is disabled, display error message to user.
                getFacesContext().addMessage("global", 
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Your account is disabled. "
                        + "Please check with the administrator.", ""));
                logger.debug(loginName + ": account is disabled.");
                // Account disabled, return to login page.
            }
        }
        else {
            getFacesContext().addMessage("global", new FacesMessage(
                    FacesMessage.SEVERITY_WARN, 
                    "Invalid name or password.", ""));
            // Record login failure into database.
            ActivityLogDB.recordUserActivity(loginName, Constants.LOG_IN, "Fail");
            logger.info(loginName + ": failed to login to the system!");
            // User ID/Password invalid, return to login page.
        }
        
        return result;
    }
    
    // This is a dummy method used to reset the session timer.
    public void extendSession() {
        // Do nothing, just log the event.
        logger.debug(loginName + " extended browser's session." );
    }
    
    // To invalidate the session and remove the user credential after the 
    // user logout.
    public String logout() {
        getFacesContext().getExternalContext().invalidateSession();
        getFacesContext().getExternalContext().getSessionMap().remove("User");
        
        // Record this logout activity into database.
        ActivityLogDB.recordUserActivity(loginName, Constants.LOG_OFF, "");
        logger.info(loginName + ": logout from the system.");
        // User logoff from system, redirect to Login Page.
        return Constants.LOGIN_PAGE + "?faces-redirect=true";
    }
    
    // Return the active status for feature CBIOPORTAL VISUALIZER.
    public boolean isCBioPortalON() {
        return featureList.get("CBIOPORTAL VISUALIZER");
    }
    // Return the active status for feature VISUALIZATION.
    public boolean isVisualizationON() {
        return featureList.get("VISUALIZATION");
    }
    
    // Return true if the user is an Admin else return false. The return value 
    // will be used to control the access to some link.
    public boolean isAdministrator() {
        if (loginName.compareTo("super") == 0) {
            return Constants.OK;
        }
        else {
            return userAcct.getRole_id() == UserRoleDB.admin();            
        }
    }
    
    // Return true if the user is an Admin or Director.
    public boolean isDirector() {
        return isAdministrator() || (userAcct.getRole_id() == UserRoleDB.director());
    }
    // Return true if the user is an Admin or PI.
    public boolean isPI() {
        return isAdministrator() || (userAcct.getRole_id() == UserRoleDB.pi());
    }
    // Return true if the user is an Admin or User.
    public boolean isUser() {
        if (loginName.compareTo("super") == 0) {
            return Constants.OK;
        }
        else {
            return isAdministrator() || (userAcct.getRole_id() == UserRoleDB.user());
        }
    }
    // Return true if the user can be a lead (i.e. Director|HOD|PI).
    public boolean isLead() {
        return (userAcct.getRole_id() == UserRoleDB.director()) ||
               (userAcct.getRole_id() == UserRoleDB.hod()) ||
               (userAcct.getRole_id() == UserRoleDB.pi());
    }
    // Return true if the user is an PI AND is heading a group; to decide 
    // whether to render the Finalize Study link.
    public boolean isPILead() {
        if (loginName.compareTo("super") == 0) {
            return Constants.OK;
        }
        else {
            return isLead() && GroupDB.isPILead(userAcct.getUser_id());
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    // Retrieve the servlet context
    private ServletContext getServletContext() {
        return (ServletContext) FacesContext.getCurrentInstance().
                getExternalContext().getContext();
    }
    
    // Supply the Institution-Unit string to all the views.
    public String getHeaderInstDept() { 
        if (loginName.compareTo("super") == 0) {
            return loginName;
        }
        else {
            return instName + " - " + userAcct.getUnit_id();            
        }
    }
    
    // Supply the Full Name string to all the views.
    public String getHeaderFullName() { 
        if (loginName.compareTo("super") == 0) {
            return loginName;
        }
        else {
            // This method might need to be customised for different 
            // type of users.
            return "Welcome " +  userAcct.getFirst_name();
        }
    }
    
    // Supply the user photo file path.
    public String getUserPhoto() {
        return userAcct.getPhoto();
    }
    
    // Check whether the user has a photo uploaded. The return boolean will be
    // used to decide whether to render the graphic image or not.
    public boolean isUserPhotoAvailable() {
        // Proceed to retrieve the uploaded photo only if this is not the 
        // "super" user and photo has been uploaded before.
        if ((loginName.compareTo("super") != 0) && 
            (userAcct.getPhoto().compareTo("NA") != 0)) {
            // User has a photo uploaded before, make sure the photo still 
            // exists in the system.
            Path photopath = FileSystems.getDefault().getPath(
                            Constants.getSYSTEM_PATH() +
                            Constants.getPIC_PATH() + userAcct.getPhoto());
            
            if (Files.exists(photopath)) {
                return true;
            }
        }
        // If control reaches here, user photo is not available.
        return false;
    }
    
    // Administrator accessing job management page. Set the single user mode to 
    // false.
    public String allUsersJobStatus() {
        // Save the single user mode selection in the session map to be use by
        // job status bean.
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("singleUser", false);
        
        return Constants.JOB_STATUS;
    }
    
    // User accessing his/her work area. Set the single user mode to true.
    public String singleUserJobStatus() {
        // Save the single user mode selection in the session map to be use by
        // job status bean.
        FacesContext.getCurrentInstance().getExternalContext().
                getSessionMap().put("singleUser", true);
        
        return Constants.JOB_STATUS;
    }

    // Machine generated getters and setters
    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
