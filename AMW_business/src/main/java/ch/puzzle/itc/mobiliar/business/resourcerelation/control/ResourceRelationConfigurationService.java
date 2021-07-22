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

package ch.puzzle.itc.mobiliar.business.resourcerelation.control;

import java.util.ArrayList;
import java.util.List;

import ch.puzzle.itc.mobiliar.business.foreignable.entity.ForeignableOwner;
import ch.puzzle.itc.mobiliar.business.resourcerelation.entity.AbstractResourceRelationEntity;

public class ResourceRelationConfigurationService {

    private static List<List<ForeignableOwner>> suspectRelations = new ArrayList<>(1);

    public ResourceRelationConfigurationService() {

    // a owner combination should always consist of three owners: source, relation and target owner
    List<ForeignableOwner> suspectCombination = new ArrayList<>(3);
    suspectCombination.add(ForeignableOwner.MAIA);
    suspectCombination.add(ForeignableOwner.AMW);
    suspectCombination.add(ForeignableOwner.MAIA);
    suspectRelations.add(suspectCombination);

    }

    /**
     * Returns true if a ResourceRelation is questionable
     */
    public boolean isSuspectRelation(AbstractResourceRelationEntity resourceRelation) {
        return isSuspectOwnerCombination(resourceRelation.getMasterResource().getOwner(), resourceRelation.getOwner(), resourceRelation.getSlaveResource().getOwner());
    }

    /**
     * Returns true if a ResourceRelation is reasonable
     */
    public boolean isPlausibleRelation(AbstractResourceRelationEntity resourceRelation) {
        return !isSuspectRelation(resourceRelation);
    }

    /**
     * Returns true if a ownership combination is questionable
     */
    public boolean isSuspectOwnerCombination(ForeignableOwner sourceOwner, ForeignableOwner relationOwner, ForeignableOwner targetOwner) {

        List<ForeignableOwner> currentCombination = new ArrayList<>(3);
        currentCombination.add(sourceOwner);
        currentCombination.add(relationOwner);
        currentCombination.add(targetOwner);

        for (List<ForeignableOwner> suspectRelation : suspectRelations) {
            if (currentCombination.equals(suspectRelation)) {
                return true;
            }
        }
        return false;

    }

    /**
     * Returns true if a ownership combination is reasonable
     */
    public boolean isPlausibleOwnerCombination(ForeignableOwner sourceOwner, ForeignableOwner relationOwner, ForeignableOwner targetOwner) {
        return !isSuspectOwnerCombination(sourceOwner, relationOwner, targetOwner);
    }

}
