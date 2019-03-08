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
// Library for Java
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.inject.Named;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;

@Named("GatkWGBean")
@ViewScoped
public class GATKWholeGenomeBean extends GEXAffymetrixBean {
    
    public GATKWholeGenomeBean() {
        logger.debug("GATKWholeGenomeBean created.");
    }
    
    @Override
    public List<String> getAllFilenameFromAnnot() {
        if (pipelineName.equals(PipelineDB.GATK_WG_SOMA)) {
            return getFilenamePairsFromAnnot();
        } else {
            return super.getAllFilenameFromAnnot();
        }
    }
    
    @Override
    @PostConstruct
    public void initFiles() {
        init();
        // Raw data file extension for GATK Whole-Genome Sequencing pipelines.
        rdFileExt = "bam";
        // Set some default values for readDepth and variantDepth.
        readDepth = 100;
        variantDepth = 10;
    }
}
