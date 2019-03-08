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

import TIMS.Database.ActivityLogDB;
import TIMS.Database.Feature;
import TIMS.Database.FeatureDB;
import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.naming.NamingException;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;
// Libraries for PrimeFaces
import org.primefaces.event.RowEditEvent;

@Named("fteMgntBean")
@ViewScoped
public class FeatureManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(FeatureManagementBean.class.getName());
    private final String userName;
    private List<Feature> fteList;
    private final FeatureDB featureDB;
    
    public FeatureManagementBean() {
        userName = (String) getFacesContext().getExternalContext().
                getSessionMap().get("User");
        featureDB = new FeatureDB();
        logger.info(userName + ": access Feature Management page.");
    }
    
    @PostConstruct
    public void init() {
        fteList = featureDB.getAllFeatureStatus();
    }
    
    // Update the feature table in the database.
    public void onRowEdit(RowEditEvent event) {
        try {
            Feature fte = (Feature) event.getObject();
            featureDB.updateFeature(fte);
            // Record this feature setup activity into database.
            StringBuilder detail = new StringBuilder(fte.getFcode()).
                                       append(" - ").append(fte.getStatus());
            ActivityLogDB.recordUserActivity(userName, Constants.SET_FTE, detail.toString());
            StringBuilder oper = new StringBuilder(userName).
                    append(": updated ").append(fte.getFcode()).
                    append(" to ").append(fte.getStatus());
            logger.info(oper);
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO, "Feature setting updated.", ""));
            // Update feature list.
            AuthenticationBean.setupFeatureList(featureDB.getAllFeatureStatusHash());
        }
        catch (SQLException|NamingException e) {
            logger.error("Fail to update feature setting!");
            logger.error(e.getMessage());
            getFacesContext().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, 
                    "Failed to update feature setting!", ""));            
        }
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Machine generated getters and setters.
    public List<Feature> getFteList() {
        return fteList;
    }
}
