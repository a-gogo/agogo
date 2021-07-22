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

package ch.puzzle.itc.mobiliar.business.resourcegroup.boundary;

import ch.puzzle.itc.mobiliar.business.domain.applist.ApplistScreenDomainService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.*;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.usersettings.control.UserSettingsService;
import ch.puzzle.itc.mobiliar.business.usersettings.entity.UserSettingsEntity;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.test.testrunner.PersistenceTestRunner;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.Date;

@RunWith(PersistenceTestRunner.class)
public class ResourceRelationsIntegrationTest {

	@PersistenceContext
	private EntityManager entityManager;

	@InjectMocks
	ResourceRelations service;

     @Mock
    	UserSettingsService userSettingsService;
    	@Mock
    	PermissionService permissionService;

    	@Mock
    	ApplistScreenDomainService applistScreenDomainService;

    	@Mock
    	UserSettingsEntity userSettingsEntity;

    @InjectMocks
    ResourceDependencyResolverService dependencyResolver;

    ReleaseEntity release1;
    ReleaseEntity release2;
    ResourceTypeEntity astype;
    ResourceTypeEntity apptype;
    ResourceEntity asRel1;
    ResourceEntity asRel2;
    ResourceEntity appRel1;
    ResourceEntity appRel2;

    	@Before
    	public void before() {
	   MockitoAnnotations.openMocks(this);
	   release1 = new ReleaseEntity();
	   release1.setName("Release 1");
	   release1.setInstallationInProductionAt(DateUtils.addDays(new Date(), -1));
	   release2 = new ReleaseEntity();
	   release2.setName("Release 2");
	   release2.setInstallationInProductionAt(new Date());
	   entityManager.persist(release1);
	   entityManager.persist(release2);

	    astype = new ResourceTypeEntity();
	    astype.setName(DefaultResourceTypeDefinition.APPLICATIONSERVER.name());
	    apptype = new ResourceTypeEntity();
	    apptype.setName(DefaultResourceTypeDefinition.APPLICATION.name());
	    entityManager.persist(astype);
	    entityManager.persist(apptype);

	    asRel1 = ResourceFactory.createNewResource("as");
	    asRel1.setRelease(release1);
	    asRel2 = ResourceFactory.createNewResource(asRel1.getResourceGroup());
	    asRel2.setRelease(release2);
	    entityManager.persist(asRel1);
	    entityManager.persist(asRel2);

	    appRel1 = ResourceFactory.createNewResource("app");
	    appRel1.setRelease(release1);
	    appRel2 = ResourceFactory.createNewResource(appRel1.getResourceGroup());
	    appRel2.setRelease(release2);
	    entityManager.persist(appRel1);
	    entityManager.persist(appRel2);

	    ResourceRelationTypeEntity resRelType = new ResourceRelationTypeEntity();
	    resRelType.setResourceTypes(astype, apptype);
	    entityManager.persist(resRelType);

	    entityManager.persist(asRel1.addConsumedResourceRelation(appRel1, resRelType, null, ForeignableOwner.AMW));
	    entityManager.persist(asRel1.addConsumedResourceRelation(appRel2, resRelType, null, ForeignableOwner.AMW));
	    entityManager.persist(asRel2.addConsumedResourceRelation(appRel1, resRelType, null, ForeignableOwner.AMW));
	    entityManager.persist(asRel2.addConsumedResourceRelation(appRel2, resRelType, null, ForeignableOwner.AMW));
	     Mockito.when(applistScreenDomainService.getAppServerResourcesWithApplications(Mockito.anyString(),
				Mockito.anyInt(), Mockito.anyList(), Mockito.anyBoolean())).thenReturn(
			    Arrays.asList(asRel1,asRel2));
	    service.dependencyResolverService = dependencyResolver;
	}

	@Test
	public void testGetAppServersWithApplications() {
	    Mockito.when(userSettingsEntity.isMyAmwEnabled()).thenReturn(false);
	    service.getAppServersWithApplications("app", null, release1);
	}

}
