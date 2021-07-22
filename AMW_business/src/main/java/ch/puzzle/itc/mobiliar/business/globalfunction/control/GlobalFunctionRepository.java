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

package ch.puzzle.itc.mobiliar.business.globalfunction.control;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.persistence.criteria.CriteriaQuery;

import ch.puzzle.itc.mobiliar.business.globalfunction.entity.GlobalFunctionEntity;

public class GlobalFunctionRepository {

	@Inject
    EntityManager entityManager;

	public List<GlobalFunctionEntity> getAllGlobalFunctions(){
		return entityManager.createQuery("from GlobalFunctionEntity order by LOWER(name) asc", GlobalFunctionEntity.class).getResultList();
	}

	public boolean saveFunction(GlobalFunctionEntity gFunction) {
		if (gFunction.getId() == null) {
			if (isExistingName(gFunction)) {
				return false;
			}
			entityManager.persist(gFunction);
		}
		else {
			entityManager.merge(gFunction);
		}
		return true;
	}

	public void deleteFunction(GlobalFunctionEntity gFunction){
		entityManager.remove(gFunction);
	}

	public boolean isExistingName(GlobalFunctionEntity gFunction) {
		return findFunctionByName(gFunction.getName()) != null;
	}

	private GlobalFunctionEntity findFunctionByName(String name) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<GlobalFunctionEntity> from = query.from(GlobalFunctionEntity.class);
		CriteriaQuery<Object> select = query.select(from);
		Predicate predicate = cb.equal(from.get("name"), name);
		query.where(predicate);
		TypedQuery<Object> tq = entityManager.createQuery(select);
		List<Object> result = tq.getResultList();
		return result.isEmpty() ? null : (GlobalFunctionEntity) result.get(0);
	}
}
