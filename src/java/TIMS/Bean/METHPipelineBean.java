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
import java.util.List;
// Libraries for Java Extension
import javax.inject.Named;
import javax.annotation.PostConstruct;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("methPBean")
@ViewScoped
public class METHPipelineBean extends GEXAffymetrixBean {

    public METHPipelineBean() {
        logger.debug("METHPipelineBean created.");
    }
    
    @Override
    @PostConstruct
    public void initFiles() {
        init();
        // Raw data file extension for Methylation pipeline.
        rdFileExt = "idat";
    }
    
    // For Methylation pipeline, there will be 2 filenames for each sample 
    // input in the annotation file.
    @Override
    public List<String> getAllFilenameFromAnnot() {
        return getFilenamePairsFromAnnot();
    }
}
