/*
 * Copyright @2015
 */
package Clinical.Data.Sink.General;

import java.io.IOException;
import javax.faces.application.ResourceHandler;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * NoCacheFilter helps to ensure that all the request to resources (i.e. xhrml)
 * under the /restricted folder get its response 'Cache-Control', 'Pragma', etc
 * set to 'no-cache, no-store, etc'. It also make sure the users login to the
 * system before they can proceed any further.
 * 
 * Author: Tay Wei Hong
 * Date: 11-Nov-2015
 * 
 * Revision History
 * 11-Nov-2015 - Created with one override method doFilter().
 */

@WebFilter("/restricted/*")
public class NoCacheFilter implements Filter {

    @Override
    public void init(FilterConfig config) throws ServletException {
        System.out.println("Filter init called.");
        // If you have any <init-param> in web.xml, then you could get them
        // here by config.getInitParameter("name") and assign it as field.
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession();

        if (!request.getRequestURI().startsWith(request.getContextPath() + 
                ResourceHandler.RESOURCE_IDENTIFIER)) 
                // Skip JSF resources (CSS/JS/Images/etc)
        {
            System.out.println("doFilter no-cache control.");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0
            response.setDateHeader("Expires", 0); // Proxies
        }
        
        // Make sure the user is login to the system before allowing him/her to
        // proceed, else redirect user to login page.
        if (session.getAttribute("User") == null) {
            String loginURL = request.getContextPath() + "/login.xhtml";
            System.out.println("User redirected to login page: " + loginURL);
            response.sendRedirect(loginURL);
        }
        else {
            chain.doFilter(req, res);            
        }
    }

    @Override
    public void destroy() {
        System.out.println("Filter destroy called.");
        // If you have assigned any expensive resources as field of this
        // Filter class, then you could clean/close them here.
    }
}
