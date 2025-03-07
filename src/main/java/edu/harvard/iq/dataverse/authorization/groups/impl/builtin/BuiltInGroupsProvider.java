package edu.harvard.iq.dataverse.authorization.groups.impl.builtin;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.Collections;
import java.util.Set;
import org.hibernate.validator.internal.util.CollectionHelper;

/**
 * Provider for the built-in, hard coded groups. This class is a singleton (no
 * point in having more than one) so please use {@link #get()} to obtain the
 * instance.
 *
 * @author michael
 */
public class BuiltInGroupsProvider implements GroupProvider<Group> {
    
    private static final BuiltInGroupsProvider instance = new BuiltInGroupsProvider();
    
    private BuiltInGroupsProvider(){}
    
    public static BuiltInGroupsProvider get() {
        return instance;
    }

    @Override
    public String getGroupProviderAlias() {
        return "builtIn";
    }

    @Override
    public String getGroupProviderInfo() {
        return "Holder for groups built into dataverse.";
    }

    @Override
    public Set<Group> groupsFor(RoleAssignee ra, DvObject o) {
        return (Set<Group>) ((ra instanceof AuthenticatedUser)
                ? CollectionHelper.asSet(AllUsers.get(), AuthenticatedUsers.get())
                : Collections.singleton(AllUsers.get()));
    }

    @Override
    public Group get(String groupAlias) {
        return groupAlias.equals(AllUsers.get().getAlias()) ? AllUsers.get()
                : ( groupAlias.equals(AuthenticatedUsers.get().getAlias()) ? AuthenticatedUsers.get() : null );
    }
    
    public Group getByIdentifier(String identifier){
        return identifier.equals(AllUsers.get().getIdentifier()) ? AllUsers.get()
                : (identifier.equals(AuthenticatedUsers.get().getIdentifier()) ? AuthenticatedUsers.get() : null);
    }

    @Override
    public Set<Group> findGlobalGroups() {
        return CollectionHelper.asSet(AllUsers.get(), AuthenticatedUsers.get());
    }

}
