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

package ch.puzzle.itc.mobiliar.business.shakedown.control;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import ch.puzzle.itc.mobiliar.business.generator.control.ShakedownTestGeneratorDomainService;
import ch.puzzle.itc.mobiliar.business.security.interceptor.HasPermission;
import ch.puzzle.itc.mobiliar.business.template.control.TemplatesScreenDomainService;
import ch.puzzle.itc.mobiliar.business.shakedown.entity.ShakedownStpEntity;
import ch.puzzle.itc.mobiliar.business.security.entity.Permission;
import ch.puzzle.itc.mobiliar.common.exception.ElementAlreadyExistsException;
import ch.puzzle.itc.mobiliar.common.exception.StpNotFoundException;
import ch.puzzle.itc.mobiliar.common.exception.TemplateNotDeletableException;

import static ch.puzzle.itc.mobiliar.business.security.entity.Action.CREATE;
import static ch.puzzle.itc.mobiliar.business.security.entity.Action.DELETE;
import static ch.puzzle.itc.mobiliar.business.security.entity.Action.UPDATE;

@Stateless
public class ShakedownStpService {

	@Inject
	private Logger log;

	@PersistenceContext
	EntityManager em;

	@Inject
	TemplatesScreenDomainService templatesScreenDomainService;

	public List<ShakedownStpEntity> getSTPs() {
		try {
			TypedQuery<ShakedownStpEntity> q = em.createQuery(
					"from ShakedownStpEntity order by lower(stpName)", ShakedownStpEntity.class);
			return q.getResultList();
		}
		catch (Exception e) {
			return new ArrayList<>();
		}
	}

	public List<ShakedownStpEntity> getSTPsWithoutSTS() {
		try {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<ShakedownStpEntity> q = cb.createQuery(ShakedownStpEntity.class);
			Root<ShakedownStpEntity> r = q.from(ShakedownStpEntity.class);
			Predicate stsPred= cb.notLike(r.<String> get("stpName"), ShakedownTestGeneratorDomainService.STS_NAME);

			q.where(stsPred);
			q.orderBy(cb.asc(cb.lower(r.<String> get("stpName"))));

			TypedQuery<ShakedownStpEntity> query = em.createQuery(q);
			return query.getResultList();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	@HasPermission(permission = Permission.SHAKEDOWNTEST, action = DELETE)
	public void deleteSTPEntity(final Integer stpId) throws StpNotFoundException, TemplateNotDeletableException {
		ShakedownStpEntity shakedownStpEntity = getSTPById(stpId);
		if (shakedownStpEntity == null) {
			String message = "Die zu löschende STP Entität ist nicht vorhanden ";
			log.info(message);
			throw new StpNotFoundException(message);
		}

		// If delete SPT, delete all defined testing-template with
		// this STP
		templatesScreenDomainService.deleteSTPTemplate(shakedownStpEntity.getStpName());
		em.remove(shakedownStpEntity);
		log.info("STP mit der Id: " + stpId + " wurde aus der DB gelöscht");

	}

	private ShakedownStpEntity getSTPById(final Integer id) {
		ShakedownStpEntity shakedownStpEntity = null;
		try {
			shakedownStpEntity = (ShakedownStpEntity) em
					.createQuery("from ShakedownStpEntity test where test.id=:id")
					.setParameter("id", id).getSingleResult();
		}
		catch (NoResultException nre) {
			String message = "Die STP Entität mit der id" + id + " existiert nicht aud der DB";
			log.log(Level.WARNING, message);
		}
		return shakedownStpEntity;
	}

	private ShakedownStpEntity getSTPByName(final String stpName) {
		ShakedownStpEntity shakedownStpEntity = null;
		try {
			shakedownStpEntity = (ShakedownStpEntity) em
					.createQuery("from ShakedownStpEntity test where test.stpName=:stpNewName")
					.setParameter("stpNewName", stpName).getSingleResult();
		}
		catch (NoResultException nre) {
			String message = "Die STP Entität " + stpName + " existiert nicht aud der DB";
			log.log(Level.WARNING, message);
		}
		return shakedownStpEntity;
	}

	@HasPermission(permission = Permission.SHAKEDOWNTEST, action = CREATE)
	public ShakedownStpEntity createNewSTP(final String newSTPName, final String newSTPVersion,
			final List<String> args) throws ElementAlreadyExistsException {
		ShakedownStpEntity shakedownStpEntity = getSTPByName(newSTPName);
		ShakedownStpEntity result;
		if (shakedownStpEntity == null) {
			result = new ShakedownStpEntity();
			result.setStpName(newSTPName);
			result.setVersion(newSTPVersion);
			if (args != null && !args.isEmpty()) {
				for (String arg : args) {
					if (arg != null) {
						result.addParameter(arg);
					}
				}
			}
			em.persist(result);
			log.info("STP " + newSTPName + " in DB persist");
		}
		else {
			String message = "Die STP Entität mit dem Namen: " + newSTPName
					+ " ist bereits vorhanden und kann nicht erstellen werden";
			log.info(message);
			throw new ElementAlreadyExistsException(message, ShakedownStpEntity.class, newSTPName);
		}
		return result;
	}

	/**
	 * @param stpEntity
	 * @param args
	 * @return list with not added args or null if some error occurs
	 */
	@HasPermission(permission = Permission.SHAKEDOWNTEST, action = UPDATE)
	public List<String> editSTPEntity(final ShakedownStpEntity stpEntity, final List<String> args) {
		List<String> notAddedArgs = new ArrayList<>();
		if (stpEntity == null) {
			String message = "Keine STP Entität ist selektiert";
			log.info(message);
			return null;
		} else {
			ShakedownStpEntity current = em.find(ShakedownStpEntity.class, stpEntity.getId());
			String oldName = current != null ? current.getStpName() : null;
			if (args != null && !args.isEmpty()) {
				stpEntity.setComaSeperatedParameters(null);
				for (String arg : args) {
					boolean success = stpEntity.addParameter(arg);
					if (!success) {
						notAddedArgs.add(arg);
					}
				}
			}
			em.merge(stpEntity);

			if (oldName != null && !stpEntity.getStpName().equals(oldName)) {
				String message = "STP mit dem Namen " + oldName + " wurde geändert und umbenannt zu " + stpEntity.getStpName();
				log.info(message);
				// the stpEntity was renamed, we have to rename the template accordingly
				templatesScreenDomainService.renameTestingTemplates(oldName, stpEntity.getStpName());
			} else {
				String message = "STP mit dem Namen " + stpEntity.getStpName() + " wurde geändert";
				log.info(message);
			}
		}
		return notAddedArgs;
	}

}
