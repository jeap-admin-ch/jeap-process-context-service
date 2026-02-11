package ch.admin.bit.jeap.processcontext.adapter.kafka.message;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "jeap.processcontext.kafka")
@Data
public class ProcessContextKafkaConfiguration {

    Map<String, String> filters = new HashMap<>();

    boolean messageConsumerPaused = false;
}
