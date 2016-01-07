/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.General;

import javax.servlet.ServletContextEvent;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.web.Log4jServletContextListener;

/**
 * LogContextListener will be created when the ServletContext is being created
 * while deploying the application. The method contextInitialized will be 
 * called to load the respective log4j2 configuration file depending on the
 * Operating System the application is hosted on.
 * 
 * Author: Tay Wei Hong
 * Date: 03-Nov-2015
 * 
 * Revision History
 * 03-Nov-2015 - Created with one overwritten method contextInitialized.
 */

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
