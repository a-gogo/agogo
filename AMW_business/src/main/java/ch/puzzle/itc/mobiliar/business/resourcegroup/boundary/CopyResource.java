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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ch.puzzle.itc.mobiliar.business.resourcegroup.boundary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import ch.puzzle.itc.mobiliar.business.domain.commons.CommonDomainService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.releasing.boundary.ReleaseLocator;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceResult;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceGroupPersistenceService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceGroupRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroup;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.NotAuthorizedException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

/**
 * A boundary for copy resourcess
 *
 * @author cweber
 */
@Stateless
public class CopyResource {

    @Inject
    private ResourceGroupRepository resourceGroupRepository;

	@Inject
	private ResourceGroupPersistenceService resourceGroupService;

	@Inject
	private CopyResourceDomainService copyResourceDomainService;

	@Inject
	private ReleaseLocator releaseLocator;

	@Inject
	private EntityManager entityManager;

	@Inject
	private PermissionBoundary permissionBoundary;

	@Inject
	CommonDomainService commonDomainService;

	@Inject
	ResourceLocator resourceLocator;

	/**
	 * @param typeId
	 * @return list of resourceGroups
	 */
	public List<ResourceGroup> loadResourceGroupsForType(Integer typeId, ResourceEntity originResource) {
		List<ResourceGroup> groups = new ArrayList<ResourceGroup>();
		List<ResourceGroupEntity> result = resourceGroupRepository.getGroupsForType(typeId, Collections.<Integer> emptyList(), true);

		for (ResourceGroupEntity g : result) {
			ResourceGroup group = ResourceGroup.createByResource(g, null);
			if (g.getId().equals(originResource.getResourceGroup().getId())) {
				group.addReleaseToExclude(originResource.getRelease());
			}
			groups.add(group);
		}
		return groups;
	}

	/**
	 * @param targetResourceId
	 * @param originResourceId
	 * @return result if copy was successful, contains a list with error messages if copy fails
	 * @throws ResourceNotFoundException
	 */
	public CopyResourceResult doCopyResource(Integer targetResourceId, Integer originResourceId, ForeignableOwner actingOwner)
            throws ForeignableOwnerViolationException, AMWException {
		ResourceEntity targetResource = commonDomainService.getResourceEntityById(targetResourceId);
		ResourceEntity originResource = commonDomainService.getResourceEntityById(originResourceId);

		return  this.doCopyResource(targetResource, originResource, actingOwner);
	}

	/**
	 * @param targetResourceEntity
	 * @param originResourceEntity
	 * @return result if copy was successful, contains a list with error messages if copy fails
	 * @throws ResourceNotFoundException
	 */
	public CopyResourceResult doCopyResource(ResourceEntity targetResourceEntity, ResourceEntity originResourceEntity, ForeignableOwner actingOwner)
			throws ForeignableOwnerViolationException, AMWException {
		if (!originResourceEntity.getResourceType().getId().equals(targetResourceEntity.getResourceType().getId())) {
			throw new AMWException("Target and origin Resource are not of the same ResourceType");
		}

		if(!permissionBoundary.canCopyFromSpecificResource(originResourceEntity, targetResourceEntity.getResourceGroup())){
			throw new NotAuthorizedException("Permission Denied");
		}

		return copyResourceDomainService.copyFromOriginToTargetResource(originResourceEntity, targetResourceEntity, actingOwner);
	}


	public CopyResourceResult doCopyResource(String targetResourceGroupName, String targetReleaseName,
			String originResourceGroupName, String originReleaseName) throws ValidationException, ForeignableOwnerViolationException, AMWException {

        ResourceEntity targetResourceEntity = resourceLocator.getResourceByGroupNameAndRelease(targetResourceGroupName, targetReleaseName);
        if (targetResourceEntity == null) {
            throw new ResourceNotFoundException("Target Resource not found");
        }
        ResourceEntity originResourceEntity = resourceLocator.getResourceByGroupNameAndRelease(originResourceGroupName, originReleaseName);
        if (originResourceEntity == null) {
            throw new ResourceNotFoundException("Origin Resource not found");
        }

		return this.doCopyResource(targetResourceEntity, originResourceEntity, ForeignableOwner.getSystemOwner());
    }

	/**
	 * Creates a new Release of an existing Resource
	 *
	 * @param resourceGroupName name of the existing ResourceGroup
	 * @param targetReleaseName name of the new Release
	 * @param originReleaseName name of the current Release
	 * @param actingOwner the acting ForeignableOwner
	 * @return
	 * @throws ForeignableOwnerViolationException
	 * @throws AMWException
	 */
	public CopyResourceResult doCreateResourceRelease(String resourceGroupName, String targetReleaseName,
		  String originReleaseName, ForeignableOwner actingOwner) throws ForeignableOwnerViolationException, AMWException {
		ReleaseEntity targetRelease;
		ReleaseEntity originRelease;
		ResourceGroupEntity resourceGroup = resourceGroupRepository.getResourceGroupByName(resourceGroupName);
		if (resourceGroup == null) {
			throw new ResourceNotFoundException("No ResourceGroup with name " +resourceGroupName + " found");
		}
		try {
			targetRelease = releaseLocator.getReleaseByName(targetReleaseName);
		} catch (EJBException e) {
			throw new ResourceNotFoundException("Target release '" + targetReleaseName + "' found");
		}
		try {
			originRelease = releaseLocator.getReleaseByName(originReleaseName);
		} catch (EJBException e) {
			throw new ResourceNotFoundException("Origin release '" + originReleaseName + "' found");
		}
		return doCreateResourceRelease(resourceGroup, targetRelease, originRelease, actingOwner);
	}

	public CopyResourceResult doCreateResourceRelease(ResourceGroupEntity targetResourceGroup,
			ReleaseEntity targetRelease, ReleaseEntity originRelease, ForeignableOwner actingOwner) throws AMWException,
			ForeignableOwnerViolationException {
		targetResourceGroup = entityManager.find(ResourceGroupEntity.class, targetResourceGroup.getId());
		// Do not overwrite existing release
		if (targetResourceGroup.getReleases() != null && targetResourceGroup.getReleases().contains(targetRelease)) {
			throw new AMWException("Release " + targetRelease.getName() + " already exists");
		}
		// Can not copy from inexisting release
		if (targetResourceGroup.getReleases() != null && !targetResourceGroup.getReleases().contains(originRelease)) {
			throw new AMWException("Release " + originRelease.getName() + " must exist");
		}

		ResourceEntity originResource = commonDomainService.getResourceEntityByGroupAndRelease(targetResourceGroup.getId(),
					originRelease.getId());
		if(!permissionBoundary.canCopyFromSpecificResource(originResource, targetResourceGroup)){
			throw new NotAuthorizedException("Permission Denied");
		}
		return copyResourceDomainService.createReleaseFromOriginResource(originResource, targetRelease, actingOwner);
	}
}
