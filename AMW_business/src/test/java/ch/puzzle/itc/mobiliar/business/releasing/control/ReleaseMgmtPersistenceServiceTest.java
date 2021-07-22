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

package ch.puzzle.itc.mobiliar.business.releasing.control;

import ch.puzzle.itc.mobiliar.business.releasing.entity.ReleaseEntity;
import ch.puzzle.itc.mobiliar.test.testrunner.PersistenceTestRunner;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Persistence tests for {@link ch.puzzle.itc.mobiliar.business.releasing.control.ReleaseMgmtPersistenceService}
 */
@RunWith(PersistenceTestRunner.class)
public class ReleaseMgmtPersistenceServiceTest {

	@Spy
	@PersistenceContext
	EntityManager entityManager;

	@Mock
	Logger log;

	@InjectMocks
	ReleaseMgmtPersistenceService service;
	private ReleaseEntity releaseEntity;

	@Before
	public void before() {
		MockitoAnnotations.openMocks(this);
		releaseEntity = new ReleaseEntity();
	}

	@Test
	public void test_getById() {
		// given
		releaseEntity.setId(1);
		service.saveReleaseEntity(releaseEntity);

		// when
		ReleaseEntity release = service.getById(1);

		// then
		assertNotNull(release);
	}

	@Test
	public void test_findByName() {
		// given
		releaseEntity.setName("RL-13.04");
		service.saveReleaseEntity(releaseEntity);

		// when
		ReleaseEntity release = service.findByName("RL-13.04");

		// then
		assertNotNull(release);
		assertEquals("RL-13.04", release.getName());
	}

	@Test
	public void test_findByEmptyName() {
		// given
		releaseEntity.setName("RL-13.04");
		service.saveReleaseEntity(releaseEntity);

		// when
		ReleaseEntity release1 = service.findByName("");
		ReleaseEntity release2 = service.findByName(null);

		// then
		assertNull(release1);
		assertNull(release2);
	}

	@Test
	public void test_loadAllReleaseEntities() {
		// given
		service.saveReleaseEntity(releaseEntity);
		service.saveReleaseEntity(new ReleaseEntity());
		service.saveReleaseEntity(new ReleaseEntity());

		// when
		List<ReleaseEntity> releases = service.loadReleaseEntities(0, 10, false);
		List<ReleaseEntity> releasesSubSet = service.loadReleaseEntities(0, 2, false);

		// then
		assertEquals(3, releases.size());
		assertEquals(2, releasesSubSet.size());
	}

	@Test
	public void test_deleteReleaseEntity() {
		// given
		service.saveReleaseEntity(releaseEntity);
		service.saveReleaseEntity(new ReleaseEntity());
		service.saveReleaseEntity(new ReleaseEntity());

		assertEquals(3, service.loadReleaseEntities(0, 10, false).size());

		// when
		service.deleteReleaseEntity(2);

		// then
		assertNotNull(service.getById(1));
		assertNotNull(service.getById(3));
		assertNull(service.getById(2));
	}

	@Test
	public void test_count() {
		// given
		assertEquals(0, service.count());
		service.saveReleaseEntity(releaseEntity);
		service.saveReleaseEntity(new ReleaseEntity());
		service.saveReleaseEntity(new ReleaseEntity());

		assertEquals(3, service.loadReleaseEntities(0, 10, false).size());

		// when
		int count = service.count();

		// then
		assertEquals(3, count);
	}
	
	@Test
	public void test_getDefaultRelease() {
		releaseEntity.setInstallationInProductionAt(DateUtils.addDays(new Date(), -2));
		ReleaseEntity secondEntity = new ReleaseEntity();
		secondEntity.setInstallationInProductionAt(DateUtils.addDays(new Date(), 2));
		
		
		service.saveReleaseEntity(secondEntity);
		service.saveReleaseEntity(releaseEntity);
		
		// when
		ReleaseEntity release = service.getDefaultRelease();

		// then
		assertEquals(releaseEntity, release);
	}


}
