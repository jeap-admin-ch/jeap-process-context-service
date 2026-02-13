package ch.admin.bit.jeap.processcontext.adapter.restapi.config;

import ch.admin.bit.jeap.processcontext.adapter.restapi.ProcessInstanceController;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@ComponentScan(basePackageClasses = {ProcessInstanceController.class})
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class RestApiAdapterConfig {
}
