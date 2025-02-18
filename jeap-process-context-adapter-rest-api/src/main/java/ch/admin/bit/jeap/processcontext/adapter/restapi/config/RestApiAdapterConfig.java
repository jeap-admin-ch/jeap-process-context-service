package ch.admin.bit.jeap.processcontext.adapter.restapi.config;

import ch.admin.bit.jeap.processcontext.adapter.restapi.ProcessInstanceController;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {ProcessInstanceController.class})
class RestApiAdapterConfig {
}
