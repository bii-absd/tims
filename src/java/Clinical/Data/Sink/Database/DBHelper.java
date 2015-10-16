/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.sql.*;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * DBHelper is a helper class that provide the postgresSQL database connection 
 * to those classes that needed to access the database. 
 * 
 * Author: Tay Wei Hong
 * Date: 28-Sep-2015
 * 
 * Revision History
 * 28-Sep-2015 - First baseline with 3 methods (getDBConn, runQuery and
 * getServletContext) created.
 * 29-Sep-2015 - Hard-coded the database dirver and database name due to the
 * difficulty in maintaining the values at Constants class.
 * 02-Oct-2015 - Added in the comments for the code.
 * 07-Oct-2015 - Added Log4j2 for this class. Changed to connection based for 
 * database access.
 * 15-Oct-2015 - Critical error handling. All the exceptions encountered
 * while getting the database connection should be handle by the caller.
 */

@SessionScoped
public class DBHelper {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DBHelper.class.getName());
    
    private static Connection dbConn;
    
    public DBHelper() throws ClassNotFoundException, 
            InstantiationException, IllegalAccessException, SQLException {
        ServletContext context = getServletContext();
        // Loading the DB username and password
        String uname = context.getInitParameter("uname");
        String pword = context.getInitParameter("pword");

        // Debug Statement to print out the database driver used.
        logger.debug("Loading DB Driver: " + Constants.getDATABASE_DRIVER());
        Class.forName(Constants.getDATABASE_DRIVER()).newInstance();
        dbConn = DriverManager.getConnection(Constants.getDATABASE_NAME(), 
                 uname, pword);
    }
    
    // getDBConn will return the connection to be use by the application
    // to access the database.
    public static Connection getDBConn() {
        return dbConn;
    }
    
    // runQuery will execute any query string passed in. The result will be
    // returned to the caller using a ResultSet object.
    public static ResultSet runQuery(String query) {
        ResultSet queryResult = null;
        
        // DO NOT PUT the PreparedStatement inside try() because the 
        // PreparedStatement will get closed, resulting in a empty result being
        // returned to the caller.
        try {
            PreparedStatement queryStatement = dbConn.prepareStatement(query);
            queryResult = queryStatement.executeQuery();
        }
        catch (SQLException e) {
            logger.error("SQLException at runQuery.");
            logger.error(e.getMessage());
        }
        
        return queryResult;
    }
    
    // getServletContext will return the servlet context
    private ServletContext getServletContext() {
        return (ServletContext) FacesContext.getCurrentInstance().
                getExternalContext().getContext();
    }
}