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

package ch.puzzle.itc.mobiliar.presentation.copyResource;

import ch.puzzle.itc.mobiliar.business.foreignable.boundary.ForeignableBoundary;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.predecessor.boundary.MaiaAmwFederationServicePredecessorHandler;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.PredecessorResult;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.PredecessorResultMessage;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.ProcessingState;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.CopyResource;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceResult;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroup;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.utils.Identifiable;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.presentation.common.ResourceTypeDataProvider;
import ch.puzzle.itc.mobiliar.presentation.resourcesedit.EditResourceView;
import ch.puzzle.itc.mobiliar.presentation.util.GlobalMessageAppender;
import lombok.Getter;

import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.*;

/**
 * For copy resource usecase
 * 
 * @author cweber
 */
@Named
@ViewScoped
public class CopyResourceDataProvider implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	EditResourceView resource;

	@Inject
	private CopyResource copyResource;

	@Inject
	private MaiaAmwFederationServicePredecessorHandler maiaAmwFederationServicePredecessorHandler;

	@Inject
    PermissionBoundary permissionBoundary;

    @Inject
    ForeignableBoundary foreignableBoundary;

	@Inject
	EditResourceView editResourceView;

	@Inject
	@Getter
	private ResourceTypeDataProvider resourceTypeDataProvider;

	private Map<Integer, ResourceGroup> resourceGroupMap;

	@Getter
	boolean loadList;

	public void enableLoadList() {
	    if(!loadList) {
		   loadList = true;
		   refreshResourceList();
	    }

	}


	/**
	 * @return list with resources for the selected type
	 */
	public List<ResourceGroup> getResourcesForSelectedResourceType() {
		if (resourceGroupMap == null) {
			return new ArrayList<>();
		}
		SortedSet<ResourceGroup> groups = new TreeSet<>(resourceGroupMap.values());
		return new ArrayList<>(groups);
	}

	/**
	 * load resources for the selected type
	 */
	private void refreshResourceList() {
		resourceGroupMap = new HashMap<>();
		if (loadList) {
			List<ResourceGroup> freshGroupList = copyResource.loadResourceGroupsForType(
					resource.getResourceType().getId(),
					resource.getResource());

			for (ResourceGroup group : freshGroupList) {
				// ResourceGroupEntity group = r.getEntity().getResourceGroup();
				if (!resourceGroupMap.containsKey(group.getId())) {
					resourceGroupMap.put(group.getId(), group);
				}

				// special handling if resource is the same that will be overwritten
				ResourceEntity res = group.getEntity().getResources().iterator().next();
				if (res.getId().equals(resource.getResourceId())) {
					if (group.getReleases().size() <= 1) {
						// if there is only one existing release, do not list group
						resourceGroupMap.remove(group.getId());
					}
					else {
						// do not show release in popup, it makes on sense to copy from itself
						resourceGroupMap.get(group.getId()).addReleaseToExclude(res.getRelease());
					}
				}
			}
		}
	}

	public void copyFromResource(Integer selectedGroup) {
		try {
			copyFromResourceAction(resource.getResource(), resourceGroupMap.get(selectedGroup).getSelectedResourceId());
			editResourceView.forceReload();
		} catch (ForeignableOwnerViolationException e) {
            GlobalMessageAppender.addErrorMessage("Owner "+e.getViolatingOwner()+" is not allowed to copy from selected resource because of violating "+e.getViolatedForeignableObject().getForeignableObjectName()+" with id "+ ((Identifiable)e.getViolatedForeignableObject()).getId());
        }
		catch (AMWException e){
			GlobalMessageAppender.addErrorMessage(e.getMessage());
		}
	}

	public void copyFromPredecessorResource(Integer selectedGroup) {
		copyFromPredecessorResourceAction(resource.getResource(), resourceGroupMap.get(selectedGroup));
	}

	/**
	 * @param resourceToOverwrite
	 * @param copyFromResourceId
	 * @return true if copy was successful, false otherwise
	 * @throws ResourceNotFoundException
	 * @throws AMWException
	 */
	private boolean copyFromResourceAction(ResourceEntity resourceToOverwrite, Integer copyFromResourceId)
			throws AMWException, ForeignableOwnerViolationException {
		if (resourceToOverwrite == null) {
			String message = "No resource selected.";
			GlobalMessageAppender.addErrorMessage(message);
			return false;
		}
		if (copyFromResourceId == null) {
			String message = "No resource to copy from selected.";
			GlobalMessageAppender.addErrorMessage(message);
			return false;
		}
		
		CopyResourceResult result = copyResource.doCopyResource(resourceToOverwrite.getId(), copyFromResourceId, ForeignableOwner.getSystemOwner());
		if (result != null && !result.isSuccess()) {
			for (String error : result.getExceptions()) {
				GlobalMessageAppender.addErrorMessage(error);
			}
			return false;
		}

		GlobalMessageAppender.addSuccessMessage("Copy successful");
		return true;
	}

	/**
	 * @param resourceToOverwrite
	 * @param predecessor
	 * @return true if copy was successful, false otherwise
	 */
	private boolean copyFromPredecessorResourceAction(ResourceEntity resourceToOverwrite, ResourceGroup predecessor) {
		if (resourceToOverwrite == null) {
			String message = "No resource selected.";
			GlobalMessageAppender.addErrorMessage(message);
		}
		else if (predecessor == null) {
			String message = "No resource to copy from selected.";
			GlobalMessageAppender.addErrorMessage(message);
		}
		else {

			PredecessorResult predecessorResult = maiaAmwFederationServicePredecessorHandler.handlePredecessor(resourceToOverwrite.getName(), resourceToOverwrite.getRelease(), predecessor.getName(), ForeignableOwner.MAIA);

			if (predecessorResult != null && ProcessingState.FAILED.equals(predecessorResult.getProcessingState())) {
				for (PredecessorResultMessage error : predecessorResult.getMessages()) {
					GlobalMessageAppender.addErrorMessage(error.getHumanReadableMessage());
				}
			}
			else {
				GlobalMessageAppender.addSuccessMessage("Predecessor Copy was successful");
			}
		}
		return false;
	}

    /**
	 * Load all resources for the selected type and exclude resource from excludeList
	 * 
	 * @param resourceTypeId
	 * @param excludedList
	 * @return
	 */
	public List<ResourceGroup> loadResourcesForSelectedType(Integer resourceTypeId,
			List<Integer> excludedList) {
		return copyResource.loadResourceGroupsForType(resourceTypeId, resource.getResource());
	}

    public boolean isCanCopyResource() {
        return permissionBoundary.canCopyFromResource(resource.getResource()) && (resource.getResource() != null);
    }

	public boolean isCanCopyFromPredecessorResource() {
		return permissionBoundary.canCopyFromResource(resource.getResource()) && (resource.getResource() != null);
	}

	public boolean allowedToCopyFromThatResource(ResourceGroup originResourceGroup) {
		return permissionBoundary.canReadFromResource(originResourceGroup.getEntity()) && (resource.getResource() != null);
	}
}
