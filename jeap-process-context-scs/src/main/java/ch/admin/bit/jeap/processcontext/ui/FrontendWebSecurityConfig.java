package ch.admin.bit.jeap.processcontext.ui;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
class FrontendWebSecurityConfig {

    @Bean
    @Order(100)
        // same as on the deprecated WebSecurityConfigurerAdapter
    SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) throws Exception {
        // protect the API
        // allow public access to frontend resources (i.e. non-/api-routes)
        // permit open access to open API docs & swagger ui as they are only enabled on test environments
        RequestMatcher antPathMatcher = new AntPathRequestMatcher("/api/**");
        RequestMatcher matcher = new NegatedRequestMatcher(antPathMatcher);
        http.securityMatcher(matcher)
                .authorizeHttpRequests(req -> req.anyRequest().permitAll());

        // this is used for the auth - silent-refresh.html
        http.headers(headers ->
                headers.frameOptions(FrameOptionsConfig::sameOrigin));

        return http.build();
    }
}
