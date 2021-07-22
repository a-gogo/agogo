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

package ch.puzzle.itc.mobiliar.business.property.boundary;

import ch.puzzle.itc.mobiliar.business.auditview.control.AuditService;
import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.environment.control.ContextDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextDependency;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.HasContexts;
import ch.puzzle.itc.mobiliar.business.foreignable.control.ForeignableService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.property.control.*;
import ch.puzzle.itc.mobiliar.business.property.entity.*;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceEditService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceValidationService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.boundary.ResourceRelationLocator;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationContextRepository;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.AbstractResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.security.interceptor.HasPermission;
import ch.puzzle.itc.mobiliar.business.utils.ValidationHelper;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.NotAuthorizedException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import ch.puzzle.itc.mobiliar.common.util.ContextNames;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.*;
import java.util.logging.Logger;

/**
 * ALL boundary for property editing
 */
@Stateless
public class PropertyEditor {

    // TODO Move methods to the proper boundary

    @Inject
    AuditService auditService;

    @Inject
    PropertyDescriptorService propertyDescriptorService;

    @Inject
    PropertyValueService propertyValueService;

    @Inject
    PropertyValidationService propertyValidationService;

    @Inject
    PropertyTypeService propertyTypeService;

    @Inject
    ResourceValidationService resourceValidationService;

    @Inject
    EntityManager entityManager;

    @Inject
    PropertyEditingService propertyEditingService;

    @Inject
    ResourceEditService resourceEditService;

    @Inject
    ContextDomainService contextService;

    @Inject
    PermissionBoundary permissionBoundary;

    @Inject
    ResourceLocator resourceLocator;

    @Inject
    ResourceRepository resourceRepository;

    @Inject
    ResourceRelationLocator resourceRelationLocator;

    @Inject
    ContextLocator contextLocator;

    @Inject
    PropertyTagEditingService propertyTagEditingService;

    @Inject
    ResourceRelationContextRepository resourceRelationContextRepository;

    @Inject
    protected Logger log;

    @Inject
    ForeignableService foreignableService;

    public List<PropertyDescriptorEntity> getAllPropertyDescriptorsForResourceWithNullCardinality(
            ResourceEntity resource) {
        return propertyDescriptorService
                .getPropertyDescriptorsForHasContextWithNullCardinality(entityManager.find(
                        ResourceEntity.class, resource.getId()));
    }

    /**
     * Loads all property descriptors for the given resource (but not of its resource type) except those
     * which are marked to have a special cardinality (usually system properties)
     *
     * @param resource
     * @return
     */
    public List<PropertyDescriptorEntity> getPropertyDescriptorsForResourceWithNullCardinality(
            ResourceEntity resource) {
        return propertyDescriptorService
                .getPropertyDescriptorsForHasContextWithNullCardinality(entityManager.find(
                        ResourceEntity.class, resource.getId()));
    }

    /**
     * Loads all property descriptors for the given resource type except those which are marked to have a
     * special cardinality (usually system properties)
     *
     * @param resourceType
     * @return
     */
    public List<PropertyDescriptorEntity> getPropertyDescriptorsForResourceTypeWithNullCardinality(
            ResourceTypeEntity resourceType) {
        return propertyDescriptorService
                .getPropertyDescriptorsForHasContextWithNullCardinality(entityManager.find(
                        ResourceTypeEntity.class, resourceType.getId()));
    }

    /**
     * @return all available property types with validation logic
     */
    public List<PropertyTypeEntity> getPropertyTypes() {
        return propertyTypeService.getPropertyTypes();
    }

    /**
     * Returns the property values (including information about their descriptors and overwritten values) for
     * the given resource in the specified context.
     *
     * @param resourceId
     * @param contextId
     * @return
     */
    public List<ResourceEditProperty> getPropertiesForResource(Integer resourceId, Integer contextId) {
        ResourceEntity resource = entityManager.find(ResourceEntity.class, resourceId);
        ContextEntity context = entityManager.find(ContextEntity.class, contextId);
        List<ResourceEditProperty> properties = propertyEditingService.loadPropertiesForEditResource(
                resource.getId(), resource.getResourceType(), context);
        if (permissionBoundary.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, context, Action.ALL, resource, null)) {
            return propertyValueService.decryptProperties(properties);
        }
        return properties;
    }

    /**
     * Returns the property values (including information about their descriptors and overwritten values) for
     * the given resource type in the specified context.
     *
     * @param resourceTypeId
     * @param contextId
     * @return
     */
    public List<ResourceEditProperty> getPropertiesForResourceType(Integer resourceTypeId, Integer contextId) {
        ResourceTypeEntity resourceType = entityManager.find(ResourceTypeEntity.class, resourceTypeId);
        ContextEntity context = entityManager.find(ContextEntity.class, contextId);
        if (permissionBoundary.hasPermission(Permission.RESOURCETYPE_PROPERTY_DECRYPT, context, Action.ALL, null, resourceType)) {
            return propertyValueService.decryptProperties(propertyEditingService
                    .loadPropertiesForEditResourceType(resourceType, context));
        }
        return propertyEditingService.loadPropertiesForEditResourceType(resourceType, context);
    }

    /**
     * Returns the property values (including information about their descriptors and overwritten values)
     *
     * @param masterResourceId
     * @param contextId
     * @return
     */
    public List<ResourceEditProperty> getPropertiesForRelatedResource(Integer masterResourceId,
            ResourceEditRelation resourceRelation, Integer contextId) {
        if (masterResourceId != null && resourceRelation != null && contextId != null) {
            ResourceEntity resource = entityManager.find(ResourceEntity.class, masterResourceId);
            ContextEntity context = entityManager.find(ContextEntity.class, contextId);
            ResourceEntity slaveResource = entityManager.find(ResourceEntity.class,
                    resourceRelation.getSlaveId());
            // a decrypt permission on the master resource allows to decrypt on the related slave properties
            if (permissionBoundary.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, context, Action.ALL, resource, null)) {
                return propertyValueService.decryptProperties(propertyEditingService.loadPropertiesForEditRelation(
                        resourceRelation.getMode(), resourceRelation.getResRelId(), slaveResource.getId(),
                        resource.getResourceType(), slaveResource.getResourceType(), context));
            }
            return propertyEditingService.loadPropertiesForEditRelation(resourceRelation.getMode(),
                    resourceRelation.getResRelId(), slaveResource.getId(), resource.getResourceType(),
                    slaveResource.getResourceType(), context);
        }
        return new ArrayList<>();
    }

    /**
     * Returns the property values (including information about their descriptors and overwritten values)
     * Encrypted property values are returned decrypted if the caller has the permission to do so
     */
    public List<ResourceEditProperty> getPropertiesForRelatedResource(
            ConsumedResourceRelationEntity relationEntity, Integer contextId) {
        ResourceEntity resource = entityManager.find(ResourceEntity.class,
                relationEntity.getMasterResourceId());
        ContextEntity context = entityManager.find(ContextEntity.class, contextId);
        ResourceEntity slaveResource = entityManager.find(ResourceEntity.class, relationEntity
                .getSlaveResource().getId());
        // a decrypt permission on the master resource allows to decrypt on the related slave properties
        if (permissionBoundary.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, context, Action.ALL, resource, null)) {
            return propertyValueService.decryptProperties(propertyEditingService.loadPropertiesForEditRelation(
                    ResourceEditRelation.Mode.CONSUMED, relationEntity.getId(), slaveResource.getId(),
                    resource.getResourceType(), slaveResource.getResourceType(), context));
        }
        return propertyEditingService.loadPropertiesForEditRelation(ResourceEditRelation.Mode.CONSUMED,
                relationEntity.getId(), slaveResource.getId(), resource.getResourceType(),
                slaveResource.getResourceType(), context);
    }

    /**
     * Returns the property values (including information about their descriptors and overwritten values) of
     * the given ResourceRelationTypeEntity
     * Encrypted property values are returned decrypted if the caller has the permission to do so
     *
     * @param resourceRelation
     * @param contextId
     * @return
     */
    public List<ResourceEditProperty> getPropertiesForRelatedResourceType(ResourceEditRelation resourceRelation, Integer contextId) {
        ContextEntity context = entityManager.find(ContextEntity.class, contextId);
        ResourceRelationTypeEntity relationTypeEntity = entityManager.find(
                ResourceRelationTypeEntity.class, resourceRelation.getResRelTypeId());
        // a decrypt permission on the resource type A allows to decrypt on the related resource type B properties
        if (permissionBoundary.hasPermission(Permission.RESOURCETYPE_PROPERTY_DECRYPT, context, Action.ALL, null,
                relationTypeEntity.getResourceTypeA())) {
            return propertyValueService.decryptProperties(propertyEditingService
                    .loadPropertiesForEditResourceTypeRelation(relationTypeEntity.getResourceTypeA(),
                            relationTypeEntity.getResourceTypeB(), context));
        }
        return propertyEditingService.loadPropertiesForEditResourceTypeRelation(relationTypeEntity.getResourceTypeA(),
                relationTypeEntity.getResourceTypeB(), context);
    }

    public Map<ResourceEditRelation.Mode, List<ResourceEditRelation>> getRelationsForResource(
            Integer resourceId) {
        return resourceEditService.loadResourceRelationsForEdit(resourceId);
    }

    public Map<ResourceEditRelation.Mode, List<ResourceEditRelation>> getRelationsForResourceType(
            Integer resourceTypeId) {
        ResourceTypeEntity resourceType = entityManager.find(ResourceTypeEntity.class, resourceTypeId);
        return resourceEditService.loadResourceRelationTypesForEdit(resourceType);
    }

    /**
     * Persists changes made on a Resource if the use has the permission to do so
     *
     * @param changingOwner
     * @param contextId
     * @param resourceId
     * @param resourceProperties
     * @param relation
     * @param relationProperties
     * @param resourceName
     * @throws AMWException
     * @throws ValidationException
     * @throws ForeignableOwnerViolationException
     */
    public void save(ForeignableOwner changingOwner, Integer contextId, Integer resourceId, List<ResourceEditProperty> resourceProperties,
            ResourceEditRelation relation, List<ResourceEditProperty> relationProperties, String resourceName, String relationIdentifier) throws AMWException, ValidationException, ForeignableOwnerViolationException {

        ContextEntity context = entityManager.find(ContextEntity.class, contextId);
        ResourceEntity editedResource = verifyAndSaveResource(resourceId, changingOwner, resourceName, context);

        if (permissionBoundary.hasPermission(Permission.RESOURCE, context, Action.UPDATE, editedResource, editedResource.getResourceType())) {
            propertyValueService.saveProperties(context, editedResource, resourceProperties);
            auditService.setResourceIdInThreadLocal(editedResource.getId());
            if (relation != null) {
                handleRelations(relation, relationProperties, relationIdentifier, context, editedResource);
            }
        }
    }

    private void handleRelations(ResourceEditRelation relation, List<ResourceEditProperty> relationProperties, String relationIdentifier,
                                 ContextEntity context, ResourceEntity editedResource) throws ValidationException {
        String previousIdentifier = null;
        String slaveName = null;
        AbstractResourceRelationEntity resourceRelation = editedResource.getResourceRelation(relation);
        if (relation.hasIdentifierChanged(relationIdentifier)) {
            previousIdentifier = relation.getQualifiedIdentifier();
            slaveName = relation.getSlaveName();
            resourceRelation.setIdentifier(relationIdentifier);
        }
        propertyValueService.saveProperties(context, resourceRelation, relationProperties);
        if (previousIdentifier != null) {
            // if the previousIdentifier equals to the slaveName, then the identifier of the relation has been empty before
            if (previousIdentifier.equals(slaveName)) {
                for (ConsumedResourceRelationEntity consumedResourceRelation : editedResource.getConsumedMasterRelations()) {
                    if (consumedResourceRelation.getIdentifier() == null
                            && consumedResourceRelation.getSlaveResource().getName().equals(previousIdentifier)) {
                        consumedResourceRelation.setIdentifier(relationIdentifier);
                        entityManager.merge(consumedResourceRelation);
                    }
                }
            } else {
                for (ConsumedResourceRelationEntity consumedResourceRelation : editedResource.getConsumedMasterRelations()) {
                    if (consumedResourceRelation.getIdentifier() != null
                            && consumedResourceRelation.getIdentifier().equals(previousIdentifier)) {
                        consumedResourceRelation.setIdentifier(relationIdentifier);
                        entityManager.merge(consumedResourceRelation);
                    }
                }
            }
        }
    }

    private ResourceEntity verifyAndSaveResource(Integer resourceId, ForeignableOwner changingOwner, String resourceName, ContextEntity context) throws ForeignableOwnerViolationException, AMWException {
        ResourceEntity resource = resourceRepository.find(resourceId);
        int beforeChangeForeignableHashCode = resource.foreignableFieldHashCode();

        // do permission check
        if (permissionBoundary.hasPermission(Permission.RESOURCE, context, Action.UPDATE, resource, null)) {
            verifyAndSetResourceName(resourceName, resource);
        }

        // check if owner can modify resource
        foreignableService.verifyEditableByOwner(changingOwner, beforeChangeForeignableHashCode, resource);
        return resource;
    }

    private void verifyAndSetResourceName(String resourceName, ResourceEntity resource) throws AMWException {
        if (resourceName == null || !resourceName.equals(resource.getName())) {
            resourceValidationService.validateResourceName(resourceName);
            resource.setName(resourceName);
        }
    }

    /**
     * Persists changes made on a ResourceType
     *
     * @param contextId
     * @param resourceTypeId
     * @param resourceProperties
     * @param relation
     * @param relationProperties
     * @param resourceTypeName
     * @param typeRelationIdentifier
     * @throws AMWException
     * @throws ValidationException
     */
    public void savePropertiesForResourceType(Integer contextId, Integer resourceTypeId,
            List<ResourceEditProperty> resourceProperties, ResourceEditRelation relation,
            List<ResourceEditProperty> relationProperties, String resourceTypeName,
            String typeRelationIdentifier) throws AMWException, ValidationException {
        ResourceTypeEntity resourceType = entityManager.find(ResourceTypeEntity.class, resourceTypeId);

        ContextEntity context = entityManager.find(ContextEntity.class, contextId);
        if (permissionBoundary.hasPermission(Permission.RESOURCETYPE, context, Action.UPDATE, null, resourceType)) {
            if (resourceTypeName == null || !resourceTypeName.equals(resourceType.getName())) {
                resourceValidationService.validateResourceTypeName(resourceTypeName, resourceType.getName());
                resourceType.setName(resourceTypeName);
            }
            propertyValueService.saveProperties(context, resourceType, resourceProperties);
            if (relation != null && relation.getResRelTypeId() != null) {
                ResourceRelationTypeEntity resourceRelationTypeEntity = entityManager.find(
                        ResourceRelationTypeEntity.class, relation.getResRelTypeId());
                if (relation.hasIdentifierChanged(typeRelationIdentifier)) {
                    resourceRelationTypeEntity.setIdentifier(typeRelationIdentifier);
                }
                propertyValueService.saveProperties(context, resourceRelationTypeEntity,
                        relationProperties);
            }
        }
    }

    private ResourceEntity getRelevantResource(HasContexts<?> hasContexts) {
        if (hasContexts instanceof ResourceEntity) {
            return (ResourceEntity) hasContexts;
        }
        if (hasContexts instanceof ConsumedResourceRelationEntity) {
            ConsumedResourceRelationEntity relation = (ConsumedResourceRelationEntity) hasContexts;
            return relation.getMasterResource();
        }
        log.warning("Unexpected Object "+hasContexts.getClass().getSimpleName());
        return null;
    }

    private void resetSingleProperty(HasContexts<?> hasContexts, ContextEntity context,
            Integer propertyDescriptorId) {
        ResourceEntity resource = getRelevantResource(hasContexts);
        if (permissionBoundary.hasPermission(Permission.RESOURCE, context, Action.UPDATE, resource, null)) {
            HasContexts<?> hasContextMerged = entityManager.merge(hasContexts);
            ContextEntity contextMerged = entityManager.merge(context);
            ContextDependency<?> contextDependency = hasContextMerged.getOrCreateContext(contextMerged);
            propertyValueService.resetPropertyValue(contextDependency, propertyDescriptorId);
        }
        else {
            log.warning("Not allowed to reset property value");
            throw new NotAuthorizedException("Not allowed to reset property value");
        }
    }

    private void setSingleProperty(HasContexts<?> hasContexts, ContextEntity context,
            Integer propertyDescriptorId, String unobfuscatedValue) throws ValidationException {
        ResourceEntity resource = getRelevantResource(hasContexts);
        if (permissionBoundary.hasPermission(Permission.RESOURCE, context, Action.UPDATE, resource, null)) {
            HasContexts<?> hasContextMerged = entityManager.merge(hasContexts);
            ContextEntity contextMerged = entityManager.merge(context);
            ContextDependency<?> contextDependency = hasContextMerged.getOrCreateContext(contextMerged);
            auditService.storeIdInThreadLocalForAuditLog(contextDependency);
            propertyValueService.setPropertyValue(contextDependency, propertyDescriptorId,
                    unobfuscatedValue);
        }
        else {
            log.warning("Not allowed to set property value");
            throw new NotAuthorizedException("Not allowed to set property value");
        }
    }

    /**
     * Set property value for resource in release and context.
     *
     * @param resourceGroupName
     *             resource group name
     * @param releaseName
     *             release name
     * @param contextName
     *             context name
     * @param propertyName
     *             property name for which tha value should be set
     * @param propertyValue
     *             value to set
     * @throws ValidationException
     *              thrown if one of the arguments is either empty or null
     */
    public void setPropertyValueOnResourceForContext(String resourceGroupName, String releaseName,
            String contextName, String propertyName, String propertyValue) throws ValidationException {
        ValidationHelper
                .validateNotNullOrEmptyChecked(resourceGroupName, releaseName, propertyValue, propertyName);

        ResourceEntity resourceByNameAndRelease = resourceLocator.getResourceByGroupNameAndRelease(
                resourceGroupName, releaseName);

        if (resourceByNameAndRelease == null) {
            log.info("Resource group " + resourceGroupName + " not found in release " + releaseName);
            throw new NoResultException("Could not find resource group in this release");
        }

        ContextEntity context = contextLocator.getContextByName(contextName == null ? ContextNames.GLOBAL
                .getDisplayName() : contextName);

        List<ResourceEditProperty> resourceEditProperties = propertyEditingService
                .loadPropertiesForEditResource(resourceByNameAndRelease.getId(),
                        resourceByNameAndRelease.getResourceType(), context);

        ResourceEditProperty property = findByName(propertyName, resourceEditProperties);
        property.setPropertyValue(propertyValue);
        setSingleProperty(resourceByNameAndRelease, context, property.getDescriptorId(), property.getUnobfuscatedValue());
    }

    /**
     * Reset property value for resource in release and context.
     *
     * @param resourceGroupName
     *             resource group name
     * @param releaseName
     *             release name
     * @param contextName
     *             context name
     * @param propertyName
     *             property name for which tha value should be set
     * @throws ValidationException
     *              thrown if one of the arguments is either empty or null
     */
    public void resetPropertyValueOnResourceForContext(String resourceGroupName, String releaseName,
            String contextName, String propertyName) throws ValidationException {
        ValidationHelper.validateNotNullOrEmptyChecked(resourceGroupName, releaseName, propertyName);

        ResourceEntity resource = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName,
                releaseName);
        ContextEntity context = contextLocator.getContextByName(contextName == null ? ContextNames.GLOBAL
                .getDisplayName() : contextName);

        List<ResourceEditProperty> resourceEditProperties = propertyEditingService
                .loadPropertiesForEditResource(resource.getId(), resource.getResourceType(), context);

        List<ResourceEditProperty> potentiallyDecryptedProperties;
        if (permissionBoundary.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, context, Action.ALL, resource, null)) {
            potentiallyDecryptedProperties = propertyValueService.decryptProperties(resourceEditProperties);
        } else {
            potentiallyDecryptedProperties = resourceEditProperties;
        }
        ResourceEditProperty property = findByName(propertyName, potentiallyDecryptedProperties);
        resetSingleProperty(resource, context, property.getDescriptorId());
    }

    /**
     * Set hostname value on all relations between a node release and all releases of an application server
     * where no hostname is defined yet
     *
     * @param masterResourceGroupName
     *             name master resource (group)
     * @param slaveResourceGroupName
     *             slave resource group name
     * @param slaveResourceReleaseName
     *             release name of slave resource
     * @param contextName
     *             context name
     * @param propertyName
     *             property name for which tha value should be set
     * @param propertyValue
     *             value to set
     * @throws ValidationException
     *              thrown if one of the arguments is either empty or null
     */
    public void setPropertyValueOnAllResourceRelationsForContextWhereNotYetSet(
            String masterResourceGroupName, String slaveResourceGroupName,
            String slaveResourceReleaseName, String contextName, String propertyName, String propertyValue)
            throws ValidationException {
        ValidationHelper.validateNotNullOrEmptyChecked(masterResourceGroupName, slaveResourceGroupName,
                slaveResourceReleaseName, contextName, propertyName, propertyValue);

        List<ResourceEntity> resourcesByGroupNameWithRelations = resourceLocator
                .getResourcesByGroupNameWithRelations(masterResourceGroupName);
        ContextEntity context = contextLocator.getContextByName(contextName == null ? ContextNames.GLOBAL
                .getDisplayName() : contextName);

        ResourceEntity node = resourceLocator.getResourceByGroupNameAndRelease(slaveResourceGroupName,
                slaveResourceReleaseName);

        if (node == null || resourcesByGroupNameWithRelations.isEmpty()) {
            log.info("No relations found between any resource in group" + masterResourceGroupName
                    + " to resource " + slaveResourceGroupName + " in release "
                    + slaveResourceReleaseName);
            throw new NoResultException("Could not find relation");
        }

        for (ResourceEntity r : resourcesByGroupNameWithRelations) {
            ConsumedResourceRelationEntity resourceRelation = r.getConsumedRelation(node);

            if (resourceRelation != null) {
                ResourceEditProperty property = getPropertyForRelationAndContext(propertyName, context,
                        resourceRelation);
                if (!property.isPropertyValueSet()) {
                    log.info("Set host name on relation between node " + slaveResourceGroupName
                            + " and application server " + masterResourceGroupName + " for release "
                            + slaveResourceReleaseName + " and context " + contextName);
                    setSingleProperty(resourceRelation, context, property.getDescriptorId(),
                            propertyValue);
                }
            }
        }
    }

    private ResourceEditProperty getPropertyForRelationAndContext(String hostName, ContextEntity context,
            ConsumedResourceRelationEntity resourceRelation) {
        List<ResourceEditProperty> propertiesForRelatedResource = getPropertiesForRelatedResource(
                resourceRelation, context.getId());
        return findByName(hostName, propertiesForRelatedResource);
    }

    /**
     * Set property value on relation between two resource releases and context.
     *
     * @param resourceGroupName
     *             resource group name
     * @param resourceReleaseName
     *             release name
     * @param relatedResourceGroupName
     *             related resource group name
     * @param relatedResourceReleaseName
     *             related resource release name
     * @param contextName
     *             context name
     * @param propertyName
     *             property name for which tha value should be set
     * @param propertyValue
     *             value to set
     * @throws ValidationException
     *              thrown if one of the arguments is either empty or null
     */
    public void setPropertyValueOnResourceRelationForContext(String resourceGroupName,
            String resourceReleaseName, String relatedResourceGroupName,
            String relatedResourceReleaseName, String contextName, String propertyName,
            String propertyValue) throws ValidationException {
        ValidationHelper.validateNotNullOrEmptyChecked(resourceGroupName, resourceReleaseName, relatedResourceGroupName, relatedResourceReleaseName, propertyValue, propertyName);

        ConsumedResourceRelationEntity resourceRelation = resourceRelationLocator.getResourceRelation(resourceGroupName, resourceReleaseName, relatedResourceGroupName, relatedResourceReleaseName);
        ContextEntity context = contextLocator.getContextByName(contextName == null ? ContextNames.GLOBAL.getDisplayName() : contextName);

        ResourceEditProperty property = getPropertyForRelationAndContext(propertyName, context, resourceRelation);
        property.setPropertyValue(propertyValue);
        setSingleProperty(resourceRelation, context, property.getDescriptorId(), property.getUnobfuscatedValue());
    }

    public PropertyDescriptorEntity getPropertyDescriptor(Integer propertyDescriptorId) {
        return propertyDescriptorService.getPropertyDescriptorWithTags(propertyDescriptorId);
    }

    /**
     * Reset property value on relation between two resource releases and context.
     *
     * @param resourceGroupName
     *             resource group name
     * @param resourceReleaseName
     *             release name
     * @param relatedResourceGroupName
     *             related resource group name
     * @param relatedResourceReleaseName
     *             related resource release name
     * @param contextName
     *             context name
     * @param propertyName
     *             property name for which tha value should be set
     * @throws ValidationException
     *              thrown if one of the arguments is either empty or null
     */
    public void resetPropertyValueOnResourceRelationForContext(String resourceGroupName,
            String resourceReleaseName, String relatedResourceGroupName,
            String relatedResourceReleaseName, String contextName, String propertyName)
            throws IllegalArgumentException, EJBException, IllegalStateException, NotAuthorizedException,
            ValidationException {
        ValidationHelper.validateNotNullOrEmptyChecked(resourceGroupName, resourceReleaseName, relatedResourceGroupName, relatedResourceReleaseName, propertyName);

        ConsumedResourceRelationEntity resourceRelation = resourceRelationLocator.getResourceRelation(resourceGroupName, resourceReleaseName, relatedResourceGroupName, relatedResourceReleaseName);
        ContextEntity context = contextLocator.getContextByName(contextName == null ? ContextNames.GLOBAL.getDisplayName() : contextName);

        ResourceRelationContextEntity resourceRelationContext = resourceRelationContextRepository.getResourceRelationContext(resourceRelation, context);
        PropertyDescriptorEntity propertyDescriptor = getMatchingPropertyDescriptorEntity(propertyName, resourceRelationContext);

        if (propertyDescriptor == null) {
            throw new IllegalArgumentException("Property value for property with name '" + propertyName + "' was not found on env " + context.getName());
        }
        resetSingleProperty(resourceRelation, context, propertyDescriptor.getId());
    }

    private PropertyDescriptorEntity getMatchingPropertyDescriptorEntity(String propertyName, ResourceRelationContextEntity resourceRelationContext) {
        if (resourceRelationContext != null && resourceRelationContext.getProperties() != null) {
            for (PropertyEntity property : resourceRelationContext.getProperties()) {
                if (property.getDescriptor().getPropertyName().equals(propertyName)) {
                    return property.getDescriptor();
                }
            }
        }
        return null;
    }

    /**
     * Checks Permissions and Persists the given shakedown test property descriptor to the database for resource instance
     */
    @HasPermission(permission = Permission.SHAKEDOWN_TEST_MODE)
    public PropertyDescriptorEntity saveTestingPropertyDescriptorForResource(Integer resourceId, PropertyDescriptorEntity descriptor,int foreignableHashBeforeModification, String propertyTagsString) throws AMWException, ForeignableOwnerViolationException {
        // verify if modifications are allowed
        foreignableService.verifyEditableByOwner(ForeignableOwner.getSystemOwner(), foreignableHashBeforeModification, descriptor);
        return savePropertyDescriptorResource(ForeignableOwner.getSystemOwner(), resourceId, descriptor, propertyTagsString);
    }

    /**
     * Checks Permissions and Persists the given property descriptor to the database for resource instance
     */
    public PropertyDescriptorEntity savePropertyDescriptorForResource(ForeignableOwner editingOwner, Integer resourceId, PropertyDescriptorEntity descriptor, int foreignableHashBeforeModification, String propertyTagsString) throws AMWException, ForeignableOwnerViolationException {
        ResourceEntity attachedResource = entityManager.find(ResourceEntity.class, resourceId);
        // verify if modifications are allowed
        permissionBoundary.checkPermissionAndFireException(Permission.RESOURCE, null, Action.UPDATE, attachedResource.getResourceGroup(), null, null);
        foreignableService.verifyEditableByOwner(editingOwner, foreignableHashBeforeModification, descriptor);
        return savePropertyDescriptorResource(editingOwner, resourceId, descriptor, propertyTagsString);
    }

    private PropertyDescriptorEntity savePropertyDescriptorResource(ForeignableOwner editingOwner, Integer resourceId, PropertyDescriptorEntity descriptor, String propertyTagsString) throws AMWException {
        ResourceEntity attachedResource = entityManager.find(ResourceEntity.class, Objects.requireNonNull(resourceId));
        ResourceContextEntity resourceContext = attachedResource.getOrCreateContext(contextService.getGlobalResourceContextEntity());
        ResourceTypeContextEntity resourceTypeContextEntity = attachedResource.getResourceType().getOrCreateContext(contextService.getGlobalResourceContextEntity());
        List<String> duplicatePropertyDescriptorNames = propertyValidationService.getDuplicatePropertyDescriptors(resourceContext, resourceTypeContextEntity, descriptor);

        if (!duplicatePropertyDescriptorNames.isEmpty()) {
            throw new AMWException("Failure - duplicate propertydescriptors: " + duplicatePropertyDescriptorNames);
        }

        return propertyDescriptorService.savePropertyDescriptorForOwner(editingOwner, resourceContext, descriptor, propertyTagEditingService.convertToTags(propertyTagsString), attachedResource);
    }

    /**
     * Checks Permissions and Persists the given shakedown test property descriptor to the database for resource type
     */
    @HasPermission(permission = Permission.SHAKEDOWN_TEST_MODE)
    public PropertyDescriptorEntity saveTestingPropertyDescriptorForResourceType(Integer resourceTypeId, PropertyDescriptorEntity descriptor, int foreignableHashBeforeModification, String propertyTagsString) throws AMWException, ForeignableOwnerViolationException {

        // verify if modifications are allowed
        foreignableService.verifyEditableByOwner(ForeignableOwner.getSystemOwner(), foreignableHashBeforeModification, descriptor);
        return savePropertyDescriptorResourceType(ForeignableOwner.getSystemOwner(), resourceTypeId, descriptor, propertyTagsString);
    }

    /**
     * Checks Permissions and Persists the given property descriptor to the database for resource type
     */
    public PropertyDescriptorEntity savePropertyDescriptorForResourceType(ForeignableOwner editingOwner, Integer resourceTypeId, PropertyDescriptorEntity descriptor,int foreignableHashBeforeModification, String propertyTagsString) throws AMWException, ForeignableOwnerViolationException {
        ResourceTypeEntity attachedResourceType = entityManager.find(ResourceTypeEntity.class, resourceTypeId);
        // verify if modifications are allowed
        permissionBoundary.checkPermissionAndFireException(Permission.RESOURCETYPE, null, Action.UPDATE, null, attachedResourceType, null);
        foreignableService.verifyEditableByOwner(editingOwner, foreignableHashBeforeModification, descriptor);
        return savePropertyDescriptorResourceType(editingOwner, resourceTypeId, descriptor, propertyTagsString);
    }

    private PropertyDescriptorEntity savePropertyDescriptorResourceType(ForeignableOwner editingOwner, Integer resourceTypeId, PropertyDescriptorEntity descriptor, String propertyTagsString) throws AMWException {
        ResourceTypeEntity attachedResourceType = entityManager.find(ResourceTypeEntity.class, resourceTypeId);
        ResourceTypeContextEntity resourceTypeContextEntity = attachedResourceType.getOrCreateContext(contextService.getGlobalResourceContextEntity());
        return propertyDescriptorService.savePropertyDescriptorForOwner(editingOwner, resourceTypeContextEntity, descriptor, propertyTagEditingService.convertToTags(propertyTagsString), attachedResourceType);
    }

    /**
     * Checks Permissions and deletes the given property descriptor from database for resource
     */
    public void deletePropertyDescriptorForResource(ForeignableOwner deletingOwner, Integer resourceId, PropertyDescriptorEntity descriptor, boolean forceDelete) throws AMWException, ForeignableOwnerViolationException {

        if (descriptor != null && descriptor.getId() != null) {
            ResourceEntity attachedResource = entityManager.find(ResourceEntity.class, resourceId);
            ResourceContextEntity resourceContext = attachedResource.getOrCreateContext(contextService.getGlobalResourceContextEntity());

            permissionBoundary.checkPermissionAndFireException(Permission.RESOURCE, null, Action.UPDATE, attachedResource.getResourceGroup(), null, null);
            foreignableService.verifyDeletableByOwner(deletingOwner, descriptor);
            if (forceDelete) {
                propertyDescriptorService.deletePropertyDescriptorByOwnerIncludingPropertyValues(descriptor, resourceContext, attachedResource);
            } else {
                propertyDescriptorService.deletePropertyDescriptorByOwnerInResourceContext(descriptor, resourceContext, resourceId);
            }
        }
    }

    /**
     * Checks Permissions and deletes the given property descriptor from database for resource type
     */
    public void deletePropertyDescriptorForResourceType(ForeignableOwner deletingOwner, Integer resourceTypeId, PropertyDescriptorEntity descriptor, boolean forceDelete) throws AMWException, ForeignableOwnerViolationException {

        if (descriptor != null && descriptor.getId() != null) {
            ResourceTypeEntity attachedResourceType = entityManager.find(ResourceTypeEntity.class, resourceTypeId);
            ResourceTypeContextEntity resourceTypeContextEntity = attachedResourceType.getOrCreateContext(contextService.getGlobalResourceContextEntity());

            permissionBoundary.checkPermissionAndFireException(Permission.RESOURCETYPE, null, Action.UPDATE, null, attachedResourceType, null);
            foreignableService.verifyDeletableByOwner(deletingOwner, descriptor);
            if (forceDelete) {
                propertyDescriptorService.deletePropertyDescriptorByOwnerIncludingPropertyValues(descriptor, resourceTypeContextEntity, attachedResourceType);
            } else {
                propertyDescriptorService.deletePropertyDescriptorByOwnerInResourceTypeContext(descriptor, resourceTypeContextEntity, resourceTypeId);
            }
        }
    }

    private ResourceEditProperty findByName(String propertyName,
            List<ResourceEditProperty> resourceEditProperties) {
        for (ResourceEditProperty property : resourceEditProperties) {
            if (property.getTechnicalKey().equals(propertyName)) {
                return property;
            }
        }
        log.info("Property " + propertyName + " is not in propertylist");
        throw new NoResultException("Could not find property " + propertyName);
    }

    public List<PropertyEditingService.DifferingProperty> getPropertyOverviewForResource(ResourceEntity resourceEntity, ResourceEditProperty property, List<ContextEntity> relevantContexts) {
        return propertyEditingService.getPropertyOverviewForResource(resourceEntity, property, relevantContexts);
    }

    public List<PropertyEditingService.DifferingProperty> getPropertyOverviewForResourceType(ResourceTypeEntity resourceTypeEntity, ResourceEditProperty property, List<ContextEntity> relevantContexts) {
        return propertyEditingService.getPropertyOverviewForResourceType(resourceTypeEntity, property, relevantContexts);
    }

    public List<PropertyEditingService.DifferingProperty> getPropertyOverviewForRelation(ResourceEditRelation relation, ResourceEditProperty property, List<ContextEntity> relevantContexts) {
        return propertyEditingService.getPropertyOverviewForRelation(relation, property, relevantContexts);
    }
}
