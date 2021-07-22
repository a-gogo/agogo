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

package ch.puzzle.itc.mobiliar.presentation.dependencies;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import lombok.Getter;
import lombok.Setter;
import ch.puzzle.itc.mobiliar.business.domain.commons.CommonDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ApplicationServer;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationService;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.presentation.resourceDependencies.DependencyModel;


/**
 * Deprecated: use {@link ch.puzzle.itc.mobiliar.presentation.resourceDependencies.ResourceDependenciesView} instead
 */
@Named
@RequestScoped
@Deprecated
public class DependenciesController {

	@Inject
	private DependenciesDataProvider dependenciesDataProvider;

	@Inject
	private ResourceRelationService relationService;

	@Getter @Setter
	private Integer contextId;

	@Inject
	private ResourceLocator resourceLocator;

	@Inject
	private CommonDomainService commonDomainService;

	@Getter
	private Integer resourceId;
	
	public void setResourceId(Integer resourceId){
		if(this.resourceId==null || !this.resourceId.equals(resourceId)){
			this.resourceId = resourceId;
			resource = resourceLocator.getResourceWithGroupAndRelatedResources(resourceId);
			resource.getConsumedSlaveRelations();
			dependenciesDataProvider.init(resource, getConsumedRelations(), getProvidedRelations());
		}
	}
	
	@Getter
	private ResourceEntity resource;

	private List<DependencyModel> getConsumedRelations() {
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

	private List<DependencyModel> getProvidedRelations() {
		List<DependencyModel> models = new ArrayList<DependencyModel>();
		List<ProvidedResourceRelationEntity> provided = relationService.getProvidedSlaveRelations(resource);
		for (ProvidedResourceRelationEntity rel : provided) {
			models.add(new DependencyModel(rel));
		}
		return models;
	}

}
