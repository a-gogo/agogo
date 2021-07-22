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

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
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

import ch.puzzle.itc.mobiliar.business.generator.control.extracted.templates.AppServerRelationsTemplateProcessor;
import ch.puzzle.itc.mobiliar.business.property.boundary.PropertyEditor;
import ch.puzzle.itc.mobiliar.business.server.boundary.ServerView;
import ch.puzzle.itc.mobiliar.business.server.entity.ServerTuple;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

@RequestScoped
@Path("/hostNames")
@Api(value = "/hostNames", description = "Hostnames")
public class HostNamesRest {

	@Inject
	PropertyEditor propertyEditor;
	@Inject
	private ServerView serverView;

	@PUT
	@Path("/{node}/{release}/{env}")
	@Consumes("text/plain")
	@ApiOperation(value = "Set hostname value on a given node in a specific node release and environment")
	public Response setHostNameOnNode(@ApiParam("node name") @PathParam("node") String nodeName,
			@ApiParam("release of node") @PathParam("release") String nodeRelease,
			@ApiParam("environment") @PathParam("env") String environmentName,
			@ApiParam("value of the hostname") String hostNameValue) throws ValidationException {
		
		String hostName = AppServerRelationsTemplateProcessor.HOST_NAME;
		propertyEditor.setPropertyValueOnResourceForContext(nodeName, nodeRelease, environmentName, hostName,
				hostNameValue);
		return Response.status(Response.Status.OK).build();
	}

	@PUT
	@Path("/{node}/{release}/{env}/{appServer}")
	@Consumes("text/plain")
	@ApiOperation(value = "Set hostname value on all relations between a node release and all releases of an application server where no hostname is defined yet.")
	public Response setHostNameBetweenAppServerAndNodesForAllReleasesWhereNotYetDefined(
			@ApiParam("node name") @PathParam("node") String nodeName,
			@ApiParam("release of node") @PathParam("release") String nodeRelease,
			@ApiParam("environment") @PathParam("env") String environmentName,
			@ApiParam("application server name") @PathParam("appServer") String appServerName,
			@ApiParam("value of the hostname") String hostNameValue) throws ValidationException {
		
		String hostName = AppServerRelationsTemplateProcessor.HOST_NAME;
		propertyEditor.setPropertyValueOnAllResourceRelationsForContextWhereNotYetSet(appServerName, nodeName, nodeRelease, environmentName, hostName, hostNameValue);
		return Response.status(Response.Status.OK).build();
	}

	@PUT
	@Path("/{node}/{release}/{env}/{appServer}/{asRelease}")
	@Consumes("text/plain")
	@ApiOperation(value = "Set hostname value on the relation between a node release and an application server release.")
	public Response setHostNameBetweenAppServerAndNodesForSpecifiedReleases(
			@ApiParam("node name") @PathParam("node") String nodeName,
			@ApiParam("release of node") @PathParam("release") String nodeRelease,
			@ApiParam("environment") @PathParam("env") String environmentName,
			@ApiParam("application server name") @PathParam("appServer") String appServerName,
			@ApiParam("application server release") @PathParam("asRelease") String appServerRelease,
			@ApiParam("value of the hostname") String hostNameValue) throws ValidationException {

		String hostName = AppServerRelationsTemplateProcessor.HOST_NAME;
		propertyEditor.setPropertyValueOnResourceRelationForContext(appServerName, appServerRelease, nodeName, nodeRelease, environmentName, hostName, hostNameValue);
		return Response.status(Response.Status.OK).build();
	}
	
	@GET
	@ApiOperation(value = "Get hostnames", notes = "Returns all hostnames matching the optional filter Query Params", produces = "application/json, text/csv")
	public List<ServerTuple> getHostNames(
			 @ApiParam("Application server name") @QueryParam("appServer") String appServer,
			 @ApiParam("Runtime name") @QueryParam("runtime") String runtime,
			 @ApiParam("Environement name") @QueryParam("environment") String environment,
			 @ApiParam("Host name") @QueryParam("host") String host,
			 @ApiParam("Node name") @QueryParam("node") String node,
			 @ApiParam("Merge releases") @QueryParam("disableMerge") @DefaultValue("false") boolean disableMerge) {

		return serverView.getServers(host, appServer, runtime, node, environment, !disableMerge);
	}

}
