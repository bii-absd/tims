/*
 * Copyright @2016
 */
package TIMS.General;

// Libraries for Apache Common Math
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Statistics is an abstract class and not mean to be instantiate, its main job 
 * is to perform general statistical functions.
 *
 * Author: Tay Wei Hong
 * Date: 01-Aug-2016
 * 
 * Revision History
 * 10-Aug-2016 - Implemented method zScore() to calculate how many standard
 * deviations an element is from the mean.
 */

public abstract class Statistics {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(Statistics.class.getName());

    // Calculate the z-score (standard score) of x.
    public static double zScore(double x, double mean, double sd) {
        return (x - mean) / sd;
    }
    
    // Create and load the DescriptiveStatistics instance with the values 
    // passed in, starting with the index startIndex.
    public static DescriptiveStatistics createStatsInstance(String[] values, 
            int startIndex) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        
        for (int i = startIndex; i < values.length; i++) {
            stats.addValue(Double.parseDouble(values[i]));
        }
        
        return stats;
    }
}
