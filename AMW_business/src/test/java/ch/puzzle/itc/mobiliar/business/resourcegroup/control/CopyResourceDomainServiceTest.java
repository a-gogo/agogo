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

package ch.puzzle.itc.mobiliar.business.resourcegroup.control;

import ch.puzzle.itc.mobiliar.builders.*;
import ch.puzzle.itc.mobiliar.business.environment.entity.ContextEntity;
import ch.puzzle.itc.mobiliar.business.foreignable.control.ForeignableService;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwnerViolationException;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyDescriptorEntity;
import ch.puzzle.itc.mobiliar.business.property.entity.PropertyEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceContextEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.ResourceRelationTypeEntity;
import ch.puzzle.itc.mobiliar.common.exception.AMWException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 */
public class CopyResourceDomainServiceTest {

    CopyResourceDomainService copyResourceDomainService;
    ForeignableService foreignableService;

    private ContextEntityBuilder contextEntityBuilder = new ContextEntityBuilder();

    private ResourceEntity originResource;
    private ResourceEntity targetResource;
    ContextEntity globalContextMock;

    @Before
    public void setUp() {

        globalContextMock = contextEntityBuilder.mockContextEntity("GLOBAL", null, null);

        originResource = new ResourceEntityBuilder().buildApplicationEntity("appOriginResource", null, null, true);
        targetResource = new ResourceEntityBuilder().buildApplicationEntity("appTargetResource", null, null, true);

        copyResourceDomainService = new CopyResourceDomainService();
        foreignableService = mock(ForeignableService.class);
        copyResourceDomainService.foreignableService = foreignableService;
    }

    @Test
    public void copyResourceContexts_shouldCopyOnly_AmwOwned_PropertyDescriptors_In_Maia_PredecessorMode() throws ForeignableOwnerViolationException, AMWException {
        // given
        PropertyDescriptorEntity amwPropertyDesc = new PropertyDescriptorEntityBuilder().withId(1).withPropertyName("propertyName").build();
        PropertyDescriptorEntity maiaPropertyDesc = new PropertyDescriptorEntityBuilder().withId(2).withPropertyName("propertyNameMaia").withOwner(ForeignableOwner.MAIA).build();

        addPropertyDescriptor(originResource, amwPropertyDesc);
        addPropertyDescriptor(originResource, maiaPropertyDesc);

        CopyUnit copyUnit = new CopyUnit(originResource, targetResource, CopyResourceDomainService.CopyMode.MAIA_PREDECESSOR, ForeignableOwner.MAIA);

        // when
        copyResourceDomainService.copyResourceContexts(copyUnit);

        // then
        assertEquals(1, targetResource.getContexts().iterator().next().getPropertyDescriptors().size());
        assertEquals("propertyName", targetResource.getContexts().iterator().next().getPropertyDescriptors().iterator().next().getPropertyName());
    }

    @Test
    public void copyResourceContexts_shouldCopy_Amw_PropertyValues() throws ForeignableOwnerViolationException, AMWException {
        // given
        PropertyDescriptorEntity amwPropertyDesc = new PropertyDescriptorEntityBuilder().withId(1).withPropertyName("propertyName").build();

        addPropertyDescriptor(originResource, amwPropertyDesc);
        addPropertyValue(originResource, amwPropertyDesc, "propertyValue");

        CopyUnit copyUnit = new CopyUnit(originResource, targetResource, CopyResourceDomainService.CopyMode.MAIA_PREDECESSOR, ForeignableOwner.MAIA);

        // when
        copyResourceDomainService.copyResourceContexts(copyUnit);

        // then
        assertEquals(1, targetResource.getContexts().iterator().next().getPropertyDescriptors().size());
        assertEquals("propertyName", targetResource.getContexts().iterator().next().getPropertyDescriptors().iterator().next().getPropertyName());
        assertEquals(1, targetResource.getContexts().iterator().next().getProperties().size());
        assertEquals("propertyValue", targetResource.getContexts().iterator().next().getProperties().iterator().next().getValue());
    }

    @Test
    public void copyResourceContexts_shouldCopy_Amw_PropertyValuesOnMaiaProperties() throws ForeignableOwnerViolationException, AMWException {
        // given
        PropertyDescriptorEntity maiaPropertyDesc = new PropertyDescriptorEntityBuilder().withId(1).withPropertyName("propertyNameMaia").withOwner(ForeignableOwner.MAIA).build();

        addPropertyDescriptor(originResource, maiaPropertyDesc);
        addPropertyValue(originResource, maiaPropertyDesc, "propertyValueMaia");

        // Property with same name, was added on the target Resource by Maia
        PropertyDescriptorEntity maiaPropertyDescTarget = new PropertyDescriptorEntityBuilder().withId(2).withPropertyName("propertyNameMaia").withOwner(ForeignableOwner.MAIA).build();
        addPropertyDescriptor(targetResource, maiaPropertyDescTarget);

        CopyUnit copyUnit = new CopyUnit(originResource, targetResource, CopyResourceDomainService.CopyMode.MAIA_PREDECESSOR, ForeignableOwner.MAIA);

        // when
        copyResourceDomainService.copyResourceContexts(copyUnit);

        // then
        assertEquals(1, targetResource.getContexts().iterator().next().getPropertyDescriptors().size());
        assertEquals("propertyNameMaia", targetResource.getContexts().iterator().next().getPropertyDescriptors().iterator().next().getPropertyName());
        assertEquals(1, targetResource.getContexts().iterator().next().getProperties().size());
        assertEquals("propertyValueMaia", targetResource.getContexts().iterator().next().getProperties().iterator().next().getValue());
    }

    @Test
    public void copyConsumedMasterRelations_shouldOnlyCopyAmwOwnedReleations() throws ForeignableOwnerViolationException, AMWException {
        // given

        ResourceEntity slave = new ResourceEntityBuilder().buildApplicationEntity("database", null, null, true);
        slave.setOwner(ForeignableOwner.getSystemOwner());

        ResourceEntity slave2 = new ResourceEntityBuilder().buildApplicationEntity("cpi", null, null, true);
        slave2.setOwner(ForeignableOwner.MAIA);

        originResource.addConsumedResourceRelation(slave, mock(ResourceRelationTypeEntity.class), null, ForeignableOwner.getSystemOwner());
        originResource.addConsumedResourceRelation(slave2, mock(ResourceRelationTypeEntity.class), null, ForeignableOwner.MAIA);


        CopyUnit copyUnit = new CopyUnit(originResource, targetResource, CopyResourceDomainService.CopyMode.MAIA_PREDECESSOR, ForeignableOwner.MAIA);

        // when
        copyResourceDomainService.copyConsumedMasterRelations(copyUnit);
        // then
        assertEquals(1, targetResource.getConsumedMasterRelations().size());
        assertEquals("database", targetResource.getConsumedMasterRelations().iterator().next().getSlaveResource().getName());

    }

    @Test
    public void copyProvidedMasterRelations_shouldOnlyCopyAmwOwnedReleations() throws ForeignableOwnerViolationException, AMWException {
        // given

        ResourceEntity provider = new ResourceEntityBuilder().buildApplicationEntity("amwws", null, null, true);
        provider.setOwner(ForeignableOwner.getSystemOwner());
        provider.setId(Integer.valueOf(1));

        ResourceEntity provider2 = new ResourceEntityBuilder().buildApplicationEntity("ppi", null, null, true);
        provider2.setOwner(ForeignableOwner.MAIA);
        provider2.setId(Integer.valueOf(2));

        originResource.addProvidedResourceRelation(provider, mock(ResourceRelationTypeEntity.class), ForeignableOwner.getSystemOwner());
        originResource.addProvidedResourceRelation(provider2, mock(ResourceRelationTypeEntity.class), ForeignableOwner.MAIA);

        CopyUnit copyUnit = new CopyUnit(originResource, targetResource, CopyResourceDomainService.CopyMode.MAIA_PREDECESSOR, ForeignableOwner.MAIA);

        // when
        copyResourceDomainService.copyProvidedMasterRelations(copyUnit);
        // then
        assertEquals(1, targetResource.getProvidedMasterRelations().size());
        assertEquals("amwws", targetResource.getProvidedMasterRelations().iterator().next().getSlaveResource().getName());
    }

    // TODO add also values on relations (AMW owned)

    private void addPropertyDescriptor(ResourceEntity resource, PropertyDescriptorEntity propertyDescriptor) {
        ResourceContextEntity context = resource.getOrCreateContext(globalContextMock);
        context.addPropertyDescriptor(propertyDescriptor);
    }

    private void addPropertyValue(ResourceEntity resource, PropertyDescriptorEntity propertyDescriptor, String value) {
        ResourceContextEntity context = resource.getOrCreateContext(globalContextMock);
        PropertyEntity propertyEntity = new PropertyEntityBuilder().buildPropertyEntity(value, propertyDescriptor);
        context.addProperty(propertyEntity);
    }
}
