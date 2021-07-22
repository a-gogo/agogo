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

import ch.puzzle.itc.mobiliar.business.environment.entity.ContextDependency;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.HasContexts;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditProperty;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import ch.puzzle.itc.mobiliar.business.auditview.control.AuditService;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.List;

/**
 * All the logic required for reading and saving property values
 */
public class PropertyValueService {

    @Inject
    EntityManager entityManager;

    @Inject
    PropertyValidationService propertyValidationService;

    @Inject
    AuditService auditService;


    public void resetPropertyValue(ContextDependency<?> resourceContext, Integer propertyDescriptorId) {
        PropertyEntity property = resourceContext.getPropertyForDescriptor(propertyDescriptorId);
        if (property == null) {
            throw new NoResultException("Property not found");
        }
        resourceContext.removePropertyById(property.getId());
    }

    public void setPropertyValue(ContextDependency<?> resourceContext, Integer propertyDescriptorId, String unobfuscatedValue) throws ValidationException {
        PropertyEntity p = resourceContext.getPropertyForDescriptor(propertyDescriptorId);
        if (p == null) {
            PropertyDescriptorEntity propertyDescriptor = entityManager.find(PropertyDescriptorEntity.class, propertyDescriptorId);
            p = new PropertyEntity();
            p.setDescriptor(propertyDescriptor);
            p.setOwningResource(resourceContext);
            resourceContext.addProperty(p);
        }
        if (!propertyValidationService.canPropertyValueBeSetOnContext(p.getDescriptor(), resourceContext)) {
            throw new ValidationException("The property " + p.getDescriptor() + " can not be set on context " + resourceContext.getContext());
        }
        p.setValue(unobfuscatedValue);
    }


    public void saveProperties(ContextEntity context, HasContexts<?> hasContexts,
                               List<ResourceEditProperty> resourceProperties) throws ValidationException {
        List<ResourceEditProperty> propertiesToBeSaved = new ArrayList<>();
        List<ResourceEditProperty> propertiesToBeRemoved = new ArrayList<>();
        for (ResourceEditProperty p : resourceProperties) {
            if (isNotNullOrEmpty(p.getPropertyValue()) && p.hasChanged()) {
                propertiesToBeSaved.add(p);
            } else if (p.isReset()) {
                propertiesToBeRemoved.add(p);
            }
        }
        if (!propertiesToBeSaved.isEmpty() || !propertiesToBeRemoved.isEmpty()) {
            auditService.storeIdInThreadLocalForAuditLog(hasContexts);
            if (!propertiesToBeSaved.isEmpty()) {
                ContextDependency<?> resourceContext = hasContexts.getOrCreateContext(context);
                for (ResourceEditProperty saveProp : propertiesToBeSaved) {
                    verifyDefaultPropertyCanBeSet(saveProp, context);
                    setPropertyValue(resourceContext, saveProp.getDescriptorId(), saveProp.getUnobfuscatedValue());
                }
            }
            if (!propertiesToBeRemoved.isEmpty()) {
                ContextDependency<?> resourceContext = hasContexts.getOrCreateContext(context);
                for (ResourceEditProperty removeProp : propertiesToBeRemoved) {
                    resetPropertyValue(resourceContext, removeProp.getDescriptorId());
                }
            }
        }
    }

    protected void verifyDefaultPropertyCanBeSet(ResourceEditProperty property, ContextEntity context) throws ValidationException {
        if (isSameValue(property.getDefaultValue(), property.getPropertyValue())) {
            if (context.isGlobal()) {
                // do not overwrite property value
                throw new ValidationException("The default value of property \"" + property.getPropertyDisplayName() + "\" can not be set on global context");
            }

            if (property.getParent() == null && property.getOriginalValue() == null) {
                throw new ValidationException("The default value of property \"" + property.getPropertyDisplayName() + "\" can not be set unless it overwrites a value defined on a parent context");
            }
        }
    }

    private boolean isSameValue(String defaultValue, String propertyValue) {
        return isNotNullOrEmpty(defaultValue) && defaultValue.trim().equals(propertyValue.trim());
    }

    private boolean isNotNullOrEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * IMPORTANT: This method does not check, if the caller has the permission to decrypt the provided properties!
     *
     * @param properties
     * @return
     */
    public List<ResourceEditProperty> decryptProperties(List<ResourceEditProperty> properties) {
        if (properties != null) {
            for (ResourceEditProperty p : properties) {
                p.decrypt();
            }
        }
        return properties;
    }

}
