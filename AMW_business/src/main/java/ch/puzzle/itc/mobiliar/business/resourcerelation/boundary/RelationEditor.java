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

package ch.puzzle.itc.mobiliar.business.resourcerelation.boundary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.google.common.base.Enums;

import org.apache.commons.lang3.StringUtils;

import ch.puzzle.itc.mobiliar.business.foreignable.control.ForeignableService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceGroupLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceGroupPersistenceService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.NamedIdentifiable;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationService;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.AbstractResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceTypeNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;

/**
 * A boundary for relation editing
 * 
 * @author cweber
 */
@Stateless
public class RelationEditor {

	@Inject
	EntityManager entityManager;

	@Inject
	ResourceRelationService resourceRelationService;

	@Inject
	ResourceRelationBoundary resourceRelationBoundary;

	@Inject
	ResourceTypeDomainService resourceTypeDomainService;

	@Inject
	ResourceRepository resourceRepository;

	@Inject
	ResourceLocator resourceLocator;

	@Inject
	ResourceGroupLocator resourceGroupLocator;

    @Inject
    ForeignableService foreignableService;

	@Inject
	private ResourceGroupPersistenceService resourceGroupService;

	public enum ResourceRelationType {
		CONSUMED,
		PROVIDED
	}

	/**
	 * Load all resourceGroups for the given type
	 * 
	 * @param typeName
	 * @param resourceId
	 *             - the resourceGroup for this resource will be excluded
	 * @return a list with resourceGroup entities
	 */
	public List<? extends NamedIdentifiable> loadResourceGroupsForType(String typeName, Integer resourceId) {
		ResourceEntity resource = resourceRepository
				.loadWithResourceGroupAndRelatedResourcesForId(resourceId);
		return resourceGroupService.loadGroupsForTypeNameExcludeSelected(typeName,
				Collections.singletonList(resource.getResourceGroup().getId()));
	}

	/**
	 * @param masterId
	 * @param provided
	 * @param relationName
	 * @throws ResourceNotFoundException
	 * @throws ElementAlreadyExistsException
	 */
	public void addRelation(Integer masterId, Integer slaveGroupId, boolean provided, String relationName,
			ForeignableOwner changingOwner) throws ResourceNotFoundException, ElementAlreadyExistsException {
		resourceRelationService.addRelationByGroup(masterId, slaveGroupId, provided, relationName, null, changingOwner);
	}

	/**
	 *
	 * @param masterGroupName
	 * @param slaveGroupName
	 * @param provided
	 * @param relationName
	 * @param typeIdentifier
	 * @param releaseName
	 * @param changingOwner
	 * @throws ResourceNotFoundException
	 * @throws ElementAlreadyExistsException
	 * @throws ValidationException
	 */
	public void addResourceRelationForSpecificRelease(String masterGroupName, String slaveGroupName, boolean provided,
			String relationName, String typeIdentifier, String releaseName, ForeignableOwner changingOwner)
			throws ResourceNotFoundException, ElementAlreadyExistsException, ValidationException {

		ResourceEntity master = resourceLocator.getResourceByGroupNameAndRelease(masterGroupName, releaseName);
		if (master == null) {
			throw new ResourceNotFoundException("Resource with name '" + masterGroupName + "' and Release '" + releaseName + "' not found");
		}
		// a Resource shall only be provided by one ResourceGroup
		if (typeIdentifier.toLowerCase().equals("provided") && !resourceRelationBoundary.isAddableAsProvidedResourceToResourceGroup(master, slaveGroupName)) {
			throw new ValidationException("Resource '" + slaveGroupName + "' is already provided by another ResourceGroup");
		}

		ResourceGroupEntity slaveGroup = null;
		try {
			slaveGroup = resourceGroupLocator.getResourceGroupByName(slaveGroupName);
		} catch (RuntimeException e) {
			throw new ResourceNotFoundException("ResourceGroup with name '" + slaveGroupName + "' not found");
		}

		resourceRelationService.addRelationByGroup(master.getId(), slaveGroup.getId(), provided, relationName,
				typeIdentifier, changingOwner);
	}

	public void addResourceTypeRelation(ResourceTypeEntity masterType, Integer slaveResourceTypeId)
			throws ResourceTypeNotFoundException {
		ResourceTypeEntity slaveResourceType = entityManager.find(ResourceTypeEntity.class,
				slaveResourceTypeId);
		Set<ResourceRelationTypeEntity> relations = slaveResourceType.getResourceRelationTypesB();
		List<String> identifiers = new ArrayList<>();
		for (ResourceRelationTypeEntity relation : relations) {
			if (relation.getResourceTypeA().getId().equals(masterType.getId())) {
				identifiers.add(relation.getRelationIdentifier());
			}
		}
		resourceTypeDomainService.createResourceTypeRelation(masterType.getId(), slaveResourceTypeId,
				resourceTypeDomainService.nextFreeIdentifier(identifiers, slaveResourceType.getName(),
						null));
	}

	/**
	 * Removes a consumed or provided ResourceRelationEntity, identified either by its relation name (aka relation identifier) or by the name of the slave resouce (group)
	 *
	 * @param relations a Collection containing consumed or provided ResourceRelationEntities (the haystack)
	 * @param relationName the relation "name" or the name of the slave resource (group)
	 * @return boolean true if it has been removed, false if it could not be found
	 * @throws ForeignableOwnerViolationException
	 * @throws ResourceNotFoundException
	 * @throws ElementAlreadyExistsException
	 */
	public boolean removeMatchingRelation(Collection<? extends AbstractResourceRelationEntity> relations, String relationName)
			throws ForeignableOwnerViolationException, ResourceNotFoundException, ElementAlreadyExistsException {
		for (AbstractResourceRelationEntity relation : relations) {
			if (isMatchingRelationName(relation, relationName)) {
				removeRelation(ForeignableOwner.getSystemOwner(), relation.getId());
				return true;
			}
		}
		return false;
	}

	protected boolean isMatchingRelationName(AbstractResourceRelationEntity relation, String relationName) {
		if (relation.getIdentifier() != null && relation.getIdentifier().equals(relationName)) {
			return true;
		}
		return relation.getIdentifier() == null && relation.getSlaveResource().getName().equals(relationName);
	}

	/**
	 * @param relationId
	 * @throws ResourceNotFoundException
	 * @throws ElementAlreadyExistsException
	 */
	public void removeRelation(ForeignableOwner deletingOwner, Integer relationId) throws ResourceNotFoundException, ElementAlreadyExistsException, ForeignableOwnerViolationException {
        AbstractResourceRelationEntity resourceRelationEntity = resourceRelationService.getResourceRelation(relationId);

        foreignableService.verifyDeletableByOwner(deletingOwner, resourceRelationEntity);
        resourceRelationService.removeRelation(resourceRelationEntity);
	}

	/**
	 * @throws ResourceNotFoundException
	 * @throws ElementAlreadyExistsException
	 */
	public void removeResourceTypeRelation(Integer resourceTypeRelationId) throws
			ResourceTypeNotFoundException {
		resourceTypeDomainService.removeResourceTypeRelation(resourceTypeRelationId);
	}

	public Set<ResourceGroupEntity> getApplicationsFromOtherResourcesInGroup(ResourceEntity resource) {

		// ... but we can also add those applications, which are already consumed by another resource of our
		// own resoure group as long as it is not connected to our release...
		Set<ResourceGroupEntity> applicationsFromOtherResourcesInGroup = new HashSet<>();
		resource = entityManager.find(ResourceEntity.class, resource.getId());
		for (ResourceEntity r : resource.getResourceGroup().getResources()) {
			if (!resource.equals(r)) {
				List<ResourceEntity> resourceEntities = r
						.getConsumedRelatedResourcesByResourceType(DefaultResourceTypeDefinition.APPLICATION);
				if (resourceEntities != null) {
					for (ResourceEntity r2 : resourceEntities) {
						applicationsFromOtherResourcesInGroup.add(getResourceGroupWithAllResources(r2
								.getResourceGroup().getId()));
					}
				}
			}
		}
		return applicationsFromOtherResourcesInGroup;
	}

	public ResourceGroupEntity getResourceGroupWithAllResources(Integer resourceGroupId) {
		try {
			TypedQuery<ResourceGroupEntity> resGroupQuery = entityManager
					.createQuery(
							"select rg from ResourceGroupEntity rg left join fetch rg.resources where rg.id=:id",
							ResourceGroupEntity.class).setParameter("id", resourceGroupId);
			return resGroupQuery.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	public boolean isValidResourceRelationType(String resourceRelationTypeString) {
		if (StringUtils.isEmpty(resourceRelationTypeString)) {
			return false;
		}
		return (Enums.getIfPresent(ResourceRelationType.class, resourceRelationTypeString.toUpperCase()).isPresent());
	}

}
