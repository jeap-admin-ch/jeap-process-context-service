package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJob;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobException;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobExceptionReason;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobResult;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobState;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceJobType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTargetType;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTask;
import ch.admin.bit.jeap.processcontext.domain.maintenance.MaintenanceTaskState;
import ch.admin.bit.jeap.processcontext.domain.maintenance.RelationPublicationJobService;
import ch.admin.bit.jeap.processcontext.domain.maintenance.RelationPublicationJobSubmission;
import ch.admin.bit.jeap.processcontext.test.ReevaluationJobControllerTestApplication;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.testSecurityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RelationPublicationJobController.class,
        properties = "jeap.processcontext.maintenance.enabled=true")
@ContextConfiguration(classes = {
        ReevaluationJobControllerTestApplication.class,
        RelationPublicationJobController.class,
        MaintenanceJobExceptionHandler.class
})
@Import(MaintenanceYamlTestConfig.class)
@AutoConfigureMockMvc
class RelationPublicationJobControllerTest {

    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID TASK_ID = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
    private static final UUID RELATION_ID = UUID.fromString("019c8c72-7b42-7a04-9443-bf8ec98ce871");
    private static final Instant STARTED = Instant.parse("2026-08-06T08:03:12Z");
    private static final Instant COMPLETED = Instant.parse("2026-08-06T08:04:12Z");
    private static final String REQUEST_YAML = """
            relationIds:
              - 019c8c72-7b42-7a04-9443-bf8ec98ce871
            """;
    private static final SemanticApplicationRole WRITE_ROLE = role("write");
    private static final SemanticApplicationRole READ_ROLE = role("read");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RelationPublicationJobService relationPublicationJobService;

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContextHolder.clearContext();
    }

    @Test
    void put_validYaml_submitsJobAndReturnsCreated() throws Exception {
        when(relationPublicationJobService.submit(any())).thenReturn(true);

        performPut(REQUEST_YAML).andExpect(status().isCreated());

        ArgumentCaptor<RelationPublicationJobSubmission> captor =
                ArgumentCaptor.forClass(RelationPublicationJobSubmission.class);
        verify(relationPublicationJobService).submit(captor.capture());
        assertThat(captor.getValue().jobId()).isEqualTo(JOB_ID);
        assertThat(captor.getValue().relationIds()).containsExactly(RELATION_ID);
    }

    @Test
    void put_idempotentSubmission_returnsOk() throws Exception {
        when(relationPublicationJobService.submit(any())).thenReturn(false);

        performPut(REQUEST_YAML).andExpect(status().isOk());
    }

    @Test
    void put_invalidYamlAndRequest_returnBadRequest() throws Exception {
        performPut("relationIds: [").andExpect(status().isBadRequest());
        performPut("relationIds: []\n")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid request"));
    }

    @Test
    void put_jsonContent_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(put("/api/relation-publication-jobs/{jobId}", JOB_ID)
                        .contentType("application/json")
                        .content("{}")
                        .with(authenticationForUserRoles(WRITE_ROLE))
                        .with(csrf()))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void put_domainValidationAndConflict_useMaintenanceStatusSemantics() throws Exception {
        MaintenanceJobException invalid = mock(MaintenanceJobException.class);
        when(invalid.getReason()).thenReturn(MaintenanceJobExceptionReason.INVALID_REQUEST);
        doThrow(invalid).when(relationPublicationJobService).submit(any());
        performPut(REQUEST_YAML).andExpect(status().isBadRequest());

        MaintenanceJobException conflict = mock(MaintenanceJobException.class);
        when(conflict.getReason()).thenReturn(MaintenanceJobExceptionReason.CONFLICT);
        doThrow(conflict).when(relationPublicationJobService).submit(any());
        performPut(REQUEST_YAML).andExpect(status().isConflict());
    }

    @Test
    void put_withoutWriteRole_returnsForbidden() throws Exception {
        mockMvc.perform(put("/api/relation-publication-jobs/{jobId}", JOB_ID)
                        .contentType(RelationPublicationJobController.APPLICATION_YAML_VALUE)
                        .content(REQUEST_YAML)
                        .with(authenticationForUserRoles(READ_ROLE))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(relationPublicationJobService);
    }

    @Test
    void put_jwtClaims_areIncludedInSubmission() {
        RelationPublicationJobService service = mock(RelationPublicationJobService.class);
        RelationPublicationJobController controller = new RelationPublicationJobController(service);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("name", "John Doe")
                .claim("ext_id", "287365")
                .build();

        controller.create(JOB_ID, new RelationPublicationJobRequest(List.of(RELATION_ID)),
                new JwtAuthenticationToken(jwt));

        ArgumentCaptor<RelationPublicationJobSubmission> captor =
                ArgumentCaptor.forClass(RelationPublicationJobSubmission.class);
        verify(service).submit(captor.capture());
        assertThat(captor.getValue().submitter().name()).isEqualTo("John Doe");
        assertThat(captor.getValue().submitter().extId()).isEqualTo("287365");
    }

    @Test
    void get_existingJob_serializesYamlMetadataRelationAndSharedError() throws Exception {
        when(relationPublicationJobService.get(JOB_ID)).thenReturn(Optional.of(completedJob()));

        mockMvc.perform(get("/api/relation-publication-jobs/{jobId}", JOB_ID)
                        .accept(RelationPublicationJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(RelationPublicationJobController.APPLICATION_YAML_VALUE))
                .andExpect(content().string(containsString("job-id: 88dbb65f-9634-4685-bc86-17b72d715d3e")))
                .andExpect(content().string(containsString("job-type: relation-republication")))
                .andExpect(content().string(containsString("job-state: completed")))
                .andExpect(content().string(containsString("job-result: failed")))
                .andExpect(content().string(containsString("started: 2026-08-06T08:03:12Z")))
                .andExpect(content().string(containsString("completed: 2026-08-06T08:04:12Z")))
                .andExpect(content().string(containsString("started-by-name: John Doe")))
                .andExpect(content().string(containsString("started-by-ext-id: \"287365\"")))
                .andExpect(content().string(containsString("relations:")))
                .andExpect(content().string(containsString("- task-id: 019c8c72-6fd1-7f25-a9a1-3b3d51fbb321")))
                .andExpect(content().string(containsString("relation-id: 019c8c72-7b42-7a04-9443-bf8ec98ce871")))
                .andExpect(content().string(containsString("state: failed")))
                .andExpect(content().string(containsString("message: Publication failed")))
                .andExpect(content().string(containsString("trace-id: trace-4711")))
                .andExpect(content().string(not(containsString("process-template-name:"))))
                .andExpect(content().string(not(containsString("origin-process-id:"))));
    }

    @Test
    void get_unknownJob_returnsNotFound() throws Exception {
        when(relationPublicationJobService.get(JOB_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/relation-publication-jobs/{jobId}", JOB_ID)
                        .accept(RelationPublicationJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(READ_ROLE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_withoutReadRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/relation-publication-jobs/{jobId}", JOB_ID)
                        .accept(RelationPublicationJobController.APPLICATION_YAML_VALUE)
                        .with(authenticationForUserRoles(WRITE_ROLE)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(relationPublicationJobService);
    }

    @Test
    void openApiAnnotations_defineYamlSchemasExamplesOAuthAndResponses() throws Exception {
        Method create = RelationPublicationJobController.class.getMethod(
                "create", UUID.class, RelationPublicationJobRequest.class,
                org.springframework.security.core.Authentication.class);
        Operation createOperation = create.getAnnotation(Operation.class);
        assertThat(createOperation.requestBody().content()).singleElement().satisfies(yaml -> {
            assertThat(yaml.mediaType()).isEqualTo(RelationPublicationJobController.APPLICATION_YAML_VALUE);
            assertThat(yaml.schema().implementation()).isEqualTo(RelationPublicationJobRequest.class);
            assertThat(yaml.examples()).singleElement().satisfies(example ->
                    assertThat(example.value()).contains("relationIds:", RELATION_ID.toString()));
        });
        assertOperation(createOperation, Set.of("200", "201", "400", "403", "409"));

        Method get = RelationPublicationJobController.class.getMethod("get", UUID.class);
        Operation getOperation = get.getAnnotation(Operation.class);
        assertOperation(getOperation, Set.of("200", "403", "404"));
        assertThat(Arrays.stream(getOperation.responses())
                .filter(response -> response.responseCode().equals("200"))
                .findFirst().orElseThrow().content()).singleElement().satisfies(yaml -> {
            assertThat(yaml.mediaType()).isEqualTo(RelationPublicationJobController.APPLICATION_YAML_VALUE);
            assertThat(yaml.schema().implementation()).isEqualTo(RelationPublicationJobReport.class);
            assertThat(yaml.examples()).singleElement().satisfies(example ->
                    assertThat(example.value()).contains("relations:", "relation-id:"));
        });
    }

    private org.springframework.test.web.servlet.ResultActions performPut(String request) throws Exception {
        return mockMvc.perform(put("/api/relation-publication-jobs/{jobId}", JOB_ID)
                .contentType(RelationPublicationJobController.APPLICATION_YAML_VALUE)
                .content(request)
                .with(authenticationForUserRoles(WRITE_ROLE))
                .with(csrf()));
    }

    private static MaintenanceJob completedJob() {
        return new MaintenanceJob(
                JOB_ID,
                MaintenanceJobType.RELATION_REPUBLICATION,
                null,
                "a".repeat(64),
                MaintenanceJobState.COMPLETED,
                MaintenanceJobResult.FAILED,
                STARTED,
                COMPLETED,
                "John Doe",
                "287365",
                List.of(new MaintenanceTask(
                        TASK_ID,
                        MaintenanceTargetType.RELATION,
                        RELATION_ID.toString(),
                        "assessment-4711",
                        RELATION_ID,
                        MaintenanceTaskState.FAILED,
                        STARTED,
                        COMPLETED,
                        "Publication failed",
                        "trace-4711",
                        List.of())));
    }

    private static void assertOperation(Operation operation, Set<String> responseCodes) {
        assertThat(Arrays.stream(operation.responses())
                .map(ApiResponse::responseCode)
                .collect(Collectors.toSet())).isEqualTo(responseCodes);
        assertThat(Arrays.stream(operation.security())
                .map(SecurityRequirement::name)
                .collect(Collectors.toSet())).containsExactlyInAnyOrder("OIDC_Enduser", "OIDC_System");
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
