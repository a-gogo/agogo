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

import static org.junit.Assert.*;

import java.util.*;

import ch.puzzle.itc.mobiliar.builders.ReleaseEntityBuilder;
import ch.puzzle.itc.mobiliar.common.util.ConfigKey;
import org.junit.Before;
import org.junit.Test;

import ch.puzzle.itc.mobiliar.builders.ResourceEntityBuilder;
import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.business.resourcegroup.entity.ResourceEntity;

public class ResourceImportServiceTest {

    private static final ReleaseEntity MAIN_RELEASE_A = ReleaseEntityBuilder.createMainReleaseEntity("Alpha", 10, new Date(10_000_000));
    private static final ReleaseEntity MINOR_RELEASE_A_1 = ReleaseEntityBuilder.createMinorReleaseEntity("MinorAlpha1", 11, new Date(11_000_000));
    private static final ReleaseEntity MAIN_RELEASE_B = ReleaseEntityBuilder.createMainReleaseEntity("Beta", 20, new Date(20_000_000));
    private static final ReleaseEntity MINOR_RELEASE_B_1 = ReleaseEntityBuilder.createMinorReleaseEntity("MinorBeta1", 21, new Date(21_000_000));
    private static final ReleaseEntity MINOR_RELEASE_B_2 = ReleaseEntityBuilder.createMinorReleaseEntity("MinorBeta2", 22, new Date(22_000_000));
    private static final ReleaseEntity MINOR_RELEASE_B_3 = ReleaseEntityBuilder.createMinorReleaseEntity("MinorBeta3", 23, new Date(23_000_000));
    private static final ReleaseEntity MAIN_RELEASE_C = ReleaseEntityBuilder.createMainReleaseEntity("Gamma", 30, new Date(30_000_000));

    private static final String APP_GROUP_NAME = "appname";
    private static final ResourceEntity RESOURCE_MAIN_RELEASE_A = new ResourceEntityBuilder().withName(APP_GROUP_NAME).withRelease(MAIN_RELEASE_A).withId(Integer.valueOf(1)).build();
    private static final ResourceEntity RESOURCE_MINOR_RELEASE_A_1 = new ResourceEntityBuilder().withName(APP_GROUP_NAME).withRelease(MINOR_RELEASE_A_1).withId(Integer.valueOf(2)).build();
    private static final ResourceEntity RESOURCE_MAIN_RELEASE_B = new ResourceEntityBuilder().withName(APP_GROUP_NAME).withRelease(MAIN_RELEASE_B).withId(Integer.valueOf(3)).build();
    private static final ResourceEntity RESOURCE_MINOR_RELEASE_B_1 = new ResourceEntityBuilder().withName(APP_GROUP_NAME).withRelease(MINOR_RELEASE_B_1).withId(Integer.valueOf(4)).build();
    private static final ResourceEntity RESOURCE_MINOR_RELEASE_B_2 = new ResourceEntityBuilder().withName(APP_GROUP_NAME).withRelease(MINOR_RELEASE_B_2).withId(Integer.valueOf(5)).build();
    private static final ResourceEntity RESOURCE_MINOR_RELEASE_B_3 = new ResourceEntityBuilder().withName(APP_GROUP_NAME).withRelease(MINOR_RELEASE_B_3).withId(Integer.valueOf(6)).build();
    private static final ResourceEntity RESOURCE_MAIN_RELEASE_C = new ResourceEntityBuilder().withName(APP_GROUP_NAME).withRelease(MAIN_RELEASE_C).withId(Integer.valueOf(7)).build();

    private ResourceImportService resourceImportService;

    private Set<ResourceEntity> resourcesInGroupNotOrderedByRelease = new HashSet<>(Arrays.asList(RESOURCE_MINOR_RELEASE_A_1, RESOURCE_MAIN_RELEASE_C, RESOURCE_MAIN_RELEASE_A, RESOURCE_MAIN_RELEASE_B, RESOURCE_MINOR_RELEASE_B_1, RESOURCE_MINOR_RELEASE_B_2, RESOURCE_MINOR_RELEASE_B_3));

    @Before
    public void setUp(){
        resourceImportService = new ResourceImportService();
        resourceImportService.resourceReleaseComparator = new ResourceReleaseComparator();
    }


    @Test
    public void getAllFollowingMinorReleaseForResourceInReleaseShouldReturnAllMinorReleases(){

        // when
        List<ResourceEntity> allFollowingMinorReleaseForResourceNameInRelease = resourceImportService.getAllMinorReleasesFollowingRelease(resourcesInGroupNotOrderedByRelease, MAIN_RELEASE_B);

        // then
        verifyReleaseResourcesInResult(allFollowingMinorReleaseForResourceNameInRelease, RESOURCE_MINOR_RELEASE_B_1, RESOURCE_MINOR_RELEASE_B_2, RESOURCE_MINOR_RELEASE_B_3);
        verifyReleaseResourcesNotInResult(allFollowingMinorReleaseForResourceNameInRelease, RESOURCE_MAIN_RELEASE_A, RESOURCE_MAIN_RELEASE_B, RESOURCE_MAIN_RELEASE_C, RESOURCE_MINOR_RELEASE_A_1);
    }

    @Test
    public void getAllFollowingMinorReleaseForResourceInMinorReleaseShouldReturnAllMinorReleases(){
        List<ResourceEntity> allFollowingMinorReleaseForResourceNameInRelease = resourceImportService.getAllMinorReleasesFollowingRelease(resourcesInGroupNotOrderedByRelease, MINOR_RELEASE_B_2);

        // then
        verifyReleaseResourcesInResult(allFollowingMinorReleaseForResourceNameInRelease, RESOURCE_MINOR_RELEASE_B_3);
        verifyReleaseResourcesNotInResult(allFollowingMinorReleaseForResourceNameInRelease, RESOURCE_MAIN_RELEASE_A, RESOURCE_MAIN_RELEASE_B, RESOURCE_MINOR_RELEASE_B_1, RESOURCE_MINOR_RELEASE_B_2, RESOURCE_MAIN_RELEASE_C, RESOURCE_MINOR_RELEASE_A_1);
    }

    @Test
    public void getAllFollowingMinorReleaseForLastResourceInMinorReleaseShouldReturnEmptyList(){
        List<ResourceEntity> allFollowingMinorReleaseForResourceNameInRelease = resourceImportService.getAllMinorReleasesFollowingRelease(resourcesInGroupNotOrderedByRelease, MINOR_RELEASE_B_3);

        // then
        assertTrue(allFollowingMinorReleaseForResourceNameInRelease.isEmpty());
    }

    @Test
    public void getPreviousReleaseShouldReturnNull(){
        ResourceEntity result = resourceImportService.getPreviousRelease(resourcesInGroupNotOrderedByRelease, null);

        // then
        assertNull(result);
    }

    @Test
    public void getPreviousReleaseShouldReturnB3(){
        ResourceEntity result = resourceImportService.getPreviousRelease(resourcesInGroupNotOrderedByRelease, RESOURCE_MAIN_RELEASE_C);

        // then
        assertNotNull(result);
        assertEquals(RESOURCE_MINOR_RELEASE_B_3, result);
    }

    @Test
    public void getPreviousReleaseShouldReturnNullNoPreviousRelease(){
        ResourceEntity result = resourceImportService.getPreviousRelease(resourcesInGroupNotOrderedByRelease, RESOURCE_MAIN_RELEASE_A);

        // then
        assertNull(result);
    }

    @Test
    public void getPreviousReleaseShouldReturnPreviousMainReleaseIfNoMinorReleaseIsAvailable(){

        resourcesInGroupNotOrderedByRelease = new HashSet<>(Arrays.asList(RESOURCE_MAIN_RELEASE_C, RESOURCE_MAIN_RELEASE_A, RESOURCE_MAIN_RELEASE_B, RESOURCE_MINOR_RELEASE_B_1, RESOURCE_MINOR_RELEASE_B_2, RESOURCE_MINOR_RELEASE_B_3));

        ResourceEntity result = resourceImportService.getPreviousRelease(resourcesInGroupNotOrderedByRelease, RESOURCE_MAIN_RELEASE_B);

        // then
        assertNotNull(result);
        assertEquals(RESOURCE_MAIN_RELEASE_A, result);
    }

    @Test
    public void getPreviousReleaseShouldReturnNullIfResourceIsNotInListOfResources(){

        resourcesInGroupNotOrderedByRelease = new HashSet<>(Arrays.asList(RESOURCE_MAIN_RELEASE_C, RESOURCE_MAIN_RELEASE_B, RESOURCE_MINOR_RELEASE_B_1, RESOURCE_MINOR_RELEASE_B_2, RESOURCE_MINOR_RELEASE_B_3));

        ResourceEntity result = resourceImportService.getPreviousRelease(resourcesInGroupNotOrderedByRelease, RESOURCE_MAIN_RELEASE_A);

        // then
        assertNull(result);
    }

    @Test
    public void shouldReturnCorrectBackLinkURL(){
        // when
        String backlinkUrl = resourceImportService.getImportedResourceBacklink();

        // then
        assertEquals("http://localhost:8080/AMW_web/pages/editResourceView.xhtml?ctx=1&id=", backlinkUrl);
    }
    @Test
    public void shouldReturnCorrectBackLinkURL_Configured(){
        // when

        System.getProperties().setProperty(ConfigKey.EXTERNAL_RESOURCE_BACKLINK_SCHEMA.getValue(), "https");
        System.getProperties().setProperty(ConfigKey.EXTERNAL_RESOURCE_BACKLINK_HOST.getValue(), "hostwithoutPort.ch");

        String backlinkUrl = resourceImportService.getImportedResourceBacklink();

        // then
        assertEquals("https://hostwithoutPort.ch/AMW_web/pages/editResourceView.xhtml?ctx=1&id=", backlinkUrl);

        // remove System Properties to avoid Sideeffects
        System.getProperties().remove(ConfigKey.EXTERNAL_RESOURCE_BACKLINK_SCHEMA.getValue());
        System.getProperties().remove(ConfigKey.EXTERNAL_RESOURCE_BACKLINK_HOST.getValue());
    }

    private void verifyReleaseResourcesInResult(List<ResourceEntity> result, ResourceEntity ... containedResourcesToVerify){
        for ( ResourceEntity resourceToVerify : containedResourcesToVerify) {
            assertTrue(result.contains(resourceToVerify));
        }
    }

    private void verifyReleaseResourcesNotInResult(List<ResourceEntity> result, ResourceEntity ... containedRsourcesToVerify){
        for ( ResourceEntity resourceToVerify : containedRsourcesToVerify) {
            assertFalse(result.contains(resourceToVerify));
        }
    }

}
