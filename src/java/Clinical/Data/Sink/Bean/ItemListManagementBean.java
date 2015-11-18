/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

// Libraries for Log4j
import Clinical.Data.Sink.Database.Department;
import Clinical.Data.Sink.Database.Institution;
import Clinical.Data.Sink.Database.InstitutionDB;
import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.primefaces.event.RowEditEvent;

/**
 * ItemListManagementBean is the backing bean for the itemlistmanagement view.
 * 
 * Author: Tay Wei Hong
 * Date: 16-Nov-2015
 * 
 * Revision History
 * 06-Nov-2015 - Created with all the standard getters and setters. Added new
 * method createNewInstitution() for creating new institution.
 * 18-Nov-2015 - Added one new method onInstRowEdit() to allow user to edit
 * the institution's information.
 */

@ManagedBean (name="itemListMgntBean")
@ViewScoped
public class ItemListManagementBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ItemListManagementBean.class.getName());
    // Attributes for Institution object
    private String inst_code, inst_name;
    // Attributes for Department object
    private String dept_code, dept_name;
    private List<Institution> instList;
    private List<Department> deptList;
    
    public ItemListManagementBean() {
        logger.debug("ItemListManagementBean created");
        logger.info(AuthenticationBean.getUserName() + 
                ": access Item List Management page.");
    }
    
    @PostConstruct
    public void init() {
        instList = InstitutionDB.getInstList();
    }

    // Create the new institution code.
    public String createNewInstitution() {
        FacesContext fc = getFacesContext();
        Institution newInst = new Institution(inst_code, inst_name);
        
        if (InstitutionDB.insertInstitution(newInst)) {
            logger.info(AuthenticationBean.getUserName() +
                    ": created new institution code: " + inst_code);
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "New institution code created.", ""));
        }
        else {
            logger.error("Failed to create new institution code: " + inst_code);
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Failed to create new institution code!", ""));
        }
        
        return Constants.ITEM_LIST_MANAGEMENT;
    }
    
    // Update the institution table in the database.
    public void onInstRowEdit(RowEditEvent event) {
        FacesContext fc = getFacesContext();
        
        if (InstitutionDB.updateInstitution((Institution) event.getObject())) {
            logger.info(AuthenticationBean.getUserName() + 
                    ": updated Institution.");
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Institution updated.", ""));
        }
        else {
            logger.error("Institution update failed.");
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Failed to update institution!", ""));
        }
    }
    
    // Update the department table in the database.
    public void onDeptRowEdit(RowEditEvent event) {
        logger.info(AuthenticationBean.getUserName() + 
                ": updated Department.");
    }
    
    // Retrieve the faces context
    private FacesContext getFacesContext() {
	return FacesContext.getCurrentInstance();
    }
    
    // Machine generated getters and setters
    public List<Institution> getInstList() {
        return instList;
    }
    public void setInstList(List<Institution> instList) {
        this.instList = instList;
    }
    public String getInst_code() {
        return inst_code;
    }
    public void setInst_code(String inst_code) {
        this.inst_code = inst_code;
    }
    public String getInst_name() {
        return inst_name;
    }
    public void setInst_name(String inst_name) {
        this.inst_name = inst_name;
    }
    public String getDept_code() {
        return dept_code;
    }
    public void setDept_code(String dept_code) {
        this.dept_code = dept_code;
    }
    public String getDept_name() {
        return dept_name;
    }
    public void setDept_name(String dept_name) {
        this.dept_name = dept_name;
    }   
}
