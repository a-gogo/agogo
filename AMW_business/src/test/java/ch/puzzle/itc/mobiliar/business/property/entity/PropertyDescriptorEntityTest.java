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


import ch.puzzle.itc.mobiliar.builders.PropertyDescriptorEntityBuilder;
import ch.puzzle.itc.mobiliar.builders.PropertyEntityBuilder;
import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyResourceDomainService;
import ch.puzzle.itc.mobiliar.business.resourcegroup.control.CopyUnit;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class PropertyDescriptorEntityTest {
	

    private PropertyDescriptorEntity propertyDescriptorEntity;
    private PropertyDescriptorEntityBuilder propDescBuilder = new PropertyDescriptorEntityBuilder();
    private PropertyEntityBuilder propBuilder = new PropertyEntityBuilder();

    @Before
    public void setUp(){
        propertyDescriptorEntity = new PropertyDescriptorEntity();
    }

    @Test
    public void propertyNamesShallNotHaveLeadingSpaces(){
        // when
        propertyDescriptorEntity = new PropertyDescriptorEntity();
        propertyDescriptorEntity.setPropertyName(" Test");

        // then
        assertNotNull(propertyDescriptorEntity.getPropertyName());
        assertEquals("Test",propertyDescriptorEntity.getPropertyName());
    }

    @Test
    public void propertyNamesShallNotHaveTrailingSpaces(){
        // when
        propertyDescriptorEntity = new PropertyDescriptorEntity();
        propertyDescriptorEntity.setPropertyName("Test ");

        // then
        assertNotNull(propertyDescriptorEntity.getPropertyName());
        assertEquals("Test",propertyDescriptorEntity.getPropertyName());
    }

    @Test
    public void addPropertyTagOnEmptyTagsShouldSetNewTag(){
        // given
        String tagName = "tag1";
        PropertyTagEntity tag1 = createTagWithName(tagName);
        Assert.assertTrue(propertyDescriptorEntity.getPropertyTags().isEmpty());

        // when
        propertyDescriptorEntity.addPropertyTag(tag1);

        // then
        assertEquals(1, propertyDescriptorEntity.getPropertyTags().size());
    }

    @Test
    public void addPropertyTagWhenTagAlreadyExistsShouldNotAddTag(){
        // given
        String tagName = "tag1";
        PropertyTagEntity tag1 = createTagWithName(tagName);
        propertyDescriptorEntity.addPropertyTag(tag1);
        Assert.assertFalse(propertyDescriptorEntity.getPropertyTags().isEmpty());

        // when
        propertyDescriptorEntity.addPropertyTag(tag1);

        // then
        assertEquals(1, propertyDescriptorEntity.getPropertyTags().size());
    }

    @Test
    public void addPropertyTagWhenTagNotYetExistsShouldAddTag(){
        // given
        String tagName = "tag1";
        PropertyTagEntity tag1 = createTagWithName(tagName);
        propertyDescriptorEntity.addPropertyTag(tag1);
        Assert.assertFalse(propertyDescriptorEntity.getPropertyTags().isEmpty());

        // when
        propertyDescriptorEntity.addPropertyTag(createTagWithName("otherTagName"));

        // then
        assertEquals(2, propertyDescriptorEntity.getPropertyTags().size());
    }

    @Test
    public void getValidationLogic_shouldReturnValidationLogicFromDescriptor(){
        // given
        PropertyTypeEntity type = new PropertyTypeEntity();
        type.setId(1);
        String typeRegex = "*";
        type.setValidationRegex(typeRegex);
        propertyDescriptorEntity.setPropertyTypeEntity(type);
        String descRegex = "[0-9]*";
        propertyDescriptorEntity.setValidationLogic(descRegex);

        // when
        String validationLogic = propertyDescriptorEntity.getValidationLogic();

        // then
        assertEquals(descRegex, validationLogic);
    }

    @Test
    public void getValidationLogic_shouldReturnValidationLogicFromType(){
        // given
        PropertyTypeEntity type = new PropertyTypeEntity();
        type.setId(1);
        String typeRegex = "*";
        type.setValidationRegex(typeRegex);
        propertyDescriptorEntity.setPropertyTypeEntity(type);

        // when
        String validationLogic = propertyDescriptorEntity.getValidationLogic();

        // then
        assertEquals(typeRegex, validationLogic);
    }

    @Test
    public void getValidationLogic_shouldReturnDefaultValidationLogic(){
        // when
        String validationLogic = propertyDescriptorEntity.getValidationLogic();

        // then
        assertEquals(PropertyDescriptorEntity.DEFAULTVALIDATIONEXPRESSION, validationLogic);
    }

    private PropertyTagEntity createTagWithName(String name){
        PropertyTagEntity tag = new PropertyTagEntity();
        tag.setName(name);
        return tag;
    }

    @Test
    public void defaultConstructorShouldSetDefaultSystemOwner(){

        // when
        propertyDescriptorEntity = new PropertyDescriptorEntity();

        // then
        assertNotNull(propertyDescriptorEntity.getOwner());
        assertEquals(ForeignableOwner.getSystemOwner(),propertyDescriptorEntity.getOwner());
    }

    @Test
    public void constructorWithOwnerShouldSetOwner(){
        // given
        ForeignableOwner owner = ForeignableOwner.MAIA;

        // when
        propertyDescriptorEntity = new PropertyDescriptorEntity(owner);

        // then
        assertEquals(owner,propertyDescriptorEntity.getOwner());
    }

    @Test(expected = NullPointerException.class)
    public void constructorWithNullShouldThrowException(){
        // given
        ForeignableOwner owner = null;

        // when
        propertyDescriptorEntity = new PropertyDescriptorEntity(owner);
    }

    @Test
    public void foreignableFieldHashCodeWithSameFieldsShouldReturnTrue(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedIdShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withId(99).build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedDisplayNameShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withDisplayName("otherDisplayName").build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedOwnerShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withOwner(ForeignableOwner.MAIA).build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedExternalLinkShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withFcExternalLink("otherExternalLink").build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedExternalKeyShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withFcExternalKey("otherExternalKey").build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedDefaultValueShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withDefaultValue("otherDefaultValue").build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedExampleValueShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withExampleValue("otherExampletValue").build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedMikShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withMik("otherMIK").build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedOptionalShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().isOptional(true).build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedEncryptedShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().isEncrypted(true).build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedPropertyNameShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withPropertyName("otherPropertyName").build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedNullableShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().isNullable(true).build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedTestingShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().isTesting(true).build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedValidationLogicShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withValidationLogic("otherValidationLogic").build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedPropertyCommentShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withPropertyComment("otherPropertyComment").build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedCardinalityShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        int otherHashCode = new PropertyDescriptorEntityBuilder().withCardinalityProperty(999).build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void foreignableFieldHashCodeWithChangedPropertyTypeShouldBeDifferent(){
        // given
        propertyDescriptorEntity = new PropertyDescriptorEntityBuilder().build();
        PropertyTypeEntity otherType = new PropertyTypeEntity();
        otherType.setId(2000);
        int otherHashCode = new PropertyDescriptorEntityBuilder().withPropertyType(otherType).build().foreignableFieldHashCode();

        // when
        int foreignableFieldHashCode = propertyDescriptorEntity.foreignableFieldHashCode();

        // then
        assertNotEquals(otherHashCode, foreignableFieldHashCode);
    }

    @Test
    public void copyPropertyDescriptorEntity_should_create_target_if_null() throws AMWException{
        // given
        String origName = "foo";
        PropertyDescriptorEntity origin = createOriginPropDesc(origName);

        // when
        PropertyDescriptorEntity copy = origin.getCopy(null, new CopyUnit(null, new ResourceEntityBuilder().withName("targetResource").build(), CopyResourceDomainService.CopyMode.COPY, ForeignableOwner.AMW));
        PropertyDescriptorEntity release = origin.getCopy(null, new CopyUnit(null, new ResourceEntityBuilder().withName("targetResource").build(), CopyResourceDomainService.CopyMode.RELEASE, ForeignableOwner.AMW));

        // then
        assertPropDescCopy(copy, origin, null, null);
        assertPropDescCopy(release, origin, null, null);
    }

    @Test
    public void copyPropertyDescriptorEntity_existing() throws AMWException{
        // given
        String origName = "foo";
        PropertyDescriptorEntity origin = createOriginPropDesc(origName);

        String targetComment = "targetComment";
        String targetDefaultValue = "targetDefaultValue";
        String targetExampleValue = "targetExampleValue";
        String targetMik = "targetMik";
        String targetDisplayName = "targetDisplayName";
        String targetValue = "targetValue";
        PropertyTagEntity targetTag = new PropertyTagEntity();
        targetTag.setName("targetTag");
        PropertyEntity targetProp = propBuilder.buildPropertyEntity(targetValue, null);
        PropertyTypeEntity targetType = propDescBuilder.mockPropertyTypeEntity("type2");
        PropertyDescriptorEntity copyTarget = propDescBuilder.withGeneratedId().withPropertyName(origName)
                .withProperties(Collections.singleton(targetProp)).withPropertyComment(targetComment)
                .withDefaultValue(targetDefaultValue).withMik(targetMik)
                .withExampleValue(targetExampleValue).withPropertyType(targetType)
                .withDisplayName(targetDisplayName).withTags(targetTag).build();
        PropertyDescriptorEntity releaseTarget = propDescBuilder.withGeneratedId().withPropertyName(origName)
                .withProperties(Collections.singleton(targetProp)).withPropertyComment(targetComment)
                .withDefaultValue(targetDefaultValue).withMik(targetMik)
                .withExampleValue(targetExampleValue).withPropertyType(targetType)
                .withDisplayName(targetDisplayName).withTags(targetTag).build();

        Set<PropertyEntity> copyTargetProps = Collections.unmodifiableSet(copyTarget.getProperties());
        Set<PropertyEntity> releaseTargetProps = Collections.unmodifiableSet(releaseTarget.getProperties());

        // when
        PropertyDescriptorEntity copy = origin.getCopy(copyTarget, new CopyUnit(null, new ResourceEntityBuilder().withName("targetResource").build(), CopyResourceDomainService.CopyMode.COPY, ForeignableOwner.AMW));
        PropertyDescriptorEntity release = origin.getCopy(releaseTarget, new CopyUnit(null, new ResourceEntityBuilder().withName("targetResource").build(), CopyResourceDomainService.CopyMode.RELEASE, ForeignableOwner.AMW));

        // then
        assertPropDescCopy(copy, origin, copyTarget, copyTargetProps);
        assertPropDescCopy(release, origin, releaseTarget, releaseTargetProps);
    }

    private PropertyDescriptorEntity createOriginPropDesc(String name){
        String origComment = "origComment";
        String origDefaultValue = "origDefaultVal";
        String origExampleValue = "origExampleVal";
        String origMik = "origMik";
        String origDisplayName = "origDisplayName";
        String origValue = "origValue";
        PropertyTagEntity origTag = new PropertyTagEntity();
        origTag.setName("origTag");
        PropertyEntity origProp = propBuilder.buildPropertyEntity(origValue, null);
        PropertyTypeEntity origType = propDescBuilder.mockPropertyTypeEntity("type1");
        PropertyDescriptorEntity origin = propDescBuilder.withGeneratedId().withPropertyName(name)
                .withProperties(Collections.singleton(origProp)).withPropertyComment(origComment)
                .withDefaultValue(origDefaultValue).withMik(origMik).withExampleValue(origExampleValue)
                .withPropertyType(origType).withDisplayName(origDisplayName)
                .withTags(origTag).build();
        return  origin;
    }

    private void assertPropDescCopy(PropertyDescriptorEntity copy, PropertyDescriptorEntity origin,
                                    PropertyDescriptorEntity target, Set<PropertyEntity> existingTargetProperties) {
        assertNotNull(copy);
        // properties from origin will be added in copyPropertyEntity()
        if (target == null) {
            assertTrue( copy.getProperties().isEmpty());
        }
        else {
            assertEquals(existingTargetProperties, copy.getProperties());
        }
        assertEquals(origin.isEncrypt(), copy.isEncrypt());
        assertEquals(origin.getPropertyName(), copy.getPropertyName());
        assertEquals(origin.isNullable(), copy.isNullable());
        assertEquals(origin.isTesting(), copy.isTesting());
        assertEquals(origin.getValidationLogic(), copy.getValidationLogic());
        assertEquals(origin.getPropertyComment(), copy.getPropertyComment());
        assertEquals(origin.getCardinalityProperty(), copy.getCardinalityProperty());
        assertEquals(origin.getPropertyTypeEntity(), copy.getPropertyTypeEntity());
        assertEquals(origin.getDefaultValue(), copy.getDefaultValue());
        assertEquals(origin.getExampleValue(), copy.getExampleValue());
        assertEquals(origin.getMachineInterpretationKey(), copy.getMachineInterpretationKey());
        assertEquals(origin.isOptional(), copy.isOptional());
    }

}
