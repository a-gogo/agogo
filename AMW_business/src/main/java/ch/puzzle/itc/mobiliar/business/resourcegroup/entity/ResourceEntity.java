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

package ch.puzzle.itc.mobiliar.business.resourcegroup.entity;

import ch.puzzle.itc.mobiliar.business.configurationtag.entity.ResourceTagEntity;
import ch.puzzle.itc.mobiliar.business.database.control.Constants;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.HasContexts;
import ch.puzzle.itc.mobiliar.business.environment.entity.HasTypeContext;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.Foreignable;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.function.entity.AmwFunctionEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditRelation;
import ch.puzzle.itc.mobiliar.business.property.entity.ResourceEditRelation.Mode;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceResult;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyUnit;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.AbstractResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ConsumedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ProvidedResourceRelationEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import ch.puzzle.itc.mobiliar.business.shakedown.entity.ShakedownTestEntity;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.entity.SoftlinkRelationEntity;
import ch.puzzle.itc.mobiliar.business.utils.CopyHelper;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.ResourceNotFoundException;
import ch.puzzle.itc.mobiliar.common.util.ApplicationServerContainer;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.*;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;

/**
 * Entity implementation class for Entity: Resource
 */
@Entity
@Audited
@Table(name = "TAMW_resource")
@EqualsAndHashCode(callSuper = false, of = { "id", "name", "release" })
public class ResourceEntity extends HasContexts<ResourceContextEntity> implements Serializable,
		NamedIdentifiable, Comparable<ResourceEntity>, HasTypeContext<ResourceTypeEntity>, Foreignable<ResourceEntity> {

    // IMPORTANT! Whenever a new field (not relation to other entity) is added then this field must be added to foreignableFieldEquals method!!!

	@TableGenerator(name = "resourceIdGen", table = Constants.GENERATORTABLE, pkColumnName = Constants.GENERATORPKCOLUMNNAME, valueColumnName = Constants.GENERATORVALUECOLUMNNAME, pkColumnValue = "resourceId")
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "resourceIdGen")
	@Id
	@Column(unique = true, nullable = false)
	@Getter
	@Setter
	private Integer id;

	@ManyToOne(cascade = PERSIST)
	@Getter
	private ResourceTypeEntity resourceType;

	@Column(nullable = false)
	@Getter
	@Setter
	private boolean deletable = true;

	// Consumed resources
	@OneToMany(cascade = ALL, mappedBy = "masterResource")
	@BatchSize(size = 15)
	@Getter
	@Setter
	private Set<ConsumedResourceRelationEntity> consumedMasterRelations;

	@OneToMany(cascade = ALL, mappedBy = "slaveResource")
	@Getter
	@Setter
	private Set<ConsumedResourceRelationEntity> consumedSlaveRelations;

	// Provided resources
	@OneToMany(cascade = ALL, mappedBy = "masterResource")
	@BatchSize(size = 15)
	@Getter
	@Setter
	private Set<ProvidedResourceRelationEntity> providedMasterRelations;

	@OneToMany(cascade = ALL, mappedBy = "slaveResource")
	@Getter
	@Setter
	private Set<ProvidedResourceRelationEntity> providedSlaveRelations;

	@OneToMany(cascade = ALL, mappedBy = "resource")
	private Set<ResourceContextEntity> contexts;

	@OneToMany(cascade = ALL, mappedBy = "resource")
	@Getter
	@Setter
	private Set<ResourceTagEntity> resourceTags;

	@ManyToOne
	@Getter
	@Setter
	private ReleaseEntity release;

	@OneToMany(cascade = {PERSIST, MERGE}, mappedBy = "resource")
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	@NotAudited
	@Getter
	@Setter
	private Set<DeploymentEntity> deployments;

	@OneToMany(cascade = ALL, mappedBy = "applicationServer")
	@NotAudited
	@Getter
	private Set<ShakedownTestEntity> shakedownTests;

	@ManyToOne(optional = false, cascade = PERSIST)
	@Getter
	@Setter
	private ResourceGroupEntity resourceGroup;

	@Version
	@Getter
	private long v;

	@Getter
	private String name;

    @Enumerated(EnumType.STRING)
    private ForeignableOwner fcOwner;

	/**
	 * This field is for entity mapping only. It represents the relations between a runtime resource and its
	 * corresponding deployments.
	 */
	@OneToMany(cascade = {PERSIST, MERGE}, mappedBy = "runtime")
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	@NotAudited
	@Getter
	private Set<DeploymentEntity> deploymentsOfRuntime;


	@OneToMany(mappedBy = "resource", cascade = PERSIST)
	@Setter
	private Set<AmwFunctionEntity> functions;

	/**
	 * For PPI & CPI resources only
	 */

	@Getter
	@Setter
	private String localPortId;

	/**
	 * Only for PPI resources
	 */
	@Setter
	@Getter
	private String softlinkId;

	/**
	 * Only for CPI resources
	 */
	@OneToMany(cascade = ALL, mappedBy = "cpiResource")
	@BatchSize(size = 15)
	private Set<SoftlinkRelationEntity> softlinkRelations;

	public void setName(final String name) {
		// delegate set name to resourceGroup guarantees that all resources in the group have the same name
		this.name = name;
		getResourceGroup().setName(name);
	}

	public void setResourceType(ResourceTypeEntity resourceType) {
		this.resourceType = resourceType;
		getResourceGroup().setResourceType(resourceType);
	}

	/**
	 * This should only be called inside {@link ResourceGroup}!
	 * 
	 * @param resourceType
	 */
	protected void updateResourceType(ResourceTypeEntity resourceType) {
		this.resourceType = resourceType;
	}

	/**
	 * This should only be called inside {@link ResourceGroup}!
	 * 
	 * @param name
	 */
	protected void updateName(String name) {
		this.name = name;
	}

	private static final long serialVersionUID = 1L;

    /**
     * Creates new entity object with default system owner
     */
	public ResourceEntity() {
		this(ForeignableOwner.getSystemOwner());
	}

    ResourceEntity(ForeignableOwner owner) {
        this.fcOwner = Objects.requireNonNull(owner, "Owner must not be null");
    }

	public void addConsumedRelation(final ConsumedResourceRelationEntity relation) {
		if (consumedMasterRelations == null) {
			consumedMasterRelations = new HashSet<ConsumedResourceRelationEntity>();
		}
		consumedMasterRelations.add(relation);
	}

	public void addProvidedSlaveRelation(final ProvidedResourceRelationEntity relation){
		if (providedSlaveRelations == null) {
			providedSlaveRelations = new HashSet<ProvidedResourceRelationEntity>();
		}
		providedSlaveRelations.add(relation);
	}

	public void addProvidedRelation(final ProvidedResourceRelationEntity relation) {
		if (providedMasterRelations == null) {
			providedMasterRelations = new HashSet<ProvidedResourceRelationEntity>();
		}
		providedMasterRelations.add(relation);
	}

	public <T extends AbstractResourceRelationEntity> void removeRelation(final T relation) {
		if (relation instanceof ConsumedResourceRelationEntity) {
			if (relation != null && consumedMasterRelations != null) {
				consumedMasterRelations.remove(relation);
			}
		}
		else {
			if (relation != null && providedMasterRelations != null) {
				providedMasterRelations.remove(relation);
			}
		}
	}

	public <T extends AbstractResourceRelationEntity> void removeSlaveRelation(final T relation) {
		if (relation instanceof ConsumedResourceRelationEntity) {
			if (relation != null && consumedSlaveRelations != null) {
				consumedSlaveRelations.remove(relation);
			}
		}
		else {
			if (relation != null && providedSlaveRelations != null) {
				providedSlaveRelations.remove(relation);
			}
		}
	}

	public ProvidedResourceRelationEntity getProvidedRelationById(final Integer relatedResourceId) {
		return getRelationById(relatedResourceId, getProvidedMasterRelations());
	}

	public ConsumedResourceRelationEntity getConsumedRelationById(final Integer relatedResourceId) {
		return getRelationById(relatedResourceId, getConsumedMasterRelations());
	}

	<T extends AbstractResourceRelationEntity> T getRelationById(final Integer relatedResourceId,
			final Set<T> relations) {
		if (relations != null) {
			for (final T relation : relations) {
				if (relation.getSlaveResource().getId().equals(relatedResourceId)) {
					return relation;
				}
			}
		}
		return null;
	}

	/**
	 * This method returns the consumed resource relation for this resource dependent on the given
	 * resourceRelationId
	 * 
	 * @param resourceRelationId
	 * @return
	 */
	public ConsumedResourceRelationEntity getConsumedResourceRelation(Integer resourceRelationId) {
		for (ConsumedResourceRelationEntity rel : getConsumedMasterRelations()) {
			if (rel.getId().equals(resourceRelationId)) {
				return rel;
			}
		}
		return null;
	}

	public AbstractResourceRelationEntity getResourceRelation(ResourceEditRelation relation) {
		Set<? extends AbstractResourceRelationEntity> relationList = null;
		if (relation.getMode() == Mode.CONSUMED) {
			relationList = getConsumedMasterRelations();
		}
		else if (relation.getMode() == Mode.PROVIDED) {
			relationList = getProvidedMasterRelations();
		}
		if (relationList != null) {
			for (AbstractResourceRelationEntity rel : relationList) {
				if (rel.getId().equals(relation.getResRelId())) {
					return rel;
				}
			}
		}
		return null;
	}

	public ConsumedResourceRelationEntity getConsumedRelation(final ResourceEntity relatedResource) {
		return getMasterRelation(relatedResource, getConsumedMasterRelations());
	}

	public ProvidedResourceRelationEntity getProvidedRelation(final ResourceEntity relatedResource) {
		return getMasterRelation(relatedResource, getProvidedMasterRelations());
	}

	<T extends AbstractResourceRelationEntity> T getMasterRelation(final ResourceEntity relatedResource,
			final Set<T> relations) {
		if (relations != null) {
			for (final T relation : relations) {
				if (relation.getSlaveResource() != null && relation.getSlaveResource().getId() != null) {
					if (relation.getSlaveResource().getId().equals(relatedResource.getId())) {
						return relation;
					}
				}
			}
		}
		return null;
	}

	public List<ResourceRelationTypeEntity> getConsumedResourceRelationTypes() {
		List<ResourceRelationTypeEntity> resourceRelationTypes = new ArrayList<ResourceRelationTypeEntity>();
		ResourceTypeEntity resType = getResourceType();
		while (resType != null) {
			resourceRelationTypes.addAll(resType.getResourceRelationTypesA());
			resType = resType.getParentResourceType();
		}
		return resourceRelationTypes;
	}

	public ConsumedResourceRelationEntity getConsumedSlaveRelation(final ResourceEntity relatedResource) {
		if (getConsumedSlaveRelations() != null) {
			for (final ConsumedResourceRelationEntity relation : getConsumedSlaveRelations()) {
				if (relation.getMasterResource() != null && relation.getMasterResource().getId() != null) {
					if (relatedResource.getName() != null
							&& relation.getMasterResource().getId().equals(relatedResource.getId())
							&& !relatedResource.getName().equals(
									ApplicationServerContainer.APPSERVERCONTAINER.getDisplayName())) {
						return relation;
					}
				}
			}
		}
		return null;
	}

	public List<ResourceEntity> getMasterResourcesOfProvidedSlaveRelations() {
		List<ResourceEntity> masterResources = new ArrayList<ResourceEntity>();
		for (ProvidedResourceRelationEntity r : getProvidedSlaveRelations()) {
			masterResources.add(r.getMasterResource());
		}
		return masterResources;
	}

	public List<ResourceEntity> getConsumedRelatedResources() {
		List<ResourceEntity> list = new ArrayList<>();
		if (getConsumedMasterRelations() != null) {
			for (AbstractResourceRelationEntity relation : getConsumedMasterRelations()) {
				if (relation != null && relation instanceof ConsumedResourceRelationEntity) {
					ConsumedResourceRelationEntity consumedRelation = (ConsumedResourceRelationEntity) relation;
                    list.add(consumedRelation.getSlaveResource());
				}
			}
		}
        Collections.sort(list);
		return list;
	}

	public List<ResourceEntity> getConsumedRelatedResourcesByResourceType(
			DefaultResourceTypeDefinition resourceTypeDefinition) {
		final List<ResourceEntity> result = new ArrayList<ResourceEntity>();

		List<ResourceEntity> consumedRelatedResources = getConsumedRelatedResources();
		for (final ResourceEntity r : consumedRelatedResources) {
			if (r.getResourceType() != null && r.getResourceType().getId() != null) {
				if (r.getResourceType().isResourceType(resourceTypeDefinition)) {
					result.add(r);
				}
			}
		}
		return result;
	}

	public List<ResourceEntity> getMasterResourcesOfConsumedSlaveRelationByResourceType(
			final DefaultResourceTypeDefinition resourceTypeDefinition) {
		final List<ResourceEntity> result = new ArrayList<ResourceEntity>();
		for (final ConsumedResourceRelationEntity relation : getConsumedSlaveRelations()) {
			final ResourceEntity master = relation.getMasterResource();
			if (master.getResourceType().isResourceType(resourceTypeDefinition)) {
				result.add(master);
			}
		}
		return result;
	}

	public List<ResourceEntity> getMasterResourcesOfProvidedSlaveRelationByResourceType(
			final DefaultResourceTypeDefinition resourceTypeDefinition) {
		final List<ResourceEntity> result = new ArrayList<ResourceEntity>();
		for (final ProvidedResourceRelationEntity relation : getProvidedSlaveRelations()) {
			final ResourceEntity master = relation.getMasterResource();
			if (master.getResourceType().isResourceType(resourceTypeDefinition)) {
				result.add(master);
			}
		}
		return result;
	}

	public List<ResourceEntity> getConsumedRelatedMasterResourcesByResourceType(
			final ResourceTypeEntity resourceType) {
		final List<ResourceEntity> result = new ArrayList<ResourceEntity>();
		for (final ResourceEntity r : getConsumedRelatedMasterResources()) {
			if (r.getResourceType().getId().equals(resourceType.getId())) {
				result.add(r);
			}
		}
		return result;
	}

	private List<ResourceEntity> getConsumedRelatedMasterResources() {
		final List<ResourceEntity> result = new ArrayList<ResourceEntity>();
		if (getConsumedSlaveRelations() != null) {
			for (final ConsumedResourceRelationEntity relation : getConsumedSlaveRelations()) {
				final ResourceEntity r = relation.getMasterResource();
				if (r != null) {
					result.add(r);
				}
			}
		}
		return result;
	}


	/**
	 * Only for hibernate - do not make public<br>
	 *     Workaround because so far softlinkrelation is a one-to-one relation
	 * @return
	 */
	@SuppressWarnings("unused")
	private Set<SoftlinkRelationEntity> getSoftlinkRelations() {
		return softlinkRelations;
	}

	public void clearSoftlinkRelations() {
		if (softlinkRelations != null) {
			softlinkRelations.clear();
		}
	}

	/**
	 * Only for hibernate - do not make public <br>
	 *     Workaround because so far softlinkrelation is a one-to-one relation
	 * @param softlinkRelations
	 */
	@SuppressWarnings("unused")
	private void setSoftlinkRelations(Set<SoftlinkRelationEntity> softlinkRelations) {
		this.softlinkRelations = softlinkRelations;
	}

	public SoftlinkRelationEntity getSoftlinkRelation(){
		// workaround because so far softlinkrelation is a one-to-one relation
		if ( softlinkRelations != null && !softlinkRelations.isEmpty()) {
			return softlinkRelations.iterator().next();
		}
		return null;
	}

	public void setSoftlinkRelation(SoftlinkRelationEntity softlinkRelation){
		throw new UnsupportedOperationException("You can not directly add a softlinkrelation to the resource. Use SoftlinkRelationService.setSoftlinkRelation(ResourceEntity cpiResource, String softlinkRef) instead");
	}

	public ProvidedResourceRelationEntity addProvidedResourceRelation(final ResourceEntity relatedResource,
			final ResourceRelationTypeEntity resourceRelationTypeOfRelation, ForeignableOwner changingOwner)
			throws ElementAlreadyExistsException {
		if (getProvidedRelation(relatedResource) != null) {
			throw new ElementAlreadyExistsException();
		}
		final ProvidedResourceRelationEntity r = new ProvidedResourceRelationEntity();
		r.setResourceRelationType(resourceRelationTypeOfRelation);
		r.setMasterResource(this);
		r.setSlaveResource(relatedResource);
		r.setOwner(changingOwner);
		relatedResource.addProvidedSlaveRelation(r);
		this.addProvidedRelation(r);
		return r;
	}

	public ConsumedResourceRelationEntity addConsumedResourceRelation(final ResourceEntity relatedResource,
			final ResourceRelationTypeEntity resourceRelationTypeOfRelation, String relationName, ForeignableOwner changingOwner) {
		final ConsumedResourceRelationEntity r = new ConsumedResourceRelationEntity();

		r.setIdentifier(relationName);
		r.setResourceRelationType(resourceRelationTypeOfRelation);
		r.setMasterResource(this);
		r.setSlaveResource(relatedResource);
		r.setOwner(changingOwner);
		this.addConsumedRelation(r);
		return r;
	}

	// für applikationsgruppen!!!
	public void changeResourceRelation(final ResourceEntity oldRelatedResource,
			final ResourceEntity newRelatedResource,
			final ResourceRelationTypeEntity resourceTypeOfNewRelation)
			throws ElementAlreadyExistsException, ResourceNotFoundException {
		if (getConsumedSlaveRelation(newRelatedResource) != null) {
			throw new ElementAlreadyExistsException();
		}
		final ConsumedResourceRelationEntity relation = getConsumedSlaveRelation(oldRelatedResource);
		if (relation == null) {
			throw new ResourceNotFoundException("Es wurde keine Beziehung zwischen "
					+ oldRelatedResource.getName() + " und " + this.getName() + " gefunden!");
		}
		relation.setResourceRelationType(resourceTypeOfNewRelation);
		relation.setMasterResource(newRelatedResource);
		oldRelatedResource.removeRelation(relation);
	}

	@Override
	public ResourceContextEntity createContext() {
		final ResourceContextEntity c = new ResourceContextEntity();
		c.setContextualizedObject(this);
		return c;
	}

	@Override
	public Set<ResourceContextEntity> getContexts() {
		return contexts;
	}

	@Override
	public void setContexts(final Set<ResourceContextEntity> contexts) {
		this.contexts = contexts;
	}

	@Override
	public int compareTo(final ResourceEntity o) {
		if (getName() == null) {
			return -1;
		}
		if (o == null) {
			return 1;
		}
		int c = getName().compareToIgnoreCase(o.getName());
		if (c == 0) {
			return getId() != null ? getId().compareTo(o.getId()) : o.getId() == null ? 0 : -1;
		}
		return c;
	}

	@Override
	public String toString() {
		return "ResourceEntity [id=" + id + ", name=" + name + "]";
	}

	/**
	 * Returns all Relations for given resourceId
	 * 
	 * @param resourceId
	 * @return
	 */
	public List<AbstractResourceRelationEntity> getMasterRelationsForResource(Integer resourceId) {
		List<AbstractResourceRelationEntity> list = new ArrayList<AbstractResourceRelationEntity>();
		list.addAll(getConsumedMasterRelations());
		list.addAll(getProvidedMasterRelations());

		List<AbstractResourceRelationEntity> result = new ArrayList<AbstractResourceRelationEntity>();

		for (AbstractResourceRelationEntity relation : list) {
			if (resourceId.equals(relation.getSlaveResource().getId())) {
				result.add(relation);
			}
		}
		return result;
	}

	@Override
	public ResourceTypeEntity getTypeContext() {
		return resourceType;
	}

	/**
	 * This is a container for virtual consumed resource relations - a virtual consumed resource relation is
	 * a construct which represents an unresolved resource type relation which is overridden by a application
	 * server relation. Since the evaluation of these relations is context dependent, it is not possible to
	 * persist these values and have to be extracted during generation process.
	 */
	@Transient
	@Getter
	final List<ConsumedResourceRelationEntity> virtualConsumedResources = new ArrayList<>();

	/**
	 * @return the relations pointing to the runtime or null if not found
	 */
	public Set<ConsumedResourceRelationEntity> getRuntimeRelations() {
		Set<ConsumedResourceRelationEntity> relations = new HashSet<ConsumedResourceRelationEntity>();
		if (resourceType.isApplicationServerResourceType()) {
			for (ConsumedResourceRelationEntity rel : getConsumedMasterRelations()) {
				if (rel.getSlaveResource() != null
						&& rel.getSlaveResource().getResourceType().isRuntimeType()) {
					relations.add(rel);
				}
			}
		}
		return relations.isEmpty() ? null : relations;
	}

	/**
	 * @return the runtime resource group without resources and releases
	 */
	public ResourceGroupEntity getRuntime() {
		Set<ConsumedResourceRelationEntity> runtimeRelations = getRuntimeRelations();
		return runtimeRelations != null ? runtimeRelations.iterator().next().getSlaveResource()
				.getResourceGroup() : null;
	}


	public Set<AmwFunctionEntity> getFunctions() {
		if (functions == null){
			return new HashSet<>();
		}
		return functions;
	}

	public void addFunction(AmwFunctionEntity function){
		if (functions == null){
			functions = new HashSet<>();
		}
		functions.add(function);
	}

    @Override
    public ForeignableOwner getOwner() {
        return fcOwner;
    }

    @Override
    public void setOwner(ForeignableOwner owner) {
        this.fcOwner = owner;
    }

    @Override
    public String getExternalLink() {
        if (resourceGroup != null) {
            return resourceGroup.getFcExternalLink();
        }
        return null;
    }

    @Override
    public void setExternalLink(String externalLink) {
        if (resourceGroup != null) {
            resourceGroup.setFcExternalLink(externalLink);
        }
    }

    @Override
    public String getExternalKey() {
        if (resourceGroup != null) {
            return resourceGroup.getFcExternalKey();
        }
        return null;
    }

    @Override
    public void setExternalKey(String externalKey) {
        if (resourceGroup != null) {
            resourceGroup.setFcExternalKey(externalKey);
        }
    }

    @Override
    public String getForeignableObjectName() {
        return this.getClass().getSimpleName();
    }




    @Override
	public ResourceEntity getCopy(ResourceEntity target, CopyUnit copyUnit) {
		// copy name when releasing
		if(copyUnit.getMode() == CopyResourceDomainService.CopyMode.RELEASE){
			target.setName(this.getName());
			copyUnit.getResult().setTargetResourceName(target.getName());
		}

		// resource type can not be changed! (both resources must be from the same type/subtype!)
		Integer origResTypeId = this.getResourceType() != null ? this.getResourceType().getId() : null;
		Integer targetResTypeId = target.getResourceType() != null ? target.getResourceType().getId() : null;
		if (targetResTypeId != null && !CopyHelper.equalsWithNullCheck(origResTypeId, targetResTypeId)) {
			copyUnit.getResult().addCopyResultError(CopyResourceResult.CopyFailure.RESOURCETYPE_DIFF, CopyResourceResult.CopyTarget.RESOURCE,
					null, target.getResourceType().getName(),
					this.getResourceType().getName());
		}
		if (targetResTypeId == null) {
			target.setResourceType(this.getResourceType());
		}

		// is deletable
		if (target.isDeletable() != this.isDeletable()) {
			copyUnit.getResult().addChangedResourceParam(CopyResourceResult.CopyInfo.DELETABLE_CHANGED);
		}
		target.setDeletable(this.isDeletable());
		// only set softlink rel on target if not in Predecessor mode
		if(!CopyResourceDomainService.CopyMode.MAIA_PREDECESSOR.equals(copyUnit.getMode())){
			target.setSoftlinkId(this.getSoftlinkId());
		}

		CopyHelper.copyForeignable(target, this, copyUnit);
		return target;
	}


    @Override
    public int foreignableFieldHashCode() {
        HashCodeBuilder eb = new HashCodeBuilder();

        eb.append(this.id);
        eb.append(this.fcOwner);
        eb.append(this.getExternalKey());
        eb.append(this.getExternalLink());
        eb.append(this.deletable);
        eb.append(this.name);
		eb.append(this.localPortId);
        eb.append(this.softlinkId);

        eb.append(this.resourceType != null ? this.resourceType.getId() : null);
        eb.append(this.resourceGroup != null ? this.resourceGroup.getId() : null);
        eb.append(this.release != null ? this.release.getId() : null);

        return eb.toHashCode();
    }
}
