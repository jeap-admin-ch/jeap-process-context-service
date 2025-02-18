package ch.admin.bit.jeap.processcontext.ui;

import ch.admin.bit.jeap.processcontext.ui.configuration.FrontendConfigProperties;
import ch.admin.bit.jeap.starter.application.web.FrontendRouteRedirectExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
class FrontendWebConfig implements WebMvcConfigurer {

    private final FrontendConfigProperties frontendConfigProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedHeaders("*")
                .allowedMethods("*")
                .allowedOrigins(getOrigin())
                .allowCredentials(false);
    }

    String getOrigin() {
        URI applicationURL = frontendConfigProperties.getApplicationUrl();
        if (applicationURL == null) {
            return "http://localhost:4200";
        }

        UriComponents uriComponents = UriComponentsBuilder
                .fromUri(applicationURL).build();
        String origin = "%s://%s".formatted(uriComponents.getScheme(), uriComponents.getHost());
        if (uriComponents.getPort() != -1) {
            origin += ":" + uriComponents.getPort();
        }
        return origin;
    }

    @Bean
    public FrontendRouteRedirectExceptionHandler frontendRouteRedirectExceptionHandler() {
        return new FrontendRouteRedirectExceptionHandler();
    }
}
