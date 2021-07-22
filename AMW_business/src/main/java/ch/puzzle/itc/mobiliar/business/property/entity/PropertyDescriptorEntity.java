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

package ch.puzzle.itc.mobiliar.business.property.entity;

import ch.puzzle.itc.mobiliar.business.database.control.Constants;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.Foreignable;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyUnit;
import ch.puzzle.itc.mobiliar.business.auditview.entity.Auditable;
import ch.puzzle.itc.mobiliar.business.utils.CopyHelper;
import ch.puzzle.itc.mobiliar.business.utils.Identifiable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.CascadeType.PERSIST;

/**
 * Entity implementation class for Entity: PropertyDescriptor
 */
@Entity
@Audited
@Table(name = "TAMW_propertyDescriptor")
public class PropertyDescriptorEntity implements Identifiable, Serializable, PropertyTagEntityHolder, Foreignable<PropertyDescriptorEntity>, Auditable {

    // IMPORTANT! Whenever a new field (not relation to other entity) is added then this field must be added to foreignableFieldEquals method!!!

	@Getter
	@Setter
	@TableGenerator(name = "propertyDescriptorIdGen", table = Constants.GENERATORTABLE, pkColumnName = Constants.GENERATORPKCOLUMNNAME, valueColumnName = Constants.GENERATORVALUECOLUMNNAME, pkColumnValue = "propertyDescriptorId")
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "propertyDescriptorIdGen")
	@Id
	@Column(unique = true, nullable = false)
	private Integer id;

	@Setter
	@Getter
	@Column(nullable = false)
	private boolean encrypt;

	/**
	 * The propertyName represents the technical Key TODO: rename
	 */
	@Getter
	@Column
	private String propertyName;

	@Setter
	@Getter
	@Column(nullable = false)
	// TODO: Rename to valueOptional
	private boolean nullable;

	@Getter
	@Setter
	@Column(nullable = false)
	private boolean testing;

	@Setter
	private String validationLogic;

	@Getter
	@Setter
	@Column(length = 65536)
	@Lob
	private String propertyComment;

	/**
	 * Kardinalität uf de Properties definiert, öb das Systemeigeni Properties si. Das bedütet: We dert öppis
	 * anders als NULL definiert isch (wie z.B. Maven-Properties) de het das e spezielli bedütig. Im
	 * Bispilfall dass si über e Deployscreen chöi überschribe wärde. D Kardinalität het uswirkige druf i
	 * welere Reihefolg si ufem Editscreen dargstellt wärde u öb si vom Benutzer chöi glöscht wärde oder nid
	 * (hei si e kardinalität chöi si nid glöscht wärde).
	 */
	@Getter
	@Setter
	private Integer cardinalityProperty;

	@Getter
	@Setter
	@ManyToOne
	private PropertyTypeEntity propertyTypeEntity;

    public Set<PropertyEntity> getProperties() {
        if (properties == null){
            properties = new HashSet<>();
        }
        return properties;
    }

    @Setter
	@OneToMany(mappedBy = "descriptor", cascade = ALL, orphanRemoval = true)
	private Set<PropertyEntity> properties;

	@Getter
	@Version
	private long v;

	@Getter
	@Lob
	private String defaultValue;

	@Getter
	@Lob
	private String exampleValue;

	@Getter
	@Setter
	private String machineInterpretationKey;

	@Getter
	@Setter
	// TODO: Rename to keyOptional
	private boolean optional;

	@Setter
	@ManyToMany(cascade = PERSIST)
	@JoinTable(
            name = "TAMW_propDesc_propTag",
            joinColumns = { @JoinColumn(name = "PROPERTYDESCRIPTOR_ID", referencedColumnName = "ID") },
            inverseJoinColumns = { @JoinColumn(name = "PROPERTYTAG_ID", referencedColumnName = "ID") })
	private List<PropertyTagEntity> propertyTags;

	@Getter
	@Setter
	private String displayName;

	private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    private ForeignableOwner fcOwner;

    private String fcExternalKey;
    private String fcExternalLink;

    /**
     * Creates new entity object with default system owner
     */
    public PropertyDescriptorEntity() {
        this(ForeignableOwner.getSystemOwner());
    }

    public PropertyDescriptorEntity(ForeignableOwner owner) {
        this.fcOwner = Objects.requireNonNull(owner, "Owner must not be null");
    }


    public List<PropertyTagEntity> getPropertyTags() {
		return propertyTags == null ? new ArrayList<PropertyTagEntity>() : propertyTags;
	}

	@Override
	public Set<String> getPropertyTagsNameSet() {
		Set<String> tagNames = new HashSet<>();
		for (PropertyTagEntity propertyTagEntity : getPropertyTags()) {
			tagNames.add(propertyTagEntity.getName());
		}
		return tagNames;
	}

	public void removeProperty(PropertyEntity p) {
		if (properties != null) {
			properties.remove(p);
		}
	}

	public void setPropertyName(String propertyName) {
		if (propertyName != null) {
			this.propertyName = propertyName.trim();
		}
	}

	public void addProperty(PropertyEntity property) {
		if (properties == null) {
			properties = new HashSet<>();
		}
		properties.add(property);
	}

	public void setDefaultValue(String value) {
		if (value != null) {
			defaultValue = removeLineBreaks(value);
		}
	}

	public void setExampleValue(String value) {
		if (value != null) {
			exampleValue = removeLineBreaks(value);
		}
	}

	@Override
	public String toString() {
		return "PropertyDescriptorEntity [id=" + id + ", propertyName=" + propertyName + "]";
	}

	@Transient
	protected static final String DEFAULTVALIDATIONEXPRESSION = ".*";

	/**
	 * The validation logic can either be defined in the property validation logic or as a regex of the
	 * property type. This method distincts between those different possibility and returns the actual
	 * validation pattern.
	 * 
	 * @return
	 */
	public String getValidationLogic() {
		String result = null;
		if (isCustomType()) {
			result = this.validationLogic == null ? DEFAULTVALIDATIONEXPRESSION : this.validationLogic;
		}
		else {
			result = this.validationLogic == null ? getPropertyTypeValidationRegex()
					: this.validationLogic;
		}
		return result;
	}

	private String getPropertyTypeValidationRegex(){
		return propertyTypeEntity != null ? propertyTypeEntity.getValidationRegex() : null;
	}

	public boolean isCustomType() {
		boolean isCustomType = false;
		if (getPropertyTypeEntity() == null || getPropertyTypeEntity().getId() == null) {
			isCustomType = true;
		}
		return isCustomType;
	}

	@Transient
	public static final Comparator<PropertyDescriptorEntity> CARDINALITY_AND_NAME_COMPARATOR = new Comparator<PropertyDescriptorEntity>() {

		@Override
		public int compare(PropertyDescriptorEntity o1, PropertyDescriptorEntity o2) {
			Integer cardinality = o2.getCardinalityProperty();
			if (cardinality == null) {
				cardinality = Integer.MAX_VALUE;
			}
			Integer myCardinality = o1.getCardinalityProperty();
			if (myCardinality == null) {
				myCardinality = Integer.MAX_VALUE;
			}

			if (!myCardinality.equals(cardinality)) {
				return myCardinality - cardinality;
			}
			return NAME_SORTING_COMPARATOR.compare(o1, o2);
		}
	};

	@Transient
	public static final Comparator<PropertyDescriptorEntity> NAME_SORTING_COMPARATOR = new Comparator<PropertyDescriptorEntity>() {

		@Override
		public int compare(PropertyDescriptorEntity o1, PropertyDescriptorEntity o2) {
			if (o1 == null || o1.getPropertyName() == null) {
				if (o2 == null || o2.getPropertyName() == null) {
					return 0;
				}
				else {
					return -1;
				}
			}
			else {
				if (o2 == null) {
					return 1;
				}
				else {
					return o1.getPropertyName().compareTo(o2.getPropertyName());
				}
			}
		}
	};

	@Override
	public void addPropertyTag(PropertyTagEntity propertyTagEntity) {
		if (propertyTags == null) {
			propertyTags = new ArrayList<>();
		}

		if (!hasTagWithName(propertyTagEntity)) {
			propertyTags.add(propertyTagEntity);
		}
	}

	private boolean hasTagWithName(PropertyTagEntity newTagEntity) {
		for (PropertyTagEntity tag : propertyTags) {
			if (newTagEntity != null && newTagEntity.getName().equals(tag.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void removePropertyTag(PropertyTagEntity propertyTagEntity) {
		if (propertyTags != null) {
			propertyTags.remove(propertyTagEntity);
		}
	}

	private String removeLineBreaks(String input) {
		return input.replaceAll("\\r\\n|\\r|\\n", " ");
	}
	
	/**
	 * @return the techkey if displayName is empty
	 */
	public String getPropertyDescriptorDisplayName() {
        if (this.displayName == null || this.displayName.isEmpty()) {
            return this.propertyName;
        }
        return displayName;
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
        return fcExternalLink;
    }

    @Override
    public void setExternalLink(String externalLink) {
        this.fcExternalLink = externalLink;
    }

    @Override
    public String getExternalKey() {
        return fcExternalKey;
    }

    @Override
    public void setExternalKey(String externalKey) {
        this.fcExternalKey = externalKey;
    }

    @Override
    public String getForeignableObjectName() {
        return this.getClass().getSimpleName();
    }

    @Override
	public PropertyDescriptorEntity getCopy(PropertyDescriptorEntity target, CopyUnit copyUnit) {
		if (target == null) {
			target = new PropertyDescriptorEntity();
		}
		// properties will be added in when copying PropertyEntity
		target.setEncrypt(isEncrypt());
		target.setPropertyName(getPropertyName());
		target.setNullable(isNullable());
		target.setTesting(isTesting());
		target.setValidationLogic(getValidationLogic());
		target.setPropertyComment(getPropertyComment());
		target.setCardinalityProperty(getCardinalityProperty());
		target.setPropertyTypeEntity(getPropertyTypeEntity());
		target.setDefaultValue(getDefaultValue());
		target.setExampleValue(getExampleValue());
		target.setMachineInterpretationKey(getMachineInterpretationKey());
		target.setOptional(isOptional());
		target.setDisplayName(getDisplayName());
		CopyHelper.copyForeignable(target, this, copyUnit);

		return target;
	}


    @Override
    public int foreignableFieldHashCode() {
        HashCodeBuilder eb = new HashCodeBuilder();
        eb.append(this.id);
        eb.append(this.displayName);
        eb.append(this.fcOwner);
        eb.append(this.fcExternalKey);
        eb.append(this.fcExternalLink);

        eb.append(this.defaultValue);
        eb.append(this.exampleValue);
        eb.append(this.machineInterpretationKey);
        eb.append(this.optional);
        eb.append(this.encrypt);
        eb.append(this.propertyName);
        eb.append(this.nullable);
        eb.append(this.testing);
        eb.append(this.validationLogic);
        eb.append(this.propertyComment);
        eb.append(this.cardinalityProperty);

        eb.append(this.propertyTypeEntity != null ? this.propertyTypeEntity.getId() : null);

        return eb.toHashCode();
    }

    @Override
    public String getNewValueForAuditLog() {
        return String.format(
                    "Technical Key: %s, " +
                    "\nPropertyType: %s, " +
                    "\nMIK: %s, " +
                    "\nDefault Value: %s " +
                    "\nDisplay Name: %s " +
                    "\nValidation: %s" +
                    "\nvalue optional: %s " + // nullable
                    "\nKey optional: %s " +  // optional
                    "\nEncrypted: %s " +
                    "\nExample value: %s" +
                    "\nComment: %s" +
                    "\nTags: %s",
                this.propertyName,
                this.propertyTypeEntity == null ? StringUtils.EMPTY : this.propertyTypeEntity.getPropertyTypeName(),
                this.machineInterpretationKey,
                this.defaultValue,
                this.displayName,
                this.getValidationLogic(),
                String.valueOf(this.isNullable()),
                String.valueOf(this.isOptional()),
                String.valueOf(this.isEncrypt()),
                this.exampleValue,
                this.propertyComment,
                this.getPropertyTags().toString()
                );
    }

    @Override
    public String getType() {
        return Auditable.TYPE_PROPERTY_DESCRIPTOR;
    }

    @Override
    public String getNameForAuditLog() {
        return this.propertyName;
    }

	@Override
	public boolean isObfuscatedValue() {
		return false;
	}
}
