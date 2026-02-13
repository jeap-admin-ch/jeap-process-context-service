package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessRelationDTO;
import ch.admin.bit.jeap.processcontext.domain.Language;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.event.test6.Test6Event;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test6EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/process_relations_*.json")
class ProcessInstanceWithProcessReferenceIT extends ProcessInstanceMockS3ITBase {

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessesRelationsWithVisibilityBoth() {
        // Start a new process A
        String processTemplateNameA = "processRelationsA";
        originProcessId = "process-a";
        createProcess(originProcessId, processTemplateNameA, "A");

        // Start a second process
        String processTemplateNameB = "processRelationsB";
        String originProcessIdProcessB = "process-b";
        createProcess(originProcessIdProcessB, processTemplateNameB, "B");

        // Produce event 6
        createAndSendTestEvent6(originProcessId, originProcessIdProcessB);
        createAndSendTestEvent6(originProcessIdProcessB, originProcessId);

        // Produce event 1
        createAndSendTestEvent1(originProcessId);
        createAndSendTestEvent1(originProcessIdProcessB);

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompleted(originProcessIdProcessB);

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

    private void createProcess(String processId, String processTemplateName, String objectId) {
        Test2Event event2 = Test2EventBuilder.createForProcessId(processId)
                .objectId(objectId)
                .build();
        sendSync("topic.test2", event2);
        assertProcessInstanceCreated(processId, processTemplateName);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void testProcessesRelationsWithVisibilityOrigin() {
        // Start a new process A
        String processTemplateName = "processRelationsC";
        originProcessId = "process-c";
        createProcess(originProcessId, processTemplateName, "C");

        // Start a second process
        String processTemplateNameB = "processRelationsB";
        String originProcessIdProcessB = "process-b";
        createProcess(originProcessIdProcessB, processTemplateNameB, "B");

        // Produce event 6
        createAndSendTestEvent6(originProcessId, originProcessIdProcessB);
        createAndSendTestEvent6(originProcessIdProcessB, originProcessId);

        // Produce event 1
        createAndSendTestEvent1(originProcessId);
        createAndSendTestEvent1(originProcessIdProcessB);

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompleted(originProcessIdProcessB);

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
        // Start a new process D
        String processTemplateName = "processRelationsD";
        originProcessId = "process-d";
        createProcess(originProcessId, processTemplateName, "D");

        // Start a second process
        String processTemplateNameB = "processRelationsB";
        String originProcessIdProcessB = "process-b";
        createProcess(originProcessIdProcessB, processTemplateNameB, "B");

        // Produce event 6
        createAndSendTestEvent6(originProcessId, originProcessIdProcessB);
        createAndSendTestEvent6(originProcessIdProcessB, originProcessId);

        // Produce event 1
        createAndSendTestEvent1(originProcessId);
        createAndSendTestEvent1(originProcessIdProcessB);

        // Check that process completes after all tasks have been completed
        assertProcessInstanceCompleted(originProcessId);
        assertProcessInstanceCompleted(originProcessIdProcessB);

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
        createProcess(originProcessIdA, processTemplateName, "A");

        // Start a second process
        String processTemplateNameB = "processRelationsB";
        String originProcessIdProcessB = "process-b";
        createProcess(originProcessIdProcessB, processTemplateNameB, "B");

        // Start a third process
        String processTemplateNameE = "processRelationsE";
        String originProcessIdProcessE = "process-e";
        createProcess(originProcessIdProcessE, processTemplateNameE, "E");

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
        assertProcessInstanceCompleted(originProcessIdProcessB);
        assertProcessInstanceCompleted(originProcessIdProcessE);

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
                .until(() -> !getProcessRelations(originProcessId).isEmpty());

        List<ProcessRelationDTO> processRelationDTOList = getProcessRelations(originProcessId);
        ProcessRelationDTO dto = processRelationDTOList.getFirst();
        assertEquals(expectedDto.getProcessName().get("de"), dto.getProcessName().get("de"));
        assertEquals(expectedDto.getProcessState(), dto.getProcessState());
        assertEquals(expectedDto.getRelation(), dto.getRelation());
        assertEquals(expectedDto.getProcessId(), dto.getProcessId());
    }

    protected void assertProcessInstanceHasNoProcessReference(String originProcessId) {
        assertProcessInstanceHasNumberOfProcessReference(originProcessId, 0);
    }

    protected void assertProcessInstanceHasNumberOfProcessReference(String originProcessId, int numberOfProcessReferences) {
        List<ProcessRelationDTO> processRelationDTOList = getProcessRelations(originProcessId);
        assertEquals(numberOfProcessReferences, processRelationDTOList.size());
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

    private static Map<String, String> simulateTranslateService(String value) {
        Map<String, String> labels = new HashMap<>();
        labels.put(Language.DE.name().toLowerCase(), value);
        return labels;
    }

}
