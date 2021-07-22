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

package ch.puzzle.itc.mobiliar.business.database.control;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.exception.RevisionDoesNotExistException;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.Date;

/**
 * The AutidReader Reads Audited Entities for a given Date
 */
public class AmwAuditReader {

	@Inject
	private EntityManager entityManager;

	protected AuditReader getAuditReader() {
		return AuditReaderFactory.get(entityManager);
	}

	/**
	 * @param clazz
	 *             - the entity class which shall be found in the history of the database
	 * @param date
	 *             - the date for which the result shall be loaded in the history of data
	 * @param id
	 *             - the id of the entity class that shall be loaded
	 * @return the entity-instance with its state at the given date or null if the entity didn't exist at
	 *         that time
	 * @throws IllegalArgumentException
	 *              - if the date is null (re-thrown from {@link AuditReader#getRevisionNumberForDate(Date)})
	 */
	public <T> T getByDate(final Class<T> clazz, final Date date, final Integer id)
			throws IllegalArgumentException {
		final AuditReader r = getAuditReader();
		Number currentRevisionNumber = null;
		try {
			// find the revision number for the given date
			currentRevisionNumber = r.getRevisionNumberForDate(date);
		}
		catch (final RevisionDoesNotExistException e) {
			return null;
		}
		return r.find(clazz, id, currentRevisionNumber);
	}
}
