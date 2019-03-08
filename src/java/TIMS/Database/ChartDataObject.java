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
package TIMS.Database;

import TIMS.General.Constants;
// Libraries for Java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;

public abstract class ChartDataObject implements Serializable {
    protected Logger logger;
//    protected LinkedHashMap<String, Integer> data_series;
    protected LinkedHashMap<String, Double> data_series;

    protected void init() {
        this.data_series = new LinkedHashMap<>();
    }
    
    public void addSeries(String series_name) {
        data_series.put(series_name, 0.0);
    }
    public void addSeries(String series_name, int number) {
        data_series.put(series_name, (double) number);
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
    public double getSeriesCount(String series_name) {
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
