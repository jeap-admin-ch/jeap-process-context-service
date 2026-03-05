package ch.admin.bit.jeap.processcontext.release;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jeap.processcontext.release")
public class PcsReleaseProperties {

    private int minVersion = 0;

    @PostConstruct
    public void validate() {
        if (minVersion < 17) {
            throw new MinReleaseNotSetException();
        }
    }

    static class MinReleaseNotSetException extends RuntimeException {
        public MinReleaseNotSetException() {
            super("""
                    IMPORTANT: PCS v17 is a major release with breaking changes. To avoid data loss, you MUST follow the
                    upgrade guide for version 17.  To ensure that you are aware of this, the minimum release version
                    property jeap.processcontext.release.min-version must be set to at least 17""");
        }
    }

}
