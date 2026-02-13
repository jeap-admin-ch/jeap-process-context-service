package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelation;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelationRole;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationView;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProcessInstanceController.class)
@ContextConfiguration(classes = RestApiTestContext.class)
@AutoConfigureMockMvc
@SuppressWarnings("java:S5976") // Replace test with parametrized test, doesn't help readability in this case, ignore
class ProcessInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProcessInstanceQueryRepository repository;
    @MockitoBean
    private ProcessRelationsService processRelationsService;
    @MockitoBean
    private ProcessSnapshotRepository processSnapshotRepository;
    @MockitoBean
    private MessageRepository messageRepository;
    @MockitoBean
    private MessageReferenceRepository messageReferenceRepository;
    @MockitoBean
    private RelationRepository relationRepository;
    @MockitoBean
    private ProcessRelationRepository processRelationRepository;
    @MockitoBean
    private ProcessDataRepository processDataRepository;
    @MockitoBean
    private TaskInstanceRepository taskInstanceRepository;
    @MockitoBean
    private TranslateService translateService;

    private final Map<String, String> reason = Map.of("de", "Alle Prozessaufgaben haben einen Endzustand erreicht",
            "fr", "Alle Prozessaufgaben haben einen Endzustand erreicht",
            "it", "Alle Prozessaufgaben haben einen Endzustand erreicht");

    private static final SemanticApplicationRole VIEW_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("processinstance")
            .operation("view")
            .build();

    private static final SemanticApplicationRole FOO_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("processinstance")
            .operation("foo")
            .build();

    @Test
    void testGetProcessInstanceByOriginProcessId_whenFound_thenReturnProcessInstanceResource() throws Exception {
        String originProcessId = "123";
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        doReturn(Optional.of(processInstance)).when(repository).findByOriginProcessId(originProcessId);
        TaskInstance taskInstance = ProcessInstanceStubs.createPlannedTaskInstance(processInstance);
        when(taskInstanceRepository.findByProcessInstanceId(processInstance.getProcessTemplate(), processInstance.getId())).thenReturn(List.of(taskInstance));

        // Mock message reference repository to return the event message
        ZonedDateTime now = ZonedDateTime.now();
        MessageReferenceMessageDTO eventMessageRef = MessageReferenceMessageDTO.builder()
                .messageReferenceId(Generators.timeBasedEpochGenerator().generate())
                .messageId(Generators.timeBasedEpochGenerator().generate())
                .messageName(ProcessInstanceStubs.event)
                .messageCreatedAt(now)
                .messageReceivedAt(now)
                .messageData(List.of(MessageReferenceMessageDataDTO.builder()
                        .messageDataKey("myKey")
                        .messageDataValue("myValue")
                        .build()))
                .relatedOriginTaskIds(Set.of("taskId1", "taskId2"))
                .build();
        when(messageReferenceRepository.findByProcessInstanceId(processInstance.getId())).thenReturn(List.of(eventMessageRef));

        Map<String, String> name = Map.of("de", processInstance.getProcessTemplate().getName(),
                "fr", processInstance.getProcessTemplate().getName(),
                "it", processInstance.getProcessTemplate().getName());

        mockMvc.perform(get("/api/processes/{originProcessId}", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originProcessId", is(processInstance.getOriginProcessId())))
                .andExpect(jsonPath("$.state", is(processInstance.getState().name())))
                .andExpect(jsonPath("$.createdAt", not(empty())))
                .andExpect(jsonPath("$.modifiedAt", not(empty())))
                .andExpect(jsonPath("$.name", is(name)))
                .andExpect(jsonPath("$.tasks[0].createdAt", not(empty())))
                .andExpect(jsonPath("$.tasks[0].lifecycle", is("STATIC")))
                .andExpect(jsonPath("$.tasks[0].cardinality", is("SINGLE_INSTANCE")))
                .andExpect(jsonPath("$.tasks[0].state", is("PLANNED")));
    }

    @Test
    void testGetProcessInstanceByOriginProcessId_whenNotFound_thenReturnsNotFound() throws Exception {
        String originProcessId = "foo";

        mockMvc.perform(get("/api/processes/{originProcessId}", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetProcessInstanceByOriginProcessId_whenNoReadRole_thenReturnsForbidden() throws Exception {
        String originProcessId = "foo";

        mockMvc.perform(get("/api/processes/{originProcessId}", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(FOO_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testFindProcessInstance_whenNoHit_thenOK() throws Exception {
        mockMvc.perform(get("/api/processes/")
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }


    @Test
    void testGetCompletedProcessInstanceByOriginProcessId_whenFound_thenReturnProcessInstanceResource() throws Exception {
        String originProcessId = "123";
        ProcessInstance processInstance = ProcessInstanceStubs.createCompletedProcessInstance();
        doReturn(Optional.of(processInstance)).when(repository).findByOriginProcessId(originProcessId);
        TaskInstance completedTask = ProcessInstanceStubs.createCompletedTaskInstance(processInstance);
        when(taskInstanceRepository.findByProcessInstanceId(processInstance.getProcessTemplate(), processInstance.getId())).thenReturn(List.of(completedTask));

        Map<String, String> name = Map.of("de", processInstance.getProcessTemplate().getName(),
                "fr", processInstance.getProcessTemplate().getName(),
                "it", processInstance.getProcessTemplate().getName());

        mockMvc.perform(get("/api/processes/{originProcessId}", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originProcessId", is(processInstance.getOriginProcessId())))
                .andExpect(jsonPath("$.state", is(processInstance.getState().name())))
                .andExpect(jsonPath("$.createdAt", not(empty())))
                .andExpect(jsonPath("$.modifiedAt", not(empty())))
                .andExpect(jsonPath("$.name", is(name)))
                .andExpect(jsonPath("$.processCompletion.conclusion", is("SUCCEEDED")))
                .andExpect(jsonPath("$.processCompletion.completedAt", not(empty())))
                .andExpect(jsonPath("$.processCompletion.reason", is(reason)))
                .andExpect(jsonPath("$.tasks[0].lifecycle", is("STATIC")))
                .andExpect(jsonPath("$.tasks[0].cardinality", is("SINGLE_INSTANCE")))
                .andExpect(jsonPath("$.tasks[0].state", is("COMPLETED")));
    }


    private JeapAuthenticationToken createAuthenticationForUserRoles(SemanticApplicationRole... userroles) {
        return JeapAuthenticationTestTokenBuilder.create().withUserRoles(userroles).build();
    }

    @Test
    void testFindProcessInstanceByProcessData_whenProcessDataIsEmpty_thenOK() throws Exception {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        doReturn(new PageImpl<>(List.of(processInstance)))
                .when(repository)
                .findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        mockMvc.perform(get("/api/processes/")
                        .param("processData", "")
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void testFindProcessInstanceByProcessData_withProcessData_thenOK() throws Exception {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance("templateName");
        doReturn(new PageImpl<>(List.of(processInstance)))
                .when(repository)
                .findByProcessData("v1", PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        mockMvc.perform(get("/api/processes/")
                        .param("processData", "v1")
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void testGetProcessRelationsByOriginProcessId_whenFound_thenReturnPage() throws Exception {
        String originProcessId = "123";
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(repository.findByOriginProcessId(originProcessId)).thenReturn(Optional.of(processInstance));

        ProcessRelationView view = ProcessRelationView.builder()
                .relationName("testRelation")
                .originRole("origin")
                .targetRole("target")
                .relationRole(ProcessRelationRoleType.ORIGIN)
                .relation(Map.of("de", "Testbeziehung"))
                .processTemplateName("template")
                .processName(Map.of("de", "Prozess"))
                .processId("related-1")
                .processState("IN_PROGRESS")
                .build();
        when(processRelationsService.createProcessRelationsPaged(eq(processInstance), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(view)));

        mockMvc.perform(get("/api/processes/{originProcessId}/process-relations", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].processId", is("related-1")))
                .andExpect(jsonPath("$.content[0].processState", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.content[0].relation.de", is("Testbeziehung")))
                .andExpect(jsonPath("$.content[0].processName.de", is("Prozess")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void testGetProcessRelationsByOriginProcessId_whenNotFound_thenReturnEmptyPage() throws Exception {
        String originProcessId = "unknown";
        when(repository.findByOriginProcessId(originProcessId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/processes/{originProcessId}/process-relations", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    void testGetProcessRelationsByOriginProcessId_whenNoReadRole_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/processes/{originProcessId}/process-relations", "123")
                        .with(authentication(createAuthenticationForUserRoles(FOO_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetProcessRelationsByOriginProcessId_whenNotFoundButSnapshotExists_thenReturnSnapshotRelations() throws Exception {
        String originProcessId = "snapshot-1";
        when(repository.findByOriginProcessId(originProcessId)).thenReturn(Optional.empty());

        ProcessRelation relation = new ProcessRelation(
                ProcessRelationRole.ORIGIN, "rel1", "originRole", "targetRole",
                "related-1", "template", "Template Label", "IN_PROGRESS");
        ProcessSnapshot snapshot = mock(ProcessSnapshot.class);
        when(snapshot.getProcessRelations()).thenReturn(List.of(relation));
        when(processSnapshotRepository.loadAndDeserializeNewestSnapshot(originProcessId))
                .thenReturn(Optional.of(snapshot));
        when(translateService.translateProcessRelationOriginRole("template", "rel1"))
                .thenReturn(Map.of("de", "Beziehung"));
        when(translateService.translateProcessTemplateName("template", "Template Label"))
                .thenReturn(Map.of("de", "Vorlage"));

        mockMvc.perform(get("/api/processes/{originProcessId}/process-relations", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].processId", is("related-1")))
                .andExpect(jsonPath("$.content[0].processState", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.content[0].relation.de", is("Beziehung")))
                .andExpect(jsonPath("$.content[0].processName.de", is("Vorlage")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void testGetProcessDataByOriginProcessId_whenNotFoundButSnapshotExists_thenReturnSnapshotData() throws Exception {
        String originProcessId = "snapshot-2";
        when(repository.findIdByOriginProcessId(originProcessId)).thenReturn(null);

        var snapshotData = ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData.newBuilder()
                .setKey("snapKey").setValue("snapValue").setRole("snapRole").build();
        ProcessSnapshot snapshot = mock(ProcessSnapshot.class);
        when(snapshot.getProcessData()).thenReturn(List.of(snapshotData));
        when(processSnapshotRepository.loadAndDeserializeNewestSnapshot(originProcessId))
                .thenReturn(Optional.of(snapshot));

        mockMvc.perform(get("/api/processes/{originProcessId}/process-data", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].key", is("snapKey")))
                .andExpect(jsonPath("$.content[0].value", is("snapValue")))
                .andExpect(jsonPath("$.content[0].role", is("snapRole")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void testGetProcessDataByOriginProcessId_whenFound_thenReturnPage() throws Exception {
        String originProcessId = "123";
        UUID processInstanceId = UUID.randomUUID();
        when(repository.findIdByOriginProcessId(originProcessId)).thenReturn(processInstanceId);

        ProcessData data1 = new ProcessData("key1", "value1");
        ProcessData data2 = new ProcessData("key2", "value2", "roleA");
        when(processDataRepository.findByProcessInstanceId(eq(processInstanceId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(data1, data2), PageRequest.of(0, 10), 2));

        mockMvc.perform(get("/api/processes/{originProcessId}/process-data", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].key", is("key1")))
                .andExpect(jsonPath("$.content[0].value", is("value1")))
                .andExpect(jsonPath("$.content[1].key", is("key2")))
                .andExpect(jsonPath("$.content[1].role", is("roleA")))
                .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    void testGetProcessDataByOriginProcessId_whenNotFound_thenReturnEmptyPage() throws Exception {
        String originProcessId = "unknown";
        when(repository.findIdByOriginProcessId(originProcessId)).thenReturn(null);

        mockMvc.perform(get("/api/processes/{originProcessId}/process-data", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    void testGetProcessDataByOriginProcessId_whenNoReadRole_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/processes/{originProcessId}/process-data", "123")
                        .with(authentication(createAuthenticationForUserRoles(FOO_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetMessagesByOriginProcessId_whenFound_thenReturnPage() throws Exception {
        String originProcessId = "123";
        UUID processInstanceId = UUID.randomUUID();
        when(repository.findIdByOriginProcessId(originProcessId)).thenReturn(processInstanceId);

        ZonedDateTime now = ZonedDateTime.now();
        MessageReferenceMessageDTO ref = MessageReferenceMessageDTO.builder()
                .messageReferenceId(Generators.timeBasedEpochGenerator().generate())
                .messageId(Generators.timeBasedEpochGenerator().generate())
                .messageName("TestEvent")
                .messageCreatedAt(now)
                .messageReceivedAt(now)
                .messageData(List.of(MessageReferenceMessageDataDTO.builder()
                        .messageDataKey("dataKey")
                        .messageDataValue("dataValue")
                        .build()))
                .relatedOriginTaskIds(Set.of("task1"))
                .build();
        when(messageReferenceRepository.findByProcessInstanceId(eq(processInstanceId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ref), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/processes/{originProcessId}/messages", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("TestEvent")))
                .andExpect(jsonPath("$.content[0].relatedOriginTaskIds", hasSize(1)))
                .andExpect(jsonPath("$.content[0].relatedOriginTaskIds[0]", is("task1")))
                .andExpect(jsonPath("$.content[0].messageData", hasSize(1)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void testGetMessagesByOriginProcessId_whenNotFound_thenReturnEmptyPage() throws Exception {
        String originProcessId = "unknown";
        when(repository.findIdByOriginProcessId(originProcessId)).thenReturn(null);

        mockMvc.perform(get("/api/processes/{originProcessId}/messages", originProcessId)
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    void testGetMessagesByOriginProcessId_whenNoReadRole_thenReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/processes/{originProcessId}/messages", "123")
                        .with(authentication(createAuthenticationForUserRoles(FOO_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @BeforeEach
    void setup() {
        when(translateService.translateProcessCompletionName(anyString(), eq("allTasksInFinalStateProcessCompletionCondition"))).thenReturn(reason);
        when(translateService.translateProcessTemplateName("template")).thenReturn(
                Map.of("de", "template", "it", "template", "fr", "template"));
    }

}
