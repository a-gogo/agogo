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

import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextTypeEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.*;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import ch.puzzle.itc.mobiliar.business.template.entity.TemplateDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.utils.Identifiable;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.util.ContextNames;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Builds Domain Model for Tests
 * 
 * @author ama
 * @see {@link PersistingEntityBuilder} for building model in DB.
 */
public class EntityBuilder {
	private List<ResourceTypeEntity> types = Lists.newArrayList();
	private List<ResourceEntity> resources = Lists.newArrayList();
	private List<ProvidedResourceRelationEntity> providedRelations = Lists.newArrayList();
	private List<ConsumedResourceRelationEntity> consumedRelations = Lists.newArrayList();
	private List<ResourceRelationTypeEntity> typeRelations = Lists.newArrayList();
	private List<ContextEntity> contexts = Lists.newArrayList();

	private int resourceContextId = 0;
	private int contextTypeId = 0;
	private int contextId = 0;
	private int typeCounter = 0;
	private int resourceCounter = 0;
	private int providedRelationCounter = 0;
	private int consumedRelationCounter = 0;
	private int relationTypeCounter = 0;
	private int propertyDescriptorId = 0;
	private int propertyId = 0;
	private int templateDescriptorId = 0;
	private int resourceTypeContextIds = 0;

	public ResourceEntity platform;
	public ContextEntity context;
	public ResourceEntity as;
	public ResourceEntity app;

	public EntityBuilder() {
		build();
	}

	protected void build() {
	}

	protected void buildContextAndPlatform() {
		context = buildContext(ContextNames.ENV, "T");
		platform = resourceFor(EntityBuilderType.RUNTIME);
	}

	public void printRelations(ResourceEntity resource) {
		System.out.println(resource);
		System.out.println(resource.getConsumedMasterRelations());
		System.out.println(resource.getConsumedSlaveRelations());
		System.out.println(resource.getProvidedMasterRelations());
		System.out.println(resource.getProvidedSlaveRelations());
	}

	private ContextEntity buildContext(ContextNames typeName, String contextName) {
		ContextTypeEntity type = new ContextTypeEntity();
		type.setName(typeName.name());
		setId(type, contextTypeId++);

		ContextEntity context = new ContextEntity();
		context.setName(contextName);
		context.setContextType(type);
		setId(context, contextId++);
		contexts.add(context);

		return context;
	}

	// not sure if this must be call after all is set or doesnt matter when persisting/merging
	protected void setId(Identifiable resource, Integer id) {
		resource.setId(id);
	}

	public ContextEntity contextFor(String name) {
		for (ContextEntity context : contexts) {
			if (context.getName().equals(name)) {
				return context;
			}
		}
		throw new RuntimeException("context: " + name + " not found.");
	}

	public PropertyEntity buildResourceProperty(ResourceEntity entity, String name, String value) {
		return addResourceProperty(context, entity, name, value);
	}

	/**
	 * @deprecated use {@link #buildResourceProperty(ResourceEntity, String, String)}
	 */
	@Deprecated
	public PropertyEntity addResourceProperty(ContextEntity context, ResourceEntity entity, String name,
			String value) {
		// dont duplicate property descriptors
		ResourceContextEntity owner = getOrCreateResourceContext(context, entity);

		PropertyDescriptorEntity descriptor = new PropertyDescriptorEntity();
		descriptor.setPropertyName(name);
		setId(descriptor, propertyDescriptorId++);
		owner.addPropertyDescriptor(descriptor);

		PropertyEntity property = new PropertyEntity();
		property.setOwningResource(owner);
		property.setDescriptor(descriptor);
		property.setValue(value);
		setId(property, propertyId++);

		owner.addProperty(property);

		return property;
	}

	public ResourceContextEntity getOrCreateResourceContext(ContextEntity context, ResourceEntity as) {
		ResourceContextEntity owner = as.getOrCreateContext(context);
		if (owner.getId() == null) {
			setId(owner, resourceContextId++);
		}
		return owner;
	}

	public ResourceEntity resourceFor(EntityBuilderType type) {
		return resourceFor(type.name);
	}

	protected void buildTypeInheritance(ResourceTypeEntity base, ResourceTypeEntity child) {
		base.getChildrenResourceTypes().add(child);
		child.setParentResourceType(base);
	}

	public ConsumedResourceRelationEntity buildConsumedRelation(ResourceEntity master, ResourceEntity slave, ForeignableOwner changingOwner) {
		ResourceRelationTypeEntity typeRelation = buildTypeRelation(master.getResourceType(),
				slave.getResourceType());
		ConsumedResourceRelationEntity relation;
		relation = master.addConsumedResourceRelation(slave, typeRelation, null, changingOwner);
		setId(relation, consumedRelationCounter++);
		consumedRelations.add(relation);
		return relation;
	}

	public ConsumedResourceRelationEntity buildConsumedRelation(EntityBuilderType master,
			EntityBuilderType slave, ForeignableOwner changingOwner) {
		return buildConsumedRelation(resourceFor(master.name), resourceFor(slave.name), changingOwner);
	}

	public ProvidedResourceRelationEntity buildProvidedRelation(ResourceEntity master, ResourceEntity slave, ForeignableOwner changingOwner) {
		ResourceRelationTypeEntity buildTypeRelation = buildTypeRelation(master.getResourceType(),
				slave.getResourceType());
		ProvidedResourceRelationEntity relation;
		try {
			relation = master.addProvidedResourceRelation(slave, buildTypeRelation, changingOwner);
			setId(relation, providedRelationCounter++);
			providedRelations.add(relation);
			return relation;

		}
		catch (ElementAlreadyExistsException e) {
			throw new RuntimeException(e);
		}
	}

	//
	public ResourceRelationTypeEntity buildTypeRelation(EntityBuilderType master, EntityBuilderType slave) {
		return buildTypeRelation(typeFor(master.type), typeFor(slave.type));
	}

	public ResourceRelationTypeEntity buildTypeRelation(ResourceTypeEntity master, ResourceTypeEntity slave) {
		ResourceRelationTypeEntity typeRelation = new ResourceRelationTypeEntity();
		typeRelation.setResourceTypes(master, slave);
		setId(typeRelation, relationTypeCounter++);

		master.getResourceRelationTypesA().add(typeRelation);
		slave.getResourceRelationTypesB().add(typeRelation);
		typeRelations.add(typeRelation);

		return typeRelation;
	}

	public ResourceEntity resourceFor(String name) {
		for (ResourceEntity type : resources) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		throw new RuntimeException("resource: " + name + " not found.");
	}

	public ResourceTypeEntity typeFor(String name) {
		for (ResourceTypeEntity type : types) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		throw new RuntimeException("type: " + name + " not found.");
	}

	public ConsumedResourceRelationEntity relationFor(String master, String slave) {
		for (ConsumedResourceRelationEntity relation : consumedRelations) {
			if (relation.getMasterResource().getName().equals(master)
					&& relation.getSlaveResource().getName().equals(slave)) {
				return relation;
			}
		}
		throw new RuntimeException("master: " + master + ", slave: " + slave + " not found.");
	}

	public ConsumedResourceRelationEntity relationFor(EntityBuilderType master, EntityBuilderType slave) {
		return relationFor(master.name, slave.name);
	}

	public ResourceRelationTypeEntity typeRelationFor(EntityBuilderType master, EntityBuilderType slave) {
		return typeRelationFor(master.type, slave.type);
	}

	public ResourceRelationTypeEntity typeRelationFor(String master, String slave) {
		for (ResourceRelationTypeEntity relation : typeRelations) {
			if (relation.getResourceTypeA().getName().equals(master)
					&& relation.getResourceTypeB().getName().equals(slave)) {
				return relation;
			}
		}
		throw new RuntimeException("master: " + master + ", slave: " + slave + " not found.");
	}

	public ConsumedResourceRelationEntity buildRelation(ResourceEntity resource, Integer id) {
		ConsumedResourceRelationEntity relation = new ConsumedResourceRelationEntity();
		relation.setSlaveResource(resource);
		setId(relation, id);
		return relation;
	}

	public ResourceEntity buildResource(ResourceTypeEntity type, String name) {
		ResourceEntity resource = buildResource(type, name, resourceCounter++);
		resources.add(resource);
		return resource;
	}

	public ResourceEntity buildResource(EntityBuilderType type, String name) {
		return buildResource(typeFor(type.type), name);
	}

	/**
	 * @deprecated replaced by {@link #buildResource(ResourceTypeEntity, String)}
	 */
	@Deprecated
	public ResourceEntity buildResource(ResourceTypeEntity type, String name, Integer id) {
		ResourceGroupEntity resourceGroupEntity = new ResourceGroupEntity();
		setId(resourceGroupEntity, id);
		resourceGroupEntity.setName(name);
		ResourceEntity entity = ResourceFactory.createNewResource(resourceGroupEntity);
		entity.setResourceType(type);
		entity.setContexts(new HashSet<ResourceContextEntity>());
		entity.setConsumedMasterRelations(new HashSet<ConsumedResourceRelationEntity>());
		entity.setConsumedSlaveRelations(new HashSet<ConsumedResourceRelationEntity>());
		entity.setProvidedMasterRelations(new HashSet<ProvidedResourceRelationEntity>());
		entity.setProvidedSlaveRelations(new HashSet<ProvidedResourceRelationEntity>());
		setId(entity, id);
		return entity;
	}

	public ResourceTypeEntity buildResourceType(String name) {
		ResourceTypeEntity type = buildResourceType(name, ++typeCounter);
		types.add(type);
		return type;
	}

	/**
	 * @deprecated replaced by {@link #buildResourceType(String)}
	 */
	@Deprecated
	public ResourceTypeEntity buildResourceType(String name, Integer id) {
		ResourceTypeEntity type = new ResourceTypeEntity();

		type.setName(name);
		type.setResourceRelationTypesA(new HashSet<ResourceRelationTypeEntity>());
		type.setResourceRelationTypesB(new HashSet<ResourceRelationTypeEntity>());
		type.setChildrenResourceTypes(new HashSet<ResourceTypeEntity>());
		type.setContexts(new HashSet<ResourceTypeContextEntity>());
		setId(type, id);
		return type;
	}

	public List<ResourceEntity> getResources() {
		return resources;
	}

	public List<ConsumedResourceRelationEntity> getConsumedRelations() {
		return consumedRelations;
	}

	public TemplateDescriptorEntity buildResourceTemplate(ResourceEntity resource, String name,
			String content, String path) {
		TemplateDescriptorEntity template = buildTemplate(name, content, path);
		getOrCreateResourceContext(context, resource).addTemplate(template);
		return template;
	}

	public TemplateDescriptorEntity buildResourceTypeTemplate(ResourceTypeEntity type, String name,
			String content, String path) {
		TemplateDescriptorEntity template = buildTemplate(name, content, path);
		ResourceTypeContextEntity resourceTypeContext = buildResourceTypeContext(context, type);
		resourceTypeContext.addTemplate(template);
		type.getContexts().add(resourceTypeContext);
		return template;
	}

	private ResourceTypeContextEntity buildResourceTypeContext(ContextEntity context, ResourceTypeEntity type) {
		ResourceTypeContextEntity resourceTypeContext = new ResourceTypeContextEntity();
		resourceTypeContext.setContext(context);
		resourceTypeContext.setContextualizedObject(type);
		setId(resourceTypeContext, resourceTypeContextIds++);
		return resourceTypeContext;
	}

	private void addResourceTemplate(ResourceContextEntity resourceContext, String name, String content,
			String path) {
		resourceContext.addTemplate(buildTemplate(name, content, path));
	}

	protected TemplateDescriptorEntity buildTemplate(String name, String content, String path) {
		TemplateDescriptorEntity template = new TemplateDescriptorEntity();
		template.setFileContent(content);
		template.setName(name);
		template.setTesting(false);
		template.setTargetPlatforms(new LinkedHashSet<ResourceGroupEntity>(Arrays.asList(platform
				.getResourceGroup())));
		template.setTargetPath(path);
		setId(template, templateDescriptorId++);

		return template;
	}

	/**
	 * @deprecated replaced by {@link #buildResourceTypeTemplate(ResourceTypeEntity, String, String, String)}
	 */
	@Deprecated
	public void addResourceTypeTemplate(ContextEntity context, ResourceTypeEntity type, String name,
			String content, String path) {
		TemplateDescriptorEntity template = buildTemplate(name, content, path);

		ResourceTypeContextEntity resourceTypeContext = buildResourceTypeContext(context, type);
		resourceTypeContext.addTemplate(template);
		type.getContexts().add(resourceTypeContext);

	}

	/**
	 * @deprecated replaced by {@link #buildResourceTemplate(ResourceEntity, String, String, String)}
	 */
	@Deprecated
	public void addResourceTemplate(ContextEntity context, ResourceEntity resource, String name,
			String content) {
		addResourceTemplate(context, resource, name, content, null);
	}

	/**
	 * @deprecated replaced by {@link #buildResourceTemplate(ResourceEntity, String, String, String)}
	 */
	@Deprecated
	public void addResourceTemplate(ContextEntity context, ResourceEntity resource, String name,
			String content, String path) {
		addResourceTemplate(getOrCreateResourceContext(context, resource), name, content, path);
	}

}
