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

// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ProcessExitDetector extends Thread {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ProcessExitDetector.class.getName());    
    // The process which we want to detect the end
    private Process process;
    // The associated listener to be invoked at the end of the process
    private ExitListener listener;
    // The job ID that this process is associated with
    private int jobID;
    // The Study ID of this job
    private String studyID;
    
    public ProcessExitDetector(int jobID, String studyID, Process process, 
            ExitListener listener) {
        try {
            // Test if the process is finished. exitValue will throw
            // IllegalThreadStateException if the process has not yet ended.
            int result = process.exitValue();

            // If exitValue return, this means the process has already ended.
            logger.error("Process already ended before creating detector.");
            // Update the job status according to the process exit value.
            listener.processFinished(jobID, studyID, result);
            // Throw an exception that will propagates beyond the run method
            throw new IllegalArgumentException(
                    "Process already ended before creating detector.");
        } catch (IllegalThreadStateException e) {
            // This mean that the process has not yet complete i.e. we need to
            // detect it's end.
            this.jobID = jobID;
            this.studyID = studyID;
            this.process = process;
            this.listener = listener;
            logger.debug("Process exit detector created.");
        }
    }
    
    // Return the process currently being monitor
    public Process getProcess() {
        return process;
    }
    
    @Override
    public void run() {
        try {
            // Wait for the process to finish
            int result = process.waitFor();
            // Invoke the listener
            logger.debug("Pipeline for job ID " + jobID + 
                    " has completed with exit status: " + result);
            listener.processFinished(jobID, studyID, result);
        }
        catch (InterruptedException e) {
            logger.error("InterruptedException encountered while monitoring "
                    + "the pipeline execution for job ID: " + jobID);
        }
    }
}
