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

package ch.puzzle.itc.mobiliar.business.resourcegroup.entity;

import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.builders.ResourceTypeEntityBuilder;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceResult;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyUnit;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.AbstractResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.util.ApplicationServerContainer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class ResourceEntityTest {

	@Mock
	ResourceEntity resourceEntity;

	ResourceEntity slaveResourceEntity = ResourceFactory.createNewResource();
	ResourceEntity masterResource = ResourceFactory.createNewResource();
	ResourceRelationTypeEntity relationTypeEntity = new ResourceRelationTypeEntity();
	ProvidedResourceRelationEntity providedResourceRelationEntity = new ProvidedResourceRelationEntity();
	private ResourceEntityBuilder resourceEntityBuilder = new ResourceEntityBuilder();
	private ResourceTypeEntityBuilder typeEntityBuilder = new ResourceTypeEntityBuilder();

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		providedResourceRelationEntity.setResourceRelationType(relationTypeEntity);
		providedResourceRelationEntity.setMasterResource(masterResource);
		providedResourceRelationEntity.setSlaveResource(slaveResourceEntity);
	}

	@Test
	public void testAddProvidedResourceRelation() {
		assertEquals(relationTypeEntity, providedResourceRelationEntity.getResourceRelationType());
		assertEquals(slaveResourceEntity, providedResourceRelationEntity.getSlaveResource());
	}

	@Test
	public void testGetRelationById() {
		// given
		Mockito.when(resourceEntity.getRelationById(ArgumentMatchers.anyInt(), ArgumentMatchers.anySet())).thenCallRealMethod();

		final int slaveResourceId = 21;

		final Set<ConsumedResourceRelationEntity> relations = new HashSet<ConsumedResourceRelationEntity>();
		final ConsumedResourceRelationEntity resourceRelation = Mockito.mock(ConsumedResourceRelationEntity.class);
		relations.add(resourceRelation);
		final ResourceEntity slaveResource = Mockito.mock(ResourceEntity.class);
		Mockito.when(slaveResource.getId()).thenReturn(slaveResourceId);
		Mockito.when(resourceRelation.getSlaveResource()).thenReturn(slaveResource);

		// when
		final AbstractResourceRelationEntity result = resourceEntity.getRelationById(slaveResourceId, relations);

		// then
		assertEquals(resourceRelation, result);

	}

	@Test
	public void testGetMasterRelation() {
		// given
		Mockito.when(resourceEntity.getMasterRelation(ArgumentMatchers.any(ResourceEntity.class), ArgumentMatchers.anySet())).thenCallRealMethod();

		final int slaveResourceId = 21;
		final Set<ConsumedResourceRelationEntity> relations = new HashSet<ConsumedResourceRelationEntity>();
		final ConsumedResourceRelationEntity resourceRelation = Mockito.mock(ConsumedResourceRelationEntity.class);
		relations.add(resourceRelation);
		final ResourceEntity slaveResource = Mockito.mock(ResourceEntity.class);
		Mockito.when(slaveResource.getId()).thenReturn(slaveResourceId);
		Mockito.when(resourceRelation.getSlaveResource()).thenReturn(slaveResource);

		// when
		final AbstractResourceRelationEntity result = resourceEntity.getMasterRelation(slaveResource, relations);

		// then
		assertEquals(resourceRelation, result);

	}



	private ConsumedResourceRelationEntity createConsumedResourceRelation(final int masterResourceId, final int slaveResourceId) {
		final ConsumedResourceRelationEntity resourceRelation = Mockito.mock(ConsumedResourceRelationEntity.class);

		final ResourceEntity slaveResource = Mockito.mock(ResourceEntity.class);
		Mockito.when(slaveResource.getId()).thenReturn(slaveResourceId);
		Mockito.when(resourceRelation.getSlaveResource()).thenReturn(slaveResource);

		final ResourceEntity masterResource = Mockito.mock(ResourceEntity.class);
		Mockito.when(masterResource.getId()).thenReturn(masterResourceId);
		Mockito.when(resourceRelation.getMasterResource()).thenReturn(masterResource);

		return resourceRelation;
	}

	private ConsumedResourceRelationEntity prepareConsumedSlaveRelationTest() {
		Mockito.when(resourceEntity.getConsumedSlaveRelation(ArgumentMatchers.any(ResourceEntity.class))).thenCallRealMethod();

		final ConsumedResourceRelationEntity consumedResourceRelationEntity = createConsumedResourceRelation(11, 12);

		final Set<ConsumedResourceRelationEntity> relations = new HashSet<ConsumedResourceRelationEntity>();
		relations.add(consumedResourceRelationEntity);

		Mockito.when(resourceEntity.getConsumedSlaveRelations()).thenReturn(relations);

		return consumedResourceRelationEntity;
	}

	@Test
	public void testGetConsumedSlaveRelation() {
		ConsumedResourceRelationEntity consumedResourceRelationEntity = prepareConsumedSlaveRelationTest();

		ResourceEntity queryEntity = ResourceFactory.createNewResource("testName");
		queryEntity.setId(11);

		assertEquals(consumedResourceRelationEntity, resourceEntity.getConsumedSlaveRelation(queryEntity));
	}

	@Test
	public void testGetConsumedSlaveRelationWithoutName() {
		prepareConsumedSlaveRelationTest();

		final ResourceEntity queryEntity = ResourceFactory.createNewResource();
		queryEntity.setId(11);

		assertNull(resourceEntity.getConsumedSlaveRelation(queryEntity));
	}

	@Test
	public void testGetConsumedSlaveRelationWithAppserverDisplayName() {
		prepareConsumedSlaveRelationTest();

		ResourceEntity queryEntity = ResourceFactory.createNewResource(ApplicationServerContainer.APPSERVERCONTAINER.getDisplayName());
		queryEntity.setId(11);

		assertNull(resourceEntity.getConsumedSlaveRelation(queryEntity));
	}

	@Test
	public void testGetConsumedSlaveRelationWithWrongResource() {
		prepareConsumedSlaveRelationTest();

		final ResourceEntity queryEntity = ResourceFactory.createNewResource(ApplicationServerContainer.APPSERVERCONTAINER.getDisplayName());
		queryEntity.setId(12);

		assertNull(resourceEntity.getConsumedSlaveRelation(queryEntity));
	}

    @Test
    public void getExternalLinkWhenResourceGroupIsNullShouldReturnNull() {
        // given
        resourceEntity = new ResourceEntityBuilder().build();
        resourceEntity.setResourceGroup(null);

        // when
        String externalLink = resourceEntity.getExternalLink();

        // then
        assertNull(externalLink);
    }

    @Test
    public void getExternalLinkShouldReturnExternalLinkFromGroup() {
        // given
        String externalLink = "externalLink";
        ResourceGroupEntity group = new ResourceGroupEntity();
        group.setFcExternalLink(externalLink);
        resourceEntity = new ResourceEntityBuilder().forResourceGroup(group).build();

        // when
        String delegatedExternalLink = resourceEntity.getExternalLink();

        // then
        assertEquals(externalLink, delegatedExternalLink);
    }

    @Test
    public void getExternalKeyWhenResourceGroupIsNullShouldReturnNull() {
        // given
        resourceEntity = new ResourceEntityBuilder().build();
        resourceEntity.setResourceGroup(null);

        // when
        String externalLink = resourceEntity.getExternalKey();

        // then
        assertNull(externalLink);
    }

    @Test
    public void getExternalKeyShouldReturnExternalKeyFromGroup() {
        // given
        String externalKey = "externalKey";
        ResourceGroupEntity group = new ResourceGroupEntity();
        group.setFcExternalKey(externalKey);
        resourceEntity = new ResourceEntityBuilder().forResourceGroup(group).build();

        // when
        String delegatedExternalKey = resourceEntity.getExternalKey();

        // then
        assertEquals(externalKey, delegatedExternalKey);
    }

    @Test
    public void setExternalLinkWhenResourceGroupIsNullShouldNotSetLinkInGroup() {
        // given
        String externalLink = "externalLink";
        resourceEntity = new ResourceEntityBuilder().build();
        resourceEntity.setResourceGroup(null);

        // when
        resourceEntity.setExternalLink(externalLink);

        // then
        assertNull(resourceEntity.getExternalLink());
    }

    @Test
    public void setExternalLinkShouldSetExternalLinkInGroup() {
        // given
        String externalLink = "externalLink";
        ResourceGroupEntity group = new ResourceGroupEntity();
        resourceEntity = new ResourceEntityBuilder().forResourceGroup(group).build();
        assertNull(group.getFcExternalLink());

        // when
        resourceEntity.setExternalLink(externalLink);

        // then
        assertEquals(externalLink, group.getFcExternalLink());
    }

    @Test
    public void setExternalKeyWhenResourceGroupIsNullShouldNotSetKeyInGroup() {
        // given
        String externalKey = "externalKey";
        resourceEntity = new ResourceEntityBuilder().build();
        resourceEntity.setResourceGroup(null);

        // when
        resourceEntity.setExternalKey(externalKey);

        // then
        assertNull(resourceEntity.getExternalKey());
    }

    @Test
    public void setExternalKeyShouldSetExternalKeyInGroup() {
        // given
        String externalKey = "externalKey";
        ResourceGroupEntity group = new ResourceGroupEntity();
        resourceEntity = new ResourceEntityBuilder().forResourceGroup(group).build();
        assertNull(group.getFcExternalKey());

        // when
        resourceEntity.setExternalKey(externalKey);

        // then
        assertEquals(externalKey, group.getFcExternalKey());
    }

    @Test
    public void defaultConstructorShouldSetDefaultSystemOwner(){

        // when
        resourceEntity = new ResourceEntity();

        // then
        assertNotNull(resourceEntity.getOwner());
        assertEquals(ForeignableOwner.getSystemOwner(), resourceEntity.getOwner());
    }

    @Test
    public void constructorWithOwnerShouldSetOwner(){
        // given
        ForeignableOwner owner = ForeignableOwner.MAIA;

        // when
        resourceEntity = new ResourceEntity(owner);

        // then
        assertEquals(owner, resourceEntity.getOwner());
    }

    @Test(expected = NullPointerException.class)
    public void constructorWithNullShouldThrowException(){
        // given
        ForeignableOwner owner = null;

        // when
        resourceEntity = new ResourceEntity(owner);
    }

	@Test
	public void copyResourceEntity_resources_should_be_same_type_COPY() throws AMWException {
		copyResourceEntity_resources_should_be_same_type(CopyResourceDomainService.CopyMode.COPY);
	}

	@Test
	public void copyResourceEntity_resources_should_be_same_type_RELEASE() throws AMWException {
		copyResourceEntity_resources_should_be_same_type(CopyResourceDomainService.CopyMode.RELEASE);
	}

	private void copyResourceEntity_resources_should_be_same_type(CopyResourceDomainService.CopyMode copying) throws AMWException{
		// given
		ResourceEntity originResource = resourceEntityBuilder.buildAppServerEntity("origin", null, null, true);
		ResourceEntity targetResource = resourceEntityBuilder.buildApplicationEntity(
				"appTargetResource", null, null, true);
		CopyUnit copyUnit = new CopyUnit(originResource, targetResource, copying, ForeignableOwner.AMW);

		// when
		originResource.getCopy(targetResource, copyUnit);

		// then
		assertFalse(copyUnit.getResult().isSuccess());
		Set<CopyResourceResult.CopyFailure> failures = copyUnit.getResult().getExceptionEnums();
		assertTrue(failures.contains(CopyResourceResult.CopyFailure.RESOURCETYPE_DIFF));
	}

	@Test
	public void getCopy_COPY() throws AMWException{
		// given
		String targetName = "target";
		Integer targetId = 2;
		boolean targetDeletable = true;
		ResourceEntity targetResource = new ResourceEntityBuilder().withName(targetName).withId(targetId).withIsDeletable(targetDeletable).build();

		String origName = "origin";
		Integer origId = 1;
		boolean originDeletable = false;
		String originSoftlinkId = "origSoftlinkId";
		ResourceTypeEntity appType = typeEntityBuilder.buildApplicationResourceTypeEntity(null, true);
		ResourceEntity originResource = new ResourceEntityBuilder().withName(origName).withId(origId).withType(appType).withIsDeletable(originDeletable).withSoftlinkId(originSoftlinkId).build();

		CopyUnit copyUnit = new CopyUnit(originResource, targetResource, CopyResourceDomainService.CopyMode.COPY, ForeignableOwner.AMW);

		// when
		ResourceEntity copy = originResource.getCopy(targetResource, copyUnit);

		// then
		assertEquals(targetName, copy.getName());
		assertEquals(targetId, copy.getId());
		assertEquals(appType, copy.getResourceType());
		assertEquals(originDeletable, copy.isDeletable());
		assertEquals(originSoftlinkId, copy.getSoftlinkId());
	}

	@Test
	public void getCopy_RELEASE() throws AMWException{
		// given
		ResourceEntity targetResource = new ResourceEntityBuilder().build();

		String origName = "origin";
		Integer origId = 1;
		boolean originDeletable = false;
		String originSoftlinkId = "origSoftlinkId";
		ResourceTypeEntity appType = typeEntityBuilder.buildApplicationResourceTypeEntity(null, true);
		ResourceEntity originResource = new ResourceEntityBuilder().withName(origName).withId(origId).withType(appType).withIsDeletable(originDeletable).withSoftlinkId(originSoftlinkId).build();

		CopyUnit copyUnit = new CopyUnit(originResource, targetResource, CopyResourceDomainService.CopyMode.RELEASE, ForeignableOwner.AMW);

		// when
		ResourceEntity copy = originResource.getCopy(targetResource, copyUnit);

		// then
		assertNull(copy.getId());
		assertEquals(origName, copy.getName());
		assertEquals(origName, copyUnit.getResult().getTargetResourceName());
		assertEquals(appType, copy.getResourceType());
		assertEquals(originDeletable, copy.isDeletable());
		assertEquals(originSoftlinkId, copy.getSoftlinkId());
	}

}