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

package ch.puzzle.itc.mobiliar.business.generator.control.extracted.templates;

import static ch.puzzle.itc.mobiliar.business.domain.TestUtils.unitFor;
import static ch.puzzle.itc.mobiliar.test.EntityBuilderType.AD;
import static ch.puzzle.itc.mobiliar.test.EntityBuilderType.AS;
import static ch.puzzle.itc.mobiliar.test.EntityBuilderType.DB2;
import static ch.puzzle.itc.mobiliar.test.EntityBuilderType.ZUSER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.generator.control.AMWTemplateExceptionHandler;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.GenerationContext;
import ch.puzzle.itc.mobiliar.business.property.entity.AmwResourceTemplateModel;
import ch.puzzle.itc.mobiliar.business.property.entity.FreeMarkerProperty;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.test.EntityBuilderType;

public class AppServerRelationsTemplateProcessorTest extends AmwTemplateProcessorTest {

	
	AmwResourceTemplateModel nodeProperties = new AmwResourceTemplateModel();
	
	@Override
	@Before
	public void before() throws Exception {
		super.before();
		this.context = builder.context;

	}

	@Test
	public void testGenerate() throws IOException {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		ResourceEntity as = builder.resourceFor(AS);
		builder.buildResourceTemplate(as, "bar", "content", "");
		builder.buildResourceTemplate(as, "foo", "content", "");

		generate(templateExceptionHandler);
		assertEquals(2, getGeneratedTemplateSize());
		assertTrue(templateExceptionHandler.isSuccess());
	}

	@Test
	public void testTemplatesForTypes() throws IOException {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		ResourceTypeEntity adType = builder.typeFor(AD.type);
		builder.buildResourceTypeTemplate(adType, "name", "path", "content");
		generate(templateExceptionHandler);

		GenerationUnit adUnit = unitFor(work.getAsSet(), AD);
		assertEquals(1, adUnit.getTemplates().size());
		assertEquals(1, getGeneratedTemplateSize());
		assertTrue(templateExceptionHandler.isSuccess());
	}

	@Test
	public void testTemplatesNestedOnResource() throws IOException {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		ConsumedResourceRelationEntity relation = builder.buildConsumedRelation(builder.resourceFor(DB2), builder.resourceFor(ZUSER), ForeignableOwner.AMW);

		assertNotNull(relation);
		ResourceTypeEntity type = builder.typeFor(ZUSER.type);
		builder.buildResourceTypeTemplate(type, "name", "path", "content");
		generate(templateExceptionHandler);

		assertEquals(16, work.getAsSet().size());
		GenerationUnit zUnit = unitFor(work.getAsSet(), ZUSER);
		assertEquals(1, zUnit.getTemplates().size());
		assertEquals(1, getGeneratedTemplateSize());
		assertTrue(templateExceptionHandler.isSuccess());
	}

	@Test
	public void testWritingFiles() throws IOException {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		ResourceEntity as = builder.resourceFor(AS);
		builder.buildResourceTemplate(as, "bar", "content", "aPath");

		generate(templateExceptionHandler);

		File generatedFile = files.get(0);
		assertEquals("aPath", generatedFile.getName());
		assertEquals("content", FileUtils.readFileToString(generatedFile, StandardCharsets.UTF_8));
		assertTrue(templateExceptionHandler.isSuccess());
	}

	@Test
	public void testNestedResources() throws IOException {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		ResourceEntity app = builder.resourceFor(EntityBuilderType.APP);
		ResourceEntity ws = builder.resourceFor(EntityBuilderType.WS);
		ResourceEntity lb = builder.resourceFor(EntityBuilderType.LB);

		builder.buildConsumedRelation(app, ws, ForeignableOwner.AMW);
		builder.buildConsumedRelation(ws, lb, ForeignableOwner.AMW);

		builder.buildResourceTemplate(ws, "ws", "${loadbalancer.name}", "aPath");

		generate(templateExceptionHandler);

		assertEquals(1, files.size());
		assertEquals(lb.getName(), FileUtils.readFileToString(files.get(0), StandardCharsets.UTF_8));
		assertTrue(templateExceptionHandler.isSuccess());
	}

    @Test
    public void test_Constant_NODE_ACTIVE() {
        assertEquals("active",AppServerRelationsTemplateProcessor.NODE_ACTIVE);
    }
	
	@Test
	public void test_isNodeEnabled_noProperties() throws IOException {
        // given
		AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));

        // when then
		assertFalse(prozessor.isNodeEnabled());
	}
	
	@Test
	public void test_isNodeEnabled_emptyProperties() {
        // given
		AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));
		prozessor.nodeProperties = nodeProperties;

        // when then
		assertFalse(prozessor.isNodeEnabled());
	}
	
	@Test
	public void test_isNodeEnabled_false() {
        // given
		AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));
		Map<String, FreeMarkerProperty> properties = new HashMap<>();
		properties.put(AppServerRelationsTemplateProcessor.NODE_ACTIVE, new FreeMarkerProperty("false", AppServerRelationsTemplateProcessor.NODE_ACTIVE));
		nodeProperties.setProperties(properties);
		prozessor.nodeProperties = nodeProperties;

        // when then
		assertFalse(prozessor.isNodeEnabled());
	}
	
	@Test
	public void test_isNodeEnabled_true() {
        // given
		AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));
		Map<String, FreeMarkerProperty> properties = new HashMap<>();
		properties.put(AppServerRelationsTemplateProcessor.NODE_ACTIVE, new FreeMarkerProperty("true", AppServerRelationsTemplateProcessor.NODE_ACTIVE));
		nodeProperties.setProperties(properties);
		prozessor.nodeProperties = nodeProperties;

        // when then
		assertTrue(prozessor.isNodeEnabled());
	}
	
	@Test
	public void test_Constant_HOST_NAME() {
		assertEquals("hostName",AppServerRelationsTemplateProcessor.HOST_NAME);
	}
	
	@Test
	public void test_hostName_is_null_no_properties() {
		AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));
		
		assertNull(prozessor.getHostname());
	}
	
	@Test
	public void test_hostName_is_null_Null_property() {
		AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));
		
		Map<String, FreeMarkerProperty> properties = new HashMap<>();
		properties.put(AppServerRelationsTemplateProcessor.HOST_NAME, null);
		nodeProperties.setProperties(properties);
		prozessor.nodeProperties = nodeProperties;	
		
		assertNull(prozessor.getHostname());
	}
	
	
	@Test
	public void test_hostName_is_set() {
		AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));
		
		Map<String, FreeMarkerProperty> properties = new HashMap<>();
		properties.put(AppServerRelationsTemplateProcessor.HOST_NAME, new FreeMarkerProperty("hostName", AppServerRelationsTemplateProcessor.HOST_NAME));
		nodeProperties.setProperties(properties);
		prozessor.nodeProperties = nodeProperties;
		
		assertEquals("hostName", prozessor.getHostname());
	}

    @Test
    public void test_setNodeEnabledForTestGeneration_propertyExists(){
        // given
        AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));
        Map<String, FreeMarkerProperty> properties = new HashMap<>();
        properties.put(AppServerRelationsTemplateProcessor.NODE_ACTIVE, new FreeMarkerProperty("false", AppServerRelationsTemplateProcessor.NODE_ACTIVE));
        nodeProperties.setProperties(properties);
        prozessor.nodeProperties = nodeProperties;

        // when
        prozessor.setNodeEnabledForTestGeneration();

        // then
        assertTrue(Boolean.valueOf(((FreeMarkerProperty)properties.get(AppServerRelationsTemplateProcessor.NODE_ACTIVE)).getCurrentValue()));
    }

    @Test
    public void test_setNodeEnabledForTestGeneration_propertyNotExists(){
        // given
        AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));
        Map<String, FreeMarkerProperty> properties = new HashMap<>();
        nodeProperties.setProperties(properties);
        prozessor.nodeProperties = nodeProperties;
        // when
        prozessor.setNodeEnabledForTestGeneration();

        // then
        assertNotNull(properties.get(AppServerRelationsTemplateProcessor.NODE_ACTIVE));
        assertTrue(Boolean.valueOf(((FreeMarkerProperty) properties.get(AppServerRelationsTemplateProcessor.NODE_ACTIVE)).getCurrentValue()));
    }

    @Test
    public void test_setNodeEnabledForTestGeneration_noProperties(){
        // given
        AppServerRelationsTemplateProcessor prozessor = new AppServerRelationsTemplateProcessor(log, new GenerationContext(null, null, null, null, null, null));

        // when
        prozessor.setNodeEnabledForTestGeneration();

        // then
        assertNotNull(prozessor.nodeProperties.getProperty(AppServerRelationsTemplateProcessor.NODE_ACTIVE));
        assertTrue(Boolean.valueOf(((FreeMarkerProperty)prozessor.nodeProperties.getProperty(AppServerRelationsTemplateProcessor.NODE_ACTIVE)).getCurrentValue()));
    }

}
