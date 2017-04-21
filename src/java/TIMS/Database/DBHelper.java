/*
 * Copyright @2015-2017
 */
package TIMS.Database;

import TIMS.General.Constants;
import java.sql.*;
import java.util.concurrent.Semaphore;
// Libraries for Java Extension
import javax.faces.bean.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.postgresql.ds.PGConnectionPoolDataSource;

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
 * 17-Dec-2015 - Added 2 static methods, setDBTransactionIsolation and
 * checkDBTransactionIsolation().
 * 26-Feb-2016 - According to Java SE 7 document, applications no longer need to
 * explictly load JDBC drivers using Class.forName(). Removed that code.
 * 29-Feb-2016 - Implementation of Data Source pooling. To use DataSource to 
 * get the database connection instead of using DriverManager.
 * 25-Apr-2016 - Increased the maximum allowed database connections to 16.
 * 12-Dec-2016 - Provide the token to proceed for finalization and closure of 
 * study.
 * 19-Apr-2017 - Changes due to PostgreSQL JDBC driver upgrade.
 */

@ApplicationScoped
public abstract class DBHelper {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DBHelper.class.getName());
    private static PGConnectionPoolDataSource ds;
    // Token to allow finalization and closure of study to proceed.
    private final static Semaphore finalizeToken = new Semaphore(1, true);
    private final static Semaphore closureToken = new Semaphore(1, true);
    
    // Initialise the data source for TIMS.
    public static void initDataSource() {
        // Only load this once when the application first started.
        if (ds == null) {
            logger.debug("Init data source for TIMS.");
            ds = new PGConnectionPoolDataSource();
            ServletContext context = getServletContext();
            // Loading the DB username and password
            String uname = context.getInitParameter("uname");
            String pword = context.getInitParameter("pword");
            
            ds.setServerName(Constants.getSERVER_NAME());
            ds.setDatabaseName(Constants.getDATABASE_NAME());
            ds.setUser(uname);
            ds.setPassword(pword);
        }
    }

    // Return the database connection to be use by the application.
    public static Connection getDSConn() 
            throws SQLException, NamingException 
    {
        return ds.getConnection();
    }
    
    // Close the database connection after use by the individual modules.
    public static void closeDSConn(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            }
            catch (SQLException e) {
                logger.error("FAIL to close data source connection!");
                logger.error(e.getMessage());
            }
        }
    }
    
    // getServletContext will return the servlet context
    private static ServletContext getServletContext() {
        return (ServletContext) FacesContext.getCurrentInstance().
                getExternalContext().getContext();
    }
    
    // Set the database transaction isolation level.
    public static void setDBTransactionIsolation(int level) {
        Connection conn = null;
        
        try {
            conn = getDSConn();
            conn.setTransactionIsolation(level);
        }
        catch (SQLException|NamingException e) {
            logger.error("SQLException when setting DB transaction isolation level!");
            logger.error(e.getMessage());
        }
        finally {
            closeDSConn(conn);
        }
    }
    
    // Check the current database transaction isolation level.
    public static int checkDBTransactionIsolation() {
        Connection conn = null;
        int txIso = Constants.DATABASE_INVALID_ID;
        
        try {
            conn = getDSConn();
            txIso = conn.getTransactionIsolation();
            
            switch (txIso) {
                case Connection.TRANSACTION_NONE:
                    logger.debug("TRANSACTION_NONE");
                    break;
                case Connection.TRANSACTION_READ_COMMITTED:
                    logger.debug("TRANSACTION_READ_COMMITTED");
                    break;
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    logger.debug("TRANSACTION_READ_UNCOMMITTED");
                    break;
                case Connection.TRANSACTION_REPEATABLE_READ:
                    logger.debug("TRANSACTION_REPEATABLE_READ");
                    break;
                case Connection.TRANSACTION_SERIALIZABLE:
                    logger.debug("TRANSACTION_SERIALIZABLE");
                    break;
                default:
                    logger.debug("UNKNOWN TRANSACTION ISOLATION.");
            }
        }
        catch (SQLException|NamingException e) {
            logger.error("SQLException when checking DB transaction isolation level!");
            logger.error(e.getMessage());
        }
        finally {
            closeDSConn(conn);
        }

        return txIso;
    }
    
    // To acquire the token to perform finalization of study.
    public static void acquireFinalizeToken(String uid) throws InterruptedException {
        logger.debug(uid + " try to acquire the token for finalization.");
        finalizeToken.acquire();
        logger.debug(uid + " acquired the token for finalization.");
    }
    // To release the token after finalization.
    public static void releaseFinalizeToken(String uid) {
        finalizeToken.release();
        logger.debug(uid + " released the token for finalization.");
    }
    // To acquire the token to perform closure of study.
    public static void acquireClosureToken(String uid) throws InterruptedException {
        logger.debug(uid + " try to acquire the token for closure.");
        closureToken.acquire();
        logger.debug(uid + " acquired the token for closure.");
    }
    // To release the token after closure.
    public static void releaseClosureToken(String uid) {
        closureToken.release();
        logger.debug(uid + " released the token for closure.");
    }
}
