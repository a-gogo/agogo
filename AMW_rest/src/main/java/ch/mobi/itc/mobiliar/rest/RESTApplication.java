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

package ch.mobi.itc.mobiliar.rest;

import ch.mobi.itc.mobiliar.rest.Analyze.TestGenerationRest;
import ch.mobi.itc.mobiliar.rest.auditview.AuditViewRest;
import ch.mobi.itc.mobiliar.rest.deployments.DeploymentDtoCsvBodyWriter;
import ch.mobi.itc.mobiliar.rest.deployments.DeploymentsRest;
import ch.mobi.itc.mobiliar.rest.environments.EnvironmentsRest;
import ch.mobi.itc.mobiliar.rest.exceptions.*;
import ch.mobi.itc.mobiliar.rest.health.HealthCheck;
import ch.mobi.itc.mobiliar.rest.permissions.RestrictionsRest;
import ch.mobi.itc.mobiliar.rest.resources.*;
import ch.mobi.itc.mobiliar.rest.releases.ReleasesRest;
import ch.mobi.itc.mobiliar.rest.settings.SettingsRest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

@ApplicationPath("/resources/")
public class RESTApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        resources.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);
        resources.add(io.swagger.jaxrs.listing.ApiListingResource.class);
        addRestResourceClasses(resources);
        return resources;
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(ReleasesRest.class);
        resources.add(ResourcesRest.class);
        resources.add(ResourcePropertiesRest.class);
        resources.add(ResourceTemplatesRest.class);
        resources.add(ResourceRelationsRest.class);
        resources.add(ResourceRelationPropertiesRest.class);
        resources.add(ResourceRelationTemplatesRest.class);
        resources.add(HostNamesRest.class);
        resources.add(DeploymentsRest.class);
        resources.add(DeploymentDtoCsvBodyWriter.class);
        resources.add(EnvironmentsRest.class);
        resources.add(AuditViewRest.class);
        resources.add(RestrictionsRest.class);
        resources.add(ClientErrorExceptionMapper.class);
        resources.add(EJBExceptionMapper.class);
        resources.add(EJBTransactionRolledbackExceptionMapper.class);
        resources.add(ExceptionDtoBodyWriter.class);
        resources.add(IllegalStateExceptionMapper.class);
        resources.add(IllegalArgumentExceptionMapper.class);
        resources.add(IOExceptionMapper.class);
        resources.add(NoResultExceptionMapper.class);
        resources.add(NotAuthorizedExceptionMapper.class);
        resources.add(ValidationExceptionMapper.class);
        resources.add(NotFoundExceptionMapper.class);
        resources.add(UncaughtExceptionMapper.class);
        resources.add(ServerTupleCSVBodyWriter.class);
        resources.add(HealthCheck.class);
        resources.add(SettingsRest.class);
        resources.add(TestGenerationRest.class);
        resources.add(ObjectMapperConfig.class);
    }
}
