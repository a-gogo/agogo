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

package ch.puzzle.itc.mobiliar.presentation.deploy;

import ch.puzzle.itc.mobiliar.business.deploy.boundary.DeploymentBoundary;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.deploymentparameter.boundary.DeploymentParameterBoundary;
import ch.puzzle.itc.mobiliar.business.deploymentparameter.entity.DeploymentParameter;
import ch.puzzle.itc.mobiliar.business.deploymentparameter.entity.Key;
import ch.puzzle.itc.mobiliar.business.environment.boundary.ContextLocator;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.releasing.control.ReleaseMgmtService;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceGroupLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.common.util.ContextNames;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.presentation.util.UserSettings;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.logging.Logger;

@Named
@RequestScoped
public class CreateDeploymentController {


    @Inject
    private DeploymentBoundary deploymentBoundary;

    @Inject
    private ContextLocator contextLocator;

    @Inject
    private ResourceDependencyResolverService dependencyResolverService;

    @Inject
    private UserSettings userSettings;

    @Inject
    private ResourceGroupLocator resourceGroupLocator;

    @Inject
    private ReleaseMgmtService releaseMgmtService;

    @Inject
    protected Logger log;

    @Inject
    protected DeploymentParameterBoundary deploymentParameterBoundary;

    public List<ContextEntity> loadEnvironments() {
        List<ContextEntity> env = new ArrayList<>();
        for (ContextEntity c : contextLocator.getAllEnvironments()) {
            if (c.getContextType().getName().equals(ContextNames.ENV.name())) {
                env.add(c);
            }
        }
        return Collections.unmodifiableList(env);
    }

    public Map<String, List<ContextEntity>> loadEnvironmentsPerDomain() {
        Map<String, List<ContextEntity>> envMap = new TreeMap<>();
        for (ContextEntity c : contextLocator.getAllEnvironments()) {
            if (c.isEnvironment()) {
                ContextEntity parent = c.getParent();

                if (envMap.containsKey(parent.getName())){
                    envMap.get(parent.getName()).add(c);
                } else {
                    List<ContextEntity> envs = new ArrayList<>();
                    envs.add(c);
                    envMap.put(parent.getName(), envs);
                }
            }
        }
        return envMap;
    }

    public ResourceEntity loadApplicationserverForRelease(ResourceGroupEntity selectedApplicationServerGroup, ReleaseEntity selectedRelease) {
        return dependencyResolverService.getResourceEntityForRelease(selectedApplicationServerGroup, selectedRelease);
    }

    public List<ResourceGroupEntity> loadAppServerGroups() {
        return resourceGroupLocator.getGroupsForType(
                DefaultResourceTypeDefinition.APPLICATIONSERVER.name(),
                userSettings.getMyAMWFilter(), false, true);
    }

    public List<Key> loadAllParameterKeys() {
        return deploymentParameterBoundary.findAllKeys();
    }

    public List<DeploymentEntity.ApplicationWithVersion> getAppsWithVersion(ResourceEntity appServer, List<String> contexts, ReleaseEntity release) {
        List<DeploymentEntity.ApplicationWithVersion> apps = deploymentBoundary.getVersions(appServer, stringToIntegerList(contexts), release);

        Collections.sort(apps, new Comparator<DeploymentEntity.ApplicationWithVersion>() {
            @Override
            public int compare(DeploymentEntity.ApplicationWithVersion app1, DeploymentEntity.ApplicationWithVersion app2) {
                return app1.getApplicationName().toLowerCase().
                        compareTo(app2.getApplicationName().toLowerCase());
            }
        });

        return apps;
    }


    private List<Integer> stringToIntegerList(List<String> list) {
        List<Integer> result = new ArrayList<>();
        if (list != null) {
            for (String s : list) {
                result.add(Integer.parseInt(s));
            }
        }
        return result;
    }

    public ResourceGroupEntity getResourceGroupWithResourceRelations(int groupId) {
        return resourceGroupLocator.getResourceGroupForCreateDeploy(groupId);
    }

    public List<ReleaseEntity> getReleasesForApppserver(ResourceGroupEntity appServer) {
        return releaseMgmtService.getDeployableReleasesForResourceGroup(appServer);
    }


    public Integer createDeploymentReturnTrackingId(Integer appServerGroupId, Integer releaseId,
                                                    List<String> contextIds, Date stateDate,
                                                    Date executionDate,
                                                    List<DeploymentEntity.ApplicationWithVersion> appsWithVersion,
                                                    List<DeploymentParameter> deployParams,
                                                    boolean sendEmail, boolean requestOnly,
                                                    boolean doSimulate, boolean doExecuteShakedownTest,
                                                    boolean doNeighbourhoodTest) {
        return deploymentBoundary.createDeploymentReturnTrackingId(appServerGroupId, releaseId,
                executionDate, stateDate, stringToIntegerList(contextIds),
                appsWithVersion, deployParams, sendEmail,
                requestOnly, doSimulate,
                doExecuteShakedownTest,
                doNeighbourhoodTest);
    }

    public Integer createDeploymentReturnTrackingId(List<DeploymentEntity> selectedDeployments,
                                                    List<String> contextIds, Date stateDate,
                                                    Date executionDate,
                                                    List<DeploymentParameter> deployParams,
                                                    boolean sendEmail, boolean requestOnly,
                                                    boolean doSimulate, boolean doExecuteShakedownTest,
                                                    boolean doNeighbourhoodTest) {
        return deploymentBoundary
                .createDeploymentsReturnTrackingId(selectedDeployments,
                        executionDate, stateDate, deployParams, stringToIntegerList(contextIds),
                        sendEmail,
                        requestOnly, doSimulate,
                        doExecuteShakedownTest,
                        doNeighbourhoodTest);
    }

    public List<DeploymentEntity> loadNewestDeploymentsPerAppserver(String... ids) {
        List<DeploymentEntity> deployments = new ArrayList<>();
        List<String> idsAsString = ids != null ? Arrays.asList(ids) : new ArrayList<String>();
        for (String idString : idsAsString) {
            deployments.add(deploymentBoundary.getDeploymentById(Integer.valueOf(idString)));
        }
        return filterNewestDeploymentForDuplicateAppServerDeployment(deployments);
    }

    private List<DeploymentEntity> filterNewestDeploymentForDuplicateAppServerDeployment(List<DeploymentEntity> selectedDeployments) {
        Map<String, DeploymentEntity> filterMap = new TreeMap<>();
        for (DeploymentEntity deployment : selectedDeployments) {
            String deploymentKey = deployment.getResourceGroup().getName();
            if (filterMap.containsKey(deploymentKey)) {
                DeploymentEntity lastDeployment = filterMap.get(deploymentKey);
                if (lastDeployment.getDeploymentDate().before(deployment.getDeploymentDate())) {
                    filterMap.put(deploymentKey, deployment);
                }
            } else {
                filterMap.put(deploymentKey, deployment);
            }
        }

        return new ArrayList<>(filterMap.values());
    }
}
