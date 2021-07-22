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

package ch.puzzle.itc.mobiliar.business.shakedown.xmlmodel;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class STSTest {

	STS sts = new STS();

	@Before
	public void setUp() throws Exception {
		sts.setTestId(12);
	}

	@Test
	public void testGetShakedowntestsAsCSV() {
		List<String> shakedownTests = new ArrayList<String>();
		shakedownTests.add("stp_module1 firstparam \"param with"+System.lineSeparator()+"space\"");	
		shakedownTests.add(" stp_module2 secondparam");
		shakedownTests.add("stp_module3");
		sts.setShakedowntests(shakedownTests);
		
		String csv = sts.getShakedowntestsAsCSV();
		Assert.assertTrue(csv.startsWith("12"));
		
		String line = csv.substring(csv.indexOf(System.lineSeparator())+System.lineSeparator().length());
		Assert.assertEquals("stp_module1\tfirstparam \"param with space\""+System.lineSeparator()+"stp_module2\tsecondparam"+System.lineSeparator()+"stp_module3"+System.lineSeparator(), line);
		System.out.println(line);		
	}

}
