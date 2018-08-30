/*
 * Copyright @2018
 */
package TIMS.Bean;

import TIMS.Database.BarChartDataObject;
import TIMS.Database.GroupDB;
import TIMS.Database.PieChartDataObject;
import TIMS.Database.Study;
import TIMS.Database.StudyDB;
import TIMS.Database.SubjectDB;
import TIMS.Database.SubjectDetail;
import TIMS.Database.UserAccount;
import TIMS.Database.UserAccountDB;
import TIMS.General.FileHelper;
import TIMS.General.QueryStringGenerator;
// Libraries for Java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
//import javax.faces.bean.ManagedBean;
//import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.inject.Named;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
// Library for omnifaces
import org.omnifaces.cdi.ViewScoped;
// Libraries for Primefaces
import org.primefaces.model.chart.PieChartModel;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.HorizontalBarChartModel;
import org.primefaces.model.chart.LegendPlacement;

/**
 * DashboardBean is the backing bean for the dashboard view.
 * 
 * Author: Tay Wei Hong
 * Created on: 25-Jun-2018
 * 
 * Revision History
 * 25-Jun-2018 - Implemented Dashboard module.
 * 13-Aug-2018 - Re-design the way the specific fields barchart is being 
 * generated. The subjectDetailList will be clear once it is no longer needed 
 * (to free up memory space). To check for empty age_at_baseline data in method 
 * genPieChartDOForAgeAtBaseline(). To hide the series info from the barchart
 * tool-tip when mouse-over.
 * 28-Aug-2018 - To replace JSF managed bean with CDI, and JSF ViewScoped with
 * omnifaces's ViewScoped.
 */

//@ManagedBean (name = "DBBean")
@Named("DBBean")
@ViewScoped
public class DashboardBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DashboardBean.class.getName());
    // Store the user ID of the current user.
    private final String userName;
    private final UserAccount user;
    private String study_id, specific_fields_selection;
    private List<SubjectDetail> subjectDetailList;
    private List<SelectItem> study_list;
    private Study study_sel;
    private List<String> categories;
    private PieChartModel piechartR, piechartL;
    private BarChartModel barchartL, barchartR;
    private HorizontalBarChartModel specificFieldsBarchart;
    private HashMap<String, Integer> specific_fields_w_data, 
                                     specific_fields_wo_data;
    
    
    public DashboardBean() {
        userName = (String) FacesContext.getCurrentInstance().
                getExternalContext().getSessionMap().get("User");
        user = UserAccountDB.getUserAct(userName);
        piechartR = new PieChartModel();
        piechartL = new PieChartModel();
        barchartR = new BarChartModel();
        barchartL = new BarChartModel();
        specificFieldsBarchart = new HorizontalBarChartModel();
        categories = new ArrayList<>();
        subjectDetailList = new ArrayList<>();
        specific_fields_w_data = new HashMap<>();
        specific_fields_wo_data = new HashMap<>();
        
        logger.debug(userName + ": access dashboard.");
    }
    
    @PostConstruct
    public void init() {
        // Retrieve the list of studies that this user are allowed to review 
        // based on his role.
        String groupQuery = QueryStringGenerator.genGrpQuery4Review(user);
        study_list = buildStudyListSelectItems(groupQuery);        
    }

    // Construct the list of select items found in the drop-down list for study
    // selection.
    private List<SelectItem> buildStudyListSelectItems(String grpQuery) {
        List<SelectItem> item_list = new ArrayList<>();
        List<String> grpIdList = new ArrayList<>();
        // Retrieve the group IDs that come under this user (based on it's role)
        if (user.getRoleName().equals("Admin") || 
            user.getRoleName().equals("User")) {
            grpIdList.add(user.getUnit_id());
        }
        else {
            grpIdList = GroupDB.getGrpIDList(grpQuery);
        }
        // Go through the groups and retrieve the studies that belong to each
        // group.
        for (String grp : grpIdList) {
            List<String> grpStudies = StudyDB.getDashboardStudyUnderGroup(grp);
            // Only create the grouping if this group owns any study.
            if (!grpStudies.isEmpty()) {
                SelectItemGroup itemGroup = new SelectItemGroup(grp);
                SelectItem[] itemArray = new SelectItem[grpStudies.size()];
                int index = 0;
                // Create one SelectItem for each study, and add them to the
                // array.
                for (String study : grpStudies) {
                    itemArray[index++] = new SelectItem(study, study);
                }
                // Add the array of SelectItem to the group, then add the group
                // to the list.
                itemGroup.setSelectItems(itemArray);
                item_list.add(itemGroup);
            }
        }
        
        return item_list;
    }
    
    // Create a pie chart based on the pie chart data object passed in.
    private PieChartModel createPieChartModel(PieChartDataObject pie) {
        PieChartModel pcm = new PieChartModel();

        // Setup the number for each category defined in the pie chart data object.
        for (String category : pie.getSeriesName()) {
            pcm.set(category, pie.getSeriesCount(category));
        }
        pcm.setTitle(pie.getTitle());
        pcm.setLegendPosition("ne");
        pcm.setShowDataLabels(true);
        
        return pcm;
    }
    
    // Create a bar chart model based on the hashmap of chart data objects
    // passed in.
    private BarChartModel createBarChartModel(HashMap<String, BarChartDataObject> 
            data_objects, String x_axis, String y_axis) {
        BarChartModel bcm = new BarChartModel();
        Map.Entry<String, BarChartDataObject> entry = 
                data_objects.entrySet().iterator().next();
        LinkedHashMap<String, ChartSeries> cs_hashmap = new LinkedHashMap<>();
        // Create the number of chart series defined.
        for (String cs_name : entry.getValue().getSeriesName()) {
            ChartSeries cs = new ChartSeries();
            cs.setLabel(cs_name);
            cs_hashmap.put(cs_name, cs);
            // Add the chart series to the bar chart.
            bcm.addSeries(cs);
        }
        // Now set the number for each data_name in each series.
        for (BarChartDataObject data_object : data_objects.values()) {
            for (String series_name : data_object.getSeriesName()) {
                // Get the ChartSeries and setup its object and number.
                cs_hashmap.get(series_name).set(data_object.getData_name(), 
                        data_object.getSeriesCount(series_name));
            }
        }
        // Setup the bar chart using the series defined.
        bcm.setLegendPosition("ne");
        bcm.setStacked(true);
        bcm.setShowPointLabels(true);
        bcm.getAxis(AxisType.X).setLabel(x_axis);
        bcm.getAxis(AxisType.Y).setLabel(y_axis);
        
        return bcm;
    }
    
    // Create a horizontal bar chart model based on the hashmap of chart data 
    // objects passed in.
    private HorizontalBarChartModel createHorizontalBarChartModel
        (LinkedHashMap<String, BarChartDataObject> data_objects) 
    {
        HorizontalBarChartModel hbcm = new HorizontalBarChartModel();
        Map.Entry<String, BarChartDataObject> entry = 
                data_objects.entrySet().iterator().next();
        LinkedHashMap<String, ChartSeries> cs_hashmap = new LinkedHashMap<>();
        // Create the number of chart series defined.
        for (String cs_name : entry.getValue().getSeriesName()) {
            ChartSeries cs = new ChartSeries();
            cs.setLabel(cs_name);
            cs_hashmap.put(cs_name, cs);
            // Add the chart series to the bar chart.
            hbcm.addSeries(cs);
        }
        // Now set the number for each data_name in each series.
        for (BarChartDataObject data_object : data_objects.values()) {
            for (String series_name : data_object.getSeriesName()) {
                // Get the ChartSeries and setup its object and number.
                cs_hashmap.get(series_name).set(data_object.getData_name(), 
                        data_object.getSeriesCount(series_name));
            }
        }
        // Setup the bar chart using the series defined.
        hbcm.setLegendPosition("ne");
        hbcm.setStacked(true);
        hbcm.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
        hbcm.setShowPointLabels(true);
        hbcm.setAnimate(true);
        // Set the default label for both the axis.
        hbcm.getAxis(AxisType.X).setLabel("Percentage of records");
        hbcm.getAxis(AxisType.Y).setLabel("Study Specific Fields");
        
        return hbcm;
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

    // A new study has been selected by the user, need to rebuild the charts.
    public void studyChange() {
        if (study_id.compareTo("0") != 0) {
            logger.info("Study ID: " + study_id);
            study_sel = StudyDB.getStudyObject(study_id);
            // Clean up the specific fields data points hashmaps.
            specific_fields_w_data.clear();
            specific_fields_wo_data.clear();
            
            subjectDetailList = SubjectDB.getSubtDetailList(study_id);
            List<String> colNameL = FileHelper.convertByteArrayToList
                                        (StudyDB.getColumnNameList(study_id));
            // Convert the meta data in each subject detail object from byte[]  
            // to hashmap for easy access.
            for (SubjectDetail subject : subjectDetailList) {
                if (!subject.convertDataToHashMap(colNameL)) {
                    logger.error("FAIL to convert subject data to hashmap!");
                    // Might need to raise an exception to flag the error here.
                    break;
                }
            }
            categories = StudyDB.getSpecificFieldCategoryFromStudy(study_id);
            // If categories is empty, it means study specific fields have not
            // been setup.
            if (!categories.isEmpty()) {
                // By default, the first category will be selected.
                specific_fields_selection = categories.get(0);
                // Build the specific fields data points hashmaps.
                buildSpecificFieldsDataPoints();
                // Update study specific fields barchart.
                updateSpecificFieldsBarchart();
            }
            // The barchart at the left plot race against gender.
            barchartL = createBarChartModel(genBarChartDOFromDBColumnsXY
                 ("race", "gender", study_id), "race", "Number of Subjects");
            // Only show the y-axis i.e. number of subjects.
            barchartL.setDatatipFormat("%2$d");
            // The barchart at the right plot race against casecontrol.
            barchartR = createBarChartModel(genBarChartDOFromDBColumnsXY
                 ("race", "casecontrol", study_id), "race", "Number of Subjects");
            // Only show the y-axis i.e. number of subjects.
            barchartR.setDatatipFormat("%2$d");
            // The piechart at the left plot the age group distribution chart.
            piechartL = createPieChartModel(genPieChartDOForAgeAtBaseline());
            // The piechart at the right plot the distribution of visit type.
            piechartR = createPieChartModel(genPieChartDOFromSpecificField
                                ("ContacType", "Visit Type Breakdown Chart"));
            // Clear the current subject detail list to free up memory.
            subjectDetailList.clear();
        }
    }

    // Build the data points (i.e. with data, without data) for all the specific
    // fields in this study.
    private void buildSpecificFieldsDataPoints() {
        int with_data, wo_data;
        // Go through all the categories, and retrieve the list of specific 
        // fields.
        for (String category : categories) {
            List<String> field_list = StudyDB.
                getSpecificFieldListFromStudyCategory(study_id, category);
            // Go through the specific fields and tally the data points.
            for (String field : field_list) {
                with_data = wo_data = 0;
                for (SubjectDetail subject : subjectDetailList) {
                    if (subject.retrieveDataFromHashMap(field) == null || 
                        subject.retrieveDataFromHashMap(field).isEmpty()) {
                        wo_data++;
                    }
                    else {
                        with_data++;
                    }
                }
                // Insert the data points for this specific field into the 
                // hashmaps.
                specific_fields_w_data.put(field, with_data);
                specific_fields_wo_data.put(field, wo_data);
            }
        }
    }
    
    // Generate the PieChartDataObject for the age_at_baseline field in the
    // subject table.
    private PieChartDataObject genPieChartDOForAgeAtBaseline() {
        PieChartDataObject pco = new PieChartDataObject("Age Group Breakdown Chart");
        List<Float> age_baseline_list = SubjectDB.getAgeAtBaselineList(study_id);
        // Make sure there is data available for further computation, else just
        // return a default piechart.
        if (age_baseline_list.isEmpty()) {
            logger.debug("No data is available for age_at_baseline!");
            pco.addSeries("INVALID AGE DATA", 1);
            return pco;
        }
        
        int floor = (int) Math.floor(age_baseline_list.get(0));
        int ceiling = (int) Math.ceil(age_baseline_list.get
                                     (age_baseline_list.size()-1));
        // Only want 8 pie in the piechart.
        int step = (ceiling - floor) / 7;
        if (step == 0) {
            // If the age range is very narrow i.e. less than 7, default the
            //  step size to 1.
            step = 1;
        }
        int lower_bound = floor;
        int upper_bound = 0;
        // Keep creating the age grouping until the upper_bound is greater than
        // the ceiling.
        while (upper_bound < ceiling) {
            upper_bound = lower_bound + step;
            int count = 0;
            // index must be less than the list size to avoid index out of
            // bound exception.
            for (int index=0; index<age_baseline_list.size(); index++ ) {
                if (age_baseline_list.get(index) < upper_bound) {
                    count++;
                }
                else {
                    // Break out of this loop once the current age is equal
                    // or greater than this age group upper bound.
                    break;
                }
            }
            // Store this age grouping and it's count.
            String age_group = lower_bound + "-" + upper_bound;
            pco.addSeries(age_group, count);
            // Remove those entries from the list once they have been counted.
            if (count < age_baseline_list.size()) {
                age_baseline_list = age_baseline_list.subList
                                        (count, age_baseline_list.size());
            }
            lower_bound = upper_bound;
        }
            
        return pco;
    }
    
    // Generate the PieChartDataObject for this specific field in the hashmap of
    // the subject detail. The type of data should be category based e.g. the 
    // available data value should be limited.
    private PieChartDataObject genPieChartDOFromSpecificField
        (String field, String title) 
    {
        LinkedHashMap<String, Integer> series_tally = new LinkedHashMap<>();
        // Tally the count for each unique series name for this field.
        for (SubjectDetail subject : subjectDetailList) {
            String series = subject.retrieveDataFromHashMap(field);
            if (series_tally.containsKey(series)) {
                // Series is already in the tally; increment by one.
                series_tally.put(series, series_tally.get(series)+1);
            }
            else {
                // This is a new series, add it to the tally with an intial
                // count of 1.
                series_tally.put(series, 1);
            }
        }
        
        PieChartDataObject pco = new PieChartDataObject(title);
        // Iterate through the series tally and fill up the piechart data object.
        for (Map.Entry series : series_tally.entrySet()) {
            // Add this series and its tally into piechart data object.
            pco.addSeries((String) series.getKey(), (int) series.getValue());
        }
        
        return pco;
    }
    
    // Generate the hashmap of BarChartDataObject using the values from database
    // columns X and Y. Column X values will plot on the x-axis, and Y values
    // will be plot on the y-axis.
    private HashMap<String, BarChartDataObject> genBarChartDOFromDBColumnsXY
        (String colX, String colY, String study) 
    {
        // x_values will store the list of data name for this chart.
        List<String> x_values = SubjectDB.getDistinctValueInColumn(colX, study);
        // series_set will store the list of series name for this chart.
        List<String> series_set = SubjectDB.getDistinctValueInColumn(colY, study);
        // Create HashMap<String, List<String>> where the first string will
        // store the data name and the list of strings will store the series 
        // values.
        LinkedHashMap<String, List<String>> x2yListValue_hashmap = 
                                                        new LinkedHashMap<>();
        for (String data_name : x_values) {
            // data_name - List of series values
            x2yListValue_hashmap.put(data_name, SubjectDB.getColXBasedOnColYValue
                                    (colY, colX, data_name, study));
        }
        // data_object_hashmap will store the data object(s) for this chart.
        LinkedHashMap<String, BarChartDataObject> data_object_hashmap = 
                                                        new LinkedHashMap<>();
        for (String data_name : x_values) {
            // Create the data object using the x_values as it's data_name.
            BarChartDataObject bco = new BarChartDataObject(data_name);
            // Add in all the unique Y values as the chart series.
            for (String series : series_set) {
                bco.addSeries(series);
            }
            // Add this data object into the object hashmap.
            data_object_hashmap.put(data_name, bco);
        }
        // Tally the series count.
        Iterator it = x2yListValue_hashmap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry data = (Map.Entry)it.next();
            @SuppressWarnings("unchecked")
            List<String> series_values = (List<String>) data.getValue();
            String data_name = (String) data.getKey();
            
            for (String series : series_values) {
                // Loop through the series values of each data_name and 
                // increment the respective series.
                data_object_hashmap.get(data_name).increSeriesCount(series);
            }
        }
        
        return data_object_hashmap;
    }

    // Field category has changed, update the specific fields chart accordingly.
    public void updateSpecificFieldsBarchart() {
        // Retrieve the list of specific fields based on the category selected.
        List<String> field_list = StudyDB.getSpecificFieldListFromStudyCategory
                                    (study_id, specific_fields_selection);
        if (field_list.size() > 15) {
            // Limit the number of specific fields to 15 per chart.
            field_list = field_list.subList(0, 15);
        }
        // data_object_hashmap will store the data object(s) for this chart.
        LinkedHashMap<String, BarChartDataObject> data_object_hashmap = 
                                                        new LinkedHashMap<>();
        for (String field : field_list) {
            BarChartDataObject dat = new BarChartDataObject(field);
            dat.addSeries("Records with data", specific_fields_w_data.get(field));
            dat.addSeries("Records without data", specific_fields_wo_data.get(field));
            // Convert the series count to percentage form.
            dat.convertSeriesCountToPercentage();
            data_object_hashmap.put(field, dat);
        }
        // Update the specific fields barchart.
        specificFieldsBarchart = createHorizontalBarChartModel(data_object_hashmap);
        // Only show the x-axis i.e. percentage of records.
        specificFieldsBarchart.setDatatipFormat("%1$d");
        logger.info("Category: " + specific_fields_selection);
    }
    
    // Check whether study specific fields have been setup or not; use to 
    // control the rendering of the specific fields barchart.
    public boolean isCategoriesEmpty() {
        return categories.isEmpty();
    }
    
    // Machine generated getters and setters.
    public Study getStudy_sel() {
        return study_sel;
    }
    public String getSpecific_fields_selection() {
        return specific_fields_selection;
    }
    public void setSpecific_fields_selection(String specific_fields_selection) {
        this.specific_fields_selection = specific_fields_selection;
    }
    public PieChartModel getPiechartL() {
        return piechartL;
    }
    public PieChartModel getPiechartR() {
        return piechartR;
    }
    public BarChartModel getBarchartL() {
        return barchartL;
    }
    public BarChartModel getBarchartR() {
        return barchartR;
    }
    public HorizontalBarChartModel getSpecificFieldsBarchart() {
        return specificFieldsBarchart;
    }    
    public String getStudy_id() {
        return study_id;
    }
    public void setStudy_id(String study_id) {
        this.study_id = study_id;
    }
    public List<SelectItem> getStudy_list() {
        return study_list;
    }
    public List<String> getCategories() {
        return categories;
    }
}
