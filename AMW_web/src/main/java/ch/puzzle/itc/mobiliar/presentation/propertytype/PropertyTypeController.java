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

package ch.puzzle.itc.mobiliar.presentation.propertytype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import javax.ejb.EJBException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import ch.puzzle.itc.mobiliar.business.property.control.PropertyTypeScreenDomainService;
import ch.puzzle.itc.mobiliar.business.property.control.PropertyTypeService;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyTypeEntity;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.NotAuthorizedException;
import ch.puzzle.itc.mobiliar.common.exception.PropertyTypeNotDeletableException;
import ch.puzzle.itc.mobiliar.common.exception.PropertyTypeNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.RenameException;
import ch.puzzle.itc.mobiliar.presentation.util.GlobalMessageAppender;


@Named
@RequestScoped
public class PropertyTypeController implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	PropertyTypeScreenDomainService propertyTypeScreenService;

	@Inject
	PropertyTypeService propertyTypeService;

	public List<PropertyTypeEntity> loadPropertyTypes() {

		List<PropertyTypeEntity> propertyTypes = propertyTypeService.getPropertyTypes();
		List<PropertyTypeEntity> uniquePropertyTypes = new ArrayList<>(propertyTypes.size());
		for (PropertyTypeEntity pType : propertyTypes) {
			int pos = uniquePropertyTypes.indexOf(pType);
			if (pos > -1){
				uniquePropertyTypes.get(pos).addPropertyTag(pType.getPropertyTags().get(0));
			}
			else {
				uniquePropertyTypes.add(pType);
			}
		}
		return uniquePropertyTypes;
	}

	public boolean doRemovePropertyType(PropertyTypeEntity propertyType) {
		boolean isSuccessful = false;
		try {
			if (propertyType == null) {
				String message = "No property type selected.";
				GlobalMessageAppender.addErrorMessage(message);
			} else {
				try{
					propertyTypeScreenService.deletePropertyTypeById(propertyType.getId());
					String message = "Property type: " + propertyType.getPropertyTypeName() + " successfully deleted.";
					GlobalMessageAppender.addSuccessMessage(message);
					isSuccessful = true;
				}catch(EJBException e){
					if(e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (PropertyTypeNotFoundException e) {
			GlobalMessageAppender.addErrorMessage("Property type has not been found.");
		} catch (PropertyTypeNotDeletableException e) {
			GlobalMessageAppender.addErrorMessage("Could not delete Property type because it is used by properties.");
		}

		return isSuccessful;
	}

	public boolean doSave(Integer prtTypeId, String prtTypeName, String prtTypeValidation, boolean encrypted, String prtTypeTagsString) {
		boolean isSuccessful = false;

		try {
			if (prtTypeId == null) {
				String message = "No property type id selected.";
				GlobalMessageAppender.addErrorMessage(message);
			} else if (prtTypeName == null) {
				String message = "No property type name selected.";
				GlobalMessageAppender.addErrorMessage(message);
			} else if (prtTypeValidation == null) {
				String message = "Property type validation must not be empty.";
				GlobalMessageAppender.addErrorMessage(message);
			} else if (checkIfRegexpSyntaxError(prtTypeValidation)) {
				String message = "Invalid property type validation pattern.";
				GlobalMessageAppender.addErrorMessage(message);
			} else {
				try{
					propertyTypeScreenService.updatePropertyType(prtTypeId, prtTypeName, prtTypeValidation, encrypted, prtTypeTagsString);
					String message = "The Property Type: " + prtTypeName + " successfully saved.";
					GlobalMessageAppender.addSuccessMessage(message);
					isSuccessful = true;
				}catch(EJBException e){
					if(e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (PropertyTypeNotFoundException e) {
			GlobalMessageAppender.addErrorMessage("The property type has not been found.");
		} catch (RenameException e) {
			GlobalMessageAppender.addErrorMessage("Was not able to rename the property type.");
		} catch (ElementAlreadyExistsException e) {
			GlobalMessageAppender.addErrorMessage("Was not able to save the property type.");
		}

		return isSuccessful;
	}

	public boolean doCreateByPropertyType(String prtTypeName, String prtTypeValidation, boolean encrypted, String propertyTypeTags) {
		boolean isSuccessful = false;

		try {
			if (prtTypeName == null) {
				String message = "Could not read name for new propertytype.";
				GlobalMessageAppender.addErrorMessage(message);
			} else if (prtTypeName == null || prtTypeName.isEmpty()) {
				String message = "The name for the propertytype must not be empty.";
				GlobalMessageAppender.addErrorMessage(message);
			}else if (prtTypeValidation==null || prtTypeValidation.isEmpty()) {
				String message = "The validation for the propertytype must not be empty.";
				GlobalMessageAppender.addErrorMessage(message);
			}else if (checkIfRegexpSyntaxError(prtTypeValidation)) {
				String message = "Invalid property type validation pattern.";
				GlobalMessageAppender.addErrorMessage(message);
			} else {
				try{
					propertyTypeScreenService.createPropertyTypeByNameAndVal(prtTypeName, prtTypeValidation, encrypted, propertyTypeTags);
					String message = "Property type " + prtTypeName + " succesfully created.";
					GlobalMessageAppender.addSuccessMessage(message);
					isSuccessful = true;
				}catch(EJBException e){
					if(e.getCause() instanceof NotAuthorizedException) {
						GlobalMessageAppender.addErrorMessage(e.getCause().getMessage());
					} else {
						throw e;
					}
				}
			}
		} catch (ElementAlreadyExistsException e) {
			GlobalMessageAppender.addErrorMessage("Property type already exists.");
		}

		return isSuccessful;
	}
	
	private boolean checkIfRegexpSyntaxError(String regexp){
		boolean result = false;
		try{
			"".matches(regexp);
			} catch (PatternSyntaxException e){
				result = true;
			}
		return result;
	}

}
