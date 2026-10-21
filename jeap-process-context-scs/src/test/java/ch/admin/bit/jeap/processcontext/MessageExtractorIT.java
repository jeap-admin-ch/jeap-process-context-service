package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.messaging.model.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageQueryRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.event.test1.SubjectReference;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test1.Test1EventReferences;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test3EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/message_extraction.json")
class MessageExtractorIT extends ProcessInstanceMockS3ITBase {

    private static final String TEMPLATE_NAME = "messageExtraction";

    @Autowired
    private ProcessContextFactory processContextFactory;
    @Autowired
    private ProcessInstanceRepository processInstanceRepository;
    @Autowired
    private MessageQueryRepository messageQueryRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void messageExtractor_combinesMetadataWithPayloadAndReferenceData() {
        Test1Event event = test1Event(originProcessId);

        sendSync("topic.test1", event);
        assertProcessInstanceCreated(originProcessId, TEMPLATE_NAME);

        assertMessageDataInProcessContext(event,
                new MessageData("messageId", event.getIdentity().getId()),
                new MessageData("messageType", "Test1Event"),
                new MessageData("key1", "taskId"),
                new MessageData("someField", "payloadValue"),
                new MessageData("theSubject", "subjectId", "subjectVersion"));
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void messageExtractor_worksWithoutPayloadOrReferenceExtractors() {
        var event = Test3EventBuilder.createForProcessId(originProcessId).build();

        sendSync("topic.test3", event);
        assertProcessInstanceCreated(originProcessId, TEMPLATE_NAME);

        assertMessageDataInProcessContext(event,
                new MessageData("messageId", event.getIdentity().getId()),
                new MessageData("messageType", "Test3Event"));
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void omittedMessageExtractor_preservesPayloadExtraction() {
        var event = Test2EventBuilder.createForProcessId(originProcessId)
                .objectId("objectId")
                .build();

        sendSync("topic.test2", event);
        assertProcessInstanceCreated(originProcessId, TEMPLATE_NAME);

        assertMessageDataInProcessContext(event,
                new MessageData("theObject", "objectId", event.getPayload().getVersion()));
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void extractedMetadata_isAvailableToProcessCompletionCondition() {
        var creatingEvent = Test2EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test2", creatingEvent);
        assertProcessInstanceCreated(originProcessId, TEMPLATE_NAME);
        assertThat(processInstanceController.getProcessInstanceByOriginProcessId(originProcessId).getState())
                .isEqualTo(ProcessState.STARTED.name());

        var completingEvent = Test3EventBuilder.createForProcessId(originProcessId).build();
        sendSync("topic.test3", completingEvent);

        assertProcessInstanceCompleted(originProcessId);
    }

    @Test
    void messageExtractor_persistsDataWithoutCorrelationToAProcess() {
        Test1Event event = test1Event(null);

        sendSync("topic.test1", event);

        Awaitility.await().pollInSameThread().untilAsserted(() ->
                new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
                    var persistedMessage = messageQueryRepository.findByMessageNameAndIdempotenceId(
                            event.getType().getName(), event.getIdentity().getIdempotenceId());
                    assertThat(persistedMessage).isPresent();
                    assertThat(persistedMessage.orElseThrow().getMessageData(TEMPLATE_NAME))
                            .map(data -> new MessageData(data.getKey(), data.getValue(), data.getRole()))
                            .containsExactlyInAnyOrder(
                                    new MessageData("messageId", event.getIdentity().getId()),
                                    new MessageData("messageType", "Test1Event"),
                                    new MessageData("key1", "taskId"),
                                    new MessageData("someField", "payloadValue"),
                                    new MessageData("theSubject", "subjectId", "subjectVersion"));
                }));
    }

    private Test1Event test1Event(String processId) {
        Test1Event event = Test1EventBuilder.createForProcessId(processId)
                .taskIds("taskId")
                .someField("payloadValue")
                .build();
        event.setReferences(Test1EventReferences.newBuilder()
                .setSubjectReference(SubjectReference.newBuilder()
                        .setSubjectId("subjectId")
                        .setVersion("subjectVersion")
                        .build())
                .build());
        return event;
    }

    private void assertMessageDataInProcessContext(Message event, MessageData... expected) {
        Awaitility.await().pollInSameThread().untilAsserted(() -> {
            var processInstance = processInstanceRepository.findByOriginProcessId(originProcessId).orElseThrow();
            var processContext = processContextFactory.createProcessContext(processInstance);
            assertThat(processContext.getMessageDataForMessageType(event.getType().getName()))
                    .containsExactlyInAnyOrder(expected);
        });
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
