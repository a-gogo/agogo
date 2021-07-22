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

package ch.puzzle.itc.mobiliar.business.auditview.control;

import ch.puzzle.itc.mobiliar.business.auditview.entity.AuditViewEntry;
import ch.puzzle.itc.mobiliar.business.auditview.entity.AuditViewEntryContainer;
import ch.puzzle.itc.mobiliar.business.auditview.entity.Auditable;
import ch.puzzle.itc.mobiliar.business.database.entity.MyRevisionEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.AbstractContext;
import ch.puzzle.itc.mobiliar.business.environment.entity.HasContexts;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.logging.Logger;

import static org.hibernate.envers.RevisionType.ADD;
import static org.hibernate.envers.RevisionType.DEL;

@Stateless
public class AuditService {

    @Inject
    private Logger log;

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    private AuditHandlerRegistry auditHandlerRegistry;

    @SuppressWarnings("unchecked")
    public <T> Object getDeletedEntity(T entity, Integer id) {
        Objects.requireNonNull(entity, "Entity can not be null");
        Objects.requireNonNull(id, "Id can not be null");

        AuditReader reader = AuditReaderFactory.get(entityManager);

        if (reader.isEntityClassAudited(entity.getClass())) {
            AuditQuery query = reader.createQuery()
                    .forRevisionsOfEntity(entity.getClass(), false, true)
                    .add(AuditEntity.id().eq(id))
                    .add(AuditEntity.revisionType().eq(DEL));

            List<Object[]> resultList = query.getResultList();
            if (!resultList.isEmpty()) {
                return resultList.get(0)[0];
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getAllDeletedEntities(Class clazz) {
        Objects.requireNonNull(clazz, "Clazz can not be null");

        AuditReader reader = AuditReaderFactory.get(entityManager);

        if (reader.isEntityClassAudited(clazz)) {
            AuditQuery query = reader.createQuery()
                    .forRevisionsOfEntity(clazz, false, true)
                    .add(AuditEntity.revisionType().eq(DEL));

            List<Object[]> resultList = query.getResultList();
            if (!resultList.isEmpty()) {
                List<Object> deleted = new ArrayList<>();
                for (Object[] objects : resultList) {
                    deleted.add(objects[0]);
                }
                return deleted;
            }
        }
        return Collections.EMPTY_LIST;
    }

    public List<AuditViewEntry> getAuditViewEntriesForResource(Integer resourceId) {
        // Map<Hashcode, AuditViewEntry>
        Map<Integer, AuditViewEntry> allAuditViewEntries = new HashMap<>();
        AuditReader reader = AuditReaderFactory.get(entityManager);
        CrossTypeRevisionChangesReader crossTypeRevisionChangesReader = reader.getCrossTypeRevisionChangesReader();
        List<MyRevisionEntity> revisionsForResource = getRevisionsForResource(resourceId);
        for (MyRevisionEntity revisionEntity : revisionsForResource) {
            List<Object> changedEntitiesForRevision = crossTypeRevisionChangesReader.findEntities(revisionEntity.getId());
            for (Object o : changedEntitiesForRevision) {
                List resultList = reader.createQuery()
                        .forRevisionsOfEntity(o.getClass(), false, true)
                        .add(AuditEntity.revisionNumber().eq(revisionEntity.getId()))
                        .getResultList();
                createAuditViewEntriesAndAddToMap(allAuditViewEntries, resultList);
            }
        }
        return new ArrayList<>(allAuditViewEntries.values());
    }

    private void createAuditViewEntriesAndAddToMap(Map<Integer, AuditViewEntry> allAuditViewEntries, List resultList) {
        for (Object triple : resultList) {
            createSingleAuditViewEntryAndAddToMap(allAuditViewEntries, (Object[]) triple);
        }
    }

    private void createSingleAuditViewEntryAndAddToMap(Map<Integer, AuditViewEntry> allAuditViewEntries, Object[] entityRevisionAndRevisionType) {
        Object entity = entityRevisionAndRevisionType[0];
        if (! (entity instanceof Auditable) ) {
            log.info(String.format("The Entity %s does not implement interface Auditable. No Audit log entry will be created for the log view.",
                    entity.getClass().getSimpleName()));
            return;
        }
        AuditViewEntryContainer auditViewEntryContainer = new AuditViewEntryContainer(entityRevisionAndRevisionType);

        try {
            AuditHandler handler = auditHandlerRegistry.getAuditHandler(entity.getClass());
            AuditViewEntry auditViewEntry = handler.createAuditViewEntry(auditViewEntryContainer);
            if (isAuditViewEntryRelevant(auditViewEntry, allAuditViewEntries)) {
                allAuditViewEntries.put(auditViewEntry.hashCode(), auditViewEntry);
            }
        } catch (NoAuditHandlerException e) {
            log.info(String.format("No AuditHandler found for %s in AuditHandlerRegistry", entity));
        }
    }

    protected boolean isAuditViewEntryRelevant(AuditViewEntry entry, Map<Integer, AuditViewEntry> allAuditViewEntries) {
        if (entry == null) {
            return false;
        }
        if (entry.getMode() == ADD || entry.getMode() == DEL) {
            return true;
        }
        if (allAuditViewEntries.get(entry.hashCode()) != null) {
            return false;
        }
        if (entry.getType().equals(Auditable.TYPE_TEMPLATE_DESCRIPTOR)) {
            // the content of the template descriptors are ignored
            return true;
        }
        if (entry.isObfuscatedValue()) {
            return true;
        }
        return !StringUtils.equals(entry.getOldValue(), entry.getValue());
    }

    private List<MyRevisionEntity> getRevisionsForResource(Integer resourceId) {
        return this.entityManager
                    .createQuery("FROM MyRevisionEntity n WHERE n.resourceId = :resourceId", MyRevisionEntity.class)
                    .setParameter("resourceId", resourceId)
                    .getResultList();
    }

    public void storeIdInThreadLocalForAuditLog(HasContexts<?> hasContexts) {
        if (hasContexts instanceof ResourceTypeEntity) {
            setResourceTypeIdInThreadLocal(hasContexts.getId());
        } else if (hasContexts instanceof ResourceEntity) {
            setResourceIdInThreadLocal(hasContexts.getId());
        } else if (hasContexts instanceof ConsumedResourceRelationEntity) {
            setResourceIdInThreadLocal(((ConsumedResourceRelationEntity) hasContexts).getMasterResourceId());
        } else if (hasContexts instanceof ProvidedResourceRelationEntity) {
            setResourceIdInThreadLocal(((ProvidedResourceRelationEntity) hasContexts).getMasterResource().getId());
        } else if (hasContexts instanceof ResourceRelationTypeEntity) {
            setResourceTypeIdInThreadLocal(((ResourceRelationTypeEntity) hasContexts).getResourceTypeA().getId());
        }
    }

    public void storeIdInThreadLocalForAuditLog(AbstractContext owner) {
        if (owner instanceof ResourceContextEntity) {
            setResourceIdInThreadLocal(((ResourceContextEntity) owner).getResource().getId());
        } else if (owner instanceof ResourceTypeContextEntity) {
            setResourceTypeIdInThreadLocal(((ResourceTypeContextEntity) owner).getResourceTypeEntity().getId());
        } else if (owner instanceof ResourceRelationContextEntity) {
            ConsumedResourceRelationEntity consumed = ((ResourceRelationContextEntity) owner).getConsumedResourceRelation();
            if (consumed != null) {
                setResourceIdInThreadLocal(consumed.getMasterResourceId());
            }
        }
    }

    public void setResourceTypeIdInThreadLocal(Integer resourceTypeId) {
        ThreadLocalUtil.setThreadVariable(ThreadLocalUtil.KEY_RESOURCE_TYPE_ID, resourceTypeId);
    }

    public void setResourceIdInThreadLocal(Integer resourceId) {
        ThreadLocalUtil.setThreadVariable(ThreadLocalUtil.KEY_RESOURCE_ID, resourceId);
    }

}
