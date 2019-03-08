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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// Libraries for Java Extension
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for Trove
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public abstract class JobStatusDB {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(JobStatusDB.class.getName());
    private static final TIntObjectHashMap<String> jsIDHash = 
            new TIntObjectHashMap<String>();
//    private static LinkedHashMap<Integer, String> jsIDHash = new LinkedHashMap<>();
    private static final TObjectIntHashMap<String> jsNameHash = 
            new TObjectIntHashMap<String>();
//    private static LinkedHashMap<String, Integer> jsNameHash = new LinkedHashMap<>();

    // Return the job status name based on the status_id passed in.
    public static String getJobStatusName(int status_id) {
        return jsIDHash.get(status_id);
    }
    
    // Return the job status ID based on the status_name passed in.
    private static int getJobStatusID(String status_name) {
        return jsNameHash.get(status_name);
    }
    
    // Return the job status ID for each job status defined in the system.
    public static int waiting() {
        return getJobStatusID("Waiting");
    }
    public static int inprogress() {
        return getJobStatusID("In-progress");
    }
    public static int completed() {
        return getJobStatusID("Completed");
    }
    public static int finalizing() {
        return getJobStatusID("Finalizing");
    }
    public static int finalized() {
        return getJobStatusID("Finalized");
    }
    public static int failed() {
        return getJobStatusID("Failed");
    }
    
    // Retrieve all the job status defined in job_status table and store them
    // in the jsIDHash and jsNameHash.
    public static void buildJobStatusDef() {
        // We will only build the job status list once.
        if (jsIDHash.isEmpty()) {
            Connection conn = null;
            
            try {
                conn = DBHelper.getDSConn();
                PreparedStatement stm = conn.prepareStatement
                            ("SELECT status_id, status_name FROM job_status");
                ResultSet rs = stm.executeQuery();
                
                while (rs.next()) {
                    // Build the 2 Hash Map; One is Status ID -> Status Name,
                    // the other is Status Name -> Status ID.
                    jsIDHash.put(rs.getInt("status_id"), rs.getString("status_name"));
                    jsNameHash.put(rs.getString("status_name"), rs.getInt("status_id"));
                }
                stm.close();
            }
            catch (SQLException|NamingException e) {
                logger.error("FAIL to retrieve job status!");
                logger.error(e.getMessage());
            }
            finally {
                DBHelper.closeDSConn(conn);
            }
        }
    }    
}
