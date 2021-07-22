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

package ch.puzzle.itc.mobiliar.business.property.control;

import ch.puzzle.itc.mobiliar.builders.ResourceEditPropertyBuilder;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextDependency;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextTypeEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditProperty;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import ch.puzzle.itc.mobiliar.business.auditview.control.AuditService;
import ch.puzzle.itc.mobiliar.business.auditview.control.ThreadLocalUtil;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PropertyValueServiceTest {


    private Integer propertyDescriptorId = 1;

    @Mock
    EntityManager entityManagerMock;

    @Mock
    ContextDependency<ContextEntity> resourceContextMock;

    @Mock
    PropertyValidationService propertyValidationServiceMock;

    @Spy
    AuditService auditService = spy(new AuditService());

    @Spy
    @InjectMocks
    PropertyValueService service = spy(new PropertyValueService());

    @Before
    public void init() {
        ThreadLocalUtil.destroy();
    }

    @Test
    public void resetPropertyValueShouldRemovePropertyOnContext(){
        // given
        Integer propertyId = 99;
        PropertyEntity property = createPropertyWithId(propertyId);
        Mockito.when(resourceContextMock.getPropertyForDescriptor(propertyDescriptorId)).thenReturn(property);

        //when
        service.resetPropertyValue(resourceContextMock, propertyDescriptorId);

        // then
        Mockito.verify(resourceContextMock).removePropertyById(propertyId);
    }

    private PropertyEntity createPropertyWithId(Integer id){
        PropertyEntity property = new PropertyEntity();
        property.setId(id);
        return property;
    }

    @Test(expected = NoResultException.class)
    public void resetPropertyValueShouldThrowExceptionWhenPropertyNotFound(){
        // given
        Mockito.when(resourceContextMock.getPropertyForDescriptor(propertyDescriptorId)).thenReturn(null);

        //when
        service.resetPropertyValue(resourceContextMock, propertyDescriptorId);

    }

    @Test
    public void setPropertyValueShouldLoadPropertyFromDBAndAddToContextWhenPropertyIsNotYetInContext() throws ValidationException {
        // given
        String unobfuscatedValue = "some value";
        PropertyDescriptorEntity descriptorMock = mock(PropertyDescriptorEntity.class);

        Mockito.when(entityManagerMock.find(PropertyDescriptorEntity.class, propertyDescriptorId)).thenReturn(descriptorMock);
        Mockito.when(resourceContextMock.getPropertyForDescriptor(propertyDescriptorId)).thenReturn(null);
        Mockito.when(propertyValidationServiceMock.canPropertyValueBeSetOnContext(Mockito.any(PropertyDescriptorEntity.class), Mockito.any(ContextDependency.class))).thenReturn(true);

        //when
        service.setPropertyValue(resourceContextMock, propertyDescriptorId, unobfuscatedValue);

        // then
        Mockito.verify(entityManagerMock).find(PropertyDescriptorEntity.class, propertyDescriptorId);
        Mockito.verify(resourceContextMock).addProperty(Mockito.any(PropertyEntity.class));
    }


    @Test
    public void setPropertyValueShouldSetValueAndCommentToPropertyInContext() throws ValidationException {
        // given
        String unobfuscatedValue = "some value";

        PropertyEntity propertyMock = mock(PropertyEntity.class);
        PropertyDescriptorEntity descriptorMock = mock(PropertyDescriptorEntity.class);

        Mockito.when(resourceContextMock.getPropertyForDescriptor(propertyDescriptorId)).thenReturn(propertyMock);
        Mockito.when(propertyMock.getDescriptor()).thenReturn(descriptorMock);
        Mockito.when(propertyValidationServiceMock.canPropertyValueBeSetOnContext(Mockito.any(PropertyDescriptorEntity.class), Mockito.any(ContextDependency.class))).thenReturn(true);

        //when
        service.setPropertyValue(resourceContextMock, propertyDescriptorId, unobfuscatedValue);

        // then
        Mockito.verify(propertyMock).setValue(unobfuscatedValue);
    }

    @Test(expected = ValidationException.class)
    public void setPropertyValueShouldThrowValidationExceptionWhenPropertyIsNotAllowedToSetOnContext() throws ValidationException {
        // given
        String unobfuscatedValue = "some value";

        PropertyEntity propertyMock = mock(PropertyEntity.class);

        Mockito.when(resourceContextMock.getPropertyForDescriptor(propertyDescriptorId)).thenReturn(propertyMock);

        //when
        service.setPropertyValue(resourceContextMock, propertyDescriptorId, unobfuscatedValue);

    }

    @Test(expected = ValidationException.class)
    public void verifyDefaultPropertyCanBeSetWhenValueEqualsDefaultValueOnGlobalContextShouldThrowException() throws ValidationException {
        // given
        String defaultValue = "defaultValue";

        ContextEntity contextEntity = new ContextEntity();
        ContextTypeEntity contextType = new ContextTypeEntity();
        contextType.setName("Global");
        contextEntity.setContextType(contextType);
        ResourceEditProperty resourceEditPropertyMock = mock(ResourceEditProperty.class);
        Mockito.when(resourceEditPropertyMock.getDefaultValue()).thenReturn(defaultValue);
        Mockito.when(resourceEditPropertyMock.getPropertyValue()).thenReturn(defaultValue);
        Mockito.when(resourceEditPropertyMock.getParent()).thenReturn(null);
        Mockito.when(resourceEditPropertyMock.getOriginalValue()).thenReturn(null);

        //when
        service.verifyDefaultPropertyCanBeSet(resourceEditPropertyMock, contextEntity);
    }

    @Test
    public void verifyDefaultPropertyCanBeSetWhenValueIsNotEqualsDefaultValueOnGlobalContextShouldNotThrowExcaption() throws ValidationException {
        // given
        String defaultValue = "defaultValue";
        String value = "value";

        ContextEntity contextEntity = new ContextEntity();
        ContextTypeEntity contextType = new ContextTypeEntity();
        contextType.setName("Global");
        contextEntity.setContextType(contextType);
        ResourceEditProperty resourceEditPropertyMock = mock(ResourceEditProperty.class);
        Mockito.when(resourceEditPropertyMock.getDefaultValue()).thenReturn(defaultValue);
        Mockito.when(resourceEditPropertyMock.getPropertyValue()).thenReturn(value);

        //when
        service.verifyDefaultPropertyCanBeSet(resourceEditPropertyMock, contextEntity);

        // then
        assertTrue(true);
    }

    @Test(expected = ValidationException.class)
    public void verifyDefaultPropertyCanBeSetWhenValueEqualsDefaultValueWhenNoParentContextSetShouldThrowException() throws ValidationException {
        // given
        String defaultValue = "defaultValue";

        ContextEntity contextEntity = new ContextEntity();
        ContextTypeEntity contextType = new ContextTypeEntity();
        contextType.setName("Env");
        contextEntity.setContextType(contextType);
        ResourceEditProperty resourceEditPropertyMock = mock(ResourceEditProperty.class);
        Mockito.when(resourceEditPropertyMock.getDefaultValue()).thenReturn(defaultValue);
        Mockito.when(resourceEditPropertyMock.getPropertyValue()).thenReturn(defaultValue);
        Mockito.when(resourceEditPropertyMock.getParent()).thenReturn(null);
        Mockito.when(resourceEditPropertyMock.getOriginalValue()).thenReturn(null);

        //when
        service.verifyDefaultPropertyCanBeSet(resourceEditPropertyMock, contextEntity);
    }

    @Test
    public void verifyDefaultPropertyCanBeSetWhenValueEqualsDefaultValueWhenParentContextSetShouldNotThrowException() throws ValidationException {
        // given
        String defaultValue = "defaultValue";

        ContextEntity contextEntity = new ContextEntity();
        ContextTypeEntity contextType = new ContextTypeEntity();
        contextType.setName("Env");
        contextEntity.setContextType(contextType);
        ResourceEditProperty resourceEditPropertyMock = mock(ResourceEditProperty.class);
        Mockito.when(resourceEditPropertyMock.getDefaultValue()).thenReturn(defaultValue);
        Mockito.when(resourceEditPropertyMock.getPropertyValue()).thenReturn(defaultValue);
        Mockito.when(resourceEditPropertyMock.getParent()).thenReturn(mock(ResourceEditProperty.class));

        //when
        service.verifyDefaultPropertyCanBeSet(resourceEditPropertyMock, contextEntity);

        // then
        assertTrue(true);
    }

    @Test
    public void verifyDefaultPropertyCanBeSetWhenValueEqualsDefaultValueWhenNoParentContextButOriginalValueSetShouldNotThrowException() throws ValidationException {
        // given
        String defaultValue = "defaultValue";

        ContextEntity contextEntity = new ContextEntity();
        ContextTypeEntity contextType = new ContextTypeEntity();
        contextType.setName("Env");
        contextEntity.setContextType(contextType);
        ResourceEditProperty resourceEditPropertyMock = mock(ResourceEditProperty.class);
        Mockito.when(resourceEditPropertyMock.getDefaultValue()).thenReturn(defaultValue);
        Mockito.when(resourceEditPropertyMock.getPropertyValue()).thenReturn(defaultValue);
        Mockito.when(resourceEditPropertyMock.getParent()).thenReturn(null);
        Mockito.when(resourceEditPropertyMock.getOriginalValue()).thenReturn("someOriginalValue");

        //when
        service.verifyDefaultPropertyCanBeSet(resourceEditPropertyMock, contextEntity);

        // then
        assertTrue(true);
    }

    @Test
    public void shouldNotStoreAnythingInThreadLocalWhenNoPropertyBeenHasChanged() throws ValidationException {
        // given
        ContextEntity contextEntity = new ContextEntity();
        ResourceEntity resourceEntityMock = mock(ResourceEntity.class);

        // when
        service.saveProperties(contextEntity, resourceEntityMock, Collections.EMPTY_LIST);

        // then
        assertThat(ThreadLocalUtil.getThreadVariable(ThreadLocalUtil.KEY_RESOURCE_ID), is(CoreMatchers.nullValue()));
    }

    @Test
    public void shouldStoreResourceIdInThreadLocalWhenPropertyHasBeenChanged() throws ValidationException {
        // given
        ContextEntity contextEntity = new ContextEntity();
        contextEntity.setId(2);
        ResourceEntity resourceEntityMock = mock(ResourceEntity.class);
        int resourceId = 22;
        ResourceEditProperty changedProperty = new ResourceEditPropertyBuilder()
                .withDisplayAndTechKeyName("Memory")
                .withValue("100")
                .withDescriptorId(123)
                .build();
        changedProperty.setPropertyValue("101");
        List<ResourceEditProperty> resourceProperties = Arrays.asList(changedProperty);
        when(resourceEntityMock.getId()).thenReturn(resourceId);
        doNothing().when(service).setPropertyValue(Mockito.<ContextDependency>any(), eq(changedProperty.getDescriptorId()), eq(changedProperty.getPropertyValue()));

        // when
        service.saveProperties(contextEntity, resourceEntityMock, resourceProperties);

        // then
        assertThat((Integer) ThreadLocalUtil.getThreadVariable(ThreadLocalUtil.KEY_RESOURCE_ID), is(resourceId));
    }

    @Test
    public void shouldStoreResourceIdInThreadLocalWhenPropertyHasBeenRemoved() throws ValidationException {
        // given
        ContextEntity contextEntity = new ContextEntity();
        contextEntity.setId(2);
        ResourceEntity resourceEntityMock = mock(ResourceEntity.class);
        int resourceId = 22;
        when(resourceEntityMock.getId()).thenReturn(resourceId);
        ResourceEditProperty changedProperty = new ResourceEditPropertyBuilder()
                .withDisplayAndTechKeyName("Memory")
                .withValue("100")
                .withDescriptorId(123)
                .build();
        changedProperty.setReset(true);
        List<ResourceEditProperty> resourceProperties = Arrays.asList(changedProperty);
        doNothing().when(service).resetPropertyValue(Mockito.<ContextDependency>any(), eq(changedProperty.getDescriptorId()));

        // when
        service.saveProperties(contextEntity, resourceEntityMock, resourceProperties);

        // then
        assertThat((Integer) ThreadLocalUtil.getThreadVariable(ThreadLocalUtil.KEY_RESOURCE_ID), is(resourceId));
    }



}