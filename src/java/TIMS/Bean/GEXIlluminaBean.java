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

import static TIMS.Bean.ConfigBean.logger;
import TIMS.Database.InputData;
import TIMS.General.Constants;
// Libraries for Java
import java.sql.SQLException;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.naming.NamingException;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("gexIlluBean")
@ViewScoped
public class GEXIlluminaBean extends ConfigBean {
    private FileUploadBean ctrlFile;

    public GEXIlluminaBean() {
        logger.debug("GEXIlluminaBean created.");
    }
    
    @PostConstruct
    public void initFiles() {
        init();
        // Set the raw data file extension for this pipeline.
        rdFileExt = "txt";
        
        if (haveNewData) {
            ctrlFile = new FileUploadBean(inputDir);
        }
    }
    
    @Override
    public void updateJobSubmissionStatus() {
        if (!haveNewData) {
            // Only update the jobSubmissionStatus if data has been selected 
            // for reuse.
            if (selectedInput != null) {
                setJobSubmissionStatus(true);
                input_sn = selectedInput.getSn();
                logger.debug("Data uploaded on " + selectedInput.getCreateTimeString() + 
                             " has been selected for reuse.");
            }
            else {
                // No data is being selected for reuse, display error message.
                logger.debug("No data selected for reuse!");
            }
        }
        else {
            // Only update the jobSubmissionStatus if all the input files are 
            // uploaded.
            if (!(inputFile.isFilelistEmpty() || sampleFile.isFilelistEmpty() ||
                ctrlFile.isFilelistEmpty())) {
                setJobSubmissionStatus(true);            
            }
        }
    }

    /* NOT IN USE ANYMORE!
    @Override
    public boolean insertJob() {
        boolean result = Constants.OK;
        // If new raw data has been uploaded, input_desc will follow the 
        // description that the user has entered.
        String input_desc = inputFileDesc;
        if (!haveNewData) {
            // Reusing raw data.
            if (custStatus) {
                // Customized raw data.
                input_desc = custDesc;
            }
            else {
                input_desc = selectedInput.getDescription();
            }
        }
        
        // job_id will not be used during insertion, just send in any value will
        // do e.g. 0
        // Insert the new job request into datbase; job status is 1 i.e. Waiting
        // DB 2.0 - For attribute summarization, set it to "NA".
        // For complete_time, set to null for the start.
        // 
        // SubmittedJob(job_id, study_id, user_id, pipeline_name, status_id, 
        // submit_time, complete_time, chip_type, input_sn, input_desc, 
        // normalization, summarization, output_file, detail_output, report) 
        SubmittedJob newJob = 
                new SubmittedJob(0, getStudyID(), userName, pipelineName, 1,
                                 submitTimeInDB, null, getType(), input_sn, 
                                 input_desc, getNormalization(), "NA", 
                                 pipelineOutput, detailOutput, pipelineReport);
        
        try {
            // Store the job_id of the inserted record
            job_id = SubmittedJobDB.insertJob(newJob);
        }
        catch (SQLException|NamingException e) {
            result = Constants.NOT_OK;
            logger.error("FAIL to insert job!");
            logger.error(e.getMessage());
        }

        return result;
    }
    */
    
    @Override
    public void saveSampleFileDetail() {
        try {
            input_sn = inputDB.getNextSn(studyID);
            // Insert a new record into input_data table.
            InputData newdata = new InputData(studyID, userName, pipelineName,
                    inputFile.getInputFilename(), 
                    inputFile.getLocalDirectoryPath(),
                    inputFileDesc, input_sn, submitTimeInDB);
            inputDB.insertInputData(newdata);
        }
        catch (SQLException|NamingException e) {
            logger.error("FAIL to insert input data detail!");
            logger.error(e.getMessage());
        }
    }

    @Override
    public void renameAnnotCtrlFiles() {
        // Rename control probe file.
        ctrlFile.renameFilename(Constants.getCONTROL_FILE_NAME() + 
                                Constants.getCONTROL_FILE_EXT());
        super.renameAnnotCtrlFiles();
    }
    
    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public boolean createConfigFile() {
        if (haveNewData) {
            input = inputFile.getLocalDirectoryPath() + inputFile.getInputFilename();
            ctrl = ctrlFile.getLocalDirectoryPath() + 
                   Constants.getCONTROL_FILE_NAME() +
                   Constants.getCONTROL_FILE_EXT();
            sample = sampleFile.getLocalDirectoryPath() + 
                     Constants.getANNOT_FILE_NAME() + 
                     Constants.getANNOT_FILE_EXT();
        }
        else {
            input = selectedInput.getFilepath() + selectedInput.getFilename();
            ctrl = selectedInput.getFilepath() + 
                   Constants.getCONTROL_FILE_NAME() +
                   Constants.getCONTROL_FILE_EXT();
            sample = selectedInput.getFilepath() + 
                     Constants.getANNOT_FILE_NAME() + 
                     Constants.getANNOT_FILE_EXT();
        }

        // Call the base class method to create the Config File.
        return super.createConfigFile();
    }

    // Machine generated getters and setters
    public FileUploadBean getCtrlFile() {
        return ctrlFile;
    }
    public void setCtrlFile(FileUploadBean ctrlFile) {
        this.ctrlFile = ctrlFile;
    }
}
