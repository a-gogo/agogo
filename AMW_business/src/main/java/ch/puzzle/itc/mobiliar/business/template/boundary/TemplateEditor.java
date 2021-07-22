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

package ch.puzzle.itc.mobiliar.business.template.boundary;

import ch.puzzle.itc.mobiliar.business.auditview.control.AuditService;
import ch.puzzle.itc.mobiliar.business.database.entity.MyRevisionEntity;
import ch.puzzle.itc.mobiliar.business.environment.control.ContextDomainService;
import ch.puzzle.itc.mobiliar.business.environment.entity.AbstractContext;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextDependency;
import ch.puzzle.itc.mobiliar.business.environment.entity.HasContexts;
import ch.puzzle.itc.mobiliar.business.environment.entity.HasTypeContext;
import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.control.ResourceRelationService;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationContextEntity;
import ch.puzzle.itc.mobiliar.business.security.control.PermissionService;
import ch.puzzle.itc.mobiliar.business.security.entity.Action;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.business.security.interceptor.HasPermission;
import ch.puzzle.itc.mobiliar.business.template.control.FreemarkerSyntaxValidator;
import ch.puzzle.itc.mobiliar.business.template.control.TemplatesScreenDomainService;
import ch.puzzle.itc.mobiliar.business.template.entity.RevisionInformation;
import ch.puzzle.itc.mobiliar.business.template.entity.TemplateDescriptorEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import ch.puzzle.itc.mobiliar.common.exception.NotAuthorizedException;
import ch.puzzle.itc.mobiliar.common.exception.NotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceTypeNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.TemplateNotDeletableException;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;
import ch.puzzle.itc.mobiliar.common.util.SystemCallTemplate;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.*;
import java.util.logging.Logger;

@Stateless
public class TemplateEditor {

	@Inject
	EntityManager entityManager;

	@Inject
	ContextDomainService contextService;

	@Inject
	ResourceRelationService relationService;

	@Inject
	PermissionService permissionService;

	@Inject
	FreemarkerSyntaxValidator freemarkerValidator;

	@Inject
	AuditService auditService;

	@Inject
	ResourceLocator resourceLocator;

	@Inject
	TemplatesScreenDomainService templateService;

	@Inject
	private Logger log;

	public TemplateDescriptorEntity getTemplateById(Integer templateId) {
		return entityManager.find(TemplateDescriptorEntity.class, templateId);
	}

	public TemplateDescriptorEntity getTemplateByIdAndRevision(Integer templateId, Number revisionId) {
		TemplateDescriptorEntity templateDescriptorEntity = AuditReaderFactory.get(entityManager).find(
				TemplateDescriptorEntity.class, templateId, revisionId);
		//We have to ensure, that the target platforms are loaded. To make sure, that the compiler doesn't optimize the access to the target platforms away, we have to do this ugly hack.
		templateDescriptorEntity.getTargetPlatforms().size();
		return templateDescriptorEntity;
	}

	public List<RevisionInformation> getTemplateRevisions(Integer templateId) {
		List<RevisionInformation> result = new ArrayList<>();
		AuditReader reader = AuditReaderFactory.get(entityManager);
		List<Number> list = reader.getRevisions(TemplateDescriptorEntity.class, templateId);
		for (Number rev : list) {
			Date date = reader.getRevisionDate(rev);
			MyRevisionEntity myRev = entityManager.find(MyRevisionEntity.class, rev);
			result.add(new RevisionInformation(rev, date, myRev.getUsername()));
		}
		Collections.sort(result);
		return result;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	boolean hasTemplateWithSameName(TemplateDescriptorEntity template, HasContexts<?> hasContext) {
		for (ContextDependency<?> c : hasContext.getContextsByLowestContext(contextService.getGlobalResourceContextEntity())) {
			for (TemplateDescriptorEntity t : c.getTemplates()) {
				// If the template doesn't exist but has the same name, we return true
				if (!t.getId().equals(template.getId()) && t.getName().equals(template.getName())) {
					return true;
				}
			}
		}
		return false;
	}


	public <T extends HasContexts<?>> void saveTemplateForRelation(TemplateDescriptorEntity template,
																   Integer relationId,
																   boolean resourceEdit) throws AMWException {
		HasContexts<?> resourceRelation = null;
		if (relationId != null) {
			if (resourceEdit) {
				permissionService.checkPermissionAndFireException(Permission.RESOURCE_TEMPLATE,
																  Action.UPDATE,
																  "update templates for resource relations");
				resourceRelation = relationService.getResourceRelation(relationId);
			} else {
				permissionService.checkPermissionAndFireException(Permission.RESOURCETYPE_TEMPLATE,
																  Action.UPDATE,
																  "update templates for resource type relations");
				resourceRelation = relationService.getResourceTypeRelation(relationId);
			}
		}
		if (resourceRelation != null) {
			saveTemplate(template, resourceRelation);
		}
	}

	@HasPermission(permission = Permission.RESOURCE_TEMPLATE, oneOfAction = {Action.UPDATE, Action.CREATE})
	public void saveTemplateForResource(TemplateDescriptorEntity template, ResourceEntity resourceEntity,
										boolean testingMode) throws AMWException {
		permissionService.assertHasPermissionShakedownTestMode(testingMode);
		if (!testingMode) {
			Action action = getAction(template);
			permissionService.checkPermissionAndFireException(Permission.RESOURCE_TEMPLATE,
															  action,
															  "create/ modify resource templates");
		}
		saveTemplate(template, resourceEntity);
	}

	public void saveTemplateForResource(TemplateDescriptorEntity template, Integer resourceId,
										boolean testingMode) throws AMWException {
		ResourceEntity resourceEntity = entityManager.find(ResourceEntity.class, resourceId);
		this.saveTemplateForResource(template, resourceEntity, testingMode);
	}

	public void saveTemplateForResource(TemplateDescriptorEntity template, String resourceGroupName, String releaseName,
			boolean testingMode) throws AMWException {
		ResourceEntity resourceEntity = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName, releaseName);
		if (resourceEntity == null) {
			throw new ResourceNotFoundException("Resource not found");
		}
		this.saveTemplateForResource(template, resourceEntity, testingMode);
	}

	@HasPermission(permission = Permission.RESOURCETYPE_TEMPLATE, oneOfAction = {Action.UPDATE, Action.CREATE})
	public void saveTemplateForResourceType(TemplateDescriptorEntity template, Integer resourceTypeId,
											boolean testingMode) throws AMWException {
		permissionService.assertHasPermissionShakedownTestMode(testingMode);
		if (!testingMode) {
			Action action = getAction(template);
			permissionService.checkPermissionAndFireException(Permission.RESOURCETYPE_TEMPLATE,
															  action,
															  "create/ modify resource type templates");
		}
		ResourceTypeEntity resourceTypeEntity = entityManager.find(ResourceTypeEntity.class, resourceTypeId);
		saveTemplate(template, resourceTypeEntity);
	}

	void validateTemplate(TemplateDescriptorEntity templateDescriptorEntity) throws ValidationException {
		if (StringUtils.isEmpty(templateDescriptorEntity.getName())) {
			throw new ValidationException("The template name must not be empty");
		}

		if (templateDescriptorEntity.getTargetPath() != null && templateDescriptorEntity.getTargetPath()
																						.startsWith("/")) {
			throw new ValidationException("Absolute paths are not allowed for file path");
		}

		if (templateDescriptorEntity.getTargetPath() != null && templateDescriptorEntity.getTargetPath()
																						.contains("../")) {
			throw new ValidationException("No path traversals like '../' allowed in file path");
		}
	}

	void saveTemplate(TemplateDescriptorEntity template, HasContexts<?> hasContext) throws ValidationException, AMWException {
		validateTemplate(template);
		freemarkerValidator.validateFreemarkerSyntax(template.getFileContent());
		hasContext = entityManager.find(hasContext.getClass(), hasContext.getId());
		auditService.storeIdInThreadLocalForAuditLog(hasContext);
		if (hasTemplateWithSameName(template, hasContext)) {
			throw new ValidationException("The defined template name is already in use");
		}
		if (hasContext instanceof HasTypeContext
				&& hasTemplateWithSameName(template, ((HasTypeContext<?>) hasContext).getTypeContext())) {
			throw new ValidationException("The defined template name is already in use");
		}


		ContextDependency<?> globalContext = hasContext.getOrCreateContext(contextService.getGlobalResourceContextEntity());
		if (template.getId() == null) {
			entityManager.persist(template);
			globalContext.addTemplate(template);
			entityManager.persist(globalContext);
		} else {
			entityManager.merge(template);
		}
	}

	/**
	 * @param templateId - the id of the template to be deleted.
	 *
	 * @throws ResourceTypeNotFoundException
	 * @throws TemplateNotDeletableException
	 */
	public void removeTemplate(Integer templateId) throws TemplateNotDeletableException {
		TemplateDescriptorEntity templateDescriptor = entityManager.find(TemplateDescriptorEntity.class, templateId);
		this.removeTemplate(templateDescriptor);
	}

	private void removeTemplate(TemplateDescriptorEntity templateDescriptor) throws TemplateNotDeletableException {
		if (templateDescriptor != null && templateDescriptor.getName() != null
				&& SystemCallTemplate.getName().equals(templateDescriptor.getName())) {
			String message = SystemCallTemplate.getName() + " Template can't be deleted since it is a system template!";
			log.info(message);
			throw new TemplateNotDeletableException(message);
		}
		AbstractContext owner = templateService.getOwnerOfTemplate(templateDescriptor);
		if (owner != null) {
			if (owner instanceof ResourceContextEntity && !permissionService.hasPermission(Permission.RESOURCE_TEMPLATE,
																						   null,
																						   Action.DELETE,
																						   ((ResourceContextEntity) owner)
																								   .getContextualizedObject()
																								   .getResourceGroup(),
																						   null)) {
				throw new NotAuthorizedException("Not authorized to remove the template of a resource");
			} else if (owner instanceof ResourceRelationContextEntity && !permissionService.hasPermission(Permission.RESOURCE_TEMPLATE,
																										  null,
																										  Action.DELETE,
																										  ((ResourceRelationContextEntity) owner)
																												  .getContextualizedObject()
																												  .getMasterResource()
																												  .getResourceGroup(),
																										  null)) {
				throw new NotAuthorizedException("Not authorized to remove the template of a resource");
			} else if (owner instanceof ResourceTypeContextEntity && !permissionService.hasPermission(Permission.RESOURCETYPE_TEMPLATE,
																									  null,
																									  Action.DELETE,
																									  null,
																									  ((ResourceTypeContextEntity) owner)
																											  .getContextualizedObject())) {
				throw new NotAuthorizedException("Not authorized to remove the template of a resource type");
			}
			auditService.storeIdInThreadLocalForAuditLog(owner);
			owner.removeTemplate(templateDescriptor);
		}
		entityManager.remove(templateDescriptor);
		log.info("Template " + templateDescriptor.getId() + " has been deleted successfully.");
	}

	public void removeTemplate(String resourceGroupName, String releaseName, String templateName, boolean testingMode)
			throws ValidationException, TemplateNotDeletableException, NotFoundException {
		ResourceEntity resource = resourceLocator.getResourceByGroupNameAndRelease(resourceGroupName, releaseName);
		if (resource == null) {
			throw new ResourceNotFoundException("Resource not found");
		}
		List<TemplateDescriptorEntity> templates = templateService.getGlobalTemplateDescriptorsForResource(resource, testingMode);

		TemplateDescriptorEntity temp = null;
		for (TemplateDescriptorEntity template : templates) {
			if (templateName.equals(template.getName())) {
				temp = template;
				break;
			}
		}

		if (temp == null) {
			throw new NotFoundException("Template not found");
		}
		this.removeTemplate(temp);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	Object getSingleObjectOrNull(Query q) {
		try {
			return q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	private Action getAction(TemplateDescriptorEntity template) {
		return template.getId() == null ? Action.CREATE : Action.UPDATE;
	}
}
