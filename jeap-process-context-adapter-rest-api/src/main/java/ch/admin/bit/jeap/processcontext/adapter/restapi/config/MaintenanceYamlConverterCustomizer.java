package ch.admin.bit.jeap.processcontext.adapter.restapi.config;

import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class MaintenanceYamlConverterCustomizer implements ServerHttpMessageConvertersCustomizer {

    private final int maxRequestBytes;

    public MaintenanceYamlConverterCustomizer(int maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }

    @Override
    public void customize(HttpMessageConverters.ServerBuilder builder) {
        builder.withYamlConverter(maintenanceYamlConverter(maxRequestBytes));
    }

    private static JacksonYamlHttpMessageConverter maintenanceYamlConverter(int maxRequestBytes) {
        var streamReadConstraints = StreamReadConstraints.builder()
                .maxDocumentLength(maxRequestBytes)
                .build();
        var yamlFactory = YAMLFactory.builder()
                .streamReadConstraints(streamReadConstraints)
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .enable(YAMLWriteFeature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
                .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
                .disable(YAMLWriteFeature.SPLIT_LINES)
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .build();
        var converter = new DocumentLengthLimitedYamlConverter(
                new YAMLMapper(yamlFactory), streamReadConstraints, maxRequestBytes);
        converter.setSupportedMediaTypes(List.of(
                MediaType.parseMediaType("application/yaml"),
                MediaType.parseMediaType("application/x-yaml")));
        return converter;
    }

    private static final class DocumentLengthLimitedYamlConverter extends JacksonYamlHttpMessageConverter {
        private final StreamReadConstraints streamReadConstraints;
        private final int maxRequestBytes;

        private DocumentLengthLimitedYamlConverter(YAMLMapper mapper, StreamReadConstraints streamReadConstraints,
                                                   int maxRequestBytes) {
            super(mapper);
            this.streamReadConstraints = streamReadConstraints;
            this.maxRequestBytes = maxRequestBytes;
        }

        @Override
        public Object read(ResolvableType type, HttpInputMessage inputMessage, Map<String, Object> hints)
                throws IOException, HttpMessageNotReadableException {
            return super.read(type, limited(inputMessage), hints);
        }

        @Override
        protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
                throws IOException, HttpMessageNotReadableException {
            return super.readInternal(clazz, limited(inputMessage));
        }

        private HttpInputMessage limited(HttpInputMessage inputMessage) {
            return new HttpInputMessage() {
                @Override
                public InputStream getBody() throws IOException {
                    return new DocumentLengthLimitedInputStream(
                            inputMessage.getBody(), streamReadConstraints, maxRequestBytes);
                }

                @Override
                public org.springframework.http.HttpHeaders getHeaders() {
                    return inputMessage.getHeaders();
                }
            };
        }
    }

    private static final class DocumentLengthLimitedInputStream extends FilterInputStream {
        private final StreamReadConstraints streamReadConstraints;
        private final int maxRequestBytes;
        private long bytesRead;

        private DocumentLengthLimitedInputStream(InputStream inputStream,
                                                 StreamReadConstraints streamReadConstraints,
                                                 int maxRequestBytes) {
            super(inputStream);
            this.streamReadConstraints = streamReadConstraints;
            this.maxRequestBytes = maxRequestBytes;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value != -1) {
                validateLength(++bytesRead);
            }
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            long remaining = maxRequestBytes - bytesRead;
            int bytesToRead = (int) Math.min(length, Math.max(1, remaining + 1));
            int count = super.read(bytes, offset, bytesToRead);
            if (count > 0) {
                bytesRead += count;
                validateLength(bytesRead);
            }
            return count;
        }

        private void validateLength(long length) {
            streamReadConstraints.validateDocumentLength(length);
        }
    }
}
