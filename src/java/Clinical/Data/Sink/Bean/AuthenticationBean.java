/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.Database.DBHelper;
import Clinical.Data.Sink.General.Constants;
import Clinical.Data.Sink.Database.UserAccount;
import Clinical.Data.Sink.Database.UserAccountDB;

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

/**
 * AuthenticationBean is the backing bean for the login.jsp view.
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
 */

@ManagedBean (name="authenticationBean")
@SessionScoped
public class AuthenticationBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(AuthenticationBean.class.getName());
    private static DBHelper dbHandle;
    private static String loginName;
    private String password;
    private static UserAccount userAcct;
    
    public AuthenticationBean() {}
    
    public String login()
    {
        // Setting up the database configuration, input and config file path.
        ServletContext context = getServletContext();
        // Load the setup filename from context-param
        String setupFile = context.getInitParameter("setting");
        logger.debug("Config file located at: " + context.getRealPath(setupFile));
        // Setup the constants using the parameters defined in setup
        if (Constants.setup(context.getRealPath(setupFile)).
                compareTo(Constants.ERROR) == 0) {
            // System having issue, shouldn't let the user proceed.
            return Constants.ERROR;
        }
        // The real path of the setup file should change accordingly once
        // the web application has been ported to linux server
        
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
            return Constants.SUCCESS;
        }
        
        userAcct = UserAccountDB.checkPwd(loginName, password);
        
        if (userAcct != null) {
            logger.info(loginName + ": login to the system.");
            return Constants.SUCCESS;
        }
        else {
            FacesContext facesContext = getFacesContext();
            
            facesContext.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN, 
                    "Invalid name or password.", ""));
            logger.info(loginName + ": failed to login to the system.");
            return Constants.FAILURE;
        }
    }
    
    // logout will help to invalidate the session after the user logout.
    public String logout() {
        FacesContext facesContext = getFacesContext();
        HttpSession session = (HttpSession)facesContext.
                getExternalContext().getSession(false);
        
        if (session != null) {
            session.invalidate();
        }
        
        logger.info(loginName + ": logout from the system.");
        return Constants.LOGOFF;
    }
    
    // getAdminRight will return true if the role ID of the user is 1 
    // (i.e. Admin), else it will return false. The return value will be used
    // to control the access to some control/link.
    public Boolean getAdminRight() {
        // For now, use a very basic way to control the access to user account
        // access. Role ID 1 is Admin; only Admin is allowed access.        
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
