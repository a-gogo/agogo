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

package ch.puzzle.itc.mobiliar.business.utils;

import org.junit.Test;

import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

import static org.junit.Assert.*;

public class ValidationHelperTest {

	@Test
	public void validateNotNullOrEmptyOnOneValidArgumentShouldValidate() {
		// given
		String arg1 = "not empty argument1";

		// when
		try {
			ValidationHelper.validateNotNullOrEmptyChecked(arg1);
		}
		catch (ValidationException e) {
			fail();
		}
	}

	@Test
	public void validateNotNullOrEmptyOnManyValidArgumentsShouldValidate() {
		// given
		String arg1 = "not empty argument1";
		String arg2 = "not empty argument2";
		String arg3 = "not empty argument3";

		// when
		try {
			ValidationHelper.validateNotNullOrEmptyChecked(arg1, arg2, arg3);
		}
		catch (ValidationException e) {
			fail();
		}
	}

	@Test(expected = ValidationException.class)
	public void validateNotNullOrEmptyOnOneEmptyArgumentShouldThrowException() throws ValidationException {
		// given
		String arg1 = "";

		// when
		ValidationHelper.validateNotNullOrEmptyChecked(arg1);
	}

	@Test(expected = ValidationException.class)
	public void validateNotNullOrEmptyOnOneNullArgumentShouldThrowException() throws ValidationException {
		// given
		String arg1 = null;

		// when
		ValidationHelper.validateNotNullOrEmptyChecked(arg1);
	}

    @Test(expected = ValidationException.class)
	public void validateNotNullOrEmptyOnAtLeastOneInvalidArgumentShouldThrowException() throws ValidationException {
		// given
		String arg1 = "not empty argument1";
		String arg2 = "";
		String arg3 = "not empty argument3";

		// when
		ValidationHelper.validateNotNullOrEmptyChecked(arg1, arg2, arg3);
	}

}