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

package ch.puzzle.itc.mobiliar.business.resourcegroup.boundary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import ch.puzzle.itc.mobiliar.common.util.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.integration.entity.util.ResourceTypeEntityBuilder;
import ch.puzzle.itc.mobiliar.business.releasing.boundary.ReleaseLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceRepository;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceFactory;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.common.util.ConfigurationService;

public class ResourceLocatorTest {

    private static final String PROVIDABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_1 = "type1";
    private static final String PROVIDABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_2 = "type2";

    private static final String CONSUMABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_1 = "constype1";
    private static final String CONSUMABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_2 = "constype2";

    @Mock
    private ResourceRepository resourceRepositoryMock;

    @Mock
    private ReleaseLocator releaseLocatorMock;

    @Mock
    private ResourceDependencyResolverService resourceDependencyResolverServiceMock;

    @Mock
    private EntityManager entityManagerMock;

    @Mock
    private Logger logMock;

    @InjectMocks
    private ResourceLocator resourceLocator;

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);

        Properties props = System.getProperties();
        props.setProperty(ConfigKey.PROVIDABLE_SOFTLINK_RESOURCE_TYPES.getValue(),
                PROVIDABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_1 + "," + PROVIDABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_2);
        props.setProperty(ConfigKey.CONSUMABLE_SOFTLINK_RESOURCE_TYPES.getValue(),
                CONSUMABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_1 + "," + CONSUMABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_2);
        System.setProperties(props);
    }

    @After
    public void tearDown() {
        Properties props = System.getProperties();
        props.remove(ConfigKey.PROVIDABLE_SOFTLINK_RESOURCE_TYPES.getValue());
        props.remove(ConfigKey.CONSUMABLE_SOFTLINK_RESOURCE_TYPES.getValue());
        System.setProperties(props);
    }

    @Test
    public void hasResourceProvidableSoftlinkTypeWhenResourceTypeIsOfProvidableSoftlinkResourceTypeShouldReturnTrue() {
        // given
        Integer resourceId = 1;
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder()
                .name(PROVIDABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_1).build();
        ResourceEntity resourceWithTypes = new ResourceEntityBuilder().withType(resourceType).build();

        Mockito.when(entityManagerMock.find(ResourceEntity.class, resourceId)).thenReturn(resourceWithTypes);

        // when
        boolean hasResourceProvidableType = resourceLocator.hasResourceProvidableSoftlinkType(resourceId);

        // then
        assertTrue(hasResourceProvidableType);
    }

    @Test
    public void hasResourceProvidableSoftlinkTypeWhenSuperResourceTypeIsOfProvidableSoftlinkResourceTypeShouldReturnTrue() {
        // given
        Integer resourceId = 1;
        ResourceTypeEntity superResourceType = new ResourceTypeEntityBuilder()
                .name(PROVIDABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_1).build();
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().name("otherResourceType")
                .parentResourceType(superResourceType).build();
        ResourceEntity resourceWithTypes = new ResourceEntityBuilder().withType(resourceType).build();

        Mockito.when(entityManagerMock.find(ResourceEntity.class, resourceId)).thenReturn(resourceWithTypes);

        // when
        boolean hasResourceProvidableType = resourceLocator.hasResourceProvidableSoftlinkType(resourceId);

        // then
        assertTrue(hasResourceProvidableType);
    }

    @Test
    public void hasResourceProvidableSoftlinkTypeWhenNoResourceTypeIsOfProvidableSoftlinkResourceTypeShouldReturnFalse() {
        // given
        Integer resourceId = 1;
        ResourceTypeEntity superResourceType = new ResourceTypeEntityBuilder().name("otherSuperResourceType").build();
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().name("otherResourceType")
                .parentResourceType(superResourceType).build();
        ResourceEntity resourceWithTypes = new ResourceEntityBuilder().withType(resourceType).build();

        Mockito.when(entityManagerMock.find(ResourceEntity.class, resourceId)).thenReturn(resourceWithTypes);

        // when
        boolean hasResourceProvidableType = resourceLocator.hasResourceProvidableSoftlinkType(resourceId);

        // then
        assertFalse(hasResourceProvidableType);
    }

    @Test
    public void extractProvidableSoftlinkResourceTypesWhenNoSystemPropertyIsSetShouldReturnEmptyList() {
        // given
        tearDown(); // remove systemProperty
        assertNull(ConfigurationService.getProperty(ConfigKey.PROVIDABLE_SOFTLINK_RESOURCE_TYPES));

        // when
        List<String> extractedResourceTypes = resourceLocator.extractResourceTypeSystemProperties(
                ConfigKey.PROVIDABLE_SOFTLINK_RESOURCE_TYPES, null);

        // then
        assertTrue(extractedResourceTypes.isEmpty());
    }

    @Test
    public void extractProvidableSoftlinkResourceTypesWhenNoSystemPropertyIsSetShouldReturnDefaultPropertyValue() {
        // given
        String defaultPropertyValue = "defaultPropertyValue";

        tearDown(); // remove systemProperty
        assertNull(ConfigurationService.getProperty(ConfigKey.PROVIDABLE_SOFTLINK_RESOURCE_TYPES));

        // when
        List<String> extractedResourceTypes = resourceLocator.extractResourceTypeSystemProperties(
                ConfigKey.PROVIDABLE_SOFTLINK_RESOURCE_TYPES, defaultPropertyValue);

        // then
        assertEquals(defaultPropertyValue.toLowerCase(), extractedResourceTypes.get(0));
    }

    @Test
    public void hasResourceConsumableSoftlinkTypeWhenResourceTypeIsOfConsumableSoftlinkResourceTypeShouldReturnTrue() {
        // given
        Integer resourceId = 1;
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder()
                .name(CONSUMABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_1).build();
        ResourceEntity resourceWithTypes = new ResourceEntityBuilder().withType(resourceType).build();

        Mockito.when(entityManagerMock.find(ResourceEntity.class, resourceId)).thenReturn(resourceWithTypes);

        // when
        boolean hasResourceProvidableType = resourceLocator.hasResourceConsumableSoftlinkType(resourceId);

        // then
        assertTrue(hasResourceProvidableType);
    }

    @Test
    public void hasResourceConsumableSoftlinkTypeWhenSuperResourceTypeIsOfConsumableSoftlinkResourceTypeShouldReturnTrue() {
        // given
        Integer resourceId = 1;
        ResourceTypeEntity superResourceType = new ResourceTypeEntityBuilder()
                .name(CONSUMABLE_SOFTLINK_TYPE_SYSTEM_PROPERTY_1).build();
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().name("otherResourceType")
                .parentResourceType(superResourceType).build();
        ResourceEntity resourceWithTypes = new ResourceEntityBuilder().withType(resourceType).build();

        Mockito.when(entityManagerMock.find(ResourceEntity.class, resourceId)).thenReturn(resourceWithTypes);

        // when
        boolean hasResourceProvidableType = resourceLocator.hasResourceConsumableSoftlinkType(resourceId);

        // then
        assertTrue(hasResourceProvidableType);
    }

    @Test
    public void hasResourceConsumableSoftlinkTypeWhenNoResourceTypeIsOfConsumableSoftlinkResourceTypeShouldReturnFalse() {
        // given
        Integer resourceId = 1;
        ResourceTypeEntity superResourceType = new ResourceTypeEntityBuilder().name("otherSuperResourceType").build();
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().name("otherResourceType")
                .parentResourceType(superResourceType).build();
        ResourceEntity resourceWithTypes = new ResourceEntityBuilder().withType(resourceType).build();

        Mockito.when(entityManagerMock.find(ResourceEntity.class, resourceId)).thenReturn(resourceWithTypes);

        // when
        boolean hasResourceProvidableType = resourceLocator.hasResourceConsumableSoftlinkType(resourceId);

        // then
        assertFalse(hasResourceProvidableType);
    }

    @Test
    public void getResourceByGroupName() throws ValidationException {
        // given
        String name = "ch_mobi_fofa_fofa_selection";
        List<ResourceEntity> list = new ArrayList<>();
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().name("ch_mobi_fofa_fofa_selection").build();
        ResourceEntity entity = new ResourceEntityBuilder().withType(resourceType).build();
        entity.setName("ch_mobi_fofa_fofa_selection");
        list.add(entity);
        Mockito.when(resourceRepositoryMock.getResourceByName(Mockito.anyString())).thenReturn(list);

        // when
        List<ResourceEntity> result = resourceLocator.getResourceByGroupName(name);

        // then
        assertTrue(name.equals(result.get(0).getName()));
    }

    @Test
    public void getAllApplicationsWithResource() throws ValidationException {
        
        // given       
        int id = 2305; //batchjobs
        String name = "ch_mobi_fofa_fofa_selection";
        List<ResourceEntity> list = new ArrayList<>();
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().name(name).build();

        ResourceEntity slave1 = ResourceFactory.createNewResource("BatchJob");
        ResourceTypeEntity type1 = new ResourceTypeEntity();
        type1.setName("type1");
        type1.setId(id);
        slave1.setResourceType(type1);

        ConsumedResourceRelationEntity relation1 = new ConsumedResourceRelationEntity();
        relation1.setSlaveResource(slave1);
        relation1.setIdentifier("1");
               
        ResourceEntity entity = new ResourceEntityBuilder().withType(resourceType).build();
        entity.addConsumedRelation(relation1);
        entity.setName(name);
        list.add(entity);        
        Mockito.when(resourceRepositoryMock.getAllApplicationsWithResource(Mockito.anyInt())).thenReturn(list);
        
        // when
        List<ResourceEntity> result = resourceLocator.getAllApplicationsWithResource(id);

        // then
        ResourceEntity re = result.get(0);
        assertTrue(name.equals(re.getName()));
        Object[] rel = re.getConsumedMasterRelations().toArray();

        assertTrue("BatchJob".equals(((ConsumedResourceRelationEntity)rel[0]).getSlaveResource().getName()));
    }

    @Test
    public void getBatchJobConsumedResources() throws ValidationException {
        
        // given       
        String name = "ch_mobi_fofa_fofa_selection";
        List<String> apps = new ArrayList<>();
        String resource = "DB2";
        apps.add(name);

        List<ResourceEntity> list = new ArrayList<>();
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().name(name).build();

        ResourceEntity slave1 = ResourceFactory.createNewResource(resource);
        ResourceTypeEntity type1 = new ResourceTypeEntity();
        type1.setName("db2Host");
        slave1.setResourceType(type1);

        ConsumedResourceRelationEntity relation1 = new ConsumedResourceRelationEntity();
        relation1.setSlaveResource(slave1);
        relation1.setIdentifier("1");
               
        ResourceEntity entity = new ResourceEntityBuilder().withType(resourceType).build();
        entity.addConsumedRelation(relation1);
        list.add(entity);        
        Mockito.when(resourceRepositoryMock.getBatchJobConsumedResources(Mockito.anyList())).thenReturn(list);
        
        // when
        List<ResourceEntity> result = resourceLocator.getBatchJobConsumedResources(apps);

        // then
        ResourceEntity re = result.get(0);
        Object[] rel = re.getConsumedMasterRelations().toArray();

        assertTrue(resource.equals(((ConsumedResourceRelationEntity)rel[0]).getSlaveResource().getName()));
    }

    @Test
    public void getBatchJobProvidedResources() throws ValidationException {
        
        // given       
        String name = "ch_mobi_javabatch_beispielapplikation";
        String resource = "integrationFile";
        List<String> apps = new ArrayList<>();
        apps.add(name);

        List<ResourceEntity> list = new ArrayList<>();
        ResourceTypeEntity resourceType = new ResourceTypeEntityBuilder().name(name).build();

        ResourceEntity slave1 = ResourceFactory.createNewResource(resource);
        ResourceTypeEntity type1 = new ResourceTypeEntity();
        type1.setName("File");
        slave1.setResourceType(type1);

        ProvidedResourceRelationEntity relation1 = new ProvidedResourceRelationEntity();
        relation1.setSlaveResource(slave1);
        relation1.setIdentifier("1");
               
        ResourceEntity entity = new ResourceEntityBuilder().withType(resourceType).build();
        entity.addProvidedRelation(relation1);
        list.add(entity);        
        Mockito.when(resourceRepositoryMock.getBatchJobProvidedResources(Mockito.anyList())).thenReturn(list);
        
        // when
        List<ResourceEntity> result = resourceLocator.getBatchJobProvidedResources(apps);

        // then
        ResourceEntity re = result.get(0);
        Object[] rel = re.getProvidedMasterRelations().toArray();

        assertTrue(resource.equals(((ProvidedResourceRelationEntity)rel[0]).getSlaveResource().getName()));
    }
    
    
    @Test
    public void getAppToAppServerMappingEmpty() {
        // given
        List<String> list = new ArrayList<>();
        list.add("app");

        // when
        Map<String, String> map = resourceLocator.getAppToAppServerMapping(list);

        // then
        assertTrue(map.isEmpty());
    }

    @Test
    public void getAppToAppServerMapping() {
        // given
        List<String> list = new ArrayList<>();
        list.add("app");
        List<ResourceEntity> value = new ArrayList<>();

        ResourceEntity master = ResourceFactory.createNewResource("server");
        ResourceEntity slave1 = ResourceFactory.createNewResource("app");
        ResourceEntity slave2 = ResourceFactory.createNewResource(slave1.getResourceGroup());

        ConsumedResourceRelationEntity relation1 = new ConsumedResourceRelationEntity();
        relation1.setMasterResource(master);
        relation1.setSlaveResource(slave1);
        relation1.setIdentifier("1");
        master.addConsumedRelation(relation1);
        ConsumedResourceRelationEntity relation2 = new ConsumedResourceRelationEntity();
        relation2.setMasterResource(master);
        relation2.setSlaveResource(slave2);
        relation2.setIdentifier("1");
        master.addConsumedRelation(relation2);

        value.add(master);
        Mockito.when(resourceRepositoryMock.getAppToAppServerMapping(list)).thenReturn(value);

        // when
        Map<String, String> map = resourceLocator.getAppToAppServerMapping(list);

        // then
        assertTrue(map.size() == 1);
        assertTrue(map.get("app").equals("server"));

    }

}