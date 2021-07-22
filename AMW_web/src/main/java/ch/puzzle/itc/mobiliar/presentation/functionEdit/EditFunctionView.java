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

package ch.puzzle.itc.mobiliar.presentation.functionEdit;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import ch.puzzle.itc.mobiliar.business.template.entity.RevisionInformation;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import ch.puzzle.itc.mobiliar.business.function.boundary.FunctionsBoundary;
import ch.puzzle.itc.mobiliar.business.function.entity.AmwFunctionEntity;
import ch.puzzle.itc.mobiliar.business.security.boundary.PermissionBoundary;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import ch.puzzle.itc.mobiliar.presentation.ViewBackingBean;
import ch.puzzle.itc.mobiliar.presentation.util.GlobalMessageAppender;

@ViewBackingBean
public class EditFunctionView implements Serializable {

    private static final String COMMA_SEPARATOR = ",";

    private enum FunctionAction {
        OVERWRITE("Overwrite function"), EDIT("Edit function"), CREATE_NEW("Create new function");

        private String displayValue;

        FunctionAction(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }

    }

    @PostConstruct
    protected void init(){
        // initially create empty function
        amwFunction = new AmwFunctionEntity();
        action = FunctionAction.CREATE_NEW;
    }

    @Inject
    FunctionsBoundary functionsBoundary;

    @Inject
    PermissionBoundary permissionBoundary;

    @Getter
    private Integer resourceIdViewParam;

    @Getter
    private Integer resourceTypeIdViewParam;

    @Getter
    private Integer functionIdViewParam;

    @Getter
    @Setter
    private Integer relationIdViewParam;

    private FunctionAction action;

    @Getter
    private AmwFunctionEntity amwFunction;

    @Getter
    @Setter
    private String currentFunctionMiksAsString;

    @Getter
    private AmwFunctionEntity compareAmwFunction;

    @Getter
    private List<RevisionInformation> revisionInformations;

    @Getter
    private RevisionInformation compareRevision;



    public boolean isEditResource() {
        return resourceIdViewParam != null;
    }

    public boolean isNewFunction() {
        return amwFunction == null || amwFunction.getId() == null;
    }

    public void setResourceIdViewParam(Integer resourceIdViewParam) {
        if (this.resourceIdViewParam == null && resourceIdViewParam != null) {
            this.resourceIdViewParam = resourceIdViewParam;

            if (functionIdViewParam != null) {
                setActionForResourceFunction();
            }
        }
    }

    public void setResourceTypeIdViewParam(Integer resourceTypeIdViewParam) {
        if (this.resourceTypeIdViewParam == null && resourceTypeIdViewParam != null) {
            this.resourceTypeIdViewParam = resourceTypeIdViewParam;

            if (functionIdViewParam != null) {
                setActionForResourceTypeFunction();
            }
        }
    }

    public void setFunctionIdViewParam(Integer functionIdViewParam) {
        if (this.functionIdViewParam == null && functionIdViewParam != null) {
            this.functionIdViewParam = functionIdViewParam;

            amwFunction = functionsBoundary.getFunctionById(functionIdViewParam);

            initializeAndSetAction();
        }
    }

    private void initializeAndSetAction(){

        if (amwFunction != null) {
            currentFunctionMiksAsString = amwFunction.getCommaseparatedMikNames();
            refreshRevisionInformation(amwFunction.getId());
        }

        if (resourceIdViewParam != null){
            setActionForResourceFunction();
        }

        if (resourceTypeIdViewParam != null){
            setActionForResourceTypeFunction();
        }
    }

    private void setActionForResourceFunction(){
        if (amwFunction != null){
            if(amwFunction.getResource() != null && amwFunction.getResource().getId().equals(resourceIdViewParam)){
                // edit existing
                action = FunctionAction.EDIT;
            } else {
                action = FunctionAction.OVERWRITE;
            }
        }
    }

    private void setActionForResourceTypeFunction(){
        if (amwFunction != null){
            if(amwFunction.getResourceType() != null && amwFunction.getResourceType().getId().equals(resourceTypeIdViewParam)){
                // edit existing
                action = FunctionAction.EDIT;
            } else {
                action = FunctionAction.OVERWRITE;
            }
        }
    }

    public String getFunctionSaveButtonTitle() {
        if (FunctionAction.EDIT.equals(action)){
            return "Save changes";
        }
        return action.getDisplayValue();
    }

    public String getFunctionActionTitle() {
        return action.getDisplayValue();
    }

    public boolean isFunctionDefinitionEditable(){
        return FunctionAction.CREATE_NEW.equals(action) && canModifyFunction();
    }

    /**
     * Defines if the current user has the rights to modify the function
     */
    public boolean canModifyFunction() {
        return isNewFunction() ? permissionBoundary.canCreateFunctionOfResourceOrResourceType(resourceIdViewParam, resourceTypeIdViewParam)
                :permissionBoundary.canUpdateFunctionOfResourceOrResourceType(resourceIdViewParam, resourceTypeIdViewParam);
    }

    private void refreshRevisionInformation(Integer funId){
        revisionInformations = Lists.reverse(functionsBoundary.getFunctionRevisions(funId));
    }

    /**
     * Returns the id of the function to which we want compare the actual function
     */
    public Integer getCompareGlobalFunctionId() {
        return compareAmwFunction != null ? compareAmwFunction.getId() : null;
    }

    /**
     * Whether or not we are comparing two revisions of a function
     */
    public boolean isCompareMode() {
        return compareAmwFunction != null;
    }

    /**
     * Returns the id of the revision to which we want compare the actual function
     */
    public Integer getCompareRevisionId() {
        return compareRevision != null ? compareRevision.getRevision().intValue() : null;
    }

    /**
     * Sets the revision id to which we want compare the actual function
     */
    public void setCompareRevisionId(Integer compareRevisionId) {
        if (compareRevisionId != null && compareRevisionId > 0) {
            for (RevisionInformation r : revisionInformations) {
                if (r.getRevision().intValue() == compareRevisionId) {
                    compareRevision = r;
                    compareAmwFunction = functionsBoundary.getFunctionByIdAndRevision(functionIdViewParam, compareRevisionId);
                    return;
                }
            }
        }
        compareAmwFunction = null;
        compareRevision = null;
    }


    /**
     * Save function
     */
    public void saveFunction() {
        if (amwFunction.getName() != null && !amwFunction.getName().isEmpty() && !amwFunction.getName().trim().isEmpty()) {
            switch (action) {
                case CREATE_NEW:
                    createNewFunction();
                    break;
                case EDIT:
                    editFunction();
                    break;
                case OVERWRITE:
                    overwriteFunction();
                    break;
            }
            refreshRevisionInformation(amwFunction.getId());
        } else {
            GlobalMessageAppender.addErrorMessage("Function name must not be empty");
        }
    }


    private void createNewFunction() {
        Set<String> functionMikNames = extractMikNamesFromCommaSeperatedString(currentFunctionMiksAsString);
        try {
            if (isEditResource()) {
                amwFunction = functionsBoundary.createNewResourceFunction(amwFunction, resourceIdViewParam, functionMikNames);
            }
            else {
                amwFunction = functionsBoundary.createNewResourceTypeFunction(amwFunction, resourceTypeIdViewParam, functionMikNames);
            }
            GlobalMessageAppender.addSuccessMessage("Function " + amwFunction.getName() + " successfully created");
            action = FunctionAction.EDIT;
        }
        catch (ValidationException e) {
            GlobalMessageAppender.addErrorMessage(buildExceptionMessage(e));
        }
        catch (AMWException e) {
            GlobalMessageAppender.addErrorMessage(e.getMessage());
        }
    }

	private String buildExceptionMessage(ValidationException e){
        StringBuilder sb = new StringBuilder("Function with the name \""+amwFunction.getName()+ "\" already defined");
        if (e.hasCausingObject() && e.getCausingObject() instanceof AmwFunctionEntity){
            AmwFunctionEntity functionWithName = (AmwFunctionEntity)e.getCausingObject();

            if (functionWithName.isDefinedOnResource()){
                sb.append(" on resource \"").append(functionWithName.getResource().getName()).append("\"");
            }
            if (functionWithName.isDefinedOnResourceType()){
                sb.append(" on resource type \"").append(functionWithName.getResourceType().getName()).append("\"");
            }
        }
        return sb.toString();
    }


    private void editFunction() {
		try {
			functionsBoundary.saveFunction(amwFunction);
            GlobalMessageAppender.addSuccessMessage("Changes on function " + amwFunction.getName() + " successfully saved");
            action = FunctionAction.EDIT;
        }
		catch (AMWException e) {
			GlobalMessageAppender.addErrorMessage(e.getMessage());
		}

    }

    private void overwriteFunction() {
        try {
            if (isEditResource()) {
                amwFunction = functionsBoundary.overwriteResourceFunction(amwFunction.getImplementation(), resourceIdViewParam, functionIdViewParam);
            } else {
                amwFunction = functionsBoundary.overwriteResourceTypeFunction(amwFunction.getImplementation(), resourceTypeIdViewParam, functionIdViewParam);
            }
            GlobalMessageAppender.addSuccessMessage("Function " + amwFunction.getName() + " successfully overwritten");
            action = FunctionAction.EDIT;
        }
        catch (AMWException e) {
            GlobalMessageAppender.addErrorMessage(e.getMessage());
        }
    }

    private Set<String> extractMikNamesFromCommaSeperatedString(String commaSeparatedMiks) {
        Set<String> mikNames = new LinkedHashSet<>();

        String[] miks = commaSeparatedMiks.split(COMMA_SEPARATOR);
        for (String mikName : miks) {
            String trimmedName = mikName.trim();
            if (!trimmedName.isEmpty()) {
                mikNames.add(trimmedName);
            }
        }

        return mikNames;
    }

}
