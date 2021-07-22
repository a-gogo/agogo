package ch.puzzle.itc.mobiliar.business.applicationinfo.boundary;

import ch.puzzle.itc.mobiliar.business.applicationinfo.entity.ConfigurationKeyValuePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApplicationVersionServiceTest {

    @InjectMocks
    ApplicationVersionService applicationVersionService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getObfuscatedApplicationConfigurationKeyValuePairsShouldObfuscateValuesCorrectly() {
        // given
        applicationVersionService.init();

        // when
        List<ConfigurationKeyValuePair> obfuscatedKeyValuePairs = applicationVersionService.getObfuscatedApplicationConfigurationKeyValuePairs();

        // then
        for (ConfigurationKeyValuePair keyValuePair : obfuscatedKeyValuePairs) {
            if (keyValuePair.getKey().isSecretValue()) {
                assertThat(keyValuePair.getValue(), is(ApplicationVersionService.OBFUSCATED));
                assertThat(keyValuePair.getDefaultValue(), is(ApplicationVersionService.OBFUSCATED));
            } else {
                assertThat(keyValuePair.getValue(), not(ApplicationVersionService.OBFUSCATED));
                assertThat(keyValuePair.getDefaultValue(), not(ApplicationVersionService.OBFUSCATED));
            }
        }

    }
}