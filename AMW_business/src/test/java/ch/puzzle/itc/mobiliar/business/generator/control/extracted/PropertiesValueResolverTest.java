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

package ch.puzzle.itc.mobiliar.business.generator.control.extracted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

public class PropertiesValueResolverTest {

	private Map<String, Object> root = new TreeMap<String, Object>();
	private Map<String, String> env = new TreeMap<String, String>();
	private Map<String, String> leave = new TreeMap<String, String>();
	private Map<String, Object> nested = new TreeMap<String, Object>();

	@Before
	public void setup() {
		env.put("name", "test");
		leave.put("name", "DB2_leave_${env.name?capitalize}");
		nested.put("name", "DB2_nested_${env.name?capitalize}");
		nested.put("leave", leave);
		root.put("name", "DB2_root_${env.name?capitalize}");
		root.put("nested", nested);
	}


	@Test
	public void testVarRegex() {
		String patternStr = "^.*\\$\\{(.*)\\}.*$";
		String str = "DB2${env.name?capitalize}ROOT";
		Pattern p = Pattern.compile(patternStr);
		Matcher m = p.matcher(str);
		assertTrue(m.matches());
		assertEquals("env.name?capitalize", m.group(1));
	}

	@Test
	public void testValueRegex() {
		String patternStr = "^env\\.(\\d+|\\w+).*$";
		String str = "env.name?capitalize";
		Pattern p = Pattern.compile(patternStr);
		Matcher m = p.matcher(str);
		assertTrue(m.matches());
		assertEquals("name", m.group(1));
	}
}
