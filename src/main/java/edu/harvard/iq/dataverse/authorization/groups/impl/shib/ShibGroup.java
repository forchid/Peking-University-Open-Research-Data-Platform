package edu.harvard.iq.dataverse.authorization.groups.impl.shib;

import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * Persistence for Shibboleth groups.
 */
@Entity
public class ShibGroup implements Group, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public void setShibGroupProvider(ShibGroupProvider shibGroupProvider) {
        this.shibGroupProvider = shibGroupProvider;
    }

    /**
     * The name of the group that will be displayed to the end user.
     */
    @Column(nullable = false)
    private String name;

    /**
     * The Shibboleth attribute to match against, such as
     * "Shib-Identity-Provider" or "memberOf".
     */
    @Column(nullable = false)
    private String attribute;

    /**
     * A regular expression to match the attribute against.
     */
    @Column(nullable = false)
    private String pattern;

    @Transient
    private ShibGroupProvider shibGroupProvider;

    /**
     * This is only here because it has to be: "The class should have a no-arg,
     * public or protected constructor." Please use the constructor that takes
     * arguments.
     */
    @Deprecated
    public ShibGroup() {
    }

    public ShibGroup(String name, String attribute, String pattern, ShibGroupProvider shibGroupProvider) {
        this.name = name;
        this.attribute = attribute;
        this.pattern = pattern;
        this.shibGroupProvider = shibGroupProvider;
    }

    @Override
    public String toString() {
        return "ShibGroup{" + "id=" + id + ", name=" + name + ", attribute=" + attribute + ", pattern=" + pattern + '}';
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public String getAlias() {
        return ShibGroupProvider.getShibProviderAlias() + Group.PATH_SEPARATOR + getId().toString();
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEditable() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public GroupProvider getGroupProvider() {
        return shibGroupProvider;
    }

    /**
     * i.e. &shib/1
     */
    @Override
    public String getIdentifier() {
        return Group.IDENTIFIER_PREFIX + ShibGroupProvider.getShibProviderAlias() + Group.PATH_SEPARATOR + getId();
    }

    @Override
    public RoleAssigneeDisplayInfo getDisplayInfo() {
//            String debugTitle = "Shibboleth group " + getId() + " (" + getAlias() + ") \"" + getAttribute() + "\" exact string match of \"" + getPattern() + "\"";
        String title = getName();
        /**
         * @todo should email be null for Shibboleth groups?
         */
//        String email = "FIXME RoleAssigneeDisplayInfo email for shibgroup id " + getId();
        String email = null;
        RoleAssigneeDisplayInfo roleAssigneeDisplayInfo = new RoleAssigneeDisplayInfo(title, email);
        return roleAssigneeDisplayInfo;
    }

    @Override
    public boolean contains(RoleAssignee anAssignee) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
