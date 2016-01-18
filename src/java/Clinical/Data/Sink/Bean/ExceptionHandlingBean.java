/*
 * Copyright @2015-2016
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.General.Postman;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
// Libraries for Log4j
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ExceptionHandlingBean is the backing bean for all the exception/error views
 * i.e. error, pagenotfound, etc.
 * 
 * Author: Tay Wei Hong
 * Created on: 15-Jan-2016
 * 
 * Revision History
 * 15-Jan-2016 - Created with 2 methods, getErrorInstruction() and 
 * getPNFInstruction().
 * 18-Jan-2016 - To send a email to the administrators whenever the user enter
 * the error page.
 */

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
