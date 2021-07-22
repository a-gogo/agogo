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

package ch.puzzle.itc.mobiliar.business.generator.control.extracted.properties;

import static ch.puzzle.itc.mobiliar.test.EntityBuilderType.APP;
import static ch.puzzle.itc.mobiliar.test.EntityBuilderType.WS;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;

import org.junit.Test;

import ch.puzzle.itc.mobiliar.business.domain.TestUtils;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.generator.control.AMWTemplateExceptionHandler;
import ch.puzzle.itc.mobiliar.business.property.entity.AmwResourceTemplateModel;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.test.CustomLogging;
import ch.puzzle.itc.mobiliar.test.SimpleEntityBuilder;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class SimpleAppServerRelationPropertiesTest {

	SimpleEntityBuilder builder = new SimpleEntityBuilder();

	static {
		CustomLogging.setup(Level.OFF);
	}

	@Test
	public void testSingleAsAppRelation() {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		AppServerRelationProperties properties = new AppServerRelationProperties(builder.context, builder.as, templateExceptionHandler);
		for (ConsumedResourceRelationEntity relation : builder.as.getConsumedMasterRelations()) {
			ResourceEntity slaveResource = relation.getSlaveResource();
			properties.addConsumedRelation(slaveResource.getName(), slaveResource, relation);
		}
		assertEquals(1, properties.getConsumed().size());
		assertEquals(0, properties.getProvided().size());
		assertTrue(templateExceptionHandler.isSuccess());
	}

	@Test
	public void testTwoRelatedResourcesOfSameType() throws TemplateModelException {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		ResourceEntity app2 = builder.buildResource(builder.typeFor(APP.type), "app2");
		builder.buildConsumedRelation(builder.as, app2, ForeignableOwner.AMW);

		AppServerRelationProperties properties = new AppServerRelationProperties(builder.context, builder.as, templateExceptionHandler);
		for (ConsumedResourceRelationEntity relation : builder.as.getConsumedMasterRelations()) {
			ResourceEntity slaveResource = relation.getSlaveResource();
			properties.addConsumedRelation(slaveResource.getName(), slaveResource, relation);
		}

		assertEquals(2, properties.getConsumed().size());
		AmwResourceTemplateModel hash = properties.transformModel();

        assertEquals("as", hash.get("label").toString());
        TemplateHashModel appModel = TestUtils.asHashModel(hash, "consumedResTypes", "APPLICATION", "app");
        assertEquals("app", appModel.get("label").toString());

        TemplateHashModel appModel2 = TestUtils.asHashModel(hash, "consumedResTypes", "APPLICATION", "app2");
        assertEquals("app2", appModel2.get("label").toString());

        TemplateHashModel propertyTypes = TestUtils.asHashModel(hash, "providedResTypes", "APPLICATION", "propertyTypes");
        assertTrue(propertyTypes.isEmpty());
		hash = properties.transformModel();
		assertTrue(templateExceptionHandler.isSuccess());
	}

	@Test
	public void testTwoRelatedProvidedResourcesOfSameType() throws TemplateModelException {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		ResourceEntity ws2 = builder.buildResource(builder.typeFor(WS.type), "ws2");
		builder.buildProvidedRelation(builder.app, builder.ws, ForeignableOwner.AMW);
		builder.buildProvidedRelation(builder.app, ws2, ForeignableOwner.AMW);

		AppServerRelationProperties properties = new AppServerRelationProperties(builder.context, builder.app, templateExceptionHandler);
		for (ProvidedResourceRelationEntity relation : builder.app.getProvidedMasterRelations()) {
			ResourceEntity slaveResource = relation.getSlaveResource();
			properties.addProvidedRelation(slaveResource.getName(), slaveResource, relation);
		}

		assertEquals(2, properties.getProvided().size());
		AmwResourceTemplateModel hash = properties.transformModel();

        assertEquals("app", hash.get("label").toString());
		assertNull(hash.get("propertyTypes"));

        assertEquals(TestUtils.asHashModel(hash, "providedResTypes", "Webservice", "ws").get("label").toString(), "ws");
        assertNull(TestUtils.asHashModel(hash, "providedResTypes", "Webservice", "ws").get("propertyTypes"));

        assertEquals(TestUtils.asHashModel(hash, "providedResTypes", "Webservice", "ws2").get("label").toString(), "ws2");
        assertNull(TestUtils.asHashModel(hash, "providedResTypes", "Webservice", "ws2").get("propertyTypes"));

		hash = properties.transformModel();
		assertTrue(templateExceptionHandler.isSuccess());

	}

}
