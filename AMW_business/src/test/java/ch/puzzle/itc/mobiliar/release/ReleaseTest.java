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

package ch.puzzle.itc.mobiliar.release;

import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.AMWTemplateExceptionHandler;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratorUtils;
import ch.puzzle.itc.mobiliar.business.generator.control.TemplateUtils;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.utils.Identifiable;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException;
import ch.puzzle.itc.mobiliar.test.PersistingEntityBuilder;
import ch.puzzle.itc.mobiliar.test.testrunner.PersistenceEnversTestRunner;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;

import static ch.puzzle.itc.mobiliar.test.EntityBuilderType.APP;
import static ch.puzzle.itc.mobiliar.test.EntityBuilderType.AS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(PersistenceEnversTestRunner.class)
public class ReleaseTest {


     @InjectMocks
    	GeneratorUtils generatorUtils;

	@Mock
	PermissionService permissions;

	@PersistenceContext
	EntityManager entityManager;

	ResourceEntity as;
	ResourceEntity app;
	ContextEntity context;
	PersistingEntityBuilder builder;

	boolean testing;

	AuditReader reader;

	ResourceContextEntity asContext;


	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
		builder = new PersistingEntityBuilder(entityManager).buildSimple();
		as = builder.resourceFor(AS);
		app = builder.resourceFor(APP);
		context = builder.context;
		asContext = as.getContextsByLowestContext(context).get(0);

		reader = AuditReaderFactory.get(entityManager);
	}

	@Test
	public void testPropertiesAndRevisions() throws TemplatePropertyException, ResourceNotFoundException {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		List<PropertyEntity> list = readProperties(templateExceptionHandler);
	    PropertyEntity property = list.get(0);
		assertEquals("testValue", property.getDecryptedValue().toString());
		property.setValueAndEncrypt("xxx");

		List<Number> revisions = reader.getRevisions(PropertyEntity.class, property.getId());
		assertEquals(0, revisions.size());
		persistAndRestart(property);

		property = readProperties(templateExceptionHandler).get(0);
		assertEquals("xxx", property.getDecryptedValue().toString());

		revisions = reader.getRevisions(PropertyEntity.class, property.getId());
		assertEquals(1, revisions.size());
		assertTrue(templateExceptionHandler.isSuccess());
	}

	private void persistAndRestart(Identifiable... entities) {
		for (Identifiable entity : entities) {
			entityManager.persist(entity);
		}

		entityManager.getTransaction().commit();
		entityManager.getTransaction().begin();
	}

	private List<PropertyEntity> readProperties(AMWTemplateExceptionHandler templateExceptionHandler) {
		Map<PropertyDescriptorEntity, List<PropertyEntity>> propertyValues = TemplateUtils.getPropertyValues(as, context, testing,
				templateExceptionHandler);
		assertEquals(1, propertyValues.size());
		List<PropertyEntity> list = generatorUtils.translatePropertyList(propertyValues);
		return list;
	}

}
