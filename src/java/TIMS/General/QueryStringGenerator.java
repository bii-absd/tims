/*
 * Copyright @2016
 */
package TIMS.General;

import TIMS.Database.UserAccount;
// Libraries for Log4j
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * QueryStringGenerator is an abstract class and not mean to be instantiate, 
 * its main job is to create general string for database query. 
 * 
 * Author: Tay Wei Hong
 * Date: 20-Jun-2016
 * 
 * Revision History
 * 22-Jun-2016 - Created with two methods, genGrpQuery4Visualize() and 
 * genGrpQuery4Review().
 */

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
