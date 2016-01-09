package edu.harvard.iq.dataverse;

import cn.edu.pku.lib.dataverse.authorization.groups.impl.iaaa.IAAAUsers;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AllUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * The place to obtain {@link RoleAssignee}s, based on their identifiers.
 *
 * @author michael
 */
@Stateless
@Named
public class RoleAssigneeServiceBean {

    private static final Logger logger = Logger.getLogger(RoleAssigneeServiceBean.class.getName());
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    AuthenticationServiceBean authSvc;

    @EJB
    GroupServiceBean groupSvc;

    Map<String, RoleAssignee> predefinedRoleAssignees = new TreeMap<>();

    @PostConstruct
    void setup() {
        GuestUser gu = new GuestUser();
        predefinedRoleAssignees.put(gu.getIdentifier(), gu);
        predefinedRoleAssignees.put(AuthenticatedUsers.get().getIdentifier(), AuthenticatedUsers.get());
        predefinedRoleAssignees.put(AllUsers.get().getIdentifier(), AllUsers.get());
        predefinedRoleAssignees.put(IAAAUsers.get().getIdentifier(), IAAAUsers.get());
    }

    public RoleAssignee getRoleAssignee(String identifier) {
        switch (identifier.charAt(0)) {
            case ':':
                return predefinedRoleAssignees.get(identifier);
            case '@':
                return authSvc.getAuthenticatedUser(identifier.substring(1));
            case '&':
                return groupSvc.getGroup(identifier.substring(1));
            default:
                throw new IllegalArgumentException("Unsupported assignee identifier '" + identifier + "'");
        }
    }

    public List<RoleAssignment> getAssignmentsFor(String roleAssigneeIdentifier) {
        return em.createNamedQuery("RoleAssignment.listByAssigneeIdentifier", RoleAssignment.class)
                .setParameter("assigneeIdentifier", roleAssigneeIdentifier)
                .getResultList();
    }

    public List<AuthenticatedUser> getExplicitUsers(RoleAssignee ra) {
        List<AuthenticatedUser> explicitUsers = new ArrayList();
        if (ra instanceof AuthenticatedUser) {
            explicitUsers.add((AuthenticatedUser) ra);
        } else if (ra instanceof ExplicitGroup) {
            ExplicitGroup group = (ExplicitGroup) ra;
            for (String raIdentifier : group.getContainedRoleAssgineeIdentifiers()) {
                explicitUsers.addAll(getExplicitUsers(getRoleAssignee(raIdentifier)));
            }
        }

        return explicitUsers;

    }
}
