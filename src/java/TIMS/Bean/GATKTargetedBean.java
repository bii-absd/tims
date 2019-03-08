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

import TIMS.Database.PipelineDB;
import TIMS.General.Constants;
import TIMS.General.ResourceRetriever;
// Library for Java
import java.io.File;
import java.util.List;
// Libraries for Java Extension
import javax.inject.Named;
import javax.annotation.PostConstruct;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("GatkTarBean")
@ViewScoped
public class GATKTargetedBean extends GEXAffymetrixBean {
    private FileUploadBean intFile;
    
    public GATKTargetedBean() {
        logger.debug("GATKTargetedBean created.");
    }
    
    @Override
    public List<String> getAllFilenameFromAnnot() {
        if (pipelineName.equals(PipelineDB.GATK_TAR_SOMA)) {
            return getFilenamePairsFromAnnot();
        } else {
            return super.getAllFilenameFromAnnot();
        }
    }

    @Override
    @PostConstruct
    public void initFiles() {
        init();
        // Raw data file extension for GATK Targeted Sequencing.
        rdFileExt = "bam";
        readDepth = 100;
        variantDepth = 10;
        
        if (haveNewData) {
            intFile = new FileUploadBean(inputDir);
        }
    }
    
    @Override
    public void renameAnnotCtrlFiles() {
        // Rename interval file.
        intFile.renameFilename(Constants.getINTERVAL_FILE_NAME() + 
                                Constants.getINTERVAL_FILE_EXT());
        super.renameAnnotCtrlFiles();
    }
    
    // Make the necessary setup to those attributes that are relevant to this
    // pipeline, and then call the base class method to create the Config File.
    @Override
    public boolean createConfigFile() {
        if (haveNewData) {
            // Check to make sure the user did upload the interval file.
            if (!intFile.isFilelistEmpty()) {
                interval = intFile.getLocalDirectoryPath() + 
                           Constants.getINTERVAL_FILE_NAME() +
                           Constants.getINTERVAL_FILE_EXT();
            }
        }
        else {
            interval = selectedInput.getFilepath() + 
                       Constants.getINTERVAL_FILE_NAME() +
                       Constants.getINTERVAL_FILE_EXT();
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

    // If the user didn't upload the interval file, let them proceed but display
    // the warning message below.
    public String getIntFileName() {
        if (intFile.isFilelistEmpty()) {
            return ResourceRetriever.getMsg("warn-int-file");
        }
        else {
            return intFile.getInputFilename();
        }
    }
    
    // Machine generated getters and setters
    public FileUploadBean getIntFile() {
        return intFile;
    }
    public void setIntFile(FileUploadBean intFile) {
        this.intFile = intFile;
    }
}
