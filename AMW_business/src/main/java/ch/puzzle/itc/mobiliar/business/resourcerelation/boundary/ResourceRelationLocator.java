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

package ch.puzzle.itc.mobiliar.business.resourcerelation.boundary;

import java.util.List;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;

import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationRepository;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.utils.ValidationHelper;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

@Stateless
public class ResourceRelationLocator {

    // TODO check permissions

    @Inject
    protected Logger log;

    @Inject
    ResourceRelationRepository resourceRelationRepository;

    @Inject
    ResourceLocator resourceLocator;

    /**
     * @param resourceGroupName
     * @param resourceGroupReleaseName
     * @param relatedResourceGroupName
     * @throws ValidationException
     *             thrown if one of the arguments is either empty or null
     */
    public List<ConsumedResourceRelationEntity> getRelatedResourcesForGroup(String resourceGroupName,
            String resourceGroupReleaseName, String relatedResourceGroupName) throws ValidationException {
        ValidationHelper.validateNotNullOrEmptyChecked(resourceGroupName, resourceGroupReleaseName,
                relatedResourceGroupName);
        ResourceEntity resource = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName,
                resourceGroupReleaseName);
        return resourceRelationRepository.getResourceRelationBySlaveResourceGroupName(resource,
                relatedResourceGroupName);

    }

    /**
     * @param resourceGroupName
     * @param resourceGroupReleaseName
     * @param relatedResourceGroupName
     * @param relatedResourceGroupReleaseName
     * @throws ValidationException
     *             thrown if one of the arguments is either empty or null
     */
    public ConsumedResourceRelationEntity getResourceRelation(String resourceGroupName, String resourceGroupReleaseName,
            String relatedResourceGroupName, String relatedResourceGroupReleaseName) throws ValidationException {
        ValidationHelper.validateNotNullOrEmptyChecked(resourceGroupName, resourceGroupReleaseName,
                relatedResourceGroupName, relatedResourceGroupReleaseName);

        ResourceEntity masterResource = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName,
                resourceGroupReleaseName);
        ResourceEntity slaveResource = resourceLocator.getResourceByGroupNameAndRelease(relatedResourceGroupName,
                relatedResourceGroupReleaseName);
        return resourceRelationRepository.getResourceRelation(masterResource, slaveResource);

    }

    /**
     * Für JavaBatch Monitor: gibt Liste zurück, da mehr als 1 'StandardJob' pro App möglich
     * 
     * @param resourceGroupName
     * @param resourceGroupReleaseName
     * @param relatedResourceGroupName
     * @param relatedResourceGroupReleaseName
     * @throws ValidationException
     *             thrown if one of the arguments is either empty or null
     */
    public List<ConsumedResourceRelationEntity> getResourceRelationList(String resourceGroupName,
            String resourceGroupReleaseName, String relatedResourceGroupName, String relatedResourceGroupReleaseName)
                    throws ValidationException {
        ValidationHelper.validateNotNullOrEmptyChecked(resourceGroupName, resourceGroupReleaseName,
                relatedResourceGroupName, relatedResourceGroupReleaseName);

        ResourceEntity masterResource = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName,
                resourceGroupReleaseName);
        ResourceEntity slaveResource = resourceLocator.getResourceByGroupNameAndRelease(relatedResourceGroupName,
                relatedResourceGroupReleaseName);
        return resourceRelationRepository.getResourceRelationList(masterResource, slaveResource);

    }

}
