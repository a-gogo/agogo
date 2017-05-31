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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;

import ch.puzzle.itc.mobiliar.business.domain.commons.CommonDomainService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceResult;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceGroupPersistenceService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceGroupRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroup;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.security.interceptor.HasPermissionInterceptor;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.NotAuthorizedException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;

/**
 * A boundary for copy resourcess
 * 
 * @author cweber
 */
@Stateless
@Interceptors(HasPermissionInterceptor.class)
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class CopyResource {

    @Inject
    private ResourceGroupRepository resourceGroupRepository;

	@Inject
	private ResourceGroupPersistenceService resourceGroupService;

	@Inject
	private CopyResourceDomainService copyResourceDomainService;

	@Inject
	private EntityManager entityManager;

	@Inject
	private PermissionBoundary permissionBoundary;

	@Inject
	CommonDomainService commonDomainService;

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
            throws ResourceNotFoundException, ForeignableOwnerViolationException, AMWException {
		// Load resources
		ResourceEntity targetResource = commonDomainService.getResourceEntityById(targetResourceId);
		ResourceEntity originResource = commonDomainService.getResourceEntityById(originResourceId);

		if(!permissionBoundary.canCopyFromResource(originResource)){
			throw new NotAuthorizedException("Permission Denied");
		}

		return copyResourceDomainService.copyFromOriginToTargetResource(originResource, targetResource, actingOwner);
	}

	public CopyResourceResult doCreateResourceRelease(ResourceGroupEntity resourceGroup,
			ReleaseEntity targetRelease, ReleaseEntity originRelease, ForeignableOwner actingOwner) throws ResourceNotFoundException,
            AMWException, ForeignableOwnerViolationException {
		resourceGroup = entityManager.find(ResourceGroupEntity.class, resourceGroup.getId());
		// Do not overwrite existing release
		if (resourceGroup.getReleases() != null && resourceGroup.getReleases().contains(targetRelease)) {
			throw new AMWException("Release " + targetRelease.getName() + " already exists");
		}

		ResourceEntity originResource = commonDomainService.getResourceEntityByGroupAndRelease(resourceGroup.getId(),
					originRelease.getId());
		if(!permissionBoundary.canCopyFromResource(originResource)){
			throw new NotAuthorizedException("Permission Denied");
		}
		return copyResourceDomainService.createReleaseFromOriginResource(originResource, targetRelease, actingOwner);
	}
}
