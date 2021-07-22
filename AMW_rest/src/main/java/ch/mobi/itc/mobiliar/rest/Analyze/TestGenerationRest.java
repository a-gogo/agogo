package ch.mobi.itc.mobiliar.rest.Analyze;

import ch.mobi.itc.mobiliar.rest.dtos.EnvironmentGenerationResultDTO;
import ch.mobi.itc.mobiliar.rest.exceptions.ExceptionDto;
import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.EnvironmentGenerationResult;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratorDomainServiceWithAppServerRelations;
import ch.puzzle.itc.mobiliar.business.releasing.boundary.ReleaseLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("/analyze")
@Api(value = "/analyze", description = "Analyze test generation")
public class TestGenerationRest {

    @Inject
    private ResourceLocator resourceLocator;

    @Inject
    private ReleaseLocator releaseLocator;

    @Inject
    private ContextLocator contextLocator;

    @Inject
    private PermissionService permissionService;

    @Inject
    private GeneratorDomainServiceWithAppServerRelations generatorDomainServiceWithAppServerRelations;

    @GET
    @Path("/{resourceGroupName}/{releaseName}/{env}")
    public Response testGeneration(@PathParam("resourceGroupName") String resourceGroupName,
                                   @PathParam("releaseName") String releaseName,
                                   @PathParam("env") String env) throws IOException, ValidationException {
        ResourceEntity resource = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName, releaseName);
        if (resource == null) {
            return Response.status(NOT_FOUND).entity(new ExceptionDto("Resource not found")).build();
        }
        ResourceEntity appServer = resource.getResourceType().isApplicationResourceType() ? resourceLocator.getApplicationServerForApplication(resource) : resource;
        Integer releaseId = releaseLocator.getReleaseByName(releaseName).getId();
        ContextEntity context = contextLocator.getContextByName(env);
        permissionService.checkPermissionAndFireException(Permission.RESOURCE_TEST_GENERATION, context, Action.READ, appServer.getResourceGroup(), null, "test generate");
        EnvironmentGenerationResult environmentGenerationResult;
        try {
            environmentGenerationResult = generatorDomainServiceWithAppServerRelations.generateApplicationServerForTest(context.getId(), appServer.getId(), releaseId, null);
        } catch (AMWException e) {
            return Response.status(INTERNAL_SERVER_ERROR).entity(new ExceptionDto(e.getMessage())).build();
        }
        if (environmentGenerationResult.hasErrors()) {
            return Response.status(INTERNAL_SERVER_ERROR).entity(new ExceptionDto(environmentGenerationResult.getErrorMessage())).build();
        }
        return Response.ok(new EnvironmentGenerationResultDTO(environmentGenerationResult)).build();
    }
}
