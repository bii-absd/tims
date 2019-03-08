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
import TIMS.General.Constants;
// Libraries for Java Extension
import javax.inject.Named;
import javax.annotation.PostConstruct;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("cnvIlluBean")
@ViewScoped
public class CNVIlluminaBean extends GEXAffymetrixBean {
    private FileUploadBean ctrlFile;

    public CNVIlluminaBean() {
        logger.debug("CNVIlluminaBean created.");
    }

    @Override
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
                // Create the input filename list (Need to do first).
                inputFile.createInputList();
                // New input files are being uploaded, need to make sure the
                // application received all the input files as listed in the 
                // annotation file.
                missingFiles = inputFile.compareFileList(getAllFilenameFromAnnot());
                
                if (!missingFiles.isEmpty()) {
                    logger.debug("File(s) not received: " + missingFiles.toString());                    
                }
                setJobSubmissionStatus(true);            
            }
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
            ctrl = ctrlFile.getLocalDirectoryPath() + 
                   Constants.getCONTROL_FILE_NAME() +
                   Constants.getCONTROL_FILE_EXT();
        }
        else {
            ctrl = selectedInput.getFilepath() + 
                   Constants.getCONTROL_FILE_NAME() +
                   Constants.getCONTROL_FILE_EXT();
        }
        // Call the base class method to create the Config File.        
        return super.createConfigFile();
    }

    // Call filterRawDataFileList() to exclude the annotation and control files 
    // from the raw data file list.
    @Override
    public void retrieveRawDataFileList() {
        filterRawDataFileList();
    }

    // Machine generated getters and setters
    public FileUploadBean getCtrlFile() {
        return ctrlFile;
    }
    public void setCtrlFile(FileUploadBean ctrlFile) {
        this.ctrlFile = ctrlFile;
    }
}
