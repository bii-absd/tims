/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.DBHelper;
import Clinical.Data.Sink.Database.UserAccount;
import Clinical.Data.Sink.Database.UserAccountDB;
import Clinical.Data.Sink.General.SelectOneMenuList;
import Clinical.Data.Sink.General.Constants;

import java.io.Serializable;
import java.sql.SQLException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ManagedBean;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

/**
 * AuthenticationBean is the backing bean for the login.xhtml view.
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
 */

@ManagedBean (name="authenticationBean")
@SessionScoped
public class AuthenticationBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(AuthenticationBean.class.getName());
    private static DBHelper dbHandle;
    private static String loginName, homeDir;
    private String password;
    private static UserAccount userAcct;
    
    public AuthenticationBean() {
        logger.debug("AuthenticationBean created.");
    }
    
    // Update log4j2 log filename
    // Use together with:
    // <RollingRandomAccessFile name="RollingRandomAccessFile" fileName="${sys:logFilename}" filePattern="${sys:logFilename}-%d{MMM-yyyy}-%i.gz">
    // NOT IN USE.
    private void updateLog4jConfiguration() {
        System.setProperty("logFilename", "C:/temp/datasink/log/log");
        
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
    }
    
    // setupConstants help to setup the database configuration, input and
    // config file path according to the OS the application is hosted on.
    private String setupConstants(ServletContext context) {
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
        logger.debug("Config file located at: " + 
                     context.getRealPath(setupFile));
        
        // Setup the constants using the parameters defined in setup
        return Constants.setup(context.getRealPath(setupFile));
    }
    
    // setupMenuList help to setup all the menu list found in the system.
    private String setupMenuList(ServletContext context) {
        // Load the itemlist filename from context-param
        String itemListFile = context.getInitParameter("itemlist");
        logger.debug("Item list file located at: " + 
                     context.getRealPath(itemListFile));
        
        // Setup the menu list using the items defined in item list config
        return SelectOneMenuList.setup(context.getRealPath(itemListFile));
    }
    
    // login will setup the system, check the user login and password against 
    // the database before letting the user use the system.
    public String login()
    {
        // Setting up the database configuration, input, config file path, etc
        ServletContext context = getServletContext();

        if ( (setupConstants(context).compareTo(Constants.ERROR) == 0) ||
             (setupMenuList(context).compareTo(Constants.ERROR) == 0) )
        {
            // System having issue, shouldn't let the user proceed.
            return Constants.ERROR;
        }
        
        try {
            // Setup the database handler
            dbHandle = new DBHelper();
        }
        // Shouldn't let the user proceed if the DBHelper cannot be created.
        catch (ClassNotFoundException e) {
            logger.error("Postgres driver not found.");
            logger.error(e.getMessage());
            
            return Constants.ERROR;
        }
        catch (InstantiationException e) {
            logger.error("InstantiationException while creating DBhelper.");
            logger.error(e.getMessage());
            
            return Constants.ERROR;
        }
        catch (SQLException e) {
            logger.error("SQLException while creating DBHelper.");
            logger.error(e.getMessage());
            
            return Constants.ERROR;
        }
        catch (IllegalAccessException e) {
            logger.error("IllegalAccessException while creating DBHelper.");
            logger.error(e.getMessage());
            
            return Constants.ERROR;
        }
        
        // Temporary hack to allow me to enter to create user when the 
        // application is first deployed.
        if ((loginName.compareTo("super")==0) && 
            (password.compareTo("super")==0)) {
            return Constants.MAIN_PAGE;
        }
        
        userAcct = UserAccountDB.checkPwd(loginName, password);
        
        if (userAcct != null) {
            logger.info(loginName + ": login to the system.");
            // Create user home directory once successfully login
            homeDir = Constants.getSYSTEM_PATH() + loginName;
            // Update the last login of this user            
            UserAccountDB.updateLastLogin(loginName, Constants.getDateTime());
            
            // Create the .../users directory 
            // Follow by .../users/loginName directory
            if (FileUploadBean.createSystemDirectory(Constants.getSYSTEM_PATH())) {
                if (FileUploadBean.createAllSystemDirectories(homeDir)) {
                    return Constants.MAIN_PAGE;
                }
            }
            
            logger.debug(loginName + ": failed to create system directories after login.");
            // If control reached here, it means some of the system directories
            // is not created, shouldn't allow user to proceed.
            return Constants.ERROR;
        }
        else {
            FacesContext facesContext = getFacesContext();
            
            facesContext.addMessage("global", new FacesMessage(
                    FacesMessage.SEVERITY_WARN, 
                    "Invalid name or password.", ""));
            logger.info(loginName + ": failed to login to the system.");
            // User ID/Password invalid, return to login page.
            return Constants.LOGIN_PAGE;
        }
    }
    
    // logout will help to invalidate the session after the user logout.
    public String logout() {
        FacesContext facesContext = getFacesContext();
        HttpSession session = (HttpSession)facesContext.
                getExternalContext().getSession(false);
        
        if (session != null) {
            session.invalidate();
            logger.debug("Session invalidated before logout.");
        }
        
        logger.info(loginName + ": logout from the system.");
        // User logoff from system, return to Login Page.
        return Constants.LOGIN_PAGE + "?faces-redirect=true";
    }
    
    // getAdminRight will return true if the role ID of the user is 1 
    // (i.e. Admin), else it will return false. The return value will be used
    // to control the access to some control/link.
    public Boolean getAdminRight() {
        // For now, use a very basic way to control the access to user account
        // access. Role ID 1 is Admin; only Admin is allowed access.        
        return userAcct.getRole_id() == 1;
    }
    
    // This function will be called by classes to determine whether the current
    // user is a adminstrator.
    public static Boolean isAdministrator() {
       return userAcct.getRole_id() == 1;
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
    
    // getUserName allows all other classes to get the id of the current user
    public static String getUserName() { return loginName; }
    // getHomeDir will return the home directory of the current user.
    public static String getHomeDir() { return homeDir; }
    
    // getHeaderInstDept will supply the Institution-Department string to
    // header.jsp view
    public static String getHeaderInstDept() { 
        if (loginName.compareTo("super") == 0) {
            return loginName;
        }
        else {
            return userAcct.getInstitution() + " - " + userAcct.getDepartment();            
        }
    }
    // getHeaderFullName will supply the Full Name string to header.jsp view
    public static String getHeaderFullName() { 
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
}
