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

package ch.puzzle.itc.mobiliar.business.function.entity;

import ch.puzzle.itc.mobiliar.business.property.entity.MikEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.when;

public class AmwFunctionEntityBuilder {

	private String name;
	private Integer id;
	private String implementation;
	private ResourceEntity resource;
	private ResourceTypeEntity resourceType;
	private Set<MikEntity> miks = new HashSet<>();
	private AmwFunctionEntity overwrittenParent;

	public AmwFunctionEntityBuilder(String name, Integer id) {
		this.name = name;
		this.id = id;
	}

	public AmwFunctionEntityBuilder withImplementation(String implementation) {
		this.implementation = implementation;
		return this;
	}

	public AmwFunctionEntityBuilder forResource(ResourceEntity resource) {
		this.resource = resource;
		return this;
	}

	public AmwFunctionEntityBuilder forResourceType(ResourceTypeEntity resourceType) {
		this.resourceType = resourceType;
		return this;
	}

	public AmwFunctionEntityBuilder with(MikEntity... miks) {
		this.miks = new HashSet<MikEntity>(Arrays.asList(miks));
		return this;
	}

	public AmwFunctionEntityBuilder withOverwrittenParent(AmwFunctionEntity overwrittenParent){
		this.overwrittenParent = overwrittenParent;
		return this;
	}

    public AmwFunctionEntity build(){
        AmwFunctionEntity amwFunctionEntity = new AmwFunctionEntity();
        amwFunctionEntity.setName(name);
        amwFunctionEntity.setId(id);
        amwFunctionEntity.setImplementation(implementation);
        if (resource != null) {
			amwFunctionEntity.setResource(resource);
		} else if (resourceType != null) {
			amwFunctionEntity.setResourceType(resourceType);
		}
        amwFunctionEntity.setMiks(miks);
		amwFunctionEntity.overwrite(overwrittenParent);
        return amwFunctionEntity;
    }

	public AmwFunctionEntity mock(){
		AmwFunctionEntity amwFunctionEntity = Mockito.mock(AmwFunctionEntity.class);
		when(amwFunctionEntity.getName()).thenReturn(name);
		when(amwFunctionEntity.getId()).thenReturn(id);
		when(amwFunctionEntity.getImplementation()).thenReturn(implementation);
		if (resource != null) {
			when(amwFunctionEntity.getResource()).thenReturn(resource);
		} else if (resourceType != null) {
			when(amwFunctionEntity.getResourceType()).thenReturn(resourceType);
		}

		Set<String> mikNames = new HashSet<>();
		if (miks != null) {
			for (MikEntity mik : miks) {
				mikNames.add(mik.getName());
			}
		}
		when(amwFunctionEntity.getMikNames()).thenReturn(mikNames);
		when(amwFunctionEntity.getOverwrittenParent()).thenReturn(overwrittenParent);
		return amwFunctionEntity;
	}

}