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

import ch.puzzle.itc.mobiliar.business.environment.entity.AbstractContext;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.AbstractResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import ch.puzzle.itc.mobiliar.business.template.entity.TemplateDescriptorEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWRuntimeException;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException.CAUSE;
import ch.puzzle.itc.mobiliar.common.util.ConfigurationService;
import ch.puzzle.itc.mobiliar.common.util.ConfigKey;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

public class TemplateUtils {

	static final Logger LOG = Logger.getLogger(TemplateUtils.class.getName());
	/*************************************** START RELATED RESOURCES **********************************************/

	/**
	 * Returns all templates as well as their property descriptors and
	 * (prioritized) values.
	 * 
	 * @param relation
	 * @param context
	 * @return
	 */
	public static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(AbstractResourceRelationEntity relation, ContextEntity context) {
		return getPropertyValues(relation, getPropertyDescriptors(context, relation, null), context);
	}

	/**
	 * Loads all templates for a related resource in a specific context
	 * 
	 * @param context
	 * @param resourceRelation
	 * @param resultMap
	 *            (used for recursion, can initially be defined as null)
	 * @return
	 */
	public static Set<TemplateDescriptorEntity> getTemplates(ContextEntity context, AbstractResourceRelationEntity resourceRelation, Set<TemplateDescriptorEntity> result) {
		if (result == null) {
			result = new HashSet<>();
		}
		getTemplatesForContext(context, resourceRelation, result);
		if (context.getParent() != null) {
			result = getTemplates(context.getParent(), resourceRelation, result);
		}
		return result;
	}

	public static Set<PropertyDescriptorEntity> getPropertyDescriptors(ContextEntity context, AbstractResourceRelationEntity resourceRelation, Set<PropertyDescriptorEntity> result) {
		if (result == null) {
			result = new HashSet<>();
		}
		getPropertyDescriptorsForContext(context, resourceRelation, result);
		if (context.getParent() != null) {
			result = getPropertyDescriptors(context.getParent(), resourceRelation, result);
		}
		return result;
	}

	private static void getTemplatesForContext(ContextEntity context, AbstractResourceRelationEntity resourceRelation, Set<TemplateDescriptorEntity> result) {
		if (resourceRelation != null) {
			// Get templates of resource relations
			if (resourceRelation.getContexts() != null) {
				for (ResourceRelationContextEntity resourceRelationContext : resourceRelation.getContexts()) {
					if (resourceRelationContext.getContext() != null && resourceRelationContext.getContext().getId().equals(context.getId())) {
						result = collectTemplateDescriptors(resourceRelationContext, result);
					}
				}
			}
			if (resourceRelation.getResourceRelationType() != null) {
				for (ResourceRelationTypeEntity relationType : getAllResourceRelationTypes(resourceRelation.getResourceRelationType().getResourceTypeA(), resourceRelation.getResourceRelationType()
						.getResourceTypeB())) {
					if (relationType.getContexts() != null) {
						for (ResourceRelationTypeContextEntity resourceRelationTypeContext : resourceRelation.getResourceRelationType().getContexts()) {
							if (resourceRelationTypeContext.getContext() != null && resourceRelationTypeContext.getContext().getId().equals(context.getId())) {
								result = collectTemplateDescriptors(resourceRelationTypeContext, result);
							}
						}
					}
				}
			}
			getTemplatesForContext(context, resourceRelation.getSlaveResource(), result);
		}
	}

	private static void getPropertyDescriptorsForContext(ContextEntity context, AbstractResourceRelationEntity resourceRelation, Set<PropertyDescriptorEntity> result) {
		if (resourceRelation != null) {
			// Get templates of resource relations
			if (resourceRelation.getContexts() != null) {
				for (ResourceRelationContextEntity resourceRelationContext : resourceRelation.getContexts()) {
					if (resourceRelationContext.getContext() != null && resourceRelationContext.getContext().getId().equals(context.getId())) {
						Set<PropertyDescriptorEntity> properties = resourceRelationContext.getPropertyDescriptors();
						if (properties != null) {
							result.addAll(properties);
						}
					}
				}
			}
			if (resourceRelation.getResourceRelationType() != null) {
				for (ResourceRelationTypeEntity relationType : getAllResourceRelationTypes(resourceRelation.getResourceRelationType().getResourceTypeA(), resourceRelation.getResourceRelationType()
						.getResourceTypeB())) {
					if (relationType.getContexts() != null) {
						for (ResourceRelationTypeContextEntity resourceRelationTypeContext : resourceRelation.getResourceRelationType().getContexts()) {
							if (resourceRelationTypeContext.getContext() != null && resourceRelationTypeContext.getContext().getId().equals(context.getId())) {
								Set<PropertyDescriptorEntity> properties = resourceRelationTypeContext.getPropertyDescriptors();
								if (properties != null) {
									result.addAll(properties);
								}
							}
						}
					}
				}
			}
			getPropertyDescriptorsForContext(context, resourceRelation.getSlaveResource(), result);
		}
	}

	public static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(AbstractResourceRelationEntity relation, Set<PropertyDescriptorEntity> properties, ContextEntity context) {
		Map<PropertyDescriptorEntity, List<PropertyEntity>> propertiesWithValues = new LinkedHashMap<>();
		for (PropertyDescriptorEntity propDescr : properties) {
			propertiesWithValues.put(propDescr, getValueForProperty(relation, propDescr, context));
		}
		return propertiesWithValues;
	}

	/**
	 * Gets the property value(s) of a specified property descriptor for a
	 * related resource
	 * 
	 * @param relation
	 * @param propDescr
	 * @param context
	 * @return
	 */
	public static List<PropertyEntity> getValueForProperty(AbstractResourceRelationEntity relation, PropertyDescriptorEntity propDescr, ContextEntity context) {
		// Die Liste soll nicht mehr veränderbar sein (die Reihenfolge muss
		// gewährleistet bleiben)
		return Collections.unmodifiableList(getValueForProperty(relation, propDescr, context, new ArrayList<PropertyEntity>()));
	}

	private static List<PropertyEntity> getValueForProperty(AbstractResourceRelationEntity relation, PropertyDescriptorEntity propDescr, ContextEntity context, List<PropertyEntity> result) {
		assert (context != null);
		assert (relation != null);

		// 1. Ressourcenbeziehung
		addResourceRelationEntityPropertiesToResult(relation, propDescr, context, result);

		// 2. Ressourcenbeziehungs-Ressourcentyp
		if (relation.getResourceRelationType() != null) {
			for (ResourceRelationTypeEntity relationType : getAllResourceRelationTypes(relation.getResourceRelationType().getResourceTypeA(), relation.getResourceRelationType().getResourceTypeB())) {
				addResourceRelationTypeEntityPropertiesToResult(relationType, propDescr, context, result);
			}
		}

		// 3. Abhängige Ressource, Ressourcentyp und Kontext
		return getValueForProperty(relation.getSlaveResource(), propDescr, context, result);
	}

	/*************************************** END RELATED RESOURCES **********************************************/

	/*************************************** START RESOURCE **********************************************/

	/**
	 * Returns all templates as well as their property descriptors and (prioritized) values.
	 * 
	 * @param resource
	 * @param context
	 * @param templateExceptionHandler
	 * @return
	 */
	public static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(ResourceEntity resource, ContextEntity context,
			boolean validate, AMWTemplateExceptionHandler templateExceptionHandler) {
		Set<PropertyDescriptorEntity> properties = getPropertyDescriptors(context, resource, null);
		return getPropertyValues(resource, properties, context, validate, templateExceptionHandler);
	}

	/**
	 * Loads all templates for a related resource in a specific context
	 * 
	 * @param context
	 * @param resourceRelation
	 * @param resultMap
	 *            (used for recursion, can initially be defined as null)
	 * @return
	 */
	public static Set<TemplateDescriptorEntity> getTemplates(ContextEntity context, ResourceEntity resource, Set<TemplateDescriptorEntity> result) {
		if (result == null) {
			result = new HashSet<>();
		}
		getTemplatesForContext(context, resource, result);
		if (context.getParent() != null) {
			result = getTemplates(context.getParent(), resource, result);
		}
		return result;
	}

	public static Set<PropertyDescriptorEntity> getPropertyDescriptors(ContextEntity context, ResourceEntity resource, Set<PropertyDescriptorEntity> result) {
		if (result == null) {
			result = new HashSet<>();
		}
		getPropertyDescriptorsForContext(context, resource, result);
		if (context.getParent() != null) {
			result = getPropertyDescriptors(context.getParent(), resource, result);
		}
		return result;
	}

	private static void getTemplatesForContext(ContextEntity context, ResourceEntity resource, Set<TemplateDescriptorEntity> result) {
		if (resource != null) {
			if (resource.getContexts() != null) {
				for (ResourceContextEntity relatedResourceContext : resource.getContexts()) {
					if (relatedResourceContext.getContext() != null && relatedResourceContext.getContext().getId().equals(context.getId())) {
						result = collectTemplateDescriptors(relatedResourceContext, result);
					}
				}
			}
			getTemplatesForContext(context, resource.getResourceType(), result);
		}
	}

	private static void getPropertyDescriptorsForContext(ContextEntity context, ResourceEntity resource, Set<PropertyDescriptorEntity> result) {
		if (resource != null) {
			if (resource.getContexts() != null) {
				for (ResourceContextEntity relatedResourceContext : resource.getContexts()) {
					if (relatedResourceContext.getContext() != null && relatedResourceContext.getContext().getId().equals(context.getId())) {
						Set<PropertyDescriptorEntity> properties = relatedResourceContext.getPropertyDescriptors();
						if (properties != null) {
							result.addAll(properties);
						}
					}
				}
			}
			getPropertyDescriptorsForContext(context, resource.getResourceType(), result);
		}
	}

	/**
	 * Gets the property values for a related resource
	 * 
	 * @param relation
	 * @param templates
	 * @param context
	 * @param templateExceptionHandler
	 * @return
	 */
	public static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(ResourceEntity resource,
			Set<PropertyDescriptorEntity> properties, ContextEntity context, boolean validate, AMWTemplateExceptionHandler templateExceptionHandler) {
		Map<PropertyDescriptorEntity, List<PropertyEntity>> result = new LinkedHashMap<>();
		for (PropertyDescriptorEntity propDescr : properties) {
			result.put(propDescr, getValueForProperty(resource, propDescr, context, validate, templateExceptionHandler));
		}
		return result;
	}

	/**
	 * Gets the property value(s) of a specified property descriptor for a resource
	 * 
	 * @param relation
	 * @param propDescr
	 * @param context
	 * @param templateExceptionHandler
	 * @return
	 */
	public static List<PropertyEntity> getValueForProperty(ResourceEntity resource, PropertyDescriptorEntity propDescr, ContextEntity context,
			boolean validate, AMWTemplateExceptionHandler templateExceptionHandler) {
		// Die Liste soll nicht mehr veränderbar sein (die Reihenfolge muss
		// gewährleistet bleiben)
		List<PropertyEntity> properties = getValueForProperty(resource, propDescr, context, new ArrayList<PropertyEntity>());
		if (validate) {
			String validationLogic = propDescr.getValidationLogic();

			if (validationLogic == null) {
				validationLogic = propDescr.getPropertyTypeEntity() != null ? propDescr.getPropertyTypeEntity().getValidationRegex() : null;
			}

			if (validationLogic != null && !properties.isEmpty() && !properties.get(0).getValue().matches(validationLogic)) {
				templateExceptionHandler.addTemplatePropertyException(new TemplatePropertyException("Property " + propDescr.getPropertyName()
						+ " has an invalid value: \"" + properties.get(0).getValue() + "\" does not match \"" + validationLogic + "\".",
						CAUSE.INVALID_PROPERTYVALUE));
			}
		}
		return Collections.unmodifiableList(properties);
	}

	private static List<PropertyEntity> getValueForProperty(ResourceEntity resource, PropertyDescriptorEntity propDescr, ContextEntity context, List<PropertyEntity> result) {
		assert (context != null);
		assert (resource != null);
		assert (resource.getResourceType() != null);

		// 1. Ressource
		addResourceContextEntityPropertiesToResult(resource, propDescr, context, result);

		// 2. Ressourcentyp und Kontext
		return getValueForProperty(resource.getResourceType(), propDescr, context, result);
	}

	/*************************************** END RESOURCE **********************************************/

	/*************************************** START RELATED RESOURCE TYPES **********************************************/

	/**
	 * Returns all templates as well as their property descriptors and
	 * (prioritized) values.
	 * 
	 * @param relation
	 * @param context
	 * @return
	 */
	public static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(ResourceTypeEntity resourceType, ResourceTypeEntity relatedResource, ContextEntity context) {
		Set<PropertyDescriptorEntity> properties = getPropertyDescriptors(context, relatedResource, null);
		return getPropertyValues(resourceType, relatedResource, properties, context);
	}

	/**
	 * Gets the property values of the given templates for a related resource
	 * 
	 * @param resourceType
	 * @param relatedResourceType
     * @param properties
	 * @param context
	 * @return
	 */
	private static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(ResourceTypeEntity resourceType, ResourceTypeEntity relatedResourceType,
			Set<PropertyDescriptorEntity> properties, ContextEntity context) {
		Map<PropertyDescriptorEntity, List<PropertyEntity>> result = new LinkedHashMap<>();
		for (PropertyDescriptorEntity propDescr : properties) {
			result.put(propDescr, getValueForProperty(resourceType, relatedResourceType, propDescr, context));
		}
		return result;
	}

	/**
	 * Gets the property value(s) of a specified property descriptor for a
	 * related resource
	 * 
	 * @param relation
	 * @param propDescr
	 * @param context
	 * @return
	 */
	private static List<PropertyEntity> getValueForProperty(ResourceTypeEntity resourceType, ResourceTypeEntity relatedResourceType, PropertyDescriptorEntity propDescr, ContextEntity context) {
		// Die Liste soll nicht mehr veränderbar sein (die Reihenfolge muss
		// gewährleistet bleiben)
		return Collections.unmodifiableList(getValueForProperty(resourceType, relatedResourceType, propDescr, context, new ArrayList<PropertyEntity>()));
	}

	private static List<PropertyEntity> getValueForProperty(ResourceTypeEntity resourceType, ResourceTypeEntity relatedResourceType, PropertyDescriptorEntity propDescr, ContextEntity context,
			List<PropertyEntity> result) {
		assert (context != null);
		assert (resourceType != null);
		assert (relatedResourceType != null);

		// 1. Ressourcenbeziehungs-Ressourcentyp (wenn vorhanden)
		for (ResourceRelationTypeEntity relationType : getAllResourceRelationTypes(resourceType, relatedResourceType)) {
			addResourceRelationTypeEntityPropertiesToResult(relationType, propDescr, context, result);
		}

		// 2. Abhängiger Ressourcentyp und Kontext
		return getValueForProperty(relatedResourceType, propDescr, context, result);
	}

	/*************************************** END RELATED RESOURCE TYPES **********************************************/

	/*************************************** START RESOURCE TYPE **********************************************/

	/**
	 * Returns all templates as well as their property descriptors and
	 * (prioritized) values.
	 * 
	 * @param relation
	 * @param context
	 * @return
	 */
	public static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(ResourceTypeEntity resourceType, ContextEntity context) {
		Set<PropertyDescriptorEntity> properties = getPropertyDescriptors(context, resourceType, null);
		return getPropertyValues(resourceType, properties, context);
	}

	/**
	 * Loads all templates for a resource type in a specific context
	 * 
	 * @param context
	 * @param resourceRelation
	 * @param resultMap
	 *            (used for recursion, can initially be defined as null)
	 * @return
	 */
	public static Set<PropertyDescriptorEntity> getPropertyDescriptors(ContextEntity context, ResourceTypeEntity resourceType, Set<PropertyDescriptorEntity> result) {
		if (result == null) {
			result = new HashSet<>();
		}
		getPropertyDescriptorsForContext(context, resourceType, result);
		if (context.getParent() != null) {
			result = getPropertyDescriptors(context.getParent(), resourceType, result);
		}
		return result;
	}

	public static Set<TemplateDescriptorEntity> getTemplates(ContextEntity context, ResourceTypeEntity resourceType, Set<TemplateDescriptorEntity> result) {
		if (result == null) {
			result = new HashSet<>();
		}
		getTemplatesForContext(context, resourceType, result);
		if (context.getParent() != null) {
			result = getTemplates(context.getParent(), resourceType, result);
		}
		return result;
	}

	private static void getTemplatesForContext(ContextEntity context, ResourceTypeEntity resourceType, Set<TemplateDescriptorEntity> result) {
		if (resourceType != null) {
			ResourceTypeEntity resType = resourceType;
			while (resType != null) {
				if (resType.getContexts() != null) {
					// Get templates of resource type context
					for (ResourceTypeContextEntity resourceTypeContext : resType.getContexts()) {
						if (resourceTypeContext.getContext() != null && resourceTypeContext.getContext().getId().equals(context.getId())) {
							result = collectTemplateDescriptors(resourceTypeContext, result);
						}
					}
				}
				resType = resType.getParentResourceType();
			}
		}
	}

	private static void getPropertyDescriptorsForContext(ContextEntity context, ResourceTypeEntity resourceType, Set<PropertyDescriptorEntity> result) {
		if (resourceType != null) {
			ResourceTypeEntity resType = resourceType;
			while (resType != null) {
				if (resType.getContexts() != null) {
					// Get templates of resource type context
					for (ResourceTypeContextEntity resourceTypeContext : resType.getContexts()) {
						if (resourceTypeContext.getContext() != null && resourceTypeContext.getContext().getId().equals(context.getId())) {
							Set<PropertyDescriptorEntity> properties = resourceTypeContext.getPropertyDescriptors();
							if (properties != null) {
								result.addAll(properties);
							}
						}
					}
				}
				resType = resType.getParentResourceType();
			}
		}
	}

	/**
	 * Gets the property values of the given context for a resource type
	 * 
	 * @param resourceType
	 * @param properties
	 * @param context
	 * @return
	 */
	public static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(ResourceTypeEntity resourceType, Set<PropertyDescriptorEntity> properties, ContextEntity context) {
		Map<PropertyDescriptorEntity, List<PropertyEntity>> result = new LinkedHashMap<>();
		for (PropertyDescriptorEntity propDescr : properties) {
			result.put(propDescr, getValueForProperty(resourceType, propDescr, context));
		}
		return result;
	}

	/**
	 * Gets the property value(s) of a specified property descriptor for a
	 * resource type
	 * 
	 * @param resourceType
	 * @param propDescr
	 * @param context
	 * @return
	 */
	public static List<PropertyEntity> getValueForProperty(ResourceTypeEntity resourceType, PropertyDescriptorEntity propDescr, ContextEntity context) {
		// Die Liste soll nicht mehr veränderbar sein (die Reihenfolge muss
		// gewährleistet bleiben)
		return Collections.unmodifiableList(getValueForProperty(resourceType, propDescr, context, new ArrayList<PropertyEntity>()));
	}

	private static List<PropertyEntity> getValueForProperty(ResourceTypeEntity resourceType, PropertyDescriptorEntity propDescr, ContextEntity context, List<PropertyEntity> result) {
		assert (context != null);
		assert (resourceType != null);

		// 1. Ressourcentyp
		ResourceTypeEntity resType = resourceType;
		while (resType != null) {
			addResourceTypeContextEntityPropertiesToResult(resType, propDescr, context, result);
			resType = resType.getParentResourceType();
		}
		// 2. Kontext
		return getValueForProperty(propDescr, context, result);
	}

	/*************************************** END RESOURCE TYPE **********************************************/

	/*************************************** START CONTEXT **********************************************/

	/**
	 * Returns all templates as well as their property descriptors and
	 * (prioritized) values.
	 *
	 * @param context
	 * @return
	 */
	public static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(ContextEntity context) {
		Set<PropertyDescriptorEntity> templates = getPropertyDescriptors(context, null);
		return getPropertyValues(templates, context);
	}

	private static Set<PropertyDescriptorEntity> getPropertyDescriptors(ContextEntity context, Set<PropertyDescriptorEntity> result) {
		if (result == null) {
			result = new HashSet<>();
		}
		getPropertyDescriptorsForContext(context, result);
		if (context.getParent() != null) {
			result = getPropertyDescriptors(context.getParent(), result);
		}
		return result;
	}

	private static void getPropertyDescriptorsForContext(ContextEntity context, Set<PropertyDescriptorEntity> result) {
		Set<PropertyDescriptorEntity> properties = context.getPropertyDescriptors();
		if (properties != null) {
			result.addAll(properties);
		}
	}

	private static Map<PropertyDescriptorEntity, List<PropertyEntity>> getPropertyValues(Set<PropertyDescriptorEntity> properties, ContextEntity context) {
		Map<PropertyDescriptorEntity, List<PropertyEntity>> result = new LinkedHashMap<>();
		for (PropertyDescriptorEntity propDescr : properties) {
			result.put(propDescr, getValueForProperty(propDescr, context));
		}
		return result;
	}

	/**
	 * Gets the property value(s) of a specified property descriptor for a
	 * context
	 *
	 * @param propDescr
	 * @param context
	 * @return
	 */
	public static List<PropertyEntity> getValueForProperty(PropertyDescriptorEntity propDescr, ContextEntity context) {
		// Die Liste soll nicht mehr veränderbar sein (die Reihenfolge muss
		// gewährleistet bleiben)
		return Collections.unmodifiableList(getValueForProperty(propDescr, context, new ArrayList<PropertyEntity>()));
	}

	private static List<PropertyEntity> getValueForProperty(PropertyDescriptorEntity propDescr, ContextEntity context, List<PropertyEntity> result) {
		assert (context != null);
		ContextEntity c = context;
		context.addPropertiesToListByDescriptor(result, propDescr);
		context.getContextType().addPropertiesToListByDescriptor(result, propDescr);
		while ((c = c.getParent()) != null) {
			context.addPropertiesToListByDescriptor(result, propDescr);
			context.getContextType().addPropertiesToListByDescriptor(result, propDescr);
		}
		return result;
	}

	/*************************************** END CONTEXT **********************************************/

	/*************************************** START HELPER METHODS **********************************************/


	/**
	 * Decrypts the given String
	 * 
	 * @param value
	 * @return decrypted String
	 */
	public static String decrypt(String value) {
		if (value != null) {
			try {
				Cipher c = Cipher.getInstance("AES");
				String key = ConfigurationService.getProperty(ConfigKey.ENCRYPTION_KEY);
				if (key == null) {
					throw new AMWRuntimeException("No encryption key defined!");
				}
				SecretKeySpec k = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
				c.init(Cipher.DECRYPT_MODE, k);
				return new String(c.doFinal(Base64.decodeBase64(value)), StandardCharsets.UTF_8);
			} catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | NoSuchAlgorithmException e) {
				throw new AMWRuntimeException("Was not able to decrypt properties", e);
			}
		}
		return null;
	}


	/**
	 * Encrypts the given String
	 * @param value
	 * @return the encrypted String
	 */
	public static String encrypt(String value) {
		if (value != null) {
			try {
				Cipher c = Cipher.getInstance("AES");

				String key = ConfigurationService.getProperty(ConfigKey.ENCRYPTION_KEY);
				if (key == null) {
					throw new AMWRuntimeException("No encryption key defined!");
				}
				SecretKeySpec k = new SecretKeySpec(key.getBytes("UTF8"), "AES");
				c.init(Cipher.ENCRYPT_MODE, k);

				return new String(Base64.encodeBase64(c.doFinal(value.getBytes("UTF8"))), "UTF8");
			}
			catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | UnsupportedEncodingException | NoSuchPaddingException e) {
				throw new AMWRuntimeException("Was not able to encrypt properties", e);
			}
		}
		return null;
	}

	private static Set<TemplateDescriptorEntity> collectTemplateDescriptors(AbstractContext context, Set<TemplateDescriptorEntity> result) {
		if (context.getTemplates() != null) {
			for (TemplateDescriptorEntity template : context.getTemplates()) {
				template.setOwnerResource(context);
				result.add(template);
			}
		}
		return result;
	}

	private static void addResourceTypeContextEntityPropertiesToResult(ResourceTypeEntity resourceTypeEntity, PropertyDescriptorEntity propDescr, ContextEntity context, List<PropertyEntity> result) {
		for (ResourceTypeContextEntity resourceTypeContext : resourceTypeEntity.getContextsByLowestContext(context)) {
			if (resourceTypeContext != null) {
				resourceTypeContext.addPropertiesToListByDescriptor(result, propDescr);
			}
		}
		if (resourceTypeEntity.getParentResourceType() != null) {
			addResourceTypeContextEntityPropertiesToResult(resourceTypeEntity.getParentResourceType(), propDescr, context, result);
		}
	}

	private static void addResourceContextEntityPropertiesToResult(ResourceEntity resourceEntity, PropertyDescriptorEntity propDescr, ContextEntity context, List<PropertyEntity> result) {
		for (ResourceContextEntity resourceContext : resourceEntity.getContextsByLowestContext(context)) {
			if (resourceContext != null) {
				resourceContext.addPropertiesToListByDescriptor(result, propDescr);
			}
		}
	}

	private static void addResourceRelationEntityPropertiesToResult(AbstractResourceRelationEntity resourceRelationEntity, PropertyDescriptorEntity propDescr, ContextEntity context,
			List<PropertyEntity> result) {
		for (ResourceRelationContextEntity resourceRelContext : resourceRelationEntity.getContextsByLowestContext(context)) {
			if (resourceRelContext != null) {
				resourceRelContext.addPropertiesToListByDescriptor(result, propDescr);
			}
		}
	}

	private static void addResourceRelationTypeEntityPropertiesToResult(ResourceRelationTypeEntity resourceRelationTypeEntity, PropertyDescriptorEntity propDescr, ContextEntity context,
			List<PropertyEntity> result) {
		for (ResourceRelationTypeContextEntity resourceRelationTypeContext : resourceRelationTypeEntity.getContextsByLowestContext(context)) {
			if (resourceRelationTypeContext != null) {
				resourceRelationTypeContext.addPropertiesToListByDescriptor(result, propDescr);
			}
		}
	}

	public static Set<ResourceRelationTypeEntity> getAllResourceRelationTypes(ResourceTypeEntity resTypeA, ResourceTypeEntity resTypeB) {
		Set<ResourceRelationTypeEntity> result = new LinkedHashSet<>();
		
		List<Integer> resourceTypeBIds = new ArrayList<>();
		ResourceTypeEntity b = resTypeB;
		while (b != null) {
			resourceTypeBIds.add(b.getId());
			b = b.getParentResourceType();
		}

		ResourceTypeEntity a = resTypeA;
		while (a != null) {
			if (a.getResourceRelationTypesA() != null) {
				for (ResourceRelationTypeEntity rel : a.getResourceRelationTypesA()) {
					if (resourceTypeBIds.contains(rel.getResourceTypeB().getId())) {
						result.add(rel);
					}
				}
			}
			a = a.getParentResourceType();
		}
		return result;
	}



	/*************************************** END HELPER METHODS **********************************************/
}
