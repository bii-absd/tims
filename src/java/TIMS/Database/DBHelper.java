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
