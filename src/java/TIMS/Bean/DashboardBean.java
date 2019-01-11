/*
 * Copyright @2019
 */
package TIMS.Bean;

import TIMS.Database.ActivityLogDB;
import TIMS.Database.BarChartDataObject;
import TIMS.Database.DashboardConfig;
import TIMS.Database.DashboardConfigDB;
import TIMS.Database.GroupDB;
import TIMS.Database.PieChartDataObject;
import TIMS.Database.Study;
import TIMS.Database.StudyDB;
import TIMS.Database.StudySpecificFieldDB;
import TIMS.Database.SubjectDB;
import TIMS.Database.SubjectDetail;
import TIMS.Database.UserAccount;
import TIMS.Database.UserAccountDB;
import TIMS.General.Constants;
import TIMS.General.FileHelper;
import TIMS.General.QueryStringGenerator;
import TIMS.General.ResourceRetriever;
// Libraries for Java
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.TreeSet;
// Libraries for Java Extension
import javax.annotation.PostConstruct;
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
// Library for Trove
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

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
 * 19-Nov-2018 - Implemented dashboard data source configurable module; to allow
 * admin to configure the data source for the pie chart and bar chart.
 * 29-Nov-2018 - Fix the bug found during UAT.
 * 07-Jan-2018 - Allow bar chart to plot specific field vs specific field. Sort
 * the data series of the pie chart. Reverse the data series of the horizontal 
 * bar chart, so that they appear in the correct order. Show the data tip of the
 * horizontal bar chart at 2 decimal points. When building the Specific Fields 
 * data points, don't count those entries with "--" content.
 */

@Named("DBBean")
@ViewScoped
public class DashboardBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(DashboardBean.class.getName());
    // Store the user ID of the current user.
    private final String userName;
    // This string represent the symbol for "Do Not Count' when building the
    // specific fields data points.
    private final String DNC = "--";
    private final UserAccount user;
    private String study_id, specific_fields_selection;
    private List<SubjectDetail> subjectDetailList;
    private List<SelectItem> study_list;
    private Study study_sel;
    private List<String> categories;
    private PieChartModel piechartR, piechartL;
    private BarChartModel barchartL, barchartR;
    private HorizontalBarChartModel specificFieldsBarchart;
    private TObjectIntHashMap<String> specific_fields_w_data, specific_fields_wo_data;
    private DashboardConfigDB dbc_db;
    private SubjectDB subject_db;
    private StudySpecificFieldDB ssf_db;
    // Each string in this list store the specific fields content of each subject.
    private List<String> subjects_ssf_content;
    
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
        subjects_ssf_content = new ArrayList<>();
        specific_fields_w_data = new TObjectIntHashMap<>();
        specific_fields_wo_data = new TObjectIntHashMap<>();
        
        logger.info(userName + ": access dashboard.");
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
            user.getRoleName().equals("User") ||
            user.getRoleName().equals("Guest")) {
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
    private BarChartModel createBarChartModel(LinkedHashMap<String, BarChartDataObject> 
            data_objects, String x_axis, String y_axis) {
        BarChartModel bcm = new BarChartModel();
        LinkedHashMap<String, ChartSeries> cs_hashmap = new LinkedHashMap<>();
        // Consolidate the number of distinct series name.
        Set<String> distinct_series_name = new HashSet<>();
        data_objects.forEach((key,bc_do) -> {
            // For each BarChartDataObject, add the distinct series name to the set.
            for (String series : bc_do.getSeriesName()) {
                distinct_series_name.add(series);
            }
        });
        // Create the number of ChartSeries based on the number of distinct 
        // series name.
        for (String cs_name : distinct_series_name) {
            ChartSeries cs = new ChartSeries();
            cs.setLabel(cs_name);
            // Initialise the ChartSeries with object name and default value 0.
            for (String object_name : data_objects.keySet()) {
                cs.set(object_name, 0);
            }
            cs_hashmap.put(cs_name, cs);
            // Add the chart series to the bar chart.
            bcm.addSeries(cs);
        }
        // Update the value for each object in each ChartSeries.
        for (BarChartDataObject data_object : data_objects.values()) {
            for (String series_name : data_object.getSeriesName()) {
                // Get the ChartSeries and setup its object and number.
                cs_hashmap.get(series_name).set(data_object.getData_name(), 
                        data_object.getSeriesCount(series_name));
            }
        }
        // Setup the bar chart using the series defined.
        bcm.setLegendPosition("ne");
        bcm.setLegendPlacement(LegendPlacement.OUTSIDE);
        bcm.setStacked(true);
        bcm.setShowPointLabels(true);
        bcm.setBarWidth(40);
        bcm.getAxis(AxisType.X).setLabel(x_axis);
//        bcm.getAxis(AxisType.Y).setLabel(y_axis);
        
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
        // Set extender to use javascript function sfBCExtender for this chart.
        hbcm.setExtender("sfBCExtender");
        // Set the default label for both the axis.
        hbcm.getAxis(AxisType.X).setLabel("Percentage of records");
        hbcm.getAxis(AxisType.X).setMax(120);
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
            study_sel = StudyDB.getStudyObject(study_id);
            subject_db = new SubjectDB(study_id);
            ssf_db = new StudySpecificFieldDB(study_id);
            dbc_db = new DashboardConfigDB(study_id);
            // Clean up the specific fields data points hashmaps.
            specific_fields_w_data.clear();
            specific_fields_wo_data.clear();
            // Clean up the specific field content for the subjects.
            subjects_ssf_content.clear();
            
            subjectDetailList = subject_db.getSubtDetailList();
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
            categories = ssf_db.getSpecificFieldCategory();
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
            // Setup the piechart and barchart.
            barchartL = setupBarchart(ResourceRetriever.getMsg("barchartL"));
            barchartR = setupBarchart(ResourceRetriever.getMsg("barchartR"));
            piechartL = setupPiechart(ResourceRetriever.getMsg("piechartL"));
            piechartR = setupPiechart(ResourceRetriever.getMsg("piechartR"));
            // Build the specific field content for all the subjects found in
            // this study.
            for (SubjectDetail subject : subjectDetailList) {
                subjects_ssf_content.add(subject.getSsf_content());
            }
            // Clear the current subject detail list to free up memory.
            subjectDetailList.clear();
        }
    }

    // Setup and return the PieChartModel for this chart_id.
    private PieChartModel setupPiechart(String chart_id) {
        DashboardConfig bc_config = dbc_db.getDBCForChartID(chart_id);
        PieChartModel model = new PieChartModel();
        
        if (bc_config.getData_source_x().equals("age")) {
            // Pie chart for age grouping is being constructed differently.
            model = createPieChartModel(genPieChartDOForAgeAtBaseline());
        }
        else {
            if (bc_config.is_x_from_core_data()) {
                model = createPieChartModel(genPieChartDOFromCoreData
                        (bc_config.getData_source_x(), bc_config.getTitle()));
            }
            else {
                model = createPieChartModel(genPieChartDOFromSpecificField
                        (bc_config.getData_source_x(), bc_config.getTitle()));
            }
        }
        
        return model;
    }
    
    // Setup and return the BarChartModel for this chart id.
    private BarChartModel setupBarchart(String chart_id) {
        DashboardConfig bc_config = dbc_db.getDBCForChartID(chart_id);
        BarChartModel model = new BarChartModel();
        LinkedHashMap<String, TObjectIntHashMap<String>> data_hashmap;
        
        // Currently y-axis is set to 'Number of Subjects' by default.
        if (bc_config.is_x_from_core_data()) {
            if (bc_config.is_y_from_core_data()) {
                // Core data vs core data.
                model = createBarChartModel(genBarChartDOFromDBColumnsXY
                    (bc_config.getData_source_x(), bc_config.getData_source_y()), 
                    bc_config.getLabel_x(), "Number of Subjects");
            }
            else {
                // Core data vs specific field.
                // Core Data -> (Specific Field -> Count)
                data_hashmap = buildCoreDataVsSpecificFieldDataPoints
                    (bc_config.getData_source_x(), bc_config.getData_source_y());
                model = createBarChartModel(genBarChartDOForCoreVsSF(data_hashmap),
                    bc_config.getLabel_x(), "Number of Subjects");
            }
        }
        else {
            if (bc_config.is_y_from_core_data()) {
                // Specific field vs core data i.e. inverted core data vs specific field.
                data_hashmap = buildCoreDataVsSpecificFieldDataPoints
                    (bc_config.getData_source_y(), bc_config.getData_source_x());
                model = createBarChartModel(genBarChartDOForSFVsCore(data_hashmap),
                    bc_config.getLabel_x(), "Number of Subjects");                
            }
            else {
                // Specific field vs specific field.
                data_hashmap = buildSF1VsSF2DataPoints
                    (bc_config.getData_source_x(), bc_config.getData_source_y());
                model = createBarChartModel(genBarChartDOForCoreVsSF(data_hashmap),
                    bc_config.getLabel_x(), "Number of Subjects");
            }
        }
        // Only show the y-axis i.e. number of subjects.
        model.setDatatipFormat("%2$d");
        model.setTitle(bc_config.getTitle());

        return model;
    }
    
    // Build the data points based on specific field vs specific field.
    private LinkedHashMap<String, TObjectIntHashMap<String>> 
        buildSF1VsSF2DataPoints(String sf1, String sf2) 
    {
        Set<String> dist_sf1_set = new THashSet<>();
        // Build the list of distinct value of specific field one.
        for (SubjectDetail sd : subjectDetailList) {
            if (sd.retrieveDataFromHashMap(sf1) != null &&
                !sd.retrieveDataFromHashMap(sf1).isEmpty()) {
                    dist_sf1_set.add(sd.retrieveDataFromHashMap(sf1));
            }
        }
        // Add a default category for empty field.
        dist_sf1_set.add(Constants.EMPTY_STR);
        // Remove DNC from specific field 1.
        dist_sf1_set.remove(DNC);
        // Specific Field 1 -> (Specific Field 2 -> Count)
        LinkedHashMap<String, TObjectIntHashMap<String>> data_hashmap = 
                                                        new LinkedHashMap<>();
        for (String dist_sf1 : dist_sf1_set) {
            data_hashmap.put(dist_sf1, new TObjectIntHashMap<>());
        }
        // Fill up the data points.
        for (SubjectDetail sd : subjectDetailList) {
            // Skip those DNC value in specific field 1.
            if (!sd.retrieveDataFromHashMap(sf1).equals(DNC)) {
                // Tally those empty specific field 1 under the tag EMPTY_STR.
                String SF1 = sd.retrieveDataFromHashMap(sf1).isEmpty()?
                        Constants.EMPTY_STR:sd.retrieveDataFromHashMap(sf1);
                if (sd.retrieveDataFromHashMap(sf2) != null &&
                    !sd.retrieveDataFromHashMap(sf2).isEmpty() ) 
                {
                    if (!sd.retrieveDataFromHashMap(sf2).equals(DNC)) {
                        // Remove DNC from specific field 2.
                        data_hashmap.get(SF1).adjustOrPutValue
                            (sd.retrieveDataFromHashMap(sf2), 1, 1);
                    }
                }
                else {
                    data_hashmap.get(SF1).adjustOrPutValue
                        (Constants.EMPTY_STR, 1, 1);
                }
            }
        }
        
        return data_hashmap;
    }

    // Build the data points based on core data vs specific field.
    private LinkedHashMap<String, TObjectIntHashMap<String>> 
        buildCoreDataVsSpecificFieldDataPoints(String cd_name, String sf_name) 
    {
        List<String> core_data_list = subject_db.getDistinctValueInColumn(cd_name);
        // Core Data -> (Specific Field -> Count)
        LinkedHashMap<String, TObjectIntHashMap<String>> data_hashmap = new LinkedHashMap<>();;
        for (String cd : core_data_list) {
            data_hashmap.put(cd, new TObjectIntHashMap<>());
        }
        // Fill up the data points.
        for (SubjectDetail sd : subjectDetailList) {
            if (sd.retrieveDataFromHashMap(sf_name) != null &&
                !sd.retrieveDataFromHashMap(sf_name).isEmpty() ) 
            {
                // Remove DNC from specific field.
                if (!sd.retrieveDataFromHashMap(sf_name).equals(DNC)) {
                    data_hashmap.get(sd.getCoreData(cd_name)).adjustOrPutValue
                        (sd.retrieveDataFromHashMap(sf_name), 1, 1);
                }
            }
            else {
                // Tally those empty specific field under the tag EMPTY_STR.
                data_hashmap.get(sd.getCoreData(cd_name)).adjustOrPutValue
                    (Constants.EMPTY_STR, 1, 1);
            }
        }

        return data_hashmap;
    }
    
    // Build the data points (i.e. with data, without data) for all the specific
    // fields in this study.
    private void buildSpecificFieldsDataPoints() {
        int with_data, wo_data;
        StringBuilder ssf_header = new StringBuilder("Subject ID").append(SubjectDetail.FIELD_BREAKER);
        // Go through all the categories, and retrieve the list of specific 
        // fields.
        for (String category : categories) {
            List<String> field_list = ssf_db.
                getSpecificFieldListFromCategory(category);
            // Go through the specific fields and tally the data points.
            for (String field : field_list) {
                with_data = wo_data = 0;
                for (SubjectDetail subject : subjectDetailList) {
                    if (subject.retrieveDataFromHashMap(field) == null || 
                        subject.retrieveDataFromHashMap(field).isEmpty()) {
                        wo_data++;
                    }
                    else {
                        // If the data is DNC, don't count it.
                        if (!subject.retrieveDataFromHashMap(field).equals(DNC)) {
                            with_data++;
                        }
                    }
                    // Append this data into the specific field content.
                    subject.appendSpecificField(subject.retrieveDataFromHashMap(field));
                }
                // Insert the data points for this specific field into the 
                // hashmaps.
                specific_fields_w_data.put(field, with_data);
                specific_fields_wo_data.put(field, wo_data);
                // Build the header of the specific field content.
                ssf_header.append(field).append(SubjectDetail.FIELD_BREAKER);
            }
        }
        // Add the header to the specific field content.
        subjects_ssf_content.add(ssf_header.toString());
    }
    
    // Generate the PieChartDataObject for the age_at_baseline field in the
    // subject table.
    private PieChartDataObject genPieChartDOForAgeAtBaseline() {
        PieChartDataObject pco = new PieChartDataObject("Age Group Breakdown Chart");
        List<Float> age_baseline_list = subject_db.getAgeAtBaselineList();
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
    
    // Generate the PieChartDataObject for this core data. Currently, only
    // race, gender and case control data are available for plotting.
    private PieChartDataObject genPieChartDOFromCoreData
        (String core_data, String title) {
        TObjectIntHashMap<String> series_tally = 
                            subject_db.getDistinctValueCountInColumn(core_data);
        PieChartDataObject pco = new PieChartDataObject(title);
        // Iterate through the series tally and fill up the pie chart data object.
        for (TObjectIntIterator it = series_tally.iterator(); it.hasNext();) {
            it.advance();
            // Add this series and its tally into piechart data object.
            pco.addSeries((String) it.key(), it.value());            
        }
        
        return pco;
    }
    
    // Generate the PieChartDataObject for this specific field in the hashmap of
    // the subject detail. The type of data should be category based e.g. the 
    // available data value should be limited.
    private PieChartDataObject genPieChartDOFromSpecificField
        (String field, String title) 
    {
        TObjectIntHashMap<String> series_tally = new TObjectIntHashMap<>();
        // Tally the count for each unique series name for this field.
        for (SubjectDetail subject : subjectDetailList) {
            if (!subject.retrieveDataFromHashMap(field).equals(DNC)) {
                // Remove DNC.
                series_tally.adjustOrPutValue(subject.
                        retrieveDataFromHashMap(field), 1, 1);
            }
        }
        
        PieChartDataObject pco = new PieChartDataObject(title);
        // Sort the series name before adding them to the piechart series.
        Set<String> value_set = series_tally.keySet();
        TreeSet<String> sorted_set = new TreeSet<>();
        sorted_set.addAll(value_set);
        // Loop through the sorted set and add them to the pie chart series.
        for (String key : sorted_set) {
            // Add this series and its tally into piechart data object.
            pco.addSeries(key, series_tally.get(key));
        }

        return pco;
    }
    
    // Generate the hashmap of BarChartDataObject using the values from database
    // columns X and Y. Column X values will plot on the x-axis, and Y values
    // will be plot on the y-axis.
    private LinkedHashMap<String, BarChartDataObject> genBarChartDOFromDBColumnsXY
        (String colX, String colY) 
    {
        // x_values will store the list of data name for this chart.
        List<String> x_values = subject_db.getDistinctValueInColumn(colX);
        // series_set will store the list of series name for this chart.
        List<String> series_set = subject_db.getDistinctValueInColumn(colY);
        // Create THashMap<String, List<String>> where the first string will
        // store the data name and the list of strings will store the series 
        // values.
        THashMap<String, List<String>> x2yListValue = new THashMap<>();
        
        for (String data_name : x_values) {
            // data_name - List of series values
            x2yListValue.put(data_name, subject_db.getColXBasedOnColYValue
                            (colY, colX, data_name));
        }
        // do_hashmap will store the data object(s) for this chart.
        LinkedHashMap<String, BarChartDataObject> do_hashmap = new LinkedHashMap<>();
        
        for (String data_name : x_values) {
            // Create the data object using the x_values as it's data_name.
            BarChartDataObject bco = new BarChartDataObject(data_name);
            // Add in all the unique Y values as the chart series.
            for (String series : series_set) {
                bco.addSeries(series);
            }
            // Add this data object into the data object hashmap.
            do_hashmap.put(data_name, bco);
        }
        // Tally the series count.
        for (String data_name : x2yListValue.keySet()) {
            List<String> series_values = x2yListValue.get(data_name);
            for (String series : series_values) {
                do_hashmap.get(data_name).increSeriesCount(series);
            }
        }

        return do_hashmap;
    }

    // Generate the hashmap of BarChartDataObject using the values from core
    // data and specific field. Core data values will be plot on the x-axis, and 
    // specific field values will be plot on the y-axis.
    private LinkedHashMap<String, BarChartDataObject> genBarChartDOForCoreVsSF
        (LinkedHashMap<String, TObjectIntHashMap<String>> data_hashmap) 
    {
        LinkedHashMap<String, BarChartDataObject> do_hashmap = new LinkedHashMap<>();
        
        for (String core_data : data_hashmap.keySet()) {
            BarChartDataObject dat = new BarChartDataObject(core_data);
            for (TObjectIntIterator it = data_hashmap.get(core_data).iterator(); 
                    it.hasNext(); ) 
            {
                it.advance();
                dat.addSeries((String) it.key(), it.value());
            }
            if (dat.getNumOfSeriesDefined() > 0) {
                do_hashmap.put(core_data, dat);
            }
        }

        return do_hashmap;
    }
    
    // Generate the hashmap of BarChartDataObject using the values from specific
    // field and core data. Specific field values will be plot on the x-axis,
    // and core data values will be plot on the y-axis.
    private LinkedHashMap<String, BarChartDataObject> genBarChartDOForSFVsCore
        (LinkedHashMap<String, TObjectIntHashMap<String>> data_hashmap) 
    {
        LinkedHashMap<String, BarChartDataObject> do_hashmap = new LinkedHashMap<>();
        // Get all the unique specific fields.
        Set<String> specific_fields = new THashSet<>();
        
        for (String core_data : data_hashmap.keySet()) {
            for (TObjectIntIterator it = data_hashmap.get(core_data).iterator(); 
                    it.hasNext(); ) 
            {
                it.advance();
                specific_fields.add((String) it.key());
            }
        }
        // Create the hashmap of BarChartDataObject based on the number of
        // specific fields.
        for (String sField : specific_fields) {
            BarChartDataObject dat = new BarChartDataObject(sField);
            do_hashmap.put(sField, dat);
        }
        // Fill in the BarChartDataObject's series and its value..
        for (String core_data : data_hashmap.keySet()) {
            for (TObjectIntIterator it = data_hashmap.get(core_data).iterator(); 
                    it.hasNext(); ) 
            {
                it.advance();
                do_hashmap.get((String) it.key()).addSeries(core_data, it.value());
            }
        }
        
        return do_hashmap;
    }
    
    // Field category has changed, update the specific fields chart accordingly.
    public void updateSpecificFieldsBarchart() {
        // Retrieve the list of specific fields based on the category selected.
        List<String> field_list = ssf_db.getSpecificFieldListFromCategory
                                    (specific_fields_selection);
        if (field_list.size() > 15) {
            // Limit the number of specific fields to 15 per chart.
            field_list = field_list.subList(0, 15);
        }
        // data_object_hashmap will store the data object(s) for this chart.
        LinkedHashMap<String, BarChartDataObject> data_object_hashmap = 
                                                        new LinkedHashMap<>();
        // Need to iterate through the specific field list in reverse, so that
        // the list will appear in the correct order in the barchart.
        ListIterator li = field_list.listIterator(field_list.size());
        String field = "";
        while (li.hasPrevious()) {
            field = (String) li.previous();
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
    
    // Build the specific field content for the subjects under this study; for
    // user to download.
    public void downloadSsfContent() {
        StringBuilder ssf_file = new StringBuilder(Constants.getSYSTEM_PATH()).
                append(Constants.getTMP_PATH()).append(study_id).
                append("_SSF_").append(Constants.getDT_yyyyMMdd_HHmm()).
                append(Constants.getOUTPUTFILE_EXT());
        if (FileHelper.generateTextFile(subjects_ssf_content, ssf_file.toString())) {
            ActivityLogDB.recordUserActivity(userName, Constants.DWL_FIL, 
                                    "Specific field content of " + study_id);
            FileHelper.download(ssf_file.toString());
            // Delete the Meta Data List after download.
            FileHelper.delete(ssf_file.toString());
        }
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
