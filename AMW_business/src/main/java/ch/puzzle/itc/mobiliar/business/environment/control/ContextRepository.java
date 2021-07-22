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

package ch.puzzle.itc.mobiliar.business.environment.control;

import ch.puzzle.itc.mobiliar.business.database.control.QueryUtils;
import ch.puzzle.itc.mobiliar.business.deploy.entity.DeploymentEntity;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.utils.BaseRepository;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Set;

public class ContextRepository extends BaseRepository<ContextEntity> {

    @Inject
    EntityManager entityManager;

    public ContextEntity getContextByName(String name){
        return entityManager.createQuery("select c from ContextEntity c where LOWER(c.name)=:name", ContextEntity.class).setParameter("name", name.toLowerCase()).getSingleResult();
    }

    /**
     * Returns all Environments
     *
     * @return
     */
    public List<ContextEntity> getEnvironments() {
        return QueryUtils.fetch(ContextEntity.class, fetchAllContexts(), 0, -1);
    }

    protected Query fetchAllContexts() {
        return entityManager.createQuery("select n from ContextEntity n left join fetch n.children as c left join fetch n.contextType left join fetch c.contextType order by n.name asc");
    }

    @Override
    protected void preRemove(ContextEntity context) {
        final Set<DeploymentEntity> deploys = context.getDeploys();
        if (deploys != null) {
            final Integer contextId = context.getId();
            for (DeploymentEntity deployment : deploys) {
                deployment.setExContextId(contextId);
                deployment.setContext(null);
                entityManager.merge(deployment);
            }
        }
    }
}
