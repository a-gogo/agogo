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

package ch.puzzle.itc.mobiliar.presentation.functionEdit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import ch.puzzle.itc.mobiliar.common.exception.NotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import lombok.Getter;
import lombok.Setter;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.function.boundary.FunctionsBoundary;
import ch.puzzle.itc.mobiliar.business.function.entity.AmwFunctionEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.utils.Identifiable;
import ch.puzzle.itc.mobiliar.presentation.CompositeBackingBean;
import ch.puzzle.itc.mobiliar.presentation.common.context.SessionContext;
import ch.puzzle.itc.mobiliar.presentation.resourceRelation.events.ChangeSelectedRelationEvent;
import ch.puzzle.itc.mobiliar.presentation.util.GlobalMessageAppender;

@CompositeBackingBean
public class EditFunction implements Serializable {

    private class FunctionComparator implements Comparator<AmwFunctionEntity> {
        @Override
        public int compare(AmwFunctionEntity f1, AmwFunctionEntity f2) {
            if (f1.getName() == null && f2.getName() == null) {
                return 0;
            }
            if (f1.getName() == null) {
                return 1;
            }
            if (f2.getName() == null) {
                return -1;
            }
            return f1.getName().compareTo(f2.getName());
        }
    }

    @Inject
    SessionContext currentContext;

    @Inject
    FunctionsBoundary functionsBoundary;

    @Inject
    PermissionBoundary permissionBoundary;

    @Getter
    @Setter
    private Integer functionId;
    @Getter
    boolean canShowInstanceFunctions;
    @Getter
    boolean canShowSuperTypeFunctions;

    @Getter
    @Setter
    private Integer selectedFunctionIdToBeRemoved;

    private Identifiable resourceOrResourceType;

    private List<AmwFunctionEntity> instanceFunctions = new ArrayList<>();

    private List<AmwFunctionEntity> superTypeFunctions = new ArrayList<>();

    @Getter
    private Integer relationId;

    private Comparator<AmwFunctionEntity> comp = new FunctionComparator();

    /**
     * Observes if currently editing resource instance
     */
    public void onChangedResource(@Observes ResourceEntity resourceEntity) {
        if (resourceEntity != null) {
            resourceOrResourceType = resourceEntity;
            loadInstanceFunctionsFor(resourceEntity);
            loadSuperTypeFunctionsFor(resourceEntity);
        }
    }

    /**
     * Observes if currently editing resource type
     */
    public void onChangedResourceType(@Observes ResourceTypeEntity resourceTypeEntity) {
        if (resourceTypeEntity != null) {
            resourceOrResourceType = resourceTypeEntity;
            loadInstanceFunctionsFor(resourceTypeEntity);
            loadSuperTypeFunctionsFor(resourceTypeEntity);
        }
    }

    public void onChangedRelation(@Observes ChangeSelectedRelationEvent relation) {
        if (relation.getRelation() != null) {
            this.relationId = relation.getRelation().getResRelId();
        } else {
            this.relationId = null;
        }
    }

    /**
     * Load all functions which are defined on the resource
     */
    private void loadInstanceFunctionsFor(ResourceEntity resourceEntity) {
        instanceFunctions = functionsBoundary.getInstanceFunctions(resourceEntity);
        Collections.sort(instanceFunctions, comp);
    }

    /**
     * Load all functions which are defined on the resource type
     */
    private void loadInstanceFunctionsFor(ResourceTypeEntity resourceTypeEntity) {
        instanceFunctions = functionsBoundary.getInstanceFunctions(resourceTypeEntity);
        Collections.sort(instanceFunctions, comp);
    }

    /**
     * Load all functions which are defined on all super types of the resource
     */
    private void loadSuperTypeFunctionsFor(ResourceEntity resourceEntity) {
        superTypeFunctions = functionsBoundary.getAllOverwritableSupertypeFunctions(resourceEntity);
        Collections.sort(superTypeFunctions, comp);
    }

    /**
     * Load all functions which are defined on all super types of the resource type
     */
    private void loadSuperTypeFunctionsFor(ResourceTypeEntity resourceTypeEntity) {
        superTypeFunctions = functionsBoundary.getAllOverwritableSupertypeFunctions(resourceTypeEntity);
        Collections.sort(superTypeFunctions, comp);
    }

    /**
     * Observes context changes and update permissions accordingly
     */
    public void onChangedContext(@Observes ContextEntity contextEntity) {
        refreshPermissions();
    }

    @PostConstruct
    private void init() {
        refreshPermissions();
    }

    public Integer getResourceTypeId() {
        if (isCurrentFocusOnResourceType()) {
            return resourceOrResourceType.getId();
        }
        return null;
    }

    public Integer getResourceId() {
        if (isCurrentFocusOnResource()) {
            return resourceOrResourceType.getId();
        }
        return null;
    }

    private ResourceTypeEntity getResourceType() {
        if (isCurrentFocusOnResourceType()) {
            return (ResourceTypeEntity) resourceOrResourceType;
        }
        return null;
    }

    private ResourceEntity getResource() {
        if (isCurrentFocusOnResource()) {
            return (ResourceEntity) resourceOrResourceType;
        }
        return null;
    }

    /**
     * @return true if current focus is of {@link ResourceTypeEntity}; If not yet set then return false as
     * default
     */
    private boolean isCurrentFocusOnResourceType() {
        return resourceOrResourceType != null && resourceOrResourceType instanceof ResourceTypeEntity;
    }

    /**
     * @return true if current focus is of {@link ResourceEntity}; If not yet set then return true as
     * default
     */
    public boolean isCurrentFocusOnResource() {
        return resourceOrResourceType != null && resourceOrResourceType instanceof ResourceEntity;
    }

    private void refreshPermissions() {

        canShowInstanceFunctions = permissionBoundary.hasPermission(Permission.RESOURCE_AMWFUNCTION, Action.READ);
        canShowSuperTypeFunctions = permissionBoundary.hasPermission(Permission.RESOURCETYPE_AMWFUNCTION, Action.READ);
    }

    public boolean isCanAdd() {
        return currentContext.getIsGlobal() && canAddFunctions();
    }

    public boolean isCanEdit() {
        return currentContext.getIsGlobal() && canUpdateFunctions();
    }

    public boolean isCanOverwrite() {
        return currentContext.getIsGlobal() && canUpdateFunctions();
    }

    public boolean isCanDelete() {
        return currentContext.getIsGlobal() && canDeleteFunctions();
    }

    private boolean canUpdateFunctions() {
        if (isCurrentFocusOnResource()) {
            return permissionBoundary.canUpdateFunctionOfResourceOrResourceType(resourceOrResourceType.getId(), null);
        }
        if (isCurrentFocusOnResourceType()) {
            return permissionBoundary.canUpdateFunctionOfResourceOrResourceType(null, resourceOrResourceType.getId());
        }
        return false;
    }

    private boolean canAddFunctions() {
        if (isCurrentFocusOnResource()) {
            return permissionBoundary.canCreateFunctionOfResourceOrResourceType(resourceOrResourceType.getId(), null);
        }
        if (isCurrentFocusOnResourceType()) {
            return permissionBoundary.canCreateFunctionOfResourceOrResourceType(null, resourceOrResourceType.getId());
        }
        return false;
    }

    private boolean canDeleteFunctions() {
        if (isCurrentFocusOnResource()) {
            return permissionBoundary.canDeleteFunctionOfResourceOrResourceType(resourceOrResourceType.getId(), null);
        }
        if (isCurrentFocusOnResourceType()) {
            return permissionBoundary.canDeleteFunctionOfResourceOrResourceType(null, resourceOrResourceType.getId());
        }
        return false;
    }

    public void deleteFunction() {
        if (selectedFunctionIdToBeRemoved != null) {
            try {
                functionsBoundary.deleteFunction(selectedFunctionIdToBeRemoved);

                selectedFunctionIdToBeRemoved = null;

                if (isCurrentFocusOnResource()) {
                    loadInstanceFunctionsFor(getResource());
                    loadSuperTypeFunctionsFor(getResource());
                }

                if (isCurrentFocusOnResourceType()) {
                    loadInstanceFunctionsFor(getResourceType());
                    loadSuperTypeFunctionsFor(getResourceType());
                }

                GlobalMessageAppender.addSuccessMessage("Successfully deleted function");

            } catch (NotFoundException e) {
                GlobalMessageAppender.addErrorMessage(e.getMessage());
            } catch (ValidationException e) {
                GlobalMessageAppender.addErrorMessage(buildExceptionMessage(e));
            }
        } else {
            GlobalMessageAppender.addErrorMessage("No function selected to be deleted");
        }
    }

    private String buildExceptionMessage(ValidationException e) {
        StringBuilder sb = new StringBuilder("Could not delete function because it is overwritten"); // by at least one sub resource type or resource function");
        if (e.hasCausingObject() && e.getCausingObject() instanceof AmwFunctionEntity) {
            AmwFunctionEntity functionWithName = (AmwFunctionEntity) e.getCausingObject();

            if (functionWithName.isDefinedOnResource()) {
                sb.append(" on resource \"").append(functionWithName.getResource().getName())
                        .append("\"");
            }
            if (functionWithName.isDefinedOnResourceType()) {
                sb.append(" on resource type \"").append(functionWithName.getResourceType().getName())
                        .append("\"");
            }
        }
        return sb.toString();
    }

    /**
     * Get all Functions which are defined on current selected resource instance or Type
     */
    public List<AmwFunctionEntity> getResourceOrTypeInstanceFunctions() {
        return instanceFunctions;
    }

    /**
     * Get all Functions, which are defined on (super) resource type - overwritable
     */
    public List<AmwFunctionEntity> getSuperTypeFunctions() {
        return superTypeFunctions;
    }

    public boolean isRootResourceType() {
        return getResourceType() != null && getResourceType().isRootResourceType();
    }

    public String getInstanceTitle() {
        if (isCurrentFocusOnResourceType()) {
            return "Type functions";
        }
        return "Resource instance functions";
    }

    public String getResourceTypeTitle() {
        if (isCurrentFocusOnResourceType()) {
            return "Supertype functions";
        }
        return "Type functions";
    }

    public String getOverwritingInfo(AmwFunctionEntity function) {
        if (function.isOverwritingResourceTypeFunction()) {
            return "(Overwrite function from " + function.getOverwrittenFunctionResourceTypeName() + ")";
        }
        return "";
    }

    public String getFunctionOriginTypeInfo(AmwFunctionEntity function) {
        if (function.isDefinedOnResourceType()) {
            return "(Defined on " + function.getResourceType().getName() + ")";
        }
        return "";
    }
}
