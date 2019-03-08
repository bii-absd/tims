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

import TIMS.General.Constants;
// Library for Java
import java.io.File;
// Libraries for Java Extension
import javax.inject.Named;
import javax.annotation.PostConstruct;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("seqrnaBean")
@ViewScoped
public class SeqRNABean extends GEXAffymetrixBean {
    private FileUploadBean gtfFile;
    
    public SeqRNABean() {
        logger.debug("SeqRNABean created.");
    }
    
    @Override
    @PostConstruct
    public void initFiles() {
        init();
        // Raw data file extension for RNA Sequencing pipeline.
        rdFileExt = "bam";
        
        if (haveNewData) {
            gtfFile = new FileUploadBean(inputDir);
        }
    }
    
    @Override
    public void renameAnnotCtrlFiles() {
        // Rename gtf file.
        gtfFile.renameFilename(Constants.getGTF_FILE_NAME() + 
                                Constants.getGTF_FILE_EXT());
        super.renameAnnotCtrlFiles();
    }

    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public boolean createConfigFile() {
        if (haveNewData) {
            gtf = gtfFile.getLocalDirectoryPath() + 
                  Constants.getGTF_FILE_NAME() + Constants.getGTF_FILE_EXT();
        }
        else {
            gtf = selectedInput.getFilepath() + 
                  Constants.getGTF_FILE_NAME() + Constants.getGTF_FILE_EXT();
        }
        // Call the base class method to create the Config File.        
        return super.createConfigFile();
    }

    @Override
    public void updateJobSubmissionStatus() {
        if (haveNewData && gtfFile.isFilelistEmpty()) {
            logger.debug("No GTF file uploaded!");
            setJobSubmissionStatus(false);
        }
        else {
            super.updateJobSubmissionStatus();
        }
    }

    // Call filterRawDataFileList() to exclude the annotation file from the raw
    // data file list.
    @Override
    public void retrieveRawDataFileList() {
        filterRawDataFileList();
    }
    
    // Machine generated getters and setters
    public FileUploadBean getGtfFile() {
        return gtfFile;
    }
    public void setGtfFile(FileUploadBean gtfFile) {
        this.gtfFile = gtfFile;
    }
}
