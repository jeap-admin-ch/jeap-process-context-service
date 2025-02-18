package ch.admin.bit.jeap.processcontext.adapter.restapi;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ExternalReferenceDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.NewProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import ch.admin.bit.jeap.security.resource.semanticAuthentication.SemanticApplicationRole;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.JeapAuthenticationTestTokenBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProcessInstanceController.class)
@ContextConfiguration(classes = RestApiTestContext.class)
@AutoConfigureMockMvc
class ProcessInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private ProcessInstanceQueryRepository repository;
    @MockitoBean
    private ProcessInstanceService processInstanceService;
    @MockitoBean
    private ProcessRelationsService processRelationsService;
    @MockitoBean
    private ProcessSnapshotRepository processSnapshotRepository;
    @MockitoBean
    private MessageRepository messageRepository;
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

    private static final SemanticApplicationRole CREATE_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("processinstance")
            .operation("create")
            .build();

    private static final SemanticApplicationRole FOO_ROLE = SemanticApplicationRole.builder()
            .system("jme")
            .resource("processinstance")
            .operation("foo")
            .build();

    @Test
    void putNewProcessInstance_whenValid_thenReturnsCreated() throws Exception {
        String originProcessId = "123";
        String processTemplateName = "my-template";
        ProcessData processData1 = new ProcessData("name1", "value1");
        ProcessData processData2 = new ProcessData("name2", "value2");
        Set<ProcessData> processData = Set.of(processData1, processData2);
        NewProcessInstanceDTO newProcessInstanceDTO = new NewProcessInstanceDTO();
        newProcessInstanceDTO.setProcessTemplateName(processTemplateName);
        Set<ExternalReferenceDTO> externalReferenceDTOs = Set.of(
                new ExternalReferenceDTO(processData1.getKey(), processData1.getValue()),
                new ExternalReferenceDTO(processData2.getKey(), processData2.getValue()));
        newProcessInstanceDTO.setExternalReferences(externalReferenceDTOs);

        mockMvc.perform(
                        put("/api/processes/{originProcessId}", originProcessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newProcessInstanceDTO))
                                .with(csrf()) // needed because in this test no bearer token is provided, just the final Spring authentication
                                .with(authentication(createAuthenticationForUserRoles(CREATE_ROLE))))
                .andExpect(status().isCreated());

        verify(processInstanceService).createProcessInstance(originProcessId, processTemplateName, processData);
    }

    @Test
    void putNewProcessInstance_whenValidWithNoProcessData_thenReturnsCreated() throws Exception {
        String originProcessId = "123";
        String processTemplateName = "my-template";
        NewProcessInstanceDTO newProcessInstanceDTO = new NewProcessInstanceDTO();
        newProcessInstanceDTO.setProcessTemplateName(processTemplateName);

        mockMvc.perform(
                        put("/api/processes/{originProcessId}", originProcessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newProcessInstanceDTO))
                                .with(csrf()) // needed because in this test no bearer token is provided, just the final Spring authentication
                                .with(authentication(createAuthenticationForUserRoles(CREATE_ROLE))))
                .andExpect(status().isCreated());

        verify(processInstanceService).createProcessInstance(originProcessId, processTemplateName, emptySet());
    }

    @Test
    void putNewProcessInstance_whenProcessInstantiationExceptionIsThrown_thenReturnsBadRequest() throws Exception {
        String originProcessId = "123";
        String processTemplateName = "my-template";
        NewProcessInstanceDTO newProcessInstanceDTO = new NewProcessInstanceDTO();
        newProcessInstanceDTO.setProcessTemplateName(processTemplateName);

        doThrow(TaskPlanningException.class).when(processInstanceService)
                .createProcessInstance(originProcessId, processTemplateName, emptySet());

        mockMvc.perform(put("/api/processes/{originProcessId}", originProcessId)
                        .with(csrf()) // needed because in this test no bearer token is provided, just the final Spring authentication
                        .with(authentication(createAuthenticationForUserRoles(CREATE_ROLE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProcessInstanceDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putNewProcessInstance_whenInvalid_thenReturnsBadRequest() throws Exception {
        String originProcessId = "123";
        NewProcessInstanceDTO newProcessInstanceDTO = new NewProcessInstanceDTO();

        mockMvc.perform(put("/api/processes/{originProcessId}", originProcessId)
                        .with(csrf()) // needed because in this test no bearer token is provided, just the final Spring authentication
                        .with(authentication(createAuthenticationForUserRoles(CREATE_ROLE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProcessInstanceDTO)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(processInstanceService);
    }

    @Test
    void testGetProcessInstanceByOriginProcessId_whenFound_thenReturnProcessInstanceResource() throws Exception {
        String originProcessId = "123";
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstanceAndReachedMilestoneAndEvent();
        doReturn(Optional.of(processInstance)).when(repository).findByOriginProcessIdLoadingMessages(originProcessId);

        Milestone milestone1 = processInstance.getMilestones().stream()
                .filter(Milestone::isReached).findFirst().orElseThrow();
        Milestone milestone2 = processInstance.getMilestones().stream()
                .filter(ms -> !ms.isReached()).findFirst().orElseThrow();
        String milestone1ReachedAt = formatDateTime(milestone1);

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
                .andExpect(jsonPath("$.tasks[0].lifecycle", is("STATIC")))
                .andExpect(jsonPath("$.tasks[0].cardinality", is("SINGLE_INSTANCE")))
                .andExpect(jsonPath("$.tasks[0].state", is("PLANNED")))
                .andExpect(jsonPath("$.milestones", hasSize(2)))
                .andExpect(jsonPath("$.milestones[0].name", is(milestone1.getName())))
                .andExpect(jsonPath("$.milestones[0].state", is(milestone1.getState().name())))
                .andExpect(jsonPath("$.milestones[0].reachedAt", is(milestone1ReachedAt)))
                .andExpect(jsonPath("$.milestones[1].name", is(milestone2.getName())))
                .andExpect(jsonPath("$.milestones[1].state", is(milestone2.getState().name())))
                .andExpect(jsonPath("$.milestones[1].reachedAt", nullValue()))
                .andExpect(jsonPath("$.messages[0].name", is(ProcessInstanceStubs.event)))
                .andExpect(jsonPath("$.messages[0].relatedOriginTaskIds", hasSize(2)));
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
    void putNewProcessInstance_whenNoWriteRole_thenReturnsForbidden() throws Exception {
        String originProcessId = "123";
        NewProcessInstanceDTO newProcessInstanceDTO = new NewProcessInstanceDTO();
        newProcessInstanceDTO.setProcessTemplateName("my-template");

        mockMvc.perform(put("/api/processes/{originProcessId}", originProcessId)
                        .with(csrf()) // needed because in this test no bearer token is provided, just the final Spring authentication
                        .with(authentication(createAuthenticationForUserRoles(FOO_ROLE)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProcessInstanceDTO)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(processInstanceService);
    }

    @Test
    void testFindProcessInstance_whenNoHit_thenOK() throws Exception {
        mockMvc.perform(get("/api/processes/")
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(0)));
    }


    @Test
    void testGetCompletedProcessInstanceByOriginProcessId_whenFound_thenReturnProcessInstanceResource() throws Exception {
        String originProcessId = "123";
        ProcessInstance processInstance = ProcessInstanceStubs.createCompletedProcessInstance();
        doReturn(Optional.of(processInstance)).when(repository).findByOriginProcessIdLoadingMessages(originProcessId);

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

    private String formatDateTime(Milestone milestone) throws JsonProcessingException {
        return objectMapper.writeValueAsString(milestone.getReachedAt()).replace("\"", "");
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
                .andExpect(jsonPath("$.totalCount", is(1)));
    }

    @Test
    void testFindProcessInstanceByProcessData_withProcessData_thenOK() throws Exception {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance(
                "templateName",
                Set.of(new ProcessData("r1", "v1"),
                        new ProcessData("r2", "v2")));
        doReturn(new PageImpl<>(List.of(processInstance)))
                .when(repository)
                .findByProcessData("v1", PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        mockMvc.perform(get("/api/processes/")
                        .param("processData", "v1")
                        .with(authentication(createAuthenticationForUserRoles(VIEW_ROLE)))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", is(1)));
    }

    @BeforeEach
    void setup(){
        when(translateService.translateProcessCompletionName(anyString(), eq("allTasksInFinalStateProcessCompletionCondition"))).thenReturn(reason);
        when(translateService.translateProcessTemplateName("template")).thenReturn(
                Map.of("de","template", "it", "template", "fr","template"));
    }

}
