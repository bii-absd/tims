/*
 * Copyright @2018
 */
package TIMS.Database;

// Libraries for Log4j
import org.apache.logging.log4j.LogManager;

/**
 * PieChartDataObject is used to represent the objects use in plotting pie 
 * chart in dashboard.
 * 
 * Author: Tay Wei Hong
 * Date: 05-Jul-2018
 * 
 * Revision History
 * 05-Jul-2018 - 
 */

public class PieChartDataObject extends ChartDataObject {
    private final String title;

    public PieChartDataObject(String title) {
        init();
        this.title = title;
        this.logger = LogManager.getLogger(PieChartDataObject.class.getName());
    }
    
    // Machine generated code.
    public String getTitle() {
        return title;
    }
}
