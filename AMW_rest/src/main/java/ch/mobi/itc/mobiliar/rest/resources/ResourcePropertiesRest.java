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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import ch.mobi.itc.mobiliar.rest.dtos.PropertyDTO;
import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.property.boundary.PropertyEditor;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditProperty;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

@RequestScoped
@Path("/resources/{resourceGroupName}/{releaseName}/properties")
@Api(value = "/resources/{resourceGroupName}/{releaseName}/properties", description = "Resource properties")
public class ResourcePropertiesRest {

    @PathParam("resourceGroupName")
    String resourceGroupName;

    @PathParam("releaseName")
    String releaseName;

    @Inject
    PropertyEditor propertyEditor;

    @Inject
    ResourceLocator resourceLocator;

    @Inject
    ContextLocator contextLocator;

    @GET
    @ApiOperation(value = "Get all properties for a resource in a specific release")
    public Response getResourceProperties(@DefaultValue("Global") @QueryParam("env") String environment) throws ValidationException {
        List<PropertyDTO> resourceProperties = getResourceProperties(resourceGroupName, releaseName, environment);
        if (resourceProperties.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(resourceProperties).build();
    }

    List<PropertyDTO> getResourceProperties(String resourceGroupName, String releaseName, String environment) throws ValidationException {
        ResourceEntity resource = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName, releaseName);
        List<PropertyDTO> result = new ArrayList<>();
        if (resource != null) {
            ContextEntity context = contextLocator.getContextByName(environment);
            List<ResourceEditProperty> properties = propertyEditor.getPropertiesForResource(resource.getId(),
                    context.getId());
            for (ResourceEditProperty property : properties) {
                result.add(new PropertyDTO(property, context.getName()));
            }
        }
        return result;
    }

    @Path("/{propertyName}")
    @GET
    @ApiOperation(value = "Get the property including its value for a resource in a specific release")
    public Response getResourcePropertyValueForEnvironment(@PathParam("propertyName") String propertyName, @DefaultValue("Global") @QueryParam("env") String environment) throws ValidationException {
        ResourceEntity resource = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName, releaseName);
        if (resource != null) {
            ContextEntity context = contextLocator.getContextByName(environment);
            List<ResourceEditProperty> properties = propertyEditor.getPropertiesForResource(resource.getId(),
                    context.getId());
            for (ResourceEditProperty property : properties) {
                if (property.getTechnicalKey().equals(propertyName)) {
                    return Response.ok(new PropertyDTO(property, context.getName())).build();
                }
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Path("/{propertyName}")
    @PUT
    @Consumes("text/plain")
    @ApiOperation(value = "Set the value of an existing property on a resource in a specific release")
    public Response updateResourceProperty(@ApiParam("the new value of the property") String value, @PathParam("propertyName") String propertyName,  @DefaultValue("Global")  @QueryParam("env")  String environment) throws ValidationException {
        propertyEditor.setPropertyValueOnResourceForContext(resourceGroupName, releaseName, environment, propertyName, value);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/{propertyName}")
    @DELETE
    @ApiOperation(value = "Reset the value of the given property in the specified context to null")
    public Response resetResourceProperty(@PathParam("propertyName") String propertyName,  @DefaultValue("Global")  @QueryParam("env")  String environment) throws ValidationException {
        propertyEditor.resetPropertyValueOnResourceForContext(resourceGroupName, releaseName, environment, propertyName);
        return Response.status(Response.Status.OK).build();
    }

}
