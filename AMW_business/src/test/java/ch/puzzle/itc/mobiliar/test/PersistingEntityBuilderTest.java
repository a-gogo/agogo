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

package ch.puzzle.itc.mobiliar.test;

import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextTypeEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.AMWTemplateExceptionHandler;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratorUtils;
import ch.puzzle.itc.mobiliar.business.generator.control.TemplateUtils;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException;
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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(PersistenceTestRunner.class)
public class PersistingEntityBuilderTest {

	@Mock
	Logger logger;

	@PersistenceContext
	@Spy
	EntityManager entityManager;

	@Mock
	PermissionService permissionsService;

	@InjectMocks
	GeneratorUtils generatorUtils;

	PersistingEntityBuilder builder;

	Boolean testing = false;

	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void testAmw() {
		builder = new PersistingEntityBuilder(entityManager).buildAmw();
		assertEquals(16, builder.getResourceCount(ResourceEntity.class));
	}

	@Test
	public void testSimple() {
		builder = new PersistingEntityBuilder(entityManager).buildSimple();

		assertEquals(3, builder.getResourceCount(ResourceEntity.class));
		assertEquals(1, builder.getResourceCount(ContextEntity.class));
		assertEquals(1, builder.getResourceCount(ContextTypeEntity.class));
		assertEquals(3, builder.getResourceCount(ResourceGroupEntity.class));
	}

	@Test
	public void testSimpleWithProperties() throws TemplatePropertyException {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		builder = new PersistingEntityBuilder(entityManager).buildSimple();
		ResourceEntity as = builder.resourceFor(EntityBuilderType.AS);
		Map<PropertyDescriptorEntity, List<PropertyEntity>> propertyMap = TemplateUtils
				.getPropertyValues(as, builder.context, false,
						templateExceptionHandler);
		List<PropertyEntity> properties = generatorUtils.translatePropertyList(propertyMap);

		assertEquals("testValue", properties.get(0).getDecryptedValue().toString());
		assertTrue(templateExceptionHandler.isSuccess());
	}

}
