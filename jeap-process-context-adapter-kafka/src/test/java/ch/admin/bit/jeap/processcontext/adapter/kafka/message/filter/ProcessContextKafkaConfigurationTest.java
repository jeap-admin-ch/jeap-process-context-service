package ch.admin.bit.jeap.processcontext.adapter.kafka.message.filter;

import ch.admin.bit.jeap.processcontext.adapter.kafka.message.ProcessContextKafkaConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ProcessContextKafkaConfigurationTest {

    private ProcessContextKafkaConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new ProcessContextKafkaConfiguration();
    }

    @Nested
    @DisplayName("filters property behavior")
    class FiltersProperty {

        @Test
        @DisplayName("returns empty map when no filters are configured")
        void returnsEmptyMapWhenNoFiltersConfigured() {
            assertTrue(configuration.getFilters().isEmpty());
        }

        @Test
        @DisplayName("binds filters correctly from configuration properties")
        @SuppressWarnings("unchecked")
        void bindsFiltersCorrectlyFromConfigurationProperties() {
            Map<String, String> properties = Map.of(
                    "jeap.processcontext.kafka.filters.filter1", "value1",
                    "jeap.processcontext.kafka.filters.filter2", "value2"
            );

            Binder binder = new Binder(new MapConfigurationPropertySource(properties));
            binder.bind("jeap.processcontext.kafka.filters", Map.class).ifBound(configuration::setFilters);

            assertEquals(2, configuration.getFilters().size());
            assertEquals("value1", configuration.getFilters().get("filter1"));
            assertEquals("value2", configuration.getFilters().get("filter2"));
        }
    }
}
