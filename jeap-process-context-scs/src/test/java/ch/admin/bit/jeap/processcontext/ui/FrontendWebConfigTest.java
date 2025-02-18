package ch.admin.bit.jeap.processcontext.ui;

import ch.admin.bit.jeap.processcontext.ui.configuration.FrontendConfigProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendWebConfigTest {

    @Test
    void getOrigin_local() {
        FrontendConfigProperties props = new FrontendConfigProperties();
        props.setApplicationUrl(URI.create("http://localhost:4200"));
        FrontendWebConfig config = new FrontendWebConfig(props);

        assertThat(config.getOrigin())
                .isEqualTo("http://localhost:4200");
    }

    @Test
    void getOrigin_withContext() {
        FrontendConfigProperties props = new FrontendConfigProperties();
        props.setApplicationUrl(URI.create("https://some-host/error-handling/"));
        FrontendWebConfig config = new FrontendWebConfig(props);

        assertThat(config.getOrigin())
                .isEqualTo("https://some-host");
    }
}
