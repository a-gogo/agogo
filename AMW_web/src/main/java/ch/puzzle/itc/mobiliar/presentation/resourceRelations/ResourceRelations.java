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

package ch.puzzle.itc.mobiliar.presentation.resourceRelations;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import lombok.Getter;
import ch.puzzle.itc.mobiliar.business.domain.commons.CommonDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ApplicationServer;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationService;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.boundary.DisplaySoftlinkDependencies;
import ch.puzzle.itc.mobiliar.presentation.CompositeBackingBean;
import ch.puzzle.itc.mobiliar.presentation.resourceDependencies.DependencyModel;
import ch.puzzle.itc.mobiliar.presentation.resourceDependencies.events.SelectedResourceEvent;

/**
 *  Component backing bean to list the consumed and provided relations for a resource
 */
@CompositeBackingBean
public class ResourceRelations implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private CommonDomainService commonDomainService;

    @Inject
    private ResourceRelationService relationService;

    @Inject
    DisplaySoftlinkDependencies displaySoftlinkDependencies;

    @Getter
    List<DependencyModel> consumedRelations;

    @Getter
    List<DependencyModel> providedRelations;

    @Getter
    private String resourceName;

    @Getter
    private String softlinkId;

    @Getter
    private List<ResourceEntity> consumingSoftlinkResources;


    public void onChangedResource(@Observes SelectedResourceEvent selectedResourceEvent) {
        consumedRelations = loadConsumedRelations(selectedResourceEvent.getSelectedResource());
        providedRelations = loadProvidedRelations(selectedResourceEvent.getSelectedResource());
        resourceName = selectedResourceEvent.getSelectedResource().getName();
        consumingSoftlinkResources = displaySoftlinkDependencies.loadConsumingResources(selectedResourceEvent.getSelectedResource());
        softlinkId = selectedResourceEvent.getSelectedResource().getSoftlinkId();
    }

    private List<DependencyModel> loadConsumedRelations(ResourceEntity resource) {
        List<DependencyModel> models = new ArrayList<DependencyModel>();
        ApplicationServer appWithoutAsContainer = null;
        appWithoutAsContainer = commonDomainService.createOrGetApplicationCollectorServer();


        List<ConsumedResourceRelationEntity> consumed = relationService.getConsumedSlaveRelations(resource);
        for(ConsumedResourceRelationEntity rel:consumed){
            if (appWithoutAsContainer == null || !rel.getMasterResource().getId().equals(appWithoutAsContainer.getId())) {
                models.add(new DependencyModel(rel));
            }
        }

        return models;
    }

    private List<DependencyModel> loadProvidedRelations(ResourceEntity resource) {
        List<DependencyModel> models = new ArrayList<DependencyModel>();
        List<ProvidedResourceRelationEntity> provided = relationService.getProvidedSlaveRelations(resource);
        for (ProvidedResourceRelationEntity rel : provided) {
            models.add(new DependencyModel(rel));
        }
        return models;
    }


}
