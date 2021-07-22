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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;

import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceTypeEntity;
import ch.puzzle.itc.mobiliar.common.util.DefaultResourceTypeDefinition;

public class ResourceTypeEntityBuilder extends BaseEntityBuilder {

	private static final int APP_TYPE_ID = 1;
	private static final int AS_TYPE_ID = 2;
	private static final int NODE_TYPE_ID = 3;
    private static final int RUNTIME_TYPE_ID = 4;

    public ResourceTypeEntityBuilder(){
        super(new Integer[]{APP_TYPE_ID, AS_TYPE_ID, NODE_TYPE_ID, RUNTIME_TYPE_ID});
    }

	public ResourceTypeEntity mockResourceTypeEntity(String name, Set<ResourceEntity> resources) {
		ResourceTypeEntity mock = mock(ResourceTypeEntity.class);
		int id = getNextId();
		when(mock.getId()).thenReturn(id);
		if (!StringUtils.isEmpty(name)) {
			when(mock.getName()).thenReturn(name);
		} else {
			when(mock.getName()).thenReturn("type" + id);
		}
		when(mock.isDefaultResourceType()).thenReturn(false);
		when(mock.isApplicationServerResourceType()).thenReturn(false);
		when(mock.isApplicationResourceType()).thenReturn(false);
		when(mock.isNodeResourceType()).thenReturn(false);

		if (resources != null) {
			when(mock.getResources()).thenReturn(resources);
			for (ResourceEntity resource : resources) {
				if (Mockito.mockingDetails(resource).isMock()) {
					when(resource.getResourceType()).thenReturn(mock);
				}
			}
		} else {
			when(mock.getResources()).thenReturn(new HashSet<ResourceEntity>());
		}

		return mock;
	}

	public ResourceTypeEntity mockAppServerResourceTypeEntity(Set<ResourceEntity> resources) {
		ResourceTypeEntity mock = mockResourceTypeEntity(DefaultResourceTypeDefinition.APPLICATIONSERVER.getDisplayName(), resources);
		when(mock.isApplicationServerResourceType()).thenReturn(true);
		when(mock.isDefaultResourceType()).thenReturn(true);
		when(mock.isResourceType(DefaultResourceTypeDefinition.APPLICATIONSERVER)).thenReturn(true);
		when(mock.getId()).thenReturn(AS_TYPE_ID);
		return mock;
	}

	public ResourceTypeEntity mockApplicationResourceTypeEntity(Set<ResourceEntity> resources) {
		ResourceTypeEntity mock = mockResourceTypeEntity(DefaultResourceTypeDefinition.APPLICATION.getDisplayName(), resources);
		when(mock.isApplicationResourceType()).thenReturn(true);
		when(mock.isDefaultResourceType()).thenReturn(true);
		when(mock.isResourceType(DefaultResourceTypeDefinition.APPLICATION)).thenReturn(true);
		when(mock.getId()).thenReturn(APP_TYPE_ID);
		return mock;
	}

	public ResourceTypeEntity mockNodeResourceTypeEntity(Set<ResourceEntity> resources) {
		ResourceTypeEntity mock = mockResourceTypeEntity(DefaultResourceTypeDefinition.NODE.getDisplayName(), resources);
		when(mock.isNodeResourceType()).thenReturn(true);
		when(mock.isDefaultResourceType()).thenReturn(true);
		when(mock.isResourceType(DefaultResourceTypeDefinition.NODE)).thenReturn(true);
		when(mock.getId()).thenReturn(NODE_TYPE_ID);
		return mock;
	}

    public ResourceTypeEntity mockRuntimeResourceTypeEntity(Set<ResourceEntity> resources) {
        ResourceTypeEntity mock = mockResourceTypeEntity(ResourceTypeEntity.RUNTIME, resources);
        when(mock.isNodeResourceType()).thenReturn(false);
        when(mock.isDefaultResourceType()).thenReturn(false);
        when(mock.isResourceType(any(DefaultResourceTypeDefinition.class))).thenReturn(false);
        when(mock.isRuntimeType()).thenReturn(true);
        when(mock.getId()).thenReturn(NODE_TYPE_ID);
        return mock;
    }

	public ResourceTypeEntity buildResourceTypeEntity(String name, Set<ResourceEntity> resources, boolean withId) {
		ResourceTypeEntity entity = new ResourceTypeEntity();
		if (withId) {
			if (DefaultResourceTypeDefinition.APPLICATION.getDisplayName().equals(name)) {
				entity.setId(APP_TYPE_ID);
			} else if (DefaultResourceTypeDefinition.APPLICATIONSERVER.getDisplayName().equals(name)) {
				entity.setId(AS_TYPE_ID);
			} else {
				Integer id = getNextId();
				entity.setId(id);
			}
		}
		if (!StringUtils.isEmpty(name)) {
			entity.setName(name);
		} else {
			entity.setName("type" + entity.getId());
		}

		if (resources != null) {
			entity.setResources(resources);
			for (ResourceEntity resource : resources) {
				resource.setResourceType(entity);
			}
		}

		return entity;
	}

	public ResourceTypeEntity buildAppServerResourceTypeEntity(Set<ResourceEntity> resources, boolean withId) {
		return buildResourceTypeEntity(DefaultResourceTypeDefinition.APPLICATIONSERVER.getDisplayName(), resources, withId);
	}

	public ResourceTypeEntity buildApplicationResourceTypeEntity(Set<ResourceEntity> resources, boolean withId) {
		return buildResourceTypeEntity(DefaultResourceTypeDefinition.APPLICATION.getDisplayName(), resources, withId);
	}

}
