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

package ch.puzzle.itc.mobiliar.business.resourcegroup.control;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import ch.puzzle.itc.mobiliar.business.auditview.control.AuditService;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ch.puzzle.itc.mobiliar.business.domain.commons.CommonDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.foreignable.control.ForeignableService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService.CopyMode;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceFactory;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.test.testrunner.PersistenceTestRunner;

@RunWith(PersistenceTestRunner.class)
public class CopyResourceDomainServicePersistenceTest {

	@Spy
	@PersistenceContext
	EntityManager entityManager;

	@Mock
	CommonDomainService commonDomainService;

    @Mock
    ForeignableService foreignableServiceMock;

    @Mock
    ResourceRepository resourceRepositoryMock;

    @Mock
	AuditService auditService;

	@InjectMocks
	CopyResourceDomainService service;

	@Mock
	Logger log;


	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
	}

	/**
	 * If there is a property overwritten in context other than Global, this property shoud be copied with
	 * the correct value.<br>
	 * Origin: Global port=8009, B port=8008<br>
	 * Copy: Global port=8009, B port=8008<br>
	 * The propertyDescriptor for 'port' for resource Copy should not be the same as for Origin!
	 * 
	 * @throws ResourceNotFoundException
	 */
	@Test
	public void test_propertyOverwrittenInContext() throws ForeignableOwnerViolationException, AMWException {
		// given
		ResourceTypeEntity appType = new ResourceTypeEntity();
		appType.setName(DefaultResourceTypeDefinition.APPLICATION.name());
		ResourceEntity origin = ResourceFactory.createNewResource("app1");
		origin.setResourceType(appType);

		ContextEntity contextGlobal = new ContextEntity();
		contextGlobal.setName("Global");
		ContextEntity contextB = new ContextEntity();
		contextB.setName("B");
		contextB.setParent(contextGlobal);

		ResourceContextEntity resContextGlobal = new ResourceContextEntity();
		resContextGlobal.setContext(contextGlobal);
		resContextGlobal.setContextualizedObject(origin);
		
		ResourceContextEntity resContextB = new ResourceContextEntity();
		resContextB.setContext(contextB);
		resContextB.setContextualizedObject(origin);

		PropertyDescriptorEntity desc = new PropertyDescriptorEntity();
		desc.setPropertyName("port");
		resContextGlobal.addPropertyDescriptor(desc);

		PropertyEntity prop = new PropertyEntity();
		prop.setDescriptor(desc);
		prop.setValue("8008");
		resContextB.addProperty(prop);

		origin.addContext(resContextGlobal);
		origin.addContext(resContextB);

		entityManager.persist(origin);
		when(commonDomainService.getResourceEntityById(origin.getId())).thenReturn(origin);

		ResourceEntity target = ResourceFactory.createNewResource(origin.getResourceGroup());
		target.setResourceType(appType);
		entityManager.persist(target);
		when(commonDomainService.getResourceEntityById(target.getId())).thenReturn(target);

		// when
		CopyResourceResult result = service.doCopyResourceAndSave(new CopyUnit(origin, target, CopyMode.COPY, ForeignableOwner.AMW));

		// then
		assertTrue(result.isSuccess());
		ResourceEntity copy = entityManager.find(ResourceEntity.class, target.getId());
		assertTrue(!copy.getContexts().isEmpty());
		ResourceContextEntity context = null;
		for (ResourceContextEntity c : copy.getContexts()) {
			if (c.getContext().equals(contextB)) {
				context = c;
				assertNotNull(c.getProperties());
				assertTrue(!c.getProperties().isEmpty());
				PropertyEntity propB = null;
				for (PropertyEntity p : c.getProperties()) {
					if (p.getDescriptor().getPropertyName().equals(desc.getPropertyName())) {
						propB = p;
					}
				}
				assertNotNull(propB);
				assertEquals("8008", propB.getValue());
				assertNotEquals(desc.getId(), propB.getDescriptor().getId());
			}
		}
		assertNotNull(context);
		verify(auditService).storeIdInThreadLocalForAuditLog(target);
	}
}
