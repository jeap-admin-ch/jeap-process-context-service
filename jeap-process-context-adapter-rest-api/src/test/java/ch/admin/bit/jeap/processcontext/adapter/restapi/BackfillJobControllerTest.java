package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.BackfillJobService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.BackfillJobSubmission;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobExceptionReason;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobResult;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobState;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTargetType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskState;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;
import ch.admin.bit.jeap.processcontext.test.ReevaluationJobControllerTestApplication;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import java.util.stream.Stream;

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

@WebMvcTest(controllers = BackfillJobController.class,
        properties = "jeap.processcontext.maintenance.enabled=true")
@ContextConfiguration(classes = {
        ReevaluationJobControllerTestApplication.class,
        BackfillJobController.class,
        MaintenanceJobExceptionHandler.class
})
@Import(MaintenanceYamlTestConfig.class)
@AutoConfigureMockMvc
class BackfillJobControllerTest {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
    private static final UUID FAILED_TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb322");
    private static final Instant STARTED = Instant.parse("2026-08-06T08:03:12Z");
    private static final Instant COMPLETED = Instant.parse("2026-08-06T08:04:12Z");
    private static final String REQUEST_YAML = """
            process-template-name: assessmentProcess
            entries:
              - origin-process-id: assessment-4711
                process-data:
                  - key: assessmentArtefactId
                    value: art-456
                    role: FinalVersion
                  - key: assessmentId
                    value: a-123
              - origin-process-id: assessment-4712
                process-data:
                  - key: assessmentId
                    value: a-789
            """;
    private static final String LEGACY_REQUEST_YAML = """
            processTemplateName: assessmentProcess
            entries:
              - originProcessId: assessment-4711
                processData:
                  - key: assessmentId
                    value: a-123
            """;

    private static final SemanticApplicationRole WRITE_ROLE = role("write");
    private static final SemanticApplicationRole READ_ROLE = role("read");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private BackfillJobService backfillJobService;

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContextHolder.clearContext();
    }

    @Test
    void put_validYaml_submitsJobAndReturnsCreated() throws Exception {
        when(backfillJobService.submit(any())).thenReturn(true);

        mockMvc.perform(put("/api/backfill-jobs/{jobId}", JOB_ID)
                        .contentType(BackfillJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isCreated());

        ArgumentCaptor<BackfillJobSubmission> captor = ArgumentCaptor.forClass(BackfillJobSubmission.class);
        verify(backfillJobService).submit(captor.capture());
        BackfillJobSubmission submission = captor.getValue();
        assertThat(submission.jobId()).isEqualTo(JOB_ID);
        assertThat(submission.processTemplateName()).isEqualTo("assessmentProcess");
        assertThat(submission.entries()).hasSize(2);
        assertThat(submission.entries().getFirst().originProcessId()).isEqualTo("assessment-4711");
        assertThat(submission.entries().getFirst().processData()).containsExactly(
                new ProcessDataValue("assessmentArtefactId", "art-456", "FinalVersion"),
                new ProcessDataValue("assessmentId", "a-123", null));
        assertThat(submission.entries().get(1).originProcessId()).isEqualTo("assessment-4712");
        assertThat(submission.entries().get(1).processData())
                .containsExactly(new ProcessDataValue("assessmentId", "a-789", null));
    }

    @Test
    void put_legacyCamelCaseWithLegacyYamlMediaType_returnsOk() throws Exception {
        mockMvc.perform(put("/api/backfill-jobs/{jobId}", JOB_ID)
                        .contentType(BackfillJobController.APPLICATION_X_YAML_VALUE)
                        .content(LEGACY_REQUEST_YAML)
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isOk());

        ArgumentCaptor<BackfillJobSubmission> captor = ArgumentCaptor.forClass(BackfillJobSubmission.class);
        verify(backfillJobService).submit(captor.capture());
        assertThat(captor.getValue().entries().getFirst().processData())
                .containsExactly(new ProcessDataValue("assessmentId", "a-123", null));
    }

    @Test
    void put_malformedYaml_returnsBadRequest() throws Exception {
        performPut("process-template-name: assessmentProcess\nentries: [")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(backfillJobService);
    }

    @Test
    void put_yamlExceedingConfiguredRequestLimit_returnsBadRequestBeforeDeserialization() throws Exception {
        performPut("process-template-name: " + "x".repeat(2_000) + "\nentries: []\n")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(backfillJobService);
    }

    @Test
    void put_jsonContent_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(put("/api/backfill-jobs/{jobId}", JOB_ID)
                        .contentType("application/json")
                        .content("{}")
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(backfillJobService);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void put_invalidBeanValidation_returnsSanitizedBadRequest(String request) throws Exception {
        performPut(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid request"));

        verifyNoInteractions(backfillJobService);
    }

    @Test
    void put_domainValidationFailure_returnsBadRequest() throws Exception {
        MaintenanceJobException exception = mock(MaintenanceJobException.class);
        when(exception.getReason()).thenReturn(MaintenanceJobExceptionReason.INVALID_REQUEST);
        doThrow(exception).when(backfillJobService).submit(any());

        performPut(REQUEST_YAML)
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_conflictingJob_returnsConflict() throws Exception {
        MaintenanceJobException exception = mock(MaintenanceJobException.class);
        when(exception.getReason()).thenReturn(MaintenanceJobExceptionReason.CONFLICT);
        doThrow(exception).when(backfillJobService).submit(any());

        performPut(REQUEST_YAML)
                .andExpect(status().isConflict());
    }

    @Test
    void put_withoutWriteRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/backfill-jobs/{jobId}", JOB_ID)
                        .contentType(BackfillJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(READ_ROLE))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(backfillJobService);
    }

    @Test
    void put_jwtClaims_areIncludedInSubmission() {
        BackfillJobService service = mock(BackfillJobService.class);
        BackfillJobController controller = new BackfillJobController(service);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("name", "John Doe")
                .claim("ext_id", "287365")
                .build();
        BackfillJobRequest request = new BackfillJobRequest(
                "assessmentProcess",
                List.of(new BackfillJobRequest.EntryRequest(
                        "assessment-4711",
                        List.of(new BackfillJobRequest.ProcessDataRequest("assessmentId", "a-123", null)))));

        controller.create(JOB_ID, request, new JwtAuthenticationToken(jwt));

        ArgumentCaptor<BackfillJobSubmission> captor = ArgumentCaptor.forClass(BackfillJobSubmission.class);
        verify(service).submit(captor.capture());
        assertThat(captor.getValue().submitter().name()).isEqualTo("John Doe");
        assertThat(captor.getValue().submitter().extId()).isEqualTo("287365");
    }

    @Test
    void get_openJob_returnsYamlReportWithoutProcessData() throws Exception {
        when(backfillJobService.get(JOB_ID)).thenReturn(Optional.of(openJob()));

        mockMvc.perform(get("/api/backfill-jobs/{jobId}", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(BackfillJobController.APPLICATION_YAML_VALUE))
                .andExpect(content().string(containsString("job-id: 88dbb65f-9634-4685-bc86-17b72d715d3e")))
                .andExpect(content().string(containsString("job-type: process-data-backfill")))
                .andExpect(content().string(containsString("process-template-name: assessmentProcess")))
                .andExpect(content().string(containsString("job-state: open")))
                .andExpect(content().string(not(containsString("job-result:"))))
                .andExpect(content().string(containsString("started: 2026-08-06T08:03:12Z")))
                .andExpect(content().string(containsString("started-by-name: John Doe")))
                .andExpect(content().string(containsString("started-by-ext-id: \"287365\"")))
                .andExpect(content().string(containsString("entries:")))
                .andExpect(content().string(containsString("- task-id: 019c8c72-6fd1-7f25-a9a1-3b3d51fbb321")))
                .andExpect(content().string(containsString("origin-process-id: \"00123\"")))
                .andExpect(content().string(containsString("state: command-queued")))
                .andExpect(content().string(not(containsString("\n  process-data:"))))
                .andExpect(content().string(not(containsString("secret-value"))));
    }

    @Test
    void get_completedJob_returnsResultAndTaskError() throws Exception {
        when(backfillJobService.get(JOB_ID)).thenReturn(Optional.of(completedJob()));

        mockMvc.perform(get("/api/backfill-jobs/{jobId}", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("job-state: completed")))
                .andExpect(content().string(containsString("job-result: partially-succeeded")))
                .andExpect(content().string(containsString("completed: 2026-08-06T08:04:12Z")))
                .andExpect(content().string(containsString("state: succeeded")))
                .andExpect(content().string(containsString("state: failed")))
                .andExpect(content().string(containsString("error:")))
                .andExpect(content().string(containsString("message: Backfill failed")))
                .andExpect(content().string(containsString("trace-id: trace-4712")));
    }

    @Test
    void get_existingJob_acceptsLegacyYamlMediaType() throws Exception {
        when(backfillJobService.get(JOB_ID)).thenReturn(Optional.of(openJob()));

        mockMvc.perform(get("/api/backfill-jobs/{jobId}", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_X_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(BackfillJobController.APPLICATION_X_YAML_VALUE));
    }

    @Test
    void get_unknownJob_returnsNotFound() throws Exception {
        when(backfillJobService.get(JOB_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/backfill-jobs/{jobId}", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_withoutReadRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/backfill-jobs/{jobId}", JOB_ID)
                        .accept(BackfillJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(WRITE_ROLE)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(backfillJobService);
    }

    private org.springframework.test.web.servlet.ResultActions performPut(String request) throws Exception {
        return mockMvc.perform(put("/api/backfill-jobs/{jobId}", JOB_ID)
                .contentType(BackfillJobController.APPLICATION_YAML_VALUE)
                .content(request)
                .with(authenticationForUserRoles(WRITE_ROLE))
                .with(csrf()));
    }

    private static Stream<String> invalidRequests() {
        return Stream.of(
                """
                        process-template-name: ""
                        entries:
                          - origin-process-id: assessment-4711
                            process-data:
                              - key: assessmentId
                                value: a-123
                        """,
                """
                        process-template-name: assessmentProcess
                        entries: []
                        """,
                """
                        process-template-name: assessmentProcess
                        entries:
                          - origin-process-id: ""
                            process-data:
                              - key: assessmentId
                                value: a-123
                        """,
                """
                        process-template-name: assessmentProcess
                        entries:
                          - origin-process-id: assessment-4711
                            process-data: []
                        """,
                """
                        process-template-name: assessmentProcess
                        entries:
                          - origin-process-id: assessment-4711
                            process-data:
                              - key: ""
                                value: ""
                        """);
    }

    private static MaintenanceJob openJob() {
        return new MaintenanceJob(
                JOB_ID,
                MaintenanceJobType.PROCESS_DATA_BACKFILL,
                "assessmentProcess",
                "a".repeat(64),
                MaintenanceJobState.OPEN,
                null,
                STARTED,
                null,
                "John Doe",
                "287365",
                List.of(task(TASK_ID, "00123", MaintenanceTaskState.COMMAND_QUEUED, null, null)));
    }

    private static MaintenanceJob completedJob() {
        return new MaintenanceJob(
                JOB_ID,
                MaintenanceJobType.PROCESS_DATA_BACKFILL,
                "assessmentProcess",
                "a".repeat(64),
                MaintenanceJobState.COMPLETED,
                MaintenanceJobResult.PARTIALLY_SUCCEEDED,
                STARTED,
                COMPLETED,
                "John Doe",
                "287365",
                List.of(
                        task(TASK_ID, "assessment-4711", MaintenanceTaskState.SUCCEEDED, null, null),
                        task(FAILED_TASK_ID, "assessment-4712", MaintenanceTaskState.FAILED,
                                "Backfill failed", "trace-4712")));
    }

    private static MaintenanceTask task(UUID taskId, String originProcessId, MaintenanceTaskState state,
                                        String errorMessage, String errorTraceId) {
        return new MaintenanceTask(
                taskId,
                MaintenanceTargetType.PROCESS,
                originProcessId,
                originProcessId,
                state,
                STARTED,
                state.isTerminal() ? COMPLETED : null,
                errorMessage,
                errorTraceId,
                List.of(new ProcessDataValue("secret-key", "secret-value", null)));
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
}
