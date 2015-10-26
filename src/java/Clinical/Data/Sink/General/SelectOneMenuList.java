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
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
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
 */

@ManagedBean (name="selectOneMenuList")
@ApplicationScoped
public class SelectOneMenuList {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(SelectOneMenuList.class.getName());
    // Used LinkedHashMap in order to maintains the insertion order
    private static final LinkedHashMap vendorList = new LinkedHashMap();
    private static final LinkedHashMap affymetrixTypeList = new LinkedHashMap();    
    private static final LinkedHashMap illuminaTypeList = new LinkedHashMap();    
    private static final LinkedHashMap institutionList = new LinkedHashMap();
    private static final LinkedHashMap departmentList = new LinkedHashMap();

    public SelectOneMenuList() {
        // Construct the dropdown list during first initialization.
        ServletContext context = getServletContext();
        // Load the itemlist filename from context-param
        String itemListFile = context.getInitParameter("itemlist");
        // Get the real path of the itemListFile
        String realPath = context.getRealPath(itemListFile);
        logger.debug("Item list file located at: " + realPath);
        
        try (BufferedReader br = new BufferedReader(new FileReader(realPath)))
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
                        case "VENDOR":
                            vendorList.put(itemPair[0], itemPair[1]);
                            break;
                        case "AFFYMETRIX":
                            affymetrixTypeList.put(itemPair[0], itemPair[1]);
                            break;
                        case "ILLUMINA":
                            illuminaTypeList.put(itemPair[0], itemPair[1]);
                            break;
                        case "INSTITUTION":
                            institutionList.put(itemPair[0], itemPair[1]);
                            break;
                        case "DEPARTMENT":
                            departmentList.put(itemPair[0], itemPair[1]);
                            break;
                        default:
                            // something is wrong with the item list file
                            break;
                    }
                }
            }
            
            logger.debug(realPath + " loaded.");
            logger.debug(vendorList.values());
            logger.debug(affymetrixTypeList.values());
            logger.debug(illuminaTypeList.values());
            logger.debug(institutionList.values());
            logger.debug(departmentList.values());
        } catch (IOException e) {
                logger.error("IOException encountered while loading " + 
                        itemListFile);
                logger.error(e.getMessage());
        }
    }
    
    // getVendorTypes will return the list of vendors.
    public LinkedHashMap<String,String> getVendorTypes() 
    {   return vendorList;      }
    
    // getAffymetrixType will return the list of Affymetrix type.
    public LinkedHashMap<String,String> getAffymetrixType() 
    {   return affymetrixTypeList;        }

    // getIlluminaType will return the list of Illumina type.
    public LinkedHashMap<String,String> getIlluminaType()
    {   return illuminaTypeList;    }
    
    // getInstitution will return the list of institution.
    public LinkedHashMap<String,String> getInstitution() 
    {   return institutionList; }
    
    // getDepartment will return the list of department
    public LinkedHashMap<String,String> getDepartment() 
    {   return departmentList;  }
    
    // Retrieve the servlet context
    private ServletContext getServletContext() {
        return (ServletContext) FacesContext.getCurrentInstance().
                getExternalContext().getContext();
    }    
}
