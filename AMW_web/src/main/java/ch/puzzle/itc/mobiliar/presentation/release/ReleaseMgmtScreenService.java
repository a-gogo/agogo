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

package ch.puzzle.itc.mobiliar.presentation.release;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ch.puzzle.itc.mobiliar.business.deploy.boundary.DeploymentBoundary;
import ch.puzzle.itc.mobiliar.business.releasing.boundary.ReleaseLocator;
import ch.puzzle.itc.mobiliar.business.releasing.control.ReleaseMgmtService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.presentation.components.impl.PaginationComp;
import ch.puzzle.itc.mobiliar.presentation.util.GlobalMessageAppender;

/**
 * This screen service holds the information required for the whole view-session.
 *
 */
@Named
@SessionScoped
public class ReleaseMgmtScreenService extends PaginationComp implements Serializable {

	private static final long serialVersionUID = 4495708946323626547L;

	@Inject
	ReleaseMgmtService releaseMgmtService;

	@Inject
	ReleaseLocator releaseLocator;

	@Inject
    DeploymentBoundary deploymentBoundary;

	private List<ReleaseEntity> releases = new ArrayList<ReleaseEntity>();

	private Integer totalCount;

	private ReleaseEntity defaultRelease;

	/**
	 * Get the releases available - this method loads and caches the results for fast re-use
	 * @return
	 */
	public List<ReleaseEntity> getReleases() {
		if (releases == null || releases.isEmpty()) {
			releases = releaseMgmtService.loadReleasesForMgmt(getStartIndex(), getPageSize(), true);
		}
		return releases;
	}

	public ReleaseEntity getDefaultRelease(){
		if(defaultRelease == null){
			defaultRelease = releaseMgmtService.getDefaultRelease();
		}
		return defaultRelease;
	}

	/**
	 * Helper method to do lazy fetching of the release-count
	 * 
	 * @return
	 */
	private int countReleases() {
		if (totalCount == null) {
			totalCount = releaseMgmtService.countReleases();
		}
		return totalCount.intValue();
	}

	/**
	 * Clear the cached data to cause a reload during next rendering process
	 */
	private void reload(){
		releases.clear();
		totalCount = null;
		currentRelease = null;
		defaultRelease = null;
	}


	/*********** EDIT/REMOVE RELEASE *****************/

	public boolean isDefaultRelease(ReleaseEntity release){
		return getDefaultRelease().equals(release);
	}


	private ReleaseEntity currentRelease;
	private SortedMap<ResourceTypeEntity, SortedSet<ResourceEntity>> resourcesForCurrentRelease;

	public void remove(){
		releaseLocator.delete(currentRelease);
		GlobalMessageAppender.addSuccessMessage("Release " + currentRelease.getName() + " successfully removed.");
		reload();
	}

	/**
	 * Initialize the creation process of a release by calling before the "add" dialog is rendered.
	 */
	public void createRelease(){
		currentRelease = new ReleaseEntity();
	}

	public ReleaseEntity getCurrentRelease() {
		return currentRelease;
	}

	public Integer getReleaseId() {
		return currentRelease!=null ? currentRelease.getId() : null;
	}

	public void setReleaseId(Integer release) {
		for (ReleaseEntity r : getReleases()) {
			if(r.getId().equals(release)){
				currentRelease = r;
				break;
			}
		}
	}

	public void loadResourcesAndDeploymentsForRelease(Integer releaseId) {
		List<ResourceEntity> result = releaseMgmtService.getResourcesForRelease(releaseId);
		resourcesForCurrentRelease = new TreeMap();
		for (ResourceEntity r : result) {
			if (!resourcesForCurrentRelease.containsKey(r.getResourceType())) {
				resourcesForCurrentRelease.put(r.getResourceType(), new TreeSet<ResourceEntity>());
			}
			resourcesForCurrentRelease.get(r.getResourceType()).add(r);
		}
	}

	public boolean hasResourcesForCurrentRelease() {
		return resourcesForCurrentRelease != null
				&& !resourcesForCurrentRelease.isEmpty();
	}

	public List<ResourceTypeEntity> getResTypesForCurrentRelease() {
		if (hasResourcesForCurrentRelease()) {
			return new ArrayList<>(resourcesForCurrentRelease.keySet());
		}
		return null;
	}

	public List<ResourceEntity> getResForCurrentReleaseByType(Integer typeId) {
		if (hasResourcesForCurrentRelease()) {
			for (ResourceTypeEntity t : resourcesForCurrentRelease.keySet()) {
				if (t.getId().equals(typeId)) {
					return new ArrayList<>(resourcesForCurrentRelease.get(t));
				}
			}
		}
		return null;
	}

	/**
	 * Stores the release with the corresponding service method. Please call during edit- or create-form submission.
	 */
	public boolean save() {
		if (currentRelease.getId() == null) {
			releaseMgmtService.create(currentRelease);
		} else {
			releaseMgmtService.update(currentRelease);
		}
		GlobalMessageAppender.addSuccessMessage("Release " + currentRelease.getName()
				+ " successfully saved.");
		reload();
		return true;
	}

	@Override
	public void reloadData() {
		reload();
	}

	@Override
	public int getTotalCount() {
		return countReleases();
	}
}