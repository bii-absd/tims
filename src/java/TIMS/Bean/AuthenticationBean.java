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
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.DBHelper;
import TIMS.Database.FeatureDB;
import TIMS.Database.GroupDB;
import TIMS.Database.InstitutionDB;
import TIMS.Database.JobStatusDB;
import TIMS.Database.SystemParametersDB;
import TIMS.Database.UserAccount;
import TIMS.Database.UserAccountDB;
import TIMS.Database.UserRoleDB;
import TIMS.General.Constants;
// Libraries for Java
import java.io.File;
import java.io.Serializable;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
// Libraries for Java Extension
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ManagedBean;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@ManagedBean (name="authBean")
@SessionScoped
public class AuthenticationBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(AuthenticationBean.class.getName());
    private String loginName, password, instName;
    private UserAccount userAcct;
    private static Map<String, String> featureList;
    
    public AuthenticationBean() {
        logger.debug("AuthenticationBean created.");
    }
    
    // Retrieve the feature status list from the database.
    public static void setupFeatureList(Map<String, String> feature) {
        featureList = feature;
    }
    
    // Setup the database configuration, input and config file path according 
    // to the OS the application is hosted on.
    private boolean setupConstants(ServletContext context) {
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
    public String login() {
        // Next page to proceed to
        String result = Constants.LOGIN_PAGE;
        ServletContext context = getServletContext();
        FeatureDB featureDB = new FeatureDB();
        
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
        // Load the system parameters from database.
        SystemParametersDB.loadSystemParameters();
        // Retrieve the job status definition from database.
        JobStatusDB.buildJobStatusDef();
        // Build the role list.
        UserRoleDB.getRoleNameHash();
        // Build the ICD10 Code HashMap.
//        ICD10DB.buildICDHashMaps();
        // Build the Institution ID HashMap.
//        InstitutionDB.buildInstIDHash();
        // Setup the feature active status list.
        setupFeatureList(featureDB.getAllFeatureStatusHash());
        
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
                UserAccountDB.updateLastLogin(loginName, Constants.getStandardDT());
                // Save the user ID in the session map.
                getFacesContext().getExternalContext().getSessionMap().
                                    put("User", loginName);
                // Save the institution name where this user belongs to.
                instName = InstitutionDB.getInstName(userAcct.getUnit_id());
                // Proceed from login to the next view depending on user role.
                if ( (userAcct.getRoleName().compareTo("Director") == 0) || 
                     (userAcct.getRoleName().compareTo("HOD") == 0) ) {
                    // For director and HOD, direct them to the dashboard.
                    result = Constants.PAGES_DIR + Constants.DASHBOARD;
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
    
    // Return true if the visualizer is set to cBioPortal.
    public boolean isCBioPortalON() {
        return featureList.get("Visualizer").equals("cBioPortal");
    }
    // Return true if the visualizer is set to None.
    public boolean isVisualizationON() {
        return !featureList.get("Visualizer").equals("None");
    }
    // Return true if array processing pipelines are available.
    public boolean isArrayProcessingON() {
        return featureList.get("Array Processing").equals("ON");
    }
    // Return true if NGS processing pipelines are available.
    public boolean isNgsProcessingON() {
        return featureList.get("NGS Processing").equals("ON");
    }
    
    // Return the home link to be display at the main page; depending on the
    // user's assgined role.
    public String getHomeStr() {
        String homeStr = "Home";

        if (loginName.compareTo("super")==0) {
            homeStr = "Super";
        }
        else if ( (userAcct.getRole_id() == UserRoleDB.director()) || 
                  (userAcct.getRole_id() == UserRoleDB.hod()) ) {
            homeStr = "Home (Dashboard)";
        }
        else if ( (userAcct.getRole_id() == UserRoleDB.pi()) || 
                  (userAcct.getRole_id() == UserRoleDB.user())) {
            homeStr = "Home (Workflow)";
        }
        
        return homeStr;
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
    // Return true if the user is an Admin or an PI Lead.
    public boolean isAdminPILead() {
        return isAdministrator() || isPILead();
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
