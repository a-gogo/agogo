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

package ch.mobi.itc.mobiliar.rest.deployments;

import ch.mobi.itc.mobiliar.rest.dtos.*;
import ch.mobi.itc.mobiliar.rest.exceptions.ExceptionDto;
import ch.puzzle.itc.mobiliar.business.deploy.boundary.DeploymentBoundary;
import ch.puzzle.itc.mobiliar.business.deploy.entity.*;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity.ApplicationWithVersion;
import ch.puzzle.itc.mobiliar.business.deploy.entity.NodeJobEntity.NodeJobStatus;
import ch.puzzle.itc.mobiliar.business.deploymentparameter.control.KeyRepository;
import ch.puzzle.itc.mobiliar.business.deploymentparameter.entity.DeploymentParameter;
import ch.puzzle.itc.mobiliar.business.deploymentparameter.entity.Key;
import ch.puzzle.itc.mobiliar.business.domain.commons.CommonFilterService;
import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.environment.control.EnvironmentsScreenDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratorDomainServiceWithAppServerRelations;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.releasing.control.ReleaseMgmtService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceGroupPersistenceService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeProvider;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.common.exception.DeploymentStateException;
import ch.puzzle.itc.mobiliar.common.exception.NotFoundException;
import ch.puzzle.itc.mobiliar.common.util.ConfigurationService;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.common.util.Tuple;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.ValidationException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentFilterTypes.*;
import static ch.puzzle.itc.mobiliar.common.util.ConfigKey.FEATURE_DISABLE_ANGULAR_DEPLOYMENT_GUI;

@Stateless
@Path("/deployments")
@Api(value = "/deployments", description = "Managing deployment resources")
public class DeploymentsRest {

    @Inject
    private DeploymentBoundary deploymentBoundary;
    @Inject
    private EnvironmentsScreenDomainService environmentsService;
    @Inject
    private ReleaseMgmtService releaseService;
    @Inject
    private ResourceDependencyResolverService dependencyResolverService;
    @Inject
    private PermissionService permissionService;
    @Inject
    private ResourceGroupPersistenceService resourceGroupService;
    @Inject
    private ResourceTypeProvider resourceTypeProvider;
    @Inject
    private GeneratorDomainServiceWithAppServerRelations generatorDomainServiceWithAppServerRelations;
    @Inject
    private KeyRepository keyRepository;
    @Inject
    private PermissionBoundary permissionBoundary;
    @Inject
    private ContextLocator contextLocator;

    @Inject
    Logger log;

    @GET
    @Path("/filter")
    @ApiOperation(value = "returns all Deployments matching the list of json filters - used by Angular", produces = "application/json, text/csv")
    public Response getDeployments(@ApiParam("Filters") @QueryParam("filters") String jsonListOfFilters,
                                   @QueryParam("colToSort") String colToSort,
                                   @QueryParam("sortDirection") String sortDirection,
                                   @QueryParam("maxResults") Integer maxResults,
                                   @QueryParam("offset") Integer offset) {
        DeploymentFilterDTO[] filterDTOs;
        try {
            filterDTOs = new Gson().fromJson(jsonListOfFilters, DeploymentFilterDTO[].class);
        } catch (JsonSyntaxException e) {
            String msg = String.format("json is not a valid representation for an object of type %s", DeploymentFilterDTO.class.getSimpleName());
            String detail = "example: [{\"name\":\"Application\",\"comp\":\"eq\",\"val\":\"Latest\"},{\"name\":\"Id\",\"comp\":\"eq\",\"val\":\"25\"}]";
            return Response.status(Status.BAD_REQUEST).entity(new ExceptionDto(msg, detail)).build();
        }
        if (filterDTOs == null) {
            return Response.status(Status.BAD_REQUEST).entity(new ExceptionDto("at least one filter must be set")).build();
        }
        CommonFilterService.SortingDirectionType sortingDirectionType = getSortingDirectionType(sortDirection);
        LinkedList<CustomFilter> filters = createCustomFilters(filterDTOs);
        Tuple<Set<DeploymentEntity>, Integer> filteredDeployments = deploymentBoundary.getFilteredDeployments(offset, maxResults, filters, colToSort, sortingDirectionType, null);
        List<DeploymentDTO> deploymentDTOs = createDeploymentDTOs(filteredDeployments);
        return Response.status(Status.OK).header("X-Total-Count", filteredDeployments.getB()).entity(new GenericEntity<List<DeploymentDTO>>(deploymentDTOs) {}).build();
    }

    private CommonFilterService.SortingDirectionType getSortingDirectionType(String sortDirection) {
        CommonFilterService.SortingDirectionType sortingDirectionType = null;
        if (sortDirection != null) {
            sortingDirectionType = CommonFilterService.SortingDirectionType.valueOf(sortDirection);
        }
        return sortingDirectionType;
    }

    private LinkedList<CustomFilter> createCustomFilters(DeploymentFilterDTO[] filterDTOs) {
        LinkedList<CustomFilter> filters = new LinkedList<>();
        for (DeploymentFilterDTO filterDTO : filterDTOs) {
            filters.add(createCustomFilterByDeploymentFilterDTO(filterDTO));
        }
        return filters;
    }

    private List<DeploymentDTO> createDeploymentDTOs(Tuple<Set<DeploymentEntity>, Integer> result) {
        List<DeploymentDTO> deploymentDTOs = new ArrayList<>();
        for (DeploymentEntity deployment : result.getA()) {
            deploymentDTOs.add(createDeploymentDTO(deployment));
        }
        return deploymentDTOs;
    }

    protected DeploymentDTO createDeploymentDTO(DeploymentEntity deployment) {
        if (deployment.isPreserved()) {
            return createPreservedDeploymentDTO(deployment);
        } else {
            DeploymentDTO deploymentDTO = new DeploymentDTO(deployment);
            deploymentDTO.setActions(createDeploymentActionsDTO(deployment));
            return deploymentDTO;
        }
    }

    private DeploymentDTO createPreservedDeploymentDTO(DeploymentEntity deployment) {
        DeploymentDTO deploymentDTO = new DeploymentDTO();
        DeploymentDTO.PreservedProperties properties = deploymentDTO.new PreservedProperties();
        if (deployment.getExResourcegroupId() != null) {
            properties.setAppServerName(deploymentBoundary.getDeletedResourceGroupName(deployment));
            properties.setAppServerId(deployment.getExResourcegroupId());
        }
        if (deployment.getExResourceId() != null) {
            properties.setResourceId(deployment.getExResourceId());
        }
        if (deployment.getExContextId() != null) {
            properties.setEnvironmentName(deploymentBoundary.getDeletedContextName(deployment));
            properties.setEnvironmentNameAlias(deploymentBoundary.getDeletedContextNameAlias(deployment));
        }
        if (deployment.getExReleaseId() != null) {
            properties.setReleaseName(deploymentBoundary.getDeletedReleaseName(deployment));
        }
        if (deployment.getExRuntimeResourceId() != null) {
            properties.setRuntimeName(deploymentBoundary.getDeletedRuntimeName(deployment));
        }
        deploymentDTO.setPreservedValues(deployment, properties);
        deploymentDTO.setActions(new DeploymentActionsDTO());
        return deploymentDTO;
    }

    private DeploymentActionsDTO createDeploymentActionsDTO(DeploymentEntity deployment) {
        DeploymentActionsDTO actionsDTO = new DeploymentActionsDTO();
        actionsDTO.setConfirmPossible(deploymentBoundary.isConfirmPossible(deployment).isPossible());
        actionsDTO.setRejectPossible(deploymentBoundary.isConfirmPossible(deployment).isPossible());
        actionsDTO.setCancelPossible(deploymentBoundary.isCancelPossible(deployment).isPossible());
        actionsDTO.setRedeployPossible(permissionService.hasPermissionForDeploymentCreation(deployment));
        actionsDTO.setHasLogFiles(deploymentBoundary.getLogFileNames(deployment.getId()).length > 0);
        actionsDTO.setEditPossible(deploymentBoundary.isChangeDeploymentDatePossible(deployment).isPossible()
                && (permissionService.hasPermissionToCreateDeployment() || permissionService.hasPermissionToEditDeployment()));
        return actionsDTO;
    }

    private CustomFilter createCustomFilterByDeploymentFilterDTO(DeploymentFilterDTO filterDTO) {
        DeploymentFilterTypes filterType = DeploymentFilterTypes.getByDisplayName(filterDTO.getName());
        if (filterDTO.getName().equals("Release")) {
            filterDTO.setVal(Long.toString(deploymentBoundary.getReleaseByName(filterDTO.getVal()).getInstallationInProductionAt().getTime()));
        }
        ComparatorFilterOption filterOption = ComparatorFilterOption.valueOf(filterDTO.getComp());
        CustomFilter filter = CustomFilter
                .builder(filterType)
                .comparatorSelection(filterOption)
                .build();
        filter.setValueFromRest(filterDTO.getVal());
        return filter;
    }

    /**
     * Query for deployments. All parameters are optional.
     * Date format: epoch timestamp (number of milliseconds since January 1st, 1970, UTC)
     *
     * @deprecated use {@link #getDeployments(String, String, String, Integer, Integer)} instead
     *
     * @return the deployments. The header X-Total-Count contains the total result count.
     **/
    @Deprecated
    @GET
    @ApiOperation(value = "returns all Deployments matching the optional filter Query Params")
    public Response getDeployments(
            @ApiParam("Tracking ID") @QueryParam("trackingId") Integer trackingId,
            @ApiParam("Deployments State") @QueryParam("deploymentState") DeploymentState deploymentState,
            @ApiParam("Application Server Name") @QueryParam("appServerName") List<String> appServerNames,
            @ApiParam("Application Name") @QueryParam("appName") List<String> appNames,
            @ApiParam("Runtime Name") @QueryParam("runtimeName") List<String> runtimeNames,
            @ApiParam("Max Results") @QueryParam("maxResults") Integer maxResults,
            @ApiParam("Offset") @DefaultValue("0") @QueryParam("offset") Integer offset,
            @ApiParam("From Date Filter") @QueryParam("fromDate") Long fromDate,
            @ApiParam("To Date Filter") @QueryParam("toDate") Long toDate,
            @ApiParam("Environment Filter") @QueryParam("environmentName") List<String> environmentNames,
            @ApiParam("Deployment Parameter Filter") @QueryParam("deploymentParameter") List<String> deploymentParameters,
            @ApiParam("Deployment Parameter Value Filter") @QueryParam("deploymentParameterValue") List<String> deploymentParameterValues,
            @ApiParam("only Latest Filter") @DefaultValue("false") @QueryParam("onlyLatest") boolean onlyLatest) {

        LinkedList<CustomFilter> filters = new LinkedList<>();

        if (trackingId != null) {
            CustomFilter trackingIdFilter = CustomFilter.builder(TRACKING_ID).build();
            trackingIdFilter.setValue(trackingId.toString());
            filters.add(trackingIdFilter);
        }
        if (deploymentState != null) {
            CustomFilter deploymentStateFilter = CustomFilter.builder(DEPLOYMENT_STATE).enumType(DeploymentState.class).build();
            deploymentStateFilter.setValue(deploymentState.name());
            filters.add(deploymentStateFilter);
        }
        if (fromDate != null) {
            CustomFilter deploymentDateFromFilter = CustomFilter.builder(DEPLOYMENT_DATE).comparatorSelection(ComparatorFilterOption.gte).build();
            deploymentDateFromFilter.setDateValue(new Date(fromDate));
            filters.add(deploymentDateFromFilter);
        }
        if (toDate != null) {
            CustomFilter deploymentDateFilter = CustomFilter.builder(DEPLOYMENT_DATE).comparatorSelection(ComparatorFilterOption.lte).build();
            deploymentDateFilter.setDateValue(new Date(toDate));
            filters.add(deploymentDateFilter);
        }
        if (onlyLatest) {
            filters.add(CustomFilter.builder(LASTDEPLOYJOBFORASENV).comparatorSelection(null).build());
        }
        if (appServerNames != null) {
            createFiltersAndAddToList(APPSERVER_NAME, appServerNames, filters);
        }
        if (appNames != null) {
            createFiltersAndAddToList(APPLICATION_NAME, appNames, filters);
        }
        if (runtimeNames != null) {
            createFiltersAndAddToList(TARGETPLATFORM, runtimeNames, filters);
        }
        if (environmentNames != null) {
            createFiltersAndAddToList(ENVIRONMENT_NAME, environmentNames, filters);
        }
        if (deploymentParameters != null) {
            createFiltersAndAddToList(DEPLOYMENT_PARAMETER, deploymentParameters, filters);
        }
        if (deploymentParameterValues != null) {
            createFiltersAndAddToList(DEPLOYMENT_PARAMETER, deploymentParameterValues, filters);
        }
        Tuple<Set<DeploymentEntity>, Integer> filteredDeployments = deploymentBoundary.getFilteredDeployments(offset, maxResults, filters, null, null, null);

        List<DeploymentDTO> deploymentDTOs = createDeploymentDTOs(filteredDeployments);

        return Response.status(Status.OK).header("X-Total-Count", filteredDeployments.getB()).entity(deploymentDTOs).build();
    }

    private void createFiltersAndAddToList(DeploymentFilterTypes deploymentFilterType, List<String> values, LinkedList<CustomFilter> filters) {
        for (String value : values) {
            CustomFilter deploymentParameterValueFilter = CustomFilter.builder(deploymentFilterType).build();
            deploymentParameterValueFilter.setValue(value);
            filters.add(deploymentParameterValueFilter);
        }
    }

    /**
     * Get information to a deployment Id.
     * In the future, this can be also used to update Deployments.
     *
     * @param id
     * @return the deployment or 404 if no deployment exists.
     * @throws Exception
     **/
    @GET
    @Path("/{id : \\d+}")
    // support digit only
    @ApiOperation(value = "get Deployment by id")
    public Response getDeployment(@ApiParam("Deployment ID") @PathParam("id") Integer id) {
        DeploymentEntity result;

        try {
            result = deploymentBoundary.getDeploymentById(id);
        } catch (RuntimeException e) {
            return catchNoResultException(e, "Deployment with id " + id + " not found.");
        }

        return Response.status(Status.OK).entity(new DeploymentDTO(result)).build();
    }

    @GET
    @Path("/{id : \\d+}/logs/{fileName}")
    @ApiOperation(value = "get the log file content as plain text for a given deployment and file name")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDeploymentLogFileContent(@ApiParam("Deployment ID") @PathParam("id") Integer id, @PathParam("fileName") String fileName) {
        List<String> availableLogFiles = Arrays.asList(deploymentBoundary.getLogFileNames(id));
        if (!availableLogFiles.contains(fileName)) {
            return Response.status(Status.BAD_REQUEST).entity("No logfile with name " + fileName + " for deployment with id " + id).build();
        }

        String logfileContent = "";
        try {
            logfileContent = deploymentBoundary.getDeploymentLog(fileName);
        } catch (IllegalAccessException e) {
            String msg = "error: unable to get contents of logfile " + fileName;
            log.info(msg);
            return Response.status(Status.BAD_REQUEST).entity(msg).build();
        }

        return Response.ok(logfileContent).build();
    }

    @GET
    @Path("/{deploymentId : \\d+}/logs")
    @ApiOperation(value = "get list of log files meta data for a given deployment id")
    public Response getDeploymentLogFileNames(@PathParam("deploymentId") Integer deploymentId) {
        String[] fileNames = deploymentBoundary.getLogFileNames(deploymentId);
        List<DeploymentLog> deploymentLogFiles = Arrays.stream(fileNames).map(fileName -> new DeploymentLog(deploymentId, fileName)).collect(Collectors.toList());
        return Response.ok(deploymentLogFiles).build();
    }

    @GET
    @Path("/deploymentParameterKeys/")
    @ApiOperation(value = "returns the keys of all available DeploymentParameter")
    public Response getAllDeploymentParameterKeys() {
        List<DeploymentParameterDTO> deploymentParameters = new ArrayList<>();
        for (Key key : keyRepository.findAll()) {
            deploymentParameters.add(new DeploymentParameterDTO(key.getName(), null));
        }
        return Response.status(Status.OK).entity(deploymentParameters).build();
    }

    @GET
    @Path("/deploymentFilterTypes")
    @ApiOperation(value = "Returns all available DeploymentFilterTypes - used by Angular")
    public Response getAllDeploymentFilterTypes() {
        List<DeploymentFilterTypeDTO> deploymentFilterTypes = new ArrayList<>();
        for (DeploymentFilterTypes filterType : deploymentBoundary.getDeploymentFilterTypes()) {
            deploymentFilterTypes.add(new DeploymentFilterTypeDTO(filterType.getFilterDisplayName(), filterType.getFilterType().name()));
        }
        return Response.status(Status.OK).entity(deploymentFilterTypes).build();
    }

    @GET
    @Path("/comparatorFilterOptions")
    @ApiOperation(value = "Returns all available ComparatorFilterOptions - used by Angular")
    public Response getAllComparatorFilterOptions() {
        List<ComparatorFilterOptionDTO> comparatorFilterOptions = new ArrayList<>();
        for (ComparatorFilterOption filterOption : deploymentBoundary.getComparatorFilterOptions()) {
            comparatorFilterOptions.add(new ComparatorFilterOptionDTO(filterOption.name(), filterOption.getDisplayName()));
        }
        return Response.status(Status.OK).entity(comparatorFilterOptions).build();
    }

    @GET
    @Path("/filterOptionValues")
    @ApiOperation(value = "Returns all available option values for a specific Filter - used by Angular")
    public Response getFilterOptionValues(@ApiParam("Filter name") @QueryParam("filterName") String filterName) {
        return Response.status(Status.OK).entity(deploymentBoundary.getFilterOptionValues(filterName)).build();
    }

    /**
     * Creates a new deployment and returns the newly created deployment. Only creates one deployment per request.
     *
     * @param request containing a DeploymentRequestDTO
     * @return the new DeploymentDTO
     **/
    @POST
    @ApiOperation(value = "adds a DeploymentRequest")
    public Response addDeployment(@ApiParam("Deployment Request") DeploymentRequestDTO request) {
        Integer trackingId;
        ResourceEntity appServer;
        Set<ResourceEntity> apps;
        ContextEntity environment = null;
        List<ApplicationWithVersion> applicationsWithVersion = new ArrayList<>();
        LinkedList<CustomFilter> filters = new LinkedList<>();
        ReleaseEntity release;
        ResourceGroupEntity group;
        LinkedList<Integer> contexts = new LinkedList<>();
        ArrayList<DeploymentParameter> deployParams;

        // use default releaseId if no release in request
        if (request.getReleaseName() == null) {
            List<ReleaseEntity> releases = releaseService.loadAllReleases(false);
            release = dependencyResolverService.findMostRelevantRelease(new TreeSet<>(releases), new Date());
        } else {
            // try to fetch release by name
            release = releaseService.findByName(request.getReleaseName());
            if (release == null) {
                return Response.status(Status.BAD_REQUEST).entity(new ExceptionDto("Release " + request
                        .getReleaseName() + " not found."))
                        .build();
            }
        }

        // get the id of the ApplicationServer
        group = resourceGroupService.loadUniqueGroupByNameAndType(request.getAppServerName(), resourceTypeProvider
                .getOrCreateDefaultResourceType(DefaultResourceTypeDefinition.APPLICATIONSERVER).getId());

        if (group == null) {
            return Response.status(Status.BAD_REQUEST).entity(new ExceptionDto("ApplicationServer with name " + request.getAppServerName() + " not found."))
                    .build();
        }
        appServer = dependencyResolverService.getResourceEntityForRelease(group, release);
        if (appServer == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ExceptionDto("ApplicationServer " + request.getAppServerName() + " does not exist in release " + release.getName()))
                    .build();
        }

        // get the id of the Environment
        if (request.getEnvironmentName() != null) {
            try {
                environment = environmentsService.getContextByName(request.getEnvironmentName());
            } catch (RuntimeException e) {
                return catchNoResultException(e, "Environment " + request.getEnvironmentName() + " not found.");
            }
        }

        // get the apps of the appServer
        apps = dependencyResolverService.getConsumedRelatedResourcesByResourceType(appServer, DefaultResourceTypeDefinition.APPLICATION, release);

        // convert the appsWithVersion
        try {
            if (apps == null) {
                apps = new HashSet<>();
            }
            if (request.getAppsWithVersion() != null) {
                applicationsWithVersion = convertToApplicationWithVersion(request.getAppsWithVersion(), apps);
            } else {
                applicationsWithVersion = deploymentBoundary.getVersions(appServer, new ArrayList<Integer>(environment.getId()), release);
            }

        } catch (ValidationException e) {
            return Response.status(Status.BAD_REQUEST).entity(
                    new ExceptionDto("ValidationException", e.getMessage())).build();
        }

        try {
            deployParams = convertToDeploymentParameter(request.getDeploymentParameters());
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new ExceptionDto("Could not create deployment", e.getMessage())).build();
        }

        // check whether the AS has at least one node with hostname to deploy to
        if (environment != null) {
            boolean hasNode = generatorDomainServiceWithAppServerRelations.hasActiveNodeToDeployOnAtDate(appServer, environment, request.getStateToDeploy());
            if (!hasNode) {
                return Response.status(424)
                        .entity(new ExceptionDto("No active Node found on Environement " + request.getEnvironmentName()))
                        .build();
            }
            contexts.add(environment.getId());
        } else if (request.getContextIds() != null && !request.getContextIds().isEmpty()) {
            contexts.addAll(request.getContextIds());
        }

        if (contexts.isEmpty()) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(new ExceptionDto("No ContextIds"))
                    .build();
        }

        trackingId = deploymentBoundary.createDeploymentReturnTrackingId(group.getId(), release.getId(), request.getDeploymentDate(),
                request.getStateToDeploy(), contexts,
                applicationsWithVersion, deployParams, request.getSendEmail(), request.getRequestOnly(), request.getSimulate(), request.getExecuteShakedownTest(),
                request.getNeighbourhoodTest());

        // get the deployment from the tracking id
        CustomFilter trackingIdFilter = CustomFilter.builder(TRACKING_ID).build();
        trackingIdFilter.setValue(trackingId.toString());
        filters.add(trackingIdFilter);
        Tuple<Set<DeploymentEntity>, Integer> result = deploymentBoundary.getFilteredDeployments( 0, 1, filters, null, null, null);

        DeploymentDTO deploymentDto = new DeploymentDTO(result.getA().iterator().next());

        return Response.status(Status.CREATED).header("Location", "/deployments/" + deploymentDto.getId()).entity(deploymentDto).build();
    }

    private ArrayList<DeploymentParameter> convertToDeploymentParameter(List<DeploymentParameterDTO> deploymentParameters) {
        ArrayList<DeploymentParameter> parameters = new ArrayList<>();

        if (deploymentParameters != null) {
            for (DeploymentParameterDTO deploymentParameterDto : deploymentParameters) {
                String key = deploymentParameterDto.getKey().trim();
            	parameters.add(new DeploymentParameter(key, deploymentParameterDto.getValue()));
            }
        }

        return parameters;
    }

    @PUT
    @Path("/{id : \\d+}/state")
    @Consumes("text/plain")
    @ApiOperation(value = "Update the state of a deployment")
    public Response patchDeployment(@ApiParam("deployment Id") @PathParam("id") Integer deploymentId, @ApiParam("New status") String stateStr) {
        DeploymentState newState = DeploymentState.getByString(stateStr);

        if (newState == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ExceptionDto("Invalid status",
                    "Allowed values are " + Arrays.asList(DeploymentState.values()))).build();
        }

        try {
            deploymentBoundary.updateDeploymentState(deploymentId, newState);
        } catch (RuntimeException e) {
            return catchDeploymentStateException(e);
        }

        return Response.status(Response.Status.OK).build();
    }

    @PUT
    @Path("/{id : \\d+}/confirm")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Confirm a deployment")
    public Response confirmDeployment(@ApiParam("deployment Id") @PathParam("id") Integer deploymentId,
                                      @ApiParam("New status") DeploymentDTO deploymentDTO) {
        try{
            deploymentBoundary.confirmDeployment(deploymentId,
                    deploymentDTO.isSendEmailWhenDeployed(),
                    deploymentDTO.isShakedownTestsWhenDeployed(),
                    deploymentDTO.isNeighbourhoodTest(),
                    deploymentDTO.isSimulateBeforeDeployment(),
                    deploymentDTO.getDeploymentDate());
            return Response.status(Response.Status.OK).build();
        } catch (RuntimeException e) {
            return catchDeploymentStateException(e);
        }

    }

    @PUT
    @Path("/{id: \\d+}/jobs/{nodeJobId: \\d+}")
    @Consumes("text/plain")
    @ApiOperation(value = "Set the nodeJobResult for the given Deployment (id) and the nodeJob.")
    public Response updateNodeJobResult(
            @ApiParam("deployment Id") @PathParam("id") Integer deploymentId,
            @ApiParam("nodeJob Id") @PathParam("nodeJobId") Integer nodeJobId,
            @ApiParam("Status result") String statusStr) {

        NodeJobStatus status = NodeJobStatus.getNodeJobStatusByString(statusStr);
        if (status == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new ExceptionDto("Invalid status",
                    "Allowed values are " + Arrays.asList(NodeJobStatus.values()))).build();
        }

        try {
            deploymentBoundary.updateNodeJobStatus(deploymentId, nodeJobId, status);
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new ExceptionDto(e)).build();
        } catch (RuntimeException e) {
            return catchDeploymentStateException(e);
        }
        return Response.status(Response.Status.OK).build();

    }

    @PUT
    @Path("/{id : \\d+}/date")
    @ApiOperation(value = "Update the DeploymentDate of a Deployment - used by Angular")
    public Response changeDeploymentDate(@ApiParam("deployment Id") @PathParam("id") Integer deploymentId, @ApiParam("New date") long date) {
        Date newDate = new Date(date);
        try {
            deploymentBoundary.changeDeploymentDate(deploymentId, newDate);
        } catch (RuntimeException e) {
            return catchDeploymentStateException(e);
        }
        return Response.status(Response.Status.OK).build();
    }


    @PUT
    @Path("/{id : \\d+}/updateState")
    @Consumes("text/plain")
    @ApiOperation(value = "Update state of a deployment - used by Angular")
    public Response updateState(@ApiParam("deployment Id") @PathParam("id") Integer deploymentId,
                                @ApiParam("state as string") String statusStr) {
        DeploymentState state;
        try {
            state = DeploymentState.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            String possibleValues = Arrays.toString(DeploymentState.values());
            return Response.status(Response.Status.BAD_REQUEST).entity(new ExceptionDto(String.format("invalid state. must be one of %s", possibleValues) )).build();
        }

        try {
            switch (state) {
                case canceled:
                    deploymentBoundary.cancelDeployment(deploymentId);
                    break;
                case rejected:
                    deploymentBoundary.rejectDeployment(deploymentId);
                    break;
                default:
                    return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(new ExceptionDto(String.format("Change state to '%s' not implemented yet", state.toString()) )).build();
            }
            return Response.status(Status.OK).build();
        } catch (RuntimeException e) {
            return catchDeploymentStateException(e);
        }
    }

    @GET
    @Path("/{id : \\d+}/withActions")
    // support digit only
    @ApiOperation(value = "Get a Deployment including actions by id - used by Angular")
    public Response getDeploymentWithActions(@ApiParam("Deployment ID") @PathParam("id") Integer id) {
        DeploymentEntity result;

        try {
            result = deploymentBoundary.getDeploymentById(id);
        } catch (RuntimeException e) {
            return catchNoResultException(e, "Deployment with id " + id + " not found.");
        }

        return Response.status(Status.OK).entity(createDeploymentDTO(result)).build();
    }

    @GET
    @Path("/canDeploy/{resourceGroupId}")
    @ApiOperation(value = "Checks if caller is allowed to deploy a given ResourceGroup on the specified Environment(s) - used by Angular")
    public Response canDeploy(@PathParam("resourceGroupId") Integer resourceGroupId,
                              @QueryParam("contextId") Set<Integer> contextIds) {
        ResourceGroupEntity resourceGroup = resourceGroupService.getById(resourceGroupId);
        if (resourceGroup == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        boolean hasPermission = false;
        for (Integer contextId : contextIds) {
            ContextEntity context = contextLocator.getContextById(contextId);
            hasPermission = permissionBoundary.hasPermission(Permission.DEPLOYMENT, Action.CREATE, context, resourceGroup)
                    && permissionBoundary.hasPermission(Permission.DEPLOYMENT, Action.UPDATE, context, resourceGroup);
            if (!hasPermission) {
                return Response.ok(hasPermission).build();
            }
        }
        return Response.ok(hasPermission).build();
    }

    @GET
    @Path("/canRequestDeployment/{resourceGroupId}")
    @ApiOperation(value = "Checks if caller is allowed to request a deployment a given ResourceGroup on the specified Environment(s) - used by Angular")
    public Response canRequestDeployment(@PathParam("resourceGroupId") Integer resourceGroupId,
                              @QueryParam("contextId") Set<Integer> contextIds) {
        ResourceGroupEntity resourceGroup = resourceGroupService.getById(resourceGroupId);
        if (resourceGroup == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        boolean hasPermission = false;
        for (Integer contextId : contextIds) {
            ContextEntity context = contextLocator.getContextById(contextId);
            hasPermission = permissionBoundary.hasPermission(Permission.DEPLOYMENT, Action.CREATE, context, resourceGroup);
            if (!hasPermission) {
                return Response.ok(hasPermission).build();
            }
        }
        return Response.ok(hasPermission).build();
    }

    @GET
    @Path("/canRequestDeployment/")
    @ApiOperation(value = "Checks if the caller is allowed to request a deployment at all - used by Angular")
    public Response canRequestDeployment() {

        return Response.ok(permissionBoundary.hasPermission(Permission.DEPLOYMENT, Action.CREATE)).build();
    }

    @GET
    @Path("/isAngularDeploymentsGuiActive/")
    @ApiOperation(value = "Check if angular deployments gui is active - used by Angular")
    public Response isAngularDeploymentsGuiActive() {
        boolean isActive = ! ConfigurationService.getPropertyAsBoolean(FEATURE_DISABLE_ANGULAR_DEPLOYMENT_GUI);
        return Response.ok(isActive).build();
    }

    /**
     * Convert AppWithVersion to ApplicationWithVersion. Checks if
     * AppWithVersion contains all the Applications and adds the Application id.
     **/
    private List<ApplicationWithVersion> convertToApplicationWithVersion(List<AppWithVersionDTO> requestedApps, Set<ResourceEntity> apps)
            throws ValidationException {
        LinkedList<ApplicationWithVersion> result = new LinkedList<>();
        List<AppWithVersionDTO> requestedAppsCopy = new LinkedList<>(requestedApps);

        // copy the collections as we don't want to modify the originals
        Set<ResourceEntity> appsCopy = new HashSet<>(apps);
        StringBuilder sb = new StringBuilder();

        for (AppWithVersionDTO requestedApp : requestedApps) {
            // find the corresponding app
            for (ResourceEntity app : apps) {
                // if the name matches, convert the app
                if (requestedApp.getApplicationName().equals(app.getName())) {
                    //for backwards compatibility: use MavenVersion as Version
                    String appVersion = (requestedApp.getVersion() != null && !requestedApp.getVersion().isEmpty())
                            ? requestedApp.getVersion() : requestedApp.getVersion();
                    //convert
                    result.add(
                            new ApplicationWithVersion(
                                    requestedApp.getApplicationName(), app.getId(), appVersion));
                    //scratch off
                    requestedAppsCopy.remove(requestedApp);
                    appsCopy.remove(app);
                    break;
                }
            }
        }

        // check if something is left in the collections
        if (requestedAppsCopy.size() != 0) {
            sb.append("Applications not found:");

            for (AppWithVersionDTO app : requestedAppsCopy) {
                sb.append(' ').append(app.getApplicationName());
            }
        }
        if (appsCopy.size() != 0) {
            if (sb.length() != 0) {
                sb.append(" / ");
            }
            sb.append("Applications missing in request:");

            for (ResourceEntity app : appsCopy) {
                sb.append(' ').append(app.getName());
            }
        }

        if (sb.length() != 0) {
            throw new ValidationException(sb.toString());
        }

        return result;
    }

    private Response catchNoResultException(RuntimeException e, String message) {
        if (e instanceof NoResultException || e.getCause() instanceof NoResultException) {
            return Response.status(Status.NOT_FOUND).entity(new ExceptionDto(message)).build();
        } else {
            throw e;
        }
    }

    private Response catchDeploymentStateException(RuntimeException e) {
        if (e instanceof DeploymentStateException || e.getCause() instanceof DeploymentStateException) {
            return Response.status(Response.Status.CONFLICT).entity(new ExceptionDto(e)).build();
        } else {
            throw e;
        }
    }


}
