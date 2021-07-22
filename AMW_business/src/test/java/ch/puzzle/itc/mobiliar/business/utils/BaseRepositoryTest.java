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

package ch.puzzle.itc.mobiliar.business.utils;

import ch.puzzle.itc.mobiliar.test.Entity.Color;
import ch.puzzle.itc.mobiliar.test.testrunner.PersistenceTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(PersistenceTestRunner.class)
public class BaseRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;
    private ColorRepository repository;
    private Color blue, red;

    @Before
    public void setup() throws Exception {

        repository = new ColorRepository();
        repository.setEntityManager(entityManager);

        blue = new Color("Blue");
        entityManager.persist(blue);
        red = new Color("Red");
        entityManager.persist(red);
    }

    @Test
    public void shouldFindEntity() throws Exception {
        // when
        Color result = repository.find(red.getId());

        // then
        assertThat(result, equalTo(red));
    }

    @Test
    public void shouldPersistEntity() throws Exception {
        // given
        Color black = new Color("Black");

        // when
        repository.persist(black);
        entityManager.flush();

        // then
        assertThat(black.getId(), is(notNullValue()));
        assertThat(entityManager.find(Color.class, black.getId()), equalTo(black));
    }

    @Test
    public void shouldMergeEntity() throws Exception {
        // given
        entityManager.flush();
        entityManager.detach(blue);

        // when
        blue.setName("Dark Blue");
        repository.merge(blue);
        entityManager.flush();

        // then
        String query = "select s from Color s where s.name = 'Dark Blue'";
        Color merged = entityManager.createQuery(query, Color.class).getSingleResult();
        assertThat(merged.getId(), equalTo(blue.getId()));
    }

    @Test
    public void shouldIncrementVersionValueOnMerge() throws Exception {
        // given
        entityManager.flush();
        entityManager.detach(blue);

        // when
        blue.setName("Light Blue");
        Color merged = repository.merge(blue);
        entityManager.flush();

        // then
        assertThat(merged.getVersion(), is(greaterThan(blue.getVersion())));
    }

    @Test
    public void shouldRemoveEntityByObject() throws Exception {
        // when
        repository.remove(blue);
        entityManager.flush();

        // then
        assertThat(entityManager.find(Color.class, blue.getId()), is(nullValue()));
    }

    @Test
    public void shouldRemoveEntityByIdAndReturnTrue() throws Exception {
        // when
        boolean found = repository.remove(blue.getId());
        entityManager.flush();

        // then
        assertThat(found, is(true));
        assertThat(entityManager.find(Color.class, blue.getId()), is(nullValue()));
    }

    @Test
    public void shouldReturnFalseIfEntityToRemoveWasNotFound() throws Exception {
        // when
        boolean found = repository.remove(Long.MAX_VALUE);
        entityManager.flush();

        // then
        assertThat(found, is(false));
    }

    @Test
    public void shouldReturnNullIfResultListIsEmpty() throws Exception {
        // given
        entityManager.remove(blue);
        entityManager.remove(red);

        // when
        Color color = repository.singleResult(entityManager.createQuery("select c from Color c", Color.class));

        // then
        assertThat(color, is(nullValue()));
    }

    @Test
    public void shouldFindAll() throws Exception {
        // when
        List<Color> result = repository.findAll();

        // then
        assertEquals(2, result.size());
    }

    private static class ColorRepository extends BaseRepository<Color> {
    }

}
