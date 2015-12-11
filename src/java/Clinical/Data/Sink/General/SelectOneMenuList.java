/*
 * Copyright @2015
 */
package Clinical.Data.Sink.General;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * SelectItemType is the backing bean for all the drop-down list box used in
 * the views. It support dynamic list of items for the drop-down box.
 * 
 * Author: Tay Wei Hong
 * Date: 08-Oct-2015
 * 
 * Revision History
 * 08-Oct-2015 - Created with 2 drop-down lists for vendor types and type. The 
 * list is currently hard-coded.
 * 09-Oct-2015 - Added 2 new drop-down lists for institution and department.
 * 14-Oct-2015 - Item list will be read in and constructed during object's
 * initialization. Added logging for this class.
 * 22-Oct-2015 - Separate the Type into vendor specific type.
 * 27-Oct-2015 - Moved the setup function out of the Constructor, and place it
 * in static function setup. The functions to get the Illumina and Affymetrix
 * types have also been made static.
 * 11-Nov-2015 - Changed the return type of setup method.
 * 01-Dec-2015 - Removed the setup for vendor, institution and department.
 * 11-Dec-2015 - Removed unused code.
 */

@ManagedBean (name="selectOneMenuList")
@ApplicationScoped
public class SelectOneMenuList {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SelectOneMenuList.class.getName());
    // Used LinkedHashMap in order to maintains the insertion order
    private static final LinkedHashMap<String,String> affymetrixTypeList = new LinkedHashMap<>();    
    private static final LinkedHashMap<String,String> illuminaTypeList = new LinkedHashMap<>();    
    // Setup indicator
    private static Boolean setup = false;

    public SelectOneMenuList() {}
    
    // Setup the item lists using the config file passed in.
    public static Boolean setup(String uri) {
        if (!setup) {
            try (BufferedReader br = new BufferedReader(new FileReader(uri)))
            {
                String currentLine;
                String switchStr = null;

                while ((currentLine = br.readLine()) != null) {
                    if (currentLine.startsWith("#")) {
                        // Remove the # character, only want the remaining characters
                        switchStr = currentLine.substring(1);
                        // switchStr will tell us which hashmap to build on next.
                        continue;  // Read the next line
                    }
                    // As split takes in regular expression, so need to escape
                    // special character like '$'
                    String[] itemPair = currentLine.split("\\$");
                    // Only take in the values if they are in pair
                    if (itemPair.length == 2) {
                        switch(switchStr) {
                            case "AFFYMETRIX":
                                affymetrixTypeList.put(itemPair[0], itemPair[1]);
                                break;
                            case "ILLUMINA":
                                illuminaTypeList.put(itemPair[0], itemPair[1]);
                                break;
                            default:
                                // something is wrong with the item list file
                                break;
                        }
                    }
                }
            
                setup = true;
                logger.debug(uri + " loaded.");
                logger.debug(affymetrixTypeList.values());
                logger.debug(illuminaTypeList.values());
            } catch (IOException e) {
                logger.error("IOException when loading config file " + uri);
                logger.error(e.getMessage());
                return Constants.NOT_OK;
            }
        }
        
        return Constants.OK;
    }
    
    // Return the list of Affymetrix type.
    public LinkedHashMap<String,String> getAffymetrixType() 
    {   return affymetrixTypeList;        }
    public static LinkedHashMap<String,String> getAffymetrixTypeList()
    {   return affymetrixTypeList;        }

    // Return the list of Illumina type.
    public LinkedHashMap<String,String> getIlluminaType()
    {   return illuminaTypeList;    }
    public static LinkedHashMap<String,String> getIlluminaTypeList()
    {   return illuminaTypeList;    }
}
