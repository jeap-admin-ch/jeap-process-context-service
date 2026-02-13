package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.processcontext.adapter.restapi.ProcessInstanceController;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.MessageDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessDataDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessRelationDTO;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.event.process.snapshot.created.ProcessSnapshotCreatedEvent;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import ch.admin.bit.jeap.test.processcontext.KafkaIntegrationTestConfig;
import ch.admin.bit.jeap.test.processcontext.TestApp;
import com.fasterxml.uuid.Generators;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("local")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        value = {
                "jeap.processcontext.kafka.topic.process-snapshot-created=process-snapshot-created",
                "jeap.processcontext.kafka.topic.process-outdated-internal=outdated",
                "jeap.messaging.kafka.error-topic-name=errorTopic",
                "jeap.security.oauth2.resourceserver.authorizationServer.issuer=http://test/",
                "jeap.processcontext.housekeeping.pageSize=1",
                "jeap.messaging.kafka.bootstrapServers=${spring.embedded.kafka.brokers}",
                "jeap.processcontext.objectstorage.snapshot-bucket=test-bucket",
                "jeap.processcontext.objectstorage.snapshot-retention-days=3"
        })
@ContextConfiguration(classes = {TestApp.class, KafkaIntegrationTestConfig.class})
@Slf4j
@AutoConfigureObservability
public abstract class ProcessInstanceITBase extends KafkaIntegrationTestBase {

    protected static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Autowired
    protected ProcessInstanceController processInstanceController;
    @Autowired
    protected EventListenerStub<ProcessSnapshotCreatedEvent> processSnapshotCreatedEventListener;
    @Autowired
    protected ProcessTemplateRepository processTemplateRepository;
    @MockitoBean
    private TranslateService translateService;

    protected String originProcessId;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        Awaitility.setDefaultTimeout(TIMEOUT);
    }

    @BeforeEach
    void translateService() {
        when(translateService.translateProcessCompletionName(anyString(), anyString())).thenAnswer(invocation -> Map.of("de", invocation.getArgument(0)));
        when(translateService.translateProcessCompletionNameFromSnapshot(anyString(), anyString(), anyString())).thenAnswer(invocation -> Map.of("de", invocation.getArgument(1)));
        when(translateService.translateProcessTemplateName(anyString())).thenAnswer(invocation -> Map.of("de", invocation.getArgument(0)));
        when(translateService.translateTaskTypeName(anyString(), anyString())).thenAnswer(invocation -> Map.of("de", invocation.getArgument(0) + ".task." + invocation.getArgument(1)));
        when(translateService.translateTaskDataKey(anyString(), anyString(), anyString())).thenAnswer(invocation -> Map.of("de", invocation.getArgument(0) + ".task." + invocation.getArgument(1) + ".data." + invocation.getArgument(2)));
        when(translateService.translateProcessRelationTargetRole(anyString(), anyString())).thenAnswer(invocation -> Map.of("de", invocation.getArgument(0) + "." + invocation.getArgument(1) + ".targetRole"));
        when(translateService.translateProcessRelationOriginRole(anyString(), anyString())).thenAnswer(invocation -> Map.of("de", invocation.getArgument(0) + "." + invocation.getArgument(1) + ".originRole"));
    }

    @BeforeEach
    void generateProcessId() {
        originProcessId = Generators.timeBasedEpochGenerator().generate().toString();
    }

    @BeforeEach
    void clearDatabase() {
        originProcessId = Generators.timeBasedEpochGenerator().generate().toString();
        jdbcTemplate.update("DELETE FROM task_instance");
        jdbcTemplate.update("DELETE FROM process_instance_process_data");
        jdbcTemplate.update("DELETE FROM pending_message");
        jdbcTemplate.update("DELETE FROM events_event_data");
        jdbcTemplate.update("DELETE FROM event_reference");
        jdbcTemplate.update("DELETE FROM events_origin_task_ids");
        jdbcTemplate.update("DELETE FROM events");
        jdbcTemplate.update("DELETE FROM process_instance_relations");
        jdbcTemplate.update("DELETE FROM process_instance_process_relations");
        jdbcTemplate.update("DELETE FROM process_instance");
    }

    protected void assertTasks(TaskInstanceAssertionDto... expectedTasks) {
        assertTasks(originProcessId, expectedTasks);
    }

    protected void assertTasks(String originProcessId, TaskInstanceAssertionDto... expectedTasks) {
        Awaitility.await()
                .pollInSameThread()
                .until(() -> getTasks(originProcessId), is(equalTo(Set.of(expectedTasks))));
    }

    private Set<TaskInstanceAssertionDto> getTasks(String originProcessId) {
        return TaskInstanceAssertionDto.toTaskInstanceAssertions(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId));
    }

    protected void assertProcessInstanceCompleted(String originProcessId) {
        Awaitility.await()
                .pollInSameThread()
                .until(() -> {
                    Optional<ProcessInstanceDTO> processInstanceDTO = getProcessInstance(originProcessId);
                    return processInstanceDTO.isPresent() && ProcessState.COMPLETED.name().equals(processInstanceDTO.get().getState());
                });
    }

    protected void assertSnapshotCreatedEvent() {
        processSnapshotCreatedEventListener
                .awaitEvent(e -> originProcessId.equals(e.getProcessId()) &&
                        (e.getReferences().getReference().getSnapshotVersion() == 1));
    }

    protected void assertProcessInstanceCreated(String originProcessId, String processName) {
        Awaitility.await()
                .pollInSameThread()
                .until(() -> {
                    Optional<ProcessInstanceDTO> processInstanceDTO = getProcessInstance(originProcessId);
                    return processInstanceDTO.isPresent() &&
                            processInstanceDTO.get().getTemplateName().equals(processName);
                });
    }

    private Optional<ProcessInstanceDTO> getProcessInstance(String originProcessId) {
        try {
            return Optional.ofNullable(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    protected List<MessageDTO> getMessages(String originProcessId) {
        return processInstanceController.getMessagesByOriginProcessId(originProcessId, Pageable.unpaged()).getContent();
    }

    protected List<ProcessDataDTO> getProcessData(String originProcessId) {
        return processInstanceController.getProcessDataByOriginProcessId(originProcessId, Pageable.unpaged()).getContent();
    }

    protected List<ProcessRelationDTO> getProcessRelations(String originProcessId) {
        return processInstanceController.getProcessRelationsByOriginProcessId(originProcessId, Pageable.unpaged()).getContent();
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        final SemanticApplicationRole readRole = SemanticApplicationRole.builder()
                .system("jme")
                .resource("processinstance")
                .operation("view")
                .build();
        final SemanticApplicationRole createRole = SemanticApplicationRole.builder()
                .system("jme")
                .resource("processinstance")
                .operation("create")
                .build();

        return createAuthenticationForUserRoles(readRole, createRole);
    }

    JeapAuthenticationToken createAuthenticationForUserRoles(SemanticApplicationRole... userroles) {
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(userroles).build();
    }

}
