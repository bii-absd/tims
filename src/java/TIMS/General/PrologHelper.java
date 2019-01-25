/*
 * Copyright @2018
 */
package TIMS.General;

// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for Java-Prolog Interface
import org.jpl7.Query;
import org.jpl7.Term;

/**
 * PrologHelper is an abstract class and not mean to be instantiate, its main 
 * job is to create the Prolog database, and to query the Prolog database.
 * 
 * Author: Tay Wei Hong
 * Date: 26-Mar-2018
 * 
 * Revision History
 * 06-Apr-2018 - Created with static methods (i.e. queryPrologDatabase,
 * createMetaRecordValidationDatabase and deleteMetaRecordValidationDatabase).
 * 21-May-2018 - Enhanced the rules, and added two new facts to the Prolog
 * database.
 */

public abstract class PrologHelper {
    // The opening and closing space is needed i.e. ( (...) ) if you
    // are asserting or retracting a rule;
    private final static String rule_1 = 
            "( (partial_validate(Race,Gender,CaseControl) :- " +
            "sex(Gender), race(Race), casecontrol(CaseControl)) )";
    private final static String rule_2 = 
            "( (full_validate(Race,Gender,CaseControl,Height,Weight) :- " +
            "sex(Gender), race(Race), casecontrol(CaseControl)," +
            "Height =< 2.51, Height >= 0.54, " +
            "Weight =< 635, Weight >= 2) )";
    private final static String fact_1 = "(sex(f))";
    private final static String fact_2 = "(sex(m))";
    private final static String fact_3 = "(race(chinese))";
    private final static String fact_4 = "(race(malay))";
    private final static String fact_5 = "(race(indian))";
    private final static String fact_6 = "(race(eurasian))";
    private final static String fact_7 = "(race(others))";
    private final static String fact_8 = "(casecontrol(case))";
    private final static String fact_9 = "(casecontrol(control))";
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(PrologHelper.class.getName());

    // Query Prolog database with the query passed in, and return the first 
    // solution tied to goal.
    public static String queryPrologDatabase(String query, String goal) {
        return Query.oneSolution(query).get(goal).toString();
    }
    
    // Query Prolog database with the query passed in, and return true if a
    // solution is found, else false.
    public static boolean queryPrologDatabase(String query) {
        return Query.hasSolution(query);
    }
    
    // Create the rules and facts for Meta Record validation. This method needs
    // to be run first before user can query the Prolog database.
    public static boolean createMetaRecordValidationDatabase() {
        String create = "assert";
        Term swi = Query.oneSolution("current_prolog_flag(version_data,Swi)").get("Swi");
        logger.info("swipl.version = " + swi.arg(1) + "." + swi.arg(2) + "." + swi.arg(3));
        
        return Query.hasSolution(create + rule_1) && Query.hasSolution(create + fact_1) && 
               Query.hasSolution(create + fact_2) && Query.hasSolution(create + fact_3) && 
               Query.hasSolution(create + fact_4) && Query.hasSolution(create + fact_5) && 
               Query.hasSolution(create + fact_6) && Query.hasSolution(create + fact_7) &&
               Query.hasSolution(create + fact_8) && Query.hasSolution(create + fact_9) &&
               Query.hasSolution(create + rule_2);
    }
    
    // Delete the rules and facts for Meta Record validation. This method needs
    // to be called before existing from Prolog.
    public static boolean deleteMetaRecordValidationDatabase() {
        String del = "retract";
        
        return Query.hasSolution(del + rule_1) && Query.hasSolution(del + fact_1) && 
               Query.hasSolution(del + fact_2) && Query.hasSolution(del + fact_3) && 
               Query.hasSolution(del + fact_4) && Query.hasSolution(del + fact_5) && 
               Query.hasSolution(del + fact_6) && Query.hasSolution(del + fact_7) &&
               Query.hasSolution(del + fact_8) && Query.hasSolution(del + fact_9) &&
               Query.hasSolution(del + rule_2);
    }
}
