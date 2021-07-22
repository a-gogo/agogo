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

import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceFactory;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.usersettings.entity.FavoriteResourceEntity;
import ch.puzzle.itc.mobiliar.business.usersettings.entity.UserSettingsEntity;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceTypeNotFoundException;
import ch.puzzle.itc.mobiliar.common.util.ApplicationServerContainer;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.test.testrunner.PersistenceTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

@RunWith(PersistenceTestRunner.class)
public class ResourceGroupRepositoryTest {


    @Spy
    @PersistenceContext
    EntityManager entityManager;

    @Mock
    Logger log;

    @InjectMocks
    ResourceGroupRepository repository;

    @InjectMocks
    ResourceTypeProvider resourceTypeProvider;

    @Mock
    ResourceTypeRepository resourceTypeRepository;


    ResourceTypeEntity type1;
    ResourceTypeEntity type2;
    ResourceEntity resource1;
    ResourceEntity resource2;
    ResourceEntity resource3;
    ResourceEntity resource4;
    ResourceEntity asContainer;

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
    }

    private void init() {
        // ResourceTypes
        type1 = resourceTypeProvider.getOrCreateDefaultResourceType(DefaultResourceTypeDefinition.APPLICATIONSERVER);
        entityManager.persist(type1);
        type2 = resourceTypeProvider.getOrCreateDefaultResourceType(DefaultResourceTypeDefinition.APPLICATION);
        entityManager.persist(type2);

        // Resources
        resource1 = ResourceFactory.createNewResource("Z");
        resource1.setResourceType(type1);
        entityManager.persist(resource1);

        resource2 = ResourceFactory.createNewResource(resource1.getResourceGroup());
        resource2.setResourceType(type1);
        entityManager.persist(resource2);

        resource3 = ResourceFactory.createNewResource("D");
        resource3.setResourceType(type2);
        entityManager.persist(resource3);

        resource4 = ResourceFactory.createNewResource("C");
        resource4.setResourceType(type1);
        entityManager.persist(resource4);

        asContainer = ResourceFactory.createNewResource(ApplicationServerContainer.APPSERVERCONTAINER.getDisplayName());
        asContainer.setResourceType(type1);
        entityManager.persist(asContainer);
    }

    @Test
    public void test_loadGroupsForType() throws ElementAlreadyExistsException,
            ResourceNotFoundException, ResourceTypeNotFoundException {
        // given
        init();

        UserSettingsEntity userEntity = new UserSettingsEntity();
        userEntity.setUserName("tester");
        entityManager.persist(userEntity);

        FavoriteResourceEntity fav = new FavoriteResourceEntity();
        fav.setResourceGroup(resource1.getResourceGroup());
        fav.setUser(userEntity);
        entityManager.persist(fav);

        // when
        List<ResourceGroupEntity> result = repository.getGroupsForType(type1.getId(), null, true);
        List<ResourceGroupEntity> resultFavorites = repository.getGroupsForType(type1.getId(), Collections.singletonList(resource1.getId()), true);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        for (ResourceGroupEntity g : result) {
            for (ResourceEntity r : g.getResources()) {
                assertEquals(type1.getId(), r.getResourceType().getId());
            }
        }

        assertNotNull(resultFavorites);
        assertEquals(1, resultFavorites.size());
    }

    @Test
    public void test_loadGroupsForType_myAmw() throws ElementAlreadyExistsException, ResourceNotFoundException,
            ResourceTypeNotFoundException {
        // given
        init();

        List<Integer> myAmw = new ArrayList<Integer>();
        myAmw.add(resource1.getResourceGroup().getId());
        myAmw.add(resource4.getResourceGroup().getId());

        // when
        List<ResourceGroupEntity> result = repository.getGroupsForType(type1.getId(), myAmw, true);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1.getResourceGroup()));
        assertTrue(result.contains(resource4.getResourceGroup()));
    }
}