package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageQueryRepository;
import ch.admin.bit.jeap.processcontext.event.test1.SubjectReference;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ProcessInstanceMessageCorrelatedToMultipleTemplatesIT extends ProcessInstanceMockS3ITBase {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RelationListenerStub relationListenerStub;

    @Autowired
    private MessageQueryRepository messageQueryRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void expectConsumedEventsToBePersistedIrrespectiveOfCorrelationToProcess() {
        // Start a new process
        String processTemplateName = "relations";
        createProcessInstanceFromTemplate(processTemplateName);
        assertProcessInstanceCreatedEvent(originProcessId, processTemplateName);

        // Produce an event which is referenced in at least two templates, with different payload extractors
        // and thus different event data definitions. The event is not correlated to a process, and is expected
        // to be persisted nonetheless.
        Test1Event sentFirstEvent = sendTest1Event(null);

        // Trigger process completion / wait for it to complete
        sendTest1Event(originProcessId);
        assertProcessInstanceCompleted(originProcessId);

        // Assert that the first event (not correlated to a specific process instance) as been persisted, along with
        // event data from at least two templates
        Optional<Message> persistedEvent = messageQueryRepository.findByMessageNameAndIdempotenceId(
                sentFirstEvent.getType().getName(), sentFirstEvent.getIdentity().getIdempotenceId());
        assertTrue(persistedEvent.isPresent());

        assertThat(persistedEvent.get().getMessageData("domainEventsWithPayloadExtractor"))
                .hasSize(1)
                .allMatch(eventData -> eventData.getKey().equals("key1"));
        assertThat(persistedEvent.get().getMessageData("domainEventsWithDifferentPayloadExtractor"))
                .hasSize(1)
                .allMatch(eventData -> eventData.getKey().equals("differentKey1"));
    }

    private Test1Event sendTest1Event(String originProcessId) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(originProcessId)
                .taskIds("taskId1")
                .build();
        event1.setReferences(Test1EventReferences.newBuilder()
                .setSubjectReference(SubjectReference.newBuilder()
                        .setSubjectId("subjectId123")
                        .build())
                .build());
        sendSync("topic.test1", event1);
        return event1;
    }

    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
