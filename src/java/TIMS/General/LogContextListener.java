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

import javax.servlet.ServletContextEvent;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.web.Log4jServletContextListener;

public class LogContextListener extends Log4jServletContextListener{
    
    public LogContextListener(){}
    
    @Override
    public void contextInitialized(ServletContextEvent event) {
        String OS = System.getProperty("os.name");
        String pathToConfigFile, realPathToConfigFile;
        
        // Load the log4j2 config file from context-param according to the
        // Operating system the application is hosted on.
        if (OS.startsWith("Windows")) {
            pathToConfigFile = event.getServletContext().
                    getInitParameter("log4j2w");
        }
        else {
            pathToConfigFile = event.getServletContext().
                    getInitParameter("log4j2l");
        }
        
        realPathToConfigFile = event.getServletContext().
                getRealPath(pathToConfigFile);
        Configurator.initialize(null, realPathToConfigFile);
        super.contextInitialized(event);
    }
}
