/*
 * Copyright @2015-2016
 */
package TIMS.Bean;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * MenuBean is the backing bean for all the drop-down list box used in
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
 * 14-Jan-2016 - Renamed from SelectOneMenuList to MenuBean. Implements 
 * Serializable, and moved from General to Bean package. Removed all the static 
 * variables in MenuBean class.
 * 26-Jan-2016 - Added the setup for activity.
 */

@ManagedBean (name="menuBean")
@ApplicationScoped
public class MenuBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(MenuBean.class.getName());
    // Used LinkedHashMap in order to maintains the insertion order
    private LinkedHashMap<String,String> affymetrixTypeList;    
    private LinkedHashMap<String,String> illuminaTypeList;
    private LinkedHashMap<String,String> activityList;

    public MenuBean() {
        affymetrixTypeList = new LinkedHashMap<>();
        illuminaTypeList = new LinkedHashMap<>();
        activityList = new LinkedHashMap<>();
        ServletContext sc = (ServletContext) FacesContext.getCurrentInstance().
                getExternalContext().getContext();
        // Load the itemlist filename from context-param.
        String itemListFile = sc.getInitParameter("itemlist");
        // Setup the menu drop-down list.
        buildMenuItems(sc.getRealPath(itemListFile));
    }
    
    // Setup the item lists using the config file passed in.
    public void buildMenuItems(String uri) {
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
                        case "ACTIVITY":
                            activityList.put(itemPair[0], itemPair[1]);
                            break;
                        default:
                            // something is wrong with the item list file
                            break;
                    }
                }
            }
            
            logger.debug(uri + " loaded.");
            logger.debug(affymetrixTypeList.values());
            logger.debug(illuminaTypeList.values());
            logger.debug(activityList.values());
        } catch (IOException ioe) {
            logger.error("FAIL to load menu item list file: " + uri);
            logger.error(ioe.getMessage());
        }
    }
    
    // Return the list of Affymetrix type.
    public LinkedHashMap<String,String> getAffymetrixType() 
    {   return affymetrixTypeList;  }
    // Return the list of Illumina type.
    public LinkedHashMap<String,String> getIlluminaType()
    {   return illuminaTypeList;    }
    public LinkedHashMap<String, String> getActivityList() 
    {   return activityList;        }
}
