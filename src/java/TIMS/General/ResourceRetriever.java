/*
 * Copyright @2016
 */
package TIMS.General;

// Libraries for Log4j
import java.util.ResourceBundle;
import javax.faces.context.FacesContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ResourceRetriever is an abstract class and not mean to be instantiate, its 
 * main job is to retrieve those messages defined in the resource bundle.
 * 
 * Author: Tay Wei Hong
 * Date: 20-Jan-2016
 * 
 * Revision History
 * 20-Jan-2016 - Created with one method, getMsg.
 */

public abstract class ResourceRetriever {
    // Get the logger for Log4j
    private static final Logger logger = LogManager.
            getLogger(ResourceRetriever.class.getName());
    private static final FacesContext fc = FacesContext.getCurrentInstance();
    private static final ResourceBundle msg = fc.getApplication().
            evaluateExpressionGet(fc, "#{msg}", ResourceBundle.class);
    
    // Return the message content for this key defined in the resource bundle.
    public static String getMsg(String key) {
        return msg.getString(key);
    }
}
