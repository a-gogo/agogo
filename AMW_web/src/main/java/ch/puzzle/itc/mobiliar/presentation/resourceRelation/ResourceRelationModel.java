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

package ch.puzzle.itc.mobiliar.presentation.resourceRelation;

import ch.puzzle.itc.mobiliar.business.domain.commons.CommonDomainService;
import ch.puzzle.itc.mobiliar.business.foreignable.boundary.ForeignableBoundary;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableAttributesDTO;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.property.boundary.PropertyEditor;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditRelation;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditRelation.Mode;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.boundary.ResourceRelationBoundary;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationService;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.boundary.SoftlinkRelationBoundary;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.entity.SoftlinkRelationEntity;
import ch.puzzle.itc.mobiliar.business.utils.Identifiable;
import ch.puzzle.itc.mobiliar.business.utils.ValidationException;
import ch.puzzle.itc.mobiliar.common.exception.GeneralDBException;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.presentation.common.context.SessionContext;
import ch.puzzle.itc.mobiliar.presentation.resourceRelation.events.ChangeSelectedRelationEvent;
import ch.puzzle.itc.mobiliar.presentation.resourcesedit.events.SelectedRelationId;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

@Named
@ViewScoped
public class ResourceRelationModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    Logger log;

    @Inject
    PropertyEditor editor;

    @Inject
    ResourceRelationBoundary resourceRelationBoundary;

    @Inject
    CommonDomainService commonDomainService;

    @Inject
    ResourceRelationService resourceRelationService;

    @Inject
    PermissionBoundary permissionBoundary;

    @Inject
    ForeignableBoundary foreignableBoundary;

    @Inject
    SessionContext sessionContext;

    @Inject
    ResourceLocator resourceLocator;

    @Inject
    SoftlinkRelationBoundary softlinkRelationBoundary;

    private Map<Mode, List<ResourceEditRelation>> resourceRelations;

    @Getter
    private Map<String, SortedSet<String>> unresolvedRelations;

    @Getter
    private List<ResourceEditRelation> runtimeRelations;

    @Getter
    private SortedMap<String, List<ResourceEditRelation>> consumedRelations;

    @Getter
    private List<ResourceEditRelation> consumedApplications;

    @Getter
    private SortedMap<String, List<ResourceEditRelation>> providedRelations;

    @Getter
    private SortedMap<String, List<ResourceEditRelation>> resourceTypeRelations;

    // Map<resourceGroupId, Map<Identifier, List<ResourceEditRelation>>>
    private Map<Integer, Map<String, List<ResourceEditRelation>>> consumedGroupIdentifierMap = new HashMap<>();
    private Map<Integer, Map<String, List<ResourceEditRelation>>> providedGroupIdentifierMap = new HashMap<>();

    @Getter
    private ResourceEditRelation removeResourceRelation;

    public void setResourceRelationForRemoval(ResourceEditRelation relation) {
        this.removeResourceRelation = relation;
    }

    @Getter
    @Setter
    private ResourceEditRelation currentResourceRelation;

    @Getter
    private boolean allowedToAddRelations = false;

    @Getter
    private boolean allowedToRemoveRelations = false;

    @Getter
    private boolean allowedToAddProvidedResources = false;

    @Getter
    private boolean allowedToSelectRuntime = false;

    @Getter
    private boolean allowedToListConsumedRelations = false;

    @Getter
    private boolean allowedToListProvidedRelations = false;

    @Getter
    private boolean allowedToListResourceTypeRelations = false;

    @Getter
    private boolean canShowSoftlinkRelations = false;

    @Getter
    private boolean canEditSoftlinkRelation = false;

    @Getter
    private boolean canShowAddSoftlinkRelationButton = false;

    @Getter
    private boolean allowedToJumpToRelatedResourceEditScreen = false;


    @Getter
    private boolean canAddConsumedRelations;

    @Getter
    private boolean canAddProvidedRelations;

    private Integer currentRelationId;

    private Identifiable currentSelectedResourceOrType;

    @Getter
    private SoftlinkRelationModel softlinkRelation;

    @Getter
    private boolean isSoftlinkRelationSelected = false;

    @Inject
    Event<ChangeSelectedRelationEvent> changeSelectedRelationEvent;

    @PostConstruct
    public void init() {
        allowedToListConsumedRelations = permissionBoundary.hasPermission(Permission.CONSUMED_RES_LIST);
        allowedToListProvidedRelations = permissionBoundary.hasPermission(Permission.PROVIDED_RES_LIST);
        allowedToListResourceTypeRelations = permissionBoundary
                .hasPermission(Permission.REL_RESTYPE_PANEL_LIST);
        allowedToJumpToRelatedResourceEditScreen = permissionBoundary.hasPermission(Permission.EDIT_RES);
    }

    public void onChangedResource(@Observes ResourceEntity resourceEntity) throws GeneralDBException {
        allowedToSelectRuntime = permissionBoundary.hasPermission(Permission.SELECT_RUNTIME);
        allowedToRemoveRelations = permissionBoundary.hasPermissionToDeleteRelation(resourceEntity);
        canAddConsumedRelations = permissionBoundary.hasPermissionToAddRelation(resourceEntity, false);
        canAddProvidedRelations = permissionBoundary.hasPermissionToAddRelation(resourceEntity, true);
        allowedToAddRelations = canAddConsumedRelations || canAddProvidedRelations;
        currentSelectedResourceOrType = resourceEntity;

        canShowSoftlinkRelations = sessionContext.getIsGlobal()
                && hasConsumableSoftlinkSuperType(resourceEntity)
                && getResourceSoftlinkRelation() != null;


        canShowAddSoftlinkRelationButton = sessionContext.getIsGlobal()
                && getResourceSoftlinkRelation() == null
                && hasConsumableSoftlinkSuperType(resourceEntity)
                && permissionBoundary.hasPermission(Permission.SET_SOFTLINK_ID_OR_REF);

        canEditSoftlinkRelation = sessionContext.getIsGlobal()
                && getResourceSoftlinkRelation() != null
                && hasConsumableSoftlinkSuperType(resourceEntity)
                && foreignableBoundary.isModifiableByOwner(ForeignableOwner.getSystemOwner(), createForeignableDto(getResourceSoftlinkRelation()))
                && permissionBoundary.hasPermission(Permission.SET_SOFTLINK_ID_OR_REF);

        reloadValues();
    }

    private ForeignableAttributesDTO createForeignableDto(SoftlinkRelationEntity resourceSoftlinkRelation) {
        if (resourceSoftlinkRelation != null) {
            return new ForeignableAttributesDTO(resourceSoftlinkRelation.getOwner(), resourceSoftlinkRelation.getExternalKey(), resourceSoftlinkRelation.getExternalLink());
        }
        return null;
    }

    public void onChangedResourceType(@Observes ResourceTypeEntity resourceTypeEntity)
            throws GeneralDBException {
        allowedToSelectRuntime = false;
        canAddConsumedRelations = false;
        canAddProvidedRelations = false;
        allowedToRemoveRelations = permissionBoundary.hasPermissionToDeleteRelationType(resourceTypeEntity);
        // FIXME: find the correct permission
        allowedToAddRelations = true;
        currentSelectedResourceOrType = resourceTypeEntity;
        canShowSoftlinkRelations = false;
        canShowAddSoftlinkRelationButton = false;
        canEditSoftlinkRelation = false;
        reloadValues();
    }

    @SelectedRelationId
    public void onChangedRelationIdViewParam(@Observes Integer relationIdViewParam) {
        this.currentRelationId = relationIdViewParam;
    }

    public void reloadValues() {
        consumedApplications = null;
        softlinkRelation = null;

        if (isResourceSelected()) {
            setResourceRelations(editor.getRelationsForResource(currentSelectedResourceOrType.getId()));
            mapUnresolvedRelations();
            resolveSoftlinkRelation();
        }

        if (isResourceTypeSelected()) {
            setResourceRelations(editor.getRelationsForResourceType(currentSelectedResourceOrType.getId()));
        }

        findAndLoadSelectedRelation();
    }

    private void resolveSoftlinkRelation() {
        SoftlinkRelationEntity currentResourceSoftlinkRelation = getResourceSoftlinkRelation();
        if (currentResourceSoftlinkRelation != null) {
            softlinkRelation = new SoftlinkRelationModel(currentResourceSoftlinkRelation);
            softlinkRelation.setSoftlinkResolvingSlaveResource(softlinkRelationBoundary.getSoftlinkResolvableSlaveResource(currentResourceSoftlinkRelation,
                    currentResourceSoftlinkRelation.getCpiResource().getRelease()));
        }
    }


    private void findAndLoadSelectedRelation() {
        ResourceEditRelation selectedRel = findSelectedOrBestmatchingRelation();

        if (selectedRel == null && canShowSoftlinkRelations) {
            selectSoftlinkResourceRelation();
        } else {
            loadResourceRelation(selectedRel);
        }
    }

    private ResourceEditRelation findSelectedOrBestmatchingRelation() {
        ResourceEditRelation selectedRel = null;
        // find already selected relation
        if (currentRelationId != null || getCurrentResourceRelationId() != null) {
            selectedRel = findCurrentSelectedRelation();
        }
        if (selectedRel == null && isResourceSelected()) {
            selectedRel = getFirstRelationForSelectedResource();
        }
        if (selectedRel == null && isResourceTypeSelected()) {
            selectedRel = getFirstRelationForSelectedResourceType();
        }

        return selectedRel;
    }

    private ResourceEditRelation getFirstRelationForSelectedResource() {
        if (hasConsumedRelations()) {

            if (hasRuntimeRealtions()) {
                return runtimeRelations.iterator().next();
            }

            return consumedRelations.get(consumedRelations.keySet().iterator().next()).iterator().next();
        }

        if (hasProvidedRelations()) {
            return providedRelations.get(providedRelations.keySet().iterator().next()).iterator().next();
        }

        return null;
    }

    private ResourceEditRelation getFirstRelationForSelectedResourceType() {
        if (hasResourceTypeRelations()) {
            return resourceTypeRelations.get(resourceTypeRelations.keySet().iterator().next()).iterator().next();
        }
        return null;
    }

    private boolean hasRuntimeRealtions() {
        return runtimeRelations != null && !runtimeRelations.isEmpty();
    }

    private boolean hasProvidedRelations() {
        return providedRelations != null && !providedRelations.isEmpty();
    }

    private boolean hasConsumedRelations() {
        return consumedRelations != null && !consumedRelations.isEmpty();
    }

    private boolean hasResourceTypeRelations() {
        return resourceTypeRelations != null && !resourceTypeRelations.isEmpty();
    }


    private ResourceEditRelation findCurrentSelectedRelation() {
        List<ResourceEditRelation> allRelations = new ArrayList<>();
        if (isResourceSelected()) {
            allRelations.addAll(resourceRelations.get(Mode.CONSUMED) != null ? resourceRelations.get(Mode.CONSUMED) : Collections.<ResourceEditRelation>emptyList());
            allRelations.addAll(resourceRelations.get(Mode.PROVIDED) != null ? resourceRelations.get(Mode.PROVIDED) : Collections.<ResourceEditRelation>emptyList());
        } else {
            allRelations.addAll(resourceRelations.get(Mode.TYPE));
        }
        for (ResourceEditRelation relation : allRelations) {

            Integer currentResourceRelationId = getSelectedResourceRelationId();
            if (currentResourceRelationId != null && currentResourceRelationId.equals(relation.getUniqueIdentifier())) {
                return relation;
            }
        }
        return null;
    }

    private boolean isResourceSelected() {
        return currentSelectedResourceOrType != null && currentSelectedResourceOrType instanceof ResourceEntity;
    }

    private boolean isResourceTypeSelected() {
        return currentSelectedResourceOrType != null && currentSelectedResourceOrType instanceof ResourceTypeEntity;
    }

    /**
     * JSF is not able to handle objects within selectboxes - we therefore have to make a round trip over
     * the relations id...
     *
     * @return
     */
    public Integer getCurrentResourceRelationId() {
        return currentResourceRelation != null ? currentResourceRelation.getResRelId() : null;
    }

    public Integer getCurrentRelationUniqueIdentifier() {
        return currentResourceRelation != null ? currentResourceRelation.getUniqueIdentifier() : null;
    }

    public void setCurrentResourceRelationId(Integer id) {
        // In the context this method is used, only available release relations
        // are possible...
        for (ResourceEditRelation relation : getAvailableReleaseRelations()) {
            if (id != null && id.equals(relation.getResRelId())) {
                if (consumedRelations.containsKey(relation.getSlaveTypeName())) {
                    // replace selected value in map
                    List<ResourceEditRelation> oldSelections = consumedRelations.remove(relation.getSlaveTypeName());
                    consumedRelations.put(relation.getSlaveTypeName(), replaceSelectedRelationForType(relation, oldSelections));
                }

                if (providedRelations.containsKey(relation.getSlaveTypeName())) {
                    // replace selected value in map
                    List<ResourceEditRelation> oldSelections = providedRelations.remove(relation.getSlaveTypeName());
                    providedRelations.put(relation.getSlaveTypeName(), replaceSelectedRelationForType(relation, oldSelections));
                }

                loadResourceRelation(relation);
                break;
            }
        }
    }

    private List<ResourceEditRelation> replaceSelectedRelationForType(ResourceEditRelation selectedRelation, List<ResourceEditRelation> oldSelections) {
        List<ResourceEditRelation> newSelections = new ArrayList<>();
        for (ResourceEditRelation relation : oldSelections) {
            if (relation.getIdentifier() != null ? relation.getIdentifier().equals(selectedRelation.getIdentifier()) : selectedRelation.getIdentifier() == null) {
                newSelections.add(selectedRelation);
            } else {
                newSelections.add(relation);
            }
        }
        return newSelections;
    }

    public void loadResourceRelation(ResourceEditRelation resourceRelation) {
        isSoftlinkRelationSelected = false;

        boolean switchedRelation = !isCurrentRelation(resourceRelation);
        if (resourceRelation == null) {
            this.currentResourceRelation = null;
        } else if (switchedRelation) {
            this.currentResourceRelation = resourceRelation;
        }

        if (switchedRelation) {
            changeSelectedRelationEvent.fire(new ChangeSelectedRelationEvent(resourceRelation));
        }
    }

    public void selectSoftlinkResourceRelation() {
        if (!isSoftlinkRelationSelected) {
            this.currentResourceRelation = null;
            isSoftlinkRelationSelected = true;
        }
    }

    public boolean isActiveRelation(ResourceEditRelation resourceRelation) {
        Integer current = currentResourceRelation != null ? currentResourceRelation.getUniqueIdentifier() : null;
        Integer other = resourceRelation != null ? resourceRelation.getUniqueIdentifier() : null;
        if (current != null && current.equals(other)) {
            return true;
        }
        return false;
    }

    public boolean isCurrentRelation(ResourceEditRelation resourceRelation) {
        if (resourceRelation == null) {
            return this.currentResourceRelation == null;
        }
        return resourceRelation.equals(this.currentResourceRelation);
    }

    public boolean isConsumedRelation() {
        return this.currentResourceRelation != null
                && this.currentResourceRelation.getMode() == Mode.CONSUMED;
    }

    public boolean isTypeRelation() {
        return this.currentResourceRelation != null
                && this.currentResourceRelation.getMode() == Mode.TYPE;
    }

    public ResourceEditRelation getResourceRelation() {
        return currentResourceRelation;
    }

    public List<String> getUnresolvedHeaders() {
        return unresolvedRelations != null ? new ArrayList<String>(unresolvedRelations.keySet())
                : new ArrayList<String>();
    }

    public List<String> getUnresolvedRelations(String header) {
        if (header != null && !header.isEmpty()) {
            SortedSet<String> sortedSet = unresolvedRelations.get(header);
            return sortedSet != null ? new ArrayList<String>(sortedSet) : null;
        }
        return null;
    }

    public List<ResourceEditRelation> getAvailableReleaseRelations() {
        if (currentResourceRelation != null) {
            Map<String, List<ResourceEditRelation>> relMap = null;
            SortedSet<ResourceEditRelation> relSet = new TreeSet<>(ResourceEditRelation.releaseComparator());
            switch (currentResourceRelation.getMode()) {
                case CONSUMED:
                    relMap = consumedGroupIdentifierMap.get(currentResourceRelation.getSlaveGroupId());
                    break;
                case PROVIDED:
                    relMap = providedGroupIdentifierMap.get(currentResourceRelation.getSlaveGroupId());
                    break;
                default:
            }
            if (relMap != null) {
                relSet.addAll(relMap.get(currentResourceRelation.getQualifiedIdentifier()));
            }
            return new ArrayList<>(relSet);
        }
        return new ArrayList<>();
    }

    private void setResourceRelations(Map<Mode, List<ResourceEditRelation>> relations) {
        this.resourceRelations = relations;
        if (resourceRelations != null) {
            if (isResourceSelected()) {
                mapRelationsForResource();
            } else {
                mapTypeRelations();
            }
        }
    }

    private void mapRelationsForResource() {
        mapRelations(Mode.CONSUMED);
        mapRelations(Mode.PROVIDED);
    }

    private void mapTypeRelations() {
        resourceTypeRelations = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        for (ResourceEditRelation rel : resourceRelations.get(Mode.TYPE)) {
            List<ResourceEditRelation> list = new ArrayList<>();
            list.add(rel);
            resourceTypeRelations.put(rel.getDisplayName(), list);
        }
    }

    private void mapRelations(Mode mode) {
        Map<Integer, Map<String, List<ResourceEditRelation>>> groupIdentifierMap = new HashMap<Integer, Map<String, List<ResourceEditRelation>>>();
        SortedMap<String, SortedMap<String, List<ResourceEditRelation>>> mapForNavigation = new TreeMap(String.CASE_INSENSITIVE_ORDER);

        if (resourceRelations.get(mode) != null) {
            // group by resourceGroupId and identifier
            for (ResourceEditRelation rel : resourceRelations.get(mode)) {
                Integer groupId = rel.getSlaveGroupId();
                String identifier = rel.getQualifiedIdentifier();
                String type = rel.getSlaveTypeName();
                if (!groupIdentifierMap.containsKey(groupId)) {
                    groupIdentifierMap.put(groupId,
                            new HashMap<String, List<ResourceEditRelation>>());
                }
                if (!groupIdentifierMap.get(groupId).containsKey(identifier)) {
                    groupIdentifierMap.get(groupId).put(identifier, new ArrayList<ResourceEditRelation>());
                    // put the first in the navigation map - it does not matter
                    // which exact relation it
                    // is
                    // because we use only values that are identical for
                    // relations with same group and
                    // identifier
                    if (!mapForNavigation.containsKey(type)) {
                        mapForNavigation.put(rel.getSlaveTypeName(), new TreeMap(String.CASE_INSENSITIVE_ORDER));
                    }

                    if (!mapForNavigation.get(type).containsKey(rel.getDisplayName())) {
                        mapForNavigation.get(type).put(rel.getDisplayName(), new ArrayList<ResourceEditRelation>());
                    }

                }
                // put now all release relations to list for given type
                mapForNavigation.get(type).get(rel.getDisplayName()).add(rel);
                groupIdentifierMap.get(groupId).get(identifier).add(rel);
            }

            setBestMatchingOrSelectedRelationsToViewMaps(mode, groupIdentifierMap, mapForNavigation);
            setApplicationsAndRuntimes(mapForNavigation);
        }
    }

    private void setApplicationsAndRuntimes(SortedMap<String, SortedMap<String, List<ResourceEditRelation>>> mapForNavigation) {
        ResourceTypeEntity resourceTypeOfSelectedResource = getCurrentSelectedResource().getResourceType();
        if (resourceTypeOfSelectedResource.isApplicationServerResourceType()) {

            for (String key : mapForNavigation.keySet()) {
                if (key.equals(DefaultResourceTypeDefinition.APPLICATION.name())) {
                    for (List<ResourceEditRelation> applications : new ArrayList<>(mapForNavigation.get(key).values())) {
                        if (consumedApplications == null) {
                            consumedApplications = new ArrayList<>();
                        }
                        consumedApplications.add(resourceRelationService.getBestMatchingRelationRelease(applications, (ResourceEntity) currentSelectedResourceOrType));
                    }
                }

                if (key.equals(ResourceTypeEntity.RUNTIME)) {
                    runtimeRelations = unmapToList(mapForNavigation.get(key).values());
                }
            }
        }
    }


    private void setBestMatchingOrSelectedRelationsToViewMaps(Mode mode, Map<Integer, Map<String, List<ResourceEditRelation>>> groupIdentifierMap, SortedMap<String, SortedMap<String, List<ResourceEditRelation>>> mapForNavigation) {
        switch (mode) {
            case CONSUMED:
                consumedGroupIdentifierMap = groupIdentifierMap;
                consumedRelations = new TreeMap(String.CASE_INSENSITIVE_ORDER);
                for (String key : mapForNavigation.keySet()) {
                    if (!key.equals(DefaultResourceTypeDefinition.APPLICATION.name()) && !key.equals(ResourceTypeEntity.RUNTIME)) {
                        consumedRelations.put(key, findBestMatchingOrSelectedReleaseRelationsForType(mapForNavigation.get(key).values()));
                    }
                }
                break;
            case PROVIDED:
                providedGroupIdentifierMap = groupIdentifierMap;
                providedRelations = new TreeMap(String.CASE_INSENSITIVE_ORDER);
                for (String key : mapForNavigation.keySet()) {
                    providedRelations.put(key, findBestMatchingOrSelectedReleaseRelationsForType(mapForNavigation.get(key).values()));
                }
                break;
            default:
                break;
        }
    }

    private List<ResourceEditRelation> unmapToList(Collection<List<ResourceEditRelation>> values) {
        List<ResourceEditRelation> allReleaseRelations = new ArrayList<>();
        for (List<ResourceEditRelation> relations : new ArrayList<>(values)) {
            allReleaseRelations.addAll(relations);
        }
        return allReleaseRelations;
    }

    private List<ResourceEditRelation> findBestMatchingOrSelectedReleaseRelationsForType(Collection<List<ResourceEditRelation>> values) {

        List<ResourceEditRelation> allReleaseRelations = new ArrayList<>();
        for (List<ResourceEditRelation> relations : new ArrayList<>(values)) {
            allReleaseRelations.add(findBestMatchingOrSelectedReleaseRelation(relations));
        }
        return allReleaseRelations;
    }

    private ResourceEditRelation findBestMatchingOrSelectedReleaseRelation(List<ResourceEditRelation> allReleaseRelations) {
        Integer currentResourceRelationId = getSelectedResourceRelationId();

        if (currentResourceRelationId != null && allReleaseRelations.size() > 1) {
            ResourceEditRelation selectedRelation = null;

            for (ResourceEditRelation relation : allReleaseRelations) {
                if (relation.getResRelId().equals(currentResourceRelationId)) {
                    selectedRelation = relation;
                    break;
                }
            }
            if (selectedRelation != null) {
                // if selected relation is within list return list with selected element otherwise leave the list as it is
                allReleaseRelations = new ArrayList<>(Arrays.asList(selectedRelation));
            }
        }

        if (isResourceSelected()) {
            return resourceRelationService.getBestMatchingRelationRelease(allReleaseRelations, (ResourceEntity) currentSelectedResourceOrType);
        } else {
            // resourcetype does not have release relations - thus return first item from list
            return allReleaseRelations.get(0);
        }
    }

    private Integer getSelectedResourceRelationId() {
        Integer currentResourceRelationId = getCurrentResourceRelationId();
        if (currentResourceRelationId == null && currentRelationId != null) {
            currentResourceRelationId = currentRelationId;
        }
        return currentResourceRelationId;
    }

    private void mapUnresolvedRelations() {
        this.unresolvedRelations = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        List<ResourceEditRelation> resourceTypeRelations = resourceRelations.get(Mode.TYPE);
        if (resourceTypeRelations != null) {
            SortedMap<String, SortedSet<String>> unresolvedResRelMap = new TreeMap(String.CASE_INSENSITIVE_ORDER);

            // prepare a map with all instance relations
            Set<Integer> resolved = new HashSet<Integer>();
            List<ResourceEditRelation> consumedResourceRelations = resourceRelations.get(Mode.CONSUMED);
            if (consumedResourceRelations != null) {
                for (ResourceEditRelation rel : consumedResourceRelations) {
                    resolved.add(rel.getResRelTypeId());
                }
            }
            List<ResourceEditRelation> providedResourceRelations = resourceRelations.get(Mode.PROVIDED);
            if (providedResourceRelations != null) {
                for (ResourceEditRelation rel : providedResourceRelations) {
                    resolved.add(rel.getResRelTypeId());
                }
            }
            for (ResourceEditRelation rel : resourceTypeRelations) {
                // we need only type relations not contained in consumed or provided for unresolved
                if (!resolved.contains(rel.getResRelTypeId())) {
                    if (!unresolvedResRelMap.containsKey(rel.getSlaveTypeName())) {
                        unresolvedResRelMap.put(rel.getSlaveTypeName(), new TreeSet<String>());
                    }
                    unresolvedResRelMap.get(rel.getSlaveTypeName()).add(rel.getDisplayName());
                }
            }

            for (String key : unresolvedResRelMap.keySet()) {
                this.unresolvedRelations.put(key, unresolvedResRelMap.get(key));
            }
        }
    }

    /**
     * @return true if type is one of {@link DefaultResourceTypeDefinition}, false otherwise
     */
    public boolean isDefaultResourceType() {
        if (isResourceSelected()) {
            ResourceTypeEntity resourceTypeOfSelectedResource = (getCurrentSelectedResource()).getResourceType();

            return resourceTypeOfSelectedResource != null && DefaultResourceTypeDefinition.contains(resourceTypeOfSelectedResource.getName());
        }
        return false;
    }

    public boolean isAppServerToNode() {
        return currentResourceRelation != null
                && DefaultResourceTypeDefinition.APPLICATIONSERVER.name().equals(currentResourceRelation.getMasterTypeName())
                && DefaultResourceTypeDefinition.NODE.name().equals(currentResourceRelation.getSlaveTypeName());

    }

    public boolean isNode() {
        if (isResourceSelected()) {
            ResourceTypeEntity resourceTypeOfSelectedResource = (getCurrentSelectedResource()).getResourceType();
            return DefaultResourceTypeDefinition.NODE.name().equals(resourceTypeOfSelectedResource.getName());
        }
        return false;
    }

    private ResourceEntity getCurrentSelectedResource() {
        if (isResourceSelected()) {
            return (ResourceEntity) currentSelectedResourceOrType;
        }
        return null;
    }

    /**
     * Returns true if a ownership combination is questionable
     */
    public boolean isSuspectRelation(ForeignableAttributesDTO sourceForeignables,
                                     ForeignableAttributesDTO relationForeignables, ForeignableAttributesDTO targetForeignables) {
        ForeignableOwner sourceOwner = sourceForeignables != null ? sourceForeignables.getOwner() : null;
        ForeignableOwner relationOwner = relationForeignables != null ? relationForeignables.getOwner()
                : null;
        ForeignableOwner targetOwner = targetForeignables != null ? targetForeignables.getOwner() : null;
        return resourceRelationBoundary.isSuspectRelation(sourceOwner, relationOwner, targetOwner);
    }

    public boolean canDeleteRelation() {
        boolean result = sessionContext.getIsGlobal()
                && (currentSelectedResourceOrType != null ? allowedToRemoveRelations : false)
                && getCurrentRelationUniqueIdentifier() != null
                && foreignableBoundary.isModifiableByOwner(ForeignableOwner.getSystemOwner(),
                currentResourceRelation.getRelationForeignableAttributes());
        return result;
    }

    private boolean hasConsumableSoftlinkSuperType(ResourceEntity resourceEntity) {
        return resourceLocator.hasResourceConsumableSoftlinkType(resourceEntity.getId());
    }

    public SoftlinkRelationEntity getResourceSoftlinkRelation() {

        ResourceEntity currentSelectedResource = getCurrentSelectedResource();
        if (currentSelectedResource != null) {
            return currentSelectedResource.getSoftlinkRelation();
        }

        return null;
    }

    public void reloadResource() throws ValidationException {
        if (isResourceSelected()) {
            currentSelectedResourceOrType = resourceLocator.getResourceWithGroupAndRelatedResources(
                    currentSelectedResourceOrType.getId());

            canShowSoftlinkRelations = sessionContext.getIsGlobal()
                    && hasConsumableSoftlinkSuperType(getCurrentSelectedResource())
                    && getResourceSoftlinkRelation() != null;


            canShowAddSoftlinkRelationButton = sessionContext.getIsGlobal()
                    && getResourceSoftlinkRelation() == null
                    && hasConsumableSoftlinkSuperType(getCurrentSelectedResource())
                    && permissionBoundary.hasPermission(Permission.SET_SOFTLINK_ID_OR_REF);

            canEditSoftlinkRelation = sessionContext.getIsGlobal()
                    && getResourceSoftlinkRelation() != null
                    && hasConsumableSoftlinkSuperType(getCurrentSelectedResource())
                    && foreignableBoundary.isModifiableByOwner(ForeignableOwner.getSystemOwner(), createForeignableDto(getResourceSoftlinkRelation()))
                    && permissionBoundary.hasPermission(Permission.SET_SOFTLINK_ID_OR_REF);

            reloadValues();

        }
    }
}
