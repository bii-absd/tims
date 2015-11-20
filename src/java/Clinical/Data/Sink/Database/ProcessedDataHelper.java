/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Database;

import Clinical.Data.Sink.General.Constants;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    // Update the tid table's data field (at array_index) with the value 
    // passed in.
    public static Boolean updateProcessedData(String tid, String gsymbol, 
            int array_index, String value) {
        Boolean result = Constants.OK;
        
        
        return result;
    }
    
    // Insert a new pipeline record into the database.
    public static Boolean createPipelineRecord(PipelineRecord record) {
        Boolean result = Constants.OK;
        
        String insertStr = "INSERT INTO pipeline_record"
                + "(tid,array_index,pid,study_id,rdate) "
                + "VALUES(?,?,?,?,?)";
        
        try (PreparedStatement insertStm = conn.prepareStatement(insertStr)) {
            insertStm.setString(1, record.getTid());
            insertStm.setInt(2, record.getArray_index());
            insertStm.setString(3, record.getPid());
            insertStm.setString(4, record.getStudy_id());
            insertStm.setDate(5, record.getRdate());
            insertStm.executeUpdate();
            
            logger.debug("New pipeline record for technology " + 
                        record.getTid()+ " inserted at index: " + 
                        record.getArray_index());
        }
        catch (SQLException e) {
            logger.error("SQLException when inserting new pipeline record!");
            logger.error(e.getMessage());
        }
        
        return result;
    }
    
    // Return the next array index to be use for this record.
    public static int getNextArrayInd(String tid) {
        int count = Constants.DATABASE_INVALID_ID;
        String queryStr = "SELECT COUNT(*) FROM pipeline_record WHERE tid = ?";

        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, tid);
            ResultSet result = queryStm.executeQuery();
            
            while (result.next()) {
                count = result.getInt(1);                
            }
            
            logger.debug("Next array index: " + count);
        }
        catch (SQLException e) {
            logger.debug("SQLException when getting the no of row in table " + tid);
            logger.debug(e.getMessage());
        }
        
        return count;
    }
    
    // Check whether the patient exists in the database or not.
    public static Boolean PIDExistInDB(String pid) {
        Boolean pidExist = Constants.OK;
        
        String queryStr = "SELECT * FROM patient WHERE pid = ?";
        
        try (PreparedStatement queryStm = conn.prepareStatement(queryStr)) {
            queryStm.setString(1, pid);
            ResultSet result = queryStm.executeQuery();
            
            // Check whether does this pid exist in the database
            if (!result.isBeforeFirst()) {
                pidExist = Constants.NOT_OK;
                logger.debug(pid + " doesn't exist in the database.");
            }
        }
        catch (SQLException e) {
            logger.error("SQLException when searching for pid in database!");
            logger.error(e.getMessage());
        }
        
        return pidExist;
    }
    
    // Stored the gene data into the database.
    public static Boolean storeIntoDB(String fileUriX) {
        Boolean result = Constants.OK;
        int[] arrayIndex;
        String[] values;
        // Hardcored values for now.
        String tid = "Array";
        String study_id = "Bayer";
        Date rdate = Date.valueOf("2015-11-20");
        
        // Hardcode the filename for now
        String fileUri = "C:\\temp\\output-trial-23Oct2015_0329PM.txt";
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileUri));
            String line;
            // Subject line processing
            line = br.readLine();
            values = line.split("\t");
            arrayIndex = new int[values.length];
            
            // Debug purpose
            /*
            logger.debug("Data read: " + line);
            for (int j = 0; j < values.length; j++) {
                logger.debug(values[j]);
            }
            */

            // Ignore the first two strings (i.e. geneID and EntrezID)
            for (int i = 2; i < values.length; i++) {
                if (PIDExistInDB(values[i])) {
                    // Temporarily hardcore tid to Array
                    arrayIndex[i] = getNextArrayInd(tid);
                    // Insert a record into pipeline_record
                    PipelineRecord record = new PipelineRecord(tid, values[i],
                                                study_id, arrayIndex[i], rdate);
                    // Stop creating the record for now :)
//                    createPipelineRecord(record);
                }
                else {
                    arrayIndex[i] = Constants.DATABASE_INVALID_ID;
                }
            }
            
            String gsymbol;
            int temp = 0;
            // Process the gene data
            while ((line = br.readLine()) != null) {
                values = line.split("\t");
                // The first string is the gene symbol
                gsymbol = values[0];
                temp++;
//                System.out.print(gsymbol);
                // Start reading in the data from 3rd string
                for (int i = 2; i < values.length; i++) {
                    // Only process those data with valid PID
                    if (arrayIndex[i] != Constants.DATABASE_INVALID_ID) {
                        // Call updateProcessedData
                        
                    }
                }
            }
            
            System.out.println("Total gene record read: " + temp);
        }
        catch (IOException ioe) {
            logger.error("IOException when storing gene data into database.");
            logger.error(ioe.getMessage());
            result = Constants.NOT_OK;
        }
        
        return result;
    }
}
