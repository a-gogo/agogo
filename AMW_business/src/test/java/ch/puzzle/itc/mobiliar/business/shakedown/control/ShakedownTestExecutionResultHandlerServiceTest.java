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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ch.puzzle.itc.mobiliar.business.shakedown.entity.ShakedownTestEntity;
import ch.puzzle.itc.mobiliar.business.shakedown.entity.ShakedownTestEntity.shakedownTest_state;
import ch.puzzle.itc.mobiliar.common.exception.GeneratorException;
import ch.puzzle.itc.mobiliar.common.exception.GeneratorException.MISSING;

public class ShakedownTestExecutionResultHandlerServiceTest {

	@InjectMocks
	ShakedownTestExecutionResultHandlerService shakedownTestExecutionResultHandlerService;

	@Mock
	Logger log;

	@Mock
	ShakedownTestService shakedownTestService;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void test_handleSuccessfulShakedownTest() {
		// given
		ShakedownTestEntity test = mock(ShakedownTestEntity.class);
		when(test.getId()).thenReturn(12);
		when(test.getTestResult()).thenReturn("success");
		when(shakedownTestService.updateShakedownInfo(anyInt(), anyString(), any(shakedownTest_state.class))).thenReturn(test);

		// when
		shakedownTestExecutionResultHandlerService.handleSuccessfulShakedownTest(test);

		// then
		verify(shakedownTestService, times(1)).updateShakedownInfo(Integer.valueOf(12), "success", shakedownTest_state.success);
		verify(log, times(1)).info("Shakedown test 12 executed and result parsed");
	}

	@Test
	public void test_handleUnsuccessfulShakedownTest_withTestresult() {
		// given
		ShakedownTestEntity test = mock(ShakedownTestEntity.class);
		when(test.getId()).thenReturn(12);
		when(shakedownTestService.updateShakedownInfo(anyInt(), anyString(), any(shakedownTest_state.class))).thenReturn(test);

		// when
		shakedownTestExecutionResultHandlerService.handleUnsuccessfulShakedownTest("foo", 12);

		// then
		verify(shakedownTestService, times(1)).updateShakedownInfo(Integer.valueOf(12), "foo", shakedownTest_state.failed);
		verify(log, times(1)).log(Level.WARNING, "Shakedown test 12 had an issue with its generation", "foo");
	}

	@Test
	public void test_handleUnsuccessfulShakedownTest_withException() {
		// given
		GeneratorException exception = new GeneratorException("exception", MISSING.APPLICATION);
		ShakedownTestEntity test = mock(ShakedownTestEntity.class);
		when(test.getId()).thenReturn(12);
		when(shakedownTestService.updateShakedownInfo(anyInt(), anyString(), any(shakedownTest_state.class))).thenReturn(test);

		// when
		shakedownTestExecutionResultHandlerService.handleUnsuccessfulShakedownTest(exception, 12);

		// then
		verify(shakedownTestService, times(1)).updateShakedownInfo(Integer.valueOf(12), exception.getMessage(), shakedownTest_state.failed);
		verify(log, times(1)).log(Level.SEVERE, "Shakedown test 12 execution failed!", exception);
	}

	@Test
	public void test_handleUnsuccessfulShakedownTest_ResourceIdNotSet() {
		// given
		GeneratorException exception = new GeneratorException("exception", MISSING.APPLICATION);
		ShakedownTestEntity test = mock(ShakedownTestEntity.class);
		when(test.getId()).thenReturn(null);
		when(shakedownTestService.updateShakedownInfo(anyInt(), anyString(), any(shakedownTest_state.class))).thenReturn(test);

		// when
		shakedownTestExecutionResultHandlerService.handleUnsuccessfulShakedownTest(exception, null);

		// then
		verify(shakedownTestService, times(0)).updateShakedownInfo(null, exception.getMessage(), shakedownTest_state.failed);
		verify(log, times(1)).log(Level.SEVERE, "Shakedown test execution failed!", exception);
	}

}
