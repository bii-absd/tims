/*
 * Copyright @2016
 */
package Clinical.Data.Sink.General;

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
    private final static Logger logger = LogManager.
            getLogger(ResourceRetriever.class.getName());
    private static FacesContext fc = FacesContext.getCurrentInstance();
    private static ResourceBundle msg = fc.getApplication().
            evaluateExpressionGet(fc, "#{msg}", ResourceBundle.class);
    
    // Return the message content for this key defined in the resource bundle.
    public static String getMsg(String key) {
        String content = msg.getString(key);
        logger.debug(key + " - " + content);
        
        return content;
    }
}
