package ch.admin.bit.jeap.processcontext.domain;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


import static ch.admin.bit.jeap.processcontext.domain.Language.DE;

@Data
@Configuration
@ConfigurationProperties(prefix = "jeap.processcontext")
public class PcsConfigProperties {
    private int processInstanceUpdateApplicationBatchSize = 10;
    private Language processSnapshotLanguage = DE;
    private int processSnapshotArchiveRetentionPeriodMonths = 60;
}
