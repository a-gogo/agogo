package ch.mobi.itc.mobiliar.rest.resources;

import ch.puzzle.itc.mobiliar.business.resourcegroup.boundary.ResourceLocator;
import ch.puzzle.itc.mobiliar.common.exception.ValidationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResourcePropertiesRestTest {

    @InjectMocks
    ResourcePropertiesRest rest;

    @Mock
    ResourceLocator resourceLocator;

    @Before
    public void configure() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void shouldReturnNotFoundStatusIfNoPropertiesHaveBeenFound() throws ValidationException {
        // given
        int notFound = Response.Status.NOT_FOUND.getStatusCode();

        // when
        Response response = rest.getResourceProperties("Test");

        // then
        assertThat(response.getStatus(), is(notFound));
    }

}
