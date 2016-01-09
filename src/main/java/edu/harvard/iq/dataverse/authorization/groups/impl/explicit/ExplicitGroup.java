package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.groups.GroupException;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser.UserType;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import javax.validation.constraints.Pattern;
import org.hibernate.validator.constraints.NotBlank;

/**
 * A group that explicitly lists {@link RoleAssignee}s that belong to it. Implementation-wise,
 * there are three cases here: {@link AuthenticatedUser}s, other {@link ExplicitGroup}s, and all the rest.
 * AuthenticatedUsers and ExplicitGroups go in tables of their own. The rest are kept via their identifier.
 * 
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="ExplicitGroup.findAll",
                 query="SELECT eg FROM ExplicitGroup eg"),
    @NamedQuery( name="ExplicitGroup.findById",
                 query="SELECT eg FROM ExplicitGroup eg WHERE eg.id=:id"),
    @NamedQuery( name="ExplicitGroup.findByOwnerIdAndAlias",
                 query="SELECT eg FROM ExplicitGroup eg WHERE eg.owner.id=:ownerId AND eg.groupAliasInOwner=:alias"),
    @NamedQuery( name="ExplicitGroup.findByAlias",
                 query="SELECT eg FROM ExplicitGroup eg WHERE eg.groupAlias=:alias"),
    @NamedQuery( name="ExplicitGroup.findByOwnerId",
                 query="SELECT eg FROM ExplicitGroup eg WHERE eg.owner.id=:ownerId"),
    @NamedQuery( name="ExplicitGroup.findByOwnerAndAuthUserId",
                 query="SELECT eg FROM ExplicitGroup eg join eg.containedAuthenticatedUsers au "
                      +"WHERE eg.owner.id=:ownerId AND au.id=:authUserId"),
    @NamedQuery( name="ExplicitGroup.findByOwnerAndSubExGroupId",
                 query="SELECT eg FROM ExplicitGroup eg join eg.containedExplicitGroups ceg "
                      +"WHERE eg.owner.id=:ownerId AND ceg.id=:subExGroupId"),
    @NamedQuery( name="ExplicitGroup.findByOwnerAndRAIdtf",
                 query="SELECT eg FROM ExplicitGroup eg join eg.containedRoleAssignees ra "
                      +"WHERE eg.owner.id=:ownerId AND ra=:raIdtf")
})
@Entity
public class ExplicitGroup implements Group, java.io.Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    /**
     * Authenticated users directly added to the group.
     */
    @ManyToMany
    private Set<AuthenticatedUser> containedAuthenticatedUsers;
    
    /**
     * Explicit groups that belong to {@code this} explicit gorups.
     */
    @ManyToMany
    @JoinTable(name = "explicitgroup_explicitgroup", 
            joinColumns = @JoinColumn(name="explicitgroup_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name="containedexplicitgroups_id", referencedColumnName = "id") )
    Set<ExplicitGroup> containedExplicitGroups;
    
    /**
     * All the role assignees that belong to this group
     * and are not {@link authenticatedUser}s or {@ExplicitGroup}s, are stored
     * here via their identifiers.
     * 
     * @see RoleAssignee#getIdentifier() 
     */
    @ElementCollection
    private Set<String> containedRoleAssignees;
    
    @Column( length = 1024 )
    private String description;
    
    @NotBlank
    private String displayName;
    
    /**
     * The DvObject under which this group is defined.
     */
    @ManyToOne
    DvObject owner;
    
    /** Given alias of the group, e.g by the user that created it. Unique in the owner. */
    @NotBlank
    @Pattern(regexp = "[a-zA-Z0-9\\_\\-]*", message = "Found an illegal character(s). Valid characters are a-Z, 0-9, '_', and '-'.")
    private String groupAliasInOwner;
    
    /** Alias of the group. Calculated from the group's name and its owner id. Unique in the table. */
    @Column( unique = true )
    private String groupAlias;
    
    /**restrict the user type for request join the group*/
    private UserType requestJoinUserType;
    
    @Transient
    private ExplicitGroupProvider provider;
    
    @ManyToMany
    @JoinTable(name = "joingrouprequests",
    joinColumns = @JoinColumn(name = "group_id"),
    inverseJoinColumns = @JoinColumn(name = "authenticated_user_id"))
    private List<AuthenticatedUser> joinGroupRequesters;
    
    public ExplicitGroup( ExplicitGroupProvider prv ) {
        provider = prv;
        containedAuthenticatedUsers = new HashSet<>();
        containedExplicitGroups = new HashSet<>();
        containedRoleAssignees = new TreeSet<>();
    }
    
    /**
     * Constructor for JPA.
     */
    protected ExplicitGroup() {}
    
    public void add( User u ) {
        if ( u instanceof AuthenticatedUser ) {
            containedAuthenticatedUsers.add((AuthenticatedUser)u);
        } else {
            containedRoleAssignees.add( u.getIdentifier() );
        }
    }
    
    /**
     * Adds the {@link RoleAssignee} to {@code this} group. 
     * 
     * @param ra the role assignee to be added to this group.
     * @throws GroupException if {@code ra} is a group, and is an ancestor of {@code this}.
     */
    public void add( RoleAssignee ra ) throws GroupException {
        
        if ( ra.equals(this) ) {
            throw new GroupException(this, "A group cannot be added to itself.");
        }
        
        if ( ra instanceof User ) {
            add( (User)ra );
            
        } else {
            // validate no circular deps
            Group g = (Group) ra;
            if ( g.contains(this) ) {
                throw new GroupException(this, "A group cannot be added to one of its childs.");
            }
            
            // add
            if ( g instanceof ExplicitGroup ) {
                containedExplicitGroups.add( (ExplicitGroup)g );
            } else {
                containedRoleAssignees.add( g.getIdentifier() );
            }
            
        }
        
    }
    
    public void remove(RoleAssignee roleAssignee) {
        removeByRoleAssgineeIdentifier( roleAssignee.getIdentifier() );
    }
    
    /**
     * Returns all the role assignee identifiers in this group. <br>
     * <b>Note</b> some of the identifiers may be stale (i.e. group deleted but 
     * identifiers lingered for a while).
     * 
     * @return A list of the role assignee identifiers.
     */
    public Set<String> getContainedRoleAssgineeIdentifiers() {
        Set<String> retVal = new TreeSet<>();
        retVal.addAll( containedRoleAssignees );
        for ( ExplicitGroup subg : containedExplicitGroups ) {
            retVal.add( subg.getIdentifier() );
        }
        for ( AuthenticatedUser au : containedAuthenticatedUsers ) {
            retVal.add( au.getIdentifier() );
        }
        
        return retVal;
    }
    
    public Set<AuthenticatedUser> getContainedAuthenticatedUsers(){
        return this.containedAuthenticatedUsers;
    }
    
    public Set<String> getContatinedGlobalRoleAssignee(){
        return this.containedRoleAssignees;
    }
    
    public Set<ExplicitGroup> getContainedExplicitGroups(){
        return this.containedExplicitGroups;
    }
    
    public void removeByRoleAssgineeIdentifier( String idtf ) {
        if ( containedRoleAssignees.contains(idtf) ) {
            containedRoleAssignees.remove(idtf);
        } else {
            for ( AuthenticatedUser au : containedAuthenticatedUsers ) {
                if ( au.getIdentifier().equals(idtf) ) {
                    containedAuthenticatedUsers.remove(au);
                    return;
                }
            }
            for ( ExplicitGroup eg : containedExplicitGroups ) {
                if ( eg.getIdentifier().equals(idtf) ) {
                    containedExplicitGroups.remove(eg);
                    return;
                }
            }
        }
    }
    
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean contains(RoleAssignee ra) {
        return containsDirectly(ra) || containsIndirectly(ra);
    }
    
    protected boolean containsDirectly( RoleAssignee ra ) {
        if ( ra instanceof AuthenticatedUser ) {
            AuthenticatedUser au = (AuthenticatedUser) ra;
            return containedAuthenticatedUsers.contains(au);
            
        } else if ( ra instanceof ExplicitGroup ) {
            ExplicitGroup eg = (ExplicitGroup) ra;
            return containedExplicitGroups.contains(eg);
            
        } else {
           return containedRoleAssignees.contains( ra.getIdentifier() );
        }
    }

    private boolean containsIndirectly(RoleAssignee ra) {
        for ( ExplicitGroup ceg : containedExplicitGroups ) {
            if ( ceg.contains(ra) ) {
                return true;
            }
        }
        
        for ( String containedRAIdtf : containedRoleAssignees ) {
            RoleAssignee containedRa = provider.findRoleAssignee(containedRAIdtf);
            if ( containedRa != null ) {
                if ( containedRa instanceof Group ) {
                    if (((Group)containedRa).contains(ra)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Updates the alias of the group. Call this after setting the owner or the 
     * groupAliasInOwner fields. JPA-related activities call this automatically.
     */
    public void updateAlias() {
        groupAlias = ((getOwner()!=null) 
                           ? Long.toString(getOwner().getId()) + "-" 
                           : "") + getGroupAliasInOwner();
    }
    
    @PrePersist
    void prepersist() {
        updateAlias();
    }
    
    @PostLoad
    void postload() {
        updateAlias();
    }
    
    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public ExplicitGroupProvider getGroupProvider() {
        return provider;
    }
    
    public void setProvider( ExplicitGroupProvider c ) {
        provider = c;
    }

    @Override
    public String getIdentifier() {
        return Group.IDENTIFIER_PREFIX + provider.getGroupProviderAlias()
                + Group.PATH_SEPARATOR + getAlias();
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
        return new RoleAssigneeDisplayInfo(getDisplayName(), null);
    }

    public String getGroupAliasInOwner() {
        return groupAliasInOwner;
    }

    public void setGroupAliasInOwner(String groupAliasInOwner) {
        this.groupAliasInOwner = groupAliasInOwner;
    }
    
    @Override
    public String getAlias() {
        return groupAlias;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public DvObject getOwner() {
        return owner;
    }

    public void setOwner(DvObject owner) {
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.id);
        hash = 53 * hash + Objects.hashCode(this.groupAliasInOwner);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof ExplicitGroup)) {
            return false;
        }
        final ExplicitGroup other = (ExplicitGroup) obj;
        if ( id!=null && other.getId()!=null) {
            return Objects.equals(id, other.getId());
        } else {
            return Objects.equals(this.groupAliasInOwner, other.groupAliasInOwner)
                    && Objects.equals(this.owner, other.owner);
        }
    }
    
    /**
     * Low-level call to return the role assignee identifier strings. Note that
     * the role assignees themselves might be stale, which is why this call is here - 
     * to allow the {@link ExplicitGroupServiceBean} to clean up this collection.
     * @return the strings of the role assignees in this group.
     */
    Set<String> getContainedRoleAssignees() {
        return containedRoleAssignees;
    }
    
    @Override
    public String toString() {
        return "[ExplicitGroup " + groupAlias + "]";
    }

    public List<AuthenticatedUser> getJoinGroupRequesters() {
        return joinGroupRequesters;
    }

    public void setJoinGroupRequesters(List<AuthenticatedUser> joinGroupRequesters) {
        this.joinGroupRequesters = joinGroupRequesters;
    }

    public UserType getRequestJoinUserType() {
        return requestJoinUserType;
    }

    public void setRequestJoinUserType(UserType requestJoinUserType) {
        this.requestJoinUserType = requestJoinUserType;
    }
}
