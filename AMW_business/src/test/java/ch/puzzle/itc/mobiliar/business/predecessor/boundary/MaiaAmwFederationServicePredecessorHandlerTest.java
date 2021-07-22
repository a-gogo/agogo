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

package ch.puzzle.itc.mobiliar.business.predecessor.boundary;

import ch.puzzle.itc.mobiliar.builders.ReleaseEntityBuilder;
import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.builders.ResourceRelationEntityBuilder;
import ch.puzzle.itc.mobiliar.business.foreignable.control.ForeignableService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.MessageSeverity;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.PredecessorResult;
import ch.puzzle.itc.mobiliar.business.predecessor.entity.ProcessingState;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.RelationImportService;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationService;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.AMWRuntimeException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link MaiaAmwFederationServicePredecessorHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class MaiaAmwFederationServicePredecessorHandlerTest {

    private static final String APPNAME_P = "predecessor";
    private static final String APPNAME_S = "successor";

    @InjectMocks
    MaiaAmwFederationServicePredecessorHandler servicePredecessorHandler;

    @Mock
    private ResourceLocator resourceLocatorMock;

    @Mock
    private ResourceRepository resourceRepositoryMock;

    @Mock
    private ResourceDependencyResolverService dependencyResolverServiceMock;

    @Mock
    private ForeignableService foreignableServiceMock;

    @Mock
    private Logger loggerMock;

    @Mock
    private CopyResourceDomainService copyServiceMock;

    @Mock
    private ResourceRelationService resourceRelationServiceMock;

    @Mock
    private RelationImportService relationImportServiceMock;

    private ResourceRelationEntityBuilder relationBuilder = new ResourceRelationEntityBuilder();


    @Test(expected = AMWRuntimeException.class)
    public void shouldFailIfSuccessorIsNotFound() throws ValidationException {

        // given
        Mockito.when(resourceRepositoryMock.getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_S)).thenReturn(new LinkedList<ResourceEntity>());

        // when // then
        servicePredecessorHandler.handlePredecessor(APPNAME_S, APPNAME_P, ForeignableOwner.MAIA);
    }

    @Test
    public void shouldPassEvenWhenPredecessorIsNotFound() throws ValidationException {
        // given
        ResourceEntity resource = ResourceEntityBuilder.createResourceEntity(APPNAME_S, 1);
        List<ResourceEntity> resources = new ArrayList<>();
        resources.add(resource);
        Mockito.when(resourceRepositoryMock.getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_S)).thenReturn(resources);
        Mockito.when(resourceRepositoryMock.getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_P)).thenReturn(new LinkedList<ResourceEntity>());

        // when
        servicePredecessorHandler.handlePredecessor(APPNAME_S, APPNAME_P, ForeignableOwner.MAIA);

        // then
        Mockito.verify(resourceRepositoryMock, Mockito.times(1)).getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_P);
        Mockito.verify(resourceRepositoryMock, Mockito.times(1)).getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_S);
    }

    @Test
    public void shouldPassEvenWhenPredecessorCpiIsNotFound() throws ValidationException {
        // given
        ResourceEntity successorResource = ResourceEntityBuilder.createResourceEntity(APPNAME_S, 1);
        ResourceEntity predecessorResource = ResourceEntityBuilder.createResourceEntity(APPNAME_P, 1);

        String cpiLocalPortId = "cpiLocalPortId";
        ResourceEntity cpi = new ResourceEntityBuilder().withName("cpi").withTypeOfName(ResourceLocator.WS_CPI_TYPE).withId(10).withLocalPortId(cpiLocalPortId).build();

        successorResource.addConsumedRelation(relationBuilder.buildConsumedResRelEntity(successorResource, cpi, "fooCpi", 100));

        List<ResourceEntity> resources = new ArrayList<>();
        resources.add(predecessorResource);
        Mockito.when(resourceRepositoryMock.getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_S)).thenReturn(Collections.singletonList(successorResource));
        Mockito.when(resourceRepositoryMock.getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_P)).thenReturn(new LinkedList<ResourceEntity>(resources));

        // when
        PredecessorResult rh = servicePredecessorHandler.handlePredecessor(APPNAME_S, APPNAME_P, ForeignableOwner.MAIA);

        // then
        assertEquals(ProcessingState.OK, rh.getProcessingState());
        assertEquals(1, rh.getMessages().size());
        assertEquals(MessageSeverity.WARNING, rh.getMessages().get(0).getSeverity());
    }

    @Test
    public void shouldPassEvenWhenPredecessorPpiIsNotFound() throws ValidationException {
        // given
        ResourceEntity successorResource = ResourceEntityBuilder.createResourceEntity(APPNAME_S, 1);
        ResourceEntity predecessorResource = ResourceEntityBuilder.createResourceEntity(APPNAME_P, 1);

        String ppiLocalPortId = "ppiLocalPortId";
        ResourceEntity ppi = new ResourceEntityBuilder().withName("ppi").withTypeOfName(ResourceLocator.WS_PPI_TYPE).withId(20).withLocalPortId(ppiLocalPortId).build();

        successorResource.addProvidedRelation(relationBuilder.buildProvidedResRelEntity(successorResource, ppi, "fooPpi", 200));

        List<ResourceEntity> resources = new ArrayList<>();
        resources.add(predecessorResource);
        Mockito.when(resourceRepositoryMock.getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_S)).thenReturn(Collections.singletonList(successorResource));
        Mockito.when(resourceRepositoryMock.getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_P)).thenReturn(new LinkedList<ResourceEntity>(resources));

        // when
        PredecessorResult rh = servicePredecessorHandler.handlePredecessor(APPNAME_S, APPNAME_P, ForeignableOwner.MAIA);

        // then
        assertEquals(ProcessingState.OK, rh.getProcessingState());
        assertEquals(1, rh.getMessages().size());
        assertEquals(MessageSeverity.WARNING, rh.getMessages().get(0).getSeverity());
    }

    @Test
    public void shouldPassWhenEverythingIsFine() throws ValidationException, ForeignableOwnerViolationException, AMWException {
        // given

        String[] releaseNames = {"older","newer","newest"};
        List<ReleaseEntity> releases = new LinkedList<>();
        Integer i = 1;
        for (String releaseName : releaseNames) {
            releases.add(ReleaseEntityBuilder.createMainReleaseEntity(releaseName, i++));
        }


        ResourceEntity successorResource = ResourceEntityBuilder.createResourceEntity(APPNAME_S, 1);
        successorResource.setRelease(releases.get(2));
        List<ResourceEntity> successorResources = new ArrayList<>();
        successorResources.add(successorResource);

        ResourceEntity predecessorResource = ResourceEntityBuilder.createResourceEntity(APPNAME_P, 2);
        predecessorResource.setRelease(releases.get(0));
        List<ResourceEntity> predecessorResources = new ArrayList<>();
        predecessorResources.add(predecessorResource);

        Mockito.when(resourceRepositoryMock.getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_S)).thenReturn(successorResources);
        Mockito.when(resourceRepositoryMock.getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_P)).thenReturn(predecessorResources);
        Mockito.when(dependencyResolverServiceMock.getResourceEntityForRelease(predecessorResources, successorResources.get(0).getRelease())).thenReturn(predecessorResource);


        // when
        servicePredecessorHandler.handlePredecessor(APPNAME_S, APPNAME_P, ForeignableOwner.MAIA);

        // then
        Mockito.verify(resourceRepositoryMock, Mockito.times(1)).getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_P);
        Mockito.verify(resourceRepositoryMock, Mockito.times(1)).getResourcesByGroupNameWithAllRelationsOrderedByRelease(APPNAME_S);
        Mockito.verify(copyServiceMock, Mockito.times(1)).copyFromPredecessorToSuccessorResource(predecessorResource, successorResource, ForeignableOwner.MAIA);
    }
}