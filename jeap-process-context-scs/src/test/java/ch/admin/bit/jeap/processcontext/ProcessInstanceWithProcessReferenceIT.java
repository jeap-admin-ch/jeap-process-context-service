package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessRelationDTO;
import ch.admin.bit.jeap.processcontext.domain.Language;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test6.Test6Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test6EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import lombok.NonNull;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessInstanceWithProcessReferenceIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessesRelationsWithVisibilityBoth() {
        // Start a new process A
        String processTemplateName = "processRelationsA";
        originProcessId = "process-a";
        createProcessInstanceFromTemplate(processTemplateName);

        // Start a second process
        String processTemplateNameB = "processRelationsB";
        String originProcessIdProcessB = "process-b";
        createProcessInstanceFromTemplate(processTemplateNameB, originProcessIdProcessB);

        // Check process instance created event published for origin id
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessIdProcessB, processTemplateNameB);

        // Produce event 6
        createAndSendTestEvent6(originProcessId, originProcessIdProcessB);
        createAndSendTestEvent6(originProcessIdProcessB, originProcessId);

        // Produce event 1
        createAndSendTestEvent1(originProcessId);
        createAndSendTestEvent1(originProcessIdProcessB);

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompletedEvent(originProcessIdProcessB);

        ProcessRelationDTO expectedDtoProcessA = ProcessRelationDTO.builder()
                .processName(simulateTranslateService("processRelationsB"))
                .relation(simulateTranslateService("processRelationsA.processRelationOrigin.targetRole"))
                .processState("COMPLETED")
                .processId(originProcessIdProcessB)
                .build();
        ProcessRelationDTO expectedDtoProcessB = ProcessRelationDTO.builder()
                .processName(simulateTranslateService("processRelationsA"))
                .relation(simulateTranslateService("processRelationsA.processRelationOrigin.originRole"))
                .processState("COMPLETED")
                .processId(originProcessId)
                .build();
        assertProcessInstanceHasProcessReference(originProcessId, expectedDtoProcessA);
        assertProcessInstanceHasProcessReference(originProcessIdProcessB, expectedDtoProcessB);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessesRelationsWithVisibilityOrigin() {
        // Start a new process A
        String processTemplateName = "processRelationsC";
        originProcessId = "process-c";
        createProcessInstanceFromTemplate(processTemplateName);

        // Start a second process
        String processTemplateNameB = "processRelationsB";
        String originProcessIdProcessB = "process-b";
        createProcessInstanceFromTemplate(processTemplateNameB, originProcessIdProcessB);

        // Check process instance created event published for origin id
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessIdProcessB, processTemplateNameB);

        // Produce event 6
        createAndSendTestEvent6(originProcessId, originProcessIdProcessB);
        createAndSendTestEvent6(originProcessIdProcessB, originProcessId);

        // Produce event 1
        createAndSendTestEvent1(originProcessId);
        createAndSendTestEvent1(originProcessIdProcessB);

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompletedEvent(originProcessIdProcessB);

        ProcessRelationDTO expectedDtoProcessA = ProcessRelationDTO.builder()
                .processName(simulateTranslateService("processRelationsB"))
                .relation(simulateTranslateService("processRelationsC.processRelationOrigin.targetRole"))
                .processState("COMPLETED")
                .processId(originProcessIdProcessB)
                .build();

        assertProcessInstanceHasProcessReference(originProcessId, expectedDtoProcessA);
        assertProcessInstanceHasNoProcessReference(originProcessIdProcessB);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessesRelationsWithVisibilityTarget() {
        // Start a new process A
        String processTemplateName = "processRelationsD";
        originProcessId = "process-a";
        createProcessInstanceFromTemplate(processTemplateName);

        // Start a second process
        String processTemplateNameB = "processRelationsB";
        String originProcessIdProcessB = "process-b";
        createProcessInstanceFromTemplate(processTemplateNameB, originProcessIdProcessB);

        // Check process instance created event published for origin id
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessIdProcessB, processTemplateNameB);

        // Produce event 6
        createAndSendTestEvent6(originProcessId, originProcessIdProcessB);
        createAndSendTestEvent6(originProcessIdProcessB, originProcessId);

        // Produce event 1
        createAndSendTestEvent1(originProcessId);
        createAndSendTestEvent1(originProcessIdProcessB);

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompletedEvent(originProcessIdProcessB);

        ProcessRelationDTO expectedDtoProcessD = ProcessRelationDTO.builder()
                .processName(simulateTranslateService("processRelationsD"))
                .relation(simulateTranslateService("processRelationsD.processRelationOrigin.originRole"))
                .processState("COMPLETED")
                .processId(originProcessId)
                .build();
        assertProcessInstanceHasNoProcessReference(originProcessId);
        assertProcessInstanceHasProcessReference(originProcessIdProcessB, expectedDtoProcessD);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessesRelationsWithThreeProcesses() {
        // Start a new process A
        String processTemplateName = "processRelationsA";
        String originProcessIdA = "process-a";
        createProcessInstanceFromTemplate(processTemplateName, originProcessIdA);

        // Start a second process
        String processTemplateNameB = "processRelationsB";
        String originProcessIdProcessB = "process-b";
        createProcessInstanceFromTemplate(processTemplateNameB, originProcessIdProcessB);

        // Start a third process
        String processTemplateNameE = "processRelationsE";
        String originProcessIdProcessE = "process-e";
        createProcessInstanceFromTemplate(processTemplateNameE, originProcessIdProcessE);

        // Check process instance created event published for origin id
        assertProcessInstanceCreatedEvent(originProcessIdA, processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessIdProcessB, processTemplateNameB);
        assertProcessInstanceCreatedEvent(originProcessIdProcessE, processTemplateNameE);

        // Produce event 6
        createAndSendTestEvent6(originProcessIdA, originProcessIdProcessB);
        createAndSendTestEvent6(originProcessIdProcessB, originProcessIdA);
        createAndSendTestEvent6(originProcessIdProcessE, originProcessIdA);

        // Produce event 1
        createAndSendTestEvent1(originProcessIdA);
        createAndSendTestEvent1(originProcessIdProcessB);
        createAndSendTestEvent1(originProcessIdProcessE);

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessIdA);
        assertProcessInstanceCompletedEvent(originProcessIdProcessB);
        assertProcessInstanceCompletedEvent(originProcessIdProcessE);

        assertProcessInstanceHasNumberOfProcessReference(originProcessIdA, 2);
        assertProcessInstanceHasNumberOfProcessReference(originProcessIdProcessB, 1);
        assertProcessInstanceHasNumberOfProcessReference(originProcessIdProcessE, 1);
    }

    private void createAndSendTestEvent1(String originProcessId) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds("taskId1")
                .build();
        sendSync("topic.test1", event1);
    }

    private void createAndSendTestEvent6(String originProcessId, String otherProcessId) {
        Test6Event test6Event = Test6EventBuilder.createForProcessId(originProcessId)
                .processReference(otherProcessId)
                .build();
        sendSync("topic.test6", test6Event);
    }

    protected void assertProcessInstanceHasProcessReference(String originProcessId, ProcessRelationDTO expectedDto) {
        Awaitility.await()
                .pollInSameThread()
                .atMost(TIMEOUT)
                .until(() -> !processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getProcessRelations().isEmpty());

        List<ProcessRelationDTO> processRelationDTOList = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getProcessRelations();
        ProcessRelationDTO dto = processRelationDTOList.get(0);
        assertEquals(expectedDto.getProcessName().get("de"), dto.getProcessName().get("de"));
        assertEquals(expectedDto.getProcessState(), dto.getProcessState());
        assertEquals(expectedDto.getRelation(), dto.getRelation());
        assertEquals(expectedDto.getProcessId(), dto.getProcessId());
    }

    protected void assertProcessInstanceHasNoProcessReference(String originProcessId) {
        assertProcessInstanceHasNumberOfProcessReference(originProcessId, 0);
    }

    protected void assertProcessInstanceHasNumberOfProcessReference(String originProcessId, int numberOfProcessReferences) {
        List<ProcessRelationDTO> processRelationDTOList = processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getProcessRelations();
        assertEquals(numberOfProcessReferences, processRelationDTOList.size());
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

    private static Map<String, String> simulateTranslateService(@NonNull String value) {
        Map<String, String> labels = new HashMap<>();
        labels.put(Language.DE.name().toLowerCase(), value);
        return labels;
    }

}
