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

package ch.puzzle.itc.mobiliar.business.softlinkRelation.control;

import ch.puzzle.itc.mobiliar.builders.ReleaseEntityBuilder;
import ch.puzzle.itc.mobiliar.business.auditview.control.AuditService;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeProvider;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceFactory;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.entity.SoftlinkRelationEntity;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.test.testrunner.PersistenceTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link SoftlinkRelationService}
 */
@RunWith(PersistenceTestRunner.class)
public class SoftlinkRelationServicePersistenceTest {

    @Spy
    @PersistenceContext
    EntityManager entityManager;

    @Mock
    AuditService auditService;

    @Mock
    Logger log;

    @InjectMocks
    ResourceDependencyResolverService dependencyResolverService;

    @InjectMocks
    SoftlinkRelationService service;

    @InjectMocks
    ResourceTypeProvider resourceTypeProvider;

    @Mock
    ResourceTypeRepository resourceTypeRepository;


    ResourceTypeEntity type1;
    ResourceTypeEntity type2;
    ResourceEntity cpiResource;
    ResourceEntity ppiResource;
    ResourceEntity ppiResource2;
    SoftlinkRelationEntity softlinkRelation;
    String softlinkRef = "softlinkRef";
    ReleaseEntity pastRelease;
    ReleaseEntity release2;


    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
        service.dependencyResolverService = dependencyResolverService;
        init();
        EntityTransaction transaction = entityManager.getTransaction();
        transaction.commit();
        transaction.begin();
    }

    private void init() {
        // Release
        pastRelease = new ReleaseEntityBuilder().buildReleaseEntity("Past", new Date(), false);
        entityManager.persist(pastRelease);

        Calendar cal = Calendar.getInstance();
        cal.setTime(pastRelease.getInstallationInProductionAt());
        cal.add(Calendar.DATE, 1);
        release2 = new ReleaseEntityBuilder().buildReleaseEntity("rel", cal.getTime(), false);
        entityManager.persist(release2);

        // ResourceTypes
        type1 = resourceTypeProvider.getOrCreateDefaultResourceType(DefaultResourceTypeDefinition.APPLICATIONSERVER);
        entityManager.persist(type1);
        type2 = resourceTypeProvider.getOrCreateDefaultResourceType(DefaultResourceTypeDefinition.APPLICATION);
        entityManager.persist(type2);

        // Resources
        cpiResource = ResourceFactory.createNewResource("CPI");
        cpiResource.setResourceType(type1);
        cpiResource.setRelease(pastRelease);
        entityManager.persist(cpiResource);

        ppiResource = ResourceFactory.createNewResource("PPI");
        ppiResource.setResourceType(type2);
        ppiResource.setSoftlinkId(softlinkRef);
        ppiResource.setRelease(pastRelease);
        entityManager.persist(ppiResource);

        ppiResource2 = ResourceFactory.createNewResource(ppiResource.getResourceGroup());
        ppiResource2.setResourceType(type2);
        ppiResource2.setSoftlinkId(softlinkRef);
        ppiResource2.setRelease(release2);
        entityManager.persist(ppiResource2);

        // Softlinks
        softlinkRelation = new SoftlinkRelationEntity();
        softlinkRelation.setCpiResource(cpiResource);
        softlinkRelation.setSoftlinkRef(softlinkRef);
        entityManager.persist(softlinkRelation);

        entityManager.flush();
        entityManager.refresh(cpiResource);

        assertNotNull(entityManager.find(SoftlinkRelationEntity.class, softlinkRelation.getId()));
        assertNotNull(cpiResource.getSoftlinkRelation());
    }

	@Test
	public void test_getSoftLinkRelationByCpiAndSoftlinkRef() {
		// when
		SoftlinkRelationEntity result = service.getSoftLinkRelationByCpiAndSoftlinkRef(cpiResource,
				softlinkRef);

		// then
		assertNotNull(result);
		assertEquals(softlinkRelation.getId(), result.getId());
	}

    @Test
    public void shouldRemoveSoftlinkRelation() {
        // when
        service.removeSoftlinkRelation(cpiResource);

        // then
        assertNull(entityManager.find(SoftlinkRelationEntity.class, softlinkRelation.getId()));
    }

    @Test
    public void shouldSetSoftlinkRelationAndRemoveExisting() {
        //given
        SoftlinkRelationEntity softlinkRelation2 = new SoftlinkRelationEntity();
        String softlinkRef2 = "softlinkRef2";
        softlinkRelation2.setCpiResource(cpiResource);
        softlinkRelation2.setSoftlinkRef(softlinkRef2);

        // when
        service.setSoftlinkRelation(cpiResource, softlinkRelation2);
        entityManager.flush();

        // then
        verify(entityManager).remove(softlinkRelation);
        verify(entityManager).persist(softlinkRelation2);

    }

    @Test
    public void getSoftlinkResolvableSlaveResourceShouldReturnSlaveResourceResolvedBySoftlink() {
        // when
        ResourceEntity result1 = service.getSoftlinkResolvableSlaveResource(softlinkRef, pastRelease);

        // then
        assertNotNull(result1);
        assertEquals(ppiResource.getId(), result1.getId());
    }

    @Test
    public void getSoftlinkResolvableSlaveResourceShouldReturnNullWhenNoResourceForSoftlinkExists() {
        // when
        ResourceEntity result = service.getSoftlinkResolvableSlaveResource("any softlink", pastRelease);

        // then
        assertNull(result);
    }

}