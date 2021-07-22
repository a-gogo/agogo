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

package ch.mobi.itc.mobiliar.rest.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ch.mobi.itc.mobiliar.rest.dtos.*;
import ch.mobi.itc.mobiliar.rest.exceptions.ExceptionDto;
import ch.puzzle.itc.mobiliar.business.deploy.boundary.DeploymentBoundary;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.releasing.boundary.ReleaseLocator;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.CopyResource;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceBoundary;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceResult;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.Resource;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.NotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceTypeNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceGroupLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.server.boundary.ServerView;
import ch.puzzle.itc.mobiliar.business.server.entity.ServerTuple;

import javax.ws.rs.core.Response;

import static ch.puzzle.itc.mobiliar.common.util.ApplicationServerContainer.APPSERVERCONTAINER;
import static javax.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ResourcesRestTest {

    @InjectMocks
    ResourcesRest rest;

    @Mock
    ResourceGroupLocator resourceGroupLocatorMock;

    @Mock
    ResourceLocator resourceLocatorMock;

    @Mock
    ResourceBoundary resourceBoundaryMock;

    @Mock
    CopyResource copyResourceMock;

    @Mock
    ServerView serverViewMock;

    @Mock
    ResourceRelationsRest resourceRelationsMock;

    @Mock
    ResourcePropertiesRest resourcePropertiesMock;

    @Mock
    ResourceTemplatesRest resourceTemplatesRestMock;

    @Mock
    ReleaseLocator releaseLocatorMock;

    @Mock
    DeploymentBoundary deploymentBoundaryMock;

    @Mock
    ResourceDependencyResolverService resourceDependencyResolverServiceMock;

    @Before
    public void configure() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getResourcesWhenTypeIsNullAndNoRessourceGroupsShouldReturnEmptyResult() {
        // given
        String typeName = null;
        List<ResourceGroupEntity> resourceGroupEntities = new ArrayList<>();

        when(resourceGroupLocatorMock.getResourceGroups()).thenReturn(resourceGroupEntities);

        // when
        List<ResourceGroupDTO> resourcesResult = rest.getResources(typeName);

        // then
        assertTrue(resourcesResult.isEmpty());
    }

    @Test
    public void getResourcesWhenTypeIsNullShouldReturnAllGroups() {
        // given
        String typeName = null;
        List<ResourceGroupEntity> resourceGroupEntities = new ArrayList<>();
        resourceGroupEntities.add(createResourceGroupEntity("name", "type"));
        resourceGroupEntities.add(createResourceGroupEntity("name2", "type2"));

        when(resourceGroupLocatorMock.getResourceGroups()).thenReturn(resourceGroupEntities);

        // when
        List<ResourceGroupDTO> resourcesResult = rest.getResources(typeName);

        // then
        Assert.assertFalse(resourcesResult.isEmpty());
        Assert.assertEquals(resourceGroupEntities.size(), resourcesResult.size());
    }

    @Test
    public void getResourcesWhenTypeIsNullAndFoundResourceGroupTypeIsNullShouldReturnTheGroup() {
        // given
        String typeName = null;
        List<ResourceGroupEntity> resourceGroupEntities = new ArrayList<>();
        String groupName = "name";
        resourceGroupEntities.add(createResourceGroupEntity(groupName, null));

        when(resourceGroupLocatorMock.getResourceGroups()).thenReturn(resourceGroupEntities);

        // when
        List<ResourceGroupDTO> resourcesResult = rest.getResources(typeName);

        // then
        Assert.assertFalse(resourcesResult.isEmpty());
        Assert.assertEquals(resourceGroupEntities.get(0).getName(), resourcesResult.get(0).getName());
        Assert.assertEquals(groupName, resourcesResult.get(0).getName());
        Assert.assertNull(resourcesResult.get(0).getType());
    }

    @Test
    public void getResourcesForTypeWhereTypeDoesNotExistsShouldReturnEmptyResult() {
        // given
        String typeName = "type";

        when(resourceGroupLocatorMock.getGroupsForType(typeName, Collections.EMPTY_LIST, true, true)).thenReturn(new ArrayList<ResourceGroupEntity>());

        // when
        List<ResourceGroupDTO> resourcesResult = rest.getResources(typeName);

        // then
        assertTrue(resourcesResult.isEmpty());
    }

    private ResourceGroupEntity createResourceGroupEntity(String name, String type) {
        ResourceGroupEntity resourceGroupEntity = new ResourceGroupEntity();
        resourceGroupEntity.setName(name);
        if (type != null) {
            ResourceTypeEntity resourceType = new ResourceTypeEntity();
            resourceType.setName(type);
            resourceGroupEntity.setResourceType(resourceType);
        }
        return resourceGroupEntity;
    }

    @Test
    public void getBatchJobResources() throws ValidationException {
        // given
        BatchResourceDTO d = Mockito.mock(BatchResourceDTO.class);
        List<BatchResourceDTO> list = new ArrayList<>();
        list.add(d);
        String app = "app";

        // when
        List<BatchResourceDTO> result = rest.getBatchJobResources(app);

        // then
        assertTrue(result != null && result.size() == 0);

    }

    @Test
    public void getBatchJobInventoryEmpty() {
        // given
        String env = "V";
        Integer type = 2305;
        List<ServerTuple> list = new ArrayList<>();
        when(serverViewMock.getServers(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(list);

        // when
        BatchJobInventoryDTO result = rest.getBatchJobInventar(env, type, null, null, null, null, null);

        // then
        assertTrue(result != null);

    }

    @Test
    public void shouldNotAllowCreationOfNewResourcesWithoutRelease() {
        // given
        ResourceReleaseDTO resourceReleaseDTO = new ResourceReleaseDTO();
        resourceReleaseDTO.setName("Test");

        // when
        Response response = rest.addResource(resourceReleaseDTO);

        // then
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    @Test
    public void shouldReturnExpectedLocationHeaderOnSuccessfullResourceCreation() throws ResourceTypeNotFoundException, ResourceNotFoundException, ElementAlreadyExistsException {
        // given
        ResourceTypeEntity resType = new ResourceTypeEntity();
        resType.setName("APP");
        ResourceGroupEntity resGroup = new ResourceGroupEntity();
        resGroup.setName("Test");
        resGroup.setResourceType(resType);
        ReleaseEntity release = new ReleaseEntity();
        release.setName("TestRelease");
        release.setId(1);

        ResourceReleaseDTO resourceReleaseDTO = new ResourceReleaseDTO();
        resourceReleaseDTO.setName(resGroup.getName());
        resourceReleaseDTO.setType(resType.getName());
        resourceReleaseDTO.setReleaseName(release.getName());

        ResourceEntity  resEnt = new ResourceEntity();
        resEnt.setResourceGroup(resGroup);
        resEnt.setName(resGroup.getName());
        Resource resource = Resource.createByResource(ForeignableOwner.getSystemOwner(), resEnt, resType, new ContextEntity());
        Mockito.when(resourceBoundaryMock.createNewResourceByName(ForeignableOwner.getSystemOwner(), resGroup.getName(), resType.getName(), release.getName())).thenReturn(resource);

        // when
        Response response = rest.addResource(resourceReleaseDTO);

        // then
        assertEquals(CREATED.getStatusCode(), response.getStatus());
        assertTrue(response.getMetadata().get("Location").contains("/resources/" + resGroup.getName()));
    }

    @Test
    public void shouldNotAllowCreationOfResourceReleaseOfAResourcesWithEmptyName() {
        // given
        ResourceReleaseCopyDTO resourceReleaseCopyDTO = new ResourceReleaseCopyDTO();
        resourceReleaseCopyDTO.setReleaseName("");
        resourceReleaseCopyDTO.setSourceReleaseName("TestRelease");

        // when
        Response response = rest.addNewResourceRelease(resourceReleaseCopyDTO, "TestResourceGroup");

        // then
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    @Test
    public void shouldNotAllowCreationOfResourceReleaseOfAResourcesWithoutOriginRelease() {
        // given
        ResourceReleaseCopyDTO resourceReleaseCopyDTO = new ResourceReleaseCopyDTO();
        resourceReleaseCopyDTO.setReleaseName("NewRelease");
        resourceReleaseCopyDTO.setSourceReleaseName("");

        // when
        Response response = rest.addNewResourceRelease(resourceReleaseCopyDTO, "TestResourceGroup");

        // then
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    @Test
    public void shouldReturnExpectedLocationHeaderOnSuccessfullResourceReleaseCreation() throws AMWException, ForeignableOwnerViolationException {
        // given
        ResourceReleaseCopyDTO resourceReleaseCopyDTO = new ResourceReleaseCopyDTO();
        resourceReleaseCopyDTO.setReleaseName("NewRelease");
        resourceReleaseCopyDTO.setSourceReleaseName("TestRelease");
        String resourceGroupName = "TestApp";

        CopyResourceResult copyResourceResult = new CopyResourceResult(resourceGroupName);
        Mockito.when(copyResourceMock.doCreateResourceRelease(resourceGroupName, resourceReleaseCopyDTO.getReleaseName(),
                resourceReleaseCopyDTO.getSourceReleaseName(), ForeignableOwner.getSystemOwner())).thenReturn(copyResourceResult);

        // when
        Response response = rest.addNewResourceRelease(resourceReleaseCopyDTO, resourceGroupName);

        // then
        assertEquals(CREATED.getStatusCode(), response.getStatus());
        assertTrue(response.getMetadata().get("Location").contains("/resources/" + resourceGroupName + "/" + resourceReleaseCopyDTO.getReleaseName()));
    }

    @Test
    public void shouldReturnBadRequestIfReleaseCreationFailed() throws AMWException, ForeignableOwnerViolationException {
        // given
        ResourceReleaseCopyDTO resourceReleaseCopyDTO = new ResourceReleaseCopyDTO();
        resourceReleaseCopyDTO.setReleaseName("NewRelease");
        resourceReleaseCopyDTO.setSourceReleaseName("TestRelease");
        String resourceGroupName = "TetApp";

        CopyResourceResult copyResourceResult = new CopyResourceResult(resourceGroupName);
        copyResourceResult.getExceptions().add("bogus");
        Mockito.when(copyResourceMock.doCreateResourceRelease(resourceGroupName, resourceReleaseCopyDTO.getReleaseName(),
                resourceReleaseCopyDTO.getSourceReleaseName(), ForeignableOwner.getSystemOwner())).thenReturn(copyResourceResult);

        // when
        Response response = rest.addNewResourceRelease(resourceReleaseCopyDTO, resourceGroupName);

        // then
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void shouldInvokeResourcesWithRightArgumentsOnGetClosestPastRelease() throws ValidationException, NotFoundException {
        // given
        String resourceGroupName = "TEST";
        String releaseName = "RL-17.10";
        String env = "V";
        String resourceTypeName = "APPLICATION";
        ReleaseEntity closestRelease = new ReleaseEntity();
        closestRelease.setName("RL-16.10");
        when(resourceLocatorMock.getExactOrClosestPastReleaseByGroupNameAndRelease(resourceGroupName,releaseName)).thenReturn(closestRelease);

        // when
        rest.getExactOrClosestPastRelease(resourceGroupName, releaseName, env, resourceTypeName);

        // then
        verify(resourceRelationsMock).getResourceRelations(resourceGroupName, closestRelease.getName(), resourceTypeName);
        verify(resourcePropertiesMock).getResourceProperties(resourceGroupName, closestRelease.getName(), env);
        verify(resourceTemplatesRestMock).getResourceTemplates(resourceGroupName, closestRelease.getName());
    }

    @Test
    public void shouldInvokeBoundaryWithRightArgumentsOnGetApplicationsWithVersionForRelease() {
        // given
        Integer resourceGroupId = 8;
        Integer releaseId = 9;
        ResourceEntity appServer = new ResourceEntity();
        List<Integer> contextIds = Arrays.asList(1,2);
        ReleaseEntity release = new ReleaseEntity();
        release.setName("TestRelease");
        when(resourceLocatorMock.getExactOrClosestPastReleaseByGroupIdAndReleaseId(resourceGroupId, releaseId)).thenReturn(appServer);
        when(releaseLocatorMock.getReleaseById(releaseId)).thenReturn(release);

        // when
        rest.getApplicationsWithVersionForRelease(resourceGroupId, releaseId, contextIds);

        // then
        verify(deploymentBoundaryMock, times(1)).getVersions(appServer, contextIds, release);
    }

    @Test
    public void shouldReturnBadRequestWhenCopyFromDoesNotSucceed() throws ValidationException, ForeignableOwnerViolationException, AMWException {
        // given
        CopyResourceResult copyResourceResult = mock(CopyResourceResult.class);
        when(copyResourceResult.isSuccess()).thenReturn(false);
        when(copyResourceMock.doCopyResource("targetResourceGroupName", "targetReleaseName", "originResourceGroupName", "originReleaseName")).thenReturn(copyResourceResult);

        // when
        final Response response = rest.copyFromResource("targetResourceGroupName", "targetReleaseName", "originResourceGroupName", "originReleaseName");

        // then
        assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
        assertThat(((ExceptionDto) response.getEntity()).getMessage(), is("Copy from Origin failed"));
    }

    @Test
    public void shouldReturnBadRequestWhenResourcesOfDifferentTypes() throws ValidationException, ForeignableOwnerViolationException, AMWException {
        when(copyResourceMock.doCopyResource("targetResourceGroupName", "targetReleaseName", "originResourceGroupName", "originReleaseName")).thenThrow(new AMWException("Target and origin Resource are not of the same ResourceType"));

        // when
        Response response = rest.copyFromResource("targetResourceGroupName", "targetReleaseName", "originResourceGroupName", "originReleaseName");

        // then
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void shouldAllowCopy() throws ValidationException, ForeignableOwnerViolationException, AMWException {
        // given
        CopyResourceResult copyResourceResult = mock(CopyResourceResult.class);
        when(copyResourceResult.isSuccess()).thenReturn(true);
        when(copyResourceMock.doCopyResource("targetResourceGroupName", "targetReleaseName", "originResourceGroupName", "originReleaseName")).thenReturn(copyResourceResult);

        // when
        Response response = rest.copyFromResource("targetResourceGroupName", "targetReleaseName", "originResourceGroupName", "originReleaseName");

        // then
        assertEquals(OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void shouldFilterOutAppServerContainer() {
        // given
        ResourceGroupEntity wanted = new ResourceGroupEntity();
        wanted.setId(7);
        wanted.setName("wanted");
        ResourceGroupEntity unWanted = new ResourceGroupEntity();
        unWanted.setId(8);
        unWanted.setName(APPSERVERCONTAINER.getDisplayName());
        when(resourceGroupLocatorMock.getAllResourceGroupsByName()).thenReturn(Arrays.asList(wanted, unWanted));

        // when
        List<ResourceGroupDTO> filteredGroups = rest.getAllResourceGroups(false);

        // then
        assertThat(filteredGroups.size(), is(1));
        assertThat(filteredGroups.get(0).getName(), is(wanted.getName()));
    }

    @Test
    public void shouldDeleteResource() throws ResourceNotFoundException, ElementAlreadyExistsException, ForeignableOwnerViolationException {
        // given
        Integer resourceGroupId = 8;
        Integer resourceId = 9;
        Integer releaseId = 10;
        ResourceEntity resource = new ResourceEntity();
        resource.setId(resourceId);
        when(resourceDependencyResolverServiceMock.getResourceEntityForRelease(resourceGroupId, releaseId)).thenReturn(resource);

        // when
        rest.deleteResourceRelease(resourceGroupId, releaseId);

        // then
        verify(resourceBoundaryMock, times(1)).removeResource(ForeignableOwner.getSystemOwner(), resourceId);
    }

    @Test
    public void shouldReturnNotFoundWhenDeleteResource() throws ResourceNotFoundException, ElementAlreadyExistsException, ForeignableOwnerViolationException {
        // given
        Integer resourceGroupId = 8;
        Integer releaseId = 10;
        when(resourceDependencyResolverServiceMock.getResourceEntityForRelease(resourceGroupId, releaseId)).thenReturn(null);

        // when
        final Response response = rest.deleteResourceRelease(resourceGroupId, releaseId);

        // then
        assertThat(response.getStatus(), is(NOT_FOUND.getStatusCode()));
        assertThat(((ExceptionDto) response.getEntity()).getMessage(), is("Resource not found"));
    }

}
