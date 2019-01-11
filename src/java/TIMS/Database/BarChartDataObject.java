/*
 * Copyright @2018
 */
package TIMS.Database;

// Libraries for Log4j
import org.apache.logging.log4j.LogManager;

/**
 * BarChartDataObject is used to represent the objects use in plotting bar
 * chart in dashboard.
 * 
 * Author: Tay Wei Hong
 * Date: 05-Jul-2018
 * 
 * Revision History
 * 05-Jul-2018 - Added one new method convertSeriesCountToPercentage().
 * 20-Dec-2018 - Modified convertSeriesCountToPercentage(); round up the 
 * percentage to 2 decimal points.
 */

public class BarChartDataObject extends ChartDataObject {
    private final String data_name;

    public BarChartDataObject(String data_name) {
        init();
        this.data_name = data_name;
        this.logger = LogManager.getLogger(BarChartDataObject.class.getName());
    }
    
    // Convert the series count to a percentage number (based on the total
    // number of series counts.)
    public void convertSeriesCountToPercentage() {
        float total = 0;
        
        // Get the total count from all the series.
        for (String series : data_series.keySet()) {
            total += data_series.get(series);
        }
        // Convert each series count as a percentage of the total.
        for (String series : data_series.keySet()) {
            double percent = (data_series.get(series) * 100.0) / total;
            // Round the percentage to 2 decimal points.
            double percentR2DP = Math.round(percent * 100.0) / 100.0;
            data_series.put(series, percentR2DP);
        }
    }
    
    // Machine generated code.
    public String getData_name() {
        return data_name;
    }
}
