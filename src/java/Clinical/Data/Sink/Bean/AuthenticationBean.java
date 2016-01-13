/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.DBHelper;
import Clinical.Data.Sink.Database.InstitutionDB;
import Clinical.Data.Sink.Database.JobStatusDB;
import Clinical.Data.Sink.Database.UserAccount;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.General.SelectOneMenuList;
import Clinical.Data.Sink.General.Constants;

import java.io.Serializable;
import java.sql.SQLException;
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
 */

@ManagedBean (name="authenticationBean")
@SessionScoped
public class AuthenticationBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(AuthenticationBean.class.getName());
    private static DBHelper dbHandle;
    private String loginName, password;
    private UserAccount userAcct;
    private String instName;
    
    public AuthenticationBean() {
        logger.debug("AuthenticationBean created.");
    }
    
    // Setup the database configuration, input and config file path according 
    // to the OS the application is hosted on.
    private Boolean setupConstants(ServletContext context) {
        String setupFile;
        String OS = System.getProperty("os.name");
        
        // Load the setup file from context-param according to the Operating
        // System the application is hosted on.
        if (OS.startsWith("Windows")) {
            setupFile = context.getInitParameter("windows");
        }
        else {
            setupFile = context.getInitParameter("linux");            
        }

        logger.debug("Application is hosted on: " + OS);
//        logger.debug("Config file located at: " + context.getRealPath(setupFile));
        
        // Setup the constants using the parameters defined in setup
        return Constants.setup(context.getRealPath(setupFile));
    }
    
    // Setup all the menu list found in the system.
    private Boolean setupMenuList(ServletContext context) {
        // Load the itemlist filename from context-param
        String itemListFile = context.getInitParameter("itemlist");
//        logger.debug("Item list file located at: " + context.getRealPath(itemListFile));
        
        // Setup the menu list using the items defined in item list config
        return SelectOneMenuList.setup(context.getRealPath(itemListFile));
    }
    
    // Setup the system, check the user login and password against the 
    // database before letting the user use the system.
    public String login()
    {
        // Setting up the database configuration, input, config file path, etc
        ServletContext context = getServletContext();

        if (!(setupConstants(context) && setupMenuList(context)) )
        {
            // System having issue, shouldn't let the user proceed.
            return Constants.ERROR;
        }
        
        try {
            // Setup the database handler
            dbHandle = new DBHelper();
        }
        // Shouldn't let the user proceed if the DBHelper cannot be created.
        catch (ClassNotFoundException|InstantiationException|
               SQLException|IllegalAccessException e) {
            logger.error("FAIL to create DBHelper!");
            logger.error(e.getMessage());
            
            return Constants.ERROR;
        }
        
        // Retrieve the job status definition from database
        JobStatusDB.getJobStatusDef();
        
        // Temporary hack to allow me to enter to create user when the 
        // application is first deployed.
        if ((loginName.compareTo("super")==0) && 
            (password.compareTo("super")==0)) {
            getFacesContext().getExternalContext().getSessionMap().
                    put("User", "super");
            // "Super" user, no further check required. Proceed from 
            // login to /restricted folder
            return Constants.PAGES_DIR + Constants.MAIN_PAGE;
        }
        
        userAcct = UserAccountDB.checkPwd(loginName, password);
        // Next page to proceed to
        String result = Constants.LOGIN_PAGE;
        
        if (userAcct != null) {
            // Check is account enabled.
            if (userAcct.getActive()) {
                logger.info(loginName + ": login to the system.");
                // Create user home directory once successfully login
                String homeDir = Constants.getSYSTEM_PATH() + 
                          Constants.getUSERS_PATH() + loginName;
                // Update the last login of this user            
                UserAccountDB.updateLastLogin(loginName, Constants.getDateTime());
            
                // Create system directories, follow by .../users/loginName directories
                if ( FileUploadBean.createSystemDirectories(Constants.getSYSTEM_PATH()) &&
                    (FileUploadBean.createUsersDirectories(homeDir))) {
                    // Save the user ID in the session map.
                    getFacesContext().getExternalContext().getSessionMap().
                                put("User", loginName);
                    // Save the institution name where this user belongs to.
                    instName = InstitutionDB.getInstName(userAcct.getInst_id());
                    // Everything is fine, proceed from login to /restricted folder
                    result =  Constants.PAGES_DIR + Constants.MAIN_PAGE;
                }
                else {
                    logger.error(loginName + ": failed to create system dirs after login!");
                    // If control reached here, it means some of the system directories
                    // is not created, shouldn't allow user to proceed.
                    result = Constants.PAGES_DIR + Constants.ERROR;                    
                }
            }
            else {
                // Account is disabled, display error message to user.
                getFacesContext().addMessage("global", 
                        new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Your account is disabled. Please check with the administrator.", ""));
                logger.info(loginName + ": account is disabled.");
                // Account disabled, return to login page.
            }
        }
        else {
            FacesContext facesContext = getFacesContext();
            
            facesContext.addMessage("global", new FacesMessage(
                    FacesMessage.SEVERITY_WARN, 
                    "Invalid name or password.", ""));
            logger.info(loginName + ": failed to login to the system!");
            // User ID/Password invalid, return to login page.
        }
        
        return result;
    }
    
    // To invalidate the session and remove the user credential after the 
    // user logout.
    public String logout() {
        getFacesContext().getExternalContext().invalidateSession();
        getFacesContext().getExternalContext().getSessionMap().remove("User");
        
        logger.info(loginName + ": logout from the system.");
        // User logoff from system, redirect to Login Page.
        return Constants.LOGIN_PAGE + "?faces-redirect=true";
    }
    
    // Return true if the role ID of the user is 1 (i.e. Admin), else it will 
    // return false. The return value will be used to control the access to 
    // some control/link.
    public Boolean getAdminRight() {
        if (loginName.compareTo("super") == 0) {
            return Constants.OK;
        }
        else {
            // For now, use a very basic way to control the access to user account
            // access. Role ID 1 is Admin; only Admin is allowed access.        
            return userAcct.getRole_id() == 1;            
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
    
    // Supply the Institution-Department string to all the views.
    public String getHeaderInstDept() { 
        if (loginName.compareTo("super") == 0) {
            return loginName;
        }
        else {
            return instName + " - " + userAcct.getDept_id();            
        }
    }
    
    // Supply the Full Name string to all the views.
    public String getHeaderFullName() { 
        if (loginName.compareTo("super") == 0) {
            return loginName;
        }
        else {
            // Most likey this method need to be customised for different 
            // type of users.
            return "Welcome " +  userAcct.getFirst_name();
        }
    }
    
    // Machine generated getters and setters
    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    /* NOT IN USE
    // Update log4j2 log filename
    // Use together with:
    // <RollingRandomAccessFile name="RollingRandomAccessFile" fileName="${sys:logFilename}" filePattern="${sys:logFilename}-%d{MMM-yyyy}-%i.gz">
    // NOT IN USE.
    private void updateLog4jConfiguration() {
        System.setProperty("logFilename", "C:/temp/datasink/log/log");
        
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
    }
    */
}
