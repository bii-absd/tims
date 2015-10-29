/*
 * Copyright @2015
 */
package Clinical.Data.Sink.Bean;

import Clinical.Data.Sink.General.Constants;
import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
// Libraries for Log4j
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ArrayConfigBean is used as the backing bean for the array.xhtml and 
 arrayConfigReview.xhtml views.
 * 
 * Author: Tay Wei Hong
 * Date: 22-Sep-2015
 * 
 * Revision History
 * 22-Sep-2015 - Created with all the standard getters and setters.
 * 30-Sep-2015 - Added in the methods for reading in the user inputs for the
 * pipeline configuration.
 * 02-Oct-2015 - Added in 2 extra variables to store the submit time. Refactor
 * the methods involved in printing config, excute, and submit. Added in the
 * comments for the code.
 * 05-Oct-2015 - Create the directory for the output file if this is a new user.
 * Added in the temporary code to create the output file.
 * 06-Oct-2015 - To keep the job_id for all newly inserted job request (needed
 * for further processing). Added Log4j2 for this class. Fix a bug found when
 * testing whether inputFile is null or not.
 * 07-Oct-2015 - Added in 3 new variables (type, filterType & columnAverage) to
 * capture the user configuration inputs.
 * 08-Oct-2015 - Store the job_id of the inserted record. To handle the status
 * returned by the helper functions during executeJob. Removed qualityPlot and 
 * "Differential expression analysis" heading. Moved column to "Processing
 * Parameters".
 * 09-Oct-2015 - Added one new method executePipeline. Added in the code to
 * simulate the running of pipeline.
 * 12-Oct-2015 - Added one helper method createSystemDirectory to help create
 * the directory needed for storing system output file. Change the way the 
 * output file is being created (will created it based on the output from the 
 * process). Log the exception message. Implemented pipeline process exit 
 * detection module. Added job_id field during insertion into database.
 * 15-Oct-2015 - Critical error handling.
 * 21-Oct-2015 - Pipeline command(s) will be read from setup file. Changed the
 * system directory structure.
 * 22-Oct-2015 - Removed the Vendor variable from the config file. Will keep the
 * variable in this class for now.
 * 23-Oct-2015 - Removed the genReport,mpsFile & probeAntFile variables from the 
 * config file (will keep these variable in this class for now). Added 
 * pipelineReport to store the filepath of the pipeline report that will be 
 * generated. Pipeline report module.
 */

@ManagedBean (name="arrayConfigBean")
@ViewScoped
public class ArrayConfigBean implements Serializable {
    // Get the logger for Log4j
    private final static Logger logger = LogManager.
            getLogger(ArrayConfigBean.class.getName());
    // Input Parameters
    private String studyID, type;
    // Processing Parameters
    private String probeFilter, normalization, phenotype;
    private List<String> probeFilters;
    private boolean probeSelect;
    // Further Processing
    private String average, stdLog2Ratio;
    // Pipeline name
    private static String pipelineName;
    // Pipeline type
    private static String pipelineType;
    // To store the 3 kind of input files that will be uploaded by the users
    private FileUploadBean inputFile, sampleFile, ctrlFile;

    public ArrayConfigBean() {}

    @PostConstruct
    public void initFiles() {
        if (pipelineType.compareTo(Constants.GEX_ILLUMINA) == 0) {
            // Only Illumina pipeline required the user to upload the control file
            ctrlFile = new FileUploadBean();            
        }
        inputFile = new FileUploadBean();
        sampleFile = new FileUploadBean();
    }
    
    // If any of the input files is not uploaded, user is not allowed to
    // submit the job for execution.
    public Boolean allowToSubmitJob() {
        if (pipelineType.compareTo(Constants.GEX_ILLUMINA) == 0) {
            return !(inputFile.checkFileIsEmpty() ||
                     sampleFile.checkFileIsEmpty() ||
                     ctrlFile.checkFileIsEmpty());
        } else {
            return !(inputFile.checkFileIsEmpty() ||
                     sampleFile.checkFileIsEmpty());
        }
    }
    
    // After reviewing the configuration, user decided to proceed with 
    // the pipeline execution.
    public String submitJob() {
        logger.info(AuthenticationBean.getUserName() + ": started " +
                    pipelineName);
        
        return Constants.MAIN_PAGE;
    }
    
    // After reviewing the configuration, user decided not to proceed with
    // the pipeline execution.
    public void cancelJob() {
        logger.info(AuthenticationBean.getUserName() + 
                ": decided not to proceed with " + pipelineName);
    }
    
    // Machine generated getters and setters
    public FileUploadBean getCtrlFile() {
        return ctrlFile;
    }
    public void setCtrlFile(FileUploadBean ctrlFile) {
        this.ctrlFile = ctrlFile;
    }
    public FileUploadBean getSampleFile() {
        return sampleFile;
    }
    public void setSampleFile(FileUploadBean sampleFile) {
        this.sampleFile = sampleFile;
    }
    public FileUploadBean getInputFile() {
        return inputFile;
    }
    public void setInputFile(FileUploadBean inputFile) {
        this.inputFile = inputFile;
    }
    public static String getPipelineType() {
        return pipelineType;
    }
    public static void setPipelineType(String pipelineType) {
        ArrayConfigBean.pipelineType = pipelineType;
    }
    public String getPipelineName() {
        return pipelineName;
    }    
    public static void setPipelineName(String pipelineName) {
        ArrayConfigBean.pipelineName = pipelineName;
    }
    public String getStudyID() {
        return studyID;
    }
    public void setStudyID(String studyID) {
        this.studyID = studyID;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getNormalization() {
        return normalization;
    }
    public void setNormalization(String normalization) {
        this.normalization = normalization;
    }
    public String getProbeFilter() {
        return probeFilter;
    }
    public void setProbeFilter(String probeFilter) {
        this.probeFilter = probeFilter;
    }
    public List<String> getProbeFilters() {
        return probeFilters;
    }
    public void setProbeFilters(List<String> probeFilters) {
        this.probeFilters = probeFilters;
    }
    public String getPhenotype() {
        return phenotype;
    }
    public void setPhenotype(String phenotype) {
        this.phenotype = phenotype;
    }
    public boolean isProbeSelect() {
        return probeSelect;
    }
    public void setProbeSelect(boolean probeSelect) {
        this.probeSelect = probeSelect;
    }
    public String getAverage() {
        return average;
    }
    public void setAverage(String average) {
        this.average = average;
    }
    public String getStdLog2Ratio() {
        return stdLog2Ratio;
    }
    public void setStdLog2Ratio(String stdLog2Ratio) {
        this.stdLog2Ratio = stdLog2Ratio;
    }
}
