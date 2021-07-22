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

package ch.puzzle.itc.mobiliar.business.generator.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException.CAUSE;

public class GenerationUnitGenerationResultTest {

	@Test
	public void should_NothaveErrors_non_added() {
		// given
		GenerationUnitGenerationResult result = new GenerationUnitGenerationResult();
		// when
		
		// then
		assertTrue(result.isSuccess());
		assertEquals("", result.getErrorMessageAsString());
	}
	
	@Test
	public void should_haveErrors_added() {
		// given
		GenerationUnitGenerationResult result = new GenerationUnitGenerationResult();

		result.addErrorMessage(new TemplatePropertyException("error", CAUSE.INVALID_PROPERTY));
		// when
		
		// then
		assertFalse(result.isSuccess());
	}
	
	@Test
	public void should_haveErrors_Errors_in_generated_template() {
		// given
		GenerationUnitGenerationResult result = new GenerationUnitGenerationResult();
		GeneratedTemplate generatedTemplate = Mockito.mock(GeneratedTemplate.class);
		
		Mockito.when(generatedTemplate.hasErrors()).thenReturn(Boolean.TRUE);
		List<GeneratedTemplate> templates = new ArrayList<GeneratedTemplate>();
		templates.add(generatedTemplate);
		result.setGeneratedTemplates(templates);

		// when
		
		// then
		assertFalse(result.isSuccess());
	}
	
	@Test
	public void should_NotHaveErrors_NoErrors_in_generated_template() {
		// given
		GenerationUnitGenerationResult result = new GenerationUnitGenerationResult();
		GeneratedTemplate generatedTemplate = Mockito.mock(GeneratedTemplate.class);
		
		Mockito.when(generatedTemplate.hasErrors()).thenReturn(Boolean.FALSE);
		List<GeneratedTemplate> templates = new ArrayList<GeneratedTemplate>();
		templates.add(generatedTemplate);
		result.setGeneratedTemplates(templates);

		// when
		
		// then
		assertTrue(result.isSuccess());
		assertEquals("", result.getErrorMessageAsString());
	}
	
	@Test
	public void should_haveErrors_NoErrors_in_generated_templateMultiple() {
		// given
		List<GeneratedTemplate> templates = new ArrayList<GeneratedTemplate>();
		
		GenerationUnitGenerationResult result = new GenerationUnitGenerationResult();
		GeneratedTemplate generatedTemplate = Mockito.mock(GeneratedTemplate.class);
		Mockito.when(generatedTemplate.hasErrors()).thenReturn(Boolean.FALSE);
		
		
		GeneratedTemplate generatedTemplate2 = Mockito.mock(GeneratedTemplate.class);
		Mockito.when(generatedTemplate2.hasErrors()).thenReturn(Boolean.TRUE);
		
		templates.add(generatedTemplate2);
		
		result.setGeneratedTemplates(templates);

		// when
		
		// then
		assertFalse(result.isSuccess());
	}
	
	
	@Test
	public void should_return_error_string() {
		// given
		GenerationUnitGenerationResult result = new GenerationUnitGenerationResult();
		// add General Error
		result.addErrorMessage(new TemplatePropertyException("error", CAUSE.INVALID_PROPERTY));
		
		// Template Errors
		GeneratedTemplate generatedTemplate = Mockito.mock(GeneratedTemplate.class);
		Mockito.when(generatedTemplate.hasErrors()).thenReturn(Boolean.TRUE);
		Mockito.when(generatedTemplate.getErrorMessageAsString()).thenReturn("GeneratedTemplateError\n");
		List<GeneratedTemplate> templates = new ArrayList<GeneratedTemplate>();
		templates.add(generatedTemplate);
		result.setGeneratedTemplates(templates);
		
		// when
		String message = result.getErrorMessageAsString();
		// then
		
		String expected = "General Unit Template Errors\n"
						+ "error\n"
						+ "Template Errors\n"
						+ "GeneratedTemplateError\n";
		
		assertEquals(expected, message);
	}

}
