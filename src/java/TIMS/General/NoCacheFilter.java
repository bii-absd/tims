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

import TIMS.Database.UserAccountDB;
import TIMS.Database.UserRoleDB;
// Library for Java
import java.io.IOException;
// Libraries for Java Extension
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
            // For guest, only allow them to view the dashboard.
            if ( (UserAccountDB.getRoleID((String) session.getAttribute("User")) == UserRoleDB.guest()) &&
                 (!request.getRequestURI().contains("/restricted/dashboard.xhtml")) ){
                System.out.println("Redirect to dashboard.");
                response.sendRedirect(request.getContextPath() + "/restricted/dashboard.xhtml");
            }
            else {
                chain.doFilter(req, res);
            }
        }
    }

    @Override
    public void destroy() {
        System.out.println("Filter destroy called.");
        // If you have assigned any expensive resources as field of this
        // Filter class, then you could clean/close them here.
    }
}
