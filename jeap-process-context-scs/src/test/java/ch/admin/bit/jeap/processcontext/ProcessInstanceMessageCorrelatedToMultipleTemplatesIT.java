package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/*_extractor.json")
class ProcessInstanceMessageCorrelatedToMultipleTemplatesIT extends ProcessInstanceMockS3ITBase {

    @Autowired
    private MessageQueryRepository messageQueryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void expectConsumedEventsToBePersistedIrrespectiveOfCorrelationToProcess() {

        // Produce an event which is referenced in at least two templates, with different payload extractors
        // and thus different event data definitions. The event is not correlated to a process, and is expected
        // to be persisted nonetheless.
        Test1Event sentFirstEvent = sendTest1Event();

        Awaitility.await()
                .pollInSameThread()
                .until(() -> messageQueryRepository.findByMessageNameAndIdempotenceId(
                        sentFirstEvent.getType().getName(), sentFirstEvent.getIdentity().getIdempotenceId()).isPresent());

        // Assert that the first event (not correlated to a specific process instance) has been persisted, along with
        // event data from at least two templates
        assertThat(getMessageData(sentFirstEvent, "domainEventsWithPayloadExtractor"))
                .hasSize(1)
                .allMatch(eventData -> eventData.getKey().equals("key1"));
        assertThat(getMessageData(sentFirstEvent, "domainEventsWithDifferentPayloadExtractor"))
                .hasSize(1)
                .allMatch(eventData -> eventData.getKey().equals("differentKey1"));
    }

    private List<MessageData> getMessageData(Test1Event sentFirstEvent, String templateName) {
        return new TransactionTemplate(transactionManager).execute(ignored -> {
            Message persistedEvent = messageQueryRepository.findByMessageNameAndIdempotenceId(
                    sentFirstEvent.getType().getName(), sentFirstEvent.getIdentity().getIdempotenceId()).orElseThrow();
            return persistedEvent.getMessageData(templateName);

        });
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
