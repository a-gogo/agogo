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

package ch.puzzle.itc.mobiliar.business.resourceactivation.control;

import ch.puzzle.itc.mobiliar.business.environment.control.ContextHierarchy;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.resourceactivation.entity.ResourceActivationEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationContextRepository;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationContextEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.*;

public class ResourceActivationTest {

    @Mock
    EntityManager entityManager;

    @Mock
    ContextHierarchy contextHierarchy;

    @Mock
    ResourceRelationContextRepository resourceRelationContextRepository;

    @InjectMocks
    ResourceActivation resourceActivation;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSetResourceActivation_new_inactive() throws Exception {
        setResourceActivation(false, null, false, false);
        verify(entityManager, times(1)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(0)).remove(Mockito.any(ResourceActivationEntity.class));
    }

    @Test
    public void testSetResourceActivation_new_active() throws Exception {
        setResourceActivation(true, null, false, false);
        //If there is nothing set yet, and the element shall be set to active, we don't have to persist anything since this is default.
        verify(entityManager, times(0)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(0)).remove(Mockito.any(ResourceActivationEntity.class));
    }

    @Test
    public void testSetResourceActivation_nothing() throws Exception {
        setResourceActivation(null, null, false, false);
        verify(entityManager, times(0)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(0)).remove(Mockito.any(ResourceActivationEntity.class));
    }

    @Test
    public void testSetResourceActivation_sameLevel_differentValue() throws Exception {
        setResourceActivation(true, false, false, true);
        setResourceActivation(false, true, false, true);
        setResourceActivation(true, false, true, false);
        verify(entityManager, times(3)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(0)).remove(Mockito.any(ResourceActivationEntity.class));
    }

    @Test
    public void testSetResourceActivation_sameLevel_activateLastActivation() throws Exception{
        setResourceActivation(true, false, true, true);
        //If there is only one activation entity which is activated, it will be removed since active is default.
        verify(entityManager, times(0)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(1)).remove(Mockito.any(ResourceActivationEntity.class));
    }

    @Test
    public void testSetResourceActivation_sameLevel_noChanges() throws Exception {
        setResourceActivation(true, true, false, true);
        setResourceActivation(false, false, false, true);
        setResourceActivation(true, true, true, true);
        setResourceActivation(false, false, true, true);
        verify(entityManager, times(0)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(0)).remove(Mockito.any(ResourceActivationEntity.class));
    }

    @Test
    public void testSetResourceActivation_sameLevel_reset() throws Exception {
        setResourceActivation(null, true, false, true);
        setResourceActivation(null, false, false, true);
        setResourceActivation(null, true, true, true);
        setResourceActivation(null, false, true, true);
        verify(entityManager, times(0)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(4)).remove(Mockito.any(ResourceActivationEntity.class));
    }

    @Test
    public void testSetResourceActivation_differentLevel_differentValue() throws Exception {
        setResourceActivation(true, false, false, false);
        setResourceActivation(false, true, false, false);
        setResourceActivation(true, false, true, false);
        setResourceActivation(false, true, true, false);
        verify(entityManager, times(4)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(0)).remove(Mockito.any(ResourceActivationEntity.class));
    }

    @Test
    public void testSetResourceActivation_differentLevel_noChanges() throws Exception {
        setResourceActivation(true, true, false, false);
        setResourceActivation(false, false, false, false);
        setResourceActivation(true, true, true, false);
        setResourceActivation(false, false, true, false);
        //Since it is a different level, the elements have to persisted even though they do have the same value as before.
        verify(entityManager, times(4)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(0)).remove(Mockito.any(ResourceActivationEntity.class));
    }

    @Test
    public void testSetResourceActivation_differentLevel_reset() throws Exception {
        setResourceActivation(null, true, false, false);
        setResourceActivation(null, false, false, false);
        setResourceActivation(null, true, true, false);
        setResourceActivation(null, false, true, false);
        //Reset on a different level does not have any impact
        verify(entityManager, times(0)).persist(Mockito.any(ResourceActivationEntity.class));
        verify(entityManager, times(0)).remove(Mockito.any(ResourceActivationEntity.class));
    }



    private void setResourceActivation(Boolean shallBeActive, Boolean wasActive, boolean isOnlyResourceActivation, boolean definedOnSameContext) throws Exception {
        ResourceRelationContextEntity resRelCtx = Mockito.mock(ResourceRelationContextEntity.class);
        ContextEntity ctx = Mockito.mock(ContextEntity.class);
        when(resRelCtx.getContext()).thenReturn(ctx);
        when(ctx.getId()).thenReturn(1);
        ResourceGroupEntity resGrp = Mockito.mock(ResourceGroupEntity.class);
        if(wasActive!=null) {
            ResourceRelationContextEntity resRelCtx2 = Mockito.mock(ResourceRelationContextEntity.class);
            ContextEntity ctx2 = Mockito.mock(ContextEntity.class);
            when(resRelCtx2.getContext()).thenReturn(ctx2);
            when(ctx2.getId()).thenReturn(2);

            ResourceActivationEntity resActEnt = Mockito.mock(ResourceActivationEntity.class);
            if (definedOnSameContext) {
                when(resActEnt.getResourceRelationContext()).thenReturn(resRelCtx);
            }
            else {
                when(resActEnt.getResourceRelationContext()).thenReturn(resRelCtx2);
            }

            when(resActEnt.isActive()).thenReturn(wasActive);
            when(resActEnt.isOnlyActivationEntityForResourceRelation()).thenReturn(isOnlyResourceActivation);

            resourceActivation.setResourceActivation(resRelCtx, resGrp, resActEnt, shallBeActive);
        }
        else{
            resourceActivation.setResourceActivation(resRelCtx, resGrp, null, shallBeActive);
        }
    }


    @Test
    public void testGetMostRelevantResourceActivationEntities() throws Exception {
        ResourceActivation spiedResourceActivation = Mockito.spy(resourceActivation);
        ConsumedResourceRelationEntity resourceRelationEntity = Mockito.mock(
                  ConsumedResourceRelationEntity.class);
        ResourceGroupEntity resGroup = Mockito.mock(ResourceGroupEntity.class);
        when(resGroup.getId()).thenReturn(123);

        ResourceRelationContextEntity globalContext = Mockito.mock(ResourceRelationContextEntity.class);
        ResourceActivationEntity globalResActEnt = Mockito.mock(ResourceActivationEntity.class);

        when(globalResActEnt.getResourceGroup()).thenReturn(resGroup);
        when(globalContext.getResourceActivationEntities()).thenReturn(new HashSet<>(Arrays.asList(globalResActEnt)));

        ResourceRelationContextEntity envContext = Mockito.mock(ResourceRelationContextEntity.class);
        ResourceActivationEntity envResActEnt = Mockito.mock(ResourceActivationEntity.class);
        when(envResActEnt.getResourceGroup()).thenReturn(resGroup);
        when(envContext.getResourceActivationEntities()).thenReturn(new HashSet<>(Arrays.asList(envResActEnt)));

        //Return the resource relation contexts in the correct order (lower first, global last)
        doReturn(Arrays.asList(envContext, globalContext)).when(spiedResourceActivation).getResourceActivationEntitiesForContextHierarchy(
                  Mockito.any(ConsumedResourceRelationEntity.class),
                  Mockito.any(ContextEntity.class));

        List<ResourceActivationEntity> resourceActivations = spiedResourceActivation.getMostRelevantResourceActivationEntities(
                  resourceRelationEntity, Mockito.mock(ContextEntity.class));

        Assert.assertEquals(1, resourceActivations.size());
        Assert.assertEquals(envResActEnt, resourceActivations.get(0));
        verify(envResActEnt, times(1)).setOnlyActivationEntityForResourceRelation(true);
    }

    @Test
    public void testGetResourceActivationEntitiesForContextHierarchy() throws Exception {
        ContextEntity global = new ContextEntity();
        global.setId(1);
        ContextEntity domain= new ContextEntity();
        domain.setId(2);
        ContextEntity env = new ContextEntity();
        env.setId(3);
        List<Integer> contextIds = new ArrayList<>(Arrays.asList(global.getId(), domain.getId(), env.getId()));
        when(contextHierarchy.getContextWithParentIds(Mockito.any(ContextEntity.class))).thenReturn(
                  contextIds);

        ResourceRelationContextEntity globalContext = Mockito.mock(ResourceRelationContextEntity.class);
        when(globalContext.getContext()).thenReturn(global);
        ResourceRelationContextEntity envContext = Mockito.mock(ResourceRelationContextEntity.class);
        when(envContext.getContext()).thenReturn(env);

        ConsumedResourceRelationEntity relation = Mockito.mock(ConsumedResourceRelationEntity.class);

        //We get a list of a global and an env resource relation context (important: global first, env second)
        when(resourceRelationContextRepository.getResourceRelationContextEntitiesByContextIds(relation, contextIds)).thenReturn(new ArrayList<>(Arrays.asList(globalContext, envContext)));

        List<ResourceRelationContextEntity> result = resourceActivation.getResourceActivationEntitiesForContextHierarchy(relation, env);

        //Make sure, the result is sorted correctly (environment context first, global context last).
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(envContext, result.get(0));
        Assert.assertEquals(globalContext, result.get(1));
    }
}