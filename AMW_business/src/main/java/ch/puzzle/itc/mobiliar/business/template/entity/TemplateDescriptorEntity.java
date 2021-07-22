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

package ch.puzzle.itc.mobiliar.business.template.entity;

import ch.puzzle.itc.mobiliar.business.auditview.entity.Auditable;
import ch.puzzle.itc.mobiliar.business.database.control.Constants;
import ch.puzzle.itc.mobiliar.business.environment.entity.AbstractContext;
import ch.puzzle.itc.mobiliar.business.generator.control.GeneratedTemplate;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceResult;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyUnit;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceGroupEntity;
import ch.puzzle.itc.mobiliar.business.utils.Copyable;
import ch.puzzle.itc.mobiliar.business.utils.Identifiable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Entity implementation class for Entity: TemplateDescriptor
 * 
 */
@Entity
@Audited
@Table(name = "TAMW_templateDescriptor")
public class TemplateDescriptorEntity implements Identifiable, Serializable, Copyable<TemplateDescriptorEntity>, Auditable {

	@TableGenerator(name = "templateDescriptorIdGen", table = Constants.GENERATORTABLE, pkColumnName = Constants.GENERATORPKCOLUMNNAME, valueColumnName = Constants.GENERATORVALUECOLUMNNAME, pkColumnValue = "templateDescriptorId")
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "templateDescriptorIdGen")
	@Id
	@Column(unique = true, nullable = false)
	private Integer id;
	@Column(length = 65536)
	@Lob
	private String fileContent;
	private String name;
	private String targetPath;

	@Column(nullable = false)
	private boolean testing = true;

	@ManyToMany(fetch=FetchType.EAGER)
	@Getter
	@Setter
	@JoinTable(name = "TAMW_tmplDesc_targetPlat", joinColumns = { @JoinColumn(name = "TEMPLATEDESCRIPTORS_ID", referencedColumnName = "ID") }, inverseJoinColumns = { @JoinColumn(name = "RESGROUP_ID", referencedColumnName = "ID") })
	private Set<ResourceGroupEntity> targetPlatforms;

	@Transient
	private String relatedResourceIdentifier;

	@Transient
	private AbstractContext ownerResource;

	@Getter
	@Setter
	@Transient
	private boolean relationTemplate;

	@Version
	private long v;

	private static final long serialVersionUID = 1L;

	public TemplateDescriptorEntity() {
		super();
	}

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return this.id;
    }

    @Override
    public String getNewValueForAuditLog() {
        return "";
    }

    @Override
    public String getType() {
        return Auditable.TYPE_TEMPLATE_DESCRIPTOR;
    }

    @Override
    public String getNameForAuditLog() {
        return this.name;
    }

	@Override
	public boolean isObfuscatedValue() {
		return false;
	}

	public String getFileContent() {
		// Oracle saves empty strings as null & Freemarker doesn't like null. See #7532
		if (this.fileContent == null) {
			return "";
		}

		return this.fileContent;
	}

	public void setFileContent(String fileContent) {
		this.fileContent = fileContent;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTargetPath() {
		return this.targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

	public AbstractContext getOwnerResource() {
		return ownerResource;
	}

	public void setOwnerResource(AbstractContext ownerResource) {
		this.ownerResource = ownerResource;
	}

	public boolean isTesting() {
		return testing;
	}

	public void setTesting(boolean testing) {
		this.testing = testing;
	}

	public long getV() {
		return v;
	}

	public boolean isDefaultTemplate(){
		return getName() == null;
	}

	public String getRelatedResourceIdentifier() {
		return relatedResourceIdentifier;
	}

	//TODO: unused?
	public void setRelatedResourceIdentifier(String relatedResourceIdentifier) {
		this.relatedResourceIdentifier = relatedResourceIdentifier;
	}

	@Override
	public String toString() {
		return "TemplateDescriptorEntity [id=" + id + ", name=" + name + ", targetPath=" + targetPath + ", ownerResource=" + ownerResource + "]";
	}

	@Override
	public TemplateDescriptorEntity getCopy(TemplateDescriptorEntity target, CopyUnit copyUnit) {
		boolean alreadyExists = true;
		if (target == null) {
			target = new TemplateDescriptorEntity();
			alreadyExists = false;
		}

		// name
		target.setName(this.getName());

		// testing
		target.setTesting(this.isTesting());

		// fileContent
		if (alreadyExists && !StringUtils.equals(target.getFileContent(), this.getFileContent())) {
			copyUnit.getResult().addTemplateChange(target.getId(), target.getName(), CopyResourceResult.CopyInfo.FILECONTENT_CHANGED);
		}
		target.setFileContent(this.getFileContent());

		// targetPath
		if (alreadyExists && !StringUtils.equals(target.getTargetPath(), this.getTargetPath())) {
			copyUnit.getResult().addTemplateChange(target.getId(), target.getName(), CopyResourceResult.CopyInfo.TARGETPATH_CHANGED);
		}
		target.setTargetPath(this.getTargetPath());

		// targetPlatforms
		if (this.getTargetPlatforms() != null) {
			if (target.getTargetPlatforms() == null) {
				target.setTargetPlatforms(new HashSet<ResourceGroupEntity>());
			}
			for (ResourceGroupEntity tPlatform : this.getTargetPlatforms()) {
				if (target.getTargetPlatforms().add(tPlatform) && alreadyExists) {
					copyUnit.getResult().addTemplateChange(target.getId(), target.getName(),
							CopyResourceResult.CopyInfo.TARGETPLATFORM_ADDED);
				}
			}
		}

		target.setRelatedResourceIdentifier(this.getRelatedResourceIdentifier());

		return target;
	}
	
	public Map<String, String> toHash() {
		Map<String, String> hash = new HashMap<>();
		hash.put(GeneratedTemplate.RESERVED_PROPERTY_PATH, this.targetPath);
		hash.put(GeneratedTemplate.RESERVED_PROPERTY_CONTENT, this.fileContent);
		hash.put(GeneratedTemplate.RESERVED_PROPERTY_NAME, this.name);
		hash.put(GeneratedTemplate.RESERVED_PROPERTY_IS_RELATION_TEMPLATE, String.valueOf(this.isRelationTemplate()));
		return hash;
	}
}
