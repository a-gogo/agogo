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

package ch.puzzle.itc.mobiliar.business.generator.control;

import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * This Class holds the Generation Result for a Node
 */
public class NodeGenerationResult {
	@Getter
	@Setter
	private ResourceEntity node;

	@Getter
	@Setter
	String hostname;

	@Getter
	@Setter
	boolean nodeEnabled;

	@Getter
	@Setter
	private String folderToExecute;

	@Getter
	private List<TemplatePropertyException> propertyValidationExceptions = new ArrayList<TemplatePropertyException>();

	@Getter
	@Setter
	private List<GenerationUnitGenerationResult> applicationServerResults = new ArrayList<GenerationUnitGenerationResult>();

	@Getter
	@Setter
	private List<ApplicationGenerationResult> applicationResults = new ArrayList<ApplicationGenerationResult>();

    @Setter
    private boolean nodeEnabledForTestGeneration = false;
	@Setter
	@Getter
	private String deploymentLogfilePath;

     public void omitAllTemplates(){
	    if(applicationServerResults!=null) {
		   for (GenerationUnitGenerationResult applicationServerResult : applicationServerResults) {
			  if(applicationServerResult.getGeneratedTemplates()!=null) {
				 for (GeneratedTemplate generatedTemplate : applicationServerResult
						 .getGeneratedTemplates()) {
					generatedTemplate.setOmitted(true);
				 }
			  }
		   }
	    }
	    if(applicationResults!=null) {
		   for (ApplicationGenerationResult applicationResult : applicationResults) {
			  if(applicationResult.getGeneratedTemplates()!=null) {
				 for (GeneratedTemplate generatedTemplate : applicationResult.getGeneratedTemplates()) {
					generatedTemplate.setOmitted(true);
				 }
			  }
		   }
	    }
	}

	public void addAllPropertyValidationExceptions(List<TemplatePropertyException> exceptions){
		propertyValidationExceptions.addAll(exceptions);
	}

	/**
	 * checks if there were errors during generation for both applicationServerResult and applicationResults
	 * @return
	 */
	public boolean hasErrors() {
		if(!propertyValidationExceptions.isEmpty()){
			return true;
		}
		for (GenerationUnitGenerationResult result : applicationServerResults) {
			if(!result.isSuccess()){
				return true;
			}
		}
		for (ApplicationGenerationResult result : applicationResults) {
			if(result.hasErrors()){
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the Errors as String
	 */
	public String getErrorMessage() {
		StringBuilder sb = new StringBuilder();
		if(hasErrors()){
			sb.append("Error on Node: " + node.getName() + "\n");

			for (TemplatePropertyException e : propertyValidationExceptions) {
				sb.append(e.getMessage() + "\n");
			}
			for (GenerationUnitGenerationResult result : applicationServerResults) {
				if(!result.isSuccess()){
					sb.append(result.getErrorMessageAsString());
				}
			}
			for (ApplicationGenerationResult result : applicationResults) {
				if(result.hasErrors()){
					sb.append(result.getErrorMessageAsString());
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Returns all generated Templates
	 * @return
	 */
	public List<GeneratedTemplate> getGeneratedTemplates(){
		List<GeneratedTemplate> templates = new ArrayList<GeneratedTemplate>();

		// collect generated Templates for the Application Server
		for (GenerationUnitGenerationResult asResult : applicationServerResults) {
			templates.addAll(asResult.getGeneratedTemplates());
		}
		// collect all generated Templates for all Applications
		for (ApplicationGenerationResult appResult : applicationResults) {
			templates.addAll(appResult.getGeneratedTemplates());
		}

		return templates;
	}

	/**
	 * Get Generated Node Info for the Node generation
	 * @return
	 */
	public String getGeneratedNodeInfo() {
		StringBuilder sb = new StringBuilder();
		if (this.node != null) {
			sb.append("Node: ");
			sb.append(this.node.getName());
			if (this.nodeEnabled) {
				sb.append(" on host ");
				sb.append(hostname);
			} else {
				sb.append(" is disabled");
			}
		}

		return sb.toString();
	}

	public String getTestGenerationNodeInfo() {
		if (node != null && nodeEnabledForTestGeneration) {
			return "Node " + this.node.getName() + " was disabled, enabled for test generation.";
		}
		return null;
	}

	public List<TemplatePropertyException> getPreprocessResults() {
		List<TemplatePropertyException> result = new ArrayList<>();

		// collect preprocess Results for the Application Server
		for (GenerationUnitGenerationResult asResult : applicationServerResults) {
			result.addAll(asResult.getPreprocessResults());
		}
		// collect preprocess Results for all Applications
		for (ApplicationGenerationResult appResult : applicationResults) {
			result.addAll(appResult.getPreprocessResults());
		}
		return result;
	}
}
