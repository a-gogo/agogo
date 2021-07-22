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

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.*;

import ch.puzzle.itc.mobiliar.business.database.control.QueryUtils;
import ch.puzzle.itc.mobiliar.business.environment.control.ContextDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.Application;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.common.util.ApplicationServerContainer;

/**
 * Use {@link ResourceGroupRepository} - move all functionality to the control to fulfill the cec pattern
 */
@Stateless
@Deprecated
public class ResourceGroupPersistenceService {

	@Inject
	EntityManager entityManager;

	@Inject
	ResourceTypeProvider resourceTypeProvider;

	@Inject
	ContextDomainService context;

	/**
	 * @param id
	 * @return the entity with the given id
	 */
	public ResourceGroupEntity getById(int id) {
		return entityManager.find(ResourceGroupEntity.class, id);
	}

	/**
	 * @param resourceTypeName
	 * @param myAmw
	 * @return
	 */
	public List<ResourceGroupEntity> loadGroupsForTypeName(String resourceTypeName, List<Integer> myAmw) {
		List<ResourceGroupEntity> result = new ArrayList<>();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ResourceGroupEntity> q = cb.createQuery(ResourceGroupEntity.class);
		Root<ResourceGroupEntity> r = q.from(ResourceGroupEntity.class);
		r.fetch("resources");
		Join<ResourceGroupEntity, ResourceEntity> resources = r.join("resources");
		Predicate typeNamePred = cb.equal(resources.get("resourceType").get("name"), resourceTypeName);
		Predicate asContainerPred = cb.notEqual(resources.get("name"),
				ApplicationServerContainer.APPSERVERCONTAINER.getDisplayName());

		if (myAmw != null && !myAmw.isEmpty()) {
			Predicate myAmwPred = r.get("id").in(myAmw);
			q.where(cb.and(cb.and(typeNamePred, asContainerPred), myAmwPred));
		}
		else {
			q.where(cb.and(typeNamePred, asContainerPred));
		}

		q.distinct(true);

		result = entityManager.createQuery(q).getResultList();

		return result;
	}

	/**
	 * @param resourceTypeName
	 * @param excludeGroups
	 * @return list of resource group entities
	 */
	public List<ResourceGroupEntity> loadGroupsForTypeNameExcludeSelected(String resourceTypeName,
			List<Integer> excludeGroups) {

		List<ResourceGroupEntity> result = new ArrayList<>();

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ResourceGroupEntity> q = cb.createQuery(ResourceGroupEntity.class);
		Root<ResourceGroupEntity> r = q.from(ResourceGroupEntity.class);
		Join<ResourceGroupEntity, ResourceEntity> resources = r.join("resources");
		r.fetch("resources", JoinType.LEFT);
		Predicate typeNamePred = cb.equal(resources.get("resourceType").get("name"), resourceTypeName);

		if (excludeGroups != null && !excludeGroups.isEmpty()) {
			Predicate excludePred = r.get("id").in(excludeGroups);
			q.where(cb.and(typeNamePred, cb.not(excludePred)));
		}
		else {
			q.where(typeNamePred);
		}

		q.distinct(true);

		result = entityManager.createQuery(q).getResultList();

		return result;
	}

	/**
	 * @param name
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

	/**
	 * Listet Applications auf alphabetic sorted
	 *
	 * @return
	 */
	public List<Application> getAllApplicationsNotBelongingToAServer() {
		List<Application> allApplications = new ArrayList<>();
		Query query = entityManager
				.createQuery(
						"select resEnt from ResourceEntity resEnt left join fetch resEnt.resourceGroup rg left join fetch rg.resources otherRes left join fetch resEnt.contexts left join fetch resEnt.consumedSlaveRelations as resConsSlaveRel left join fetch resConsSlaveRel.masterResource as master where master.name=:name order by resEnt.name")
				.setParameter("name", ApplicationServerContainer.APPSERVERCONTAINER.getDisplayName());

		List<ResourceEntity> allApplicationsResourceEntities = QueryUtils.fetch(ResourceEntity.class,
				query, 0, -1);

		ContextEntity ctx = context.getGlobalResourceContextEntity();
		for (ResourceEntity resourceEntity : allApplicationsResourceEntities) {
			Application application = Application.createByResource(resourceEntity, resourceTypeProvider,
					ctx);
			allApplications.add(application);
		}

		return allApplications;
	}

}
