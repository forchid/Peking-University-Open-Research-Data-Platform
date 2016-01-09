package edu.harvard.iq.dataverse.authorization.groups.impl.explicit;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.jboss.logging.Logger;

/**
 * A bean providing the {@link ExplicitGroupProvider}s with container services,
 * such as database connectivity.
 * 
 * @author michael
 */
@Named
@Stateless
public class ExplicitGroupServiceBean {
    
    @EJB
    private RoleAssigneeServiceBean roleAssigneeSvc;
    @EJB
    private PermissionServiceBean permissionService;
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
	protected EntityManager em;
	
    ExplicitGroupProvider provider;
    
    @PostConstruct
    void setup() {
        provider = new ExplicitGroupProvider(this, roleAssigneeSvc);
    }
    
    public ExplicitGroupProvider getProvider() {
        return provider;
    }
    
    public ExplicitGroup persist( ExplicitGroup g ) {
        if ( g.getId() == null ) {
            em.persist( g );
            return g;
        } else {
            // clean stale data once in a while
            if ( Math.random() >= 0.5 ) {
                Set<String> stale = new TreeSet<>();
                for ( String idtf : g.getContainedRoleAssignees()) {
                    if ( roleAssigneeSvc.getRoleAssignee(idtf) == null ) {
                        stale.add(idtf);
                    }
                }
                if ( ! stale.isEmpty() ) {
                    g.getContainedRoleAssignees().removeAll(stale);
                }
            }
            
            return em.merge( g );
        }    
    }
    
    public List<ExplicitGroup> findByOwner( Long dvObjectId ) {
        return provider.updateProvider(em.createNamedQuery( "ExplicitGroup.findByOwnerId" )
                 .setParameter("ownerId", dvObjectId )
                 .getResultList());
    }
    
    public ExplicitGroup findById(Long id) {
        try  {
            return provider.updateProvider( em.createNamedQuery("ExplicitGroup.findById", ExplicitGroup.class)
                    .setParameter("id", id)
                    .getSingleResult());
        } catch ( NoResultException nre ) {
            return null;
        }
    }
    
    public ExplicitGroup findByAlias(String groupAlias) {
        try  {
            return provider.updateProvider( em.createNamedQuery("ExplicitGroup.findByAlias", ExplicitGroup.class)
                    .setParameter("alias", groupAlias)
                    .getSingleResult());
        } catch ( NoResultException nre ) {
            return null;
        }
    }

    public ExplicitGroup findInOwner(Long ownerId, String groupAliasInOwner) {
        try  {
            return provider.updateProvider( 
                    em.createNamedQuery("ExplicitGroup.findByOwnerIdAndAlias", ExplicitGroup.class)
                        .setParameter("alias", groupAliasInOwner)
                        .setParameter("ownerId", ownerId)
                        .getSingleResult());
        } catch ( NoResultException nre ) {
            return null;
        }
    }

    public void removeGroup(ExplicitGroup explicitGroup) {
        em.remove( em.merge(explicitGroup) );
    }
    
    /**
     * Returns all the explicit groups that are available in the context of the passed DvObject.
     * @param d The DvObject where the groups are queried
     * @return All the explicit groups defined at {@code d} and its ancestors.
     */
    public Set<ExplicitGroup> findAvailableFor( DvObject d ) {
        Set<ExplicitGroup> egs = new HashSet<>();
        while ( d != null ) {
            egs.addAll( findByOwner(d.getId()) );
            d = d.getOwner();
        }
        return provider.updateProvider( egs );
    }
    
    public Map<Group,Boolean> findAvailableFor(DvObject d, User user){
        Map<Group, Boolean> map = new LinkedHashMap<>();
        while(d != null){
            List<ExplicitGroup> groups = findByOwner(d.getId());
            boolean permission = permissionService.permissionsFor(user, d)
                    .contains(Permission.ManageDataversePermissions);
            for(ExplicitGroup group : groups){
                map.put(group, permission);
            }
            d = d.getOwner();
        }
        return map;
    }
    
    /**
     * Finds all the groups {@code ra} belongs to in the context of {@code o}. In effect,
     * collects all the groups {@code ra} belongs to and that are defined at {@code o}
     * or one of its ancestors.
     * 
     * @param ra The role assignee that belongs to the groups
     * @param o the DvObject that defines the context of the search.
     * @return All the groups ra belongs to in the context of o.
     */
    public Set<ExplicitGroup> findGroups( RoleAssignee ra, DvObject o ) {
        if ( o == null ) return Collections.emptySet();
        List<ExplicitGroup> groupList = new LinkedList<>();
        
        if ( ra instanceof ExplicitGroup ) {
            for ( DvObject cur = o; cur != null; cur=cur.getOwner() ) {
                groupList.addAll( em.createNamedQuery("ExplicitGroup.findByOwnerAndSubExGroupId", ExplicitGroup.class)
                  .setParameter("ownerId", cur.getId())
                  .setParameter("subExGroupId", ((ExplicitGroup)ra).getId())
                  .getResultList() );
            }
            
        } else if ( ra instanceof AuthenticatedUser ) {
            for ( DvObject cur = o; cur != null; cur=cur.getOwner() ) {
                groupList.addAll( em.createNamedQuery("ExplicitGroup.findByOwnerAndAuthUserId", ExplicitGroup.class)
                  .setParameter("ownerId", cur.getId())
                  .setParameter("authUserId", ((AuthenticatedUser)ra).getId())
                  .getResultList() );
            }
            
        } else {
            for ( DvObject cur = o; cur != null; cur=cur.getOwner() ) {
                groupList.addAll( em.createNamedQuery("ExplicitGroup.findByOwnerAndRAIdtf", ExplicitGroup.class)
                  .setParameter("ownerId", cur.getId())
                  .setParameter("raIdtf", ra.getIdentifier())
                  .getResultList() );
            }
        }
        
        return provider.updateProvider( new HashSet<>(groupList) );
    }
    
}
