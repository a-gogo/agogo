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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;

public class QueryUtils {


	/**
	 * Convenience method that returns a subset (defined by start and length) of results casted to the given
	 * type.
	 * 
	 * @param commonDomainService
	 *             TODO
	 * @param clazz
	 *             - the type the result set should be casted to
	 * @param query
	 *             - the query to be executed
	 * @param start
	 *             - the (0 based) index from which the result set should start
	 * @param length
	 *             - the length of the result set, no restriction if -1
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> fetch(Class<T> clazz, Query query, int start, int length) {
		query.setFirstResult(start);
		if (length != -1) {
			query.setMaxResults(length);
		}
		return query.getResultList();

	}

	/**
	 * Convenience method to return a single result of a query
	 * 
	 * @param q
	 * @return the single result of the query, null if no result is found.
	 */
	public static <T> T singleResult(Query q) {
		try {
			return (T) q.getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * @param data
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	public static String clobToString(Clob data) throws SQLException, IOException {
		StringBuilder sb = new StringBuilder();
		if (data != null) {
			try {
				Reader reader = data.getCharacterStream();
				BufferedReader br = new BufferedReader(reader);

				String line;
				while (null != (line = br.readLine())) {
					sb.append(line);
				}
				br.close();
			}
			catch (SQLException e) {
				throw e;
			}
			catch (IOException e) {
				throw e;
			}
		}
		String result = sb.toString();
		return !result.isEmpty() ? sb.toString() : null;
	}

	public static String getTable(Class<?> entityClass) {
		return entityClass.getAnnotation(Table.class).name();
	}

}
