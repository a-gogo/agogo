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

package ch.puzzle.itc.mobiliar.business.domain.commons;

import ch.puzzle.itc.mobiliar.business.database.control.QueryUtils;
import ch.puzzle.itc.mobiliar.business.environment.control.ContextDomainService;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyTypeEntity;
import ch.puzzle.itc.mobiliar.business.releasing.control.ReleaseMgmtPersistenceService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceGroupPersistenceService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeProvider;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.*;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceTypeNotFoundException;
import ch.puzzle.itc.mobiliar.common.util.ApplicationServerContainer;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The abstract domain service, used to provide generic functionalities for similar screens
 *
 */
@Stateless
public class CommonDomainService {
    @Inject
    private EntityManager entityManager;

    @Inject
    public Logger log;

    //TODO: commons service should not have dependencies to other services
    @Inject
    private ResourceTypeProvider resourceTypeProvider;

    @Inject
    private ContextDomainService contextDomainService;

    @Inject
    private PermissionService permissionService;

    @Inject
    private CommonQueries commonQueries;

    @Inject
    private ResourceDependencyResolverService dependencyResolverService;

    @Inject
    private ReleaseMgmtPersistenceService releaseMgmt;


    /**
     * Does not lazy load resources and releases for the resourceGroups. Use
     * {@link ResourceGroupPersistenceService#loadGroupsForTypeName(String, List)} instead.
     *
     * @return the alphabetically sorted list of available target platforms.
     */
    public List<ResourceGroupEntity> getRuntimeResourceGroups() {
        List<ResourceGroupEntity> allPlatformEntityList = QueryUtils.fetch(ResourceGroupEntity.class, commonQueries.fetchAllRuntimes(), 0, -1);

        Collections.sort(allPlatformEntityList, new Comparator<ResourceGroupEntity>() {
            @Override
            public int compare(ResourceGroupEntity arg0, ResourceGroupEntity arg1) {
                if (arg0 == null) {
                    return arg1 == null ? 0 : 1;
                }
                if (arg1 == null) {
                    return -1;
                }
                return arg0.getName().compareToIgnoreCase(arg1.getName());
            }
        });

        return allPlatformEntityList;
    }


    /**
     * @param resourceId
     * @return the resource entity with the given id
     * @throws ResourceNotFoundException
     */
    public ResourceEntity getResourceEntityById(int resourceId) throws ResourceNotFoundException {
        ResourceEntity result = null;
        try {
            result = entityManager.find(ResourceEntity.class, resourceId);
        } catch (NoResultException nre) {
            String message = "Die Ressource mit der Id: " + resourceId + " existiert nicht auf der DB";
            log.log(Level.WARNING, message, nre);
            throw new ResourceNotFoundException(message, nre);
        }
        return result;
    }

    /**
     * @param resourceGroupId
     * @param releaseId
     * @return
     */
    public ResourceEntity getResourceEntityByGroupAndRelease(Integer resourceGroupId, Integer releaseId) {
        try {
            TypedQuery<ResourceEntity> query = entityManager.createQuery(
                    "from ResourceEntity r where r.resourceGroup.id=:groupId and r.release.id=:releaseId", ResourceEntity.class);
            query.setParameter("groupId", resourceGroupId);
            query.setParameter("releaseId", releaseId);
            return query.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    /**
     * Hole für eine Id (resourceId) die ResourceTypeEntity
     *
     * @param resourceTypeId
     * @return
     * @throws ResourceTypeNotFoundException
     */
    public ResourceTypeEntity getResourceTypeEntityById(int resourceTypeId) throws ResourceTypeNotFoundException {
        ResourceTypeEntity result = null;
        try {
            result = entityManager.find(ResourceTypeEntity.class, resourceTypeId);

        } catch (NoResultException nre) {
            String message = "Der RessourceType mit der Id: " + resourceTypeId + " existiert nicht auf der DB";
            log.log(Level.WARNING, message);
            throw new ResourceTypeNotFoundException(message, nre);
        }
        return result;
    }

    public ResourceType getResourceTypeById(Integer resourceTypeId) throws ResourceTypeNotFoundException {
        ResourceTypeEntity resourceTypeEntityById = getResourceTypeEntityById(resourceTypeId);
        return ResourceType.createByResourceType(resourceTypeEntityById, null);
    }

    /**
     * Listet alle ResourceTypes auf.
     *
     * alphabetic sorted
     *
     * @return
     */
    public List<ResourceType> getAllResourceTypes(boolean rootTypesOnly) {
        List<ResourceType> result = new ArrayList<ResourceType>();

        TypedQuery<ResourceTypeEntity> q;
        if (rootTypesOnly) {
            q = entityManager.createQuery(
                    "select distinct r from ResourceTypeEntity r left join fetch r.childrenResourceTypes c where r.parentResourceType IS NULL order by r.name asc, c.name asc",
                    ResourceTypeEntity.class);
        } else {
            q = entityManager.createQuery("select distinct r from ResourceTypeEntity r left join fetch r.childrenResourceTypes c order by r.name asc, c.name asc ", ResourceTypeEntity.class);
        }

        List<ResourceTypeEntity> queryResult = q.getResultList();
        if (queryResult != null) {
            for (ResourceTypeEntity t : queryResult) {
                if (t.getName() != null) {
                    ResourceType resCat = ResourceType.createByResourceType(t, null);
                    result.add(resCat);
                }
            }
        }

        return result;
    }

    /**
     * Gibt ApplicationCollectorGroup Resource zurück. Falls diese noch nicht
     * existiert, so wird die Gruppe erstellt. Diese Gruppe wird für alle
     * Applikationen zugewiesen, für welche nach löschen ihrer
     * Applikationsgruppen keine Gruppe mehr zugewiesen sind.
     *
     * @return
     */
    public ApplicationServer createOrGetApplicationCollectorServer()  {
        ApplicationServer result;
        ResourceEntity appServerResource = getUniqueResourceByNameAndType(
                ApplicationServerContainer.APPSERVERCONTAINER.getDisplayName(), DefaultResourceTypeDefinition.APPLICATIONSERVER.name());

        if (appServerResource == null) {
            result = ApplicationServer.createByName(ApplicationServerContainer.APPSERVERCONTAINER.getDisplayName(), resourceTypeProvider, contextDomainService.getGlobalResourceContextEntity());
            result.getEntity().setDeletable(false);
            result.getEntity().setRelease(releaseMgmt.getDefaultRelease());
            entityManager.persist(result.getEntity());
        } else {
            result = ApplicationServer.createByResource(appServerResource, resourceTypeProvider, contextDomainService.getGlobalResourceContextEntity());
        }
        return result;
    }

    /**
     * Find unique resource entity with given name, type and release
     *
     * @param resourceName
     * @param typeName
     * @param releaseId
     * @return the resource or null if no resource was found
     */
    public ResourceEntity getUniqueResourceByNameAndTypeAndReleaseId(String resourceName, String typeName, Integer releaseId) {
        return getUniqueResource(resourceName, typeName, releaseId);
    }

    /**
     * Find unique resource entity with given name, type
     *
     * @param resourceName
     * @param typeName
     * @return the resource or null if no resource was found
     */
    public ResourceEntity getUniqueResourceByNameAndType(String resourceName, String typeName) {
        return getUniqueResource(resourceName, typeName, null);
    }

    private ResourceEntity getUniqueResource(String resourceName, String typeName, Integer releaseId) {
        ResourceEntity resource = null;
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<ResourceEntity> q = cb.createQuery(ResourceEntity.class);
            Root<ResourceEntity> r = q.from(ResourceEntity.class);
            Predicate resNamePred = cb.equal(r.get("name"), resourceName);
            Predicate typeNamePred = cb.equal(r.get("resourceType").get("name"), typeName);
            if (releaseId != null) {
                Predicate releasePred = cb.equal(r.get("release").get("id"), releaseId);
                q.where(cb.and(resNamePred, typeNamePred, releasePred));
            } else {
                q.where(cb.and(resNamePred, typeNamePred));
            }

            resource = entityManager.createQuery(q).getSingleResult();
        }
        catch (NoResultException e) {
            // do nothing
        }
        return resource;
    }


    /**
     * Hole für einen Name (prtTypeName) die PropertyType wenn sie existiert.
     *
     * @param prtTypeName
     * @return
     */
    public PropertyTypeEntity getUniquePropertyTypeByName(String prtTypeName) {

        PropertyTypeEntity propertyTypeEntity = null;
        try {
            Query searchUniquePropertyType = commonQueries.searchPropertyTypeByName(prtTypeName);
            propertyTypeEntity = (PropertyTypeEntity) searchUniquePropertyType.getSingleResult();
        } catch (NoResultException nre) {
            String message = "Das Property Type " + prtTypeName + " existiert nicht auf der DB";
            log.log(Level.WARNING, message);
        }
        return propertyTypeEntity;
    }

}
