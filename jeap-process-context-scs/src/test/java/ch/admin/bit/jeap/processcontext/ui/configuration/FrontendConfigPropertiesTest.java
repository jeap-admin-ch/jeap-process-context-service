package ch.admin.bit.jeap.processcontext.ui.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the values derived from the PAMS related configuration properties.
 */
class FrontendConfigPropertiesTest {

    @Test
    void pamsIsEnabledByDefault() {
        assertThat(new FrontendConfigProperties().isPamsEnabled()).isTrue();
        assertThat(new FrontendConfigProperties().isMockPams()).isFalse();
    }

    @Test
    void disablingPamsImpliesMockingPams() {
        FrontendConfigProperties properties = new FrontendConfigProperties();
        properties.setPamsEnabled(false);
        properties.setMockPams(false);

        assertThat(properties.isMockPamsEffective()).isTrue();
    }

    @Test
    void pamsMockIsNotEnabledByEnablingPams() {
        FrontendConfigProperties properties = new FrontendConfigProperties();

        assertThat(properties.isMockPamsEffective()).isFalse();

        properties.setMockPams(true);
        assertThat(properties.isMockPamsEffective()).isTrue();
    }

    @Test
    void pamsEnvironmentIsNotServedWhenPamsIsDisabled() {
        FrontendConfigProperties properties = new FrontendConfigProperties();
        properties.setPamsEnvironment("REF");
        assertThat(properties.getEffectivePamsEnvironment()).isEqualTo("REF");

        properties.setPamsEnabled(false);
        assertThat(properties.getEffectivePamsEnvironment()).isNull();
    }
}
