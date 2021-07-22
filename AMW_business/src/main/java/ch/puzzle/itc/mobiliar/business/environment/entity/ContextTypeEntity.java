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

package ch.puzzle.itc.mobiliar.business.environment.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Set;

import static javax.persistence.CascadeType.ALL;

/**
 * Entity implementation class for Entity: ResourceType
 *
 */
@Entity
@Audited
@Table(name="TAMW_contextType")
@NamedQuery(name= ContextTypeEntity.LOAD_CONTEXT_TYPE_BY_NAME_QUERY_NAME, query="select c from ContextTypeEntity c left join fetch c.contexts where c.name=:name", cacheable=true)

@AssociationOverrides({
	@AssociationOverride(name="properties", joinTable=@JoinTable(name="TAMW_contextType_prop", joinColumns = @JoinColumn(name="TAMW_CONTEXTTYPE_ID", referencedColumnName="ID"))),
	@AssociationOverride(name="propertyDescriptors", joinTable=@JoinTable(name="TAMW_contextType_propDesc", joinColumns = @JoinColumn(name="TAMW_CONTEXTTYPE_ID", referencedColumnName="ID"))),
	@AssociationOverride(name="templates", joinTable=@JoinTable(name="TAMW_contextType_tmplDesc", joinColumns = @JoinColumn(name="TAMW_CONTEXTTYPE_ID", referencedColumnName="ID")))
})
public class ContextTypeEntity extends AbstractContext {
    public static final String LOAD_CONTEXT_TYPE_BY_NAME_QUERY_NAME = "loadContextTypeByName";

    	@Getter
	@Setter
	@OneToMany(mappedBy = "contextType", cascade = ALL)
	private Set<ContextEntity> contexts;
	@Getter
	@Setter
	private String name;

	private static final long serialVersionUID = 1L;

}
