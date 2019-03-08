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

// Libraries for Apache Common Math
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
