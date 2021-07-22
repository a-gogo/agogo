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

import ch.puzzle.itc.mobiliar.business.database.control.Constants;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ResourceRepository {

    @Inject
    EntityManager entityManager;

    @Inject
    private Logger log;

    public ResourceEntity getResourceByNameAndRelease(String name, ReleaseEntity release) {
        return entityManager
                .createQuery("select r from ResourceEntity r where LOWER(r.name)=:name and r.release=:release",
                        ResourceEntity.class)
                .setParameter("name", name.toLowerCase()).setParameter("release", release).getSingleResult();
    }

    public ResourceEntity getResourceByGroupIdAndRelease(Integer id, ReleaseEntity release) {
        return entityManager
                .createQuery("select r from ResourceEntity r where r.resourceGroup.id=:id and r.release=:release",
                        ResourceEntity.class)
                .setParameter("id", id).setParameter("release", release).getSingleResult();
    }

    public ResourceEntity getApplicationByNameAndRelease(String name, ReleaseEntity release) {
        return entityManager
                .createQuery(
                        "select r from ResourceEntity r where LOWER(r.name)=:name and r.release=:release and r.resourceType.name=:typeName",
                        ResourceEntity.class)
                .setParameter("name", name.toLowerCase())
                .setParameter("typeName", DefaultResourceTypeDefinition.APPLICATION.name())
                .setParameter("release", release).getSingleResult();
    }

    public ResourceEntity getResourceByNameAndReleaseWithConsumedRelations(String name, ReleaseEntity release) {
        return entityManager
                .createQuery(
                        "select r from ResourceEntity r left join fetch r.consumedMasterRelations rel left join fetch rel.slaveResource where LOWER(r.name)=:name and r.release=:release",
                        ResourceEntity.class)
                .setParameter("name", name.toLowerCase()).setParameter("release", release).getSingleResult();
    }

    public ResourceEntity getResourceByNameAndReleaseWithProvidedRelations(String name, ReleaseEntity release) {
        return entityManager
                .createQuery(
                        "select r from ResourceEntity r left join fetch r.providedMasterRelations rel left join fetch rel.slaveResource where LOWER(r.name)=:name and r.release=:release",
                        ResourceEntity.class)
                .setParameter("name", name.toLowerCase()).setParameter("release", release).getSingleResult();
    }

    public ResourceEntity getResourceByNameAndReleaseWithAllRelations(String name, ReleaseEntity release) {
        return entityManager
                .createQuery(
                        "select r from ResourceEntity r left join fetch r.providedMasterRelations rel left join fetch r.consumedMasterRelations rel left join fetch rel.slaveResource where LOWER(r.name)=:name and r.release=:release",
                        ResourceEntity.class)
                .setParameter("name", name.toLowerCase()).setParameter("release", release).getSingleResult();
    }

    public ResourceEntity getResourceByIdWithRelations(int resourceId) {
        String qlString = "SELECT r FROM ResourceEntity r " +
                          "LEFT JOIN FETCH r.consumedMasterRelations rel " +
                          "LEFT JOIN FETCH rel.slaveResource " +
                          "WHERE LOWER(r.id)=:resourceId";
        return entityManager.createQuery(qlString,ResourceEntity.class)
                .setParameter("resourceId", resourceId)
                .getSingleResult();
    }

    public List<ResourceEntity> getResourcesByGroupNameWithRelations(String name) {
        return entityManager
                .createQuery(
                        "select r from ResourceEntity r " + "left join fetch r.resourceGroup rg "
                                + "left join fetch r.consumedMasterRelations rel "
                                + "left join fetch rel.slaveResource " + "where LOWER(rg.name)=:name",
                        ResourceEntity.class)
                .setParameter("name", name.toLowerCase()).getResultList();
    }

    public List<ResourceEntity> getResourcesByGroupNameWithAllRelationsOrderedByRelease(String name) {
        return entityManager.createQuery(
                "select r from ResourceEntity r " + "left join fetch r.resourceGroup rg "
                        + "left join fetch r.consumedMasterRelations rel " + "left join fetch rel.slaveResource "
                        + "where LOWER(rg.name)=:name order by r.release.installationInProductionAt asc",
                ResourceEntity.class).setParameter("name", name.toLowerCase()).getResultList();
    }

    public ResourceEntity loadWithResourceGroupAndRelatedResourcesForId(Integer id) {
        List<ResourceEntity> entity = entityManager
                .createQuery("select r from ResourceEntity r " + "left join fetch r.resourceGroup g "
                        + "left join fetch g.resources " + "where r.id=:resId", ResourceEntity.class)
                .setParameter("resId", id).setMaxResults(1).getResultList();
        return entity.isEmpty() ? null : entity.get(0);
    }

    public ResourceEntity loadWithFunctionsAndMiksForId(Integer id) {
        List<ResourceEntity> entity = entityManager
                .createQuery("select r from ResourceEntity r " + "left join fetch r.functions f "
                        + "left join fetch f.miks m " + "where r.id=:resId", ResourceEntity.class)
                .setParameter("resId", id).setMaxResults(1).getResultList();
        return entity.isEmpty() ? null : entity.get(0);
    }

    public ResourceEntity find(Integer resourceId) {
        return entityManager.find(ResourceEntity.class, resourceId);
    }

    /**
     * Removes a ResourceEntity preserving its deployments
     *
     * @param resource
     */
    public void remove(ResourceEntity resource) {
        if (resource.getResourceType().isRuntimeType()) {
            removeRuntime(resource);
        } else {
            removeResource(resource);
        }
    }

    private void removeResource(ResourceEntity resource) {
        Integer resourceId = resource.getId();
        if (resource.getDeployments() != null) {
            for (DeploymentEntity deploymentEntity : resource.getDeployments()) {
                deploymentEntity.setExResourceId(resourceId);
                deploymentEntity.setResource(null);
                entityManager.merge(deploymentEntity);
            }
        }
        entityManager.remove(resource);
        log.info("Resource with Id: " + resourceId + " was removed from the db");
    }

    private void removeRuntime(ResourceEntity resource) {
        Integer resourceId = resource.getId();
        if (resource.getDeploymentsOfRuntime() != null) {
            for (DeploymentEntity deploymentEntity : resource.getDeploymentsOfRuntime()) {
                deploymentEntity.setExRuntimeResourceId(resourceId);
                deploymentEntity.setRuntime(null);
                entityManager.merge(deploymentEntity);
            }
        }
        entityManager.remove(resource);
        log.info("Runtime with Id: " + resourceId + " was removed from the db");
    }

    public void removeResourceGroup(ResourceGroupEntity resourceGroup) {
        entityManager.remove(resourceGroup);
    }

    /**
     * Für JavaBatch Monitor
     * 
     * @param name
     * @return
     */
    public List<ResourceEntity> getResourceByName(String name) {
        return entityManager
                .createQuery("select r from ResourceEntity r where LOWER(r.name)=:name", ResourceEntity.class)
                .setParameter("name", name.toLowerCase()).getResultList();
    }

    /**
     * Für JavaBatch Monitor
     * 
     * @param name
     * @param release
     * @return
     */
    public ResourceEntity getMasterResourceByNameAndReleaseWithRelations(String name, ReleaseEntity release) {
        return entityManager
                .createQuery(
                        "select r from ResourceEntity r left join fetch r.consumedMasterRelations rel left join fetch rel.masterResource where LOWER(r.name)=:name and r.release=:release",
                        ResourceEntity.class)
                .setParameter("name", name.toLowerCase()).setParameter("release", release).getSingleResult();
    }

    /**
     * Für JavaBatch Monitor<br>
     * Resourcetypes see TAMW_Resourcetype
     *
     * 
     * @param resource
     * @return
     */
    public List<ResourceEntity> getAllApplicationsWithResource(int resource) {
        return entityManager.createQuery(
                "select distinct r from ResourceEntity r left join " + "fetch r.consumedMasterRelations rel "
                        + "left join fetch rel.slaveResource s " + "where s.resourceType.id =:resource ",
                ResourceEntity.class).setMaxResults(101).setParameter("resource", resource).getResultList();
    }

    /**
     *  Für JavaBatch Monitor
     * @param apps
     * @return
     */
    public List<ResourceEntity> getBatchJobConsumedResources(List<String> apps) {
        List<Integer> rsc = new ArrayList<>();
        rsc.add(Constants.RESOURCETYPE_DB2);
        rsc.add(Constants.RESOURCETYPE_ORACLE);
        rsc.add(Constants.RESOURCETYPE_WS);
        rsc.add(Constants.RESOURCETYPE_REST);
        rsc.add(Constants.RESOURCETYPE_FILE);
        return entityManager
                .createQuery(
                        "select r from ResourceEntity r " +      //
                                "left join fetch r.resourceGroup rg " +      //
                                "left join fetch r.consumedMasterRelations rel " +      //
                                "left join fetch rel.slaveResource slave " +      //
                                "where LOWER(rg.name) in :apps " + "and slave.resourceType.id in :rsc ",
                        ResourceEntity.class)
                .setParameter("apps", apps).setParameter("rsc", rsc).getResultList();
    }

    /**
     *  Für JavaBatch Monitor
     * @param apps
     * @return
     */
    public List<ResourceEntity> getBatchJobProvidedResources(List<String> apps) {
        List<Integer> rsc = new ArrayList<>();
        rsc.add(Constants.RESOURCETYPE_DB2);
        rsc.add(Constants.RESOURCETYPE_ORACLE);
        rsc.add(Constants.RESOURCETYPE_WS);
        rsc.add(Constants.RESOURCETYPE_REST);
        rsc.add(Constants.RESOURCETYPE_FILE);
        return entityManager
                .createQuery(
                        "select r from ResourceEntity r " +      //
                                "left join fetch r.resourceGroup rg " +      //
                                "left join fetch r.providedMasterRelations rel " +      //
                                "left join fetch rel.slaveResource slave " +      //
                                "where LOWER(rg.name) in :apps " + "and slave.resourceType.id in :rsc ",
                        ResourceEntity.class)
                .setParameter("apps", apps).setParameter("rsc", rsc).getResultList();
    }

    /**
     * Für JavaBatch Monitor <br>
     * read only Applications
     * @param appServerList
     * @return
     */
    public List<ResourceEntity> getAppToAppServerMapping(List<String> appServerList) {
        return entityManager.createQuery(
                "select r from ResourceEntity r left join fetch r.consumedMasterRelations rel " //
                        + "left join fetch rel.slaveResource s where s.resourceType.id = "
                        + Constants.RESOURCETYPE_APPLICATION + " AND LOWER(r.name) in :appServerList",
                ResourceEntity.class).setParameter("appServerList", appServerList).getResultList();

    }
    
}
