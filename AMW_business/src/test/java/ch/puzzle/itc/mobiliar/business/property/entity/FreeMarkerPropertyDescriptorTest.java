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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class FreeMarkerPropertyDescriptorTest {

    @Test
    public void testHasTag_true() throws Exception {
        // given
        PropertyTagEntity tag = new PropertyTagEntity();
        tag.setName("tag");
        List<PropertyTagEntity> tags = new ArrayList<>();
        tags.add(tag);

        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        des.setPropertyTags(tags);
        FreeMarkerPropertyDescriptor p = new FreeMarkerPropertyDescriptor(des);

        // then
        assertTrue(p.hasTag("tag"));
    }
    @Test
    public void testHasTag_null() throws Exception {
        // given
        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        FreeMarkerPropertyDescriptor p = new FreeMarkerPropertyDescriptor(des);

        // then
        assertFalse(p.hasTag("tag"));
        assertEquals(0, p.getTags().size());
    }
    @Test
    public void testHasTag_ignoreCase() throws Exception {
        // given
        PropertyTagEntity tag = new PropertyTagEntity();
        tag.setName("TAG");
        List<PropertyTagEntity> tags = new ArrayList<>();
        tags.add(tag);

        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        des.setPropertyTags(tags);
        FreeMarkerPropertyDescriptor p = new FreeMarkerPropertyDescriptor(des);

        // then
        assertTrue(p.hasTag("tag"));
    }

    @Test
    public void testHasTag_notFound() throws Exception {
        // given
        PropertyTagEntity tag = new PropertyTagEntity();
        tag.setName("tag1");
        PropertyTagEntity tag2 = new PropertyTagEntity();
        tag2.setName("tag2");
        List<PropertyTagEntity> tags = new ArrayList<>();
        tags.add(tag);
        tags.add(tag2);

        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        des.setPropertyTags(tags);
        FreeMarkerPropertyDescriptor p = new FreeMarkerPropertyDescriptor(des);

        // then
        assertFalse(p.hasTag("tag"));
    }

    @Test
    public void testHasTag_propertyTagEntity() throws Exception {
        // given
        PropertyTagEntity tag = new PropertyTagEntity();
        tag.setName("tag1");
        PropertyTagEntity tag2 = new PropertyTagEntity();
        tag2.setName("tag2");
        List<PropertyTagEntity> tags = new ArrayList<>();
        tags.add(tag);
        tags.add(tag2);

        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        des.setPropertyTags(tags);
        FreeMarkerPropertyDescriptor p = new FreeMarkerPropertyDescriptor(des);

        // then
        assertTrue(p.hasTag(tag));
    }
    @Test
    public void testHasTag_propertyTagEntity_null() throws Exception {
        // given
        PropertyTagEntity tag = new PropertyTagEntity();
        tag.setName("tag1");
        PropertyTagEntity tag2 = new PropertyTagEntity();
        tag2.setName("tag2");
        List<PropertyTagEntity> tags = new ArrayList<>();
        tags.add(tag);
        tags.add(tag2);

        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        des.setPropertyTags(tags);
        FreeMarkerPropertyDescriptor p = new FreeMarkerPropertyDescriptor(des);

        PropertyTagEntity tagParam = null;

        // then
        assertFalse(p.hasTag(tagParam));
    }

    @Test
    public void testGetTags() throws Exception {
        // given
        PropertyTagEntity tag = new PropertyTagEntity();
        tag.setName("tag1");
        PropertyTagEntity tag2 = new PropertyTagEntity();
        tag2.setName("tag2");
        List<PropertyTagEntity> tags = new ArrayList<>();
        tags.add(tag);
        tags.add(tag2);

        PropertyDescriptorEntity des = new PropertyDescriptorEntity();
        des.setPropertyTags(tags);
        FreeMarkerPropertyDescriptor p = new FreeMarkerPropertyDescriptor(des);

        // when
        Set<String> result =  p.getTags();

        // then
        assertEquals(2, result.size());
        assertTrue(result.contains("tag1"));
        assertTrue(result.contains("tag2"));
    }
}