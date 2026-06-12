package ch.admin.bit.jeap.processcontext.ui;

import ch.admin.bit.jeap.processcontext.ProcessInstanceMockS3ITBase;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Verifies that a request to the application's context root is served the SPA entry point (index.html).
 * <p>
 * Spring Boot maps the welcome page "/" to a {@code forward:index.html}. Combined with the
 * jeap-spring-boot-web-config-starter's response-buffering ShallowEtag filter, that forward returned an empty
 * body under Spring Boot 4 / Spring Framework 7 (the forward commits the response before the filter copies the
 * buffered body). The starter now disables ETag content-caching for forward dispatches, so the body is served.
 * <p>
 * This must run against a real servlet container ({@code RANDOM_PORT}) so that the welcome-page forward and the
 * full filter chain actually execute; MockMvc only records the forward URL and would not detect the empty body.
 */
@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/metrics.json")
class WelcomePageIndexHtmlIT extends ProcessInstanceMockS3ITBase {

    @LocalServerPort
    private int localServerPort;

    @Test
    void contextRoot_servesIndexHtmlContent() throws Exception {
        String expectedIndexHtml = new ClassPathResource("static/index.html")
                .getContentAsString(StandardCharsets.UTF_8);

        String body = RestAssured.given()
                .port(localServerPort)
                .when()
                .get("/process-context/")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .extract().asString();

        assertThat(body).isEqualTo(expectedIndexHtml);
    }
}
