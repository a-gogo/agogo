/*
 * AMW - Automated Middleware allows you to manage the configurations of
 * your Java EE applications on an unlimited number of different environments
 * with various versions, including the automated deployment of those apps.
 * Copyright (C) 2013-2016 by Puzzle ITC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.puzzle.itc.mobiliar.business.resourcegroup.control;

import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.common.util.ApplicationServerContainer;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class ResourceGroupRepository {

    @Inject
    EntityManager entityManager;

    @Inject
    private Logger log;

    public List<ResourceGroupEntity> getResourceGroups(){
        return entityManager.createQuery("select distinct r from ResourceGroupEntity r left join fetch r.resources res", ResourceGroupEntity.class).getResultList();
    }

    public List<ResourceGroupEntity> getAllResourceGroupsByName(){
        return entityManager.createQuery("select r from ResourceGroupEntity r order by r.name", ResourceGroupEntity.class).getResultList();
    }

    public ResourceGroupEntity getResourceGroupByName(String name){
        name = name.toLowerCase();
        return entityManager.createQuery("select r from ResourceGroupEntity r where LOWER(r.name)=:name", ResourceGroupEntity.class).setParameter(
                "name", name).getSingleResult();
    }

    public ResourceGroupEntity getResourceGroupById(Integer groupId){
        return entityManager.createQuery("select r from ResourceGroupEntity r where r.id=:groupId", ResourceGroupEntity.class).setParameter(
                "groupId", groupId).getSingleResult();
    }

    public List<ResourceGroupEntity> getResourceGroupsOrderedByName(Collection<Integer> resourceGroupIds){
        return entityManager.createQuery("select r from ResourceGroupEntity r where r.id in(:groupIds) order by r.name", ResourceGroupEntity.class).setParameter("groupIds", resourceGroupIds).getResultList();
    }
    
    /**
     * Fetches the resourceGroup as required for the create deployment popup of the deploy screen
     */
	public ResourceGroupEntity getResourceGroupForCreateDeploy(Integer groupId) {
		TypedQuery<ResourceGroupEntity> q = entityManager.createQuery("select r from ResourceGroupEntity r"
        		+ " left join fetch r.resources res"
        		+ " left join fetch res.consumedMasterRelations"
        		+ " left join fetch res.resourceTags"
        		+ " where r.id=:id",
        		ResourceGroupEntity.class);
		q.setParameter("id", groupId);
		return q.getSingleResult();
	}
	
    /**
     * @param resourceTypeName
     * @param myAmw
     * @param fetchResources determines if resources fetched
     * @return a list of Groups
     */    
	public List<ResourceGroupEntity> getGroupsForType(String resourceTypeName, List<Integer> myAmw, boolean fetchResources, boolean sorted) {
		return getGroupsForType("name", resourceTypeName, myAmw, fetchResources, sorted);
	}
	
	public List<ResourceGroupEntity> getGroupsForType(String resourceTypeName, List<Integer> myAmw, boolean fetchResources) {
		return getGroupsForType("name", resourceTypeName, myAmw, fetchResources, false);
	}
	
	public List<ResourceGroupEntity> getGroupsForType(int resourceTypeId, List<Integer> myAmw, boolean fetchResources, boolean sorted) {
		return getGroupsForType("id", resourceTypeId, myAmw, fetchResources, sorted);
	}
	
	public List<ResourceGroupEntity> getGroupsForType(int resourceTypeId, List<Integer> myAmw, boolean fetchResources) {
		return getGroupsForType("id", resourceTypeId, myAmw, fetchResources, false);
	}

    /**
     *
     * @param name
     * @param resourceTypeId
     * @return
     */
    public ResourceGroupEntity loadUniqueGroupByNameAndType(String name, Integer resourceTypeId) {
        ResourceGroupEntity result = null;
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<ResourceGroupEntity> q = cb.createQuery(ResourceGroupEntity.class);
            Root<ResourceGroupEntity> r = q.from(ResourceGroupEntity.class);
            r.fetch("resources");
            Join<ResourceGroupEntity, ResourceEntity> resources = r.join("resources");
            Predicate typePred = cb.equal(resources.get("resourceType").get("id"), resourceTypeId);
            Predicate resNamePred = cb.equal(resources.get("name"), name);

            q.where(cb.and(typePred, resNamePred));

            q.distinct(true);

            result = entityManager.createQuery(q).getSingleResult();
        }
        catch (NoResultException e) {
            // do nothing
        }
        return result;
    }


	
	private List<ResourceGroupEntity> getGroupsForType(String typeParam, Object typeParamValue, List<Integer> myAmw, boolean fetchResources, boolean sorted) {
        StringBuilder qString = new StringBuilder();
        String fetchString = StringUtils.EMPTY;
        List<ResourceGroupEntity> result;
        
        if(fetchResources) {
        	fetchString = "fetch ";
        }

        qString.append("SELECT DISTINCT g FROM ResourceGroupEntity g left join " + fetchString + "g.resources r");
        qString.append(" WHERE r.resourceType."+typeParam+"=:typePram");
        qString.append(" AND r.name IS NOT :asContainer");
        if (myAmw != null && myAmw.size() != 0) {
            qString.append(" AND g.id in (:myAmw)");
        }

        TypedQuery<ResourceGroupEntity> q = entityManager.createQuery(qString.toString(),
                ResourceGroupEntity.class);
        q.setParameter("typePram", typeParamValue);
        q.setParameter("asContainer", ApplicationServerContainer.APPSERVERCONTAINER.getDisplayName());
        if (myAmw != null && myAmw.size() != 0) {
            q.setParameter("myAmw", myAmw);
        }
        
        result = q.getResultList();
        
        if(sorted) {
	        Collections.sort(result, new Comparator<ResourceGroupEntity>() {
				@Override
				public int compare(ResourceGroupEntity group1, ResourceGroupEntity group2) {
					return group1.getName().toLowerCase().
							compareTo(group2.getName().toLowerCase());
				}
			});
        }
        return result;

	}

    public ResourceGroupEntity find(Integer resourceGroupId) {
        return entityManager.find(ResourceGroupEntity.class, resourceGroupId);
    }

    /**
     * Removes a ResourceGroupEntity preserving its deployments
     *
     * @param resourceGroup
     */
    public void remove(ResourceGroupEntity resourceGroup) {
        final Integer resourceGroupId = resourceGroup.getId();
        entityManager.remove(resourceGroup);
        preserveDeployments(resourceGroupId);
        log.info("ResourceGroup with Id: " + resourceGroupId + " was removed from the db");
    }

    private void preserveDeployments(Integer resourceGroupId) {
        entityManager.createQuery("update DeploymentEntity d set d.exResourcegroupId =:groupId, d.resourceGroup = null where d.resourceGroup.id =:groupId")
		.setParameter("groupId", resourceGroupId)
		.executeUpdate();
    }

}
