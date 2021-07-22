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

import ch.mobi.itc.mobiliar.rest.dtos.AppWithVersionDTO;
import ch.mobi.itc.mobiliar.rest.dtos.DeploymentDTO;
import ch.mobi.itc.mobiliar.rest.dtos.DeploymentRequestDTO;
import ch.mobi.itc.mobiliar.rest.exceptions.ExceptionDto;
import ch.puzzle.itc.mobiliar.business.deploy.boundary.DeploymentBoundary;
import ch.puzzle.itc.mobiliar.business.deploy.entity.CustomFilter;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity.ApplicationWithVersion;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentState;
import ch.puzzle.itc.mobiliar.business.deploy.entity.NodeJobEntity;
import ch.puzzle.itc.mobiliar.business.deploymentparameter.entity.DeploymentParameter;
import ch.puzzle.itc.mobiliar.business.domain.commons.CommonFilterService;
import ch.puzzle.itc.mobiliar.business.environment.control.ContextDomainService;
import ch.puzzle.itc.mobiliar.business.environment.control.EnvironmentsScreenDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratorDomainServiceWithAppServerRelations;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.releasing.control.ReleaseMgmtService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceGroupPersistenceService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceTypeProvider;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.*;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.common.util.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.persistence.NoResultException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.logging.Logger;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeploymentTest {

    @InjectMocks
    private DeploymentsRest deploymentRestService;
    @Mock
    private DeploymentBoundary deploymentBoundary;
    @Mock
    private GeneratorDomainServiceWithAppServerRelations generatorDomainServiceWithAppServerRelations;
    @Mock
    private EnvironmentsScreenDomainService environmentsService;
    @Mock
    private ApplicationServer applicationServer;
    @Mock
    private ReleaseMgmtService releaseService;
    @Mock
    private ResourceDependencyResolverService dependencyResolverService;
    @Mock
    private ResourceTypeProvider resourceTypeProvider;
    @Mock
    private ResourceGroupPersistenceService resourceGroupService;
    @Mock
    private ContextDomainService contextDomainService;
    @Mock
    private Logger log;


    private HashSet<DeploymentEntity> entities;
    private DeploymentEntity deploymentEntity;
    private DeploymentDTO deploymentDto;
    private DeploymentRequestDTO deploymentRequestDto;
    private ReleaseEntity release;
    
    @Before
    public void configure() {
        MockitoAnnotations.openMocks(this);

        // Test data
        entities = new HashSet<>();
        deploymentEntity = new DeploymentEntity();
        LinkedList<DeploymentEntity.ApplicationWithVersion> appsWithVersion = new LinkedList<>();
        appsWithVersion.add(new ApplicationWithVersion("test", 123, "1.2.3"));
        appsWithVersion.add(new ApplicationWithVersion("west", 124, "1.2.4"));
        deploymentEntity.setApplicationsWithVersion(appsWithVersion);
        deploymentEntity.setDeploymentParameters(new LinkedList<DeploymentParameter>());
        deploymentEntity.setId(123);
        deploymentEntity.setDeploymentDate(new Date());
        deploymentEntity.setStateToDeploy(new Date());
        deploymentEntity.setSimulating(false);
        deploymentEntity.setShakedownTests(null);
        deploymentEntity.setTrackingId(321);
        ResourceEntity targetPlatform = ResourceFactory.createNewResource("test");
        deploymentEntity.setRuntime(targetPlatform);
        deploymentEntity.setDeploymentCancelUser("u123456");
        deploymentEntity.setDeploymentRequestUser("u123457");
        deploymentEntity.setDeploymentConfirmationUser("u123458");
        deploymentEntity.setNodeJobs(new HashSet<NodeJobEntity>());
        release = new ReleaseEntity();
        release.setId(2);
        release.setName("RL-13.04");
        deploymentEntity.setRelease(release);
        entities.add(deploymentEntity);

        ResourceEntity resource =  ResourceFactory.createNewResource("test");
        resource.setId(1);
        deploymentEntity.setResource(resource);
        deploymentEntity.setResourceGroup(resource.getResourceGroup());

        ContextEntity context = new ContextEntity();
        context.setName("test");
        context.setId(2323);
        deploymentEntity.setContext(context);

        deploymentDto = new DeploymentDTO(deploymentEntity);

        deploymentRequestDto = new DeploymentRequestDTO();
        deploymentRequestDto.setAppServerName(deploymentEntity.getResourceGroup().getName());
        LinkedList<AppWithVersionDTO> apps = new LinkedList<>();
        apps.add(new AppWithVersionDTO(appsWithVersion.getFirst().getApplicationName(), appsWithVersion.getFirst().getApplicationId(), appsWithVersion.getFirst().getVersion()));
        deploymentRequestDto.setAppsWithVersion(apps);
        deploymentRequestDto.setDeploymentDate(deploymentEntity.getDeploymentDate());
        deploymentRequestDto.setEnvironmentName(deploymentEntity.getContext().getName());
        deploymentRequestDto.setExecuteShakedownTest(deploymentEntity.isCreateTestAfterDeployment());
        deploymentRequestDto.setNeighbourhoodTest(deploymentEntity.isCreateTestForNeighborhoodAfterDeployment());
        deploymentRequestDto.setRequestOnly(false);
        deploymentRequestDto.setSendEmail(deploymentEntity.isSendEmail());
        deploymentRequestDto.setSimulate(deploymentEntity.isSimulating());

        //mock app
        LinkedList<Application> applications = new LinkedList<>();
        Application application = mock(Application.class);
        when(application.getName()).thenReturn(appsWithVersion.getFirst().getApplicationName());
        applications.add(application);
        

        //mock as
        when(applicationServer.getName()).thenReturn(deploymentEntity.getResourceGroup().getName());
        when(applicationServer.getAMWApplications()).thenReturn(applications);
//        when(appServerService.getApplicationServerById(anyInt())).thenReturn(applicationServer);
        
        // resource type
        ResourceTypeEntity defaultAS = mock(ResourceTypeEntity.class);
        when(defaultAS.getId()).thenReturn(1);
        when(resourceTypeProvider.getOrCreateDefaultResourceType(
                DefaultResourceTypeDefinition.APPLICATIONSERVER)).thenReturn(defaultAS);
        
        // resource group
        when(resourceGroupService.loadUniqueGroupByNameAndType(anyString(), anyInt())).thenReturn(resource.getResourceGroup());
        
        when(dependencyResolverService.getResourceEntityForRelease(any(ResourceGroupEntity.class),
                any(ReleaseEntity.class))).thenReturn(resource);
        when(dependencyResolverService.findMostRelevantRelease((TreeSet<ReleaseEntity>) any(), (Date) any())).thenReturn(release);
         when(dependencyResolverService
                .getConsumedRelatedResourcesByResourceType(Mockito.any(ResourceEntity.class),
                        Mockito.any(DefaultResourceTypeDefinition.class),
                                Mockito.any(ReleaseEntity.class))).thenAnswer(new Answer<Set<ResourceEntity>>() {
            @Override public Set<ResourceEntity> answer(InvocationOnMock invocation) {
               HashSet<ResourceEntity> set = new HashSet<>();
               for(Application a : applicationServer.getAMWApplications()){
                  final String name = a.getName();
                  ResourceEntity resMock = Mockito.mock(ResourceEntity.class);
                  when(resMock.getName()).thenReturn(name);
                  set.add(resMock);
               }
               return set;
            }
        });
        
        ContextEntity global = mock(ContextEntity.class);
        when(contextDomainService.getGlobalResourceContextEntity()).thenReturn(global);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getDeploymentsBasic() {
        when(deploymentBoundary.getFilteredDeployments(null, null, new LinkedList<CustomFilter>(), null, null, null)).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(new HashSet<DeploymentEntity>(), 0));

        Response response = deploymentRestService.getDeployments(null, null, null, null, null, null, null, null, null, null, null, null, false);
        ArrayList<DeploymentDTO> deploymentDtos = (ArrayList<DeploymentDTO>) response.getEntity();
        LinkedList<Integer> metaList = new LinkedList<>();
        metaList.add(0);

        assertEquals(200, response.getStatus());
        assertEquals(0, deploymentDtos.size());
        assertEquals(metaList, response.getMetadata().get("X-Total-Count"));
    }

    @Test
    public void verifyThatDeploymentServiceIsCalledWithNonEmptyFilterIfDeploymentParametersAreSet() {
        when(deploymentBoundary.getFilteredDeployments(isNull(), isNull(), anyList(), isNull(), isNull(), isNull())).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(new HashSet<DeploymentEntity>(), 0));
        deploymentRestService.getDeployments(null, null, null, null, null, null, null, null, null, null, Collections.singletonList("TEST"), null, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomFilter>> captor = ArgumentCaptor.forClass(List.class);
        verify(deploymentBoundary, times(1)).getFilteredDeployments(isNull(), isNull(), captor.capture(), isNull(), isNull(), isNull());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    public void verifyThatDeploymentServiceIsCalledWithNonEmptyFilterIfDeploymentParameterValuesAreSet() {
        when(deploymentBoundary.getFilteredDeployments(isNull(), isNull(), anyList(), isNull(), isNull(), isNull())).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(new HashSet<DeploymentEntity>(), 0));
        deploymentRestService.getDeployments(null, null, null, null, null, null, null, null, null, null, null, Collections.singletonList("TESTVALUE"), false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomFilter>> captor = ArgumentCaptor.forClass(List.class);
        verify(deploymentBoundary, times(1)).getFilteredDeployments(isNull(), isNull(), captor.capture(), isNull(), isNull(), isNull());
        assertEquals(1, captor.getValue().size());
    }

    @Test
    public void verifyThatDeploymentServiceIsCalledWithNonEmptyFilterIfDeploymentParametersAndDeploymentParameterValuesAreSet() {
        when(deploymentBoundary.getFilteredDeployments(isNull(), isNull(), anyList(), isNull(), isNull(), isNull())).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(new HashSet<DeploymentEntity>(), 0));
        deploymentRestService.getDeployments(null, null, null, null, null, null, null, null, null, null, Collections.singletonList("TEST"), Collections.singletonList("TESTVALUE"), false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CustomFilter>> captor = ArgumentCaptor.forClass(List.class);
        verify(deploymentBoundary, times(1)).getFilteredDeployments(isNull(), isNull(), captor.capture(), isNull(), isNull(), isNull());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    public void getDeployment() {
        when(deploymentBoundary.getDeploymentById(deploymentEntity.getId())).thenReturn(deploymentEntity);

        Response response = deploymentRestService.getDeployment(deploymentEntity.getId());
        assertEquals(200, response.getStatus());
        
        DeploymentDTO deploymentDto = (DeploymentDTO) response.getEntity();
        assertEquals(deploymentEntity.getId(), deploymentDto.getId());

    }

    @Test
    public void getDeploymentLogFileNames_nothingFound() {
        String[] fileNames = {};
        when(deploymentBoundary.getLogFileNames(deploymentEntity.getId())).thenReturn(fileNames);

        Response response = deploymentRestService.getDeploymentLogFileNames(deploymentEntity.getId());

        assertNotNull(response);
        assertThat(Status.OK.getStatusCode(), is(response.getStatus()));
        List<DeploymentLog> expected = Collections.emptyList();
        assertThat(expected, is(response.getEntity()));
    }

    @Test
    public void getDeploymentLogfileNames() {
        String[] fileNames = {"log1", "log2"};
        when(deploymentBoundary.getLogFileNames(deploymentEntity.getId())).thenReturn(fileNames);

        Response response = deploymentRestService.getDeploymentLogFileNames(deploymentEntity.getId());

        assertNotNull(response);
        assertThat(Status.OK.getStatusCode(), is(response.getStatus()));
        verify(deploymentBoundary).getLogFileNames(deploymentEntity.getId());

        List<DeploymentLog> expected = List.of(
                new DeploymentLog(deploymentEntity.getId(), "log1"),
                new DeploymentLog(deploymentEntity.getId(), "log2"));

        assertThat(expected, is(response.getEntity()));
    }

    @Test
    public void getDeploymentNoResult() {
        when(deploymentBoundary.getDeploymentById(234)).thenThrow(new NoResultException());

        Response response = deploymentRestService.getDeployment(234);
        assertEquals(404, response.getStatus());
        
        ExceptionDto exception = (ExceptionDto) response.getEntity();
        assertTrue(exception.getMessage().length() > 0);

    }

    @Test
    public void getDeploymentLogFileContent() throws IllegalAccessException {
        String[] fileNames = {"log1", "log2"};
        when(deploymentBoundary.getLogFileNames(deploymentEntity.getId())).thenReturn(fileNames);
        when(deploymentBoundary.getDeploymentLog("log1")).thenReturn("content 1");

        Response response = deploymentRestService.getDeploymentLogFileContent(deploymentEntity.getId(), "log1");
        assertThat(response.getStatus(), is(200));
        String content = (String) response.getEntity();
        assertThat(content, is("content 1"));
        verify(deploymentBoundary).getDeploymentLog("log1");
    }

    @Test
    public void getDeploymentLogFileContent_notFound() throws IllegalAccessException {
        when(deploymentBoundary.getLogFileNames(deploymentEntity.getId())).thenReturn(new String[] {});
        when(deploymentBoundary.getDeploymentLog(anyString())).thenReturn("content");

        Response response = deploymentRestService.getDeploymentLogFileContent(deploymentEntity.getId(), "test");
        assertThat(response.getStatus(), is(400));
        verify(deploymentBoundary, never()).getDeploymentLog(anyString());
    }

    @Test
    public void getDeploymentLogs_withIllegalAccess() throws IllegalAccessException {
        String[] fileNames = {"log1", "log2"};
        when(deploymentBoundary.getLogFileNames(deploymentEntity.getId())).thenReturn(fileNames);
        when(deploymentBoundary.getDeploymentLog("unknown")).thenThrow(new IllegalAccessException());

        Response response = deploymentRestService.getDeploymentLogFileContent(deploymentEntity.getId(), "unknown");
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        String msg = (String) response.getEntity();
        assertThat(msg, is("No logfile with name unknown for deployment with id 123"));
    }

        @SuppressWarnings("unchecked")
        @Test
        public void addDeployment() {
                ReleaseEntity release = mockRelease();
                when(deploymentBoundary.createDeploymentReturnTrackingId(anyInt(), anyInt(), any(Date.class),
                                any(Date.class), any(LinkedList.class), any(LinkedList.class), any(ArrayList.class),
                                anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
                                                .thenReturn(deploymentEntity.getTrackingId());

                when(environmentsService.getContextByName(deploymentEntity.getContext().getName()))
                                .thenReturn(deploymentEntity.getContext());
                when(deploymentBoundary.getFilteredDeployments(eq(0), eq(1), any(LinkedList.class), isNull(), isNull(), isNull())).thenReturn(
                                                new Tuple<Set<DeploymentEntity>, Integer>(entities, entities.size()));
                when(releaseService.findByName(anyString())).thenReturn(release);

                when(generatorDomainServiceWithAppServerRelations.hasActiveNodeToDeployOnAtDate(
                                any(ResourceEntity.class), any(ContextEntity.class), ArgumentMatchers.<Date>any()))
                                                .thenReturn(Boolean.TRUE);

                Response response = deploymentRestService.addDeployment(deploymentRequestDto);

                assertEquals(201, response.getStatus());
                DeploymentDTO responseDto = (DeploymentDTO) response.getEntity();
                assertEquals(responseDto.getId(), deploymentDto.getId());

                LinkedList<String> metaList = new LinkedList<>();
                metaList.add("/deployments/" + deploymentEntity.getId());
                assertEquals(metaList, response.getMetadata().get("Location"));
        }

    @SuppressWarnings("unchecked")
    @Test
    public void addDeploymentWithoutAppsWithVersionShouldObtainAppsWithVersionFromBoundary() {
        // given
        ReleaseEntity release = mockRelease();
        when(deploymentBoundary.createDeploymentReturnTrackingId(anyInt(), anyInt(), any(Date.class), any(Date.class), any(LinkedList.class),
                any(LinkedList.class), any(ArrayList.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(deploymentEntity.getTrackingId());

        when(environmentsService.getContextByName(deploymentEntity.getContext().getName())).thenReturn(deploymentEntity.getContext());
        when(deploymentBoundary.getFilteredDeployments(eq(0), eq(1), any(LinkedList.class), isNull(), isNull(), isNull())).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(entities, entities.size()));
        when(releaseService.findByName(anyString())).thenReturn(release);
        when(generatorDomainServiceWithAppServerRelations.hasActiveNodeToDeployOnAtDate(
                any(ResourceEntity.class), any(ContextEntity.class), ArgumentMatchers.<Date>any()))
                                .thenReturn(Boolean.TRUE);
        when(deploymentBoundary.getVersions(deploymentEntity.getResource(), new ArrayList<Integer>(deploymentEntity.getContext().getId()), release)).thenReturn(deploymentEntity.getApplicationsWithVersion());
        deploymentRequestDto.setAppsWithVersion(null);

        // when
        Response response = deploymentRestService.addDeployment(deploymentRequestDto);

        // then
        assertEquals(201, response.getStatus());
        DeploymentDTO responseDto = (DeploymentDTO) response.getEntity();
        assertThat(responseDto.getId(), is(deploymentDto.getId()));
        assertThat(responseDto.getAppsWithVersion().size(), is(deploymentEntity.getApplicationsWithVersion().size()));
        assertThat(responseDto.getAppsWithVersion().get(0).getVersion(), is(deploymentEntity.getApplicationsWithVersion().get(0).getVersion()));
        assertThat(responseDto.getAppsWithVersion().get(1).getVersion(), is(deploymentEntity.getApplicationsWithVersion().get(1).getVersion()));
    }

    @Test
    public void addDeployment_no_active_node() {
        ReleaseEntity release = mockRelease();
        when(
                deploymentBoundary.createDeploymentReturnTrackingId(anyInt(), anyInt(), any(Date.class), any(Date.class), any(LinkedList.class),
                        any(LinkedList.class), any(ArrayList.class),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(deploymentEntity.getTrackingId());
        
        when(environmentsService.getContextByName(deploymentEntity.getContext().getName())).thenReturn(deploymentEntity.getContext());
        when(
                deploymentBoundary.getFilteredDeployments(anyInt(), anyInt(), any(LinkedList.class), anyString(),
                        any(CommonFilterService.SortingDirectionType.class), any(LinkedList.class))).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(entities, entities.size()));
        when(releaseService.findByName(anyString())).thenReturn(release);

        when(generatorDomainServiceWithAppServerRelations.hasActiveNodeToDeployOnAtDate(any(ResourceEntity.class), any(ContextEntity.class), any(Date.class))).thenReturn(Boolean.FALSE);
        
        Response response = deploymentRestService.addDeployment(deploymentRequestDto);

        assertEquals(424, response.getStatus());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void addDeploymentWithOutRelease() {
        ReleaseEntity release = mockRelease();
        when(
                deploymentBoundary.createDeploymentReturnTrackingId(anyInt(), anyInt(), any(Date.class), any(Date.class), any(LinkedList.class),
                        any(LinkedList.class), any(ArrayList.class), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(
                deploymentEntity.getTrackingId());
        when(environmentsService.getContextByName(deploymentEntity.getContext().getName())).thenReturn(deploymentEntity.getContext());
        when(deploymentBoundary.getFilteredDeployments(eq(0), eq(1), any(LinkedList.class), isNull(), isNull(), isNull())).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(entities, entities.size()));
        when(releaseService.findByName(null)).thenReturn(null);
        when(releaseService.loadAllReleases(false)).thenReturn(Collections.singletonList(release));
        when(generatorDomainServiceWithAppServerRelations.hasActiveNodeToDeployOnAtDate(
                any(ResourceEntity.class), any(ContextEntity.class), ArgumentMatchers.<Date>any()))
                                .thenReturn(Boolean.TRUE);        
        Response response = deploymentRestService.addDeployment(deploymentRequestDto);

        assertEquals(201, response.getStatus());
        DeploymentDTO responseDto = (DeploymentDTO) response.getEntity();
        assertEquals(responseDto.getId(), deploymentDto.getId());

        LinkedList<String> metaList = new LinkedList<>();
        metaList.add("/deployments/" + deploymentEntity.getId());
        assertEquals(metaList, response.getMetadata().get("Location"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addDeploymentWrongAppName() {
        DeploymentRequestDTO wrongdeploymentRequestDto = new DeploymentRequestDTO(deploymentRequestDto);
        ReleaseEntity release = mockRelease();
        wrongdeploymentRequestDto.getAppsWithVersion().get(0).setApplicationName("wrong");
        
        when(
                deploymentBoundary.createDeploymentReturnTrackingId(anyInt(), anyInt(), any(Date.class), any(Date.class), any(LinkedList.class),
                        any(LinkedList.class), any(ArrayList.class),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(deploymentEntity.getTrackingId());
        when(environmentsService.getContextByName(deploymentEntity.getContext().getName())).thenReturn(deploymentEntity.getContext());
        when(
                deploymentBoundary.getFilteredDeployments(anyInt(), anyInt(), any(LinkedList.class), anyString(),
                        any(CommonFilterService.SortingDirectionType.class), any(LinkedList.class))).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(entities, entities.size()));
        when(releaseService.findByName(anyString())).thenReturn(release);

        Response response = deploymentRestService.addDeployment(wrongdeploymentRequestDto);
        assertEquals(400, response.getStatus());
        
        ExceptionDto exception = (ExceptionDto) response.getEntity();
        assertTrue(exception.getDetail().length() > 0);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void addDeploymentNoAppInAs() {
        ReleaseEntity release = mockRelease();
        when(
                deploymentBoundary.createDeploymentReturnTrackingId(anyInt(), anyInt(), any(Date.class), any(Date.class), any(LinkedList.class),
                        any(LinkedList.class), any(ArrayList.class),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(deploymentEntity.getTrackingId());
        when(environmentsService.getContextByName(deploymentEntity.getContext().getName())).thenReturn(deploymentEntity.getContext());
        when(
                deploymentBoundary.getFilteredDeployments(anyInt(), anyInt(), any(LinkedList.class), anyString(),
                        any(CommonFilterService.SortingDirectionType.class), any(LinkedList.class))).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(entities, entities.size()));
        //the as has no apps
        when(applicationServer.getAMWApplications()).thenReturn(new LinkedList<Application>());
        when(releaseService.findByName(anyString())).thenReturn(release);
        
        Response response = deploymentRestService.addDeployment(deploymentRequestDto);
        assertEquals(400, response.getStatus());
        
        ExceptionDto exception = (ExceptionDto) response.getEntity();
        assertTrue(exception.getDetail().length() > 0);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void addDeploymentNotAllAppsInRequest() {
        
        //mock app
        List<Application> applications = applicationServer.getAMWApplications();
        Application application = mock(Application.class);
        when(application.getName()).thenReturn("noInRequest");
        applications.add(application);

        when(
                deploymentBoundary.createDeploymentReturnTrackingId(anyInt(), anyInt(), any(Date.class), any(Date.class), any(LinkedList.class),
                        any(LinkedList.class), any(ArrayList.class),
                        anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(deploymentEntity.getTrackingId());
        when(environmentsService.getContextByName(deploymentEntity.getContext().getName())).thenReturn(deploymentEntity.getContext());
        when(
                deploymentBoundary.getFilteredDeployments(anyInt(), anyInt(), any(LinkedList.class), anyString(),
                        any(CommonFilterService.SortingDirectionType.class), any(LinkedList.class))).thenReturn(
                new Tuple<Set<DeploymentEntity>, Integer>(entities, entities.size()));
        when(releaseService.findByName(anyString())).thenReturn(release);

        Response response = deploymentRestService.addDeployment(deploymentRequestDto);
        assertEquals(400, response.getStatus());
        
        ExceptionDto exception = (ExceptionDto) response.getEntity();
        assertTrue(exception.getDetail().length() > 0);
    }

    @Test
    public void shouldHandleIllegalStateInUpdateState() {
        // given
        String illegalState = "dudu";

        // when
        Response response = deploymentRestService.updateState(1, illegalState);

        // then
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void shouldHandleCancelInUpdateState() {
        // given
        String cancelState = DeploymentState.canceled.name();
        Integer deploymentId = 1;

        // when
        Response response = deploymentRestService.updateState(deploymentId, cancelState);

        // then
        verify(deploymentBoundary).cancelDeployment(deploymentId);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }

    @Test
    public void shouldHandleRejectInUpdateState() {
        // given
        String cancelState = DeploymentState.rejected.name();
        Integer deploymentId = 1;

        // when
        Response response = deploymentRestService.updateState(deploymentId, cancelState);

        // then
        verify(deploymentBoundary).rejectDeployment(deploymentId);
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }

    @Test
    public void shouldCreatePreservedDeploymentDTOIfCreateDeploymentDTOIsCalledWithADeploymentThatHasBeenPreserved() {
        // given
        String formerEnvironmentName = "EX";
        deploymentEntity.setContext(null);
        deploymentEntity.setExContextId(789);
        when(deploymentBoundary.getDeletedContextName(deploymentEntity)).thenReturn(formerEnvironmentName);

        // when
        DeploymentDTO dto = deploymentRestService.createDeploymentDTO(deploymentEntity);

        // then
        assertThat(dto.getEnvironmentName(), is(formerEnvironmentName));
    }

    @Test
    public void getDeploymentsShouldAlsoReturnPreservedOne() {
        // given
        String formerEnvironmentName = "EX";
        deploymentEntity.setContext(null);
        deploymentEntity.setExContextId(789);
        when(deploymentBoundary.getDeletedContextName(deploymentEntity)).thenReturn(formerEnvironmentName);
        Tuple<Set<DeploymentEntity>, Integer> resultTuple = new Tuple<Set<DeploymentEntity>, Integer>(new HashSet<>(Collections.singletonList(deploymentEntity)), 1);
        when(deploymentBoundary.getFilteredDeployments(null, null, new LinkedList<CustomFilter>(), null, null, null)).thenReturn(resultTuple);

        // when
        Response response = deploymentRestService.getDeployments(null, null, null, null, null, null, null, null, null, null, null, null, false);
        @SuppressWarnings("unchecked")
        ArrayList<DeploymentDTO> deploymentDtos = (ArrayList<DeploymentDTO>) response.getEntity();

        // then
        assertEquals(200, response.getStatus());
        assertEquals(1, deploymentDtos.size());
    }

    private ReleaseEntity mockRelease(){
        ReleaseEntity mock = mock(ReleaseEntity.class);
        Calendar cal = new GregorianCalendar();
        cal.set(2014, Calendar.JUNE, 1);
        when(mock.getId()).thenReturn(2);
        when(mock.getName()).thenReturn("RL-13.04");
        when(mock.getInstallationInProductionAt()).thenReturn(cal.getTime());
        return mock;
    }

}
