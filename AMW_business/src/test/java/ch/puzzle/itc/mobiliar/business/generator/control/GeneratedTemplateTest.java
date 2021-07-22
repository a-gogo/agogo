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

import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException;
import ch.puzzle.itc.mobiliar.common.exception.TemplatePropertyException.CAUSE;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GeneratedTemplateTest {
	
	@Test
	public void should_NothaveErrors_non_added() {
		// given
		GeneratedTemplate template = new GeneratedTemplate(null, null, null);
		// when
		
		// then
		assertFalse(template.hasErrors());
		assertEquals("", template.getErrorMessageAsString());
	}
	
	@Test
	public void should_haveErrors_added() {
		// given
		GeneratedTemplate template = new GeneratedTemplate(null, null, null);
		List<TemplatePropertyException> errorMessages = new ArrayList<TemplatePropertyException>();
		errorMessages.add(new TemplatePropertyException("error", CAUSE.INVALID_PROPERTY));
		template.addAllErrorMessages(errorMessages);
		// when
		
		// then
		assertTrue(template.hasErrors());
	}
	
	@Test
	public void should_return_error_string() {
		// given
		GeneratedTemplate template = new GeneratedTemplate("name", "path", "content");
		List<TemplatePropertyException> errorMessages = new ArrayList<TemplatePropertyException>();
		errorMessages.add(new TemplatePropertyException("error", CAUSE.INVALID_PROPERTY));
		template.addAllErrorMessages(errorMessages);
		// when
		
		// then
		assertEquals("Template name (path): error\n", template.getErrorMessageAsString());
	}
	
	@Test
	public void should_return_error_string_no_errors() {
		// given
		GeneratedTemplate template = new GeneratedTemplate("name", "path", "content");
		// when
		
		// then
		assertEquals("", template.getErrorMessageAsString());
	}

    @Test
    public void test_sameContent(){
	   GeneratedTemplate thisTemplate = new GeneratedTemplate("name", "path", "content");
	   GeneratedTemplate otherTemplate = new GeneratedTemplate("name2", "path2", "content");
	   Assert.assertTrue(thisTemplate.isSameContent(otherTemplate));
    }

    @Test
    public void test_sameContent_NOK(){
	   GeneratedTemplate thisTemplate = new GeneratedTemplate("name", "path", "content");
	   GeneratedTemplate otherTemplate = new GeneratedTemplate("name2", "path2", "contentX");
	   Assert.assertFalse(thisTemplate.isSameContent(otherTemplate));
    }

    @Test
    public void test_sameContent_omitted(){
	   GeneratedTemplate thisTemplate = new GeneratedTemplate("name", "path", "content");
	   thisTemplate.setOmitted(true);
	   GeneratedTemplate otherTemplate = new GeneratedTemplate("name2", "path2", "content");
	   otherTemplate.setOmitted(true);
	   Assert.assertTrue(thisTemplate.isSameContent(otherTemplate));
    }

    @Test
    public void test_sameContent_omitted_NOK(){
	   GeneratedTemplate thisTemplate = new GeneratedTemplate("name", "path", "content");
	   thisTemplate.setOmitted(true);
	   GeneratedTemplate otherTemplate = new GeneratedTemplate("name2", "path2", "contentX");
	   otherTemplate.setOmitted(true);
	   Assert.assertFalse(thisTemplate.isSameContent(otherTemplate));
    }

    @Test
    public void testGetOmittedContent(){
	   GeneratedTemplate thisTemplate = new GeneratedTemplate("name", "path", "content");
	   thisTemplate.setOmitted(true);
	   Assert.assertTrue(thisTemplate.getContent().contains("omitted"));
	   Assert.assertTrue(thisTemplate.isOmitted());
    }

    @Test
    public void testGetNotOmittedContent(){
	   GeneratedTemplate thisTemplate = new GeneratedTemplate("name", "path", "content");
	   Assert.assertFalse(thisTemplate.getContent().contains("omitted"));
	   Assert.assertFalse(thisTemplate.isOmitted());
    }
}
