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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.ws.rs.core.Response;

import ch.puzzle.itc.mobiliar.business.resourcerelation.boundary.ResourceRelationLocator;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * This is a typical rest interface test: It checks, if the underlying business logic is called with
 * appropriate (expected) parameters but not the business logic itself (this is the duty of the business
 * method test).
 */
public class ResourceRelationPropertiesRestTest {

    @InjectMocks
    ResourceRelationPropertiesRest rest;

    @Mock
    ResourceRelationLocator resourceRelationLocator;

    @Before
    public void configure() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void shouldReturnNotFoundStatusIfNoRelationsHaveBeenFound() throws ValidationException {
        // given
        int notFound = Response.Status.NOT_FOUND.getStatusCode();

        // when
        Response response = rest.getResourceRelationProperties("Test");

        // then
        assertThat(response.getStatus(), is(notFound));
    }

    // TODO #9781 implement test - original tests were written in ResourceRestTest

    //	/**
//	 * Ensures, that #setSingleProperty is called correctly - even though there are multiple properties
//	 * available on a consumed resource relation.
//	 *
//	 * @throws Exception
//	 */
//	@Test
//	public void testUpdateResourceRelationProperty() throws Exception {
//		// given
//		ResourcesRest spy = Mockito.spy(rest);
//		ConsumedResourceRelationEntity consumedResourceRelationEntity = Mockito
//				.mock(ConsumedResourceRelationEntity.class);
//		ContextEntity context = Mockito.mock(ContextEntity.class);
//
//          prepareMockForResourceRelation(spy, context, consumedResourceRelationEntity);
//
//		// when
//          Response response = spy
//                   .updateResourceRelationProperty("newValue", "amw", "Past", "Node_01", "Past", "hostName",
//                             "B");
//
//          // then
//          assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
//		Mockito.verify(propertyEditor).setSingleProperty(consumedResourceRelationEntity, context, 1,
//				"newValue", "hostNameComment");
//
//		Mockito.verify(propertyEditor, Mockito.never()).setSingleProperty(
//				Mockito.eq(consumedResourceRelationEntity), Mockito.eq(context), Mockito.anyInt(),
//				Mockito.eq("anotherProperty"), Mockito.anyString());
//
//	}

    //    @Test
//    public void shouldReturnNotFoundStatus_updateResourceRelationProperty_withIllegalPropertyName() throws Exception {
//        // given
//        ResourcesRest spy = Mockito.spy(rest);
//        ConsumedResourceRelationEntity consumedResourceRelationEntity = Mockito
//                  .mock(ConsumedResourceRelationEntity.class);
//        ContextEntity context = Mockito.mock(ContextEntity.class);
//        prepareMockForResourceRelation(spy, context, consumedResourceRelationEntity);
//
//        // when
//        Response response = spy.updateResourceRelationProperty("newValue", "amw", "Past", "Node_01", "Past",
//                  "blabla", "B");
//
//        // then
//        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
//    }

    //    @Test
//    public void shouldReturnNotFoundStatus_updateResourceRelationProperty_withEmptyPropertyList() throws Exception {
//        // given
//        ResourcesRest spy = Mockito.spy(rest);
//        ConsumedResourceRelationEntity consumedResourceRelationEntity = Mockito
//                  .mock(ConsumedResourceRelationEntity.class);
//        ContextEntity context = Mockito.mock(ContextEntity.class);
//        Mockito.doReturn(consumedResourceRelationEntity).when(spy).getResourceRelationInternal(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//        Mockito.doReturn(context).when(spy).getContextByName(Mockito.anyString());
//        Mockito.when(propertyEditor.getPropertiesForRelatedResource(
//                            Mockito.any(ConsumedResourceRelationEntity.class), Mockito.anyInt()))
//                  .thenReturn(Collections.<ResourceEditProperty>emptyList());
//        // when
//        Response response = spy.updateResourceRelationProperty("newValue", "amw", "Past", "Node_01", "Past", "hostName", "B");
//
//        // then
//        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
//    }

    //    /**
//     * Ensures, that #resetSingleProperty is called correctly - even though there are multiple properties
//     * available on a consumed resource relation.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void testResetResourceRelationProperty() throws Exception {
//        // given
//        ResourcesRest spy = Mockito.spy(rest);
//        ConsumedResourceRelationEntity consumedResourceRelationEntity = Mockito
//                  .mock(ConsumedResourceRelationEntity.class);
//        ContextEntity context = Mockito.mock(ContextEntity.class);
//
//        prepareMockForResourceRelation(spy, context, consumedResourceRelationEntity);
//
//        // when
//        spy.resetResourceRelationProperty("amw", "Past", "Node_01", "Past", "hostName", "B");
//
//        // then
//        Mockito.verify(propertyEditor).resetSingleProperty(consumedResourceRelationEntity, context, 1);
//
//        Mockito.verify(propertyEditor, Mockito.never()).resetSingleProperty(
//                  Mockito.eq(consumedResourceRelationEntity), Mockito.eq(context), Mockito.eq(2));
//
//    }

    //    /**
//     * Prepares the given spied ResourcesRest object for testing.
//     */
//    private void prepareMockForResourceRelation(ResourcesRest spy, ContextEntity context,
//              ConsumedResourceRelationEntity consumedResourceRelationEntity){
//        ResourceEditProperty property1 = Mockito.mock(ResourceEditProperty.class);
//        Mockito.when(property1.getPropertyName()).thenReturn("hostName");
//        Mockito.when(property1.getDescriptorId()).thenReturn(1);
//        Mockito.when(property1.getPropertyValueComment()).thenReturn("hostNameComment");
//
//        ResourceEditProperty property2 = Mockito.mock(ResourceEditProperty.class);
//        Mockito.when(property2.getPropertyName()).thenReturn("anotherProperty");
//        Mockito.when(property2.getDescriptorId()).thenReturn(2);
//
//        Mockito.doReturn(consumedResourceRelationEntity)
//                  .when(spy)
//                  .getResourceRelationInternal(Mockito.anyString(), Mockito.anyString(),
//                            Mockito.anyString(), Mockito.anyString());
//        Mockito.doReturn(context).when(spy).getContextByName(Mockito.anyString());
//        Mockito.when(
//                  propertyEditor.getPropertiesForRelatedResource(
//                            Mockito.any(ConsumedResourceRelationEntity.class), Mockito.anyInt()))
//                  .thenReturn(Arrays.asList(property1, property2));
//    }

}