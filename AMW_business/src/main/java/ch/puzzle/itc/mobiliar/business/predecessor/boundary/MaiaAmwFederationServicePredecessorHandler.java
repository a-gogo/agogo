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

package ch.puzzle.itc.mobiliar.business.predecessor.boundary;

import ch.puzzle.itc.mobiliar.business.foreignable.control.ForeignableService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.MessageSeverity;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.PredecessorResult;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.PredecessorResultMessage;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.ProcessingState;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceResult;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.RelationImportService;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationService;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.AbstractResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.AMWRuntimeException;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


@Stateless
public class MaiaAmwFederationServicePredecessorHandler {

    @Inject
    private ResourceLocator resourceLocator;

    @Inject
    ResourceRepository resourceRepository;

    @Inject
    ResourceDependencyResolverService dependencyResolverService;

    @Inject
    CopyResourceDomainService copyService;

    @Inject
    ForeignableService foreignableService;

    @Inject
    private ResourceRelationService resourceRelationService;

    @Inject
    private RelationImportService relationImportService;

    @Inject
    private Logger log;

    public PredecessorResult handlePredecessor(String successorName, String predecessorName, ForeignableOwner actingOwner) {
        final ResourceEntity successorResource = getSuccessor(successorName);
        return doHandlePredecessor(successorResource, predecessorName, actingOwner);
    }

    public PredecessorResult handlePredecessor(String successorName, ReleaseEntity successorReleaseEntity, String predecessorName, ForeignableOwner actingOwner) {
        final ResourceEntity successorResource = resourceRepository.getResourceByNameAndRelease(successorName, successorReleaseEntity);
        return doHandlePredecessor(successorResource, predecessorName, actingOwner);
    }


    private PredecessorResult doHandlePredecessor(ResourceEntity successorResource, String predecessorName, ForeignableOwner actingOwner) {

        PredecessorResult predecessorResult = new PredecessorResult(successorResource.getName(), ProcessingState.FAILED);
        List<ResourceEntity> predecessorCandidates = getPredecessorCandidates(predecessorName);

        if (!predecessorCandidates.isEmpty()) {
            ResourceEntity predecessorResource = findPredecessor(predecessorCandidates, successorResource, predecessorResult);

            if (copyPredecessorApplicationToSuccessor(successorResource, actingOwner, predecessorResult, predecessorResource)
                    && copyCpisFromPredecessorToSuccessor(successorResource, actingOwner, predecessorResult, predecessorResource)
                    && copyPpisFromPredecessorToSuccessor(successorResource, actingOwner, predecessorResult, predecessorResource)
                    && handleApplicationServerRelations(successorResource, predecessorResult, predecessorResource)) {

                predecessorSuccessfullyReplacedResult(successorResource, predecessorName, predecessorResult);
            }

        } else {
            // do "nothing"
            log.warning("Predecessor application " + predecessorName + " not found");
            predecessorResult.setProcessingState(ProcessingState.OK);
            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.WARNING, "Predecessor application " + predecessorName + " not found"));
        }
        return predecessorResult;
    }

    private void predecessorSuccessfullyReplacedResult(ResourceEntity successorResource, String predecessorName, PredecessorResult predecessorResult) {
        predecessorResult.setProcessingState(ProcessingState.OK);
        // only if really successful
        if (predecessorResult.getMessages().isEmpty()) {
            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.INFO, "Predecessor application " + predecessorName + " successfully replaced by " + successorResource.getName()));
        }
    }

    private boolean handleApplicationServerRelations(ResourceEntity successorResource, PredecessorResult predecessorResult, ResourceEntity predecessorResource) {
        //Copy Application Server Consumed Slave Relations
        PredecessorResult predecessorAsResult = handleApplicationServerRelations(predecessorResource, successorResource);
        // Handle Result, and merge
        if (ProcessingState.FAILED.equals(predecessorAsResult.getProcessingState())) {
            for (PredecessorResultMessage predecessorResultMessage : predecessorAsResult.getMessages()) {
                predecessorResult.addMessage(predecessorResultMessage);
            }
            return false;
        }
        return true;
    }

    private boolean copyPpisFromPredecessorToSuccessor(ResourceEntity successorResource, ForeignableOwner actingOwner, PredecessorResult predecessorResult, ResourceEntity predecessorResource) {
        // copy PPIs
        for (ProvidedResourceRelationEntity providedResourceRelationEntity : successorResource.getProvidedMasterRelations()) {
            if (predecessorResource != null && resourceLocator.hasResourceProvidableSoftlinkType(providedResourceRelationEntity.getSlaveResource())) {
                ResourceEntity successorPpi = providedResourceRelationEntity.getSlaveResource();
                try {
                    final ResourceEntity predecessorPpi = getPredecessorForCpiOrPpi(successorPpi, predecessorResource.getProvidedMasterRelations());
                    if (predecessorPpi != null) {
                        try {
                            copyService.copyFromPredecessorToSuccessorResource(predecessorPpi, successorPpi, actingOwner);
                        } catch (ForeignableOwnerViolationException e) {
                            log.warning(e.getMessage());
                            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.ERROR, e.getMessage()));
                            return false;
                        } catch (AMWException e) {
                            log.warning(e.getMessage());
                            predecessorResult.setProcessingState(ProcessingState.OK);
                            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.WARNING, e.getMessage()));
                            return false;
                        }
                    }
                } catch (AMWRuntimeException e) {
                    predecessorResult.setProcessingState(ProcessingState.OK);
                    predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.WARNING, e.getMessage()));
                    return false;
                }
            }
        }
        return true;
    }

    private boolean copyCpisFromPredecessorToSuccessor(ResourceEntity successorResource, ForeignableOwner actingOwner, PredecessorResult predecessorResult, ResourceEntity predecessorResource) {
        // copy CPIs
        for (ConsumedResourceRelationEntity consumedResourceRelationEntity : successorResource.getConsumedMasterRelations()) {
            if (predecessorResource != null && resourceLocator.hasResourceConsumableSoftlinkType(consumedResourceRelationEntity.getSlaveResource())) {
                ResourceEntity successorCpi = consumedResourceRelationEntity.getSlaveResource();
                try {
                    final ResourceEntity predecessorCpi = getPredecessorForCpiOrPpi(successorCpi, predecessorResource.getConsumedMasterRelations());
                    if (predecessorCpi != null) {
                        try {
                            copyService.copyFromPredecessorToSuccessorResource(predecessorCpi, successorCpi, actingOwner);
                        } catch (ForeignableOwnerViolationException e) {
                            log.warning(e.getMessage());
                            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.ERROR, e.getMessage()));
                            return false;
                        } catch (AMWException e) {
                            log.warning(e.getMessage());
                            predecessorResult.setProcessingState(ProcessingState.OK);
                            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.WARNING, e.getMessage()));
                            return false;
                        }
                    }
                } catch (AMWRuntimeException e) {
                    predecessorResult.setProcessingState(ProcessingState.OK);
                    predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.WARNING, e.getMessage()));
                    return false;
                }
            }
        }
        return true;
    }

    private boolean copyPredecessorApplicationToSuccessor(ResourceEntity successorResource, ForeignableOwner actingOwner, PredecessorResult predecessorResult, ResourceEntity predecessorResource) {
        // copy application
        try {
            copyService.copyFromPredecessorToSuccessorResource(predecessorResource, successorResource, actingOwner);
        } catch (ForeignableOwnerViolationException e) {
            log.warning(e.getMessage());
            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.ERROR, e.getMessage()));
            return false;
        } catch (AMWException e) {
            log.warning(e.getMessage());
            predecessorResult.setProcessingState(ProcessingState.OK);
            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.WARNING, e.getMessage()));
            return false;
        }
        return true;
    }

    private ResourceEntity findPredecessor(List<ResourceEntity> predecessorCandidates, ResourceEntity successorResource, PredecessorResult predecessorResult) {
        ResourceEntity predecessorResource = dependencyResolverService.getResourceEntityForRelease(predecessorCandidates, successorResource.getRelease());

        if (predecessorResource == null) {
            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.WARNING, "no suitable release for predecessor application " + (predecessorCandidates.isEmpty() ? "" : predecessorCandidates.get(0).getName()) + " found"));
            log.warning("No suitable predecessor release found ");
        } else {
            log.info("Best matching predecessor release is " + predecessorResource.getRelease().getName());
        }
        return predecessorResource;
    }


    private PredecessorResult handleApplicationServerRelations(ResourceEntity predecessorResource, ResourceEntity successorResource) {
        PredecessorResult predecessorResult = new PredecessorResult();
        ResourceGroupEntity asPredecessorGroup = getPredecessorApplicationServer(predecessorResource);
        if (asPredecessorGroup != null) {
            predecessorResult = handleAsForSuccessorResourceRelease(successorResource, asPredecessorGroup);

            List<ResourceEntity> allReleasesFutureRelease = dependencyResolverService.getAllFutureReleases(asPredecessorGroup.getResources(), successorResource.getRelease());

            // add successor To all future Releases of the asInSuccessorRelease (including asInSuccessorRelease)
            addSuccessorToAllFutureReleases(predecessorResult, allReleasesFutureRelease, successorResource);

            // remove predecessor AS Master Relations where now the Successor is attached
            removeAsRelationsInAllFutureReleases(predecessorResult, allReleasesFutureRelease, predecessorResource);


        } else {
            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.WARNING, "Predecessor application " + (predecessorResource != null ? predecessorResource.getName() : "") + " has no Application Server relation, therefore the Application Server Handling was not performed."));
        }
        return predecessorResult;
    }

    private PredecessorResult handleAsForSuccessorResourceRelease(ResourceEntity successorResource, ResourceGroupEntity asPredecessorGroup) {
        PredecessorResult predecessorResult = new PredecessorResult();
        try {
            if (getOrCreateAsInSuccessorRelease(asPredecessorGroup, successorResource) == null) {
                log.warning("No Applicationerver could be created or found for successor release");
                throw new AMWException("No Applicationerver could be created or found for successor release");
            }

        } catch (ForeignableOwnerViolationException e) {
            log.log(Level.WARNING, "Error while creating As in Successor Release", e);
            predecessorResult.setProcessingState(ProcessingState.FAILED);
            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.ERROR, "Error while creating As in Successor Release: " + e.getMessage()));
        } catch (AMWException e) {
            log.log(Level.WARNING, "Error while creating As in Successor Release", e);
            predecessorResult.setProcessingState(ProcessingState.FAILED);
            predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.WARNING, "Error while creating As in Successor Release: " + e.getMessage()));
        }
        return predecessorResult;
    }

    private void addSuccessorToAllFutureReleases(PredecessorResult predecessorResult, List<ResourceEntity> allFutureReleases, ResourceEntity successorResource) {
        for (ResourceEntity resourceEntity : allFutureReleases) {
            try {
                resourceRelationService.addRelationByGroup(resourceEntity.getId(), successorResource.getResourceGroup().getId(), false, null, null, ForeignableOwner.getSystemOwner());
            } catch (ElementAlreadyExistsException | ResourceNotFoundException e) {
                log.log(Level.WARNING, "Error Could not add Successor to future AS", e);
                predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.ERROR, "Error while adding successor resource to sucessor as release: " + e.getMessage()));
            }
        }
    }

    private void removeAsRelationsInAllFutureReleases(PredecessorResult predecessorResult, List<ResourceEntity> allFutureAsReleases, ResourceEntity predecessorResource) {
        List<ConsumedResourceRelationEntity> asToRemoveRelations = new ArrayList<>();

        for (ResourceEntity futureReleaseAppServer : allFutureAsReleases) {
            for (ConsumedResourceRelationEntity asToAppRelationInFutureAsRelease : futureReleaseAppServer.getConsumedMasterRelations()) {
                // compare by Name because we need to remove all predecessor Releases
                if (predecessorResource.getName().equals(asToAppRelationInFutureAsRelease.getSlaveResource().getName())) {
                    asToRemoveRelations.add(asToAppRelationInFutureAsRelease);
                }
            }
        }
        for (ConsumedResourceRelationEntity asToAppRelationInFutureAsRelease : asToRemoveRelations) {
            try {
                relationImportService.deleteConsumedPortRelations(Collections.singletonList(asToAppRelationInFutureAsRelease));
            } catch (ElementAlreadyExistsException | ResourceNotFoundException e) {
                log.log(Level.WARNING, "Error Could not add Successor to future AS", e);
                predecessorResult.addMessage(new PredecessorResultMessage(MessageSeverity.ERROR, "Error while adding successor resource to sucessor as release: " + e.getMessage()));
            }
        }
    }

    private ResourceGroupEntity getPredecessorApplicationServer(ResourceEntity predecessorResource) {
        if (predecessorResource != null && predecessorResource.getConsumedSlaveRelations() != null) {
            for (ConsumedResourceRelationEntity consumedResourceRelationEntity : predecessorResource.getConsumedSlaveRelations()) {
                // find Relation from AS to App
                if (consumedResourceRelationEntity.getMasterResource().getResourceType().isApplicationServerResourceType()) {
                    return consumedResourceRelationEntity.getMasterResource().getResourceGroup();
                }
            }
        }
        return null;
    }

    private ResourceEntity getOrCreateAsInSuccessorRelease(ResourceGroupEntity asPredecessorGroup, ResourceEntity successorResource) throws ForeignableOwnerViolationException, AMWException {
        for (ResourceEntity resource : asPredecessorGroup.getResources()) {
            if (successorResource.getRelease().getId().equals(resource.getRelease().getId())) {
                return resource;
            }
        }
        // create AS because it does not exist
        ResourceEntity originAs = dependencyResolverService.getResourceEntityForRelease(asPredecessorGroup.getResources(), successorResource.getRelease());

        CopyResourceResult releaseFromOriginResource = copyService.createReleaseFromOriginResource(originAs, successorResource.getRelease(), ForeignableOwner.getSystemOwner());
        if (releaseFromOriginResource.isSuccess()) {
            return releaseFromOriginResource.getTargetResource();
        }
        return null;
    }


    private List<ResourceEntity> getPredecessorCandidates(String predecessorName) {
        return getResourceGroup(predecessorName);
    }

    private ResourceEntity getSuccessor(String successorName) {
        List<ResourceEntity> successorGroup = getResourceGroup(successorName);
        if (successorGroup.isEmpty()) {
            throw new AMWRuntimeException("New application " + successorName + " not found");
        }
        // return the oldest release
        return successorGroup.get(0);
    }

    private List<ResourceEntity> getResourceGroup(String appName) {
        return resourceRepository.getResourcesByGroupNameWithAllRelationsOrderedByRelease(appName);
    }

    protected <T extends AbstractResourceRelationEntity> ResourceEntity getPredecessorForCpiOrPpi(ResourceEntity successor, Set<T> predecessorRelations) throws AMWRuntimeException {

        for (AbstractResourceRelationEntity predecessorRelation : predecessorRelations) {
            if (predecessorRelation.getSlaveResource().getLocalPortId() != null && predecessorRelation.getSlaveResource().getLocalPortId().equals(successor.getLocalPortId())) {
                return predecessorRelation.getSlaveResource();
            }
        }
        return null;
    }

}
