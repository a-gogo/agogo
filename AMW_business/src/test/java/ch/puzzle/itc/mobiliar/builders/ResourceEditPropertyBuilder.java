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

package ch.puzzle.itc.mobiliar.builders;

import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditProperty;

public class ResourceEditPropertyBuilder {

	private String technicalKey = null;
	private String displayName = null;
	private String propertyValue = null;
	private String propertyComment = null;
	private boolean isNullable = false;
    private boolean isOptional = false;
	private boolean isEncrypted = false;
	private Integer cardinalityProperty = null;
	private boolean isTesting = false;
	private String validationLogic = null;
    private String mik = null;
	private Integer propContextId = null;
	private Integer typeContextId = null;
	private Integer descriptorId = null;
	private String propContName = null;
	private String typeContName = null;
	private Integer typeId = null;
    private Integer propertyValueTypeId = null;
	private Integer masterTypeId = null;
	private String typeName = null;
	private String validationRegex = null;
	private Integer propertyId = null;
	private String origin = null;
	private String loadedFor = null;
	private String resourceName = null;
	private String exampleValue = null;
	private String defaultValue = null;
    private String descriptorOrigin = null;
    private String fcOwner = null;
    private String fcExternalKey = null;
    private String fcExternalLink = null;


	public ResourceEditPropertyBuilder() {
	}


    /**
     * technical key name
     */
    public ResourceEditPropertyBuilder withTechnicalKey(String technicalKey) {
        this.technicalKey = technicalKey;
        return this;
    }

    /**
     * display name
     */
    public ResourceEditPropertyBuilder withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * Comment on propertyDescriptor
     */
    public ResourceEditPropertyBuilder withComment(String propertyComment) {
        this.propertyComment = propertyComment;
        return this;
    }

    /**
     * Indicate if property is nullable
     */
    public ResourceEditPropertyBuilder withIsNullable(boolean isNullable) {
        this.isNullable = isNullable;
        return this;
    }

    /**
     * Indicate if property is encrypted
     */
    public ResourceEditPropertyBuilder withIsEncrypted(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
        return this;
    }

    /**
     * Cardinality of property
     */
    public ResourceEditPropertyBuilder withCardinality(Integer cardinalityProperty) {
        this.cardinalityProperty = cardinalityProperty;
        return this;
    }

    /**
     * Indicate if property is in testing mode
     */
    public ResourceEditPropertyBuilder withIsTesting(boolean isTesting) {
        this.isTesting = isTesting;
        return this;
    }

    /**
     * Validation logic for property
     */
    public ResourceEditPropertyBuilder withValidationLogic(String validationLogic) {
        this.validationLogic = validationLogic;
        return this;
    }

    /**
     * Id of property descriptor
     */
    public ResourceEditPropertyBuilder withDescriptorId(Integer descriptorId) {
        this.descriptorId = descriptorId;
        return this;
    }


    /**
     * Sets the display name and technical key with same value
     */
    public ResourceEditPropertyBuilder withDisplayAndTechKeyName(String name) {
        this.displayName = name;
        this.technicalKey = name;
        return this;
    }

    /**
     * the value in the given context
     */
	public ResourceEditPropertyBuilder withValue(String value) {
        this.propertyValue = value;
        return this;
    }

    /**
     * the id of the context where the property is defined or null if defined on a resource type
     */
        public ResourceEditPropertyBuilder withPropContextId(Integer propContextId) {
            this.propContextId = propContextId;
            return this;
        }


    /**
     * the id of the type-context where the property is defined or null if defined on a resource
     */
    public ResourceEditPropertyBuilder withPropertyContextTypeId(Integer contextTypeId) {
        this.typeContextId = contextTypeId;
        return this;
    }


    /**
     * the name of the context where the property is defined
     */
    public ResourceEditPropertyBuilder withPropContName(String propContName) {
        this.propContName = propContName;
        return this;
    }

    /**
     * the name of the context where the property is defined
     */
    public ResourceEditPropertyBuilder withPropTypeContName(String propTypeContName) {
        this.typeContName = propTypeContName;
        return this;
    }

    /**
     * the id of the resource type on which the property is defined
     */
    public ResourceEditPropertyBuilder withTypeId(Integer typeId) {
        this.typeId = typeId;
        return this;
    }

    public ResourceEditPropertyBuilder withMasterResTypeId(Integer masterResTypeId) {
        this.masterTypeId = masterResTypeId;
        return this;
    }


    /**
     * the name of the resource type on which the property is defined or null if the property is defined on the resource
     */
    public ResourceEditPropertyBuilder withMasterResTypeName(String resTypeName) {
        this.typeName = resTypeName;
        return this;
    }

    /**
     * the id of the property value
     */
    public ResourceEditPropertyBuilder withPropertyId(Integer propId) {
        this.propertyId = propId;
        return this;
    }


    /**
     * constant to define if the property is set on 'instance' or 'relation'
     */
    public ResourceEditPropertyBuilder withOrigin(ResourceEditProperty.Origin origin) {
        this.origin = origin.name();
        return this;
    }

    /**
     * the validation regex if any
     */
    public ResourceEditPropertyBuilder withValidationRegex(String validationRegex) {
        this.validationRegex = validationRegex;
        return this;
    }

    /**
     * Loaded for what kind of type
     */
    public ResourceEditPropertyBuilder withLoadedFor(ResourceEditProperty.Origin loadedFor) {
        this.loadedFor = loadedFor.name();
        return this;
    }

    /**
     * Resource name
     */
    public ResourceEditPropertyBuilder withResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }


    public ResourceEditPropertyBuilder withExampleValue(String exampleValue) {
        this.exampleValue = exampleValue;
        return this;
    }

    public ResourceEditPropertyBuilder withDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * constant to define if the property is set on 'instance' or 'relation'
     */
    public ResourceEditPropertyBuilder withDescriptorOrigin(ResourceEditProperty.Origin descriptorOrigin) {
        this.descriptorOrigin = descriptorOrigin.name();
        return this;
    }

    /**
     * foreignable owner
     */
    public ResourceEditPropertyBuilder withFcOwner(ForeignableOwner fcOwner) {
        this.fcOwner = fcOwner.name();
        return this;
    }

    /**
     * foreignable external link
     */
    public ResourceEditPropertyBuilder withFcExternalLink(String fcExternalLink) {
        this.fcExternalLink = fcExternalLink;
        return this;
    }

    /**
     * foreignable external link
     */
    public ResourceEditPropertyBuilder withFcExternalKey(String fcExternalKey) {
        this.fcExternalKey = fcExternalKey;
        return this;
    }

    /**
     * Build with descriptor
     */
    public ResourceEditPropertyBuilder withDescriptor(PropertyDescriptorEntity descriptor) {
        this.technicalKey = descriptor.getPropertyName();
        this.displayName = descriptor.getDisplayName();
        this.propertyComment = descriptor.getPropertyComment();
        this.isNullable = descriptor.isNullable();
        this.isOptional = descriptor.isOptional();
        this.isEncrypted = descriptor.isEncrypt();
        this.cardinalityProperty = descriptor.getCardinalityProperty();
        this.isTesting = descriptor.isTesting();
        this.validationLogic = descriptor.getValidationLogic();
        this.mik = descriptor.getMachineInterpretationKey();
        this.descriptorId = descriptor.getId();

        return this;
    }

	public ResourceEditProperty build() {
		return new ResourceEditProperty(technicalKey, displayName, propertyValue, exampleValue, defaultValue, propertyComment,
				isNullable, isOptional, isEncrypted, cardinalityProperty, isTesting, validationLogic, mik, propContextId,
				typeContextId, descriptorId, propContName, typeContName, typeId, propertyValueTypeId, masterTypeId, typeName,
				validationRegex, propertyId, origin, loadedFor, resourceName, descriptorOrigin, fcOwner, fcExternalKey, fcExternalLink);
	}
}
