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

package ch.puzzle.itc.mobiliar.business.security.boundary;

import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceGroupRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeProvider;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionRepository;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.security.control.RestrictionRepository;
import ch.puzzle.itc.mobiliar.business.security.entity.*;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;


import java.util.Arrays;

import static ch.puzzle.itc.mobiliar.business.security.entity.Action.*;
import static ch.puzzle.itc.mobiliar.business.security.entity.ResourceTypePermission.*;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PermissionBoundaryTest {

    @InjectMocks
    PermissionBoundary permissionBoundary;
    @Mock
    RestrictionRepository restrictionRepository;
    @Mock
    PermissionRepository permissionRepository;
    @Mock
    ContextLocator contextLocator;
    @Mock
    ResourceTypeRepository resourceTypeRepository;
    @Mock
    ResourceTypeProvider resourceTypeProvider;
    @Mock
    ResourceGroupRepository resourceGroupRepository;
    @Mock
    ResourceRepository resourceRepository;
    @Mock
    PermissionService permissionService;
    @Mock
    EntityManager entityManager;

    private PermissionEntity resourcePermission;
    private PermissionEntity resourceTypePermission;

    @Before
    public void setup() {
        permissionBoundary = new PermissionBoundary();
        restrictionRepository = Mockito.mock(RestrictionRepository.class);
        permissionBoundary.restrictionRepository = restrictionRepository;
        permissionRepository = Mockito.mock(PermissionRepository.class);
        permissionBoundary.permissionRepository = permissionRepository;
        contextLocator = Mockito.mock(ContextLocator.class);
        permissionBoundary.contextLocator = contextLocator;
        resourceTypeRepository = Mockito.mock(ResourceTypeRepository.class);
        permissionBoundary.resourceTypeRepository = resourceTypeRepository;
        resourceTypeProvider = Mockito.mock(ResourceTypeProvider.class);
        permissionBoundary.resourceTypeProvider = resourceTypeProvider;
        resourceGroupRepository = Mockito.mock(ResourceGroupRepository.class);
        permissionBoundary.resourceGroupRepository = resourceGroupRepository;
        resourceRepository = Mockito.mock(ResourceRepository.class);
        permissionBoundary.resourceRepository = resourceRepository;
        permissionService = Mockito.mock(PermissionService.class);
        permissionBoundary.permissionService = permissionService;
        entityManager = Mockito.mock(EntityManager.class);
        permissionBoundary.entityManager = entityManager;
        resourcePermission = new PermissionEntity();
        resourcePermission.setValue("RESOURCE");
        resourceTypePermission = new PermissionEntity();
        resourceTypePermission.setValue("RESOURCETYPE");
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnUpdateIfIdIsNull() throws AMWException {
        // given // when // then
        permissionBoundary.updateRestriction(null,null, null, null, null, null, null, null, null, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnUpdateIfRestrictionCanNotBeFound() throws AMWException {
        // given
        when(restrictionRepository.find(1)).thenReturn(null);
        // when // then
        permissionBoundary.updateRestriction(1, null, null, null, null, null, null, null, null, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnUpdateIfRolenameIsNull() throws AMWException {
        // given
        when(restrictionRepository.find(1)).thenReturn(new RestrictionEntity());
        // when // then
        permissionBoundary.updateRestriction(1, null, null, null, null, null, null, null, null, true);
    }

    @Test
    public void shouldCreateRoleOnUpdateIfRoleCanNotBeFound() throws AMWException {
        // given
        when(restrictionRepository.find(1)).thenReturn(new RestrictionEntity());
        when(permissionRepository.getRoleByName("newRole")).thenReturn(null);
        when(permissionRepository.getPermissionByName("valid")).thenReturn(resourcePermission);
        // when
        permissionBoundary.updateRestriction(1, "newRole", null, "valid", null, null, null, null, null, true);
        // then
        verify(permissionRepository).createRole("newRole");
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnUpdateIfPermissionIsNull() throws AMWException {
        // given
        when(restrictionRepository.find(1)).thenReturn(new RestrictionEntity());
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        // when // then
        permissionBoundary.updateRestriction(1, "existing", null, null, null, null, null, null, null, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnUpdateIfPermissionCanNotBeFound() throws AMWException {
        // given
        when(restrictionRepository.find(1)).thenReturn(new RestrictionEntity());
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("invalid")).thenReturn(null);
        // when // then
        permissionBoundary.updateRestriction(1, "existing", null, "invalid", null, null, null, null, null, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnUpdateIfContextCanNotBeFound() throws AMWException {
        // given
        when(restrictionRepository.find(1)).thenReturn(new RestrictionEntity());
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        when(contextLocator.getContextByName("bad")).thenThrow(new NoResultException());
        // when // then
        permissionBoundary.updateRestriction(1, "existing", null, "good", null, null, null, "bad", null, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnUpdateIfResourceTypeCanNotBeFound() throws AMWException {
        // given
        when(restrictionRepository.find(1)).thenReturn(new RestrictionEntity());
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        when(resourceTypeRepository.getByName("bad")).thenReturn(null);
        // when // then
        permissionBoundary.updateRestriction(1, "existing", null, "good", null, "bad", null, null, null, true);
    }

    @Test
    public void shouldUpdateIfContextAndActionAreNull() throws AMWException {
        // given
        when(restrictionRepository.find(1)).thenReturn(new RestrictionEntity());
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        // when
        permissionBoundary.updateRestriction(1, "existing", null, "good", null, null, null, null, null, true);
        // then
        verify(restrictionRepository).merge(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfRoleNameAndUserNameAreNull() throws AMWException {
        // given // when // then
        permissionBoundary.createRestriction(null, null, null, null, null, null, null, null, false, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfUserNameIsEmpty() throws AMWException {
        // given // when // then
        permissionBoundary.createRestriction(null, "", null, null, null, null, null, null, false, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfTrimmedUserNameIsEmpty() throws AMWException {
        // given // when // then
        permissionBoundary.createRestriction(null, " ", null, null, null, null, null, null, false, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfUserNameHasLeadingSpaces() throws AMWException {
        // given // when // then
        permissionBoundary.createRestriction(null, " invalid", null, null, null, null, null, null, false, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfRoleNameHasTrailingSpaces() throws AMWException {
        // given // when // then
        permissionBoundary.createRestriction("invalid ", null, null, null, null, null, null, null, false, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfRoleNameHasLeadingSpaces() throws AMWException {
        // given // when // then
        permissionBoundary.createRestriction(" invalid", null, null, null, null, null, null, null, false, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfUserNameHasTrailingSpaces() throws AMWException {
        // given // when // then
        permissionBoundary.createRestriction(null, "invalid ", null, null, null, null, null, null, false, true);
    }


    @Test
    public void shouldCreateRoleAndUserRestrictionOnCreateIfRoleCanNotBeFound() throws AMWException {
        // given
        when(permissionRepository.getRoleByName("newRole")).thenReturn(null);
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        // when
        permissionBoundary.createRestriction("newRole", null, "good", null, null, null, null, null, false, true);
        // then
        verify(permissionRepository).createRole("newRole");
        verify(restrictionRepository).create(any(RestrictionEntity.class));
    }

    @Test
    public void shouldCreateUserRestrictionAndRestrictionIfUserNameIsNotNull() throws AMWException {
        // given
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        // when
        permissionBoundary.createRestriction(null, "hans", "good", null, null, null, null, null, false, true);
        // then
        verify(permissionRepository).getUserRestrictionByName("hans");
        verify(permissionRepository).createUserRestriciton("hans");
        verify(restrictionRepository).create(any(RestrictionEntity.class));
    }

    @Test
    public void shouldAssignUserRestrictionAndCreateRestrictionIfUserNameHasBeenFound() throws AMWException {
        // given
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        when(permissionRepository.getUserRestrictionByName("fritz")).thenReturn(new UserRestrictionEntity());
        // when
        permissionBoundary.createRestriction(null, "fritz", "good", null, null, null, null, null, false, true);
        // then
        verify(permissionRepository, never()).createUserRestriciton(anyString());
        verify(restrictionRepository).create(any(RestrictionEntity.class));
    }

    @Test
    public void shouldCreateIfContextIsNull() throws AMWException {
        // given
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        // when
        permissionBoundary.createRestriction("existing", null, "good", null, null, null, null, CREATE, false, true);
        // then
        verify(restrictionRepository).create(any(RestrictionEntity.class));
    }

    @Test
    public void shouldCreateIfContextAndActionAreNull() throws AMWException {
        // given
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        // when
        permissionBoundary.createRestriction("existing", null, "good", null, null, null, null, null, false, true);
        // then
        verify(restrictionRepository).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCheckIfCallerHasSimilarRestrictionIfHeWantsToDelegatePermission() throws AMWException {
        // given
        when(permissionService.hasPermissionToDelegatePermission(Permission.SHAKEDOWNTEST, null, null, null, CREATE)).thenReturn(true);
        when(permissionRepository.getUserRestrictionByName("fed")).thenReturn(new UserRestrictionEntity());
        when(permissionRepository.getPermissionByName(anyString())).thenReturn(resourcePermission);
        // when
        permissionBoundary.createRestriction(null, "fred", "SHAKEDOWNTEST", null, null, null, null, CREATE, true, true);
        // then
        verify(permissionService).hasPermissionToDelegatePermission(Permission.SHAKEDOWNTEST, null, null, null, CREATE);
        verify(restrictionRepository).create(any(RestrictionEntity.class));
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionIfCallerIsNotAllowedToDelegatePermission() throws AMWException {
        // given
        when(permissionService.hasPermissionToDelegatePermission(Permission.SHAKEDOWNTEST, null, null, null, CREATE)).thenReturn(false);
        when(permissionRepository.getUserRestrictionByName("fed")).thenReturn(new UserRestrictionEntity());
        // when
        permissionBoundary.createRestriction(null, "fred", "SHAKEDOWNTEST", null, null, null, null, CREATE, true, true);
        // then
        verify(permissionService).hasPermissionToDelegatePermission(Permission.SHAKEDOWNTEST, null, null, null, CREATE);
        verify(restrictionRepository, never()).create(any(RestrictionEntity.class));
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfResourceIdCanNotBeFound() throws AMWException {
        // given
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        when(resourceGroupRepository.find(7)).thenReturn(null);
        // when // then
        permissionBoundary.createRestriction("existing", null, "good", 7, null, null, null, null, false, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfResourceGroupAndResourceTypeAreSet() throws AMWException {
        // given
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        // when // then
        permissionBoundary.createRestriction("existing", null, "good", 7, "bad", null, null, null, false, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfResourceTypePermissionIsNotEmptyAndResourceGroupIsSet() throws AMWException {
        // given
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        // when // then
        permissionBoundary.createRestriction("existing", null, "good", 7, null, DEFAULT_ONLY, null, null, false, true);
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionOnCreateIfResourceTypePermissionIsNotEmptyAndResourceTypeIsSet() throws AMWException {
        // given
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        // when // then
        permissionBoundary.createRestriction("existing", null, "good", null, "bad", NON_DEFAULT_ONLY, null, null, false, true);
    }

    @Test
    public void shouldPreserveRestrictionPropertiesIfPermissionIsNotOld() throws AMWException {
        // given
        ContextEntity envX = new ContextEntity();
        envX.setName("X");
        RestrictionEntity restriction = new RestrictionEntity();
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(resourcePermission);
        when(contextLocator.getContextByName("X")).thenReturn(envX);
        // when
        permissionBoundary.validateRestriction("existing", null, "good", null, null, ResourceTypePermission.NON_DEFAULT_ONLY, "X", CREATE, restriction);
        // then
        assertThat(restriction.getResourceTypePermission(), is(ResourceTypePermission.NON_DEFAULT_ONLY));
        assertThat(restriction.getAction(), is(CREATE));
        assertThat(restriction.getContext(), is(envX));
        assertNull(restriction.getResourceGroup());
        assertNull(restriction.getResourceType());
    }

    @Test
    public void shouldResetRestrictionPropertiesIfPermissionIsOld() throws AMWException {
        // given
        PermissionEntity globalPerm = new PermissionEntity();
        globalPerm.setValue("APP_TAB");
        RestrictionEntity restriction = new RestrictionEntity();
        when(permissionRepository.getRoleByName("existing")).thenReturn(new RoleEntity());
        when(permissionRepository.getPermissionByName("good")).thenReturn(globalPerm);
        // when
        permissionBoundary.validateRestriction("existing", null, "good", 1, null, ResourceTypePermission.NON_DEFAULT_ONLY, "X", CREATE, restriction);
        // then
        assertThat(restriction.getResourceTypePermission(), is(ResourceTypePermission.ANY));
        assertThat(restriction.getAction(), is(ALL));
        assertNull(restriction.getContext());
        assertNull(restriction.getResourceGroup());
        assertNull(restriction.getResourceType());
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAMWExceptionIfRestrictionToBeDeletedCanNotBeFound() throws AMWException {
        // given // when // then
        permissionBoundary.removeRestriction(21, true);
    }

    @Test
    public void shouldRemoveRestrictionToBeDeleted() throws AMWException {
        // given
        when(restrictionRepository.find(42)).thenReturn(new RestrictionEntity());
        // when
        permissionBoundary.removeRestriction(42, true);
        // then
        verify(restrictionRepository).deleteRestrictionById(42);
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldInvokePermissionServiceIfPermissionHasBeenFound() {
        // given
        when(permissionService.hasPermission(Permission.RESOURCE)).thenReturn(true);
        // when
        boolean result = permissionBoundary.hasPermission("RESOURCE");
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE);
        assertTrue(result);
    }

    @Test
    public void shouldDelegatePermissionCheckToPermissionService() {
        // given
        when(permissionService.hasPermission(Permission.RESOURCE)).thenReturn(true);
        // when
        boolean result = permissionBoundary.hasPermission(Permission.RESOURCE);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE);
        assertTrue(result);
    }

    @Test
    public void shouldInvokePermissionServiceIfPermissionAndActionHaveBeenFound() {
        // given
        when(permissionService.hasPermission(Permission.RESOURCE, READ)).thenReturn(true);
        // when
        boolean result = permissionBoundary.hasPermission("RESOURCE", "READ");
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE, READ);
        assertTrue(result);
    }

    @Test
    public void shouldDelegatePermissionAndActionCheckToPermissionService() {
        // given
        when(permissionService.hasPermission(Permission.RESOURCE, READ)).thenReturn(true);
        // when
        boolean result = permissionBoundary.hasPermission(Permission.RESOURCE, READ);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE, READ);
        assertTrue(result);
    }

    @Test(expected=IllegalArgumentException.class)
    public void shouldThrowAnExceptionIfRequestedPermissionDoesNotExist() {
        // given // when // then
        permissionBoundary.hasPermissionForResourceType("NotAPermission", "CREATE", "APP");
    }

    @Test(expected=IllegalArgumentException.class)
    public void shouldThrowAnExceptionIfRequestedActionDoesNotExist() {
        // given // when // then
        permissionBoundary.hasPermissionForResourceType("RESOURCE", "NADA", "APP");
    }

    @Test
    public void shouldReturnFalseIfRequestedResourceTypeDoesNotExist() {
        // given // when
        boolean result = permissionBoundary.hasPermissionForResourceType("RESOURCE", "CREATE", "DAB");
        // then
        assertFalse(result);
    }

    @Test
    public void shouldInvokeResourceTypeRepository() {
        // given // when
        permissionBoundary.hasPermissionForResourceType("RESOURCE", "CREATE", "APP");
        // then
        verify(resourceTypeRepository).getByName("APP");
    }

    @Test
    public void shouldInvokePermissionServiceIfResourceTypeHasBeenFound() {
        // given
        ResourceTypeEntity resType = new ResourceTypeEntity();
        when(resourceTypeRepository.getByName("APP")).thenReturn(resType);
        when(permissionService.hasPermission(Permission.RESOURCE, CREATE, resType)).thenReturn(true);
        // when
        boolean result = permissionBoundary.hasPermissionForResourceType("RESOURCE", "CREATE", "APP");
        // then
        verify(resourceTypeRepository).getByName("APP");
        verify(permissionService).hasPermission(Permission.RESOURCE, CREATE, resType);
        assertTrue(result);
    }

    @Test
    public void shouldInvokePermissionServiceIfResourceTypeAndContextHaveBeenFound() {
        // given
        ResourceTypeEntity resType = new ResourceTypeEntity();
        ContextEntity context = new ContextEntity();
        when(resourceTypeRepository.getByName("APP")).thenReturn(resType);
        when(contextLocator.getContextById(1)).thenReturn(context);
        when(permissionService.hasPermission(Permission.RESOURCE, context, CREATE, null, resType)).thenReturn(true);
        // when
        boolean result = permissionBoundary.hasPermissionForResourceType("RESOURCE", "CREATE", "APP", 1);
        // then
        verify(resourceTypeRepository).getByName("APP");
        verify(permissionService).hasPermission(Permission.RESOURCE, context, CREATE, null, resType);
        assertTrue(result);
    }

    @Test
    public void shouldInvokePermissionServiceWithAllParams() {
        // given
        ResourceEntity resource = new ResourceEntityBuilder().build();
        ResourceGroupEntity rg = new ResourceGroupEntity();
        resource.setResourceGroup(rg);
        ResourceTypeEntity type = new ResourceTypeEntity();
        resource.setResourceType(type);
        ContextEntity context = new ContextEntity();
        when(permissionService.hasPermission(Permission.RESOURCE, context, CREATE, rg, type)).thenReturn(true);
        // when
        boolean result = permissionBoundary.hasPermission(Permission.RESOURCE, context, CREATE, resource, type);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE, context, CREATE, rg, type);
        assertTrue(result);
    }

    @Test
    public void shouldInvokeTheRightMethodOnPermissionServiceToCheckIfHasPermissionToRemoveInstanceOfResType() {
        // given
        ResourceTypeEntity resType = new ResourceTypeEntity();
        // when
        permissionBoundary.hasPermissionToRemoveInstanceOfResType(resType);
        // then
        verify(permissionService).hasPermissionToRemoveInstanceOfResType(resType);
    }

    @Test
    public void shouldInvokePermissionServiceWithCorrectParametersOnCanCopyFromResource() {
        // given
        ResourceEntity resource = new ResourceEntityBuilder().build();
        ResourceGroupEntity rg = new ResourceGroupEntity();
        resource.setResourceGroup(rg);
        ResourceTypeEntity type = new ResourceTypeEntity();
        resource.setResourceType(type);
        when(permissionService.hasPermission(Permission.RESOURCE_RELEASE_COPY_FROM_RESOURCE, null, ALL, rg, type)).thenReturn(true);
        // when
        permissionBoundary.canCopyFromResource(resource);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE_RELEASE_COPY_FROM_RESOURCE, null, ALL, rg, type);
    }

    @Test
    public void shouldInvokePermissionServiceWithCorrectParametersOnCanCopyFromSpecificResourceFailureOnRead() {
        // given
        ResourceEntity resource = new ResourceEntityBuilder().build();
        ResourceGroupEntity rg = new ResourceGroupEntity();
        resource.setResourceGroup(rg);
        ResourceTypeEntity type = new ResourceTypeEntity();
        resource.setResourceType(type);
        ResourceEntity originResource = new ResourceEntityBuilder().build();
        ResourceGroupEntity org = new ResourceGroupEntity();
        originResource.setResourceGroup(org);
        originResource.setResourceType(type);
        when(permissionService.hasPermission(Permission.RESOURCE_RELEASE_COPY_FROM_RESOURCE, null, ALL, rg, type)).thenReturn(true);
        when(permissionService.hasPermission(Permission.RESOURCE, null, READ, org, type)).thenReturn(false);
        // when
        boolean can = permissionBoundary.canCopyFromSpecificResource(resource, org);
        verify(permissionService).hasPermission(Permission.RESOURCE_RELEASE_COPY_FROM_RESOURCE, null, ALL, rg, type);
        verify(permissionService).hasPermission(Permission.RESOURCE, null, READ, org, type);
        assertFalse(can);
    }

    @Test
    public void shouldInvokePermissionServiceWithCorrectParametersOnCanCopyFromSpecificResourceFailureOnCopyFrom() {
        // given
        ResourceEntity resource = new ResourceEntityBuilder().build();
        ResourceGroupEntity rg = new ResourceGroupEntity();
        resource.setResourceGroup(rg);
        ResourceTypeEntity type = new ResourceTypeEntity();
        resource.setResourceType(type);
        ResourceEntity originResource = new ResourceEntityBuilder().build();
        ResourceGroupEntity org = new ResourceGroupEntity();
        originResource.setResourceGroup(org);
        originResource.setResourceType(type);
        when(permissionService.hasPermission(Permission.RESOURCE_RELEASE_COPY_FROM_RESOURCE, null, ALL, rg, type)).thenReturn(false);
        when(permissionService.hasPermission(Permission.RESOURCE, null, READ, org, type)).thenReturn(true);
        // when
        boolean can = permissionBoundary.canCopyFromSpecificResource(resource, org);
        verify(permissionService).hasPermission(Permission.RESOURCE_RELEASE_COPY_FROM_RESOURCE, null, ALL, rg, type);
        verify(permissionService, never()).hasPermission(Permission.RESOURCE, null, READ, org, type);
        assertFalse(can);
    }

    @Test
    public void shouldInvokePermissionServiceWithCorrectParametersOnCanCopyFromSpecificResourceSuccess() {
        // given
        ResourceTypeEntity resType = new ResourceTypeEntity();
        ResourceEntity targetResource = new ResourceEntityBuilder().build();
        ResourceGroupEntity targetGroup = new ResourceGroupEntity();
        targetResource.setResourceGroup(targetGroup);
        targetResource.setResourceType(resType);

        ResourceEntity originResource = new ResourceEntityBuilder().build();
        ResourceGroupEntity originGroup = new ResourceGroupEntity();
        originResource.setResourceGroup(originGroup);
        originResource.setResourceType(resType);
        when(permissionService.hasPermission(Permission.RESOURCE_RELEASE_COPY_FROM_RESOURCE, null, ALL, targetGroup, resType)).thenReturn(true);
        when(permissionService.hasPermission(Permission.RESOURCE, null, READ, originGroup, resType)).thenReturn(true);
        when(permissionService.hasPermission(Permission.RESOURCE_TEMPLATE, null, Action.READ, originGroup, resType)).thenReturn(true);
        when(permissionService.hasPermission(Permission.RESOURCE_AMWFUNCTION, null, Action.READ, originGroup, resType)).thenReturn(true);
        when(permissionService.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, null, Action.ALL, originGroup, resType)).thenReturn(true);

        // when
        boolean can = permissionBoundary.canCopyFromSpecificResource(targetResource, originGroup);

        assertTrue(can);
    }

    @Test
    public void shouldInvokeTheRightMethodOnPermissionServiceToCheckIfCanCreateResourceInstance() {
        // given
        ResourceTypeEntity resType = Mockito.mock(ResourceTypeEntity.class);
        // when
        permissionBoundary.canCreateResourceInstance(resType);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE, CREATE, resType);
    }

    @Test
    public void shouldInvokeTheRightMethodsOnPermissionServiceToCheckIfCanCreateAppAndAddToAppServer() {
        // given
        ResourceEntity resource = new ResourceEntityBuilder().build();
        ResourceTypeEntity type = new ResourceTypeEntity();
        ResourceTypeEntity asType = new ResourceTypeEntity();
        asType.setName("APPLICATIONSERVER");
        resource.setResourceType(type);
        when(permissionService.hasPermission(Permission.RESOURCE, CREATE, type)).thenReturn(true);
        when(resourceTypeProvider.getOrCreateDefaultResourceType(DefaultResourceTypeDefinition.APPLICATIONSERVER)).thenReturn(asType);
        // when
        permissionBoundary.canCreateAppAndAddToAppServer(resource);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE, CREATE, type);
        verify(permissionService).hasPermission(Permission.RESOURCE, UPDATE, asType);
    }

    @Test
    public void shouldObtainListOfPermissionsFromPermissionService() {
        // given // when
        permissionBoundary.getAllPermissions();
        // then
        verify(permissionService).getPermissions();
    }

    @Test
    public void shouldInvokePermissionServiceMethodsWithCorrectParametersForResourceType() {
        // given
        ResourceTypeEntity type = new ResourceTypeEntity();
        ContextEntity context = new ContextEntity();
        doReturn(type).when(entityManager).find(type.getClass(), 21);
        when(contextLocator.getContextById(23)).thenReturn(context);
        when(permissionService.hasPermission(Permission.RESOURCETYPE, context, UPDATE, null, type)).thenReturn(true);
        // when
        permissionBoundary.hasPermissionToEditPropertiesByResourceTypeAndContext(21, 23, false);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCETYPE, context, UPDATE, null, type);
        verify(permissionService, never()).hasPermission(Permission.SHAKEDOWN_TEST_MODE);
    }

    @Test
    public void shouldInvokePermissionServiceMethodWithCorrectParametersForResource() {
        // given
        ResourceEntity resource = new ResourceEntityBuilder().build();
        ResourceGroupEntity rg = new ResourceGroupEntity();
        ResourceTypeEntity type = new ResourceTypeEntity();
        resource.setResourceGroup(rg);
        resource.setResourceType(type);
        ContextEntity context = new ContextEntity();
        doReturn(resource).when(entityManager).find(resource.getClass(), 21);
        when(contextLocator.getContextById(23)).thenReturn(context);
        when(permissionService.hasPermission(Permission.RESOURCE, context, UPDATE, rg, null)).thenReturn(false);
        // when
        permissionBoundary.hasPermissionToEditPropertiesByResourceAndContext(21, context, false);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE, context, UPDATE, rg, null);
        verify(permissionService, never()).hasPermission(Permission.SHAKEDOWN_TEST_MODE);
    }

    @Test
    public void shouldInvokePermissionServiceMethodsWithCorrectParametersIfInTestingMode() {
        // given // when
        permissionBoundary.hasPermissionToEditPropertiesByResourceTypeAndContext(21, 23, true);
        // then
        verify(permissionService, never()).hasPermission(any(Permission.class), any(ContextEntity.class),
                any(Action.class), any(ResourceGroupEntity.class), any(ResourceTypeEntity.class));
        verify(permissionService).hasPermission(Permission.SHAKEDOWN_TEST_MODE);
    }

    @Test
    public void shouldDelegateCheckPermissionAndFireExceptionToPermissionServiceWithPermissionAndMessage() {
        // given // when
        permissionBoundary.checkPermissionAndFireException(Permission.RESOURCE, "Message");
        // then
        verify(permissionService).checkPermissionAndFireException(Permission.RESOURCE, "Message");
    }

    @Test
    public void shouldDelegateCheckPermissionAndFireExceptionToPermissionServiceWithPermissionAndActionAndMessage() {
        // given // when
        permissionBoundary.checkPermissionAndFireException(Permission.RESOURCE, Action.READ, "Message");
        // then
        verify(permissionService).checkPermissionAndFireException(Permission.RESOURCE, Action.READ, "Message");
    }

    @Test
    public void shouldDelegateHasPermissionToAddRelationToPermissionService() {
        // given
        ResourceEntity resource = new ResourceEntity();
        resource.setId(7);
        ContextEntity aContext = new ContextEntity();
        when(entityManager.find(ResourceEntity.class, 7)).thenReturn(resource);
        // when
        permissionBoundary.hasPermissionToAddRelation(resource, aContext);
        // then
        verify(permissionService).hasPermissionToAddRelation(resource, aContext);
    }

    @Test
    public void shouldInvokeTheRightMethodOfPermissionServiceToAskForResourceTemplateModifyPermission() {
        // given
        ResourceEntity resource = new ResourceEntity();
        resource.setId(12);
        when(entityManager.find(ResourceEntity.class, 12)).thenReturn(resource);
        // when
        permissionBoundary.hasPermissionToAddTemplate(resource, false);
        // then
        verify(permissionService, never()).hasPermissionToAddResourceTypeTemplate((ResourceTypeEntity) anyObject(), anyBoolean());
        verify(permissionService).hasPermissionToAddResourceTemplate(resource, false);
    }

    @Test
    public void shouldInvokeTheRightMethodOfPermissionServiceToAskForResourceTypeTemplateModifyPermission() {
        // given
        ResourceTypeEntity type = new ResourceTypeEntity();
        type.setId(23);
        when(entityManager.find(ResourceTypeEntity.class, 23)).thenReturn(type);
        // when
        permissionBoundary.hasPermissionToAddTemplate(type, true);
        // then
        verify(permissionService, never()).hasPermissionToAddResourceTemplate((ResourceEntity) anyObject(), anyBoolean());
        verify(permissionService).hasPermissionToAddResourceTypeTemplate(type, true);
    }

    @Test
    public void shouldInvokePermissionServiceWithRightParametersToAskForResourceTypeFunctionEditPermissionAndSkipFurtherInvocationsIfFirstOneFails() {
        // given
        ResourceTypeEntity type = new ResourceTypeEntity();
        type.setId(23);
        when(permissionBoundary.resourceTypeRepository.find(23)).thenReturn(type);
        // when
        permissionBoundary.canUpdateFunctionOfResourceOrResourceType(null, type.getId());
        // then
        verify(permissionService).hasPermission(Permission.RESOURCETYPE_AMWFUNCTION, null, UPDATE, null, type);
    }

    @Test
    public void shouldInvokePermissionServiceWithRightParametersToAskForResourceTypeFunctionEditPermission() {
        // given
        ResourceTypeEntity type = new ResourceTypeEntity();
        type.setId(23);
        when(permissionBoundary.resourceTypeRepository.find(23)).thenReturn(type);
        // when
        permissionBoundary.canUpdateFunctionOfResourceOrResourceType(null, type.getId());
        // then
        verify(permissionService).hasPermission(Permission.RESOURCETYPE_AMWFUNCTION, null, UPDATE, null, type);
    }

    @Test
    public void shouldInvokePermissionServiceWithRightParametersToAskForResourceFunctionEditPermissionAndSkipFurtherInvocationsIfFirstOneFails() {
        // given
        ResourceGroupEntity rg = new ResourceGroupEntity();
        ResourceEntity resource = new ResourceEntity();
        resource.setId(12);
        resource.setResourceGroup(rg);
        when(permissionBoundary.resourceRepository.find(12)).thenReturn(resource);
        // when
        permissionBoundary.canUpdateFunctionOfResourceOrResourceType(resource.getId(), null);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE_AMWFUNCTION, null, UPDATE, rg, null);
    }

    @Test
    public void shouldInvokePermissionServiceWithRightParametersToAskForResourceFunctionEditPermission() {
        // given
        ResourceGroupEntity rg = new ResourceGroupEntity();
        ResourceEntity resource = new ResourceEntity();
        resource.setId(12);
        resource.setResourceGroup(rg);
        when(permissionBoundary.resourceRepository.find(12)).thenReturn(resource);
        // when
        permissionBoundary.canUpdateFunctionOfResourceOrResourceType(resource.getId(), null);
        // then
        verify(permissionService).hasPermission(Permission.RESOURCE_AMWFUNCTION, null, UPDATE, rg, null);
    }

    @Test
    public void shouldDelegateGetAllUserRestrictionToPermissionService() {
        // given // when
        permissionBoundary.getAllUserRestriction();
        // then
        verify(permissionService).getAllUserRestrictions();
    }

    @Test
    public void shouldNotCreateSelfAssignedPermissionsIfCallerHasNotTheRequiredPermission() throws AMWException {
        // given
        ReleaseEntity aRelease = new ReleaseEntity();
        aRelease.setName("release");
        ResourceEntity resource = new ResourceEntityBuilder().buildResourceEntity("TestResource", null, "aType", aRelease, true);
        resource.getResourceGroup().setId(7);
        when(permissionService.getCurrentUserName()).thenReturn("tester");
        when(permissionService.hasPermission(Permission.ADD_ADMIN_PERMISSIONS_ON_CREATED_RESOURCE)).thenReturn(false);

        // when
        permissionBoundary.createAutoAssignedRestrictions(resource);

        // then
        verify(permissionService).hasPermission(Permission.ADD_ADMIN_PERMISSIONS_ON_CREATED_RESOURCE);
        verify(restrictionRepository, never()).create(any(RestrictionEntity.class));
    }

    @Test
    public void shouldCreateAllSelfAssignedPermissionsIfCallerHasTheRequiredPermission() throws Exception {
        // given
        ReleaseEntity aRelease = new ReleaseEntity();
        aRelease.setName("release");
        ResourceEntity resource = new ResourceEntityBuilder().buildResourceEntity("TestResource", null, "aType", aRelease, true);
        resource.getResourceGroup().setId(7);
        when(permissionService.getCurrentUserName()).thenReturn("tester");
        when(permissionService.hasPermission(Permission.ADD_ADMIN_PERMISSIONS_ON_CREATED_RESOURCE)).thenReturn(true);
        when(resourceGroupRepository.find(resource.getResourceGroup().getId())).thenReturn(resource.getResourceGroup());
        when(permissionRepository.getPermissionByName(anyString())).thenReturn(resourcePermission);

        // when
        permissionBoundary.createAutoAssignedRestrictions(resource);

        // then
        verify(permissionService).hasPermission(Permission.ADD_ADMIN_PERMISSIONS_ON_CREATED_RESOURCE);
        verify(restrictionRepository, times(5)).create(any(RestrictionEntity.class));
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfARoleIsGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String permissionName1 = "RESOURCE";
        String permissionName2 = "RESOURCETYPE";
        String contextNameA = "A";
        when(permissionRepository.getPermissionByName("RESOURCE")).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName("RESOURCETYPE")).thenReturn(resourceTypePermission);
        when(resourceGroupRepository.find(1)).thenReturn(new ResourceGroupEntity());

        // when
        int total = permissionBoundary.createMultipleRestrictions(roleName1, null, Arrays.asList(permissionName1, permissionName2), Arrays.asList(1), null, ResourceTypePermission.ANY, Arrays.asList(contextNameA), Arrays.asList(Action.CREATE), false, true);

        // then
        assertThat(total, is(2));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfUsersAreGiven() throws Exception {
        //given
        String userName1 = "User1";
        String userName2 = "User2";
        String permissionName1 = "RESOURCE";
        String permissionName2 = "RESOURCETYPE";
        when(permissionRepository.getPermissionByName("RESOURCE")).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName("RESOURCETYPE")).thenReturn(resourceTypePermission);
        when(resourceGroupRepository.find(1)).thenReturn(new ResourceGroupEntity());

        // when
        int total = permissionBoundary.createMultipleRestrictions(null, Arrays.asList(userName1, userName2), Arrays.asList(permissionName1, permissionName2), Arrays.asList(1), null, ResourceTypePermission.ANY, null, Arrays.asList(Action.CREATE), false, true);

        // then
        assertThat(total, is(4));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfUsersAndRoleAreGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String userName1 = "User1";
        String userName2 = "User2";
        String permissionName1 = "RESOURCE";
        String permissionName2 = "RESOURCETYPE";
        String contextNameA = "A";
        when(permissionRepository.getPermissionByName("RESOURCE")).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName("RESOURCETYPE")).thenReturn(resourceTypePermission);
        when(resourceGroupRepository.find(1)).thenReturn(new ResourceGroupEntity());

        // when
        int total = permissionBoundary.createMultipleRestrictions(roleName1, Arrays.asList(userName1, userName2), Arrays.asList(permissionName1, permissionName2), Arrays.asList(1), null, ResourceTypePermission.ANY, Arrays.asList(contextNameA), Arrays.asList(Action.CREATE), false, true);

        // then
        assertThat(total, is(6));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfUsersAndRoleAndTwoActionsAreGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String userName1 = "User1";
        String userName2 = "User2";
        when(permissionRepository.getPermissionByName(resourcePermission.getValue())).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName(resourceTypePermission.getValue())).thenReturn(resourceTypePermission);
        when(resourceGroupRepository.find(1)).thenReturn(new ResourceGroupEntity());

        // when
        int total = permissionBoundary.createMultipleRestrictions(roleName1, Arrays.asList(userName1, userName2), Arrays.asList(resourcePermission.getValue(), resourceTypePermission.getValue()), Arrays.asList(1), null, ResourceTypePermission.ANY, null, Arrays.asList(Action.CREATE, Action.UPDATE), false, true);

        // then
        assertThat(total, is(12));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfUsersAndRoleAndTwoActionsAndTwoContextsAreGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String userName1 = "User1";
        String userName2 = "User2";
        String contextNameA = "A";
        String contextNameB = "B";
        when(permissionRepository.getPermissionByName(resourcePermission.getValue())).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName(resourceTypePermission.getValue())).thenReturn(resourceTypePermission);
        when(resourceGroupRepository.find(1)).thenReturn(new ResourceGroupEntity());

        // when
        int total = permissionBoundary.createMultipleRestrictions(roleName1, Arrays.asList(userName1, userName2), Arrays.asList(resourcePermission.getValue(), resourceTypePermission.getValue()), Arrays.asList(1), null, ResourceTypePermission.ANY, Arrays.asList(contextNameA, contextNameB), Arrays.asList(Action.CREATE, Action.UPDATE), false, true);

        // then
        assertThat(total, is(24));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfUsersAndRoleAndTwoActionsAndTwoContextsAndTwoResourceGroupsAreGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String userName1 = "User1";
        String userName2 = "User2";
        String contextNameA = "A";
        String contextNameB = "B";
        when(permissionRepository.getPermissionByName(resourcePermission.getValue())).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName(resourceTypePermission.getValue())).thenReturn(resourceTypePermission);
        when(resourceGroupRepository.find(anyInt())).thenReturn(new ResourceGroupEntity());

        // when
        int total = permissionBoundary.createMultipleRestrictions(roleName1, Arrays.asList(userName1, userName2), Arrays.asList(resourcePermission.getValue(), resourceTypePermission.getValue()), Arrays.asList(1, 2), null, ResourceTypePermission.ANY, Arrays.asList(contextNameA, contextNameB), Arrays.asList(Action.CREATE, Action.UPDATE), false, true);

        // then
        assertThat(total, is(48));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfUsersAndRoleAndTwoActionsAndTwoContextsAndTwoResourceTypesAreGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String userName1 = "User1";
        String userName2 = "User2";
        String resourceTypeName1 = "APPLICATION";
        String resourceTypeName2 = "APPSERVER";
        String contextNameA = "A";
        String contextNameB = "B";
        when(permissionRepository.getPermissionByName(resourcePermission.getValue())).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName(resourceTypePermission.getValue())).thenReturn(resourceTypePermission);
        when(resourceTypeRepository.getByName(anyString())).thenReturn(new ResourceTypeEntity());

        // when
        int total = permissionBoundary.createMultipleRestrictions(roleName1, Arrays.asList(userName1, userName2), Arrays.asList(resourcePermission.getValue(), resourceTypePermission.getValue()), null, Arrays.asList(resourceTypeName1, resourceTypeName2), ResourceTypePermission.ANY, Arrays.asList(contextNameA, contextNameB), Arrays.asList(Action.CREATE, Action.UPDATE), false, true);

        // then
        assertThat(total, is(48));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfUsersAndRoleAndTwoActionsAndTwoResourceTypesAreGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String userName1 = "User1";
        String userName2 = "User2";
        String resourceTypeName1 = "APPLICATION";
        String resourceTypeName2 = "APPSERVER";
        when(permissionRepository.getPermissionByName(resourcePermission.getValue())).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName(resourceTypePermission.getValue())).thenReturn(resourceTypePermission);
        when(resourceTypeRepository.getByName(anyString())).thenReturn(new ResourceTypeEntity());

        // when
        int total = permissionBoundary.createMultipleRestrictions(roleName1, Arrays.asList(userName1, userName2), Arrays.asList(resourcePermission.getValue(), resourceTypePermission.getValue()), null, Arrays.asList(resourceTypeName1, resourceTypeName2), ResourceTypePermission.ANY, null, Arrays.asList(Action.CREATE, Action.UPDATE), false, true);

        // then
        assertThat(total, is(24));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfUsersAndRoleAndTwoActionsAndTwoContextsAreButNoResourceGroupsOrResourceTypesAreGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String userName1 = "User1";
        String userName2 = "User2";
        String contextNameA = "A";
        String contextNameB = "B";
        when(permissionRepository.getPermissionByName(resourcePermission.getValue())).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName(resourceTypePermission.getValue())).thenReturn(resourceTypePermission);

        // when
        int total = permissionBoundary.createMultipleRestrictions(roleName1, Arrays.asList(userName1, userName2), Arrays.asList(resourcePermission.getValue(), resourceTypePermission.getValue()), null, null, ResourceTypePermission.ANY, Arrays.asList(contextNameA, contextNameB), Arrays.asList(Action.CREATE, Action.UPDATE), false, true);

        // then
        assertThat(total, is(24));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService).reloadCache();
    }

    @Test
    public void shouldCreateTheRightAmountOfRestrictionsIfUsersAndRoleAndTwoActionsAreButNoResourceGroupsOrResourceTypesAreGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String userName1 = "User1";
        String userName2 = "User2";
        when(permissionRepository.getPermissionByName(resourcePermission.getValue())).thenReturn(resourcePermission);
        when(permissionRepository.getPermissionByName(resourceTypePermission.getValue())).thenReturn(resourceTypePermission);

        // when
        int total = permissionBoundary.createMultipleRestrictions(roleName1, Arrays.asList(userName1, userName2), Arrays.asList(resourcePermission.getValue(), resourceTypePermission.getValue()), null, null, ResourceTypePermission.ANY, null, Arrays.asList(Action.CREATE, Action.UPDATE), false, false);

        // then
        assertThat(total, is(12));
        verify(restrictionRepository, times(total)).create(any(RestrictionEntity.class));
        verify(permissionService, times(0)).reloadCache();
    }

    @Test(expected=AMWException.class)
    public void shouldThrowAnExceptionIfBothResourceTypeAndResourceGroupAreGiven() throws Exception {
        //given
        String roleName1 = "Role1";
        String resourceTypeName1 = "APPLICATION";
        when(permissionRepository.getPermissionByName(resourcePermission.getValue())).thenReturn(resourcePermission);
        when(resourceTypeRepository.getByName(anyString())).thenReturn(new ResourceTypeEntity());

        // when // then
        permissionBoundary.createMultipleRestrictions(roleName1, null, Arrays.asList(resourcePermission.getValue()), Arrays.asList(1), Arrays.asList(resourceTypeName1), ResourceTypePermission.ANY, null, Arrays.asList(Action.CREATE, Action.UPDATE), false, true);
        verify(permissionService, times(0)).reloadCache();
    }

}
