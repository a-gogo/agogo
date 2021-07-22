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

package ch.puzzle.itc.mobiliar.business.deploy.scheduler;

import ch.puzzle.itc.mobiliar.business.deploy.boundary.DeploymentBoundary;
import ch.puzzle.itc.mobiliar.business.deploy.control.DeploymentExecuterService;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentFailureReason;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentState;
import ch.puzzle.itc.mobiliar.business.deploy.event.DeploymentEvent;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.GenerationModus;
import ch.puzzle.itc.mobiliar.business.shakedown.control.ShakedownTestExecuterService;
import ch.puzzle.itc.mobiliar.business.shakedown.control.ShakedownTestService;
import ch.puzzle.itc.mobiliar.business.shakedown.entity.ShakedownTestEntity;
import ch.puzzle.itc.mobiliar.business.shakedown.event.ShakedownTestEvent;
import ch.puzzle.itc.mobiliar.common.util.ConfigurationService;
import ch.puzzle.itc.mobiliar.common.util.ConfigKey;

import javax.ejb.*;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The DeploymentScheduler is the Service wich triggers the Deployments and
 * Testexecution
 */
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton
public class DeploymentScheduler {

	@Inject
	private DeploymentBoundary deploymentBoundary;

	@Inject
	private ShakedownTestService shakedownTestService;

	@Inject
	private Logger log;

	@Inject
	private DeploymentExecuterService deploymentExecuterService;

	@Inject
	private ShakedownTestExecuterService shakedownTestExecuterService;

	private Level logLevel = Level.INFO;

	/**
	 * Triggers the Deployment and Test Execution This Method is triggerd by
	 * the Container
	 */
	@Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
	public void triggerDeyploymentsAndTests() {
		// do no trigger if disabled
		if (!ConfigurationService.getPropertyAsBoolean(ConfigKey.DEPLOYMENT_SCHEDULER_DISABLED)) {
			executePreDeployments();
			executeDeployments();
			executeSimulation();
			executeForTests();
			checkForFinishedPredeploymentDeployments();
		}
	}

	/**
	 * Checks for long running Deployments and marks them as failed
	 */
	@Schedule(hour = "*", minute = "*", second = "*/45", persistent = false)
	public void checkForPendingDeploymentsAndTest() {
		// do no trigger if disabled
		if (!ConfigurationService.getPropertyAsBoolean(ConfigKey.DEPLOYMENT_SCHEDULER_DISABLED)) {
			checkForEndlessDeployments();
			checkForEndlessPredeploymentDeployments();
		}
	}

	/**
	 * Cleans up generated files and logs
	 */
	@Schedule(hour = "*", minute = "*/30", second = "0", persistent = false)
	public void cleanupDeploymentFiles() throws IOException {
		// do no trigger if disabled
		if (!ConfigurationService.getPropertyAsBoolean(ConfigKey.DEPLOYMENT_CLEANUP_SCHEDULER_DISABLED)) {
			deploymentBoundary.cleanupDeploymentFiles();
		}
		if (!ConfigurationService.getPropertyAsBoolean(ConfigKey.LOGS_CLEANUP_SCHEDULER_DISABLED)) {
			deploymentBoundary.cleanupDeploymentLogs();
		}
	}

	/**
	 * Eventhandler for the DeploymentStatusUpdate Event is triggered when
	 * deployments are created or updated
	 *
	 * @param event
	 */
	@Asynchronous
	public void handleDeploymentEvent(@Observes(during=TransactionPhase.AFTER_SUCCESS) DeploymentEvent event) {
		log.log(logLevel, "Deployment event " + event.getEventType() + " fired for deployment " + event.getDeploymentId());

		switch (event.getEventType()) {
		case NEW:
			executeSimulation();
			executePreDeployments();
			break;
		case UPDATE:
			if (event.getNewState() == DeploymentState.READY_FOR_DEPLOYMENT) {
				if(event.getDeploymentId() != null) {
					deploymentExecuterService.generateConfigurationAndExecuteDeployment(event.getDeploymentId(),
							GenerationModus.DEPLOY);
				}
				else{
					executeDeployments();
				}
			}
			break;
		case NODE_JOB_UPDATE:
			deploymentBoundary.handleNodeJobUpdate(event.getDeploymentId());
		}
	}

	@Asynchronous
	public void handleShakedownTestEvent(@Observes(during=TransactionPhase.AFTER_SUCCESS) ShakedownTestEvent event) {
		log.log(logLevel, "ShakedownTest event fired");

		executeForTests();
	}

	protected synchronized void executeSimulation() {
		int deploymentSimulationLimit = deploymentBoundary.getDeploymentSimulationLimit();
		log.log(Level.FINE, "Checking for simulations, max pro run " + deploymentSimulationLimit);
		List<DeploymentEntity> deployments = deploymentBoundary.getDeploymentsToSimulate();
		if (!deployments.isEmpty()) {
			log.log(logLevel, deployments.size() + " simulations found");
			executeDeployments(deployments, GenerationModus.SIMULATE);
		}
		else {
			log.log(Level.FINE, "No simulations found");
		}
	}

	protected synchronized void executeDeployments() {
		int deploymentProcessingLimit = deploymentBoundary.getDeploymentProcessingLimit();
		log.log(Level.FINE, "Checking for deployments, max pro run " + deploymentProcessingLimit);
		List<DeploymentEntity> deployments = deploymentBoundary.getDeploymentsToExecute();
		if (!deployments.isEmpty()) {
			log.log(logLevel, deployments.size() + " deployments found");
			executeDeployments(deployments, GenerationModus.DEPLOY);
		}
		else {
			log.log(Level.FINE, "No deployments found");
		}
	}

	protected synchronized void executePreDeployments() {
		int preDeploymentProcessingLimit = deploymentBoundary.getPreDeploymentProcessingLimit();
		log.log(Level.FINE, "Checking for preDeployments, max pro run " + preDeploymentProcessingLimit);
		List<DeploymentEntity> deployments = deploymentBoundary.getPreDeploymentsToExecute();
		if (!deployments.isEmpty()) {
			log.log(logLevel, deployments.size() + " Predeployments found");
			executeDeployments(deployments, GenerationModus.PREDEPLOY);
		}
		else {
			log.log(Level.FINE, "No preDeployments found");
		}
	}

	protected synchronized void executeForTests() {
		log.log(Level.FINE, "Checking for tests");
		List<ShakedownTestEntity> tests = shakedownTestService.getTestsToExecute();
		if (!tests.isEmpty()) {
			log.log(logLevel, tests.size() + " tests found");
			doTesting(tests);
		}
		else {
			log.log(Level.FINE, "No tests found");
		}
	}

	protected synchronized void checkForEndlessDeployments() {
		log.log(Level.FINE, "Checking deployments inProgress which reached the timeout");
		List<DeploymentEntity> deployments = deploymentBoundary.getDeploymentsInProgressTimeoutReached();
		if (!deployments.isEmpty()) {
			log.log(logLevel, deployments.size() + " deployments inProgress reached timeout");
			int timeout = ConfigurationService.getPropertyAsInt(ConfigKey.DEPLOYMENT_IN_PROGRESS_TIMEOUT);
			handleDeploymentsTimeout(deployments, GenerationModus.DEPLOY, timeout);
		}
		else {
			log.log(Level.FINE, "No deployments inProgress have reached the timeout");
		}
	}

	protected synchronized void checkForEndlessPredeploymentDeployments() {
		log.log(Level.FINE, "Checking preDeployments inprogress which reached the timeout");
		List<DeploymentEntity> deployments = deploymentBoundary.getPreDeploymentsInProgressTimeoutReached();
		if (!deployments.isEmpty()) {
			log.log(logLevel, deployments.size() + " preDeployments inProgress reached timeout");
			int timeout = ConfigurationService.getPropertyAsInt(ConfigKey.PREDEPLOYMENT_IN_PROGRESS_TIMEOUT);;
			handleDeploymentsTimeout(deployments, GenerationModus.PREDEPLOY, timeout);
		}
		else {
			log.log(Level.FINE, "No preDeployments inProgress have reached the timeout");
		}
	}

	protected synchronized void checkForFinishedPredeploymentDeployments() {
		log.log(Level.FINE, "Checking for finished preDeployments");
		List<DeploymentEntity> deployments = deploymentBoundary.getFinishedPreDeployments();
		if (!deployments.isEmpty()) {
			log.log(logLevel, deployments.size() + " preDeployments finished");
			for(DeploymentEntity d : deployments) {
				deploymentBoundary.handleNodeJobUpdate(d.getId());
			}
		}
		else {
			log.log(Level.FINE, "No finished preDeployments found");
		}
	}

	protected void handleDeploymentsTimeout(List<DeploymentEntity> deployments, GenerationModus generationModus, int timeout) {
		for (DeploymentEntity deployment : deployments) {
			log.log(logLevel, "Deployment (" + deployment.getId()
					+ ") was marked as failed because it reached the deplyoment timeout");

			deploymentBoundary.updateDeploymentInfoAndSendNotification(generationModus, deployment.getId(),
					generationModus.getAction() + " was marked as failed because it reached the deplyoment timeout (" + timeout + " s) at " + new Date(),
					deployment.getResource() != null ? deployment.getResource().getId() : null, null, DeploymentFailureReason.TIMEOUT);
		}
	}

	private void executeDeployments(List<DeploymentEntity> deployments, GenerationModus generationModus) {
		for (DeploymentEntity deployment : deployments) {
			if (deployment.getContext() != null) {
				log.log(logLevel, "Checking deployment of " + deployment.getResourceGroup().getName()
						+ " on environement " + deployment.getContext().getName() + " id " + deployment.getId());
				deploymentExecuterService.generateConfigurationAndExecuteDeployment(deployment.getId(),
						generationModus);
			}
		}
	}

	private void doTesting(List<ShakedownTestEntity> tests) {
		for (ShakedownTestEntity test : tests) {
			if (test.getContext() != null) {
				log.log(logLevel, "Checking test of " + test.getApplicationServer().getName() + " on environement "
						+ test.getContext().getName() + " id " + test.getId());
				shakedownTestExecuterService.generateConfigurationAndExecuteShakedownTest(test.getId());
			}
		}
	}

}
