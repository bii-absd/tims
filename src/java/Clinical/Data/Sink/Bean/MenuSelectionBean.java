/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * MenuSelectionBean is used as the backing bean for the main.xhtml
 * 
 * Author: Tay Wei Hong
 * Date: 23-Oct-2015
 * 
 * Revision History
 * 23-Oct-2015 - Created with all the standard getters and setters.
 * 27-Oct-2015 - Added 2 functions (gexPipeline and ngsPipeline) that will help
 * to setup the pipeline backing bean.
 */

@ManagedBean (name="menuSelectionBean")
@RequestScoped
public class MenuSelectionBean implements Serializable{
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ArrayConfigBean.class.getName());
    @ManagedProperty("#{param.command}")
    String command;
    
    public MenuSelectionBean() {}
    
    // gexPipeline will setup the ArrayConfigBean according to the specific
    // pipeline selected.
    public String gexPipeline() {
        ArrayConfigBean.setPipelineName(command);
        logger.debug(AuthenticationBean.getUserName() + ": selected " +
                     command);
        
        return Constants.ARRAYCONFIG_PAGE;
    }

    // ngsPipeline will setup the NGSConfigBean according to the specific
    // pipeline selected.
    public String ngsPipeline() {
        
        return Constants.MAIN_PAGE;
    }
    
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
}
