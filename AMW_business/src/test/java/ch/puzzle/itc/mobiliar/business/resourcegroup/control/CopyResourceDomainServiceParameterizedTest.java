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

import ch.puzzle.itc.mobiliar.builders.*;
import ch.puzzle.itc.mobiliar.business.auditview.control.AuditService;
import ch.puzzle.itc.mobiliar.business.domain.commons.CommonDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextDependency;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.foreignable.control.ForeignableService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.function.entity.AmwFunctionEntity;
import ch.puzzle.itc.mobiliar.business.function.entity.AmwFunctionEntityBuilder;
import ch.puzzle.itc.mobiliar.business.property.entity.*;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService.CopyMode;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.AbstractResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationContextEntity;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.control.SoftlinkRelationService;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.entity.SoftlinkRelationEntity;
import ch.puzzle.itc.mobiliar.business.template.control.TemplatesScreenDomainService;
import ch.puzzle.itc.mobiliar.business.template.entity.TemplateDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.utils.CopyHelper;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests {@link ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService}
 * 
 * @author cweber
 */
@RunWith(Parameterized.class)
public class CopyResourceDomainServiceParameterizedTest {

	@Mock
	private CommonDomainService commonDomainService;
	@Mock
	private TemplatesScreenDomainService templatesScreenService;
	@Mock
	private ResourceTypeProvider resourceTypeProvider;
	@Mock
	private EntityManager entityManager;
	@Mock
	private ForeignableService foreignableServiceMock;
	@Mock
	private SoftlinkRelationService softlinkRelationServiceMock;
	@Mock
	private ResourceLocator resourceLocatorMock;
	@Mock
	private AuditService auditService;

	@InjectMocks
	private CopyResourceDomainService copyDomainService = new CopyResourceDomainService();

	private ResourceTypeEntityBuilder resourceTypeEntityBuilder = new ResourceTypeEntityBuilder();
	private ResourceRelationEntityBuilder relationEntityBuilder = new ResourceRelationEntityBuilder();
	private TemplateDescriptorEntityBuilder templateDescriptorEntityBuilder = new TemplateDescriptorEntityBuilder();
	private TargetPlatformEntityBuilder targetPlatformEntityBuilder = new TargetPlatformEntityBuilder();
	private ResourceRelationContextEntityBuilder resRelContextBuilder = new ResourceRelationContextEntityBuilder();
	private ContextEntityBuilder contextEntityBuilder = new ContextEntityBuilder();
	private PropertyDescriptorEntityBuilder propDescBuilder = new PropertyDescriptorEntityBuilder();
	private PropertyEntityBuilder propBuilder = new PropertyEntityBuilder();
	private ReleaseEntityBuilder releaseEntityBuilder = new ReleaseEntityBuilder();

	private ResourceEntity originResource;
	private ResourceEntity targetResource;

	ContextEntity globalContextMock;

	private CopyMode copyMode;
	private ForeignableOwner actingOwner;

	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);

		globalContextMock = contextEntityBuilder.mockContextEntity("GLOBAL", null, null);

		ResourceTypeEntity appType = resourceTypeEntityBuilder.mockApplicationResourceTypeEntity(null);
		when(resourceTypeProvider.getOrCreateDefaultResourceType(DefaultResourceTypeDefinition.APPLICATION)).thenReturn(appType);

		originResource = new ResourceEntityBuilder().mockAppServerEntity("originResource", null, null,
				targetPlatformEntityBuilder.mockTargetPlatformEntity("EAP 6"));
		when(originResource.isDeletable()).thenReturn(true);
		Set<AmwFunctionEntity> originFunctions = new HashSet<>();
		originFunctions.add(new AmwFunctionEntityBuilder("origFct1", 1).forResource(originResource).build());
		originFunctions.add(new AmwFunctionEntityBuilder("origFct2", 2).forResource(originResource).build());
		when(originResource.getFunctions()).thenReturn(originFunctions);

		targetResource = new ResourceEntityBuilder().buildAppServerEntity("targetResource", null, null,
				true);
	}

	public CopyResourceDomainServiceParameterizedTest(CopyMode copyMode, ForeignableOwner actingOwner) {
		this.copyMode = copyMode;
		this.actingOwner = actingOwner;
	}

	@Parameterized.Parameters(name = "{index}: copyMode({0}), actingOwner({1})")
	public static Collection<Object[]> data() {
		return Arrays.asList(CopyHelper.VALID_MODE_OWNER_COMBINATIONS);
	}

	@Test
	public void copyResourceEntity_consumedRelations() throws ForeignableOwnerViolationException, AMWException {
		// given
		TemplateDescriptorEntity origTemp = templateDescriptorEntityBuilder.withName("bla").withTargetPath("/bla").withFileContent("foo").build();
		ResourceRelationContextEntity resRelContex = resRelContextBuilder.mockResourceRelationContextEntity(globalContextMock);
		when(resRelContex.getTemplates()).thenReturn(Collections.singleton(origTemp));

		ResourceEntity appOrigin = new ResourceEntityBuilder().withName("appOrigin").withTypeOfName("APPLICATION").mock();
		ResourceEntity appTarget = new ResourceEntityBuilder().withName("appTarget").withTypeOfName("APPLICATION").mock();
		ResourceEntity wsOrigin = new ResourceEntityBuilder().mockResourceEntity("wsOrigin", null,
				"webservice", null);
		ResourceEntity wsTarget = new ResourceEntityBuilder().mockResourceEntity("wsTarget", null,
				"webservice", null);

		ConsumedResourceRelationEntity originRel = relationEntityBuilder.buildConsumedResRelEntity(appOrigin, wsOrigin, wsOrigin.getName(), 1);
		originRel.setContexts(Collections.singleton(resRelContex));
		CopyUnit copyAppUnit = new CopyUnit(appOrigin, appTarget, copyMode, actingOwner);
		CopyUnit copyWsUnit = new CopyUnit(wsOrigin, wsTarget, copyMode, actingOwner);

		// when
		copyDomainService.copyConsumedMasterRelations(copyAppUnit);
		copyDomainService.copyConsumedSlaveRelations(copyWsUnit);

		// then
		assertFalse(appTarget.getConsumedMasterRelations().isEmpty());
		assertFalse(appTarget.getConsumedMasterRelations().iterator().next().getContexts().isEmpty());
		assertFalse(appTarget.getConsumedMasterRelations().iterator().next().getContexts().iterator().next().getTemplates().isEmpty());
		if (copyMode == CopyMode.COPY) {
			assertTrue(wsTarget.getConsumedSlaveRelations().isEmpty());
			assertFalse(copyWsUnit.getResult().getSkippedConsumedRelations().isEmpty());
		} else if (copyMode == CopyMode.RELEASE) {
			assertFalse(wsTarget.getConsumedSlaveRelations().isEmpty());
			assertTrue(copyWsUnit.getResult().getSkippedConsumedRelations().isEmpty());
			assertFalse(wsTarget.getConsumedSlaveRelations().iterator().next().getContexts().isEmpty());
			assertFalse(wsTarget.getConsumedSlaveRelations().iterator().next().getContexts().iterator().next().getTemplates().isEmpty());
		}
	}

	@Test
	public void copyResourceEntity_providedRelations() throws ForeignableOwnerViolationException, AMWException {
		// given
		TemplateDescriptorEntity origTemp = templateDescriptorEntityBuilder.withName("bla").withTargetPath("/bla").withFileContent("foo").build();
		ResourceRelationContextEntity resRelContex = resRelContextBuilder.mockResourceRelationContextEntity(globalContextMock);
		when(resRelContex.getTemplates()).thenReturn(Collections.singleton(origTemp));

		ResourceEntity appOrigin = new ResourceEntityBuilder().withName("appOrigin").withTypeOfName("APPLICATION").build();
		ResourceEntity appTarget = new ResourceEntityBuilder().withName("appTarget").withTypeOfName("APPLICATION").build();
		ResourceEntity wsOrigin = new ResourceEntityBuilder().withName("wsOrigin").withTypeOfName("webservice").build();
		ResourceEntity wsTarget = new ResourceEntityBuilder().mockResourceEntity("wsTarget", null,
				"webservice", null);
		ProvidedResourceRelationEntity originRel = relationEntityBuilder.buildProvidedResRelEntity(appOrigin, wsOrigin, wsOrigin.getName(), 1);
		originRel.setContexts(Collections.singleton(resRelContex));

		CopyUnit copyAppUnit = new CopyUnit(appOrigin, appTarget, copyMode, actingOwner);
		CopyUnit copyWsUnit = new CopyUnit(wsOrigin, wsTarget, copyMode, actingOwner);

		// when
		copyDomainService.copyProvidedMasterRelations(copyAppUnit);
		copyDomainService.copyProvidedSlaveRelations(copyWsUnit);

		// then
		if (copyMode == CopyMode.COPY) {
			assertTrue(wsTarget.getProvidedSlaveRelations().isEmpty());
			assertFalse(copyWsUnit.getResult().getSkippedProvidedRelations().isEmpty());
		} else if (copyMode == CopyMode.RELEASE) {
			assertFalse(wsTarget.getProvidedSlaveRelations().isEmpty());
			assertFalse(appTarget.getProvidedMasterRelations().isEmpty());
			assertFalse(appTarget.getProvidedMasterRelations().iterator().next().getContexts().isEmpty());
			assertFalse(appTarget.getProvidedMasterRelations().iterator().next().getContexts().iterator().next().getTemplates().isEmpty());
			assertTrue(copyAppUnit.getResult().getSkippedProvidedRelations().isEmpty());
			assertFalse(wsTarget.getProvidedSlaveRelations().isEmpty());
			assertFalse(wsTarget.getProvidedSlaveRelations().iterator().next().getContexts().isEmpty());
			assertFalse(wsTarget.getProvidedSlaveRelations().iterator().next().getContexts().iterator().next().getTemplates().isEmpty());
		}
	}

	@Test
	public void copyContextDependency_new_created() throws ForeignableOwnerViolationException, AMWException {
		// given
		ContextEntity context = contextEntityBuilder.mockContextEntity("GLOBAL", null, null);
		ResourceRelationContextEntity origin = resRelContextBuilder.mockResourceRelationContextEntity(context);
		ResourceRelationContextEntity target = new ResourceRelationContextEntity();
		CopyUnit copyUnit = new CopyUnit(originResource, targetResource, CopyMode.COPY, ForeignableOwner.AMW);
		Map<String, PropertyDescriptorEntity> descriptorMap = copyDomainService.copyPropertyDescriptors(
				origin.getPropertyDescriptors(), target.getPropertyDescriptors(), target, copyUnit);

		// when
		ContextDependency<?> copy = copyDomainService.copyContextDependency(origin, target, copyUnit, descriptorMap);

		// then
		assertNotNull(copy);
		assertTrue(copyUnit.getResult().isSuccess());
		assertEquals(origin.getContext(), copy.getContext());
		assertThat(copy.getId(), Is.is(context.getId()));
	}

	@Test
	public void copyContextDependency_existing() throws ForeignableOwnerViolationException, AMWException {
		// given
		ContextEntity context = contextEntityBuilder.mockContextEntity("GLOBAL", null, null);
		ResourceRelationContextEntity origin = resRelContextBuilder.mockResourceRelationContextEntity(context);
		ResourceRelationContextEntity target = resRelContextBuilder.buildResourceRelationContextEntity(context);
		CopyUnit copyUnit = new CopyUnit(originResource, targetResource, CopyMode.COPY, ForeignableOwner.AMW);
		Map<String, PropertyDescriptorEntity> descriptorMap = copyDomainService.copyPropertyDescriptors(
				origin.getPropertyDescriptors(), target.getPropertyDescriptors(), target, copyUnit);

		// when
		ContextDependency<?> copy = copyDomainService.copyContextDependency(origin, target, copyUnit, descriptorMap);

		// then
		assertNotNull(copy);
		assertTrue(copyUnit.getResult().isSuccess());
		assertEquals(origin.getContext(), copy.getContext());
		assertEquals(target.getId(), copy.getId());
	}

	@Test
	public void copyTags() {
		// given
		String origName = "foo";
		String origComment = "origComment";
		String origDefaultValue = "origDefaultVal";
		String origExampleValue = "origExampleVal";
		String origMik = "origMik";
		String origDisplayName = "origDisplayName";
		String origValue = "origValue";
		PropertyTagEntity origTag = new PropertyTagEntity();
		origTag.setName("origTag");
		PropertyEntity origProp = propBuilder.buildPropertyEntity(origValue, null);
		PropertyTypeEntity origType = propDescBuilder.mockPropertyTypeEntity("type1");
		PropertyDescriptorEntity origin = propDescBuilder.withGeneratedId().withPropertyName(origName)
				.withProperties(Collections.singleton(origProp)).withPropertyComment(origComment)
				.withDefaultValue(origDefaultValue).withMik(origMik).withExampleValue(origExampleValue)
				.withPropertyType(origType).withDisplayName(origDisplayName).withTags(origTag).build();

		String targetComment = "targetComment";
		String targetDefaultValue = "targetDefaultValue";
		String targetExampleValue = "targetExampleValue";
		String targetMik = "targetMik";
		String targetDisplayName = "targetDisplayName";
		String targetValue = "targetValue";
		PropertyTagEntity targetTag = new PropertyTagEntity();
		targetTag.setName("targetTag");
		PropertyEntity targetProp = propBuilder.buildPropertyEntity(targetValue, null);
		PropertyTypeEntity targetType = propDescBuilder.mockPropertyTypeEntity("type2");
		PropertyDescriptorEntity target = propDescBuilder.withGeneratedId().withPropertyName(origName)
				.withProperties(Collections.singleton(targetProp)).withPropertyComment(targetComment)
				.withDefaultValue(targetDefaultValue).withMik(targetMik)
				.withExampleValue(targetExampleValue).withPropertyType(targetType)
				.withDisplayName(targetDisplayName).withTags(targetTag).build();

		// when
		copyDomainService.copyTags(origin, target);

		// then
		Set<String> allTagNames = origin.getPropertyTagsNameSet();
		if (target != null) {
			allTagNames.addAll(target.getPropertyTagsNameSet());
		}
		assertEquals(allTagNames, target.getPropertyTagsNameSet());
	}

	@Test
	public void should_copyTemplates() throws AMWException {
		// given
		Set<TemplateDescriptorEntity> targets = new HashSet<>();
		TemplateDescriptorEntity targetTemplate = new TemplateDescriptorEntityBuilder().withName(
				TemplateDescriptorEntityBuilder.NAME).build();
		targets.add(targetTemplate);

		Set<TemplateDescriptorEntity> origins = new HashSet<>();
		TemplateDescriptorEntity originTemplate = new TemplateDescriptorEntityBuilder()
				.withName(TemplateDescriptorEntityBuilder.NAME)
				.withFileContent(TemplateDescriptorEntityBuilder.FILE_CONTENT).mock();
		origins.add(originTemplate);
		CopyUnit copyUnit = new CopyUnit(originResource, targetResource, copyMode, actingOwner);

		// when
		Set<TemplateDescriptorEntity> copy = copyDomainService.copyTemplates(origins, targets, copyUnit);

		// then
		verify(originTemplate).getCopy(targetTemplate, copyUnit);
		assertTrue(copyUnit.getResult().isSuccess());
		assertNotNull(copy);
		assertEquals(1, copy.size());
	}

	@Test
	public void shouldCopyTemplatesIfTargetDoesNotExist() throws AMWException {
		// given
		Set<TemplateDescriptorEntity> origins = new HashSet<>();
		TemplateDescriptorEntity originTemplate = new TemplateDescriptorEntityBuilder()
				.withName(TemplateDescriptorEntityBuilder.NAME)
				.withFileContent(TemplateDescriptorEntityBuilder.FILE_CONTENT)
				.withTargetPath(TemplateDescriptorEntityBuilder.TARGET_PATH).mock();
		origins.add(originTemplate);
		CopyUnit copyUnit = new CopyUnit(originResource, targetResource, copyMode, actingOwner);

		// when
		Set<TemplateDescriptorEntity> copy = copyDomainService.copyTemplates(origins, null, copyUnit);

		// then
		verify(originTemplate).getCopy(null, copyUnit);
		assertTrue(copyUnit.getResult().isSuccess());
		assertNotNull(copy);
		assertEquals(1, copy.size());
	}

	private PropertyDescriptorEntity createPropDesc(Integer id, String name, String value, ForeignableOwner owner) {
		Set<PropertyEntity> props = new HashSet<>();
		if (value != null) {
			PropertyEntity property = propBuilder.buildPropertyEntity(value,null);
			props.add(property);
		}
		return new PropertyDescriptorEntityBuilder().withId(id).withPropertyName(name).withProperties(props).withOwner(owner).build();
	}

	/**
	 * @throws ForeignableOwnerViolationException
	 * @throws AMWException
	 */
	@Test
	public void copyProperties() throws ForeignableOwnerViolationException, AMWException {
		for (ForeignableOwner propDescOwner : ForeignableOwner.values()) {
			shouldNotOverwriteTargetProps(new CopyUnit(originResource, targetResource, copyMode, actingOwner), propDescOwner);
			shouldSetTargetPropsFromOriginIfTargetDoesNotExist(new CopyUnit(originResource, targetResource, copyMode, actingOwner), propDescOwner);
			shouldCopyPropertyDescriptorIfNotExists(new CopyUnit(originResource, targetResource, copyMode, actingOwner), propDescOwner);
		}
	}

	/**
	 * If a propertyDescriptor of the targetResources has already a propertyValue, this value should not be
	 * overwritten.
	 *
	 * @param propDescOwner
	 * @throws ForeignableOwnerViolationException
	 */
	private void shouldNotOverwriteTargetProps(CopyUnit copyUnit, ForeignableOwner propDescOwner) throws ForeignableOwnerViolationException {
		// given
		ContextEntity context = contextEntityBuilder.mockContextEntity("GLOBAL", null, null);

		ResourceRelationContextEntity origin = resRelContextBuilder
				.mockResourceRelationContextEntity(context);
		ResourceRelationContextEntity target = resRelContextBuilder
				.buildResourceRelationContextEntity(context);

		String descName = "descriptorA";
		String origPropValue = "origPropValue";
		String targetPropValue = "targetPropValue";

		// Original descriptor with property value
		PropertyDescriptorEntity originPropDesc = createPropDesc(1, descName, origPropValue, propDescOwner);
		when(origin.getPropertyDescriptors()).thenReturn(Collections.singleton(originPropDesc));
		when(origin.getProperties()).thenReturn(
				Collections.singleton(originPropDesc.getProperties().iterator().next()));

		// Target descriptor with property value
		PropertyDescriptorEntity targetPropDesc = createPropDesc(10, descName, targetPropValue, propDescOwner);
		target.addPropertyDescriptor(targetPropDesc);

		// when
		Map<String, PropertyDescriptorEntity> descriptorMap = copyDomainService.copyPropertyDescriptors(
				origin.getPropertyDescriptors(), target.getPropertyDescriptors(), target, copyUnit);
		ContextDependency<?> copy = copyDomainService.copyContextDependency(origin, target, copyUnit, descriptorMap);

		// then
		assertNotNull(copy);
		assertTrue(copyUnit.getResult().isSuccess());
		assertEquals(1, copy.getPropertyDescriptors().size());
		assertNotNull(copy);
		assertTrue(copyUnit.getResult().isSuccess());
		assertEquals(1, copy.getPropertyDescriptors().size());
		PropertyDescriptorEntity desc = copy.getPropertyDescriptors().iterator().next();
		assertNotNull(desc);
		assertEquals(1, copy.getProperties().size());
		PropertyEntity prop = copy.getProperties().iterator().next();
		assertNotNull(prop);
		assertEquals(origPropValue, prop.getValue());
		assertEquals(targetPropDesc, prop.getDescriptor());
	}

	/**
	 * If the target PropertyDescriptor does not have a propertyValue, the value from origin should be set.
	 * 
	 * @param copyUnit
	 * @param propDescOwner
	 * @throws ForeignableOwnerViolationException
	 */
	private void shouldSetTargetPropsFromOriginIfTargetDoesNotExist(CopyUnit copyUnit, ForeignableOwner propDescOwner) throws ForeignableOwnerViolationException {
		// given
		ContextEntity context = contextEntityBuilder.mockContextEntity("GLOBAL", null, null);

		ResourceRelationContextEntity origin = resRelContextBuilder.mockResourceRelationContextEntity(context);
		ResourceRelationContextEntity target = resRelContextBuilder.buildResourceRelationContextEntity(context);

		String descName = "descriptorA";
		String origPropValue = "origPropValue";

		// Original descriptor with property value
		PropertyDescriptorEntity originPropDesc = createPropDesc(1, descName, origPropValue, propDescOwner);
		when(origin.getPropertyDescriptors()).thenReturn(Collections.singleton(originPropDesc));
		when(origin.getProperties()).thenReturn(Collections.singleton(originPropDesc.getProperties().iterator().next()));

		// Target descriptor without property value
		PropertyDescriptorEntity targetPropDesc = createPropDesc(10, descName, null, propDescOwner);
		target.addPropertyDescriptor(targetPropDesc);

		// when
		Map<String, PropertyDescriptorEntity> descriptorMap = copyDomainService.copyPropertyDescriptors(
				origin.getPropertyDescriptors(), target.getPropertyDescriptors(), target, copyUnit);
		ContextDependency<?> copy = copyDomainService.copyContextDependency(origin, target, copyUnit, descriptorMap);

		// then
		assertNotNull(copy);
		assertTrue(copyUnit.getResult().isSuccess());
		assertEquals(1, copy.getPropertyDescriptors().size());
		PropertyDescriptorEntity desc = copy.getPropertyDescriptors().iterator().next();
		assertNotNull(desc);
		assertEquals(1, copy.getProperties().size());
		PropertyEntity prop = copy.getProperties().iterator().next();
		assertNotNull(prop);
		assertEquals(origPropValue, prop.getValue());
		assertEquals(targetPropDesc, prop.getDescriptor());
	}


	private void shouldMergePropertyValuesIfTargetPropertyExists(CopyUnit copyUnit, ForeignableOwner propDescOwner) throws ForeignableOwnerViolationException {

		// given
		ContextEntity context = contextEntityBuilder.mockContextEntity("GLOBAL", null, null);

		ResourceRelationContextEntity origin = resRelContextBuilder.mockResourceRelationContextEntity(context);
		ResourceRelationContextEntity target = resRelContextBuilder.buildResourceRelationContextEntity(context);

		String descName = "descriptorA";
		String origPropValue = "origPropValue";

		// Original descriptor with property value
		PropertyDescriptorEntity originPropDesc = createPropDesc(1, descName, origPropValue, propDescOwner);
		when(origin.getPropertyDescriptors()).thenReturn(Collections.singleton(originPropDesc));
		when(origin.getProperties()).thenReturn(Collections.singleton(originPropDesc.getProperties().iterator().next()));


		// Target descriptor without property value
		PropertyDescriptorEntity targetPropDesc = createPropDesc(10, descName, null, propDescOwner);
		target.addPropertyDescriptor(targetPropDesc);

		// add a property to the target context

		PropertyEntity targetProperty = new PropertyEntityBuilder().buildPropertyEntity("my-value", origin.getPropertyDescriptors().iterator().next());
		targetProperty.setDescriptor(targetPropDesc);
		target.addProperty(targetProperty);


		// when
		Map<String, PropertyDescriptorEntity> descriptorMap = copyDomainService.copyPropertyDescriptors(
				origin.getPropertyDescriptors(), target.getPropertyDescriptors(), target, copyUnit);
		ContextDependency<?> copy = copyDomainService.copyContextDependency(origin, target, copyUnit, descriptorMap);

		// then
		assertNotNull(copy);
		assertTrue(copyUnit.getResult().isSuccess());
		assertEquals(1, copy.getPropertyDescriptors().size());
		PropertyDescriptorEntity desc = copy.getPropertyDescriptors().iterator().next();
		assertNotNull(desc);
		assertEquals(1, copy.getProperties().size());
		PropertyEntity prop = copy.getProperties().iterator().next();
		assertNotNull(prop);
		assertEquals(origPropValue, prop.getValue());
		assertEquals(targetPropDesc, prop.getDescriptor());
	}

	/**
	 * If a propertyDescriptor from origin does not yet exist on targetResource the descriptor and the
	 * propertyValue should be copied from origin. EXCEPT for {@link CopyMode#MAIA_PREDECESSOR}; if a
	 * propertyDescriptor AMW does not exist in target then it should not be copied!
	 *
	 * @param copyUnit
	 * @param propDescOwner
	 * @throws ForeignableOwnerViolationException
	 */
	private void shouldCopyPropertyDescriptorIfNotExists(CopyUnit copyUnit, ForeignableOwner propDescOwner) throws ForeignableOwnerViolationException {
		// given
		ContextEntity context = contextEntityBuilder.mockContextEntity("GLOBAL", null, null);

		ResourceRelationContextEntity origin = resRelContextBuilder.mockResourceRelationContextEntity(context);
		ResourceRelationContextEntity target = resRelContextBuilder.buildResourceRelationContextEntity(context);

		String descName = "descriptorA";
		String origPropValue = "origPropValue";

		// Original descriptor with property value
		PropertyDescriptorEntity originPropDesc = createPropDesc(1, descName, origPropValue, propDescOwner);
		when(origin.getPropertyDescriptors()).thenReturn(Collections.singleton(originPropDesc));
		when(origin.getProperties()).thenReturn(Collections.singleton(originPropDesc.getProperties().iterator().next()));

		// when
		Map<String, PropertyDescriptorEntity> descriptorMap = copyDomainService.copyPropertyDescriptors(
				origin.getPropertyDescriptors(), target.getPropertyDescriptors(), target, copyUnit);
		ContextDependency<?> copy = copyDomainService.copyContextDependency(origin, target, copyUnit, descriptorMap);

		// then
		assertNotNull(copy);
		assertTrue(copyUnit.getResult().isSuccess());
		if (copyUnit.getMode() == CopyMode.MAIA_PREDECESSOR && propDescOwner == ForeignableOwner.MAIA) {
			assertNull(copy.getPropertyDescriptors());
			assertNull(copy.getProperties());
		}
		else {
			assertEquals(1, copy.getPropertyDescriptors().size());
			PropertyDescriptorEntity desc = copy.getPropertyDescriptors().iterator().next();
			assertNotNull(desc);
			assertEquals(descName, desc.getPropertyName());
			assertEquals(1, copy.getProperties().size());
			PropertyEntity prop = copy.getProperties().iterator().next();
			assertNotNull(prop);
			assertEquals(origPropValue, prop.getValue());
			assertEquals(desc, prop.getDescriptor());
		}
	}

	@Test
	public void shouldCopyFunctions() throws AMWException {
		// given
		AmwFunctionEntity originParent = new AmwFunctionEntityBuilder("orig1", 3)
				.withImplementation("fooBar")
				.with(new MikEntity("mik1", null), new MikEntity("mik2", null))
				.forResourceType(originResource.getResourceType()).mock();
		AmwFunctionEntity origin = new AmwFunctionEntityBuilder("orig2", 4).withImplementation("foo")
				.with(new MikEntity("mik1", null), new MikEntity("mik2", null))
				.forResource(originResource).withOverwrittenParent(originParent).mock();

		Set<AmwFunctionEntity> originFunctions = new HashSet<>(originResource.getFunctions());
		originFunctions.add(origin);
		when(originResource.getFunctions()).thenReturn(originFunctions);

		AmwFunctionEntity target = new AmwFunctionEntityBuilder("target1", 10).withImplementation("blabla")
				.with(new MikEntity("mik10", null), new MikEntity("mik20", null))
				.forResource(targetResource).build();
		targetResource.addFunction(target);
		int targetResourceFctSizeBeforeCopy = targetResource.getFunctions().size();

		CopyUnit copyUnit = new CopyUnit(originResource, targetResource, copyMode, actingOwner);

		// when
		copyDomainService.copyFunctions(copyUnit);

		// then
		verify(origin).getCopy(null, copyUnit);
		verify(origin, never()).getCopy(target, copyUnit);
		assertEquals(originResource.getFunctions().size() + targetResourceFctSizeBeforeCopy, targetResource.getFunctions().size());
	}

	@Test(expected = RuntimeException.class)
	public void copyFromOriginToTargetResourceWhenTragetNullShouldThrowException() throws ForeignableOwnerViolationException, AMWException {
		// given
		ResourceEntity origin = new ResourceEntityBuilder().withId(1).build();
		ResourceEntity target = null;

		// when
		copyDomainService.copyFromOriginToTargetResource(origin, target, ForeignableOwner.AMW);
	}

	@Test
	public void doCopyResourceAndSaveWhenTargetResourceHasNoIdVerifyForeignable() throws ForeignableOwnerViolationException, AMWException {
		// given
		ForeignableOwner copyingOwner = ForeignableOwner.AMW;
		ResourceEntity origin = new ResourceEntityBuilder().withId(1).withOwner(ForeignableOwner.AMW).build();
		ResourceEntity target = new ResourceEntityBuilder().withId(null).withOwner(ForeignableOwner.MAIA).build();
		CopyUnit copyUnit = new CopyUnit(origin, target, CopyMode.COPY, copyingOwner);
		int targetHashCodeBeforeChange = target.foreignableFieldHashCode();

		// when
		copyDomainService.doCopyResourceAndSave(copyUnit);

		// then
		verify(foreignableServiceMock).verifyEditableByOwner(copyUnit.getActingOwner(), targetHashCodeBeforeChange, target);
	}

	@Test
	public void doCopyResourceAndSaveWhenTargetResourceHasIdVerifyForeignable() throws ForeignableOwnerViolationException, AMWException {
		// given
		ForeignableOwner copyingOwner = ForeignableOwner.AMW;
		ResourceEntity origin = new ResourceEntityBuilder().withId(1)
				.withName("originalUnchangedResourceLoadedFromDb").withOwner(ForeignableOwner.AMW).build();
		ResourceEntity target = new ResourceEntityBuilder().withId(2).withOwner(ForeignableOwner.MAIA).build();

		int originalTargetFromDb = target.foreignableFieldHashCode();

		CopyUnit copyUnit = new CopyUnit(origin, target, CopyMode.COPY, copyingOwner);

		// when
		copyDomainService.doCopyResourceAndSave(copyUnit);

		// then
		verify(foreignableServiceMock).verifyEditableByOwner(copyUnit.getActingOwner(), originalTargetFromDb, target);
		verify(auditService).storeIdInThreadLocalForAuditLog(target);
	}

	@Test
	public void createReleaseFromOriginResourceWhenTargetExistOnDbShouldCopyToThisTarget() throws ForeignableOwnerViolationException, AMWException {
		// given
		ReleaseEntity release = releaseEntityBuilder.mockReleaseEntity("Past", new Date());
		ResourceEntity origin = new ResourceEntityBuilder().withId(1).build();
		ResourceEntity target = new ResourceEntityBuilder().withId(2).withOwner(ForeignableOwner.MAIA).build();

		when(commonDomainService.getResourceEntityByGroupAndRelease(origin.getResourceGroup().getId(), release.getId())).thenReturn(target);

		// when
		copyDomainService.createReleaseFromOriginResource(origin, release, ForeignableOwner.AMW);

		// then
		verify(entityManager).persist(target);
	}

	@Test
	public void createCopyFromOriginResourceWhenTargetExistOnDbShouldCopyToThisTarget()
			throws ForeignableOwnerViolationException, AMWException {
		// given
		ReleaseEntity release = releaseEntityBuilder.mockReleaseEntity("Past", new Date());
		ResourceEntity origin = new ResourceEntityBuilder().withId(1).build();
		ResourceEntity target = new ResourceEntityBuilder().withId(2).withOwner(ForeignableOwner.MAIA).build();

		when(commonDomainService.getResourceEntityByGroupAndRelease(origin.getResourceGroup().getId(), release.getId())).thenReturn(target);

		// when
		copyDomainService.copyFromOriginToTargetResource(origin, target, ForeignableOwner.AMW);

		// then
		verify(entityManager).persist(target);
	}

	@Test
	public void createReleaseFromOriginResourceWhenTargetNotExistOnDbShouldCreateNewTargetWithCopyingOwner()
			throws ForeignableOwnerViolationException, AMWException {
		// given
		ReleaseEntity release = releaseEntityBuilder.mockReleaseEntity("Past", new Date());
		ResourceEntity origin = new ResourceEntityBuilder().withId(1).build();
		ForeignableOwner copyingOwner = ForeignableOwner.AMW;

		when(commonDomainService.getResourceEntityByGroupAndRelease(origin.getResourceGroup().getId(), release.getId())).thenReturn(null);

		// when
		copyDomainService.createReleaseFromOriginResource(origin, release, copyingOwner);

		// then
		ArgumentCaptor<ResourceEntity> argCapt = ArgumentCaptor.forClass(ResourceEntity.class);
		verify(entityManager).persist(argCapt.capture());

		assertEquals(copyingOwner, argCapt.getValue().getOwner());
	}

	@Test
	public void test_copySoftlinkRelation() throws ForeignableOwnerViolationException, AMWException {
		// given
		String origSoftlinkRef = "origSoftlinkRef";
		SoftlinkRelationEntity originSoftlink = new SoftlinkRelationEntity();
		originSoftlink.setOwner(ForeignableOwner.MAIA);
		originSoftlink.setSoftlinkRef(origSoftlinkRef);
		originSoftlink.setId(111);
		ResourceEntity origin = new ResourceEntityBuilder().withId(1).withOwner(ForeignableOwner.MAIA)
				.withSoftlinkRelation(originSoftlink).mock();

		String targetSoftlinkRef = "targetSoftlinkRef";
		SoftlinkRelationEntity targetSoftlink;
		ResourceEntity target;
		int targetHashCodeBeforeChange = 0;
		if (copyMode == CopyMode.COPY || copyMode == CopyMode.MAIA_PREDECESSOR) {
			targetSoftlink = new SoftlinkRelationEntity();
			targetSoftlink.setOwner(ForeignableOwner.MAIA);
			targetSoftlink.setSoftlinkRef(targetSoftlinkRef);
			targetSoftlink.setId(222);
			target = new ResourceEntityBuilder().withId(2).withOwner(ForeignableOwner.MAIA)
					.withSoftlinkRelation(targetSoftlink).mock();
			targetHashCodeBeforeChange = targetSoftlink.foreignableFieldHashCode();
		} else {
			target = new ResourceEntityBuilder().withId(2).withOwner(ForeignableOwner.MAIA).mock();
		}

		CopyUnit copyUnit = new CopyUnit(origin, target, copyMode, actingOwner);

		// when
		SoftlinkRelationEntity result = copyDomainService.copySoftlinkRelation(copyUnit);

		// then
		verify(softlinkRelationServiceMock).setSoftlinkRelation(any(ResourceEntity.class), any(SoftlinkRelationEntity.class));
		verify(foreignableServiceMock).verifyEditableByOwner(copyUnit.getActingOwner(), targetHashCodeBeforeChange, result);
		if(copyMode == CopyMode.MAIA_PREDECESSOR){
			assertEquals(targetSoftlinkRef, result.getSoftlinkRef());
		}else{
			assertEquals(origSoftlinkRef, result.getSoftlinkRef());
		}

		assertEquals(target, result.getCpiResource());
	}

	@Test
	// TODO: #12406 PropertyWerte von Relationen müssen übernommen werden wenn PropertyDescriptor auf dem Nachfolger noch existiert.
	public void shouldCopyPropertyValueFromRelationIfCopyModeMaiaPredecessor() throws AMWException, ForeignableOwnerViolationException {
		// given
		String identifier = "foo";
		String resourcePropValue = "abc";
		String relationPropValue = "xyz";

		when(resourceLocatorMock.hasResourceConsumableSoftlinkType(originResource)).thenReturn(true);
		when(resourceLocatorMock.hasResourceConsumableSoftlinkType(targetResource)).thenReturn(true);

        // create two identical descriptors/properties on origin and target

        ResourceContextEntity originResCtx = new ResourceContextEntity();
        originResCtx.setContext(globalContextMock);

        PropertyEntity originProp = new PropertyEntityBuilder().buildPropertyEntity(resourcePropValue, null);
        Set<PropertyEntity> origProps = new HashSet<>();
        origProps.add(originProp);
        PropertyDescriptorEntity originPropDes = new PropertyDescriptorEntityBuilder().withPropertyName("propDesc").withOwner(ForeignableOwner.MAIA).withProperties(origProps).build();
        originResCtx.addPropertyDescriptor(originPropDes);
        originResCtx.addProperty(originProp);

        ResourceEntity originCpi = new ResourceEntityBuilder().withName("originCpi").withTypeOfName(ResourceLocator.WS_CPI_TYPE).withContexts(Collections.singleton(originResCtx)).build();

		ResourceContextEntity targetResCtx = new ResourceContextEntity();
		targetResCtx.setContext(globalContextMock);

		PropertyEntity targetProp = new PropertyEntityBuilder().buildPropertyEntity(resourcePropValue, null);
        Set<PropertyEntity> targetProps = new HashSet<>();
        targetProps.add(targetProp);
		PropertyDescriptorEntity targetPropDes = new PropertyDescriptorEntityBuilder().withPropertyName("propDesc").withOwner(ForeignableOwner.MAIA).withProperties(targetProps).build();
		targetResCtx.addPropertyDescriptor(targetPropDes);
        targetResCtx.addProperty(targetProp);

		ResourceEntity targetCpi = new ResourceEntityBuilder().withName("targetCpi").withTypeOfName(ResourceLocator.WS_CPI_TYPE).withContexts(Collections.singleton(targetResCtx)).build();

		ResourceEntity slaveResource = new ResourceEntityBuilder().withName("slaveResource").withTypeOfName("webservice").build();

        // add a resource relation descriptor with another property value to the origin

		PropertyEntity origRelProp = new PropertyEntityBuilder().buildPropertyEntity(relationPropValue, originPropDes);
		ResourceRelationContextEntity originResRelCtx = new ResourceRelationContextEntityBuilder().mockResourceRelationContextEntity(globalContextMock);
		when(originResRelCtx.getProperties()).thenReturn(Collections.singleton(origRelProp));
		Set<ResourceRelationContextEntity> origins = Collections.singleton(originResRelCtx);

		ResourceRelationContextEntity targetResRelCtx = new ResourceRelationContextEntityBuilder().buildResourceRelationContextEntity(globalContextMock);
		AbstractResourceRelationEntity targetResRel = new ResourceRelationEntityBuilder().buildConsumedResRelEntity(targetCpi, slaveResource, identifier, 2);
		targetResRel.setContexts(Collections.singleton(targetResRelCtx));

		CopyUnit copyUnit = new CopyUnit(originCpi, targetCpi, copyMode, actingOwner);

		// when
		copyDomainService.copyResourceRelationContexts(origins, targetResRel, copyUnit);

		// then
        if (copyMode.equals(CopyMode.MAIA_PREDECESSOR) && targetResRel.getContexts().iterator().next().getProperties() != null) {
            assertEquals(relationPropValue, targetResRel.getContexts().iterator().next().getProperties().iterator().next().getValue());
        }
	}

}
