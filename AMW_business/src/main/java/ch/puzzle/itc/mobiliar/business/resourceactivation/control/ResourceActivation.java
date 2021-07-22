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

package ch.puzzle.itc.mobiliar.business.resourceactivation.control;

import ch.puzzle.itc.mobiliar.business.environment.control.ContextHierarchy;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.resourceactivation.entity.ResourceActivationEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationContextRepository;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationContextEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.*;

public class ResourceActivation {

	@Inject
	EntityManager entityManager;

	@Inject
	ContextHierarchy contextHierarchy;

	@Inject
	ResourceRelationContextRepository resourceRelationContextRepository;

	/**
	 * This method allows to persist a resourceactivation configuration.
	 *
	 * @param currentResourceRelationContext
	 * @param resourceGroup
	 * @param existingResourceActivation
	 * @param active
	 *             - true if the resource shall be active, false if it should be inactive, null if the
	 *             existing resource activation entry shall be removed
	 */
	public void setResourceActivation(ResourceRelationContextEntity currentResourceRelationContext, ResourceGroupEntity resourceGroup, ResourceActivationEntity existingResourceActivation, Boolean active) {
		//TODO cleanup active resource activation without "inactive" in between over the whole context hierarchy.
	    	boolean alreadyDefinedOnCurrentContext;
		if (existingResourceActivation != null
				&& existingResourceActivation.getResourceRelationContext().getContext().getId()
						.equals(currentResourceRelationContext.getContext().getId())) {
			alreadyDefinedOnCurrentContext = true;
		}
		else {
			alreadyDefinedOnCurrentContext = false;
		}
		if (alreadyDefinedOnCurrentContext) {
			// If active is set to null, this means a reset
			if (active == null) {
				entityManager.remove(existingResourceActivation);
			}
			else {
				// We only need to persist it if the value has changed...
				if (existingResourceActivation.isActive() != active.booleanValue()) {
					// If it is the only resource activation in the context hierarchy for this resource
					// relation and shall be active, we remove it - since active is default and we want
					// to prevent the persistence of unnecessary data.
					if (active.booleanValue()
							&& existingResourceActivation.isOnlyActivationEntityForResourceRelation()) {
						entityManager.remove(existingResourceActivation);
					}
					else {
						// activation entity for this relation.
						existingResourceActivation.setActive(active.booleanValue());
						entityManager.persist(existingResourceActivation);
					}
				}
			}
		}
		// If it is the first resource activation in the context hierarchy and shall be active, we don't
		// want to add it - since active is default and we want to prevent the persistence of unnecessary
		// data.
		else if (active != null && (!active.booleanValue() || existingResourceActivation != null)) {
			ResourceActivationEntity entity = new ResourceActivationEntity();
			entity.setResourceGroup(resourceGroup);
			entity.setResourceRelationContext(currentResourceRelationContext);
			entity.setActive(active.booleanValue());
			entityManager.persist(entity);
		}
	}

	/**
	 * This method returns a list of the (for the given context) relevant resource activation entities -
	 * taking into account all context hierarchies.
	 *
	 * @param consumedResourceRelationEntity
	 * @param context
	 * @return
	 */
	public List<ResourceActivationEntity> getMostRelevantResourceActivationEntities(
			ConsumedResourceRelationEntity consumedResourceRelationEntity, ContextEntity context) {
		List<ResourceRelationContextEntity> resourceRelationContexts = getResourceActivationEntitiesForContextHierarchy(
				consumedResourceRelationEntity, context);
		Map<Integer, ResourceActivationEntity> resultMap = new HashMap<>();
		for (ResourceRelationContextEntity ctx : resourceRelationContexts) {
			for (ResourceActivationEntity resourceActivationEntity : ctx.getResourceActivationEntities()) {
				if (!resultMap.containsKey(resourceActivationEntity.getResourceGroup().getId())) {
					resultMap.put(resourceActivationEntity.getResourceGroup().getId(),
							resourceActivationEntity);
					resourceActivationEntity.setOnlyActivationEntityForResourceRelation(true);
				}
				else {
					resultMap.get(resourceActivationEntity.getResourceGroup().getId())
							.setOnlyActivationEntityForResourceRelation(false);
				}
			}
		}
		return new ArrayList<>(resultMap.values());
	}

    /**
	* Example: if context is "B", the resource relation contexts of "B", "DEV" and "Global" (if available) will be returned.
	*
	* @param consumedResourceRelationEntity
	* @param context - the lowest context to be taken into account for the resource relation contexts
	* @return all available resource relation contexts, sorted by their hierarchy (lowest first, global last) for the given consumed resource relation in the context hierarchy beginning from the given context
	*/
	List<ResourceRelationContextEntity> getResourceActivationEntitiesForContextHierarchy(
			ConsumedResourceRelationEntity consumedResourceRelationEntity, ContextEntity context) {
		List<Integer> contextIds = contextHierarchy.getContextWithParentIds(context);
		List<ResourceRelationContextEntity> rel = resourceRelationContextRepository
				.getResourceRelationContextEntitiesByContextIds(consumedResourceRelationEntity,
						contextIds);
	     //Now that we have the resource relation contexts, we sort it by the hierarchy of contexts
		Map<Integer, ResourceRelationContextEntity> idmap = new HashMap<>();
		for (ResourceRelationContextEntity resRelCtx : rel) {
			idmap.put(resRelCtx.getContext().getId(), resRelCtx);
		}
		// We revert the list since we want the most relevant context first...
		Collections.reverse(contextIds);
		List<ResourceRelationContextEntity> relevantContexts = new ArrayList<>();
		for (Integer contextId : contextIds) {
			ResourceRelationContextEntity ctx = idmap.get(contextId);
			if (ctx != null) {
				relevantContexts.add(ctx);
			}
		}
		return relevantContexts;
	}

}
