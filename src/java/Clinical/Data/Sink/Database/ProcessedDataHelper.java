/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ProcessedDataHelper is a helper class that help to read in the gene data 
 * from the pipeline output and stored them into the database.
 * 
 * Author: Tay Wei Hong
 * Date: 19-Nov-2015
 * 
 * Revision History
 * 19-Nov-2015 - Created with all the standard getters and setters.
 */

public class ProcessedDataHelper {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ProcessedDataHelper.class.getName());
    private final static Connection conn = DBHelper.getDBConn();

    // Stored the gene data into the database.
    public static Boolean storeIntoDB(String fileUri) {
        Boolean result = Constants.OK;
        
        try (BufferedReader br = new BufferedReader(new FileReader(fileUri))) {
            
        }
        catch (IOException ioe) {
            logger.error("IOException when storing gene data into database.");
            logger.error(ioe.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
}
