package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.security.test.resource.configuration.ServletJeapAuthorizationConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
@ComponentScan
class RestApiTestContext extends ServletJeapAuthorizationConfig {

    // You have to provide the system name and the application context to the test support base class.
    RestApiTestContext(ApplicationContext applicationContext) {
        super("jme", applicationContext);
    }

    @Bean
    public TranslateService labelTranslateService() {
        return mock(TranslateService.class);
    }

}


