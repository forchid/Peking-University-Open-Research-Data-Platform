package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.search.savedsearch.SavedSearch;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 * @author gdurand
 * @author mbarsinai
 */
@NamedQueries({
    @NamedQuery(name = "Dataverse.ownedObjectsById", query = "SELECT COUNT(obj) FROM DvObject obj WHERE obj.owner.id=:id")
})
@Entity
public class Dataverse extends DvObjectContainer {

    public enum DataverseType {
        RESEARCHERS, RESEARCH_PROJECTS, JOURNALS, ORGANIZATIONS_INSTITUTIONS, TEACHING_COURSES, UNCATEGORIZED
    };
    
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Please enter a name.")
    @Column( nullable = false )
    private String name;
    
    @NotBlank(message = "Please enter a name.")
    @Column( name = "name_zh", nullable = false )
    private String nameZh;

    /**
     * @todo add @Column(nullable = false) for the database to enforce non-null
     */
    @NotBlank(message = "Please enter an alias.")
    @Column(nullable = false, unique=true)
    @Size(max = 60, message = "Alias must be at most 60 characters.")
    @Pattern.List({@Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "Found an illegal character(s). Valid characters are a-Z, 0-9, '_', and '-'."), 
        @Pattern(regexp=".*\\D.*", message="Alias should not be a number")})
    private String alias;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "description_zh", columnDefinition = "TEXT")
    private String descriptionZh;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Please select a category for your dataverse.")
    @Column( nullable = false )
    private DataverseType dataverseType;
    
    /**
     * When {@code true}, users are not granted permissions the got for parent
     * dataverses.
     */
    protected boolean permissionRoot;

    
    public DataverseType getDataverseType() {
        return dataverseType;
    }

    public void setDataverseType(DataverseType dataverseType) {
        this.dataverseType = dataverseType;
    }

    @Transient
    private final String uncategorizedString = "Uncategorized";

    /**
     * @todo Don't hard code these as English.
     */
    public String getFriendlyCategoryName(){
       switch (this.dataverseType) {
            case RESEARCHERS:
                return "Researcher";
            case RESEARCH_PROJECTS:
                return "Research Project";
            case JOURNALS:
                return "Journal";            
            case ORGANIZATIONS_INSTITUTIONS:
                return "Organization or Institution";            
            case TEACHING_COURSES:
                return "Teaching Course";            
            case UNCATEGORIZED:
                return uncategorizedString;
            default:
                return "";
        }    
    }

    public String getIndexableCategoryName() {
        String friendlyName = getFriendlyCategoryName();
        if (friendlyName.equals(uncategorizedString)) {
            return null;
        } else {
            return friendlyName;
        }
    }

    private String affiliation;
    @Column( name = "affiliation_zh")
    private String affiliationZh;

	// Note: We can't have "Remove" here, as there are role assignments that refer
    //       to this role. So, adding it would mean violating a forign key contstraint.
    @OneToMany(cascade = {CascadeType.MERGE},
            fetch = FetchType.LAZY,
            mappedBy = "owner")
    private Set<DataverseRole> roles;
    
    @ManyToOne
    @JoinColumn(nullable = false)
    private DataverseRole defaultContributorRole;

    public DataverseRole getDefaultContributorRole() {
        return defaultContributorRole;
    }

    public void setDefaultContributorRole(DataverseRole defaultContributorRole) {
        this.defaultContributorRole = defaultContributorRole;
    }
   
    private boolean metadataBlockRoot;
    private boolean facetRoot;
    private boolean themeRoot;
    private boolean templateRoot;    

    
    @OneToOne(mappedBy = "dataverse",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST}, orphanRemoval=true)
      private DataverseTheme dataverseTheme;

    @OneToMany(mappedBy = "dataverse",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST}, orphanRemoval=true)
    @OrderBy("displayOrder")
    @NotEmpty(message="At least one contact is required.")
    private List<DataverseContact> dataverseContacts = new ArrayList();
    
    @ManyToMany(cascade = {CascadeType.MERGE})
    private List<MetadataBlock> metadataBlocks = new ArrayList();

    @OneToMany(mappedBy = "dataverse",cascade={ CascadeType.REMOVE, CascadeType.MERGE,CascadeType.PERSIST}, orphanRemoval=true)
    @OrderBy("displayOrder")
    private List<DataverseFacet> dataverseFacets = new ArrayList();
    
    @ManyToMany
    @JoinTable(name = "dataversesubjects",
    joinColumns = @JoinColumn(name = "dataverse_id"),
    inverseJoinColumns = @JoinColumn(name = "controlledvocabularyvalue_id"))
    private Set<ControlledVocabularyValue> dataverseSubjects;
    
    @OneToMany(mappedBy="dataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataverseFeaturedDataverse> dataverseFeaturedDataverses;

    public List<DataverseFeaturedDataverse> getDataverseFeaturedDataverses() {
        return dataverseFeaturedDataverses;
    }

    public void setDataverseFeaturedDataverses(List<DataverseFeaturedDataverse> dataverseFeaturedDataverses) {
        this.dataverseFeaturedDataverses = dataverseFeaturedDataverses;
    }
    
    @OneToMany(mappedBy="featuredDataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataverseFeaturedDataverse> dataverseFeaturingDataverses;

    public List<DataverseFeaturedDataverse> getDataverseFeaturingDataverses() {
        return dataverseFeaturingDataverses;
    }

    public void setDataverseFeaturingDataverses(List<DataverseFeaturedDataverse> dataverseFeaturingDataverses) {
        this.dataverseFeaturingDataverses = dataverseFeaturingDataverses;
    }
    
    @OneToMany(mappedBy="dataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataverseLinkingDataverse> dataverseLinkingDataverses;

    public List<DataverseLinkingDataverse> getDataverseLinkingDataverses() {
        return dataverseLinkingDataverses;
    }

    public void setDataverseLinkingDataverses(List<DataverseLinkingDataverse> dataverseLinkingDataverses) {
        this.dataverseLinkingDataverses = dataverseLinkingDataverses;
    }
       
    @OneToMany(mappedBy="linkingDataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DataverseLinkingDataverse> dataverseLinkedDataverses;

    public List<DataverseLinkingDataverse> getDataverseLinkedDataverses() {
        return dataverseLinkedDataverses;
    }

    public void setDataverseLinkedDataverses(List<DataverseLinkingDataverse> dataverseLinkedDataverses) {
        this.dataverseLinkedDataverses = dataverseLinkedDataverses;
    }
    
    @OneToMany(mappedBy="linkingDataverse", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetLinkingDataverse> datasetLinkingDataverses;

    public List<DatasetLinkingDataverse> getDatasetLinkingDataverses() {
        return datasetLinkingDataverses;
    }

    public void setDatasetLinkingDataverses(List<DatasetLinkingDataverse> datasetLinkingDataverses) {
        this.datasetLinkingDataverses = datasetLinkingDataverses;
    }
        
    public Set<ControlledVocabularyValue> getDataverseSubjects() {
        return dataverseSubjects;
    }

    public void setDataverseSubjects(Set<ControlledVocabularyValue> dataverseSubjects) {
        this.dataverseSubjects = dataverseSubjects;
    }

    
    @OneToMany(mappedBy = "dataverse")
    private List<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels = new ArrayList();
    
    @ManyToOne
    @JoinColumn(nullable = true)
    private Template defaultTemplate;  
    
    @OneToMany(mappedBy = "definitionPoint", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<SavedSearch> savedSearches;

    public List<SavedSearch> getSavedSearches() {
        return savedSearches;
    }

    public void setSavedSearches(List<SavedSearch> savedSearches) {
        this.savedSearches = savedSearches;
    }
    
    @OneToMany(mappedBy="dataverse", cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private List<Template> templates; 
    
    @OneToMany(mappedBy="dataverse", cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private List<Guestbook> guestbooks;
        
    public List<Guestbook> getGuestbooks() {
        return guestbooks;
    }

    public void setGuestbooks(List<Guestbook> guestbooks) {
        this.guestbooks = guestbooks;
    } 
    
    @OneToOne (mappedBy="dataverse", cascade={CascadeType.PERSIST, CascadeType.REMOVE})
    private HarvestingDataverseConfig harvestingDataverseConfig;

    public HarvestingDataverseConfig getHarvestingDataverseConfig() {
        return this.harvestingDataverseConfig;
    }

    public void setHarvestingDataverseConfig(HarvestingDataverseConfig harvestingDataverseConfig) {
        this.harvestingDataverseConfig = harvestingDataverseConfig;
    }

    public boolean isHarvested() {
        return harvestingDataverseConfig != null; 
    }
    
    
    public List<Guestbook> getParentGuestbooks() {
        List<Guestbook> retList = new ArrayList();
        Dataverse testDV = this;
        while (testDV.getOwner() != null){   
           retList.addAll(testDV.getOwner().getGuestbooks());          
           if(testDV.getOwner().guestbookRoot){               
               break;
           }           
           testDV = testDV.getOwner();
        }
            return  retList;
    }
    
    public List<Guestbook> getAvailableGuestbooks() {
        //get all guestbooks
        List<Guestbook> retList = new ArrayList();
        Dataverse testDV = this;
        List<Guestbook> allGbs = new ArrayList();
        if (!this.guestbookRoot){
                    while (testDV.getOwner() != null){   
          
                allGbs.addAll(testDV.getOwner().getGuestbooks());
                if (testDV.getOwner().isGuestbookRoot()) {
                    break;
                }
                testDV = testDV.getOwner();
            }
        }

        allGbs.addAll(this.getGuestbooks());
        //then only display them if they are enabled
        for (Guestbook gbt : allGbs) {
            if (gbt.isEnabled()) {
                retList.add(gbt);
            }
        }
            return  retList;
        
    }
    
    private boolean guestbookRoot;
    
    public boolean isGuestbookRoot() {
        return guestbookRoot;
    }

    public void setGuestbookRoot(boolean guestbookRoot) {
        this.guestbookRoot = guestbookRoot;
    } 
    
    
    public void setDataverseFieldTypeInputLevels(List<DataverseFieldTypeInputLevel> dataverseFieldTypeInputLevels) {
        this.dataverseFieldTypeInputLevels = dataverseFieldTypeInputLevels;
    }

    public List<DataverseFieldTypeInputLevel> getDataverseFieldTypeInputLevels() {
        return dataverseFieldTypeInputLevels;
    }


    public Template getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(Template defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public List<Template> getTemplates() {
        return templates;
    }

    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }

    public List<Template> getParentTemplates() {
        List<Template> retList = new ArrayList();
        Dataverse testDV = this;
        while (testDV.getOwner() != null){   
            
           if (!testDV.getMetadataBlocks().equals(testDV.getOwner().getMetadataBlocks())){
               break;
           }           
           retList.addAll(testDV.getOwner().getTemplates());
           
           if(testDV.getOwner().templateRoot){               
               break;
           }           
           testDV = testDV.getOwner();
        }
            return  retList;
    }
    
    public boolean isThemeRoot() {
        return themeRoot;
    }
    
    public boolean getThemeRoot() {
        return themeRoot;
    }

    public void setThemeRoot(boolean  themeRoot) {
        this.themeRoot = themeRoot;
    }
    
    public boolean isTemplateRoot() {
        return templateRoot;
    }

    public void setTemplateRoot(boolean templateRoot) {
        this.templateRoot = templateRoot;
    }

   


    public List<MetadataBlock> getMetadataBlocks() {
        return getMetadataBlocks(false);
    }

    public List<MetadataBlock> getMetadataBlocks(boolean returnActualDB) {
        if (returnActualDB || metadataBlockRoot || getOwner() == null) {
            return metadataBlocks;
        } else {
            return getOwner().getMetadataBlocks();
        }
    }
    
    public Long getMetadataRootId(){
        if(metadataBlockRoot || getOwner() == null){
            return this.getId();
        } else { 
            return getOwner().getMetadataRootId();
        }
    }

    
    public DataverseTheme getDataverseTheme() {
        return getDataverseTheme(false);
    }

    public DataverseTheme getDataverseTheme(boolean returnActualDB) {
        if (returnActualDB || themeRoot || getOwner() == null) {
            return dataverseTheme;
        } else {
            return getOwner().getDataverseTheme();
        }
    }
    
    public String getGuestbookRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().guestbookRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getTemplateRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().templateRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getThemeRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().themeRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }

    public String getMetadataRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().metadataBlockRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }
    
    public String getFacetRootDataverseName() {
        Dataverse testDV = this;
        String retName = "Parent";
        while (testDV.getOwner() != null) {
            retName = testDV.getOwner().getDisplayName();
            if (testDV.getOwner().facetRoot) {
                break;
            }
            testDV = testDV.getOwner();
        }
        return retName;
    }
    
    public String getLogoOwnerId() {
        if (themeRoot || getOwner()==null) {
            return this.getId().toString();
        } else {
            return getOwner().getId().toString();
        }
    } 
    
    public void setDataverseTheme(DataverseTheme dataverseTheme) {
        this.dataverseTheme=dataverseTheme;
    }

    public void setMetadataBlocks(List<MetadataBlock> metadataBlocks) {
        this.metadataBlocks = metadataBlocks;
    }

    public List<DataverseFacet> getDataverseFacets() {
        return getDataverseFacets(false);
    }

    public List<DataverseFacet> getDataverseFacets(boolean returnActualDB) {
        if (returnActualDB || facetRoot || getOwner() == null) {
            return dataverseFacets;
        } else {
            return getOwner().getDataverseFacets();
        }
    }
     
    public Long getFacetRootId(){
        if(facetRoot || getOwner() == null){
            return this.getId();
        } else { 
            return getOwner().getFacetRootId();
        }        
    }

    public void setDataverseFacets(List<DataverseFacet> dataverseFacets) {
        this.dataverseFacets = dataverseFacets;
    }
    
    public List<DataverseContact> getDataverseContacts() {
        return dataverseContacts;
    }
    
    public String getContactEmails() {
        return "";
    }

    public void setDataverseContacts(List<DataverseContact> dataverseContacts) {
        this.dataverseContacts = dataverseContacts;
    }
    
    public void addDataverseContact(int index) {
        dataverseContacts.add(index, new DataverseContact(this));
    }

    public void removeDataverseContact(int index) {
        dataverseContacts.remove(index);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public boolean isMetadataBlockRoot() {
        return metadataBlockRoot;
    }

    public void setMetadataBlockRoot(boolean metadataBlockRoot) {
        this.metadataBlockRoot = metadataBlockRoot;
    }

    public boolean isFacetRoot() {
        return facetRoot;
    }

    public void setFacetRoot(boolean facetRoot) {
        this.facetRoot = facetRoot;
    }


    public void addRole(DataverseRole role) {
        role.setOwner(this);
        roles.add(role);
    }

    public Set<DataverseRole> getRoles() {
        return roles;
    }

    public List<Dataverse> getOwners() {
        List owners = new ArrayList();
        if (getOwner() != null) {
            owners.addAll(getOwner().getOwners());
            owners.add(getOwner());
        }
        return owners;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Dataverse)) {
            return false;
        }
        Dataverse other = (Dataverse) object;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    protected String toStringExtras() {
        return "name:" + getName();
    }

    @Override
    public <T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    /**
     * @todo implement in https://github.com/IQSS/dataverse/issues/551
     */
    public String getDepositTermsOfUse() {
        return "Dataverse Deposit Terms of Use will be implemented in https://github.com/IQSS/dataverse/issues/551";
    }
    
    @Override
    public String getDisplayName() {
        return getDisplayName(null);
    }
    
    @Override
    public String getDisplayName(Locale locale) {
        return locale != null && locale.getLanguage().equals("zh") ? 
                getNameZh()+"数据空间" : getName() + " Dataverse";
    }
    
    @Override
    public boolean isPermissionRoot() {
        return permissionRoot;
    }

    public void setPermissionRoot(boolean permissionRoot) {
        this.permissionRoot = permissionRoot;
    }

    public String getNameZh() {
        return nameZh;
    }

    public void setNameZh(String nameZh) {
        this.nameZh = nameZh;
    }

    public String getAffiliationZh() {
        return affiliationZh;
    }

    public void setAffiliationZh(String affiliationZh) {
        this.affiliationZh = affiliationZh;
    }

    public String getDescriptionZh() {
        return descriptionZh;
    }

    public void setDescriptionZh(String descriptionZh) {
        this.descriptionZh = descriptionZh;
    }
}
