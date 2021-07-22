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

package ch.puzzle.itc.mobiliar.business.softlinkRelation.boundary;

import java.io.Serializable;
import java.util.Objects;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import ch.puzzle.itc.mobiliar.business.foreignable.control.ForeignableService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.control.SoftlinkRelationService;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.entity.SoftlinkRelationEntity;

@Stateless
public class SoftlinkRelationBoundary implements Serializable {

    @Inject
    private SoftlinkRelationService softlinkRelationService;

    @Inject
    private ForeignableService foreignableService;

    @Inject
    PermissionService permissionService;

    @Inject
    private EntityManager entityManager;

    public void createSoftlinkRelation(ForeignableOwner creatingOwner, Integer cpiResourceId, String softlinkReference) throws ForeignableOwnerViolationException {
        ResourceEntity cpiResource = entityManager.find(ResourceEntity.class, cpiResourceId);
        permissionService.checkPermissionAndFireException(Permission.RESOURCE, null, Action.UPDATE, cpiResource.getResourceGroup(), null, null);

        SoftlinkRelationEntity softlinkRelation = cpiResource.getSoftlinkRelation();
        if (softlinkRelation == null){
            softlinkRelation = new SoftlinkRelationEntity();
            softlinkRelation.setOwner(creatingOwner);
            softlinkRelation.setCpiResource(cpiResource);
            softlinkRelation.setSoftlinkRef(softlinkReference);
        } else {
            int beforeChangeForeignableFieldHashCode = softlinkRelation.foreignableFieldHashCode();
            softlinkRelation.setSoftlinkRef(softlinkReference);
            foreignableService.verifyEditableByOwner(creatingOwner, beforeChangeForeignableFieldHashCode, softlinkRelation);
        }

        softlinkRelationService.setSoftlinkRelation(cpiResource, softlinkRelation);
    }

    public void removeRelationForResource(ForeignableOwner deletingOwner, Integer resourceId) throws ForeignableOwnerViolationException {
        ResourceEntity cpiResource = entityManager.find(ResourceEntity.class, resourceId);
        permissionService.checkPermissionAndFireException(Permission.RESOURCE, null, Action.UPDATE, cpiResource.getResourceGroup(), null, null);

        SoftlinkRelationEntity softlinkRelation = cpiResource.getSoftlinkRelation();
        if (softlinkRelation != null){
            foreignableService.verifyDeletableByOwner(deletingOwner, softlinkRelation);

            softlinkRelationService.removeSoftlinkRelation(cpiResource);
        }
    }

    public void editSoftlinkRelation(ForeignableOwner editingUser, SoftlinkRelationEntity editedSoftlinkRelation) throws ForeignableOwnerViolationException {
        SoftlinkRelationEntity softlinkRelation = entityManager.find(SoftlinkRelationEntity.class, Objects.requireNonNull(editedSoftlinkRelation, "editedSoftlinkRelation must not be null!").getId());
        permissionService.checkPermissionAndFireException(Permission.RESOURCE, null, Action.UPDATE, softlinkRelation.getCpiResource().getResourceGroup(), null, null);

        int beforeChangeForeignableFieldHashCode = softlinkRelation.foreignableFieldHashCode();
        SoftlinkRelationEntity mergedSoftlinkRelation = entityManager.merge(editedSoftlinkRelation);
        foreignableService.verifyEditableByOwner(editingUser, beforeChangeForeignableFieldHashCode, mergedSoftlinkRelation);
    }
    
    public ResourceEntity getSoftlinkResolvableSlaveResource(SoftlinkRelationEntity softlinkRelation, ReleaseEntity release){
        if (softlinkRelation != null){
            return softlinkRelationService.getSoftlinkResolvableSlaveResource(softlinkRelation.getSoftlinkRef(), release);
        }
        return null;
    }

}
