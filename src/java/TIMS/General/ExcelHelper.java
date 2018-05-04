/*
 * Copyright @2018
 */
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
// Library for stream reader
import com.monitorjbl.xlsx.StreamingReader;

/**
 * ExcelHelper is used to perform general read operation on Excel file.
 * 
 * Author: Tay Wei Hong
 * Date: 30-Apr-2018
 * 
 * Revision History
 * 30-Apr-2018 - Created with methods convertRowToStrList and readNextRow().
 */

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
                    colDataL.add(MetaRecord.df.format(c.getDateCellValue()));
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
    
    // Read the next row of the Excel sheet and return the column data as a list
    // of string.
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
