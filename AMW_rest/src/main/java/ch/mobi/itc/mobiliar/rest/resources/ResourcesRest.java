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

package ch.mobi.itc.mobiliar.rest.resources;

import ch.mobi.itc.mobiliar.rest.dtos.*;
import ch.mobi.itc.mobiliar.rest.exceptions.ExceptionDto;
import ch.puzzle.itc.mobiliar.business.database.control.Constants;
import ch.puzzle.itc.mobiliar.business.deploy.boundary.DeploymentBoundary;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.property.boundary.PropertyEditor;
import ch.puzzle.itc.mobiliar.business.releasing.boundary.ReleaseLocator;
import ch.puzzle.itc.mobiliar.business.releasing.control.ReleaseMgmtService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.*;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceResult;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.Resource;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.boundary.ResourceRelationLocator;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationService;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.server.boundary.ServerView;
import ch.puzzle.itc.mobiliar.business.server.entity.ServerTuple;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.*;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@RequestScoped
@Path("/resources")
@Api(value = "/resources", description = "Resources")
public class ResourcesRest {

    private static final String DB2 = "DB2";
    private static final String ORACLE = "Oracle";
    private static final String WEBSERVICE = "Webservice";
    private static final String FILE = "File";

    @Inject
    ReleaseLocator releaseLocator;

    @Inject
    ReleaseMgmtService releaseMgmtService;

    @Inject
    DeploymentBoundary deploymentBoundary;

    @Inject
    ResourceGroupLocator resourceGroupLocator;

    @Inject
    ResourceBoundary resourceBoundary;

    @Inject
    ResourceLocator resourceLocator;

    @Inject
    CopyResource copyResource;

    @Inject
    ResourceRelationsRest resourceRelations;

    @Inject
    ResourceTypeLocator resourceTypeLocator;

    @Inject
    ResourcePropertiesRest resourceProperties;

    @Inject
    ResourceDependencyResolverService resourceDependencyResolverService;

    @Inject
    PermissionBoundary permissionBoundary;

    @Inject
    ResourceTemplatesRest resourceTemplatesRest;

    @Inject
    ResourceRelationService resourceRelationService;

    @Inject
    private ServerView serverView;

    /**
     * Fuer JavaBatch Monitor
     */
    @Inject
    ResourceRelationLocator resourceRelationLocator;

    /**
     * Fuer JavaBatch Monitor
     */
    @Inject
    ContextLocator contextLocator;

    /**
     * Fuer JavaBatch Monitor
     */
    @Inject
    PropertyEditor propertyEditor;

    @GET
    @ApiOperation(value = "Get get resource groups", notes = "Returns the available resource groups")
    public List<ResourceGroupDTO> getResources(
            @ApiParam(value = "a resource type, the list should be filtered by") @QueryParam("type") String type) {
        List<ResourceGroupDTO> result = new ArrayList<>();
        List<ResourceGroupEntity> resourceGroups;
        // used by angular
        if (type != null) {
            // TODO my favorites only
            resourceGroups = resourceGroupLocator.getGroupsForType(type, Collections.EMPTY_LIST, true, true);
        } else {
            resourceGroups = resourceGroupLocator.getResourceGroups();
        }

        for (ResourceGroupEntity resourceGroup : resourceGroups) {
            List<ReleaseEntity> releases = new ArrayList<>();
            for (ResourceEntity resource : resourceGroup.getResources()) {
                releases.add(resource.getRelease());
            }
            result.add(new ResourceGroupDTO(resourceGroup, releases));
        }
        return result;
    }

    // TODO: better GET /{resourceId}/properties/name?
    @Path("/name/{resourceId}")
    @GET
    @ApiOperation(value = "Get resource name by id")
    public Response getResourceName(@PathParam("resourceId") Integer resourceId) {
        Map<String, String> responseMap = new HashMap<>();
        String resourceName = resourceBoundary.getResourceName(resourceId);
        responseMap.put("name", resourceName);
        return Response.status(Response.Status.OK).entity(responseMap).build();
    }

    @Path("/{resourceGroupName}")
    @GET
    @ApiOperation(value = "Get a resource group")
    public ResourceGroupDTO getResourceGroup(@PathParam("resourceGroupName") String resourceGroupName) {
        ResourceGroupEntity resourceGroupByName = resourceGroupLocator.getResourceGroupByName(resourceGroupName);
        List<ReleaseEntity> releases = releaseLocator.getReleasesForResourceGroup(resourceGroupByName);
        return new ResourceGroupDTO(resourceGroupByName, releases);
    }

    @Path("/{resourceGroupName}/{releaseName}")
    @GET
    @ApiOperation(value = "Get resource in specific release")
    public ResourceDTO getResource(@PathParam("resourceGroupName") String resourceGroupName,
                                   @PathParam("releaseName") String releaseName,
                                   @QueryParam("env") @DefaultValue("Global") String environment,
                                   @QueryParam("type") String resourceType) throws ValidationException, ResourceNotFoundException {
        ResourceEntity resourceByRelease = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName, releaseName);
        return new ResourceDTO(resourceByRelease, resourceRelations.getResourceRelations(resourceGroupName,
                releaseName, resourceType), resourceProperties.getResourceProperties(resourceGroupName, releaseName,
                environment), resourceTemplatesRest.getResourceTemplates(resourceGroupName, releaseName));
    }

    @Path("/{resourceGroupName}/lte/{releaseName}")
    @GET
    @ApiOperation(value = "Get exact or closest past resource release")
    public ResourceDTO getExactOrClosestPastRelease(@PathParam("resourceGroupName") String resourceGroupName,
                                                    @PathParam("releaseName") String releaseName,
                                                    @QueryParam("env") @DefaultValue("Global") String environment,
                                                    @QueryParam("type") String resourceType) throws ValidationException, ResourceNotFoundException {
        ReleaseEntity release = resourceLocator.getExactOrClosestPastReleaseByGroupNameAndRelease(resourceGroupName, releaseName);
        return new ResourceDTO(release, resourceRelations.getResourceRelations(resourceGroupName,
                release.getName(), resourceType), resourceProperties.getResourceProperties(resourceGroupName, release.getName(),
                environment), resourceTemplatesRest.getResourceTemplates(resourceGroupName, release.getName()));
    }

    @Path("/resourceGroups")
    @GET
    @ApiOperation(value = "Get all available ResourceGroups - used by Angular")
    public List<ResourceGroupDTO> getAllResourceGroups(@QueryParam("includeAppServerContainer") boolean includeAppServerContainer) {
        List<ResourceGroupEntity> resourceGroups = resourceGroupLocator.getAllResourceGroupsByName();
        List<ResourceGroupDTO> resourceDTOs = new ArrayList<>();
        if (includeAppServerContainer) {
            for (ResourceGroupEntity resourceGroup : resourceGroups) {
                resourceDTOs.add(new ResourceGroupDTO(resourceGroup, null));
            }
        } else {
            for (ResourceGroupEntity resourceGroup : resourceGroups) {
                if (!resourceGroup.isAppServerContainer()) {
                    resourceDTOs.add(new ResourceGroupDTO(resourceGroup, null));
                }
            }
        }
        return resourceDTOs;
    }

    // TODO: should be removed and instead handeled with a 404
    @Path("/exists/{resourceId}")
    @GET
    @ApiOperation(value = "Checks if a specific Resource still exists - used by Angular")
    public Response isExistingResourceGroup(@PathParam("resourceId") Integer resourceId) {
        ResourceEntity resource = resourceLocator.getResourceById(resourceId);
        if (resource == null) {
            return Response.ok(false).build();
        }
        return Response.ok(true).build();
    }

    @Path("/resourceGroups/{resourceGroupId}/releases/{releaseId}")
    @GET
    @ApiOperation(value = "Get resource in specific release - used by Angular")
    public ResourceDTO getResourceRelationListForRelease(@PathParam("resourceGroupId") Integer resourceGroupId,
                                                         @PathParam("releaseId") Integer releaseId) {

        ResourceEntity resource = resourceDependencyResolverService.getResourceEntityForRelease(resourceGroupId, releaseId);
        if (resource == null) {
            return null;
        }
        List<String> uniqueNames = new ArrayList<>();
        List<ResourceRelationDTO> resourceRelationDTOs = new ArrayList<>();
        for (ConsumedResourceRelationEntity consumedResourceRelationEntity : resource.getConsumedMasterRelations()) {
            if (!uniqueNames.contains(consumedResourceRelationEntity.getSlaveResource().getName())) {
                uniqueNames.add(consumedResourceRelationEntity.getSlaveResource().getName());
                resourceRelationDTOs.add(new ResourceRelationDTO(consumedResourceRelationEntity));
            }
        }
        return new ResourceDTO(resource, resourceRelationDTOs);
    }

    @Path("/resourceGroups/{resourceGroupId}/releases/{releaseId}")
    @DELETE
    @ApiOperation(value = "Delete a specific resource release")
    public Response deleteResourceRelease(@PathParam("resourceGroupId") Integer resourceGroupId,
                                                            @PathParam("releaseId") Integer releaseId) throws ResourceNotFoundException, ElementAlreadyExistsException, ForeignableOwnerViolationException {
        ResourceEntity resource = resourceDependencyResolverService.getResourceEntityForRelease(resourceGroupId, releaseId);
        if (resource == null) {
            return Response.status(NOT_FOUND).entity(new ExceptionDto("Resource not found")).build();
        }
        resourceBoundary.removeResource(ForeignableOwner.getSystemOwner(), resource.getId());
        return Response.ok().build();
    }

    @Path("/resourceGroups/{resourceGroupId}/releases/{releaseId}/appWithVersions/")
    @GET
    @ApiOperation(value = "Get application with version for a specific resourceGroup, release and context(s) - used by Angular")
    public Response getApplicationsWithVersionForRelease(@PathParam("resourceGroupId") Integer resourceGroupId,
                                                         @PathParam("releaseId") Integer releaseId,
                                                         @QueryParam("context") List<Integer> contextIds) {

        ResourceEntity appServer = resourceLocator.getExactOrClosestPastReleaseByGroupIdAndReleaseId(resourceGroupId, releaseId);
        if (appServer == null) {
            return Response.status(NOT_FOUND).build();
        }
        ReleaseEntity release = releaseLocator.getReleaseById(releaseId);
        List<AppWithVersionDTO> apps = new ArrayList<>();
        List<DeploymentEntity.ApplicationWithVersion> appVersions = deploymentBoundary.getVersions(appServer, contextIds, release);
        for (DeploymentEntity.ApplicationWithVersion appVersion : appVersions) {
            apps.add(new AppWithVersionDTO(appVersion.getApplicationName(), appVersion.getApplicationId(), appVersion.getVersion()));
        }
        return Response.ok(apps).build();

    }

    @Path("/resourceGroups/{resourceGroupId}/releases/")
    @GET
    @ApiOperation(value = "Get deployable releases for a specific resourceGroup - used by Angular")
    public Response getDeployableReleasesForResourceGroup(@PathParam("resourceGroupId") Integer resourceGroupId) {

        ResourceGroupEntity group = resourceGroupLocator.getResourceGroupById(resourceGroupId);
        if (group == null) {
            return Response.status(NOT_FOUND).build();
        }
        List<ResourceDTO> releases = new ArrayList<>();
        List<ReleaseEntity> deployableReleases = releaseMgmtService.getDeployableReleasesForResourceGroup(group);
        for (ReleaseEntity deployableRelease : deployableReleases) {
            releases.add(new ResourceDTO(deployableRelease));
        }
        return Response.ok(releases).build();

    }

    @Path("/resourceGroups/{resourceGroupId}/releases/mostRelevant/")
    @GET
    @ApiOperation(value = "Get most relevant release for a specific resourceGroup - used by Angular")
    public Response getMostRelevantReleaseForResourceGroup(@PathParam("resourceGroupId") Integer resourceGroupId) {
        ResourceGroupEntity group = resourceGroupLocator.getResourceGroupById(resourceGroupId);
        if (group == null) {
            return Response.status(NOT_FOUND).build();
        }
        SortedSet<ReleaseEntity> deployableReleases = new TreeSet(releaseMgmtService.getDeployableReleasesForResourceGroup(group));
        ResourceDTO mostRelevant = new ResourceDTO(resourceDependencyResolverService.findMostRelevantRelease(deployableReleases, null));
        return Response.ok(mostRelevant).build();
    }

    @Path("/resourceTypes")
    @GET
    @ApiOperation(value = "Get all available ResourceTypes - used by Angular")
    public List<ResourceTypeDTO> getAllResourceTypes() throws ValidationException {
        List<ResourceTypeEntity> resourceTypes = resourceTypeLocator.getAllResourceTypes();
        List<ResourceTypeDTO> resourceTypeDTOs = new ArrayList<>();
        for (ResourceTypeEntity resourceType : resourceTypes) {
            resourceTypeDTOs.add(new ResourceTypeDTO(resourceType));
        }
        return resourceTypeDTOs;
    }

    @Path("/resourceGroups/{resourceGroupId}/canCreateShakedownTest")
    @GET
    @ApiOperation(value = "Checks is caller is allowed to create/execute ShakedownTests - used by Angular")
    public Response canCreateShakedownTest(@PathParam("resourceGroupId") Integer resourceGroupId) {
        if (resourceGroupId == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        boolean hasPermission = permissionBoundary.hasPermissionToCreateShakedownTests(resourceGroupId);
        return Response.ok(hasPermission).build();
    }

    // Fuer JavaBatch Monitor

    /**
     * Alle Consumed Resources zu dieser App <br>
     * App Details<br>
     * Job name<br>
     * Server<br>
     * consumed resources (short)
     */
    @Path("/batchjobResources/{app}")
    @Produces("application/json")
    @GET
    @ApiOperation(value = "Get batch job resources", notes = "Returns the consumed resources for this app")
    public List<BatchResourceDTO> getBatchJobResources(
            @ApiParam(value = "return resources for this app") @PathParam("app") String app)
            throws ValidationException {
        if (app == null || app.isEmpty()) {
            throw new ValidationException("App must not be empty");
        }
        List<String> apps = new ArrayList<>();
        apps.add(app);
        List<BatchResourceDTO> list = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for (ResourceEntity re : resourceLocator.getBatchJobConsumedResources(apps)) {
            for (ConsumedResourceRelationEntity r : re.getConsumedMasterRelations()) {
                mapResourceDTO(list, set, r.getSlaveResource().getResourceType().getId(),
                        r.getSlaveResource().getName(), true);
            }
        }
        set.clear();
        for (ResourceEntity re : resourceLocator.getBatchJobProvidedResources(apps)) {
            for (ProvidedResourceRelationEntity r : re.getProvidedMasterRelations()) {
                mapResourceDTO(list, set, r.getSlaveResource().getResourceType().getId(),
                        r.getSlaveResource().getName(), false);
            }
        }
        return list;
    }

    // Fuer JavaBatch Monitor
    void mapResourceDTO(List<BatchResourceDTO> list, Set<String> set, int id, String name, Boolean consumed) {
        BatchResourceDTO dto = new BatchResourceDTO();
        dto.setConsumed(consumed);
        if (id == Constants.RESOURCETYPE_DB2) {
            dto.setType(DB2);
        }
        if (id == Constants.RESOURCETYPE_ORACLE) {
            dto.setType(ORACLE);
        }
        if (id == Constants.RESOURCETYPE_WS || id == Constants.RESOURCETYPE_REST) {
            dto.setType(WEBSERVICE);
        }
        if (id == Constants.RESOURCETYPE_FILE) {
            dto.setType(FILE);
        }
        if (!set.contains(name)) {
            set.add(name);
            dto.setName(name);
            list.add(dto);
        }
    }

    // Fuer JavaBatch Monitor

    /**
     * Alle Apps mit Resourcen vom Typ 'batchjob' lesen: <br>
     * App Details<br>
     * Job name<br>
     * Server<br>
     * consumed resources (short)
     */
    @Path("/resource/{env}/{resourceType : \\d+}")
    @Produces("application/json")
    @GET
    @ApiOperation(value = "Get batch job inventory", notes = "Returns the available e.g. batch applications and their related resources (only db2, oracle, ws, rest and file)")
    public BatchJobInventoryDTO getBatchJobInventar(
            @ApiParam(value = "return batch job inventory") @PathParam("env") String env,
            @ApiParam(value = "resource type (int)") @PathParam("resourceType") int resourceType,
            @ApiParam(value = "Filter by Applicationname or -part") @QueryParam("app") String appFilter,
            @ApiParam(value = "Filter by Resource or -part") @QueryParam("job") String jobFilter,
            @ApiParam(value = "Filter by Release") @QueryParam("rel") String relFilter,
            @ApiParam(value = "Filter by consumed DB type") @QueryParam("db") String dbFilter,
            @ApiParam(value = "Filter by consumed WS name or part") @QueryParam("ws") String wsFilter) {
        BatchJobInventoryDTO inventory = new BatchJobInventoryDTO();

        // Applications und ihre Resourcen lesen
        List<ResourceEntity> reList = resourceLocator.getAllApplicationsWithResource(resourceType);

        List<String> appList = new ArrayList<>();
        for (ResourceEntity re : reList) {
            for (ConsumedResourceRelationEntity a : re.getConsumedMasterRelations()) {
                BatchResourceRelationDTO job = new BatchResourceRelationDTO(a);
                // filter by appname or part of name
                if (appFilter != null && !appFilter.isEmpty() && job.getBatchApp() != null) {
                    if (!job.getBatchApp().toLowerCase().contains(appFilter.toLowerCase())) {
                        // do not add to list
                        break;
                    }
                }

                // filter by release (must match either in app or in job)
                if (relFilter != null && !relFilter.isEmpty() &&                               //
                        (job.getBatchAppRelease() != null)) { // || job.getBatchJobRelease() != null)) {
                    if (!job.getBatchAppRelease().toLowerCase().contains(relFilter.toLowerCase())) {
                        // job-Release Filter ist nicht relevant, da dort der Release nur auf Stufe 'standardJob' wirkt
                        // || job.getBatchJobRelease().toLowerCase().contains(relFilter.toLowerCase()))) {
                        // do not add to list
                        break;
                    }
                }

                inventory.getBatchJobs().add(job);
                if (!appList.contains(job.getBatchApp())) {
                    appList.add(job.getBatchApp().toLowerCase());
                }

            }
        }

        // filter by jobname or part
        List<BatchResourceRelationDTO> jobsToRemove = new ArrayList<>();
        for (BatchResourceRelationDTO job : inventory.getBatchJobs()) {
            if (jobFilter != null && !jobFilter.isEmpty()) {
                if (job.getJobName() == null) {
                    // remove from list
                    jobsToRemove.add(job);
                } else if (!job.getJobName().toLowerCase().contains(jobFilter.toLowerCase())) {
                    // remove from list
                    jobsToRemove.add(job);
                }
            }
        }
        for (BatchResourceRelationDTO deleteMe : jobsToRemove) {
            inventory.getBatchJobs().remove(deleteMe);
        }

        // Alle 'JavaBatch' Server lesen
        String appServer = null; // alle lesen
        String runtime = "JavaBatch*"; // aber nur Batch
        String host = null;
        String node = null;
        List<ServerTuple> servers = serverView.getServers(host, appServer, runtime, node, env, true);
        List<String> appServerList = new ArrayList<>();
        for (ServerTuple t : servers) {
            appServerList.add(t.getAppServer().toLowerCase());
        }

        // Mapping von fofa_batch nach ch_mobi_fofa_fofa_selection, funktioniert nur falls Server zugeordnet
        // Map contains: <ch_mobi_fofa_fofa_selection, fofa_batch>
        Map<String, String> mapping = resourceLocator.getAppToAppServerMapping(appServerList);
        for (BatchResourceRelationDTO job : inventory.getBatchJobs()) {
            String appName = mapping.get(job.getBatchApp());
            job.setAppName(appName);
        }

        for (BatchResourceRelationDTO job : inventory.getBatchJobs()) {
            for (ServerTuple server : servers) {
                if (job.getAppName() != null && job.getAppName().equals(server.getAppServer())) {
                    job.setBatchServer(server.getHost());
                }
            }
        }

        // Flags fuer verwendete Ressourcen: DB2, Oracle, WS und File ermitteln
        List<ResourceEntity> resources = resourceLocator.getBatchJobConsumedResources(appList);
        if (resources != null && !resources.isEmpty()) {
            for (BatchResourceRelationDTO job : inventory.getBatchJobs()) {
                for (ResourceEntity re : resources) {

                    for (ConsumedResourceRelationEntity r : re.getConsumedMasterRelations()) {

                        if (r.getMasterResourceName().equals(job.getBatchApp())) {
                            if (r.getSlaveResourceTypeId().equals(Constants.RESOURCETYPE_DB2)) {
                                job.setDb2(true);
                            }
                            if (r.getSlaveResourceTypeId().equals(Constants.RESOURCETYPE_ORACLE)) {
                                job.setOracle(true);
                            }
                            if (r.getSlaveResourceTypeId().equals(Constants.RESOURCETYPE_WS)
                                    || r.getSlaveResourceTypeId().equals(Constants.RESOURCETYPE_REST)) {
                                job.setWs(true);
                                if (!job.getWsList().contains(r.getSlaveResource().getName())) {
                                    job.getWsList().add(r.getSlaveResource().getName());
                                }
                            }
                            if (r.getSlaveResourceTypeId().equals(Constants.RESOURCETYPE_FILE)) {
                                job.setFile(true);
                            }
                        }
                    }
                }
            }
        }

        // filter by used db (Empty, DB2 or Oracle from Constant Dropdown)
        // filter by consumed WS
        List<BatchResourceRelationDTO> deleteJobs = new ArrayList<>();
        for (BatchResourceRelationDTO job : inventory.getBatchJobs()) {

            if (dbFilter != null && !dbFilter.isEmpty()) {
                if (dbFilter.equalsIgnoreCase(DB2) && !job.getDb2()) {
                    // remove from result
                    deleteJobs.add(job);
                }
                if (dbFilter.equalsIgnoreCase(ORACLE) && !job.getOracle()) {
                    // remove from result
                    deleteJobs.add(job);
                }
            }
            if (wsFilter != null && !wsFilter.isEmpty()) {
                boolean found = false;
                for (String ws : job.getWsList()) {
                    if (ws.toLowerCase().contains(wsFilter)) {
                        found = true;
                    }
                }
                if (!found) {
                    if (!deleteJobs.contains(job)) {
                        deleteJobs.add(job);
                    }
                }
            }
        }
        for (BatchResourceRelationDTO job : deleteJobs) {
            inventory.getBatchJobs().remove(job);
        }

        return inventory;
    }

    /**
     * Creates a new resource and returns its location.
     *
     * @param request containing a ResourceReleaseDTO
     */
    @POST
    @ApiOperation(value = "Add a Resource")
    public Response addResource(@ApiParam("Add a Resource") ResourceReleaseDTO request) {
        Resource resource;
        if (StringUtils.isEmpty(request.getName())) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto("Resource name must not be empty")).build();
        }
        if (StringUtils.isEmpty(request.getReleaseName())) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto("Release name must not be empty")).build();
        }
        try {
            resource = resourceBoundary.createNewResourceByName(ForeignableOwner.getSystemOwner(), request.getName(),
                    request.getType(), request.getReleaseName());
        } catch (AMWException e) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto(e.getMessage())).build();
        }
        return Response.status(CREATED).header("Location", "/resources/" + resource.getName()).build();
    }

    /**
     * Creates a new resource release of an existing resource and returns its location.
     *
     * @param request containing a ResourceReleaseCopyDTO
     */
    @POST
    @Path("/{resourceGroupName}")
    @ApiOperation(value = "Create a new Resource Release")
    public Response addNewResourceRelease(@ApiParam("Create a Resource Release") ResourceReleaseCopyDTO request,
                                          @PathParam("resourceGroupName") String resourceGroupName) {
        CopyResourceResult copyResourceResult;
        if (StringUtils.isEmpty(request.getReleaseName())) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto("Release name must not be empty")).build();
        }
        if (StringUtils.isEmpty(request.getSourceReleaseName())) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto("Source release name must not be empty")).build();
        }
        try {
            copyResourceResult = copyResource.doCreateResourceRelease(resourceGroupName, request.getReleaseName(),
                    request.getSourceReleaseName(), ForeignableOwner.getSystemOwner());
        } catch (ForeignableOwnerViolationException | AMWException e) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto(e.getMessage())).build();
        }
        if (!copyResourceResult.isSuccess()) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto("Release creation failed")).build();
        }
        return Response.status(CREATED).header("Location", "/resources/" + copyResourceResult.getTargetResourceName() + "/" + request.getReleaseName()).build();
    }

    /**
     * Copies the properties of a Resource into another
     */
    @Path("/{resourceGroupName}/{releaseName}/copyFrom")
    @PUT
    @ApiOperation(value = "Copy the properties of a Resource into another")
    public Response copyFromResource(@ApiParam(value = "The target ResourceGroup (to)") @PathParam("resourceGroupName") String targetResourceGroupName,
                                     @ApiParam(value = "The target ReleaseName (to)") @PathParam("releaseName") String targetReleaseName,
                                     @ApiParam(value = "The origin ResourceGroup (from)") @QueryParam("originResourceGroupName") String originResourceGroupName,
                                     @ApiParam(value = "The origin ReleaseName (from)") @QueryParam("originReleaseName") String originReleaseName) throws ValidationException {

        try {
            CopyResourceResult copyResourceResult = copyResource.doCopyResource(targetResourceGroupName, targetReleaseName, originResourceGroupName, originReleaseName);
            if (!copyResourceResult.isSuccess()) {
                return Response.status(BAD_REQUEST).entity(new ExceptionDto("Copy from Origin failed")).build();
            }
            return Response.ok().build();
        } catch (ForeignableOwnerViolationException | AMWException e) {
            return Response.status(BAD_REQUEST).entity(new ExceptionDto(e.getMessage())).build();
        }
    }

    @Path("/{resourceGroupName}/{releaseName}/dependencies/")
    @GET
    @ApiOperation(value = "Get dependencies of a resource in a specific release")
    public Response getResourceDependencies(@PathParam("resourceGroupName") String resourceGroupName,
                                            @PathParam("releaseName") String releaseName) throws ValidationException {
        ResourceEntity resource = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName, releaseName);
        if (resource == null) {
            return Response.status(NOT_FOUND).entity(new ExceptionDto("Resource not found")).build();
        }
        List<ConsumedResourceRelationEntity> consumedSlaveRelations = resourceRelationService.getConsumedSlaveRelations(resource);
        List<ProvidedResourceRelationEntity> providedSlaveRelations = resourceRelationService.getProvidedSlaveRelations(resource);
        List<DependencyDTO> resourceRelationDTOs = new ArrayList<>(consumedSlaveRelations.size() + providedSlaveRelations.size());
        for (ConsumedResourceRelationEntity consumedResourceRelationEntity : consumedSlaveRelations) {
            resourceRelationDTOs.add(new DependencyDTO(consumedResourceRelationEntity));
        }
        for (ProvidedResourceRelationEntity providedResourceRelationEntity : providedSlaveRelations) {
            resourceRelationDTOs.add(new DependencyDTO(providedResourceRelationEntity));
        }
        return Response.status(Response.Status.OK).entity(resourceRelationDTOs).build();
    }

}
