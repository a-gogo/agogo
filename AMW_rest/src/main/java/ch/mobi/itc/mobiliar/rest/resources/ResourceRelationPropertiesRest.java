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

import ch.mobi.itc.mobiliar.rest.dtos.BatchPropertyDTO;
import ch.mobi.itc.mobiliar.rest.dtos.PropertyDTO;
import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.property.boundary.PropertyEditor;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditProperty;
import ch.puzzle.itc.mobiliar.business.resourcerelation.boundary.ResourceRelationLocator;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RequestScoped
@Path("/resources/{resourceGroupName}/{releaseName}/relations/{relatedResourceGroupName}/{relatedReleaseName}/properties")
@Api(value = "/resources/{resourceGroupName}/{releaseName}/relations/{relatedResourceGroupName}/{relatedReleaseName}/properties", description = "Resource relation properties")
public class ResourceRelationPropertiesRest {

    @PathParam("resourceGroupName")
    String resourceGroupName;

    @PathParam("releaseName")
    String releaseName;

    @PathParam("relatedResourceGroupName")
    String relatedResourceGroupName;

    @PathParam("relatedReleaseName")
    String relatedReleaseName;

    @Inject
    PropertyEditor propertyEditor;

    @Inject
    ContextLocator contextLocator;

    @Inject
    ResourceRelationLocator resourceRelationLocator;

    @GET
    @ApiOperation(value = "Get all properties of the relation between the two resource releases")
    public Response getResourceRelationProperties(@DefaultValue("Global") @QueryParam("env") String environment) throws ValidationException {
        List<ConsumedResourceRelationEntity> relations = resourceRelationLocator.getResourceRelationList(resourceGroupName, releaseName, relatedResourceGroupName, relatedReleaseName);
        if (relations.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        ContextEntity context = contextLocator.getContextByName(environment);
        List<ResourceEditProperty> properties = new ArrayList<>();
        for (ConsumedResourceRelationEntity relation : relations) {
            List<ResourceEditProperty> temp = propertyEditor.getPropertiesForRelatedResource(relation,
                    context.getId());
            properties.addAll(temp);
        }
        List<PropertyDTO> propertyDTOS = new ArrayList<>();
        for (ResourceEditProperty property : properties) {
            propertyDTOS.add(new PropertyDTO(property, context.getName()));
        }
        return Response.ok(propertyDTOS).build();
    }

    /**
     * Für JavaBatch Monitor: liest alle Properties zu allen standardJobs einer Batch Applikation
     *
     * @param environment
     * @return
     * @throws ValidationException
     */
    @Path("/batch")
    @GET
    @ApiOperation(value = "Get all batch properties")
    public List<BatchPropertyDTO> getResourceRelationBatchProperties(@DefaultValue("Global")
                                                                     @ApiParam("the environment - if not set, this falls back to global") @QueryParam("env") String environment)
            throws ValidationException {

        List<ConsumedResourceRelationEntity> relations = resourceRelationLocator
                .getResourceRelationList(resourceGroupName, releaseName, relatedResourceGroupName, relatedReleaseName);

        List<BatchPropertyDTO> result = new ArrayList<BatchPropertyDTO>();

        ContextEntity context = contextLocator.getContextByName(environment);

        for (ConsumedResourceRelationEntity relation : relations) {

            List<ResourceEditProperty> properties = propertyEditor.getPropertiesForRelatedResource(relation,
                    context.getId());
            List<PropertyDTO> props = new ArrayList<>();
            for (ResourceEditProperty property : properties) {
                props.add(new PropertyDTO(property, context.getName()));
            }
            BatchPropertyDTO job = new BatchPropertyDTO(relation.getMasterResourceName(), relation.buildIdentifer(),
                    props);
            result.add(job);
        }

        return result;
    }


    //TODO Yves/Lorenz: Performance; ablösen durch mengenwertige neue Methode mit neuem Query

    /**
     * Für JavaBatch Monitor: liest ein einzelnes Property (Jobname) zu allen standardJobs einer Batch-Applikation
     *
     * @param propertyName
     * @param environment
     * @return
     * @throws ValidationException
     */
    @Path("/batch/{propertyName}")
    @GET
    @ApiOperation(value = "Get a batchjob property")
    public List<BatchPropertyDTO> getResourceRelationBatchProperty(@PathParam("propertyName") String propertyName,
                                                                   @ApiParam("the environment - if not set, this falls back to global") @DefaultValue("Global") @QueryParam("env") String environment)
            throws ValidationException {

        List<ConsumedResourceRelationEntity> relations = resourceRelationLocator
                .getResourceRelationList(resourceGroupName, releaseName, relatedResourceGroupName, relatedReleaseName);

        List<BatchPropertyDTO> result = new ArrayList<BatchPropertyDTO>();

        ContextEntity context = contextLocator.getContextByName(environment);

        for (ConsumedResourceRelationEntity relation : relations) {

            List<ResourceEditProperty> properties = propertyEditor.getPropertiesForRelatedResource(relation,
                    context.getId());
            List<PropertyDTO> props = new ArrayList<>();
            for (ResourceEditProperty property : properties) {
                if (property.getTechnicalKey().equals(propertyName)) {
                    props.add(new PropertyDTO(property, context.getName()));
                }
            }
            BatchPropertyDTO job = new BatchPropertyDTO(relation.getMasterResourceName(), relation.buildIdentifer(),
                    props);
            result.add(job);
        }

        return result;
    }

    @Path("/{propertyName}")
    @GET
    @ApiOperation(value = "Get the given property of the relation between the two resource releases")
    public Response getResourceRelationProperty(@PathParam("propertyName") String propertyName, @DefaultValue("Global") @QueryParam("env") String environment) throws ValidationException {
        List<ConsumedResourceRelationEntity> relations = resourceRelationLocator.getResourceRelationList(resourceGroupName,
                releaseName, relatedResourceGroupName, relatedReleaseName);
        ContextEntity context = contextLocator.getContextByName(environment);
        List<ResourceEditProperty> properties = new ArrayList<>();
        for (ConsumedResourceRelationEntity relation : relations) {
            List<ResourceEditProperty> temp = propertyEditor.getPropertiesForRelatedResource(relation,
                    context.getId());
            properties.addAll(temp);
        }
        for (ResourceEditProperty property : properties) {
            if (property.getTechnicalKey().equals(propertyName)) {
                return Response.ok(new PropertyDTO(property, context.getName())).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Path("/{propertyName}")
    @PUT
    @Consumes("text/plain")
    @ApiOperation(value = "Set the value of the given property on the relation between the two resource releases")
    public Response updateResourceRelationProperty(@ApiParam("the new value of the property") String value, @PathParam("propertyName") String propertyName, @DefaultValue("Global") @QueryParam("env") String environment) throws ValidationException {
        propertyEditor.setPropertyValueOnResourceRelationForContext(resourceGroupName, releaseName, relatedResourceGroupName, relatedReleaseName, environment, propertyName, value);
        return Response.status(Response.Status.OK).build();
    }

    @Path("/{propertyName}")
    @DELETE
    @ApiOperation(value = "Reset the value of the given property in the specified context to null")
    public Response resetResourceRelationProperty(@PathParam("propertyName") String propertyName, @DefaultValue("Global") @QueryParam("env") String environment) throws ValidationException {
        propertyEditor.resetPropertyValueOnResourceRelationForContext(resourceGroupName, releaseName, relatedResourceGroupName, relatedReleaseName, environment, propertyName);
        return Response.status(Response.Status.OK).build();
    }

}