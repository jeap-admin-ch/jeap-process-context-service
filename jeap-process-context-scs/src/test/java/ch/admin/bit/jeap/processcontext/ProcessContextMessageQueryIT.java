package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.event.test1.Test1Event;
import ch.admin.bit.jeap.processcontext.event.test2.Test2Event;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import ch.admin.bit.jeap.processcontext.testevent.Test1EventBuilder;
import ch.admin.bit.jeap.processcontext.testevent.Test2EventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties =
        "jeap.processcontext.template.classpath-location-pattern=classpath:/process/templates/message_queries.json")
class ProcessContextMessageQueryIT extends ProcessInstanceMockS3ITBase {

    private static final String TEST1_EVENT = Test1Event.class.getSimpleName();
    private static final String TEST2_EVENT = Test2Event.class.getSimpleName();
    static final String KEY_1 = "key1";
    static final String TASK_ID_1 = "taskId1";
    static final String SOME_FIELD = "someField";
    static final String SOME_VALUE = "someValue";
    static final String FOO = "Foo";

    @Autowired
    private ProcessContextFactory processContextFactory;
    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void expectMessageToBeFound() {
        // Instantiate another process instance to verify that messages from this instance are not matched
        String differentInstanceOriginProcessId = UUID.randomUUID().toString();
        sendTest1Event(differentInstanceOriginProcessId);
        instantiateProcess(differentInstanceOriginProcessId);
        assertProcessInstanceCreated(differentInstanceOriginProcessId, "message_queries");

        // Now instantiate the actual process instance to test message queries on
        sendTest1Event(originProcessId);
        instantiateProcess(originProcessId);
        assertProcessInstanceCreated(originProcessId, "message_queries");
        var processInstance = processInstanceRepository.findByOriginProcessIdWithoutLoadingMessages(originProcessId).orElseThrow();

        var processContext = processContextFactory.createProcessContext(processInstance);

        assertThat(processContext.containsMessageOfType(TEST1_EVENT)).isTrue();
        assertThat(processContext.containsMessageOfType(FOO)).isFalse();
        assertThat(processContext.containsMessageOfAnyType(Set.of(TEST1_EVENT))).isTrue();
        assertThat(processContext.containsMessageOfAnyType(Set.of(FOO))).isFalse();

        // See Test1EventPayloadExtractor
        assertThat(processContext.getMessageDataForMessageType(TEST1_EVENT))
                .containsOnly(
                        new MessageData(KEY_1, TASK_ID_1),
                        new MessageData(SOME_FIELD, SOME_VALUE));
        assertThat(processContext.getMessageDataForMessageType(TEST1_EVENT))
                .containsOnly(
                        new MessageData(KEY_1, TASK_ID_1),
                        new MessageData(SOME_FIELD, SOME_VALUE));
        assertThat(processContext.countMessagesByTypes(Set.of(TEST1_EVENT, FOO)))
                .containsAllEntriesOf(Map.of(FOO, 0L, TEST1_EVENT, 1L));
        assertThat(processContext.countMessagesByTypeWithMessageData(TEST1_EVENT, SOME_FIELD, SOME_VALUE))
                .isOne();
        assertThat(processContext.countMessagesByTypeWithMessageData(TEST2_EVENT, SOME_FIELD, SOME_VALUE))
                .isZero();
        assertThat(processContext.countMessagesByTypeWithMessageData(TEST1_EVENT, SOME_FIELD, "missingValue"))
                .isZero();
        assertThat(processContext.countMessagesByTypeWithAnyMessageData(TEST1_EVENT, Map.of(SOME_FIELD, SOME_VALUE, "foo", "firstMatchCounts")))
                .isOne();
        assertThat(processContext.countMessagesByTypeWithAnyMessageData(TEST1_EVENT, Map.of("foo", "noMatch")))
                .isZero();
        assertThat(processContext.containsMessageByTypeWithMessageData(TEST1_EVENT, SOME_FIELD, SOME_VALUE))
                .isTrue();
        assertThat(processContext.containsMessageByTypeWithMessageData(FOO, SOME_FIELD, SOME_VALUE))
                .isFalse();
        assertThat(processContext.containsMessageByTypeWithAnyMessageDataValue(TEST1_EVENT, SOME_FIELD, Set.of(SOME_VALUE, "otherValue")))
                .isTrue();
        assertThat(processContext.containsMessageByTypeWithAnyMessageDataValue(TEST1_EVENT, SOME_FIELD, Set.of("missingValue")))
                .isFalse();
        assertThat(processContext.containsMessageByTypeWithAnyMessageDataValue(FOO, SOME_FIELD, Set.of("missingValue")))
                .isFalse();
        assertThat(processContext.containsMessageByTypeWithAnyMessageDataKeyValue(TEST1_EVENT, Map.of(SOME_FIELD, Set.of(SOME_VALUE), "otherField", Set.of("otherValue"))))
                .isTrue();
        assertThat(processContext.containsMessageByTypeWithAnyMessageDataKeyValue(FOO, Map.of(SOME_FIELD, Set.of(SOME_VALUE))))
                .isFalse();

        assertProcessInstanceCompleted(originProcessId);
        assertThat(processContext.areAllTasksInFinalState())
                .isTrue();
    }

    private void sendTest1Event(String correlateToOriginProcessId) {
        Test1Event event1 = Test1EventBuilder.createForProcessId(correlateToOriginProcessId)
                .taskIds(TASK_ID_1)
                .someField(SOME_VALUE)
                .build();
        sendSync("topic.test1", event1);
    }

    private void instantiateProcess(String instanceOriginProcessId) {
        Test2Event event2 = Test2EventBuilder.createForProcessId(instanceOriginProcessId)
                .objectId("test")
                .build();
        sendSync("topic.test2", event2);
        assertProcessInstanceCreated(instanceOriginProcessId, "message_queries");
    }

    @Override
    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }
}
