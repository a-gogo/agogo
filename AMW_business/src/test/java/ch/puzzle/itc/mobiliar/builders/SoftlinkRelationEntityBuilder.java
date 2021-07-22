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

package ch.puzzle.itc.mobiliar.builders;

import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;
import ch.puzzle.itc.mobiliar.business.softlinkRelation.entity.SoftlinkRelationEntity;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

/**
 * Builder for tests using {@link ch.puzzle.itc.mobiliar.business.softlinkRelation.entity.SoftlinkRelationEntity}
 */
public class SoftlinkRelationEntityBuilder {

    private Integer id;
    private ForeignableOwner owner;
    private String softlinkRef;
    private ResourceEntity cpiResource;

    public SoftlinkRelationEntityBuilder withId(Integer id){
        this.id = id;
        return this;
    }

    public SoftlinkRelationEntityBuilder withOwner(ForeignableOwner owner){
        this.owner = owner;
        return this;
    }

    public SoftlinkRelationEntityBuilder withSoftlinkRef(String softlinkRef){
        this.softlinkRef = softlinkRef;
        return this;
    }

    public SoftlinkRelationEntityBuilder withCpiResource(ResourceEntity cpiResource){
        this.cpiResource = cpiResource;
        return this;
    }

    public SoftlinkRelationEntity build(){
        SoftlinkRelationEntity softlinkEntity = new SoftlinkRelationEntity();

        softlinkEntity.setId(id);
        softlinkEntity.setOwner(owner);
        softlinkEntity.setSoftlinkRef(softlinkRef);
        softlinkEntity.setCpiResource(cpiResource);

        return softlinkEntity;
    }

    public SoftlinkRelationEntity mock(){
        SoftlinkRelationEntity softlinkEntity = Mockito.mock(SoftlinkRelationEntity.class);

        when(softlinkEntity.getId()).thenReturn(id);
        when(softlinkEntity.getOwner()).thenReturn(owner);
        when(softlinkEntity.getSoftlinkRef()).thenReturn(softlinkRef);
        when(softlinkEntity.getCpiResource()).thenReturn(cpiResource);

        return softlinkEntity;
    }
}
