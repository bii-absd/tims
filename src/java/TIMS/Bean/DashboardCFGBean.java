/*
 * Copyright @2018
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.DashboardConfig;
import TIMS.Database.DashboardConfigDB;
import TIMS.Database.StudyDB;
import TIMS.Database.StudySpecificFieldDB;
import TIMS.General.Constants;
// Library for Java
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
// Libraries for Java Extension
import javax.inject.Named;
import javax.faces.context.FacesContext;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;
// Library for primefaces
import org.primefaces.event.RowEditEvent;

/**
 * DashboardCFGBean is the backing bean for the dashboard configuration view.
 * 
 * Author: Tay Wei Hong
 * Created on: 09-Nov-2018
 * 
 * Revision History
 * 09-Nov-2018 - Implemented dashboard data source configurable module.
 */

@Named("DBCFGBean")
@ViewScoped
public class DashboardCFGBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DashboardCFGBean.class.getName());
    // Store the user ID of the current user.
    private final String user_name;
    private final List<String> core_data_options;
    private String study_id, chart_id,title, data_source_x, data_source_y;
    private boolean inverted;
    private LinkedHashMap<String, String> study_list, data_source_hashmap, 
                                data_source_w_age_hashmap, core_data_hashmap;
    private List<DashboardConfig> dbc_list;
    private DashboardConfigDB dbc_db;
    private StudySpecificFieldDB ssf_db;
    
    public DashboardCFGBean() {
        user_name = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        core_data_options = Arrays.asList("race","gender","casecontrol");
        data_source_hashmap = new LinkedHashMap<>();
        data_source_w_age_hashmap = new LinkedHashMap<>();
        core_data_hashmap = new LinkedHashMap<>();
        
        logger.info(user_name + ": access dashboard configuration.");
    }
    
    @PostConstruct
    public void init() {
        study_list = StudyDB.getAllStudyHash();
        for (String core_data : core_data_options) {
            core_data_hashmap.put(core_data, core_data);
        }
    }
    
    // A new study has been selected by the user, need to rebuild the list of
    // dashboardconfig objects.
    public void studyChange() {
        if (study_id.compareTo("0") != 0) {
            dbc_db = new DashboardConfigDB(study_id);
            ssf_db = new StudySpecificFieldDB(study_id);
            dbc_list = dbc_db.getDashboardConfigList();
            init_data_source();
        }
    }
    
    // Initialise the data source hashmaps using the values from core data and
    // specific fields.
    private void init_data_source() {
        // Include the core data option before adding in the specific field options.
        data_source_hashmap.clear();
        data_source_hashmap.putAll((LinkedHashMap<String,String>) core_data_hashmap.clone());
        data_source_hashmap.putAll(ssf_db.getSpecificFieldHashMap());
        // w_age_hashmap = "Age" + "_hashmap"
        data_source_w_age_hashmap.clear();
        data_source_w_age_hashmap.put("age", "age");
        data_source_w_age_hashmap.putAll((LinkedHashMap<String,String>) data_source_hashmap.clone());
    }
    
    // Update dashboard_config in database.
    public void onDBCEdit(RowEditEvent event) {
        FacesContext fc = FacesContext.getCurrentInstance();
        
        if (dbc_db.updateDBC((DashboardConfig) event.getObject())) {
            // Record this dashboard configuration update activity into database.
            StringBuilder detail = new StringBuilder("Data source for ").
                    append(((DashboardConfig) event.getObject()).getChart_id()).
                    append(" under ").append(study_id);
            ActivityLogDB.recordUserActivity(user_name, Constants.UPD_DS, detail.toString());
            StringBuilder oper = new StringBuilder(user_name).append(": updated ").append(detail);
            logger.info(oper);
            // Refresh the dashboard configurations list.
            dbc_list = dbc_db.getDashboardConfigList();
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Dashboard data source updated.", ""));
        }
        else {
            logger.error("FAIL to update dashboard configuration!");
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Failed to update dashboard data source!", ""));
        }
    }
    
    // A study has been selected if it is not equal to null and "0".
    public boolean getStudySelectedStatus() {
        if (study_id != null) {
            if (study_id.compareTo("0") != 0) {
                // A study is selected.
                return true;
            }
        }        
        return false;
    }
    
    // Machine generated getters and setters.
    public LinkedHashMap<String, String> getStudy_list() {
        return study_list;
    }
    public LinkedHashMap<String, String> getCore_data_hashmap() {
        return core_data_hashmap;
    }
    public LinkedHashMap<String, String> getData_source_hashmap() {
        return data_source_hashmap;
    }
    public LinkedHashMap<String, String> getData_source_w_age_hashmap() {
        return data_source_w_age_hashmap;
    }
    public List<DashboardConfig> getDbc_list() {
        return dbc_list;
    }
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
}
