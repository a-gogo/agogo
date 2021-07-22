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

package ch.puzzle.itc.mobiliar.business.property.control;

import java.util.List;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import ch.puzzle.itc.mobiliar.business.domain.commons.CommonDomainService;
import ch.puzzle.itc.mobiliar.business.domain.commons.CommonQueries;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyTagEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyTypeEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.security.interceptor.HasPermission;
import ch.puzzle.itc.mobiliar.common.exception.*;

/**
 * @author aromano
 *
 */
// TODO: transfer to PropertyTypeService
@Stateless
public class PropertyTypeScreenDomainService {

	@Inject
	private Logger log;
	
	@Inject
	private CommonDomainService commonService;
	
	@Inject
	private CommonQueries commonQueries;
	
	@Inject
	private EntityManager entityManager;

    @Inject
    private PropertyTagEditingService propertyTagEditingService;

	/**
	 * Speichert/umbenennen den PropertyType anhand den Name und Validation.
	 * @param id
	 * @param name
	 * @param validation
     * @param propertyTypeTagsString
	 * @throws PropertyTypeNotFoundException
	 * @throws ElementAlreadyExistsException
	 * @throws RenameException
	 */
	@HasPermission(permission = Permission.SAVE_SETTINGS_PROPTYPE)
	public void updatePropertyType(int id, String name, String validation, boolean encrypted, String propertyTypeTagsString)
			throws PropertyTypeNotFoundException,
			ElementAlreadyExistsException, RenameException {
		
		PropertyTypeEntity propertyTypeEntity = getPropertyTypeById(id);		
		PropertyTypeEntity propertyByName = getPropertyTypeByName(name);
		
		if (propertyByName != null && !propertyByName.getId().equals(propertyTypeEntity.getId())){
			String message = "An other PropertyType with the same Name already exists.";
			throw new RenameException(message);
		}
		
		propertyTypeEntity.setValidationRegex(validation);
		propertyTypeEntity.setPropertyTypeName(name);
		propertyTypeEntity.setEncrypt(encrypted);

        // Update Tags
        List<PropertyTagEntity> tags = propertyTagEditingService.convertToTags(propertyTypeTagsString);
        propertyTagEditingService.updateTags(tags,propertyTypeEntity);
	}
	
	/**
	 * Erstellt einen neuen PropertyType anhand den Name und Validation..
	 * @param prtName
	 * @param prtValidation
	 * @throws ElementAlreadyExistsException
	 */
	@HasPermission(permission = Permission.ADD_PROPTYPE)
	public PropertyTypeEntity createPropertyTypeByNameAndVal(String prtName,
			String prtValidation, boolean encrypted, String propertyTypeTagsString) throws ElementAlreadyExistsException {
		
		PropertyTypeEntity newPropertyTypeEntity = commonService.getUniquePropertyTypeByName(prtName);
        PropertyTypeEntity result;
		
		if ( newPropertyTypeEntity == null ) {

			result = new PropertyTypeEntity();
            result.setPropertyTypeName(prtName);
            result.setValidationRegex(prtValidation);
			result.setEncrypt(encrypted);

			entityManager.persist(result);

			// Update Tags
			List<PropertyTagEntity> tags = propertyTagEditingService.convertToTags(propertyTypeTagsString);
			propertyTagEditingService.updateTags(tags, result);

			log.info("Property Type " + prtName + " in DB persist");
		} else {
			String message = "Das Property Type mit dem Namen: " + prtName
					+ " ist beretis vorhanden und kann nicht erstellen werden";
			log.info(message);
			throw new ElementAlreadyExistsException(message,
					PropertyTypeEntity.class, prtName);
		}
		return result;
	}

	/**
	 * Löscht den PropertyType anhand der Id.
	 * @param id
	 * @throws PropertyTypeNotFoundException
	 * @throws PropertyTypeNotDeletableException 
	 */
	@HasPermission(permission = Permission.DELETE_PROPTYPE)
	public void deletePropertyTypeById(int id) throws PropertyTypeNotFoundException, PropertyTypeNotDeletableException{
		
		PropertyTypeEntity propertyTypeEntity = getPropertyTypeById(id); 
		if(propertyTypeEntity == null){
			String message = "Der zu löschende Property Type ist nicht vorhanden";
			log.info(message);
			throw new PropertyTypeNotFoundException(message);
		}
		if (!propertyTypeEntity.getPropertyDescriptors().isEmpty()){
			String message = "Propertytype kann nicht gelöscht werden weil dieser von propertydescriptors verwendet wird.";
			log.info(message);
			throw new PropertyTypeNotDeletableException(message);
		}
		
		entityManager.remove(propertyTypeEntity);
		log.info("Property Type mit der Id: " + propertyTypeEntity.getId() + " wurde aus der DB gelöscht");
	}

	private PropertyTypeEntity getPropertyTypeById(int id) throws PropertyTypeNotFoundException {
		PropertyTypeEntity propertyTypeEntity = entityManager.find(PropertyTypeEntity.class, id);
		
		if(propertyTypeEntity == null){
			String message = "Der PropertyType mit der Id: " + id + " existiert nicht auf der DB";
			log.info(message);
			throw new PropertyTypeNotFoundException(message);
		}
		return propertyTypeEntity;
	}
	
	public PropertyTypeEntity getPropertyTypeByName(String propertyTypeName) {
		PropertyTypeEntity propertyType = null;
		try {
			Query searchPropertyTypeQuery = commonQueries.searchPropertyTypeByName(propertyTypeName);
			propertyType = (PropertyTypeEntity) searchPropertyTypeQuery.getSingleResult();
		} catch (NoResultException nre) {
			String message = "Der ResourceType: " + propertyTypeName + " existiert nicht auf der DB";
			log.info(message);
		}

		return propertyType;
	}

}
