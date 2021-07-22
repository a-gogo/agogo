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

package ch.puzzle.itc.mobiliar.business.resourcerelation.control;

import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.business.foreignable.control.ForeignableService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeProvider;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceFactory;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.AbstractResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.test.testrunner.PersistenceTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(PersistenceTestRunner.class)
public class ResourceRelationServicePersistenceTest {

	@Spy
	@PersistenceContext
	EntityManager entityManager;

	@Mock
	Logger log;

	@Mock
	ForeignableService foreignableServiceMock;

	@Mock
	ResourceTypeProvider resourceTypeProvider;

	@InjectMocks
	ResourceRelationService service;

	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void test_getConsumedRelationsByMasterAndSlave() {
		// given
		ResourceEntity master = ResourceFactory.createNewResource("master");
		ResourceEntity slave1 = ResourceFactory.createNewResource("slaveX");
		ResourceEntity slave2 = ResourceFactory.createNewResource(slave1.getResourceGroup());
		ResourceEntity slave3 = ResourceFactory.createNewResource("slaveY");

		ConsumedResourceRelationEntity relation1 = new ConsumedResourceRelationEntity();
		relation1.setMasterResource(master);
		relation1.setSlaveResource(slave1);
		relation1.setIdentifier("1");
		master.addConsumedRelation(relation1);
		ConsumedResourceRelationEntity relation2 = new ConsumedResourceRelationEntity();
		relation2.setMasterResource(master);
		relation2.setSlaveResource(slave2);
		relation2.setIdentifier("1");
		master.addConsumedRelation(relation2);

		ConsumedResourceRelationEntity relation3 = new ConsumedResourceRelationEntity();
		relation3.setMasterResource(master);
		relation3.setSlaveResource(slave1);
		relation3.setIdentifier("2");
		master.addConsumedRelation(relation3);
		ConsumedResourceRelationEntity relation4 = new ConsumedResourceRelationEntity();
		relation4.setMasterResource(master);
		relation4.setSlaveResource(slave2);
		relation4.setIdentifier("2");
		master.addConsumedRelation(relation4);

		ConsumedResourceRelationEntity relation5 = new ConsumedResourceRelationEntity();
		relation5.setMasterResource(master);
		relation5.setSlaveResource(slave3);
		relation5.setIdentifier("1");
		master.addConsumedRelation(relation5);

		entityManager.persist(master);

		Set<ResourceEntity> slaves = new HashSet<ResourceEntity>();
		slaves.add(slave1);
		slaves.add(slave2);

		// when
		List<AbstractResourceRelationEntity> result = service.getConsumedRelationsByMasterAndSlave(master, slaves, relation1.getIdentifier());

		// then
		assertNotNull(result);
		assertEquals(2, result.size());
		Set<Integer> ids = new HashSet<Integer>();
		for (AbstractResourceRelationEntity r : result) {
			ids.add(r.getSlaveResource().getId());
		}
		assertTrue(ids.contains(slave1.getId()));
		assertTrue(ids.contains(slave2.getId()));
	}

	@Test
	public void test_getProvidedRelationsByMasterAndSlave() {
		// given
		ResourceEntity master = ResourceFactory.createNewResource("master");
		ResourceEntity slave1 = ResourceFactory.createNewResource("slaveX");
		ResourceEntity slave2 = ResourceFactory.createNewResource(slave1.getResourceGroup());
		ResourceEntity slave3 = ResourceFactory.createNewResource("slaveY");

		ProvidedResourceRelationEntity relation1 = new ProvidedResourceRelationEntity();
		relation1.setMasterResource(master);
		relation1.setSlaveResource(slave1);
		relation1.setIdentifier("1");
		master.addProvidedRelation(relation1);
		ProvidedResourceRelationEntity relation2 = new ProvidedResourceRelationEntity();
		relation2.setMasterResource(master);
		relation2.setSlaveResource(slave2);
		relation2.setIdentifier("1");
		master.addProvidedRelation(relation2);

		ProvidedResourceRelationEntity relation3 = new ProvidedResourceRelationEntity();
		relation3.setMasterResource(master);
		relation3.setSlaveResource(slave1);
		relation3.setIdentifier("2");
		master.addProvidedRelation(relation3);
		ProvidedResourceRelationEntity relation4 = new ProvidedResourceRelationEntity();
		relation4.setMasterResource(master);
		relation4.setSlaveResource(slave2);
		relation4.setIdentifier("2");
		master.addProvidedRelation(relation4);

		ProvidedResourceRelationEntity relation5 = new ProvidedResourceRelationEntity();
		relation5.setMasterResource(master);
		relation5.setSlaveResource(slave3);
		relation5.setIdentifier("1");
		master.addProvidedRelation(relation5);


		entityManager.persist(master);

		Set<ResourceEntity> slaves = new HashSet<ResourceEntity>();
		slaves.add(slave1);
		slaves.add(slave2);

		// when
		List<AbstractResourceRelationEntity> result = service.getProvidedRelationsByMasterAndSlave(master, slaves, "1");

		// then
		assertNotNull(result);
		assertEquals(2, result.size());
		Set<Integer> ids = new HashSet<Integer>();
		for (AbstractResourceRelationEntity r : result) {
			ids.add(r.getSlaveResource().getId());
		}
		assertTrue(ids.contains(slave1.getId()));
		assertTrue(ids.contains(slave2.getId()));
	}

	@Test
	public void test_doAddResourceRelationForAllReleases() throws ResourceNotFoundException, ElementAlreadyExistsException {
		// given
		ResourceEntityBuilder resourceEntityBuilder = new ResourceEntityBuilder();
		ResourceEntity master = resourceEntityBuilder.buildApplicationEntity("master", null, null, false);
		entityManager.persist(master);

		ResourceEntity slave = resourceEntityBuilder.buildResourceEntity("slave", null, "Database", null, false);
		entityManager.persist(slave);

		ResourceRelationTypeEntity resourceRelationType = new ResourceRelationTypeEntity();
		resourceRelationType.setResourceTypes(master.getResourceType(), slave.getResourceType());
		when(resourceTypeProvider.getOrCreateResourceRelationTypeIncludingParents(master.getResourceType(), slave.getResourceType(), null)).thenReturn(resourceRelationType);

		// when
		service.doAddResourceRelationForAllReleases(master.getId(), slave.getId(), false, null, null, ForeignableOwner.MAIA);

		// then
		ConsumedResourceRelationEntity result = entityManager.find(ConsumedResourceRelationEntity.class, master.getConsumedRelationById(slave.getId()).getId());
		assertNotNull(result);
		assertEquals(master.getId(), result.getMasterResourceId());
		assertEquals(slave.getId(), result.getSlaveResourceTypeId());
		assertEquals(ForeignableOwner.MAIA, result.getOwner());
	}

}
