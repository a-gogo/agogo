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
import java.util.HashSet;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import ch.mobi.itc.mobiliar.rest.dtos.TemplateDTO;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceGroupLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.template.boundary.TemplateEditor;
import ch.puzzle.itc.mobiliar.business.template.control.TemplatesScreenDomainService;
import ch.puzzle.itc.mobiliar.business.template.entity.TemplateDescriptorEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.NotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.TemplateNotDeletableException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Rest boundary for Resource-Templates
 */
@RequestScoped
@Path("/resources/{resourceGroupName}/{releaseName}/templates")
@Api(value = "/resources/{resourceGroupName}/{releaseName}/templates", description = "Resource templates")
public class ResourceTemplatesRest {;

    @Inject
    ResourceGroupLocator resourceGroupLocator;

    @Inject
    TemplatesScreenDomainService templateService;

    @Inject
    TemplateEditor templateEditor;

    @GET
    @ApiOperation(value = "Get all templates for a resource in a specific release")
    public List<TemplateDTO> getResourceTemplates(@PathParam("resourceGroupName") String resourceGroupName,
                                                @PathParam("releaseName") String releaseName) throws ValidationException {
        List<TemplateDTO> templateDTOs = new ArrayList<>();
        List<TemplateDescriptorEntity> templates = templateService.getGlobalTemplateDescriptorsForResource(resourceGroupName, releaseName, false);

        for (TemplateDescriptorEntity template : templates) {
            TemplateDTO temp = new TemplateDTO(template);
            templateDTOs.add(temp);
        }
        return templateDTOs;
    }

    @GET
    @Path("/{templateName}")
    @ApiOperation(value = "Get a template for a resource in a specific release")
    public TemplateDTO getResourceTemplate(@PathParam("resourceGroupName") String resourceGroupName,
                                                 @PathParam("releaseName") String releaseName,
                                                 @PathParam("templateName") String templateName) throws ValidationException, NotFoundException {
        TemplateDescriptorEntity template = getTemplate(resourceGroupName, releaseName, templateName);
        if (template == null) {
            throw new NotFoundException("Template not found");
        }
        return new TemplateDTO(template);
    }

    @DELETE
    @Path("/{templateName}")
    @ApiOperation(value = "Delete a template for a resource in a specific release")
    public Response deleteResourceTemplate(@PathParam("resourceGroupName") String resourceGroupName,
                                           @PathParam("releaseName") String releaseName,
                                           @PathParam("templateName") String templateName) throws ValidationException, AMWException {
        try {
            templateEditor.removeTemplate(resourceGroupName, releaseName, templateName, false);
        } catch (TemplateNotDeletableException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @ApiOperation(value = "Create a template for a resource in a specific release")
    public Response createResourceTemplates(@PathParam("resourceGroupName") String resourceGroupName,
                                            @PathParam("releaseName") String releaseName,
                                            TemplateDTO templateDTO) throws ValidationException, AMWException {
        // make sure id isn't set or save will try to update the template
        templateDTO.setId(null);
        TemplateDescriptorEntity template = toTemplateDescriptorEntity(templateDTO, null);

        templateEditor.saveTemplateForResource(template, resourceGroupName, releaseName, false);
        return Response.ok(new TemplateDTO(template)).build();
    }

    @PUT
    @Path("/{templateName}")
    @ApiOperation(value = "Update a template for a resource in a specific release")
    public Response updateResourceTemplates(@PathParam("resourceGroupName") String resourceGroupName,
                                            @PathParam("releaseName") String releaseName,
                                            @PathParam("templateName") String templateName,
                                            TemplateDTO templateDTO) throws ValidationException, AMWException {
        TemplateDescriptorEntity template = getTemplate(resourceGroupName, releaseName, templateName);
        if (template == null) {
            throw new NotFoundException("Template not found");
        }
        template = toTemplateDescriptorEntity(templateDTO, template);

        templateEditor.saveTemplateForResource(template, resourceGroupName, releaseName, false);
        return Response.ok(new TemplateDTO(template)).build();
    }

    private TemplateDescriptorEntity getTemplate(String resourceGroupName, String releaseName, String templateName) throws ValidationException {
        List<TemplateDescriptorEntity> templates = templateService.getGlobalTemplateDescriptorsForResource(resourceGroupName, releaseName, false);
        for (TemplateDescriptorEntity template : templates) {
            if (templateName.equals(template.getName())) {
                return template;
            }
        }
        return null;
    }

    private TemplateDescriptorEntity toTemplateDescriptorEntity(TemplateDTO templateDTO, TemplateDescriptorEntity template) {
        if (template == null) {
            template = new TemplateDescriptorEntity();
        }
        template.setId(templateDTO.getId());
        template.setTesting(false);
        template.setFileContent(templateDTO.getFileContent());
        template.setName(templateDTO.getName());
        template.setTargetPath(templateDTO.getTargetPath());
        HashSet<ResourceGroupEntity> targetPlatforms = new HashSet<>();
        if (templateDTO.getTargetPlatforms() != null) {
            for(String platform : templateDTO.getTargetPlatforms()) {
                ResourceGroupEntity platformEntity = resourceGroupLocator.getResourceGroupByName(platform);
                targetPlatforms.add(platformEntity);
            }
        }
        template.setTargetPlatforms(targetPlatforms);

        return template;
    }
}
