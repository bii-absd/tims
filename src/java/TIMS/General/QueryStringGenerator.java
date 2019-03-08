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

import TIMS.Database.UserAccount;
// Libraries for Log4j
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class QueryStringGenerator {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(QueryStringGenerator.class.getName());

    // Return the query statement that will retrieve the list of group(s) that
    // this user is heading (for Director|HOD|PI) or belongs to (for Admin|User).
    public static String genGrpQuery4Visualize(UserAccount user) {
        String groupQuery = null;
        
        switch (user.getRoleName()) {
            case "Director":
            case "HOD":
            case "PI":
                groupQuery = "SELECT grp_id FROM grp WHERE pi = \'" + user.getUser_id() + "\'";
                break;
            case "Admin":
            case "User":
            default:
                groupQuery = "\'" + user.getUnit_id() + "\'";
                break;
        }
        
        logger.debug("Group query statement for visualization generated: " + groupQuery);
        return groupQuery;
    }
    
    // Return the query statement that will retrieve the list of group(s) that
    // fall under this user.
    public static String genGrpQuery4Review(UserAccount user) {
        String groupQuery = null;
        
        switch (user.getRoleName()) {
            case "Director":
                groupQuery = "SELECT grp_id FROM inst_dept_grp "
                           + "WHERE inst_id = \'" + user.getUnit_id() + "\'";
                break;
            case "HOD":
                groupQuery = "SELECT grp_id FROM inst_dept_grp "
                           + "WHERE dept_id = \'" + user.getUnit_id() + "\'";
                break;
            case "PI":
                groupQuery = "SELECT grp_id FROM grp WHERE pi = \'" + user.getUser_id() + "\'";
                break;
            case "Admin":
            case "User":
            default:
                groupQuery = "\'" + user.getUnit_id() + "\'";
                break;
        }

        logger.debug("Group query statement for review generated: " + groupQuery);
        return groupQuery;
    }
}
