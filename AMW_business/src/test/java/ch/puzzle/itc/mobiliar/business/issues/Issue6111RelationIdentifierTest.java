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

package ch.puzzle.itc.mobiliar.business.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import ch.puzzle.itc.mobiliar.business.domain.TestUtils;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.generator.control.AMWTemplateExceptionHandler;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.templates.GenerationUnit;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.templates.TemplateProcessorBaseTest;
import ch.puzzle.itc.mobiliar.business.property.entity.AmwResourceTemplateModel;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.test.EntityBuilderType;
import ch.puzzle.itc.mobiliar.test.PersistingEntityBuilder;
import ch.puzzle.itc.mobiliar.test.testrunner.PersistenceTestRunner;

@RunWith(PersistenceTestRunner.class)
public class Issue6111RelationIdentifierTest extends TemplateProcessorBaseTest<PersistingEntityBuilder> {

	@Spy
	@PersistenceContext
	EntityManager entityManager;
	
	private ResourceEntity app;

	private ResourceEntity adIntern;
	private ResourceEntity adExtern;

	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
		builder = new PersistingEntityBuilder(entityManager).buildSimple();

		context = builder.context;

		ResourceTypeEntity adType = builder.buildResourceType(EntityBuilderType.AD.type);
		adIntern = builder.buildResource(adType, EntityBuilderType.AD.name);
		adExtern = builder.buildResource(adType, "adExtern");

		app = builder.app;

		ConsumedResourceRelationEntity adInternRelation = builder.buildConsumedRelation(app, adIntern, ForeignableOwner.AMW);
		adInternRelation.setIdentifier("");
		ConsumedResourceRelationEntity adintern_1Relation = builder.buildConsumedRelation(app, adIntern, ForeignableOwner.AMW);
		adintern_1Relation.setIdentifier("1");

		ConsumedResourceRelationEntity adExternRelation = builder.buildConsumedRelation(app, adExtern, ForeignableOwner.AMW);
		adExternRelation.setIdentifier("");
		ConsumedResourceRelationEntity adExtern_1Relation = builder.buildConsumedRelation(app, adExtern, ForeignableOwner.AMW);
		adExtern_1Relation.setIdentifier("1");
	}

	@Test
	public void testConsumeMultipleResourcesWithDifferentIdentifier() throws Exception {
		AMWTemplateExceptionHandler templateExceptionHandler = new AMWTemplateExceptionHandler();
		prepareWorkUnits(templateExceptionHandler);

		GenerationUnit appUnit = TestUtils.unitFor(work.getAsSet(), builder.app);
		AmwResourceTemplateModel model = appUnit.getAppServerRelationProperties().transformModel();

		assertEquals("adIntern", TestUtils.asHashModel(model, "consumedResTypes", "ActiveDirectory", "adIntern_1").get("name").toString());
		assertEquals("adIntern", TestUtils.asHashModel(model, "consumedResTypes", "ActiveDirectory", "adIntern").get("name").toString());
		assertEquals("adIntern", TestUtils.asHashModel(model, "adIntern_1").get("name").toString());
		assertEquals("adIntern", TestUtils.asHashModel(model, "adIntern").get("name").toString());

		assertEquals("adExtern", TestUtils.asHashModel(model, "consumedResTypes", "ActiveDirectory", "adExtern_1").get("name").toString());
		assertEquals("adExtern", TestUtils.asHashModel(model, "consumedResTypes", "ActiveDirectory", "adExtern").get("name").toString());
		assertEquals("adExtern", TestUtils.asHashModel(model, "adExtern_1").get("name").toString());
		assertEquals("adExtern", TestUtils.asHashModel(model, "adExtern").get("name").toString());

		assertTrue(templateExceptionHandler.isSuccess());
	}

}
