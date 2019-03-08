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

import TIMS.Database.MetaRecord;
// Libraries for Java
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Libraries for Apache POI
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.DataFormatter;
// Library for stream reader
import com.monitorjbl.xlsx.StreamingReader;

public class ExcelHelper {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ExcelHelper.class.getName());
    private final String filename, datasheet;
    private int readInd;

    public ExcelHelper(String filename, String datasheet) {
        this.filename = filename;
        this.datasheet = datasheet;
        this.readInd = 0;
    }
    
    // Read in the data from the row and return them as a list of string.
    public static List<String> convertRowToStrList(Row values) {
        List<String> colDataL = new ArrayList<>();
        
        for (int i = 0; i < values.getLastCellNum(); i++) {
            Cell c = values.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            // For numeric and non-date columns, we need to read in
            // the cell as numeric value.
            if (c.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                if (DateUtil.isCellDateFormatted(c)) {
                    // Date, Time and Date&Time cell will be detected as Date
                    // Formatted.
                    // Read in format: dd/MM/yyyy hh:mm a
                    String tmp = MetaRecord.dateTimef.format(c.getDateCellValue());
                    if (!tmp.isEmpty()) {
                        String[] dateTimeAmPm = tmp.split(" ");
                        // For time cell, the default date is 31/12/1899.
                        if (dateTimeAmPm[0].equals("31/12/1899")) {
                            // This cell contains time value.
                            tmp = new DataFormatter().formatCellValue(c);
                        }
                        // For date cell, the default time is 12:00 or 12:20 AM
                        else if ( (dateTimeAmPm[1].equals("12:00") && dateTimeAmPm[2].equals("AM")) ||
                                  (dateTimeAmPm[1].equals("12:20") && dateTimeAmPm[2].equals("AM"))) {
                            // This cell contains date value.
                            tmp = MetaRecord.datef.format(c.getDateCellValue());
                        }
                    }
                    colDataL.add(tmp);
                }
                else {
                    colDataL.add(String.valueOf(c.getNumericCellValue()));
                }
            }
            // For others, we will read in the cell as string.
            else {
                colDataL.add(c.getStringCellValue());
            }
        }
        return colDataL;
    }
    
    // Read the next row of the excel sheet and return the column data as a list
    // of string. Only work for excel sheet that has less than 100 rows.
    public List<String> readNextRow() {
        List<String> colDataL = null;
        try (Workbook wb = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(new File(filename))) 
        {
            Sheet dataSheet = wb.getSheet(datasheet);
            int row = 0;
            // Retrieve the data from the readInd row of Excel sheet.
            for (Row values : dataSheet) {
                if (row != readInd) {
                    // Keep skipping until we reach the row number that we last
                    // read.
                    row++;
                    continue;
                }
                colDataL = convertRowToStrList(values);
                readInd++;
                // Only read in one row per call.
                break;
            }
        } catch (IOException ioe) {
            logger.error(ioe.getMessage());
            // Fail to process Excel file.
            throw new java.lang.RuntimeException("Fail to fetch row from Excel File!");
        }

        return colDataL;
    }
}
