package ch.admin.bit.jeap.processcontext.adapter.restapi.config;

import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import java.util.List;

public class MaintenanceYamlConverterCustomizer implements ServerHttpMessageConvertersCustomizer {

    @Override
    public void customize(HttpMessageConverters.ServerBuilder builder) {
        builder.withYamlConverter(maintenanceYamlConverter());
    }

    private static JacksonYamlHttpMessageConverter maintenanceYamlConverter() {
        var yamlFactory = YAMLFactory.builder()
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
                .disable(YAMLWriteFeature.SPLIT_LINES)
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .build();
        var converter = new JacksonYamlHttpMessageConverter(new YAMLMapper(yamlFactory));
        converter.setSupportedMediaTypes(List.of(
                MediaType.parseMediaType("application/yaml"),
                MediaType.parseMediaType("application/x-yaml")));
        return converter;
    }
}
