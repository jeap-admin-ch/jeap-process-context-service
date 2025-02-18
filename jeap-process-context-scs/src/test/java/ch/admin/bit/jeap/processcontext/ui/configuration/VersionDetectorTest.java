package ch.admin.bit.jeap.processcontext.ui.configuration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionDetectorTest {

    @Test
    void detectVersionFromString() {
        VersionDetector versionDetector = new VersionDetector();

        String codeSourceLocation = "file:/home/dev/.m2/repository/ch/admin/bit/jeap/jeap-process-context-service/3.13.1/jeap-process-context-scs-3.13.1.jar";
        String result = versionDetector.extractVersionFromString(codeSourceLocation);
        assertEquals("3.13.1", result);

        //some more tests
        assertEquals("1.2.14", versionDetector.extractVersionFromString("xx/.../sdss/1.2.14"));
    }

    @Test
    void detectSnapshotVersionFromString() {
        VersionDetector versionDetector = new VersionDetector();

        String codeSourceLocation = "file:/home/dev/.m2/repository/ch/admin/bit/jeap/jeap-process-context-service/3.13.1-SNAPSHOT/jeap-process-context-scs-3.13.1-SNAPSHOT.jar";
        String result = versionDetector.extractVersionFromString(codeSourceLocation);

        assertEquals("3.13.1-SNAPSHOT", result);
    }

    @Test
    void detectMissingVersionFromString() {
        VersionDetector versionDetector = new VersionDetector();

        String codeSourceLocation = "file:/home/dev/.m2/repository/ch/admin/bit/jeap/jeap-process-context-service/noversion/jeap-process-context-scs-noversion.jar";
        String result = versionDetector.extractVersionFromString(codeSourceLocation);

        assertEquals("??", result);
    }

    @Disabled("This test is problematic. If the branch names contains something that looks like a version, this test will fail the Jenkins build.")
    @Test
    void getVersion() {
        VersionDetector versionDetector = new VersionDetector();
        String version = versionDetector.getVersion();

        assertEquals("??", version);
    }

}