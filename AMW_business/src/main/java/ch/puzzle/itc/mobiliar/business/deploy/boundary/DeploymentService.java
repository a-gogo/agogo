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

package ch.puzzle.itc.mobiliar.business.deploy.boundary;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.StringUtils;

import ch.puzzle.itc.mobiliar.business.database.control.SequencesService;
import ch.puzzle.itc.mobiliar.business.deploy.control.DeploymentNotificationService;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity.ApplicationWithVersion;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity.DeploymentState;
import ch.puzzle.itc.mobiliar.business.deploy.entity.NodeJobEntity;
import ch.puzzle.itc.mobiliar.business.deploy.entity.NodeJobEntity.NodeJobStatus;
import ch.puzzle.itc.mobiliar.business.deploy.event.DeploymentEvent;
import ch.puzzle.itc.mobiliar.business.deploy.event.DeploymentEvent.DeploymentEventType;
import ch.puzzle.itc.mobiliar.business.deploymentparameter.entity.DeploymentParameter;
import ch.puzzle.itc.mobiliar.business.domain.commons.CommonFilterService;
import ch.puzzle.itc.mobiliar.business.environment.control.ContextDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.generator.control.AMWTemplateExceptionHandler;
import ch.puzzle.itc.mobiliar.business.generator.control.GenerationResult;
import ch.puzzle.itc.mobiliar.business.generator.control.LockingService;
import ch.puzzle.itc.mobiliar.business.generator.control.TemplateUtils;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.GenerationModus;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.ResourceDependencyResolverService;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyEntity;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.ResourceEditService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.shakedown.control.ShakedownTestService;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.AMWRuntimeException;
import ch.puzzle.itc.mobiliar.common.exception.DeploymentStateException;
import ch.puzzle.itc.mobiliar.common.exception.MultipleVersionsForApplicationException;
import ch.puzzle.itc.mobiliar.common.exception.NotFoundExcption;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.util.ConfigurationService;
import ch.puzzle.itc.mobiliar.common.util.ConfigurationService.ConfigKey;
import ch.puzzle.itc.mobiliar.common.util.CustomFilter;
import ch.puzzle.itc.mobiliar.common.util.CustomFilter.ComperatorFilterOption;
import ch.puzzle.itc.mobiliar.common.util.CustomFilter.FilterType;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import ch.puzzle.itc.mobiliar.common.util.Tuple;

@Stateless
public class DeploymentService {

    private static final String DEPLOYMENT_ENTITY_NAME = "DeploymentEntity";
    private static final String DEPLOYMENT_QL_ALIAS = "d";
    private static final String RELEASE_QL = DEPLOYMENT_QL_ALIAS + ".release";
    private static final String GROUP_QL = DEPLOYMENT_QL_ALIAS + ".resourceGroup";
    private static final String ENV_QL = DEPLOYMENT_QL_ALIAS + ".context";
    private static final String TARGET_PLATFORM_QL = DEPLOYMENT_QL_ALIAS + ".runtime";
    private static final String PROPERTY_DESCRIPTOR_ENTITY_QL = "propDescEnt";

    public static final String DIFF_VERSIONS = "diff versions";
    public static final String NO_VERSION = "no version";

    public enum DeploymentOperationValidation {

        MISSING_PERMISSION, WRONG_STATE, SUCCESS;

        public boolean isPossible() {
            return DeploymentOperationValidation.SUCCESS.equals(this);
        }

    }

    public enum DeploymentFilterTypes {
        ID("Id", DEPLOYMENT_QL_ALIAS + ".id", FilterType.IntegerType),
        BUILD_SUCCESS("Build success", DEPLOYMENT_QL_ALIAS + ".buildSuccess", FilterType.booleanType),
        CONFIRMATION_DATE("Confirmed on", DEPLOYMENT_QL_ALIAS + ".deploymentConfirmationDate", FilterType.DateType),
        CONFIRMATION_USER("Confirmed by", DEPLOYMENT_QL_ALIAS + ".deploymentConfirmationUser", FilterType.StringType),
        DEPLOYMENT_CONFIRMED("Confirmed", DEPLOYMENT_QL_ALIAS + ".deploymentConfirmed", FilterType.booleanType),
        DEPLOYMENT_DATE("Deployment date", DEPLOYMENT_QL_ALIAS + ".deploymentDate", FilterType.DateType),
        REQUEST_USER("Requested by", DEPLOYMENT_QL_ALIAS + ".deploymentRequestUser", FilterType.StringType),
        JOB_CREATION_DATE("Created on", DEPLOYMENT_QL_ALIAS + ".deploymentJobCreationDate", FilterType.DateType),
        CANCEL_USER("Canceled by", DEPLOYMENT_QL_ALIAS + ".deploymentCancelUser", FilterType.StringType),
        CANCEL_DATE("Canceled on", DEPLOYMENT_QL_ALIAS + ".deploymentCancelDate", FilterType.DateType),
        DEPLOYMENT_STATE("State", DEPLOYMENT_QL_ALIAS + ".deploymentState", FilterType.ENUM_TYPE),
        DEPLOYMENT_MESSAGE("Message", DEPLOYMENT_QL_ALIAS + ".stateMessage", FilterType.StringType),
        ENVIRONMENT_NAME("Environment", ENV_QL + ".name", FilterType.StringType),
        APPSERVER_NAME("Application server", GROUP_QL + ".name", FilterType.StringType),
        //RELEASE("Release", RELEASE_QL + ".installationInProductionAt", FilterType.LabeledDateType),
        RELEASE("Release", RELEASE_QL + ".installationInProductionAt", FilterType.LabeledDateType),
        APPLICATION_NAME("Application", DEPLOYMENT_QL_ALIAS + ".applicationsWithVersion", FilterType.StringType),
        TARGETPLATFORM("Targetplatform", TARGET_PLATFORM_QL + ".name", FilterType.StringType),
        LASTDEPLOYJOBFORASENV("Latest deployment job for App Server and Env", "", FilterType.SpecialFilterType),
        TRACKING_ID("Tracking Id", DEPLOYMENT_QL_ALIAS + ".trackingId", FilterType.IntegerType),
        DEPLOYMENT_PARAMETER("Deployment parameter", "p.key", "join d.deploymentParameters p", FilterType.StringType),
        DEPLOYMENT_PARAMETER_VALUE("Deployment parameter value", "p.value", "join d.deploymentParameters p", FilterType.StringType);

        private final String filterDisplayName;
        private final String filterTabColName;
        private final String filterTableJoining;
        private final FilterType filterType;

        DeploymentFilterTypes(String filterDisplayName, String filterTabColName, FilterType filterType) {
            this(filterDisplayName, filterTabColName, "", filterType);
        }

        DeploymentFilterTypes(String filterDisplayName, String filterTabColName, String filterTableJoining,
                              FilterType filterType) {
            this.filterDisplayName = filterDisplayName;
            this.filterTabColName = filterTabColName;
            this.filterType = filterType;
            this.filterTableJoining = filterTableJoining;
        }

        public String getFilterDisplayName() {
            return filterDisplayName;
        }

        public String getFilterTabColumnName() {
            return filterTabColName;
        }

        public FilterType getFilterType() {
            return filterType;
        }

        public String getFilterTableJoining() {
            return filterTableJoining;
        }
    }

    @Inject
    private ResourceEditService resourceEditService;

    @Inject
    private DeploymentNotificationService deploymentNotificationService;

    @Inject
    private PermissionService permissionService;

    @Inject
    private ContextDomainService contextDomainService;

    @Inject
    private ResourceDependencyResolverService dependencyResolver;

    @Inject
    private ShakedownTestService shakedownTestService;

    @Inject
    private SequencesService sequencesService;

    @Inject
    CommonFilterService commonFilterService;

    @Inject
    protected EntityManager em;

    @Inject
    protected Logger log;

    @Inject
    private Event<DeploymentEvent> deploymentEvent;

    @Inject
    private LockingService lockingService;

    private PropertyDescriptorEntity mavenVersionProperty = null;

    /**
     * @return a Tuple containing the filter deployments and the total deployments for that filter if doPageingCalculation is true
     */
    public Tuple<Set<DeploymentEntity>, Integer> getFilteredDeployments(
            boolean doPageingCalculation, Integer startIndex,
            Integer maxResults, List<CustomFilter> filter, String colToSort,
            CommonFilterService.SortingDirectionType sortingDirection, List<Integer> myAmw) {
        Integer totalItemsForCurrentFilter = null;

        StringBuilder stringQuery = new StringBuilder();

        if (isLastDeploymentForAsEnvFilterSet(filter)) {
            stringQuery.append(getListOfLastDeploymentsForAppServerAndContextQuery(false));
            commonFilterService.appendWhereAndMyAmwParameter(myAmw, stringQuery, "and " + getEntityDependantMyAmwParameterQl());
        } else {
            stringQuery.append("select " + DEPLOYMENT_QL_ALIAS + " from " + DEPLOYMENT_ENTITY_NAME + " " + DEPLOYMENT_QL_ALIAS + " ");
            commonFilterService.appendWhereAndMyAmwParameter(myAmw, stringQuery, getEntityDependantMyAmwParameterQl());
        }

        String baseQuery = stringQuery.toString();

        boolean lowerSortCol = DeploymentFilterTypes.APPSERVER_NAME.getFilterTabColumnName().equals(colToSort);

        Query query = commonFilterService.addFilterAndCreateQuery(stringQuery, filter, colToSort, sortingDirection, DEPLOYMENT_QL_ALIAS + ".id", lowerSortCol, isLastDeploymentForAsEnvFilterSet(filter));

        query = commonFilterService.setParameterToQuery(startIndex, maxResults, myAmw, query);

        // some stuff may be lazy loaded
        List<DeploymentEntity> resultList = query.getResultList();

        Set<DeploymentEntity> deployments = new LinkedHashSet<>();
        deployments.addAll(resultList);

        if (doPageingCalculation) {
            String countQueryString = baseQuery.replace("select " + DEPLOYMENT_QL_ALIAS, "select count(" + DEPLOYMENT_QL_ALIAS + ".id)");
            Query countQuery = commonFilterService.addFilterAndCreateQuery(new StringBuilder(countQueryString), filter, null, null, null, lowerSortCol, isLastDeploymentForAsEnvFilterSet(filter));

            commonFilterService.setParameterToQuery(null, null, myAmw, countQuery);
            totalItemsForCurrentFilter = ((Long) countQuery.getSingleResult()).intValue();
        }

        return new Tuple<>(deployments, totalItemsForCurrentFilter);
    }

    private String getEntityDependantMyAmwParameterQl() {
        return "(" + GROUP_QL + ".id in (:" + CommonFilterService.MY_AMW + ")) ";
    }

    private boolean isLastDeploymentForAsEnvFilterSet(List<CustomFilter> filter) {
        if (filter != null) {
            for (CustomFilter deploymentFilter : filter) {
                if (deploymentFilter.isSpecialFilterType()
                        && deploymentFilter.isSelected()) {
                    return true;
                }
            }
        }
        return false;
    }

    public DeploymentEntity getPreviousDeployment(DeploymentEntity sourceDeployment) throws AMWException {
        LinkedList<CustomFilter> filters = new LinkedList<>();

        DeploymentFilterTypes filterType = DeploymentFilterTypes.LASTDEPLOYJOBFORASENV;
        CustomFilter filter = new CustomFilter(filterType.getFilterDisplayName(), filterType.getFilterTabColumnName(), filterType.getFilterType());
        filters.add(filter);

        filterType = DeploymentFilterTypes.APPSERVER_NAME;
        filter = new CustomFilter(filterType.getFilterDisplayName(), filterType.getFilterTabColumnName(), filterType.getFilterType());
        filter.setValue(sourceDeployment.getResourceGroup().getName());
        filter.setComperatorSelection(ComperatorFilterOption.equals);
        filters.add(filter);

        filterType = DeploymentFilterTypes.ENVIRONMENT_NAME;
        filter = new CustomFilter(filterType.getFilterDisplayName(), filterType.getFilterTabColumnName(), filterType.getFilterType());
        filter.setValue(sourceDeployment.getContext().getName());
        filter.setComperatorSelection(ComperatorFilterOption.equals);
        filters.add(filter);

        filterType = DeploymentFilterTypes.DEPLOYMENT_STATE;
        filter = new CustomFilter(filterType.getFilterDisplayName(), filterType.getFilterTabColumnName(), filterType.getFilterType());
        filter.setEnumType(DeploymentState.class);
        filter.setValue(DeploymentState.success.name());
        filter.setComperatorSelection(ComperatorFilterOption.equals);
        filters.add(filter);

        filterType = DeploymentFilterTypes.DEPLOYMENT_STATE;
        filter = new CustomFilter(filterType.getFilterDisplayName(), filterType.getFilterTabColumnName(), filterType.getFilterType());
        filter.setEnumType(DeploymentState.class);
        filter.setValue(DeploymentState.failed.name());
        filter.setComperatorSelection(ComperatorFilterOption.equals);
        filters.add(filter);

        Set<DeploymentEntity> prevDeployments = getFilteredDeployments(false, 0, null, filters, null, null, null).getA();
        if (prevDeployments.size() != 1) {
            throw new AMWException(
                    "Previous deployment for " + sourceDeployment.getResourceGroup().getName()
                            + " context " + sourceDeployment.getContext().getName() + " not found");
        }

        return prevDeployments.iterator().next();
    }

    // TODO test
    public List<DeploymentEntity> getListOfLastDeploymentsForAppServerAndContext(
            boolean onlySuccessful) {

        TypedQuery<DeploymentEntity> query = em.createQuery(getListOfLastDeploymentsForAppServerAndContextQuery(onlySuccessful), DeploymentEntity.class);

        return query.getResultList();
    }

    private String getListOfLastDeploymentsForAppServerAndContextQuery(boolean onlySuccessful) {
        String successStateCheck = "";
        if (onlySuccessful) {
            successStateCheck = "and "
                    + DEPLOYMENT_QL_ALIAS
                    + ".deploymentState = '"
                    + DeploymentEntity.DeploymentState.success + "'";
        }

        return "select " + DEPLOYMENT_QL_ALIAS + " from " + DEPLOYMENT_ENTITY_NAME + " " + DEPLOYMENT_QL_ALIAS + " where " + DEPLOYMENT_QL_ALIAS + ".deploymentDate = "
                + "(select max(t.deploymentDate) from DeploymentEntity t "
                + "where " + DEPLOYMENT_QL_ALIAS + ".context = t.context and " + DEPLOYMENT_QL_ALIAS + ".resourceGroup = t.resourceGroup) " + successStateCheck;
    }

    /**
     * Save deployment
     *
     * @param deployment
     * @return
     */
    private DeploymentEntity saveDeployment(DeploymentEntity deployment) {
        //TODO hack (YP): calling merge on deployment will also call merge on deployment.resource. Because ResrouceEntity has a lot
        //           of "cascade = ALL" annotations all those properties will be loaded too before merge. This will cause about 800 queries.
        //           With this hack the deployment.resouce is attached and the cascades will be ignored.
        if (!em.contains(deployment) && deployment.getResource() != null) {
            deployment.setResource(em.find(ResourceEntity.class, deployment.getResource().getId()));
        }

        return em.merge(deployment);
    }

    // TODO test
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Integer createDeploymentReturnTrackingId(Integer appServerGroupId, Integer releaseId,
                                                    Date deploymentDate, Date stateToDeploy,
                                                    List<Integer> contextIds,
                                                    List<ApplicationWithVersion> applicationWithVersion,
                                                    List<DeploymentParameter> deployParams,
                                                    boolean sendEmail, boolean requestOnly, boolean doSimulate,
                                                    boolean isExecuteShakedownTest, boolean isNeighbourhoodTest) {

        Integer trackingId = sequencesService.getNextValueAndUpdate(DeploymentEntity.SEQ_NAME);

        Date now = new Date();
        if (deploymentDate == null || deploymentDate.before(now)) {
            deploymentDate = now;
        }
        createDeploymentForAppserver(appServerGroupId, releaseId, deploymentDate, stateToDeploy, contextIds, applicationWithVersion, deployParams, sendEmail, requestOnly, doSimulate,
                isExecuteShakedownTest, isNeighbourhoodTest, trackingId);

        if (deploymentDate == now && !requestOnly) {
            deploymentEvent.fire(new DeploymentEvent(DeploymentEventType.NEW, DeploymentState.scheduled));
        }

        return trackingId;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Integer createDeploymentsReturnTrackingId(List<DeploymentEntity> selectedDeployments,
                                                     Date deploymentDate, Date stateToDeploy,
                                                     List<DeploymentParameter> deployParams,
                                                     List<Integer> contextIds,
                                                     boolean sendEmail, boolean requestOnly, boolean doSimulate,
                                                     boolean isExecuteShakedownTest, boolean isNeighbourhoodTest) {

        Integer trackingId = sequencesService.getNextValueAndUpdate(DeploymentEntity.SEQ_NAME);

        Date now = new Date();
        if (deploymentDate == null || deploymentDate.before(now)) {
            deploymentDate = now;
        }

        for (DeploymentEntity selectedDeployment : selectedDeployments) {

            List<ApplicationWithVersion> applicationWithVersion = selectedDeployment.getApplicationsWithVersion();

            Integer appServerGroupId = selectedDeployment.getResourceGroup().getId();
            Integer releaseId = selectedDeployment.getRelease().getId();

            createDeploymentForAppserver(appServerGroupId, releaseId, deploymentDate, stateToDeploy, contextIds, applicationWithVersion, deployParams, sendEmail, requestOnly, doSimulate,
                    isExecuteShakedownTest, isNeighbourhoodTest, trackingId);
        }


        if (deploymentDate == now && !requestOnly) {
            deploymentEvent.fire(new DeploymentEvent(DeploymentEventType.NEW, DeploymentState.scheduled));
        }

        return trackingId;
    }

    private void createDeploymentForAppserver(Integer appServerGroupId, Integer releaseId, Date deploymentDate, Date stateToDeploy, List<Integer> contextIds, List<ApplicationWithVersion>
            applicationWithVersion, List<DeploymentParameter> deployParams, boolean sendEmail, boolean requestOnly, boolean doSimulate, boolean isExecuteShakedownTest, boolean
            isNeighbourhoodTest, Integer trackingId) {
        ResourceGroupEntity group = em.find(ResourceGroupEntity.class, appServerGroupId);
        ReleaseEntity release = em.find(ReleaseEntity.class, releaseId);
        ResourceEntity resource = dependencyResolver.getResourceEntityForRelease(group, release);

        if (contextIds == null || contextIds.size() == 0) {
            throw new IllegalArgumentException("Context can not be empty");
        }

        for (Integer contextId : contextIds) {
            DeploymentEntity deployment = new DeploymentEntity();
            deployment.setTrackingId(trackingId);

            deployment.setDeploymentJobCreationDate(new Date());
            ContextEntity context = em.find(ContextEntity.class,
                    contextId);

            deployment.setContext(context);

            deployment.setApplicationsWithVersion(applicationWithVersion);

            deployment.setDeploymentRequestUser(permissionService.getCurrentUserName());

            deployment.setCreateTestAfterDeployment(isExecuteShakedownTest);
            if (isExecuteShakedownTest && isNeighbourhoodTest) {
                deployment.setCreateTestForNeighborhoodAfterDeployment(true);
            } else {
                deployment.setCreateTestForNeighborhoodAfterDeployment(false);
            }

            boolean hasPermission = permissionService
                    .hasPermission(context.getName());
            if (!requestOnly && hasPermission) {
                deployment.confirm(permissionService.getCurrentUserName());
            } else {
                deployment.setDeploymentState(DeploymentState.requested);
            }

            if (resource != null && resource.getRuntime() != null) {
                deployment.setRuntime(dependencyResolver.getResourceEntityForRelease(resource.getRuntime(), release));
            }

            if (deployment.getRuntime() == null) {
                throw new IllegalArgumentException("No runtime found for given AppServer at deployment time");
            }

            deployment.setResourceGroup(group);
            deployment.setResource(resource);
            deployment.setDeploymentDate(deploymentDate);
            deployment.setStateToDeploy(stateToDeploy);
            deployment.setSendEmail(sendEmail);

            deployment.setSimulating(doSimulate);
            // bis simulation ausgeführt wird, soll kein buildfehler
            // angezeigt werden
            deployment.setBuildSuccess(true);

            deployment.setRelease(release);

            createAndAddDeploymentParameterForDeployment(deployment, deployParams);

            em.persist(deployment);
            log.info("Deployment for appServer " + group.getName() + " env " + contextId + " created");

        }
    }

    private void createAndAddDeploymentParameterForDeployment(DeploymentEntity deployment, List<DeploymentParameter> deployParams) {
        for(DeploymentParameter parameter : deployParams){
            DeploymentParameter persistDeploymentParameter = createPersistDeploymentParameter(parameter.getKey(), parameter.getValue());
            persistDeploymentParameter.setDeployment(deployment);
            deployment.addDeploymentParameter(persistDeploymentParameter);
        }
    }



    public DeploymentParameter createPersistDeploymentParameter(String key, String value) {
        Objects.requireNonNull(key, "Must not be null");
        return new DeploymentParameter(key, value);
    }

    /**
     * Create the NodeJobEntity for the given Development and node
     *
     * @param deployment
     * @param node
     * @return the created NodeJobEntity
     */
    public NodeJobEntity createAndPersistNodeJobEntity(DeploymentEntity deployment, ResourceEntity node) {
        NodeJobEntity nodeJobEntity = new NodeJobEntity();
        nodeJobEntity.setDeployment(deployment);
        nodeJobEntity.setDeploymentState(deployment.getDeploymentState());
        try { //ignore the exception
            nodeJobEntity.setStatus(NodeJobStatus.RUNNING);
        } catch (DeploymentStateException e) {
        }
        em.persist(nodeJobEntity);
        return nodeJobEntity;
    }

    /**
     * Set NodeJobStatus for deployment
     *
     * @param deploymentId
     * @param nodeJobId
     * @param nodeJobStatus
     */
    public void updateNodeJobStatus(Integer deploymentId, Integer nodeJobId, NodeJobStatus nodeJobStatus) throws NotFoundExcption, DeploymentStateException {
        DeploymentEntity deployment = getDeploymentById(deploymentId);
        NodeJobEntity nodeJobEntity = deployment.findNodeJobEntity(nodeJobId);

        if (nodeJobEntity == null) {
            throw new NotFoundExcption("NodeJob " + nodeJobId + " of deployment " + deploymentId + " not found!");
        }

        nodeJobEntity.setStatus(nodeJobStatus);
        em.persist(nodeJobEntity);

        // The event decouples the transaction and leads to db commit.
        // handleNodeJobUpdate needs a consistent view of the nodeJobs to detect the last nodeJob.
        deploymentEvent.fire(new DeploymentEvent(DeploymentEventType.NODE_JOB_UPDATE, deploymentId, null));
    }

    public void handleNodeJobUpdate(Integer deploymentId) {
		DeploymentEntity deployment = getDeploymentById(deploymentId);
		log.fine("handleNodeJobUpdate called state: " + deployment.getDeploymentState());

		if (!deployment.isPredeploymentFinished()) {
			return;
		}
		if (!DeploymentState.PRE_DEPLOYMENT.equals(deployment.getDeploymentState())) {
			return;
		}

		if (deployment.isPredeploymentSuccessful()) {
			try {
				handlePreDeploymentSuccessful(deployment);
			} catch (OptimisticLockException e) {
				// If it fails the deployment will be retried by the scheduler
				return;
			}
		}
        else {
            updateDeploymentInfoAndSendNotification(GenerationModus.PREDEPLOY, deploymentId, "Deployment (previous state : " + deployment.getDeploymentState() + ") failed due to NodeJob failing at " + new Date(), deployment.getResource().getId(), null);
            log.info("Deployment " + deployment.getId() + " (previous state : " + deployment.getDeploymentState() + ") failed due to NodeJob failing");
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void handlePreDeploymentSuccessful(DeploymentEntity deployment) {
		DeploymentState previousState = deployment.getDeploymentState();

        deployment.setDeploymentState(DeploymentState.READY_FOR_DEPLOYMENT);
        deployment.appendStateMessage("All NodeJobs successful, updated deployment state from " + previousState + " to " + deployment.getDeploymentState() + " at " + new Date());
        log.info("All NodeJobs of deployment " + deployment.getId() + " successful, updated deployment state from " + previousState + " to " + deployment.getDeploymentState().name());
        deploymentEvent.fire(new DeploymentEvent(DeploymentEventType.UPDATE, deployment.getId(), deployment.getDeploymentState()));
    }

    // TODO test
    private String getVersion(ResourceEntity application, List<Integer> contextIds)
            throws NumberFormatException, ResourceNotFoundException, MultipleVersionsForApplicationException {

        String currentValue = null;
        List<PropertyEntity> descriptor;
        ContextEntity context;
        ResourceEntity resource = resourceEditService.loadResourceEntityForEdit(application.getId(), false);

        for (Integer contextId : contextIds) {
            context = contextDomainService.getContextEntityById(contextId);
            descriptor = TemplateUtils.getValueForProperty(resource, getVersionProperty(), context, false, new AMWTemplateExceptionHandler());
            String tmpValue;
            if (descriptor.size() > 0) {
                tmpValue = descriptor.get(0) != null ? descriptor.get(0).getValue() : null;
                if (currentValue != null && !currentValue.equals(tmpValue)) {
                    // return
                    String msg = "For application " + application.getName() + " the are different maven version defined!";
                    log.info(msg);
                    throw new MultipleVersionsForApplicationException(msg);
                }
                currentValue = tmpValue;
            }
        }
        return currentValue;
    }

    public List<ApplicationWithVersion> getVersions(ResourceEntity appServer, List<Integer> contextIds, ReleaseEntity release) {
        List<ApplicationWithVersion> appsWithVersion = new LinkedList<>();

        //attache the appserver. without it getConsumedRelatedResourcesByResourceType
        //throws lazy load exception even if all relations of the appServer were loaded
        appServer = em.find(ResourceEntity.class, appServer.getId());
        Set<ResourceEntity> apps = dependencyResolver.getConsumedRelatedResourcesByResourceType(appServer, DefaultResourceTypeDefinition.APPLICATION, release);
        for (ResourceEntity app : apps) {
            String version = StringUtils.EMPTY;

            try {
                version = getVersion(app, contextIds);
            } catch (MultipleVersionsForApplicationException e) {
                version = DIFF_VERSIONS;
            } catch (NumberFormatException | ResourceNotFoundException e) {
                log.log(Level.WARNING, "Error getting Version for Resource", e);
            }

            appsWithVersion.add(new ApplicationWithVersion(app.getName(), app.getId(), version));
        }

        return appsWithVersion;
    }

    // TODO test
    public String[] getLogFileNames(final int deploymentId) {
        String logsPath = ConfigurationService
                .getProperty(ConfigKey.LOGS_PATH);
        if (logsPath == null) {
            String message = "System property \"" + ConfigKey.LOGS_PATH.getValue() + "\" not set!";
            log.log(Level.WARNING, message);
            throw new AMWRuntimeException(message);
        }

        File dir = new File(logsPath);
        if (dir.exists() && dir.isDirectory()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(deploymentId + "_");
                }
            };
            return dir.list(filter);

        } else {
            String message;
            if (!dir.exists()) {
                message = "Directory " + logsPath + " doesn't exist!";
            } else {
                message = logsPath + " is not a directory!";
            }
            log.log(Level.WARNING, message);
            throw new AMWRuntimeException(message);
        }
    }

    // TODO test
    public String getDeploymentLog(String logName) throws IllegalAccessException {
        String logsPath = ConfigurationService.getProperty(ConfigKey.LOGS_PATH);
        if (logName.contains(File.separator)) {
            throw new IllegalAccessException("The log file contains a file separator (\"" + File.separator + "\"). For security reasons, this is not permitted!");
        }

        StringBuilder content = new StringBuilder();
        Scanner scanner;
        try {
            scanner = new Scanner(new FileInputStream(logsPath + File.separator + logName));
            try {
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine()).append('\n');
                }
            } finally {
                scanner.close();
            }
            return content.toString();
        } catch (FileNotFoundException e) {
            String message = "The file "
                    + logsPath
                    + File.separator
                    + logName
                    + " was found, but couldn't be read!";
            log.log(Level.WARNING, message);
            throw new AMWRuntimeException(message, e);
        }

    }

    /**
     * Get the Deployments to Execute limited by the Configparameter
     *
     * @return
     */
    public List<DeploymentEntity> getDeploymentsToExecute() {
        return em.createQuery(
                "from " + DEPLOYMENT_ENTITY_NAME + " " + DEPLOYMENT_QL_ALIAS
                        + " left join fetch " + DEPLOYMENT_QL_ALIAS
                        + ".runtime where " + DEPLOYMENT_QL_ALIAS
                        + ".deploymentState = :deploymentState", DeploymentEntity.class)
                .setParameter("deploymentState", DeploymentEntity.DeploymentState.READY_FOR_DEPLOYMENT)
                .setMaxResults(getDeploymentProcessingLimit())
                .getResultList();
    }


    /**
     * Get the Predeployments to Execute limited by the Configparameter
     *
     * @return
     */
    public List<DeploymentEntity> getPreDeploymentsToExecute() {
        return em.createQuery(
                "from " + DEPLOYMENT_ENTITY_NAME + " " + DEPLOYMENT_QL_ALIAS
                        + " left join fetch " + DEPLOYMENT_QL_ALIAS
                        + ".runtime where " + DEPLOYMENT_QL_ALIAS
                        + ".deploymentState = :deploymentState and " + DEPLOYMENT_QL_ALIAS
                        + ".deploymentDate<=:now", DeploymentEntity.class)
                .setParameter("deploymentState", DeploymentEntity.DeploymentState.scheduled)
                .setParameter("now", new Date())
                .setMaxResults(getDeploymentProcessingLimit())
                .getResultList();
    }

    /**
     * @return the Amount of Deployments processing in one Run
     */
    public int getDeploymentProcessingLimit() {
        return ConfigurationService.getPropertyAsInt(ConfigKey.DEPLOYMENT_PROCESSING_AMOUNT_PER_RUN);
    }

    /**
     * @return the Amount of PreDeployments processing in one Run
     */
    public int getPreDeploymentProcessingLimit() {
        return ConfigurationService.getPropertyAsInt(ConfigKey.DEPLOYMENT_PREDEPLOYMENT_AMOUNT_PER_RUN);
    }

    /**
     * get the deployment by id
     *
     * @param id
     * @return the found deployment
     */
    public DeploymentEntity getDeploymentById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id can't be null!");
        }
        Query query = em.createQuery(
                "from " + DEPLOYMENT_ENTITY_NAME + " " + DEPLOYMENT_QL_ALIAS
                        + " where " + DEPLOYMENT_QL_ALIAS + ".id=:id")
                .setParameter("id", id);
        return (DeploymentEntity) query.getSingleResult();
    }

    /**
     * gets all Deployment that are in progress and the Timeout is reached
     *
     * @return List of DeploymentEntity
     */
    public List<DeploymentEntity> getDeploymentsInProgressTimeoutReached() {
        int timeout = ConfigurationService.getPropertyAsInt(ConfigKey.DEPLOYMENT_IN_PROGRESS_TIMEOUT);
        TypedQuery<DeploymentEntity> query = em.createQuery(
                "from " + DEPLOYMENT_ENTITY_NAME + " " + DEPLOYMENT_QL_ALIAS
                        + " where " + DEPLOYMENT_QL_ALIAS + ".deploymentState = :deploymentState "
                        + "and " + DEPLOYMENT_QL_ALIAS
                        + ".deploymentDate < :deploymentLimit", DeploymentEntity.class)
                .setParameter("deploymentState", DeploymentState.progress)
                .setParameter("deploymentLimit", getTimeoutDate(new Date(), timeout));
        return query.getResultList();
    }

    protected Date getTimeoutDate(Date from, int timeOut) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(from);
        cal.add(Calendar.SECOND, -timeOut);
        return cal.getTime();
    }

    /**
     * gets all PreDeployment that are in progress and the Timeout is reached
     *
     * @return List of DeploymentEntity
     */
    public List<DeploymentEntity> getPreDeploymentsInProgressTimeoutReached() {
        int timeout = ConfigurationService.getPropertyAsInt(ConfigKey.PREDEPLOYMENT_IN_PROGRESS_TIMEOUT);
        TypedQuery<DeploymentEntity> query = em.createQuery(
                "from " + DEPLOYMENT_ENTITY_NAME + " " + DEPLOYMENT_QL_ALIAS
                        + " where " + DEPLOYMENT_QL_ALIAS + ".deploymentState = :deploymentState "
                        + "and " + DEPLOYMENT_QL_ALIAS
                        + ".deploymentDate < :deploymentLimit", DeploymentEntity.class)
                .setParameter("deploymentState", DeploymentState.PRE_DEPLOYMENT)
                .setParameter("deploymentLimit", getTimeoutDate(new Date(), timeout));
        return query.getResultList();
    }

    /**
     * Get Deployments that are in PRE_DEPLOYMENT but all of it's nodeJobs finished.
     *
     * @return List of DeploymentEntity
     */
    public List<DeploymentEntity> getFinishedPreDeployments() {
        TypedQuery<DeploymentEntity> query = em.createQuery(
                "select "+ DEPLOYMENT_QL_ALIAS +" from "+ DEPLOYMENT_ENTITY_NAME + " " + DEPLOYMENT_QL_ALIAS
                + " where " + DEPLOYMENT_QL_ALIAS + ".deploymentState = :deploymentState"
                + " and exists (select 1 from " + DEPLOYMENT_QL_ALIAS + ".nodeJobs where deploymentState = :deploymentState)"
                + " and not exists (select 1 from " + DEPLOYMENT_QL_ALIAS + ".nodeJobs where status = :running and deploymentState = :deploymentState)",
                		DeploymentEntity.class)
                .setParameter("deploymentState", DeploymentState.PRE_DEPLOYMENT)
                .setParameter("running", NodeJobStatus.RUNNING);
        return query.getResultList();
    }

    /**
     * Returns all Deployment to be simulated
     *
     * @return the list of Deployments to simulate
     */
    public List<DeploymentEntity> getDeploymentsToSimulate() {
        return em.createQuery(
                "from " + DEPLOYMENT_ENTITY_NAME + " " + DEPLOYMENT_QL_ALIAS
                        + " left join fetch " + DEPLOYMENT_QL_ALIAS
                        + ".runtime where " + DEPLOYMENT_QL_ALIAS
                        + ".simulating=true and (" + DEPLOYMENT_QL_ALIAS
                        + ".deploymentState = :scheduled or " + DEPLOYMENT_QL_ALIAS
                        + ".deploymentState = :requested) and " + DEPLOYMENT_QL_ALIAS
                        + ".deploymentDate >= CURRENT_DATE", DeploymentEntity.class)
                .setParameter("scheduled", DeploymentState.scheduled)
                .setParameter("requested", DeploymentState.requested)
                .setMaxResults(getDeploymentSimulationLimit())
                .getResultList();
    }

    /**
     * @return the Amount of Deployments simulating in one run
     */
    public int getDeploymentSimulationLimit() {
        return ConfigurationService.getPropertyAsInt(ConfigKey.DEPLOYMENT_SIMULATION_AMOUNT_PER_RUN);
    }

    // TODO test
    //caches the value
    private PropertyDescriptorEntity getVersionProperty() {
        if (mavenVersionProperty == null) {
            mavenVersionProperty = (PropertyDescriptorEntity) em
                    .createQuery(
                            "select " + PROPERTY_DESCRIPTOR_ENTITY_QL
                                    + " from PropertyDescriptorEntity "
                                    + PROPERTY_DESCRIPTOR_ENTITY_QL
                                    + " where "
                                    + PROPERTY_DESCRIPTOR_ENTITY_QL
                                    + ".propertyName=:Version")
                    .setParameter("Version", "Version")
                    .getSingleResult();
        }
        return mavenVersionProperty;
    }

    /**
     * Helper method to append a message to the deploymentstatus message of all of the provided deployment
     * entities
     *
     * @param deployments - the deployments to be updated
     * @param message     - the message to be appended to the state message.
     */
    private void updateDeploymentStatusMessage(final List<DeploymentEntity> deployments, final String message) {
        if (message != null) {
            for (final DeploymentEntity deploymentEntity : deployments) {
                final DeploymentEntity d = em.merge(deploymentEntity);
                d.appendStateMessage(message);
            }
        }

    }

    /**
     * We update the information about a deployment that has been executed.
     *
     * @param generationModus  - if the deployment was in simulation or realistic mode
     * @param deploymentId     - the deployment id of the deployment that has been executed.
     * @param errorMessage     - the error message if any other
     * @param resourceId       - the ApplicationServe used for deployment
     * @param generationResult
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DeploymentEntity updateDeploymentInfo(GenerationModus generationModus, final Integer deploymentId, final String errorMessage, final Integer resourceId,
                                                 final GenerationResult generationResult) {
		// don't lock a deployment for predeployment as there is no need to update the deployment.
		if (GenerationModus.PREDEPLOY.equals(generationModus) && errorMessage == null) {
			log.fine("Predeploy script finished at " + new Date());
			return em.find(DeploymentEntity.class, deploymentId);
		}
		DeploymentEntity deployment = em.find(DeploymentEntity.class, deploymentId,
				LockModeType.PESSIMISTIC_FORCE_INCREMENT);

        // set as used for deployment
        if (resourceId != null) {
            ResourceEntity as = em.find(ResourceEntity.class, resourceId);
            deployment.setResource(as);
        }

        if (GenerationModus.DEPLOY.equals(generationModus)) {
            if (errorMessage == null) {
                String nodeInfo = getNodeInfoForDeployment(generationResult);
                deployment.appendStateMessage("Successfully deployed at " + new Date() + "\n" + nodeInfo);
                deployment.setDeploymentState(DeploymentState.success);
            } else {
                deployment.appendStateMessage(errorMessage);
                deployment.setDeploymentState(DeploymentState.failed);
            }
        } else if (GenerationModus.PREDEPLOY.equals(generationModus)) {
            deployment.appendStateMessage(errorMessage);
            deployment.setDeploymentState(DeploymentState.failed);
        } else {
            if (errorMessage == null) {
                String nodeInfo = getNodeInfoForDeployment(generationResult);
                deployment.appendStateMessage("Successfully generated at " + new Date() + "\n" + nodeInfo);
                deployment.setBuildSuccess(true);
            } else {
                deployment.appendStateMessage(errorMessage);
                deployment.setBuildSuccess(false);
            }
            if (deployment.getDeploymentConfirmed() != null && deployment.getDeploymentConfirmed()) {
                deployment.setDeploymentState(DeploymentState.scheduled);
            } else {
                deployment.setDeploymentState(DeploymentState.requested);
            }
        }
        deployment.setSimulating(false);
        return deployment;
    }

    private String getNodeInfoForDeployment(GenerationResult generationResult) {
        StringBuilder sb = new StringBuilder();
        if (generationResult != null) {
            sb.append(generationResult.getGeneratedNodeInfo());
        }
        return sb.toString();
    }

    /**
     * We update the information about a deployment that has been executed.
     *
     * @param generationModus  - if the deployment was in simulation or realistic mode
     * @param deploymentId     - the deployment id of the deployment that has been executed.
     * @param errorMessage     - the error message to set. It must be null if the deployment is successful
     * @param resourceId       - the ApplicationServer resource used for deployment
     * @param generationResult
     */
    public void updateDeploymentInfoAndSendNotification(GenerationModus generationModus, final Integer deploymentId, final String errorMessage, final Integer resourceId, final GenerationResult generationResult) {
        DeploymentEntity deployment = updateDeploymentInfo(generationModus, deploymentId, errorMessage, resourceId, generationResult);
        if (generationModus != null && generationModus.isSendNotificationOnErrorGenerationModus()) {
            sendOneNotificationForTrackingIdOfDeployment(deployment.getTrackingId());
        }
    }


    /**
     * Look up which other deployments (than just this one) belong together (via the tracking id) and create
     * shakedowntests after all of them are executed.
     *
     * @param trackingId
     */
    public void createShakedownTestForTrackinIdOfDeployment(Integer trackingId) {
        // hole alle deployments mit derselben trackingId
        List<DeploymentEntity> deploymentsWithSameTrackingId = getDeplyomentsWithSameTrackingId(trackingId);

        if (isAllDeploymentsWithSameTrackingIdExecuted(deploymentsWithSameTrackingId)) {
            shakedownTestService.createAndExecuteShakedowntestForDeployments(deploymentsWithSameTrackingId);
        }
    }

    /**
     * Look up which other deployments (than just this one) belong together (via the tracking id) and send a
     * single email notification if all of them are executed (independent of their success state).
     */
    public void sendOneNotificationForTrackingIdOfDeployment(Integer trackingId) {
        // hole alle deployments mit derselben trackingId
        List<DeploymentEntity> deploymentsWithSameTrackingId = getDeplyomentsWithSameTrackingId(trackingId);

        if (isAllDeploymentsWithSameTrackingIdExecuted(deploymentsWithSameTrackingId)) {
            sendsNotificationAndUpdatedStatusOfDeployments(deploymentsWithSameTrackingId);
        }
    }

    private boolean isAllDeploymentsWithSameTrackingIdExecuted(List<DeploymentEntity> deploymentsWithSameTrackingId) {
        for (final DeploymentEntity deploymentEntity : deploymentsWithSameTrackingId) {
            // not executed and not failed, failed and executed Deployments are complete
            if (!deploymentEntity.isExecuted() && !DeploymentState.failed.equals(deploymentEntity.getDeploymentState())) {
                return false;
            }
        }
        return true;
    }

    private List<DeploymentEntity> getDeplyomentsWithSameTrackingId(Integer trackingId) {
        final TypedQuery<DeploymentEntity> query = em
                .createQuery("from DeploymentEntity d where d.trackingId=:trackingId", DeploymentEntity.class);
        query.setParameter("trackingId", trackingId);

        return query.getResultList();
    }


    /**
     * Sends notification emails for the given deployments and updates the status message of the deployment
     * database entry
     *
     * @param deployments
     */
    private void sendsNotificationAndUpdatedStatusOfDeployments(final List<DeploymentEntity> deployments) {

        if (deployments != null && !deployments.isEmpty()) {
            String message = deploymentNotificationService.createAndSendMailForDeplyoments(deployments);
            updateDeploymentStatusMessage(deployments, message);
        }
    }

    public List<DeploymentEntity> getDeploymentsForRelease(Integer releaseId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<DeploymentEntity> q = cb
                .createQuery(DeploymentEntity.class);
        Root<DeploymentEntity> r = q.from(DeploymentEntity.class);
        Predicate releasePred = cb.equal(r.<Integer>get("release"), releaseId);
        q.where(releasePred);

        TypedQuery<DeploymentEntity> query = em.createQuery(q);
        return query.getResultList();
    }

    public DeploymentEntity confirmDeployment(Integer deploymentId) throws DeploymentStateException {
        DeploymentEntity deployment = getDeploymentById(deploymentId);
        Date now = new Date();

        checkValidation(isConfirmPossible(deployment), deployment);

        //if the deployment date is in the past, set it to now. Fix for #5504
        if (deployment.getDeploymentDate().before(now)) {
            deployment.setDeploymentDate(now);
        }

        deployment.confirm(permissionService.getCurrentUserName());

        return saveDeployment(deployment);
    }

    public DeploymentEntity confirmDeployment(Integer deploymentId, boolean sendEmail,
                                              boolean executeShakedownTest, boolean neighbourhoodTest, boolean simulateGeneration) throws DeploymentStateException {

        DeploymentEntity deployment = confirmDeployment(deploymentId);

        deployment.setSendEmailConfirmation(sendEmail);
        deployment.setCreateTestAfterDeployment(executeShakedownTest);
        deployment.setCreateTestForNeighborhoodAfterDeployment(neighbourhoodTest);
        deployment.setSimulating(simulateGeneration);

        return saveDeployment(deployment);
    }

    public DeploymentOperationValidation isConfirmPossible(DeploymentEntity deployment) {
        if (!permissionService.hasPermissionForDeployment(deployment)) {
            return DeploymentOperationValidation.MISSING_PERMISSION;
        } else if (!deployment.getDeploymentState().isTransitionAllowed(DeploymentState.scheduled)) {
            return DeploymentOperationValidation.WRONG_STATE;
        }
        return DeploymentOperationValidation.SUCCESS;
    }

    public DeploymentEntity rejectDeployment(Integer deploymentId) throws DeploymentStateException {
        DeploymentEntity deployment = getDeploymentById(deploymentId);

        checkValidation(isRejectPossible(deployment), deployment);

        deployment.reject(permissionService.getCurrentUserName());
        deployment = saveDeployment(deployment);

        return deployment;
    }

    public DeploymentOperationValidation isRejectPossible(DeploymentEntity deployment) {
        //is the same thing atm
        return isConfirmPossible(deployment);
    }

    /**
     * Setze Status eines noch nicht ausgeführten oder gestarteten Deployment
     * auf canceled. Falls das deployment bereits ausgeführt oder gestartet
     * wurde, dann wird eine {@link AMWRuntimeException} geworfen.
     */
    public DeploymentEntity cancelDeployment(Integer deploymentId) throws DeploymentStateException {
        DeploymentEntity deployment = getDeploymentById(deploymentId);

        checkValidation(isCancelPossible(deployment), deployment);

        deployment.cancel(permissionService.getCurrentUserName());
        deployment = saveDeployment(deployment);

        return deployment;
    }

    public DeploymentOperationValidation isCancelPossible(DeploymentEntity deployment) {
        if (!permissionService.hasPermissionForCancelDeployment(deployment)) {
            return DeploymentOperationValidation.MISSING_PERMISSION;
        }
        if (!deployment.getDeploymentState().isTransitionAllowed(DeploymentState.canceled)) {
            return DeploymentOperationValidation.WRONG_STATE;
        }
        return DeploymentOperationValidation.SUCCESS;
    }

    /**
     * Setze DeploymentTime auf dem Entity auf der Datenbank auf den deployment
     * Zeitpunkt des gelieferten (Detachten) Deployment Parameters falls das
     * Deployment nicht bereits ausgeführt oder gestartet wurde.
     */
    public DeploymentEntity changeDeploymentDate(Integer deploymentId, Date newDate) throws DeploymentStateException {
        DeploymentEntity deployment = getDeploymentById(deploymentId);
        Date now = new Date();
        checkValidation(isChangeDeploymentDatePossible(deployment), deployment);

		if(newDate == null || newDate.before(now)) {
            newDate = now;
        }

        deployment.setDeploymentDate(newDate);
        deployment = saveDeployment(deployment);

        return deployment;
    }

    public DeploymentEntity updateDeploymentState(Integer deploymentId, DeploymentState newState) {
        if (newState == null) {
            throw new DeploymentStateException("Deployment state of deployment " + deploymentId + " can be set to null");
        }

        switch (newState) {
            case canceled:
                return cancelDeployment(deploymentId);
            case rejected:
                return rejectDeployment(deploymentId);
            case scheduled:
                return confirmDeployment(deploymentId);
            default:
                throw new DeploymentStateException("Deployment " + deploymentId + " can not be changed");
        }

    }

    public DeploymentOperationValidation isChangeDeploymentDatePossible(DeploymentEntity deployment) {
        if (!permissionService.hasPermissionForDeployment(deployment)) {
            return DeploymentOperationValidation.MISSING_PERMISSION;
        } else if (!deployment.isMutable()) {
            return DeploymentOperationValidation.WRONG_STATE;
        }
        return DeploymentOperationValidation.SUCCESS;
    }

    public void cleanupDeploymentLogs() {
        int cleanupAge = ConfigurationService.getPropertyAsInt(ConfigKey.LOGS_CLEANUP_AGE);
        String logsPathName = ConfigurationService.getProperty(ConfigKey.LOGS_PATH);
        Path logsDir = Paths.get(logsPathName);

        log.fine("Cleaning logs folder " + logsDir);
        FileVisitor<Path> fileVisitor = new ClenaupFileVisitor(logsDir, cleanupAge);

        try {
            Files.walkFileTree(logsDir, fileVisitor);
        } catch (IOException e) {
            log.severe("Log cleanup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deletes all files that are older than DEPLOYMENT_CLEANUP_AGE in the GENERATOR_PATH* directories
     *
     * @throws IOException
     */
    public void cleanupDeploymentFiles() {
        final int cleanupAge = ConfigurationService.getPropertyAsInt(ConfigKey.DEPLOYMENT_CLEANUP_AGE);
        HashSet<String> pathsToCheck = new HashSet<>();
        ConfigKey[] keys = {ConfigKey.GENERATOR_PATH, ConfigKey.GENERATOR_PATH_SIMULATION, ConfigKey.GENERATOR_PATH_TEST};

        //get all generator paths and merge them
        for (ConfigKey key : keys) {
            String path = ConfigurationService.getProperty(key);
            if (path != null) {
                pathsToCheck.add(path);
            }
        }

        for (String basePathName : pathsToCheck) {
            final Path basePath = Paths.get(basePathName);
            log.fine("Cleaning generator folder " + basePath);
            FileVisitor<Path> fileVisitor = new ClenaupFileVisitor(basePath, cleanupAge);

            try {
                Files.walkFileTree(basePath, fileVisitor);
            } catch (IOException e) {
                log.severe("Deployment cleanup failed: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if the validation failed and throws the right exception
     *
     * @param validation
     */
    private void checkValidation(DeploymentOperationValidation validation, DeploymentEntity deployment) throws DeploymentStateException {
        if (DeploymentOperationValidation.MISSING_PERMISSION.equals(validation)) {
            throw new SecurityException("User " + permissionService.getCurrentUserName() + " has no permisson to change deployment " + deployment.getId());
        } else if (DeploymentOperationValidation.WRONG_STATE.equals(validation)) {
            throw new DeploymentStateException("Deployment " + deployment.getId() + " can not be changed");
        }
    }


    /**
     * this method is used for testing only
     *
     * @param em
     */
    protected void setEntityManager(EntityManager em) {
        this.em = em;
    }


    private class ClenaupFileVisitor extends SimpleFileVisitor<Path> {
        private final Date now = new Date();
        private final Path basePath;
        private final int cleanupAge;

        /**
         * Cleans up files in a directory
         *
         * @param basePath   The start folder that should not get deleted
         * @param cleanupAge The age of the files (creation time) to delete in minutes
         */
        ClenaupFileVisitor(Path basePath, int cleanupAge) {
            this.basePath = basePath;
            this.cleanupAge = cleanupAge;
        }


        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            long ageInMs = now.getTime() - attrs.lastAccessTime().toMillis();
            long ageInMin = TimeUnit.MINUTES.convert(ageInMs, TimeUnit.MILLISECONDS);

            if (ageInMin >= cleanupAge) {
                Files.delete(file);
                log.fine("Deleted file " + file);
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
            BasicFileAttributes attrs = Files.getFileAttributeView(dir, BasicFileAttributeView.class).readAttributes();
            long ageInMs = now.getTime() - attrs.lastAccessTime().toMillis();
            long ageInMin = TimeUnit.MINUTES.convert(ageInMs, TimeUnit.MILLISECONDS);

            if (e == null) {
                File file = dir.toFile();
                if (!basePath.equals(dir) && ageInMin >= cleanupAge && file.list().length == 0) {
                    file.delete();
                    log.fine("Deleted folder " + file.getCanonicalPath());
                }
                return FileVisitResult.CONTINUE;
            } else {
                // directory iteration failed
                throw e;
            }
        }
    }
}
