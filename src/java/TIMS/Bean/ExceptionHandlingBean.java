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

import TIMS.General.Postman;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ManagedBean (name="exBean")
@RequestScoped
public class ExceptionHandlingBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ExceptionHandlingBean.class.getName());

    public ExceptionHandlingBean() {
        // Send exception email to the administrators for help.
        Postman.sendExceptionEmail();
        logger.debug("ExceptionHandlingBean created.");
    }
    
    @PostConstruct
    public void init() {
        // Invalidate the session upon entering any exception/error view.
        // This is to prevent the users from using the Back button to navigate
        // to the previous page.
        FacesContext.getCurrentInstance().getExternalContext().
                invalidateSession();
    }
    
    // Return the last statement on the error view.
    public String getErrorInstruction() {
        return "Please come back later.";
    }
    
    // Return the last statement on the pagenotfound view.
    public String getPNFInstruction() {
        return "Please login ";
    }
}
