package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 *
 * @author skraffmiller
 */
@Entity
@Table(indexes = {@Index(columnList="dataset_id")} )
public class DatasetVersion implements Serializable {

    /**
     * Convenience comparator to compare dataset versions by their version number.
     * The draft version is considered the latest.
     */
    public static final Comparator<DatasetVersion> compareByVersion = new Comparator<DatasetVersion>() {
        @Override
        public int compare(DatasetVersion o1, DatasetVersion o2) {
            if ( o1.isDraft() ) {
                return o2.isDraft() ? 0 : 1;
            } else {
               return (int)Math.signum( (o1.getVersionNumber().equals(o2.getVersionNumber())) ?
                        o1.getMinorVersionNumber() - o2.getMinorVersionNumber()
                       : o1.getVersionNumber() - o2.getVersionNumber() );
            }
        }
    };

    // TODO: Determine the UI implications of various version states
    //IMPORTANT: If you add a new value to this enum, you will also have to modify the
    // StudyVersionsFragment.xhtml in order to display the correct value from a Resource Bundle
    public enum VersionState {

        DRAFT, RELEASED, ARCHIVED, DEACCESSIONED
    };
    
    public enum License {
        NONE, CC0
    }

    /**
     * @todo What does the GUI use for a default license? What does the "native"
     * API use? See also https://github.com/IQSS/dataverse/issues/1385
     */
    public static DatasetVersion.License defaultLicense = DatasetVersion.License.CC0;

    public DatasetVersion() {

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUNF() {
        return UNF;
    }

    public void setUNF(String UNF) {
        this.UNF = UNF;
    }
    

    /**
     * This is JPA's optimistic locking mechanism, and has no semantic meaning in the DV object model.
     * @return the object db version
     */
    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
    }
    
    private String UNF;

    @Version
    private Long version;

    private Long versionNumber;
    private Long minorVersionNumber;
    public static final int VERSION_NOTE_MAX_LENGTH = 1000;
    @Column(length = VERSION_NOTE_MAX_LENGTH)
    private String versionNote;
    
    @Enumerated(EnumType.STRING)
    private VersionState versionState;
    
    @Enumerated(EnumType.STRING)
    private License license;

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    @ManyToOne
    private Dataset dataset;

    @OneToMany(mappedBy = "datasetVersion", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("label") // this is not our preferred ordering, which is with the AlphaNumericComparator, but does allow the files to be grouped by category
    private List<FileMetadata> fileMetadatas = new ArrayList();

    public List<FileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }
    
    public List<FileMetadata> getFileMetadatasSorted() {
        Collections.sort(fileMetadatas, FileMetadata.compareByLabel);
        return fileMetadatas;
    }

    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    @OneToMany(mappedBy = "datasetVersion", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    //@OrderBy("datasetField.displayOrder") 
    private List<DatasetField> datasetFields = new ArrayList();

    public List<DatasetField> getDatasetFields() {
        return datasetFields;
    }

    /**
     * Sets the dataset fields for this version. Also updates the fields to 
     * have @{code this} as their dataset version.
     * @param datasetFields
     */
    public void setDatasetFields(List<DatasetField> datasetFields) {
        for ( DatasetField dsf : datasetFields ) {
            dsf.setDatasetVersion(this);
        }
        this.datasetFields = datasetFields;
    }

    /*
     @OneToMany(mappedBy="studyVersion", cascade={CascadeType.REMOVE, CascadeType.PERSIST})
     private List<VersionContributor> versionContributors;
     */
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column( nullable=false )
    private Date createTime;
    @Temporal(value = TemporalType.TIMESTAMP)
    @Column( nullable=false )
    private Date lastUpdateTime;
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date releaseTime;
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date archiveTime;
    public static final int ARCHIVE_NOTE_MAX_LENGTH = 1000;
    @Column(length = ARCHIVE_NOTE_MAX_LENGTH)
    private String archiveNote;
    private String deaccessionLink;
    
    @Column(columnDefinition="TEXT")      
    private String termsOfUse;
    
    @Column(columnDefinition="TEXT") 
    private String termsOfAccess;
    
    @Column(columnDefinition="TEXT") 
    private String confidentialityDeclaration;
    
    @Column(columnDefinition="TEXT") 
    private String specialPermissions;
    
    @Column(columnDefinition="TEXT") 
    private String restrictions;
    
    @Column(columnDefinition="TEXT") 
    private String citationRequirements;
    
    @Column(columnDefinition="TEXT") 
    private String depositorRequirements;
    
    @Column(columnDefinition="TEXT") 
    private String conditions;
    
    @Column(columnDefinition="TEXT") 
    private String disclaimer;
    
    @Column(columnDefinition="TEXT") 
    private String dataAccessPlace;
    
    @Column(columnDefinition="TEXT") 
    private String originalArchive;
    
    @Column(columnDefinition="TEXT") 
    private String availabilityStatus;
    
    @Column(columnDefinition="TEXT") 
    private String contactForAccess;
    
    @Column(columnDefinition="TEXT") 
    private String sizeOfCollection;
    
    @Column(columnDefinition="TEXT") 
    private String studyCompletion;
    
    private boolean fileAccessRequest;

    public boolean isFileAccessRequest() {
        return fileAccessRequest;
    }

    public void setFileAccessRequest(boolean fileAccessRequest) {
        this.fileAccessRequest = fileAccessRequest;
    }
    
    private boolean inReview;
    public void setInReview(boolean inReview){
        this.inReview = inReview;
    }

    public boolean isInReview() {
        return inReview;
    }

    public String getStudyCompletion() {
        return studyCompletion;
    }

    public void setStudyCompletion(String studyCompletion) {
        this.studyCompletion = studyCompletion;
    }
        
    
    public String getTermsOfUse() {
        return termsOfUse;
    }
    
    /**
     * Quick hack to disable <script> tags
     * for Terms of Use and Terms of Access.
     * 
     * Need to add jsoup or something similar.
     * 
     * @param str
     * @return 
     */
    private String stripScriptTags(String str){        
        if (str == null){
            return null;
        }

        str = str.replaceAll("(?i)<script\\b[^<]*(?:(?!<\\/script>)<[^<]*)*<\\/script>", "");
        str = str.replaceAll("(?i)<\\/script>", "");
        str = str.replaceAll("(?i)<script\\b", "");

        return str;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public String getTermsOfAccess() {
        return termsOfAccess;
    }

    public void setTermsOfAccess(String termsOfAccess) {
        this.termsOfAccess = termsOfAccess;
    }
    
        
    public String getConfidentialityDeclaration() {
        return confidentialityDeclaration;
    }

    public void setConfidentialityDeclaration(String confidentialityDeclaration) {
        this.confidentialityDeclaration = confidentialityDeclaration;
    }

    public String getSpecialPermissions() {
        return specialPermissions;
    }

    public void setSpecialPermissions(String specialPermissions) {
        this.specialPermissions = specialPermissions;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(String restrictions) {
        this.restrictions = restrictions;
    }

    public String getCitationRequirements() {
        return citationRequirements;
    }

    public void setCitationRequirements(String citationRequirements) {
        this.citationRequirements = citationRequirements;
    }

    public String getDepositorRequirements() {
        return depositorRequirements;
    }

    public void setDepositorRequirements(String depositorRequirements) {
        this.depositorRequirements = depositorRequirements;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getDataAccessPlace() {
        return dataAccessPlace;
    }

    public void setDataAccessPlace(String dataAccessPlace) {
        this.dataAccessPlace = dataAccessPlace;
    }

    public String getOriginalArchive() {
        return originalArchive;
    }

    public void setOriginalArchive(String originalArchive) {
        this.originalArchive = originalArchive;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getContactForAccess() {
        return contactForAccess;
    }

    public void setContactForAccess(String contactForAccess) {
        this.contactForAccess = contactForAccess;
    }

    public String getSizeOfCollection() {
        return sizeOfCollection;
    }

    public void setSizeOfCollection(String sizeOfCollection) {
        this.sizeOfCollection = sizeOfCollection;
    }


    public Date getArchiveTime() {
        return archiveTime;
    }

    public void setArchiveTime(Date archiveTime) {
        this.archiveTime = archiveTime;
    }

    public String getArchiveNote() {
        return archiveNote;
    }

    public void setArchiveNote(String note) {
        // @todo should this be using bean validation for trsting note length?
        if (note != null && note.length() > ARCHIVE_NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Error setting archiveNote: String length is greater than maximum (" + ARCHIVE_NOTE_MAX_LENGTH + ")."
                    + "  StudyVersion id=" + id + ", archiveNote=" + note);
        }
        this.archiveNote = note;
    }

    public String getDeaccessionLink() {
        return deaccessionLink;
    }

    public void setDeaccessionLink(String deaccessionLink) {
        this.deaccessionLink = deaccessionLink;
    }

    public GlobalId getDeaccessionLinkAsGlobalId() {
        return new GlobalId(deaccessionLink);
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        if (createTime == null) {
            createTime = lastUpdateTime;
        }
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getVersionDate() {
        if (this.lastUpdateTime == null){
            return null; 
        }
        return new SimpleDateFormat("MMMM d, yyyy").format(lastUpdateTime);
    }

    public String getVersionYear() {
        return new SimpleDateFormat("yyyy").format(lastUpdateTime);
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }

    @OneToMany(mappedBy = "datasetVersion")
    private List<DatasetVersionUser> datasetVersionUsers;

    public List<DatasetVersionUser> getDatasetVersionUsers() {
        return datasetVersionUsers;
    }

    public void setUserDatasets(List<DatasetVersionUser> datasetVersionUsers) {
        this.datasetVersionUsers = datasetVersionUsers;
    }

    public List<String> getVersionContributorIdentifiers() {
        if (this.getDatasetVersionUsers() == null) {
            return Collections.emptyList();
        }
        List<String> ret = new LinkedList<>();
        for (DatasetVersionUser contributor : this.getDatasetVersionUsers()) {
            ret.add(contributor.getAuthenticatedUser().getIdentifier());
        }
        return ret;
    }

    @Transient
    private String contributorNames;

    public String getContributorNames() {
        return contributorNames;
    }

    public void setContributorNames(String contributorNames) {
        this.contributorNames = contributorNames;
    }

 
    public String getVersionNote() {
        return versionNote;
    }

    public DatasetVersionDifference getDefaultVersionDifference() {
        // if version is deaccessioned ignore it for differences purposes
        int index = 0;
        int size = this.getDataset().getVersions().size();
        if (this.isDeaccessioned()) {
            return null;
        }
        for (DatasetVersion dsv : this.getDataset().getVersions()) {
            if (this.equals(dsv)) {
                if ((index + 1) <= (size - 1)) {
                    for (DatasetVersion dvTest : this.getDataset().getVersions().subList(index + 1, size)) {
                        if (!dvTest.isDeaccessioned()) {
                            DatasetVersionDifference dvd = new DatasetVersionDifference(this, dvTest);
                            return dvd;
                        }
                    }
                }
            }
            index++;
        }
        return null;
    }

    public VersionState getPriorVersionState() {
        int index = 0;
        int size = this.getDataset().getVersions().size();
        if (this.isDeaccessioned()) {
            return null;
        }
        for (DatasetVersion dsv : this.getDataset().getVersions()) {
            if (this.equals(dsv)) {
                if ((index + 1) <= (size - 1)) {
                    for (DatasetVersion dvTest : this.getDataset().getVersions().subList(index + 1, size)) {
                        return dvTest.getVersionState();
                    }
                }
            }
            index++;
        }
        return null;
    }

    public void setVersionNote(String note) {
        if (note != null && note.length() > VERSION_NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Error setting versionNote: String length is greater than maximum (" + VERSION_NOTE_MAX_LENGTH + ")."
                    + "  StudyVersion id=" + id + ", versionNote=" + note);
        }
        this.versionNote = note;
    }
   
    public Long getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Long getMinorVersionNumber() {
        return minorVersionNumber;
    }

    public void setMinorVersionNumber(Long minorVersionNumber) {
        this.minorVersionNumber = minorVersionNumber;
    }
    
    public String getFriendlyVersionNumber(){
        if (this.isDraft()) {
            return "DRAFT";
        } else {
            return versionNumber.toString() + "." + minorVersionNumber.toString();                    
        }
    }

    public VersionState getVersionState() {
        return versionState;
    }

    public void setVersionState(VersionState versionState) {

        this.versionState = versionState;
    }

    public boolean isReleased() {
        return versionState.equals(VersionState.RELEASED);
    }

    public boolean isDraft() {
        return versionState.equals(VersionState.DRAFT);
    }

    public boolean isWorkingCopy() {
        return versionState.equals(VersionState.DRAFT);
    }

    public boolean isArchived() {
        return versionState.equals(VersionState.ARCHIVED);
    }

    public boolean isDeaccessioned() {
        return versionState.equals(VersionState.DEACCESSIONED);
    }

    public boolean isRetiredCopy() {
        return (versionState.equals(VersionState.ARCHIVED) || versionState.equals(VersionState.DEACCESSIONED));
    }

    public boolean isMinorUpdate() {
        if (this.dataset.getLatestVersion().isWorkingCopy()) {
            if (this.dataset.getVersions().size() > 1 && this.dataset.getVersions().get(1) != null) {
                if (this.dataset.getVersions().get(1).isDeaccessioned()) {
                    return false;
                }
            }
        }
        if (this.getDataset().getReleasedVersion() != null) {
            if (this.getFileMetadatas().size() != this.getDataset().getReleasedVersion().getFileMetadatas().size()){
                return false;
            } else {
                List <DataFile> current = new ArrayList();
                List <DataFile> previous = new ArrayList();
                for (FileMetadata fmdc : this.getFileMetadatas()){
                    current.add(fmdc.getDataFile());
                }
                for (FileMetadata fmdc : this.getDataset().getReleasedVersion().getFileMetadatas()){
                    previous.add(fmdc.getDataFile());
                }
                for (DataFile fmd: current){
                    previous.remove(fmd);
                }
                return previous.isEmpty();                
            }           
        }
        return true;
    }

    public DatasetVersion getMostRecentlyReleasedVersion() {
        if (this.isReleased()) {
            return this;
        } else {
            if (this.getDataset().isReleased()) {
                for (DatasetVersion testVersion : this.dataset.getVersions()) {
                    if (testVersion.isReleased()) {
                        return testVersion;
                    }
                }
            }
        }
        return null;
    }

    public DatasetVersion getLargestMinorRelease() {

        if (this.getDataset().isReleased()) {
            for (DatasetVersion testVersion : this.dataset.getVersions()) {
                if (testVersion.getVersionNumber() != null && testVersion.getVersionNumber().equals(this.getVersionNumber())) {
                    return testVersion;
                }
            }
        }

        return this;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetVersion)) {
            return false;
        }
        DatasetVersion other = (DatasetVersion) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "[DatasetVersion id:" + getId() + "]";
    }

    public boolean isLatestVersion() {
        return this.equals(this.getDataset().getLatestVersion());
    }

    public String getTitle() {
        String retVal = "";
        for (DatasetField dsfv : this.getDatasetFields()) {
            if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.title)) {
                retVal = dsfv.getDisplayValue();
            }
        }
        return retVal;
    }
    
    public String getTitleZh() {
        String retVal = "";
        for (DatasetField dsfv : this.getDatasetFields()) {
            if (dsfv.getDatasetFieldType().getName().equals(DatasetFieldConstant.titleZh)) {
                retVal = dsfv.getDisplayValue();
            }
        }
        return retVal;
    }

    public String getProductionDate() {
        //todo get "Production Date" from datasetfieldvalue table
        return "Production Date";
    }

    public List<DatasetAuthor> getDatasetAuthors() {
        //todo get "List of Authors" from datasetfieldvalue table
        List retList = new ArrayList();
        for (DatasetField dsf : this.getDatasetFields()) {
            Boolean addAuthor = true;
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.author)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    DatasetAuthor datasetAuthor = new DatasetAuthor();
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
                            if (subField.isEmptyForDisplay()) {
                                addAuthor = false;
                            }
                            datasetAuthor.setName(subField);
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation)) {
                            datasetAuthor.setAffiliation(subField);
                        }
                    }
                    if (addAuthor) {
                        retList.add(datasetAuthor);
                    }
                }
            }
        }
        return retList;
    }
    
    public List<DatasetAuthor> getDatasetAuthorsZh() {
        //todo get "List of Authors" from datasetfieldvalue table
        List retList = new ArrayList();
        for (DatasetField dsf : this.getDatasetFields()) {
            Boolean addAuthor = true;
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorZh)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    DatasetAuthor datasetAuthor = new DatasetAuthor();
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorNameZh)) {
                            if (subField.isEmptyForDisplay()) {
                                addAuthor = false;
                            }
                            datasetAuthor.setName(subField);
                        }
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliationZh)) {
                            datasetAuthor.setAffiliation(subField);
                        }
                    }
                    if (addAuthor) {
                        retList.add(datasetAuthor);
                    }
                }
            }
        }
        return retList;
    }
    
    public String getDatasetProducersString(){
        String retVal = "";
        for (DatasetField dsf : this.getDatasetFields()) {
            if (dsf.getDatasetFieldType().getName().equals(DatasetFieldConstant.producer)) {
                for (DatasetFieldCompoundValue authorValue : dsf.getDatasetFieldCompoundValues()) {
                    for (DatasetField subField : authorValue.getChildDatasetFields()) {
                        if (subField.getDatasetFieldType().getName().equals(DatasetFieldConstant.producerName)) {
                            if (retVal.isEmpty()){
                                retVal = subField.getDisplayValue();
                            } else {
                                retVal += ", " +  subField.getDisplayValue();
                            }                           
                        }
                    }
                }
            }
        }
        return retVal;
    }

    public void setDatasetAuthors(List<DatasetAuthor> authors) {
        // FIXME add the authores to the relevant fields
    }
    
    public String getCitation() {
        return getCitation(false, null);
    }
    
    public String getCitation(boolean isOnlineVersion) {
        return getCitation(isOnlineVersion, null);
    }

    public String getCitation(boolean isOnlineVersion,String language) {

        String str = "";

        boolean includeAffiliation = false;
        String authors = null;
        if(language != null && language.equals("zh"))
            authors = this.getAuthorsStrZh(includeAffiliation);
        else
            authors = this.getAuthorsStr(includeAffiliation);
        if (!StringUtil.isEmpty(authors)) {
            str += authors;
        } else {
            str += getDatasetProducersString();
        }

        if (this.getDataset().getPublicationDate() == null || StringUtil.isEmpty(this.getDataset().getPublicationDate().toString())) {
            
            if (!this.getDataset().isHarvested()) {
                //if not released use current year
                if (!StringUtil.isEmpty(str)) {
                    str += ", ";
                }
                str += new SimpleDateFormat("yyyy").format(new Timestamp(new Date().getTime()));
            } else {
                String distDate = getDistributionDate();
                if (distDate != null) {
                    if (!StringUtil.isEmpty(str)) {
                        str += ", ";
                    }
                    str += distDate;
                }
            }
        } else {
            if (!StringUtil.isEmpty(str)) {
                str += ", ";
            }
            str += new SimpleDateFormat("yyyy").format(new Timestamp(this.getDataset().getPublicationDate().getTime()));
        }
        if(language != null && language.equals("zh")){
            if (this.getTitleZh() != null) {
                if (!StringUtil.isEmpty(this.getTitleZh())) {
                    if (!StringUtil.isEmpty(str)) {
                        str += ", ";
                    }
                    str += "\"" + this.getTitleZh() + "\"";
                }
            }
        }else{
            if (this.getTitle() != null) {
                if (!StringUtil.isEmpty(this.getTitle())) {
                    if (!StringUtil.isEmpty(str)) {
                        str += ", ";
                    }
                    str += "\"" + this.getTitle() + "\"";
                }
            }
        }
        
        if (this.getDataset().isHarvested()) {
            String distributorName = getDistributorName();
            if (distributorName != null && distributorName.trim().length() > 0) {
                if (!StringUtil.isEmpty(str)) {
                    str += ". ";
                }
                str += " " + distributorName;
                str += " [distributor]";
            }
        }
        
        // The Global Identifier: 
        // It is always part of the citation for the local datasets; 
        // And for *some* harvested datasets. 
        if (!this.getDataset().isHarvested()
                || HarvestingDataverseConfig.HARVEST_STYLE_VDC.equals(this.getDataset().getOwner().getHarvestingDataverseConfig().getHarvestStyle())
                || HarvestingDataverseConfig.HARVEST_STYLE_ICPSR.equals(this.getDataset().getOwner().getHarvestingDataverseConfig().getHarvestStyle())
                || HarvestingDataverseConfig.HARVEST_STYLE_DATAVERSE.equals(this.getDataset().getOwner().getHarvestingDataverseConfig().getHarvestStyle())) {
            if (!StringUtil.isEmpty(this.getDataset().getIdentifier())) {
                if (!StringUtil.isEmpty(str)) {
                    str += ", ";
                }
                if (isOnlineVersion) {
                    str += "<a href=\"" + this.getDataset().getPersistentURL() + "\">" + this.getDataset().getPersistentURL() + "</a>";
                } else {
                    str += this.getDataset().getPersistentURL();
                }
            }
        }

        // Get root dataverse name for Citation
        // (only for non-harvested datasets):
        if (!this.getDataset().isHarvested()) {
            String dataverseName = getRootDataverseNameforCitation(language);
            if (!StringUtil.isEmpty(dataverseName)) {
                if (!StringUtil.isEmpty(str)) {
                    str += ", ";
                }
                str += " " + dataverseName;
            }
        }

        // Version status:
        // Again, this is needed for non-harvested stuff only:
        // (the check may be redundant - we may already be dropping version 
        // numbers when harvesting -- L.A. 4.0 beta15)
        if (!this.getDataset().isHarvested()) {
            if (this.isDraft()) {
                if (!StringUtil.isEmpty(str)) {
                    str += ", ";
                }
                str += " DRAFT VERSION ";

            } else if (this.getVersionNumber() != null) {
                if (!StringUtil.isEmpty(str)) {
                    str += ", ";
                }
                str += " V" + this.getVersionNumber();

            }
            if (this.isDeaccessioned()) {
                if (!StringUtil.isEmpty(str)) {
                    str += ", ";
                }
                str += " DEACCESSIONED VERSION ";

            }
        }
        
        if (!StringUtil.isEmpty(getUNF())) {
            if (!StringUtil.isEmpty(str)) {
                str += " ";
            }
            str += "[" + getUNF() + "]";
        }
         /*
         String distributorNames = getDistributorNames();
         if (distributorNames.trim().length() > 0) {
         str += " " + distributorNames;
         str += " [Distributor]";
         }*/
        return str;
    }

    public String getDistributionDate() {
        //todo get dist date from datasetfieldvalue table
        for (DatasetField dsf : this.getDatasetFields()) {
            if (DatasetFieldConstant.distributionDate.equals(dsf.getDatasetFieldType().getName())) {
                String date = dsf.getValue();
                return date;
            }
            
        }
        return null;
    }

    public String getDistributorName() {
        for (DatasetField dsf : this.getDatasetFields()) {
            if (DatasetFieldConstant.distributorName.equals(dsf.getDatasetFieldType().getName())) {
                return dsf.getValue();
            }
        }
        return null;
    }
    
    public String getRootDataverseNameforCitation(){
        return getRootDataverseNameforCitation(null);
    }
    
    public String getRootDataverseNameforCitation(String language){
                    //Get root dataverse name for Citation
        Dataverse root = this.getDataset().getOwner();
        while (root.getOwner() != null) {
            root = root.getOwner();
        }
        String rootDataverseName = language!=null && language.equals("zh") ? 
                root.getNameZh() : root.getName();
        if (!StringUtil.isEmpty(rootDataverseName)) {
            return rootDataverseName;// + (language!=null && language.equals("zh") ?
                //   " 数据空间" : " Dataverse");
        } else {
            return "";
        }
    }

    public List<DatasetDistributor> getDatasetDistributors() {
        //todo get distributors from DatasetfieldValues
        return new ArrayList();
    }

    public void setDatasetDistributors(List<DatasetDistributor> distributors) {
        //todo implement
    }

    public String getDistributorNames() {
        String str = "";
        for (DatasetDistributor sd : this.getDatasetDistributors()) {
            if (str.trim().length() > 1) {
                str += ";";
            }
            str += sd.getName();
        }
        return str;
    }

    public String getAuthorsStr() {
        return getAuthorsStr(true);
    }

    public String getAuthorsStr(boolean affiliation) {
        String str = "";
        for (DatasetAuthor sa : getDatasetAuthors()) {
            if (sa.getName() == null) {
                break;
            }
            if (str.trim().length() > 1) {
                str += "; ";
            }
            str += sa.getName().getValue();
            if (affiliation) {
                if (sa.getAffiliation() != null) {
                    if (!StringUtil.isEmpty(sa.getAffiliation().getValue())) {
                        str += " (" + sa.getAffiliation().getValue() + ")";
                    }
                }
            }
        }
        return str;
    }
    
    public String getAuthorsStrZh(boolean affiliation) {
        String str = "";
        for (DatasetAuthor sa : getDatasetAuthorsZh()) {
            if (sa.getName() == null) {
                break;
            }
            if (str.trim().length() > 1) {
                str += "; ";
            }
            str += sa.getName().getValue();
            if (affiliation) {
                if (sa.getAffiliation() != null) {
                    if (!StringUtil.isEmpty(sa.getAffiliation().getValue())) {
                        str += " (" + sa.getAffiliation().getValue() + ")";
                    }
                }
            }
        }
        return str;
    }

    // TODO: clean up init methods and get them to work, cascading all the way down.
    // right now, only work for one level of compound objects
    private DatasetField initDatasetField(DatasetField dsf) {
        if (dsf.getDatasetFieldType().isCompound()) {
            for (DatasetFieldCompoundValue cv : dsf.getDatasetFieldCompoundValues()) {
                // for each compound value; check the datasetfieldTypes associated with its type
                for (DatasetFieldType dsfType : dsf.getDatasetFieldType().getChildDatasetFieldTypes()) {
                    boolean add = true;
                    for (DatasetField subfield : cv.getChildDatasetFields()) {
                        if (dsfType.equals(subfield.getDatasetFieldType())) {
                            add = false;
                            break;
                        }
                    }

                    if (add) {
                        cv.getChildDatasetFields().add(DatasetField.createNewEmptyChildDatasetField(dsfType, cv));
                    }
                }
            }
        }

        return dsf;
    }

    public List<DatasetField> initDatasetFields() {
        //retList - Return List of values
        List<DatasetField> retList = new ArrayList();
        //Running into null on create new dataset
        if (this.getDatasetFields() != null) {
            for (DatasetField dsf : this.getDatasetFields()) {
                retList.add(initDatasetField(dsf));
            }
        }

        //Test to see that there are values for 
        // all fields in this dataset via metadata blocks
        //only add if not added above
        for (MetadataBlock mdb : this.getDataset().getOwner().getMetadataBlocks()) {
            for (DatasetFieldType dsfType : mdb.getDatasetFieldTypes()) {
                if (!dsfType.isSubField()) {
                    boolean add = true;
                    //don't add if already added as a val
                    for (DatasetField dsf : retList) {
                        if (dsfType.equals(dsf.getDatasetFieldType())) {
                            add = false;
                            break;
                        }
                    }

                    if (add) {
                        retList.add(DatasetField.createNewEmptyDatasetField(dsfType, this));
                    }
                }
            }
        }

        //sort via display order on dataset field
        Collections.sort(retList, DatasetField.DisplayOrder);

        return retList;
    }

    /**
     * For the current server, create link back to this Dataset
     *
     * example:
     * http://dvn-build.hmdc.harvard.edu/dataset.xhtml?id=72&versionId=25
     *
     * @param serverName
     * @param dset
     * @return
     */
    public String getReturnToDatasetURL(String serverName, Dataset dset) {
        if (serverName == null) {
            return null;
        }
        if (dset == null) {
            dset = this.getDataset();
            if (dset == null) {        // currently postgres allows this, see https://github.com/IQSS/dataverse/issues/828
                return null;
            }
        }
        return serverName + "/dataset.xhtml?id=" + dset.getId() + "&versionId" + this.getId();
    }

    ;
    
    public List<DatasetField> copyDatasetFields(List<DatasetField> copyFromList) {
        List<DatasetField> retList = new ArrayList();

        for (DatasetField sourceDsf : copyFromList) {
            //the copy needs to have the current version
            retList.add(sourceDsf.copy(this));
        }

        return retList;
    }

    public List<DatasetField> getFlatDatasetFields() {
        return getFlatDatasetFields(getDatasetFields());
    }

    private List<DatasetField> getFlatDatasetFields(List<DatasetField> dsfList) {
        List<DatasetField> retList = new LinkedList();
        for (DatasetField dsf : dsfList) {
            retList.add(dsf);
            if (dsf.getDatasetFieldType().isCompound()) {
                for (DatasetFieldCompoundValue compoundValue : dsf.getDatasetFieldCompoundValues()) {
                    retList.addAll(getFlatDatasetFields(compoundValue.getChildDatasetFields()));
                }

            }
        }
        return retList;
    }

    public String getSemanticVersion() {
        /**
         * Not prepending a "v" like "v1.1" or "v2.0" because while SemVerTag
         * was in http://semver.org/spec/v1.0.0.html but later removed in
         * http://semver.org/spec/v2.0.0.html
         *
         * See also to v or not to v · Issue #1 · mojombo/semver -
         * https://github.com/mojombo/semver/issues/1#issuecomment-2605236
         */
        if (this.isReleased()) {
            return versionNumber + "." + minorVersionNumber;
        } else if (this.isDraft()){
            return VersionState.DRAFT.toString();
        } else if (this.isDeaccessioned()){
            return versionNumber + "." + minorVersionNumber;
        } else{
            return versionNumber + "." + minorVersionNumber;            
        }
        //     return VersionState.DEACCESSIONED.name();
       // } else {
       //     return "-unkwn semantic version-";
       // }
    }

    public List<ConstraintViolation> validateRequired() {
        List<ConstraintViolation> returnListreturnList = new ArrayList();
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        for (DatasetField dsf : this.getFlatDatasetFields()) {
            dsf.setValidationMessage(null); // clear out any existing validation message
            Set<ConstraintViolation<DatasetField>> constraintViolations = validator.validate(dsf);
            for (ConstraintViolation<DatasetField> constraintViolation : constraintViolations) {
                dsf.setValidationMessage(constraintViolation.getMessage());
                returnListreturnList.add(constraintViolation);
                 break; // currently only support one message, so we can break out of the loop after the first constraint violation
            }
            
        }
        return returnListreturnList;
    }
    
    public Set<ConstraintViolation> validate() {
        Set<ConstraintViolation> returnSet = new HashSet();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        for (DatasetField dsf : this.getFlatDatasetFields()) {
            dsf.setValidationMessage(null); // clear out any existing validation message
            Set<ConstraintViolation<DatasetField>> constraintViolations = validator.validate(dsf);
            for (ConstraintViolation<DatasetField> constraintViolation : constraintViolations) {
                dsf.setValidationMessage(constraintViolation.getMessage());
                returnSet.add(constraintViolation);
                break; // currently only support one message, so we can break out of the loop after the first constraint violation
            }
            for (DatasetFieldValue dsfv : dsf.getDatasetFieldValues()) {
                dsfv.setValidationMessage(null); // clear out any existing validation message
                Set<ConstraintViolation<DatasetFieldValue>> constraintViolations2 = validator.validate(dsfv);
                for (ConstraintViolation<DatasetFieldValue> constraintViolation : constraintViolations2) {
                    dsfv.setValidationMessage(constraintViolation.getMessage());
                    returnSet.add(constraintViolation);
                    break; // currently only support one message, so we can break out of the loop after the first constraint violation                    
                }
            }
        }
        return returnSet;
    }
}
