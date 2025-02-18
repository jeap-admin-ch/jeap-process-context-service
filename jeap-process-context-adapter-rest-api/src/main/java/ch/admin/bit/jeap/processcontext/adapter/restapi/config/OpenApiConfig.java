package ch.admin.bit.jeap.processcontext.adapter.restapi.config;

import ch.admin.bit.jeap.processcontext.adapter.restapi.ProcessInstanceController;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Process Context Service API",
                description = "API to manage process context",
                contact = @Contact(
                        email = "jeap-community@bit.admin.ch",
                        name = "jEAP",
                        url = "https://github.com/jeap-admin-ch/jeap"
                )
        ),
        externalDocs = @ExternalDocumentation(
                url = "https://github.com/jeap-admin-ch/jeap-process-context-service/blob/main/README.md",
                description = "Documentation Process Context Service"),
        security = {@SecurityRequirement(name = "OIDC_Enduser"), @SecurityRequirement(name = "OIDC_System")}
)
@Configuration
public class OpenApiConfig {

    @Bean
    GroupedOpenApi externalApi() {
        return GroupedOpenApi.builder()
                .group("Process Context Service")
                .pathsToMatch("/api/**")
                .packagesToScan(ProcessInstanceController.class.getPackageName())
                .build();
    }
}
