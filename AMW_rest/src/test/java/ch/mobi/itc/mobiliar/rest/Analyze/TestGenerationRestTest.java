package ch.mobi.itc.mobiliar.rest.Analyze;

import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.EnvironmentGenerationResult;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratorDomainServiceWithAppServerRelations;
import ch.puzzle.itc.mobiliar.business.releasing.boundary.ReleaseLocator;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TestGenerationRestTest {

    @InjectMocks
    TestGenerationRest rest;

    @Mock
    ResourceLocator resourceLocator;

    @Mock
    ReleaseLocator releaseLocator;

    @Mock
    ContextLocator contextLocator;

    @Mock
    GeneratorDomainServiceWithAppServerRelations generatorDomainServiceWithAppServerRelations;

    @Mock
    PermissionService permissionService;

    @Mock
    EnvironmentGenerationResult environmentGenerationResult;

    @Mock
    ResourceEntity resourceEntity;

    @Mock
    ResourceEntity appServer;

    @Mock
    ResourceTypeEntity resourceTypeEntity;

    private String resourceGroupName;
    private String releaseName;
    private String env;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        resourceGroupName = "TestGroup";
        releaseName = "TestRelease";
        env = "T";
    }

    @Test
    public void shouldReturnNotFoundStatusIfResourceIsNotFoundOnTestGeneration() throws IOException, ValidationException {
        // given => setup

        // when
        Response response = rest.testGeneration(resourceGroupName, releaseName, env);

        // then
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void shouldCheckPermissionForAppServerOnTestGeneration() throws IOException, ValidationException, AMWException {
        // given => setup
        ReleaseEntity release = new ReleaseEntity();
        release.setName(releaseName);
        release.setId(11);
        ContextEntity context = new ContextEntity();
        context.setName(env);
        context.setId(12);

        ResourceGroupEntity appServerResourceGroup = mock(ResourceGroupEntity.class);
        when(appServer.getResourceGroup()).thenReturn(appServerResourceGroup);
        ResourceTypeEntity app = mock(ResourceTypeEntity.class);
        when(app.isApplicationResourceType()).thenReturn(true);

        when(resourceLocator.getResourceByGroupNameAndRelease(anyString(), anyString())).thenReturn(resourceEntity);
        when(resourceEntity.getResourceType()).thenReturn(resourceTypeEntity);
        when(resourceEntity.getResourceType()).thenReturn(app);

        when(resourceLocator.getApplicationServerForApplication(any(ResourceEntity.class))).thenReturn(appServer);
        when(releaseLocator.getReleaseByName(releaseName)).thenReturn(release);
        when(contextLocator.getContextByName(env)).thenReturn(context);

        when(generatorDomainServiceWithAppServerRelations.generateApplicationServerForTest(anyInt(), anyInt(), anyInt(), ArgumentMatchers.<Date>any())).thenThrow(AMWException.class);

        // when
        rest.testGeneration(resourceGroupName, releaseName, env);

        // then
        verify(permissionService).checkPermissionAndFireException(Permission.RESOURCE_TEST_GENERATION, context, Action.READ, appServerResourceGroup, null, "test generate");
    }

    @Test
    public void shouldReturnInternalServerErrorStatusAmwExceptionIsThrown() throws IOException, ValidationException, AMWException {
        // given => setup
        ReleaseEntity release = new ReleaseEntity();
        release.setName(releaseName);
        release.setId(11);
        ContextEntity context = new ContextEntity();
        context.setName(env);
        context.setId(12);

        when(resourceEntity.getId()).thenReturn(13);
        when(resourceEntity.getResourceType()).thenReturn(resourceTypeEntity);
        when(resourceLocator.getResourceByGroupNameAndRelease(anyString(), anyString())).thenReturn(resourceEntity);
        when(releaseLocator.getReleaseByName(releaseName)).thenReturn(release);
        when(contextLocator.getContextByName(env)).thenReturn(context);
        when(generatorDomainServiceWithAppServerRelations.generateApplicationServerForTest(anyInt(), anyInt(), anyInt(), ArgumentMatchers.<Date>any())).thenThrow(AMWException.class);

        // when
        Response response = rest.testGeneration(resourceGroupName, releaseName, env);

        // then
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void shouldObtainAppServerIdFromResourceLocatorIfResourceIsApplication() throws IOException, ValidationException, AMWException {
        // given => setup
        ReleaseEntity release = new ReleaseEntity();
        release.setName(releaseName);
        release.setId(11);
        ContextEntity context = new ContextEntity();
        context.setName(env);
        context.setId(12);

        when(resourceEntity.getId()).thenReturn(13);
        when(resourceEntity.getResourceType()).thenReturn(resourceTypeEntity);
        when(resourceTypeEntity.isApplicationResourceType()).thenReturn(true);
        when(resourceLocator.getResourceByGroupNameAndRelease(anyString(), anyString())).thenReturn(resourceEntity);
        when(resourceLocator.getApplicationServerForApplication(resourceEntity)).thenReturn(resourceEntity);
        when(releaseLocator.getReleaseByName(releaseName)).thenReturn(release);
        when(contextLocator.getContextByName(env)).thenReturn(context);
        when(generatorDomainServiceWithAppServerRelations.generateApplicationServerForTest(anyInt(), anyInt(), anyInt(), ArgumentMatchers.<Date>any())).thenThrow(AMWException.class);

        // when
        rest.testGeneration(resourceGroupName, releaseName, env);

        // then
        verify(resourceLocator).getApplicationServerForApplication(any(ResourceEntity.class));
    }

    @Test
    public void shouldUseIdOfResourceAsAppServerIdIfResourceIsNotApplication() throws IOException, ValidationException, AMWException {
        // given => setup
        ReleaseEntity release = new ReleaseEntity();
        release.setName(releaseName);
        release.setId(11);
        ContextEntity context = new ContextEntity();
        context.setName(env);
        context.setId(12);

        when(resourceEntity.getId()).thenReturn(13);
        when(resourceEntity.getResourceType()).thenReturn(resourceTypeEntity);
        when(resourceLocator.getResourceByGroupNameAndRelease(anyString(), anyString())).thenReturn(resourceEntity);
        when(resourceLocator.getApplicationServerForApplication(resourceEntity)).thenReturn(resourceEntity);
        when(releaseLocator.getReleaseByName(releaseName)).thenReturn(release);
        when(contextLocator.getContextByName(env)).thenReturn(context);
        when(generatorDomainServiceWithAppServerRelations.generateApplicationServerForTest(anyInt(), anyInt(), anyInt(), ArgumentMatchers.<Date>any())).thenThrow(AMWException.class);

        // when
        rest.testGeneration(resourceGroupName, releaseName, env);

        // then
        verify(resourceLocator, never()).getApplicationServerForApplication(any(ResourceEntity.class));
    }


    @Test
    public void shouldReturnInternalServerErrorStatusIfGenerationResultHasErrors() throws IOException, ValidationException, AMWException {
        // given => setup
        ReleaseEntity release = new ReleaseEntity();
        release.setName(releaseName);
        release.setId(11);
        ContextEntity context = new ContextEntity();
        context.setName(env);
        context.setId(12);

        when(resourceEntity.getId()).thenReturn(13);
        when(resourceEntity.getResourceType()).thenReturn(resourceTypeEntity);
        when(resourceLocator.getResourceByGroupNameAndRelease(anyString(), anyString())).thenReturn(resourceEntity);
        when(releaseLocator.getReleaseByName(releaseName)).thenReturn(release);
        when(contextLocator.getContextByName(env)).thenReturn(context);
        when(environmentGenerationResult.hasErrors()).thenReturn(true);
        when(environmentGenerationResult.getErrorMessage()).thenReturn("error");
        when(generatorDomainServiceWithAppServerRelations.generateApplicationServerForTest(anyInt(), anyInt(), anyInt(), ArgumentMatchers.<Date>any())).thenReturn(environmentGenerationResult);

        // when
        Response response = rest.testGeneration(resourceGroupName, releaseName, env);

        // then
        assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }


}