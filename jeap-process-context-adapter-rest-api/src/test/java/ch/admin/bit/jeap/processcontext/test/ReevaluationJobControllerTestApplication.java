package ch.admin.bit.jeap.processcontext.test;

import ch.admin.bit.jeap.security.resource.configuration.SemanticMethodSecurityExpressionHandler;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootConfiguration
@EnableMethodSecurity
public class ReevaluationJobControllerTestApplication {

    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler(ApplicationContext applicationContext) {
        SemanticMethodSecurityExpressionHandler handler = new SemanticMethodSecurityExpressionHandler("jme");
        handler.setApplicationContext(applicationContext);
        return handler;
    }
}
