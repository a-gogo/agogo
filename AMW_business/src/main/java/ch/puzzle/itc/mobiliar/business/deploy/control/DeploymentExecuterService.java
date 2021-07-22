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

package ch.puzzle.itc.mobiliar.business.deploy.control;

import ch.puzzle.itc.mobiliar.business.deploy.boundary.DeploymentBoundary;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentFailureReason;
import ch.puzzle.itc.mobiliar.business.generator.control.GenerationResult;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratorDomainServiceWithAppServerRelations;
import ch.puzzle.itc.mobiliar.business.generator.control.LockingService;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.GenerationModus;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service to start Deployment asynchronously for the generated Configuration
 */
@Stateless
public class DeploymentExecuterService {

	@Inject
	private DeploymentBoundary deploymentBoundary;

	@Inject
	private GeneratorDomainServiceWithAppServerRelations generatorDomainServiceWithAppServerRelations;

	@Inject
	private LockingService lockingService;

	@Inject
	private DeploymentAsynchronousExecuter deploymentAsynchronousExecuter;

	@Inject 
	private DeploymentExecutionResultHandlerService deploymentExecutionResultHandler;
	
	@Inject
	private Logger log;

	/**
	 * Asynchronous method
	 * 
	 * Reads and generates configuration for the given deploymentId and delegates the effective execution to
	 * the deploymentExecuterService
	 * 
	 * @param deploymentId
	 *             the deploymentId for the given deployment
	 * @param generationModus
	 *             DEPLOY, PREDEPLOY, SIMULATE
	 */
	@Asynchronous
	public void generateConfigurationAndExecuteDeployment(Integer deploymentId, GenerationModus generationModus) {
		GenerationResult result = null;
		// lock Deployment
		boolean isLocked = lockingService.markDeploymentAsRunning(deploymentId, generationModus);
		//failed lock
		if (!isLocked) {
			return;
		}
		DeploymentEntity deployment = deploymentBoundary.getDeploymentById(deploymentId);
		try {
			log.info("Generating deployment " + deployment.getId());
			result = generatorDomainServiceWithAppServerRelations.generateConfigurationForDeployment(deployment, generationModus);
			// catch all Exceptions during Generation
		} catch (Exception e) {
			log.log(Level.SEVERE, "Deployment not successful: " + deploymentId, e);
			DeploymentFailureReason reason = generationModus.equals(GenerationModus.DEPLOY) ?
					DeploymentFailureReason.DEPLOYMENT_GENERATION : generationModus.equals(GenerationModus.PREDEPLOY) ? DeploymentFailureReason.PRE_DEPLOYMENT_GENERATION : null;
			deploymentExecutionResultHandler.handleUnSuccessfulDeployment(generationModus, deployment, null, e, reason);
		}

		if (result != null && !result.hasErrors()) {
			deploymentAsynchronousExecuter.executeDeployment(result, deployment, generationModus);
		}
	}
}
