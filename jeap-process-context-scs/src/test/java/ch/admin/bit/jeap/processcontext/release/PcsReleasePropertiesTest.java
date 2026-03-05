package ch.admin.bit.jeap.processcontext.release;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PcsReleasePropertiesTest {

    @Test
    void validate_throwsIfNotSet() {
        PcsReleaseProperties props = new PcsReleaseProperties();

        assertThrows(PcsReleaseProperties.MinReleaseNotSetException.class, props::validate);
    }

    @Test
    void validate_doesNotThrowIfSet() {
        PcsReleaseProperties props = new PcsReleaseProperties();
        props.setMinVersion(17);

        assertDoesNotThrow(props::validate);
    }
}
