/*
 * Copyright @2018
 */
package TIMS.Database;

import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;

/**
 * ChartDataObject is an abstract class, and it will be extended by all the 
 * chart data objects.
 * 
 * Author: Tay Wei Hong
 * Date: 02-Jul-2018
 * 
 * Revision History
 * 02-Jul-2018 - Added methods to manipulate the the series, series count and
 * series name.
 */

public abstract class ChartDataObject implements Serializable {
    protected Logger logger;
    protected LinkedHashMap<String, Integer> data_series;

    protected void init() {
        this.data_series = new LinkedHashMap<>();
    }
    
    public void addSeries(String series_name) {
        data_series.put(series_name, 0);
    }
    public void addSeries(String series_name, int number) {
        data_series.put(series_name, number);
    }
    
    public boolean increSeriesCount(String series_name) {
        if (data_series.containsKey(series_name)) {
            data_series.replace(series_name, data_series.get(series_name)+1);
            return true;
        }
        // Trying to increment an invalid series.
        return false;
    }
    
    // Return the series count for plotting.
    public int getSeriesCount(String series_name) {
        if (data_series.containsKey(series_name)) {
            return data_series.get(series_name);
        }
        // Trying to get the count of an invalid series.
        return Constants.DATABASE_INVALID_ID;
    }
    
    // Return the list of series name.
    public List<String> getSeriesName() {
        return new ArrayList<>(data_series.keySet());
    }
    
    // Return the number of series defined in this chart data object.
    public int getNumOfSeriesDefined() {
        return data_series.size();
    }    
}
