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
package TIMS.General;

import TIMS.Database.SubmittedJobDB;
import java.sql.Timestamp;
import java.util.Date;
import java.util.EventListener;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ExitListener implements EventListener {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ExitListener.class.getName());    

    public ExitListener() {}
    
    // The pipeline has completed execution, update the job status according
    // to the process return status.
    public void processFinished(int job_id, String study_id, int result) {
        if (result == 0) {
            // Zip the output file.
            String opPath = SubmittedJobDB.zipOutputFile(job_id);
            
            if (opPath != null) {
                // Delete the original output file to free up memory space.
                if (FileHelper.delete(opPath)) {
                    logger.debug("Original output file for Job ID " + job_id + " deleted.");
                }
                else {
                    logger.error("FAIL to delete original output file!");
                }
            }
            // Zip the detail output file.
            String doPath = SubmittedJobDB.zipDetailOutput(job_id);
            
            if (doPath != null) {
                // Delete the original detail output to free up memory space.
                if (FileHelper.delete(doPath)) {
                    logger.debug("Original detail output for Job ID " + job_id + " deleted.");
                }
                else {
                    logger.error("FAIL to delete original detail output!");
                }
            }            
            SubmittedJobDB.updateJobStatusToCompleted(job_id);
            logger.debug("Job status updated to completed. ID: " + job_id);
            // Send the status email.
            Postman.sendJobStatusEmail(job_id, study_id, Constants.OK);
        }
        else {
            SubmittedJobDB.updateJobStatusToFailed(job_id);
            logger.debug("Job status updated to failed. ID: " + job_id);
            Postman.sendJobStatusEmail(job_id, study_id, Constants.NOT_OK);
        }
        // Record into database, the completion time of this job.
        Date now = new Date();
        Timestamp complete_time = new Timestamp(now.getTime());
        SubmittedJobDB.updateJobCompleteTime(job_id, complete_time);
    }
}
