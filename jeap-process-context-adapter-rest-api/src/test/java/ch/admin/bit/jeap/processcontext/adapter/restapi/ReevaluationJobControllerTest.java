package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.adapter.restapi.config.MaintenanceYamlConverterCustomizer;
import ch.admin.bit.jeap.processcontext.test.ReevaluationJobControllerTestApplication;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobExceptionReason;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobState;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTargetType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskState;
import ch.admin.bit.jeap.processcontext.domain.maintenance.ReevaluationJobService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.ReevaluationJobSubmission;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReevaluationJobController.class,
        properties = "jeap.processcontext.maintenance.enabled=true")
@ContextConfiguration(classes = {
        ReevaluationJobControllerTestApplication.class,
        ReevaluationJobController.class,
        MaintenanceJobExceptionHandler.class
})
@Import(ReevaluationJobControllerTest.YamlTestConfig.class)
@AutoConfigureMockMvc
class ReevaluationJobControllerTest {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
    private static final Instant STARTED = Instant.parse("2026-08-06T08:03:12Z");
    private static final String REQUEST_YAML = """
            process-template-name: assessmentProcess
            processes:
              - origin-process-id: assessment-4711
              - origin-process-id: assessment-4712
            """;
    private static final String LEGACY_REQUEST_YAML = """
            processTemplateName: assessmentProcess
            processes:
              - originProcessId: assessment-4711
              - originProcessId: assessment-4712
            """;

    private static final SemanticApplicationRole WRITE_ROLE = role("write");
    private static final SemanticApplicationRole READ_ROLE = role("read");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ReevaluationJobService reevaluationJobService;

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContextHolder.clearContext();
    }

    @Test
    void put_validYaml_submitsJobAndReturnsCreated() throws Exception {
        when(reevaluationJobService.submit(any())).thenReturn(true);

        mockMvc.perform(put("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .contentType(ReevaluationJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isCreated());

        ArgumentCaptor<ReevaluationJobSubmission> captor = ArgumentCaptor.forClass(ReevaluationJobSubmission.class);
        verify(reevaluationJobService).submit(captor.capture());
        assertThat(captor.getValue().jobId()).isEqualTo(JOB_ID);
        assertThat(captor.getValue().processTemplateName()).isEqualTo("assessmentProcess");
        assertThat(captor.getValue().originProcessIds()).containsExactly("assessment-4711", "assessment-4712");
    }

    @Test
    void put_legacyCamelCaseWithLegacyYamlMediaType_returnsOk() throws Exception {
        mockMvc.perform(put("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .contentType(ReevaluationJobController.APPLICATION_X_YAML_VALUE)
                        .content(LEGACY_REQUEST_YAML)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void put_malformedYaml_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .contentType(ReevaluationJobController.APPLICATION_YAML_VALUE)
                        .content("processTemplateName: assessmentProcess\nprocesses: [")
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reevaluationJobService);
    }

    @Test
    void put_jsonContent_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(put("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .contentType("application/json")
                        .content("{}")
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(reevaluationJobService);
    }

    @Test
    void put_invalidRequest_returnsBadRequest() throws Exception {
        MaintenanceJobException exception = mock(MaintenanceJobException.class);
        when(exception.getReason()).thenReturn(MaintenanceJobExceptionReason.INVALID_REQUEST);
        doThrow(exception).when(reevaluationJobService).submit(any());

        mockMvc.perform(put("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .contentType(ReevaluationJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_invalidBeanValidation_returnsSanitizedBadRequest() throws Exception {
        mockMvc.perform(put("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .contentType(ReevaluationJobController.APPLICATION_YAML_VALUE)
                        .content("""
                                process-template-name: ""
                                processes: []
                                """)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid request"));

        verifyNoInteractions(reevaluationJobService);
    }

    @Test
    void put_conflictingJob_returnsConflict() throws Exception {
        MaintenanceJobException exception = mock(MaintenanceJobException.class);
        when(exception.getReason()).thenReturn(MaintenanceJobExceptionReason.CONFLICT);
        doThrow(exception).when(reevaluationJobService).submit(any());

        mockMvc.perform(put("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .contentType(ReevaluationJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void put_withoutWriteRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .contentType(ReevaluationJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(READ_ROLE))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reevaluationJobService);
    }

    @Test
    void put_jwtClaims_areIncludedInSubmission() {
        ReevaluationJobService service = mock(ReevaluationJobService.class);
        ReevaluationJobController controller = new ReevaluationJobController(service);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("name", "John Doe")
                .claim("ext_id", "287365")
                .build();
        ReevaluationJobRequest request = new ReevaluationJobRequest(
                "assessmentProcess",
                List.of(new ReevaluationJobRequest.ProcessRequest("assessment-4711")));

        controller.create(JOB_ID, request, new JwtAuthenticationToken(jwt));

        ArgumentCaptor<ReevaluationJobSubmission> captor = ArgumentCaptor.forClass(ReevaluationJobSubmission.class);
        verify(service).submit(captor.capture());
        assertThat(captor.getValue().submitter().name()).isEqualTo("John Doe");
        assertThat(captor.getValue().submitter().extId()).isEqualTo("287365");
    }

    @Test
    void get_existingJob_returnsYamlReport() throws Exception {
        when(reevaluationJobService.get(JOB_ID)).thenReturn(Optional.of(job()));

        mockMvc.perform(get("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .accept(ReevaluationJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ReevaluationJobController.APPLICATION_YAML_VALUE))
                .andExpect(content().string(containsString("job-id: 88dbb65f-9634-4685-bc86-17b72d715d3e")))
                .andExpect(content().string(containsString("job-type: relation-reevaluation")))
                .andExpect(content().string(containsString("process-template-name: assessmentProcess")))
                .andExpect(content().string(containsString("job-state: open")))
                .andExpect(content().string(not(containsString("job-result:"))))
                .andExpect(content().string(containsString("started-by-name: John Doe")))
                .andExpect(content().string(containsString("started-by-ext-id: \"287365\"")))
                .andExpect(content().string(containsString("- task-id: 019c8c72-6fd1-7f25-a9a1-3b3d51fbb321")))
                .andExpect(content().string(containsString("origin-process-id: \"00123\"")))
                .andExpect(content().string(containsString("state: event-queued")));
    }

    @Test
    void get_existingJob_acceptsLegacyYamlMediaType() throws Exception {
        when(reevaluationJobService.get(JOB_ID)).thenReturn(Optional.of(job()));

        mockMvc.perform(get("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .accept(ReevaluationJobController.APPLICATION_X_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(ReevaluationJobController.APPLICATION_X_YAML_VALUE));
    }

    @Test
    void get_unknownJob_returnsNotFound() throws Exception {
        when(reevaluationJobService.get(JOB_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .accept(ReevaluationJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_withoutReadRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/reevaluation-jobs/{jobId}", JOB_ID)
                        .accept(ReevaluationJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(WRITE_ROLE)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reevaluationJobService);
    }

    private static MaintenanceJob job() {
        return new MaintenanceJob(
                JOB_ID,
                MaintenanceJobType.RELATION_REEVALUATION,
                "assessmentProcess",
                "a".repeat(64),
                MaintenanceJobState.OPEN,
                null,
                STARTED,
                null,
                "John Doe",
                "287365",
                List.of(new MaintenanceTask(
                        TASK_ID,
                        MaintenanceTargetType.PROCESS,
                        "00123",
                        "00123",
                        MaintenanceTaskState.EVENT_QUEUED,
                        STARTED,
                        null,
                        null,
                        null)));
    }

    private static SemanticApplicationRole role(String operation) {
        return SemanticApplicationRole.builder()
                .system("jme")
                .resource("processcontextjob")
                .operation(operation)
                .build();
    }

    private static RequestPostProcessor authenticationForUserRoles(SemanticApplicationRole... roles) {
        JeapAuthenticationToken authentication = JeapAuthenticationTestTokenBuilder.create().withUserRoles(roles).build();
        return request -> {
            var securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            TestSecurityContextHolder.setContext(securityContext);
            return testSecurityContext().postProcessRequest(request);
        };
    }

    @TestConfiguration
    static class YamlTestConfig {
        @Bean
        ServerHttpMessageConvertersCustomizer yamlConverterCustomizer() {
            return new MaintenanceYamlConverterCustomizer();
        }
    }
}
