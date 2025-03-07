package edu.harvard.iq.dataverse.authorization.users;

import cn.edu.pku.lib.dataverse.authorization.providers.iaaa.PKUIAAAAuthenticationProvider;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@NamedQueries({
    @NamedQuery( name="AuthenticatedUser.findAll",
                query="select au from AuthenticatedUser au"),
    @NamedQuery( name="AuthenticatedUser.findSuperUsers",
                query="SELECT au FROM AuthenticatedUser au WHERE au.superuser = TRUE"),
    @NamedQuery( name="AuthenticatedUser.findByIdentifier",
                query="select au from AuthenticatedUser au WHERE au.userIdentifier=:identifier"),
    @NamedQuery( name="AuthenticatedUser.findByEmail",
                query="select au from AuthenticatedUser au WHERE au.email=:email"),
    @NamedQuery( name="AuthenticatedUser.countOfIdentifier",
                query="SELECT COUNT(a) FROM AuthenticatedUser a WHERE a.userIdentifier=:identifier")
})
@Entity
public class AuthenticatedUser implements User, Serializable {
    
    public enum UserType{ORDINARY, ADVANCE}
//    public enum Position{STUDENT, FACULTY, RESEARCHER, GOVERNMENT_ORGANIZATION,
//        INTERNATIONAL_ORGANIZATION, RESEARCH_INSTITUTIONS, CORPORATION, 
//        NONGOVERNMENTAL_ORGANIZATION, OTHER}
    
    public static final String IDENTIFIER_PREFIX = "@";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    @Column(nullable = false, unique=true)
    private String userIdentifier;

    private String name;


    @NotNull
    @Column(nullable = false, unique=true)
    private String email;
    private String affiliation;
    private String position;
    private String lastName;
    private String firstName;
    private boolean superuser;
    private UserType userType;

    /**
     * @todo Remove? Check for accuracy? For Solr JOINs we used to care about
     * the modification times of users but now we don't index users at all.
     */
    private Timestamp modificationTime;


    @Transient
    private UserRequestMetadata requestMetadata;

    /**
     * @todo Consider storing a hash of *all* potentially interesting Shibboleth
     * attribute key/value pairs, not just the Identity Provider (IdP).
     */
    @Transient
    private String shibIdentityProvider;

    @Override
    public String getIdentifier() {
        return IDENTIFIER_PREFIX + userIdentifier;
    }
    
    
    
    @OneToMany(mappedBy = "user", cascade={CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private List<DatasetLock> datasetLocks;
	
    public List<DatasetLock> getDatasetLocks() {
        return datasetLocks;
    }

    public void setDatasetLocks(List<DatasetLock> datasetLocks) {
        this.datasetLocks = datasetLocks;
    }
    
    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new AuthenticatedUserDisplayInfo(firstName, lastName, email, affiliation, position);
    }
    
    /**
     * Takes the passed info object and updated the internal fields according to it.
     * @param inf the info from which we update the fields.
    */
    public void applyDisplayInfo( AuthenticatedUserDisplayInfo inf ) {
        setFirstName(inf.getFirstName());
        setLastName(inf.getLastName());
        setEmail(inf.getEmailAddress());
        setAffiliation( inf.getAffiliation() );
        setPosition( inf.getPosition());
        setUserType(inf.getUserType());

    }
    
    @Override
    public boolean isAuthenticated() { return true; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public boolean isSuperuser() {
        return superuser;
    }

    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    public void setModificationTime(Timestamp modificationTime) {
        this.modificationTime = modificationTime;
    }

    public boolean isBuiltInUser() {
        String authProviderString = authenticatedUserLookup.getAuthenticationProviderId();
        if (authProviderString != null && authProviderString.equals(BuiltinAuthenticationProvider.PROVIDER_ID)) {
            return true;
        }
        
        return false;
    }
    
    public boolean isPKUIAAAUser(){
        String authProviderString = authenticatedUserLookup.getAuthenticationProviderId();
        if (authProviderString != null && authProviderString.equals(PKUIAAAAuthenticationProvider.PROVIDER_ID)) {
            return true;
        }
        
        return false;
    }

    @OneToOne(mappedBy = "authenticatedUser")
    private AuthenticatedUserLookup authenticatedUserLookup;

    public AuthenticatedUserLookup getAuthenticatedUserLookup() {
        return authenticatedUserLookup;
    }

    public void setAuthenticatedUserLookup(AuthenticatedUserLookup authenticatedUserLookup) {
        this.authenticatedUserLookup = authenticatedUserLookup;
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
        if (!(object instanceof AuthenticatedUser)) {
            return false;
        }
        AuthenticatedUser other = (AuthenticatedUser) object;
        return Objects.equals(getId(), other.getId());
    }    

    @Override
    public UserRequestMetadata getRequestMetadata() {
        return requestMetadata;
    }

    public void setRequestMetadata(UserRequestMetadata requestMetadata) {
        this.requestMetadata = requestMetadata;
    }

    public String getShibIdentityProvider() {
        return shibIdentityProvider;
    }

    public void setShibIdentityProvider(String shibIdentityProvider) {
        this.shibIdentityProvider = shibIdentityProvider;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }
}