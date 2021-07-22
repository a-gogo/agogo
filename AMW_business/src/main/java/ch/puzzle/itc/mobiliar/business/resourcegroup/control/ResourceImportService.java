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

import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.GlobalContext;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.common.util.ConfigKey;
import ch.puzzle.itc.mobiliar.common.util.ConfigurationService;

import javax.inject.Inject;
import java.util.*;

public class ResourceImportService {

    public static final String BACKLINK_URL = "/AMW_web/pages/editResourceView.xhtml?ctx=1&id=";

    @Inject
    ResourceReleaseComparator resourceReleaseComparator;

    @Inject
    @GlobalContext
    private ContextEntity globalContext;

    public List<ResourceEntity> getAllMinorReleasesFollowingRelease(Set<ResourceEntity> allReleaseResources, ReleaseEntity release) {
        List<ResourceEntity> followingMinorRelease = new ArrayList<>();

        List<ResourceEntity> allReleaseResourcesOrderedByRelease = new ArrayList<>(allReleaseResources);
        Collections.sort(allReleaseResourcesOrderedByRelease, resourceReleaseComparator);

        boolean isMinorReleaseToAdd = false;
        for (ResourceEntity resource : allReleaseResourcesOrderedByRelease) {
            if (isMinorReleaseToAdd) {
                if (!resource.getRelease().isMainRelease()) {
                    followingMinorRelease.add(resource);
                } else {
                    return followingMinorRelease;
                }
            }

            if (resource.getRelease().equals(release)) {
                // start adding releases until next
                isMinorReleaseToAdd = true;
            }
        }

        return followingMinorRelease;
    }

    public ResourceEntity getPreviousRelease(Set<ResourceEntity> allReleaseResources, ResourceEntity resource) {

        if(resource == null){
            return null;
        }

        ResourceEntity result = null;
        List<ResourceEntity> allReleaseResourcesOrderedByRelease = new ArrayList<>(allReleaseResources);
        Collections.sort(allReleaseResourcesOrderedByRelease, resourceReleaseComparator);

        for (ResourceEntity resourceInRelease : allReleaseResourcesOrderedByRelease) {

            if(resource.getId().equals(resourceInRelease.getId())){
                return result;
            }
            // add latest Release to copy From
            result = resourceInRelease;
        }

        return null;
    }


    public String getImportedResourceBacklink(){
        StringBuilder sb = new StringBuilder();
        sb.append(ConfigurationService.getProperty(ConfigKey.EXTERNAL_RESOURCE_BACKLINK_SCHEMA, "http"));
        sb.append("://");
        sb.append(ConfigurationService.getProperty(ConfigKey.EXTERNAL_RESOURCE_BACKLINK_HOST, "localhost:8080"));
        sb.append(BACKLINK_URL);
        return sb.toString();
    }
}
