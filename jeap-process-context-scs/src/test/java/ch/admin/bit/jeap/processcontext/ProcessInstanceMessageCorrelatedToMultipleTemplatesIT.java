package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageQueryRepository;
import ch.admin.bit.jeap.processcontext.event.test1.SubjectReference;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/*_extractor.json")
class ProcessInstanceMessageCorrelatedToMultipleTemplatesIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private MessageQueryRepository messageQueryRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void expectConsumedEventsToBePersistedIrrespectiveOfCorrelationToProcess() {

        // Produce an event which is referenced in at least two templates, with different payload extractors
        // and thus different event data definitions. The event is not correlated to a process, and is expected
        // to be persisted nonetheless.
        Test1Event sentFirstEvent = sendTest1Event();

        Awaitility.await()
                .pollInSameThread()
                .atMost(TIMEOUT)
                .until(() -> messageQueryRepository.findByMessageNameAndIdempotenceId(
                        sentFirstEvent.getType().getName(), sentFirstEvent.getIdentity().getIdempotenceId()).isPresent());

        // Assert that the first event (not correlated to a specific process instance) has been persisted, along with
        // event data from at least two templates
        Message persistedEvent = messageQueryRepository.findByMessageNameAndIdempotenceId(
                sentFirstEvent.getType().getName(), sentFirstEvent.getIdentity().getIdempotenceId()).orElseThrow();

        assertThat(persistedEvent.getMessageData("domainEventsWithPayloadExtractor"))
                .hasSize(1)
                .allMatch(eventData -> eventData.getKey().equals("key1"));
        assertThat(persistedEvent.getMessageData("domainEventsWithDifferentPayloadExtractor"))
                .hasSize(1)
                .allMatch(eventData -> eventData.getKey().equals("differentKey1"));
    }

    private Test1Event sendTest1Event() {
        Test1Event event1 = Test1EventBuilder.createForProcessId(null)
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

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
