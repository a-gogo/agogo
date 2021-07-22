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

package ch.puzzle.itc.mobiliar.business.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ch.puzzle.itc.mobiliar.business.generator.control.extracted.properties.AppServerRelationProperties;
import ch.puzzle.itc.mobiliar.business.generator.control.extracted.templates.GenerationUnit;
import ch.puzzle.itc.mobiliar.business.property.entity.AmwResourceTemplateModel;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.test.EntityBuilderType;

import com.google.common.io.Resources;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class TestUtils {
	public static String readRecursionTemplate() {
		try {
			return Resources.toString(Resources.getResource("rekursiv_macro_4.txt"), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<ResourceEntity> filterResources(List<ResourceEntity> list, final String typeName) {
		return list.stream()
				   .filter(r -> r.getResourceType().getName().equals(typeName))
				   .collect(Collectors.<ResourceEntity>toList());
	}

	public static String get(Map<String, Object> map, String... keys) {
		Object fromMap = getFromMap(map, keys);
		return fromMap == null ? null : fromMap.toString();
	}

    public static TemplateHashModel asHashModel(TemplateHashModel properties, String... keys) throws TemplateModelException {

        TemplateHashModel hashModel = properties;

        for (String key : keys) {
            TemplateModel templateModel = hashModel.get(key);
            if(templateModel instanceof TemplateHashModel){
                hashModel = (TemplateHashModel)templateModel;
            }
        }

        return hashModel;
    }

    public static TemplateHashModel asHashModel(TemplateHashModel properties, String key) throws TemplateModelException {
        return (TemplateHashModel)properties.get(key);
    }

	// get values from a nested map
	private static Object getFromMap(Map<String, Object> map, String... keys) {
		Object result = null;
        AmwResourceTemplateModel model = null;

		for (String key : keys) {
            if(model != null){
                try {
                    result = model.get(key);
                } catch (TemplateModelException e) {
                    e.printStackTrace();
                }
            }
			Object nested = map.get(key);
			if (nested instanceof Map) {
				map = (Map<String, Object>) nested;
				result = map;
			}
            else if(nested instanceof AmwResourceTemplateModel){
                model = ((AmwResourceTemplateModel)nested);
            }
			else {
				result = nested;
			}
			if (result == null) {
				return null;
			}
		}
		return result;
	}


	public static AppServerRelationProperties propertiesForRelation(List<AppServerRelationProperties> list,
			final EntityBuilderType entityType) {

		return list.stream()
				   .filter(p -> p.getOwner().getResourceType().getName().equals(entityType.type))
				   .findFirst()
				   .orElse(null);
	}

	public static GenerationUnit unitFor(Set<GenerationUnit> units, final ResourceEntity entity) {
		return units.stream()
					.filter(u -> u.getSlaveResource().equals(entity))
					.findFirst()
					.orElse(null);
	}

	public static GenerationUnit unitFor(Set<GenerationUnit> units, final EntityBuilderType type) {
		return units.stream()
					.filter(u -> u.getSlaveResource().getName().equals(type.name))
					.findFirst()
					.orElse(null);
	}

	public static List<GenerationUnit> unitsFor(Set<GenerationUnit> units, final EntityBuilderType type) {
		return units.stream()
					.filter(u -> u.getSlaveResource().getName().equals(type.name))
					.collect(Collectors.<GenerationUnit>toList());
	}

	public static AppServerRelationProperties propertiesFor(Set<GenerationUnit> units, final EntityBuilderType type) {
		return units.stream()
					.filter(u -> u.getSlaveResource().getName().equals(type.name))
					.map(u -> u.getAppServerRelationProperties())
					.findFirst()
					.orElse(null);
	}

	public static Set<ResourceEntity> resources(Set<GenerationUnit> units) {
		return units.stream()
					.map(u -> u.getSlaveResource())
					.collect(Collectors.<ResourceEntity>toSet());
	}

	public static List<AppServerRelationProperties> properties(Set<GenerationUnit> units) {
		return units.stream()
					.map(u -> u.getAppServerRelationProperties())
					.collect(Collectors.<AppServerRelationProperties>toList());
	}
}
