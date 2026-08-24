package ch.admin.bit.jeap.processcontext.adapter.restapi.config;

import ch.admin.bit.jeap.processcontext.adapter.restapi.BackfillJobController;
import ch.admin.bit.jeap.processcontext.adapter.restapi.ReevaluationJobController;
import ch.admin.bit.jeap.processcontext.domain.maintenance.BackfillJobService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceProperties;
import ch.admin.bit.jeap.processcontext.domain.maintenance.ReevaluationJobService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MaintenanceRestApiConfigTest {

    private static final MediaType APPLICATION_X_YAML = MediaType.parseMediaType("application/x-yaml");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    ConfigurationPropertiesAutoConfiguration.class,
                    HttpMessageConvertersAutoConfiguration.class,
                    RestClientAutoConfiguration.class,
                    MaintenanceRestApiConfig.class))
            .withUserConfiguration(MaintenanceProperties.class);

    @Test
    void disabled_doesNotRegisterMaintenanceYamlCustomizer() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean("maintenanceYamlConverterCustomizer"));
    }

    @Test
    void disabled_doesNotRegisterReevaluationJobController() {
        new ApplicationContextRunner()
                .withUserConfiguration(ReevaluationJobController.class)
                .withBean(ReevaluationJobService.class, () -> mock(ReevaluationJobService.class))
                .run(context -> assertThat(context).doesNotHaveBean(ReevaluationJobController.class));
    }

    @Test
    void enabled_registersReevaluationJobController() {
        new ApplicationContextRunner()
                .withUserConfiguration(ReevaluationJobController.class)
                .withBean(ReevaluationJobService.class, () -> mock(ReevaluationJobService.class))
                .withPropertyValues("jeap.processcontext.maintenance.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ReevaluationJobController.class));
    }

    @Test
    void disabled_doesNotRegisterBackfillJobController() {
        new ApplicationContextRunner()
                .withUserConfiguration(BackfillJobController.class)
                .withBean(BackfillJobService.class, () -> mock(BackfillJobService.class))
                .run(context -> assertThat(context).doesNotHaveBean(BackfillJobController.class));
    }

    @Test
    void enabled_registersBackfillJobController() {
        new ApplicationContextRunner()
                .withUserConfiguration(BackfillJobController.class)
                .withBean(BackfillJobService.class, () -> mock(BackfillJobService.class))
                .withPropertyValues("jeap.processcontext.maintenance.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(BackfillJobController.class));
    }

    @Test
    void restClientFromSharedBuilder_bodyWithoutContentType_isSentAsJson() {
        contextRunner.withPropertyValues("jeap.processcontext.maintenance.enabled=true").run(context -> {
            RestClient.Builder builder = context.getBean(RestClient.Builder.class);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("/api/dbschemas"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(content().string("{\"name\":\"pcs\",\"version\":1}"))
                    .andRespond(withSuccess());

            builder.build().post().uri("/api/dbschemas")
                    .body(new DbSchemaDto("pcs", 1))
                    .retrieve().toBodilessEntity();

            server.verify();
        });
    }

    @Test
    void enabled_serverConvertersContainMaintenanceYamlBehindJson() {
        contextRunner.withPropertyValues("jeap.processcontext.maintenance.enabled=true").run(context -> {
            assertThat(context).doesNotHaveBean(HttpMessageConverter.class);
            HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer().registerDefaults();
            context.getBeanProvider(ServerHttpMessageConvertersCustomizer.class).orderedStream()
                    .forEach(customizer -> customizer.customize(builder));
            List<HttpMessageConverter<?>> converters = new ArrayList<>();
            builder.build().forEach(converters::add);

            int jsonIndex = indexOfConverterSupporting(converters, MediaType.APPLICATION_JSON);
            int yamlIndex = indexOfConverterSupporting(converters, APPLICATION_X_YAML);
            assertThat(jsonIndex).isNotNegative();
            assertThat(yamlIndex).isGreaterThan(jsonIndex);
            assertThat(converters.get(yamlIndex).getSupportedMediaTypes())
                    .containsExactly(MediaType.APPLICATION_YAML, APPLICATION_X_YAML);
        });
    }

    @Test
    void enabled_configuredRequestLimitRejectsOversizedYamlDocument() {
        contextRunner.withPropertyValues(
                "jeap.processcontext.maintenance.enabled=true",
                "jeap.processcontext.maintenance.limits.max-request-bytes=128").run(context -> {
            HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer().registerDefaults();
            context.getBeanProvider(ServerHttpMessageConvertersCustomizer.class).orderedStream()
                    .forEach(customizer -> customizer.customize(builder));
            List<HttpMessageConverter<?>> converters = new ArrayList<>();
            builder.build().forEach(converters::add);
            @SuppressWarnings("unchecked")
            HttpMessageConverter<Object> yamlConverter = (HttpMessageConverter<Object>) converters.stream()
                    .filter(converter -> converter.getSupportedMediaTypes().contains(APPLICATION_X_YAML))
                    .findFirst()
                    .orElseThrow();
            MockHttpInputMessage input = new MockHttpInputMessage(("process-template-name: "
                    + "x".repeat(200) + "\nentries: []\n").getBytes());
            input.getHeaders().setContentType(APPLICATION_X_YAML);

            assertThatThrownBy(() -> yamlConverter.read(Object.class, input))
                    .isInstanceOf(HttpMessageNotReadableException.class);
        });
    }

    private static int indexOfConverterSupporting(List<HttpMessageConverter<?>> converters, MediaType mediaType) {
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i).getSupportedMediaTypes().contains(mediaType)) {
                return i;
            }
        }
        return -1;
    }

    record DbSchemaDto(String name, int version) {
    }
}
