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

package ch.puzzle.itc.mobiliar.business.releasing.control;

import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.*;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class ReleaseMgmtPersistenceService {

	@Inject
	EntityManager entityManager;

	@Inject
	private Logger log;

	private Order getDefaultOrder(Root<ReleaseEntity> root, boolean desc) {
		if (desc) {
			return entityManager.getCriteriaBuilder().desc(root.get("installationInProductionAt"));
		}
		return entityManager.getCriteriaBuilder().asc(root.get("installationInProductionAt"));
	}

	/**
	 * @param startIndex
	 * @param length
	 * @param sortDesc
	 *             , sort direction for release date
	 * @return a sublist of all releaseEntities ordered by installationInProductionAt starting from the given
	 *         startIndex and with the given length
	 */
	public List<ReleaseEntity> loadReleaseEntities(int startIndex, int length, boolean sortDesc) {
		CriteriaQuery<ReleaseEntity> query = entityManager.getCriteriaBuilder().createQuery(ReleaseEntity.class);
		Root<ReleaseEntity> root = query.from(ReleaseEntity.class);
		query.orderBy(getDefaultOrder(root, sortDesc));
		return entityManager.createQuery(query).setFirstResult(startIndex).setMaxResults(length).getResultList();
	}

	/**
	 * @return a list containing all release entities ordered by installationInProductionAt
	 */
	public List<ReleaseEntity> loadAllReleaseEntities(boolean sortDesc) {
		CriteriaQuery<ReleaseEntity> query = entityManager.getCriteriaBuilder().createQuery(ReleaseEntity.class);
		Root<ReleaseEntity> root = query.from(ReleaseEntity.class);
		query.orderBy(getDefaultOrder(root, sortDesc));
		return entityManager.createQuery(query).getResultList();
	}

	/**
	 * @return the default release which is the release with the lowest date
	 */
	public ReleaseEntity getDefaultRelease(){
		CriteriaQuery<ReleaseEntity> query = entityManager.getCriteriaBuilder().createQuery(ReleaseEntity.class);
		Root<ReleaseEntity> root = query.from(ReleaseEntity.class);
		query.orderBy(entityManager.getCriteriaBuilder().asc(root.get("installationInProductionAt")));
		return entityManager.createQuery(query).setMaxResults(1).getSingleResult();
	}


	/**
	 * @param releaseName
	 * @return the release with the given name or null if it does not exist
	 */
	public ReleaseEntity findByName(String releaseName) {
		ReleaseEntity release = null;
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ReleaseEntity> q = cb.createQuery(ReleaseEntity.class);
		Root<ReleaseEntity> r = q.from(ReleaseEntity.class);
		Predicate resNamePred = cb.equal(r.get("name"), releaseName);

		q.where(cb.and(resNamePred));

		try {
			release = entityManager.createQuery(q).getSingleResult();
		}
		catch (NoResultException e) {
			// do nothing
		}
		return release;
	}

	/**
	 * Saves the release entity - it doesn't matter if the entity is already
	 * persisted or not! This method will take care of the correct persistence
	 * actions.
	 * 
	 * @param entity
	 */
	public boolean saveReleaseEntity(ReleaseEntity entity) {
		if(entity.getId()==null){
			entityManager.persist(entity);
			log.info("Release " + entity.getName() + " saved");
		}
		else{
			entityManager.merge(entity);
			log.info("Release " + entity.getName() + " updated");
		}
		return true;
	}


	/**
	 * Deletes the entity with the given id
	 * @param id
	 */
	public boolean deleteReleaseEntity(int id) {
		ReleaseEntity r;
		try {
			r = entityManager.find(ReleaseEntity.class, id);
			entityManager.remove(r);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (TransactionRequiredException e) {
			throw e;
		}
		log.info("Release " + r.getName() + " removed");
		return true;
	}


	/**
	 * Counts the number of releases available.
	 */
	public int count(){
		CriteriaQuery<Long> cq = entityManager.getCriteriaBuilder().createQuery(Long.class);
		cq.select(entityManager.getCriteriaBuilder().count(cq.from(ReleaseEntity.class)));
		Long result = entityManager.createQuery(cq).getSingleResult();
		return result!=null ? result.intValue() : 0;
	}


	/**
	 * @param id
	 * @return the releaseEntity for the given id or null if it does not exist
	 */
	public ReleaseEntity getById(int id){
		return entityManager.find(ReleaseEntity.class, id);
	}

	public List<ResourceEntity> getResourcesForRelease(Integer releaseId) {
		ReleaseEntity release = entityManager.find(ReleaseEntity.class, releaseId);
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<ResourceEntity> q = cb.createQuery(ResourceEntity.class);
		Root<ResourceEntity> r = q.from(ResourceEntity.class);
		Predicate releasePrd = cb.equal(r.get("release"), release);
		q.where(cb.and(releasePrd));
		List<ResourceEntity> result = entityManager.createQuery(q).getResultList();
		return result;
	}

	public void changeReleaseOfResource(Integer resourceId, Integer releaseId) throws ResourceNotFoundException {
		ResourceEntity resource = entityManager.find(ResourceEntity.class, resourceId);
		ReleaseEntity release = entityManager.find(ReleaseEntity.class, releaseId);
		if (resource == null) {
			throw new ResourceNotFoundException("Resource with id " + resourceId + " not found");
		}
		if (release == null) {
			throw new ResourceNotFoundException("Release with id " + releaseId + " not found");
		}
		resource.setRelease(release);
		entityManager.persist(resource);
	}
}
