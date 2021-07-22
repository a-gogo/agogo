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

package ch.puzzle.itc.mobiliar.presentation.applist;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.inject.Inject;

import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableAttributesDTO;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceBoundary;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;

import ch.puzzle.itc.mobiliar.business.foreignable.boundary.ForeignableBoundary;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceRelations;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeProvider;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceWithRelations;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.common.exception.NotAuthorizedException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.presentation.ViewBackingBean;
import ch.puzzle.itc.mobiliar.presentation.common.ApplicationCreatorDataProvider;
import ch.puzzle.itc.mobiliar.presentation.common.ReleaseSelectionDataProvider;
import ch.puzzle.itc.mobiliar.presentation.common.ReleaseSelector;
import ch.puzzle.itc.mobiliar.presentation.resourcesedit.CreateResourceController;
import ch.puzzle.itc.mobiliar.presentation.util.GlobalMessageAppender;
import ch.puzzle.itc.mobiliar.presentation.util.NavigationUtils;

@ViewBackingBean
public class ApplistView implements Serializable, ApplicationCreatorDataProvider {

	private static final long serialVersionUID = 8800041104451876453L;

	@Inject @Getter
	private ReleaseSelectionDataProvider releaseDataProvider;
	@Inject
	CreateResourceController createResourceController;
	@Inject
	ResourceTypeProvider resourceTypeProvider;
	@Inject
	ResourceDependencyResolverService resourceDependencyResolver;
	@Inject
	ResourceRelations resourceRelations;
	@Inject
	ApplistFilter applistFilter;
	@Inject
	ResourceBoundary resourceBoundary;

	@Getter @Setter
	private String filter;
	@Getter @Setter
	private Integer selection;
	@Getter @Setter
	private Integer selectionApp;
	@Getter @Setter
	private String appServerName;
	@Getter @Setter
	private List<ResourceWithRelations> appServerList;
	@Getter @Setter
	private boolean relation;
	@Setter
	private Integer maxResults;
	@Getter
	private ReleaseSelector asReleaseSelector;
	@Getter
	private ReleaseSelector filterReleaseSelector;

    @Inject
    private PermissionBoundary permissionBoundary;

    @Inject
    private ForeignableBoundary foreignableBoundary;

	@PostConstruct
	public void init() {
		if (filterReleaseSelector == null) {
			filterReleaseSelector = new ReleaseSelector(null, releaseDataProvider.getReleaseMap());
		}
		if (asReleaseSelector == null) {
			asReleaseSelector = new ReleaseSelector(releaseDataProvider.getUpcomingReleaseId(),
					releaseDataProvider.getReleaseMap());
		}
		releaseDataProvider.reset();

		if (StringUtils.isEmpty(filter)) {
			this.filter = applistFilter.getFilter();
		}
		if (maxResults == null) {
			this.maxResults = applistFilter.getMaxResults();
		}
		if (filterReleaseSelector.getSelectedReleaseId() == null) {
			if (applistFilter.getReleaseId() != null) {
				this.filterReleaseSelector.setSelectedReleaseId(applistFilter.getReleaseId());
			}
			else {
				this.filterReleaseSelector.setSelectedReleaseId(releaseDataProvider
						.getUpcomingReleaseId());
			}
		}
		loadAppServerList();
	}



	/**
	 * Applikation entfernen
	 */
	public void removeApp() {
		if (removeApp(selectionApp)) {
			clearPopupFields();
			loadAppServerList();
		}
	}

	/**
	 * Entfernen einer Applikationsgruppe.
	 */
	public void removeAppServer() {
		if (removeAppServer(selection)) {
			clearPopupFields();
			loadAppServerList();
		}
	}

	public void createAppServer() {
		if (createResourceController.createResource(appServerName, resourceTypeProvider
						.getOrCreateDefaultResourceType(DefaultResourceTypeDefinition.APPLICATIONSERVER),
				asReleaseSelector.getSelectedRelease())) {
			clearPopupFields();
			loadAppServerList();
		}
		clearPopupFields();
	}

	public void loadAppServerList() {
		appServerList = loadAppServers(filter, getMaxResults() == 0 ? null : getMaxResults(),
				filterReleaseSelector.getSelectedRelease());
	}

	public String doFilter() {
		StringBuilder query = new StringBuilder();

		query.append("release=").append(filterReleaseSelector.getSelectedReleaseId());
		applistFilter.setReleaseId(filterReleaseSelector.getSelectedReleaseId());

		query.append("&maxResults=");
		if (maxResults != null) {
			query.append(maxResults);
			applistFilter.setMaxResults(maxResults);
		}
		else {
			query.append('0');
			applistFilter.setMaxResults(0);
		}

		query.append("&filter=");
		if (filter != null && !filter.trim().isEmpty()) {
			query.append(filter.trim());
			applistFilter.setFilter(filter.trim());
		}
		else {
			query.append(' ');
			applistFilter.setFilter(" ");
		}
		return NavigationUtils.getRefreshOutcomeWithAdditionalParam(query.toString());
	}
	public void setRelease(Integer releaseId) {
		this.filterReleaseSelector.setSelectedReleaseId(releaseId);
	}

	private void clearPopupFields() {
		appServerName = null;
	}

	public Integer getMaxResults() {
		return maxResults == null ? 0 : maxResults;
	}

	@Override
	public void afterAddingAppOrAs() {
		loadAppServerList();
		clearPopupFields();
	}

	/**
	 * Applikation entfernen
	 */
	private boolean removeApp(Integer applicationId) {
		boolean isSuccessful = false;
		try {
			if (applicationId == null) {
				String errorMessage = "No application selected.";
				GlobalMessageAppender.addErrorMessage(errorMessage);
			} else {
				try{
					resourceBoundary.removeResource(ForeignableOwner.getSystemOwner(), applicationId);
					String message = "Application successfully deleted";
					GlobalMessageAppender.addSuccessMessage(message);
					isSuccessful = true;
				} catch(EJBException e) {
					if (e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (ResourceNotFoundException e) {
			String errorMessage = "Could not load selected application for deletation.";
			GlobalMessageAppender.addErrorMessage(errorMessage);
		} catch (ForeignableOwnerViolationException e){
            GlobalMessageAppender.addErrorMessage("Application with id "+applicationId+" can not be deleted by owner "+e.getViolatingOwner());
        } catch (Exception e) {
			String errorMessage = "Could not delete selected application.";
			GlobalMessageAppender.addErrorMessage(errorMessage);
		}
		return isSuccessful;
	}

	/**
	 * Entfernen einer Applikationsgruppe.
	 */
	private boolean removeAppServer(Integer selectedAppServerId) {

		boolean isSuccessful = false;
		try {
			if (selectedAppServerId == null) {
				String errorMessage = "No application server selected.";
				GlobalMessageAppender.addErrorMessage(errorMessage);
			} else {
				try{
					resourceBoundary.deleteApplicationServerById(selectedAppServerId);
					String message = "Applicationserver successfully deleted";
					GlobalMessageAppender.addSuccessMessage(message);
					isSuccessful = true;
				} catch(EJBException e) {
					if (e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (ResourceNotFoundException e) {
			String errorMessage = "Could not load selected server for deletion.";
			GlobalMessageAppender.addErrorMessage(errorMessage);
		} catch (Exception e) {
			String errorMessage = "Could not delete selected application server.";
			GlobalMessageAppender.addErrorMessage(errorMessage);
		}
		return isSuccessful;
	}

	private List<ResourceWithRelations> loadAppServers(String filter, Integer maxResults, ReleaseEntity release) {
		if(maxResults!=null && maxResults==0) {
			maxResults = null;
		}
		return resourceRelations.getAppServersWithApplications(filter, maxResults, release);
	}

	public boolean canCreateApplicationServerInstance(){
		return permissionBoundary.canCreateResourceInstance(DefaultResourceTypeDefinition.APPLICATIONSERVER);
	}

	public boolean canCreateApplicationInstance(){
		return permissionBoundary.canCreateResourceInstance(DefaultResourceTypeDefinition.APPLICATION);
	}

    public boolean canShowDeleteApp(ResourceEntity app){
        return permissionBoundary.hasPermission(Permission.RESOURCE, null, Action.DELETE, app, null) && foreignableBoundary.isModifiableByOwner(ForeignableOwner.getSystemOwner(), app);
    }

    public boolean canShowDeleteAppServer(ResourceWithRelations appServer){
        ResourceEntity appserverResource = appServer.getResource();
        return appserverResource.isDeletable() && permissionBoundary.hasPermission(Permission.RESOURCE, null, Action.DELETE, appserverResource, null) && foreignableBoundary.isModifiableByOwner(ForeignableOwner.getSystemOwner(), appserverResource);
    }

    public ForeignableAttributesDTO getForeignableAttributes(ResourceEntity app){
        return new ForeignableAttributesDTO(app.getOwner(), app.getExternalKey(), app.getExternalLink());
    }

}
