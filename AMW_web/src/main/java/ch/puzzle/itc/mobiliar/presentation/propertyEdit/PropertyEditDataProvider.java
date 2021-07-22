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

package ch.puzzle.itc.mobiliar.presentation.propertyEdit;

import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableAttributesDTO;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.templates.AppServerRelationsTemplateProcessor;
import ch.puzzle.itc.mobiliar.business.property.boundary.PropertyEditor;
import ch.puzzle.itc.mobiliar.business.property.control.PropertyEditingService;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditProperty;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditRelation;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.NamedIdentifiable;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroup;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.boundary.SoftlinkRelationBoundary;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.presentation.CompositeBackingBean;
import ch.puzzle.itc.mobiliar.presentation.Selected;
import ch.puzzle.itc.mobiliar.presentation.common.ContextDataProvider;
import ch.puzzle.itc.mobiliar.presentation.resourceRelation.ResourceRelationModel;
import ch.puzzle.itc.mobiliar.presentation.resourceRelation.events.ChangeSelectedRelationEvent;
import ch.puzzle.itc.mobiliar.presentation.resourcesedit.DataProviderHelper;
import ch.puzzle.itc.mobiliar.presentation.resourcesedit.EditResourceView;
import ch.puzzle.itc.mobiliar.presentation.util.TestingMode;
import ch.puzzle.itc.mobiliar.presentation.util.UserSettings;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;

@CompositeBackingBean
public class PropertyEditDataProvider implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    PropertyEditor editor;

    @Inject
    PermissionBoundary permissionBoundary;

    @Inject
    private UserSettings userSettings;

    @Inject
    @Selected
    private ContextEntity currentContext;

    @Inject
    EditResourceView resourceView;

    @Inject
    ContextDataProvider contextDataProvider;

    List<ResourceEditProperty> resourceEditProperties;

    List<ResourceEditProperty> filteredResourceProperties;

    List<ResourceEditProperty> currentRelationProperties;

    List<ResourceEditProperty> filteredRelationProperties;

    @Getter
    List<PropertyEditingService.DifferingProperty> valuesForConfigOverview;

    @Getter
    ResourceEditProperty propertyForConfigOverview;

    @Getter
    private boolean showWarningForPotentialPropertyOverwriting;

    @Getter
    private boolean editableProperties = false;

    @Getter
    private boolean canDecryptProperties = false;

    @Getter
    private boolean allowedToEditProperties = false;

    @Getter
    private boolean canChangeRuntime = false;

    @Inject
    ResourceRelationModel resourceRelation;

    @Inject
    ActiveApplicationsDataProvider activeApplications;

    @Inject
    SoftlinkRelationBoundary softlinkRelationBoundary;

    @Getter
    private ResourceGroup group;

    @Inject
    @TestingMode
    private Boolean testing;

    @TestingMode
    public void onChangedTestingMode(@Observes Boolean isTesting) {
        this.testing = isTesting;
    }

    @Getter
    @Setter
    String typeRelationIdentifier;

    @Getter
    @Setter
    String relationIdentifier;

    protected DataProviderHelper helper = new DataProviderHelper();

    private NamedIdentifiable resourceOrResourceType;

    private ResourceEditRelation currentRelation;

    /**
     * @return true if testing is tue and initialized, otherwise false
     */
    public boolean isTesting() {
        return testing != null && testing;
    }

    public boolean isGlobalContext() {
        return currentContext.isGlobal();
    }

    public void onContextChanged(@Observes ContextEntity contextEntity) {
        if (isCurrentFocusOnResource()) {
            onChangedResource((ResourceEntity) resourceOrResourceType);
        } else if (isCurrentFocusOnResourceType()) {
            onChangedResourceType((ResourceTypeEntity) resourceOrResourceType);
        }
        this.currentContext = contextEntity;
    }

    public void onChangedResource(@Observes ResourceEntity resourceEntity) {
        reloadResourceEditProperties(resourceEntity);
    }

    public void onChangedResourceType(@Observes ResourceTypeEntity resourceTypeEntity) {
        reloadResourceTypeEditProperties(resourceTypeEntity);
    }

    public Integer getContextId() {
        return currentContext != null ? currentContext.getId() : null;
    }

    public void onChangedRelation(@Observes ChangeSelectedRelationEvent relation) {
        this.currentRelation = relation.getRelation();
        if (resourceOrResourceType != null) {
            loadResourceRelationEditProperties();
        }
    }

    public boolean hasEditableIdentifier() {
        return currentRelation != null && currentRelation.getMode().equals(ResourceEditRelation.Mode.CONSUMED)
                && !currentRelation.getSlaveTypeName().equals("RUNTIME");
    }

    public void loadConfigOverviewForProperty(ResourceEditProperty property) {
        this.propertyForConfigOverview = property;
        List<ContextEntity> relevantContexts = contextDataProvider.getChildrenForContext(currentContext.getId());
        if (property.getLoadedFor() == ResourceEditProperty.Origin.INSTANCE) {
            valuesForConfigOverview = editor.getPropertyOverviewForResource(resourceView.getResource(), property, relevantContexts);
            showWarningForPotentialPropertyOverwriting = false;
        } else if (property.getLoadedFor() == ResourceEditProperty.Origin.RELATION) {
            valuesForConfigOverview = editor.getPropertyOverviewForRelation(currentRelation, property, relevantContexts);
            showWarningForPotentialPropertyOverwriting = false;
            for (PropertyEditingService.DifferingProperty differingProperty : valuesForConfigOverview) {
                // only show warning if Properties defined directly on the Resource would be overwritten
                if (differingProperty.getOrigin() == ResourceEditProperty.Origin.INSTANCE) {
                    showWarningForPotentialPropertyOverwriting = true;
                    break;
                }
            }
        }
    }

    public boolean hasOverwrittenProperty(ResourceEditProperty property) {
        List<ContextEntity> relevantContexts = contextDataProvider.getChildrenForContext(currentContext.getId());
        if (property.getLoadedFor() == ResourceEditProperty.Origin.INSTANCE) {
            return !editor.getPropertyOverviewForResource(resourceView.getResource(), property, relevantContexts).isEmpty();
        } else if (property.getLoadedFor() == ResourceEditProperty.Origin.TYPE) {
            return !editor.getPropertyOverviewForResourceType(resourceView.getResourceType(), property, relevantContexts).isEmpty();
        }
        return !editor.getPropertyOverviewForRelation(currentRelation, property, relevantContexts).isEmpty();
    }

    private void loadResourceRelationEditProperties() {
        filteredRelationProperties = new ArrayList<>();

        if (currentRelation != null) {
            if (currentRelation.isResourceTypeRelation()) {
                List<ResourceEditProperty> properties = editor.getPropertiesForRelatedResourceType(currentRelation, getContextId());
                currentRelationProperties = userSettings.filterTestingProperties(properties);
                typeRelationIdentifier = currentRelation.getDisplayName();
            } else {
                currentRelationProperties = userSettings.filterTestingProperties(editor
                        .getPropertiesForRelatedResource(getResourceId(), currentRelation, getContextId()));
                filterHostNameAndActiveFromRelatedNode(currentRelation);
                relationIdentifier = currentRelation.getIdentifier();
            }
        } else {
            currentRelationProperties = new ArrayList<>();
        }
        filterHostNameAndActiveFromNode();
    }

    private void filterHostNameAndActiveFromNode() {
        if (isNodeOrNodeType() && !currentContext.isEnvironment() && resourceEditProperties != null) {
            filterHostNameAndActive(resourceEditProperties);
        }
    }

    private boolean isNodeOrNodeType() {
        return isNode() || isNodeType();
    }

    private void filterHostNameAndActiveFromRelatedNode(ResourceEditRelation relation) {
        if (relation.getSlaveTypeName().equals(DefaultResourceTypeDefinition.NODE.name())
                && !currentContext.isEnvironment() && currentRelationProperties != null) {
            filterHostNameAndActive(currentRelationProperties);
        }
    }

    private void filterHostNameAndActive(List<ResourceEditProperty> props) {
        Iterator<ResourceEditProperty> it = props.iterator();
        while (it.hasNext()) {
            ResourceEditProperty p = it.next();
            if (AppServerRelationsTemplateProcessor.HOST_NAME.equals(p.getTechnicalKey())
                    || AppServerRelationsTemplateProcessor.NODE_ACTIVE.equals(p.getTechnicalKey())) {
                filteredRelationProperties.add(p);
                it.remove();
            }
        }
    }

    public void save() throws AMWException, ForeignableOwnerViolationException, ValidationException {
        // play back the filtered properties - otherwise they will be deleted.
        resourceEditProperties.addAll(filteredResourceProperties);
        if (currentRelationProperties != null && filteredRelationProperties != null) {
            currentRelationProperties.addAll(filteredRelationProperties);
        }

        List<ResourceEditProperty> relationPropertiesToSave = new ArrayList<>();
        if (currentRelationProperties != null) {
            relationPropertiesToSave.addAll(currentRelationProperties);
        }

        activeApplications.save();

        if (isCurrentFocusOnResource()) {
            // there can be several consumed relations with identical slave resourceType
            if (currentRelation != null && currentRelation.getMode().equals(ResourceEditRelation.Mode.CONSUMED)) {
                // get next available identifier if the actual identifier is empty and has not been empty before
                if (StringUtils.isEmpty(relationIdentifier)
                        && StringUtils.isNotEmpty(currentRelation.getIdentifier())
                        && resourceRelation.isDefaultResourceType()) {
                    List<ResourceEditRelation> relationsWithSameSlaveGroup = helper.relationsWithSameSlaveGroup(
                            helper.flattenMap(resourceRelation.getConsumedRelations()), currentRelation.getSlaveGroupId());
                    // first relation shall can have an empty identifier
                    if (relationsWithSameSlaveGroup.size() > 1) {
                        relationIdentifier = helper.nextFreeIdentifierForResourceEditRelations(
                                relationsWithSameSlaveGroup, currentRelation.getSlaveGroupId(), currentRelation.getSlaveName());
                    }
                } else {
                    preventDuplicateIdentifiers();
                }
            } else {
                relationIdentifier = null;
            }

            editor.save(ForeignableOwner.getSystemOwner(), getContextId(), getResourceId(), resourceEditProperties,
                    resourceRelation.getCurrentResourceRelation(), relationPropertiesToSave,
                    getNameOfResourceOrResourceType(), relationIdentifier);

        } else {
            editor.savePropertiesForResourceType(getContextId(), getResourceTypeId(),
                    resourceEditProperties, resourceRelation.getCurrentResourceRelation(),
                    relationPropertiesToSave,
                    getNameOfResourceOrResourceType(),
                    typeRelationIdentifier);
        }
        reloadResourceEditProperties();
    }

    private void preventDuplicateIdentifiers() throws AMWException {
        Collection<List<ResourceEditRelation>> resourceEditRelations = resourceRelation.getConsumedRelations().values();
        for (List<ResourceEditRelation> consumedRelations : resourceEditRelations) {
            for (ResourceEditRelation consumedRelation : consumedRelations) {
                if (consumedRelation.getSlaveName().equals(relationIdentifier)) {
                    throw new AMWException("RelationName '" + relationIdentifier + "' is not allowed");
                }
                if (consumedRelation.getIdentifier() != null && consumedRelation.getIdentifier().equals(relationIdentifier) && !consumedRelation.equals(resourceRelation.getResourceRelation())) {
                    throw new AMWException("RelationName '" + relationIdentifier + "' is already taken");
                }
            }
        }
    }

    private String getNameOfResourceOrResourceType() {
        return resourceOrResourceType != null ? resourceOrResourceType.getName() : null;
    }

    public boolean isApplicationServer() {
        return isCurrentFocusOnResource()
                && ((ResourceEntity) resourceOrResourceType).getResourceType().isApplicationServerResourceType();
    }

    public boolean isApplication() {
        return isCurrentFocusOnResource()
                && ((ResourceEntity) resourceOrResourceType).getResourceType().isApplicationResourceType();
    }

    public boolean isNode() {
        return isCurrentFocusOnResource()
                && ((ResourceEntity) resourceOrResourceType).getResourceType().isNodeResourceType();
    }

    public boolean isNodeType() {
        return isCurrentFocusOnResourceType()
                && ((ResourceTypeEntity) resourceOrResourceType).isNodeResourceType();
    }

    /**
     * @return true if current focus is of {@link ResourceTypeEntity}; If not yet set then return false as
     * default
     */
    private boolean isCurrentFocusOnResourceType() {
        return resourceOrResourceType != null && resourceOrResourceType instanceof ResourceTypeEntity;
    }

    /**
     * @return true if current focus is of {@link ResourceEntity}; If not yet set then return true as default
     */
    public boolean isCurrentFocusOnResource() {
        return resourceOrResourceType != null && resourceOrResourceType instanceof ResourceEntity;
    }

    private Integer getResourceTypeId() {
        if (isCurrentFocusOnResourceType()) {
            return resourceOrResourceType.getId();
        }
        return null;
    }

    private Integer getResourceId() {
        if (isCurrentFocusOnResource()) {
            return resourceOrResourceType.getId();
        }
        return null;
    }

    public boolean hasEditRelationNamePermission() {
        return isCurrentFocusOnResourceType() && permissionBoundary.hasPermission(Permission.RESOURCETYPE, currentContext,
                Action.UPDATE, null, (ResourceTypeEntity) resourceOrResourceType);
    }

    public boolean isLongValue(String value) {
        return (value != null && value.length() > 70);
    }

    public ForeignableAttributesDTO getForeignableToEdit(ResourceEditProperty property) {
        if (property != null) {
            return new ForeignableAttributesDTO(property.getFcOwner(), property.getFcExternalKey(), property.getFcExternalLink());
        }
        return new ForeignableAttributesDTO();
    }

    public List<ResourceEditProperty> getResourceEditProperties() {
        List<ResourceEditProperty> sortedList = new ArrayList<>(resourceEditProperties);
        Collections.sort(sortedList);
        return sortedList;
    }

    public void reloadResourceEditProperties() {
        if (resourceView.getResource() != null) {
            reloadResourceEditProperties(resourceView.getResource());
        } else if (resourceView.getResourceType() != null) {
            reloadResourceTypeEditProperties(resourceView.getResourceType());
        }
    }

    private List<ResourceEditProperty> reloadResourceEditProperties(ResourceEntity resourceEntity) {
        filteredResourceProperties = new ArrayList<>();
        List<ResourceEditProperty> propertiesForResource = editor.getPropertiesForResource(resourceEntity.getId(), getContextId());
        resourceEditProperties = userSettings.filterTestingProperties(propertiesForResource);
        filterHostNameAndActiveFromNode();
        editableProperties = permissionBoundary.hasPermissionToEditPropertiesByResourceAndContext(resourceEntity.getId(),
                currentContext, userSettings.isTestingMode());
        canChangeRuntime = permissionBoundary.hasPermission(Permission.RESOURCE, currentContext, Action.UPDATE,
                resourceEntity, null);
        canDecryptProperties = permissionBoundary.hasPermission(Permission.RESOURCE_PROPERTY_DECRYPT, currentContext, Action.ALL,
                resourceEntity, null);
        group = ResourceGroup.createByResource(resourceEntity.getResourceGroup());

        resourceOrResourceType = resourceEntity;

        loadResourceRelationEditProperties();
        return filteredResourceProperties;
    }

    private List<ResourceEditProperty> reloadResourceTypeEditProperties(ResourceTypeEntity resourceTypeEntity) {
        filteredResourceProperties = new ArrayList<>();
        resourceEditProperties = userSettings.filterTestingProperties(editor.getPropertiesForResourceType(
                resourceTypeEntity.getId(), getContextId()));
        editableProperties = permissionBoundary.hasPermission(Permission.RESOURCETYPE, currentContext, Action.UPDATE,
                null, resourceTypeEntity);
        canDecryptProperties = permissionBoundary.hasPermission(Permission.RESOURCETYPE_PROPERTY_DECRYPT, currentContext, Action.ALL,
                null, resourceTypeEntity);
        canChangeRuntime = false;
        group = null;
        resourceOrResourceType = resourceTypeEntity;

        loadResourceRelationEditProperties();
        return resourceEditProperties;
    }

    public List<ResourceEditProperty> getCurrentRelationProperties() {
        List<ResourceEditProperty> sortedList = new ArrayList<>(currentRelationProperties);
        Collections.sort(sortedList);
        return sortedList;
    }
}
