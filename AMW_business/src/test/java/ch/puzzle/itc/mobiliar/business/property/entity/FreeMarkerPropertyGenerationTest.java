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

package ch.puzzle.itc.mobiliar.business.property.entity;

import freemarker.cache.StringTemplateLoader;
import freemarker.core.InvalidReferenceException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class FreeMarkerPropertyGenerationTest {

    @Test
    public void testTechnicalKey() throws Exception {
        // given
        String testTemplateContent = "Value: ${property} TechnicalKey: ${property._descriptor.technicalKey}";
        // set up data
        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        des.setPropertyName("techkey");
        FreeMarkerProperty p = new FreeMarkerProperty("value", des);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("property", p);

        // when
        String result = generate(testTemplateContent, data);
        // then
        assertEquals("Value: value TechnicalKey: techkey", result);
    }
    @Test
    public void testCurrentValue() throws Exception {
        // given
        String testTemplateContent = "Value: ${property.currentValue}";
        // set up data
        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        des.setPropertyName("techkey");
        FreeMarkerProperty p = new FreeMarkerProperty("value", des);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("property", p);

        // when
        String result = generate(testTemplateContent, data);
        // then
        assertEquals("Value: value", result);
    }

    @Test(expected = InvalidReferenceException.class)
    public void testCurrentNullProperties() throws Exception {
        // given
        String testTemplateContent = "Value: ${property.test}";
        // set up data
        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        des.setPropertyName("techkey");
        FreeMarkerProperty p = new FreeMarkerProperty("value", des);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("property", p);

        // when
        generate(testTemplateContent, data);
        // then
        fail();
    }

    private String generate(String testTemplateContent, Map<String, Object> data) throws IOException, TemplateException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_22);
        StringTemplateLoader loader = new StringTemplateLoader();

        String testTemplateName = "template";

        loader.putTemplate(testTemplateName,testTemplateContent);
        cfg.setTemplateLoader(loader);

        Writer fileContentWriter = new StringWriter();
        Template t = cfg.getTemplate(testTemplateName);
        // generate
        t.process(data, fileContentWriter);

        fileContentWriter.flush();
        return fileContentWriter.toString();
    }
}