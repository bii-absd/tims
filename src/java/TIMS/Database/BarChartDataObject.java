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

// Libraries for Log4j
import org.apache.logging.log4j.LogManager;

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
