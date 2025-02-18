package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.Message;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MessageProcessCompletionConditionTest {

    private static final String COMPLETING_DOMAIN_EVENT_NAME = "CompletingDomainEvent";
    private static final String OTHER_DOMAIN_EVENT_NAME_1 = "OtherDomainEvent1";
    private static final String OTHER_DOMAIN_EVENT_NAME_2 = "OtherDomainEvent2";
    private static final ProcessCompletionConclusion COMPLETING_CONCLUSION = ProcessCompletionConclusion.SUCCEEDED;
    private static final String COMPLETING_NAME = "Completed because of som reason";

    @Test
    void testIsCompleted_whenCompletingDomainEventInProcessContext_thenCompleted()  {
        ProcessContext processContext = createProcessContextWithEvents(List.of(OTHER_DOMAIN_EVENT_NAME_1, COMPLETING_DOMAIN_EVENT_NAME, OTHER_DOMAIN_EVENT_NAME_2));
        MessageProcessCompletionCondition condition =
                new MessageProcessCompletionCondition(COMPLETING_DOMAIN_EVENT_NAME, COMPLETING_CONCLUSION, COMPLETING_NAME);

        ProcessCompletionConditionResult result = condition.isProcessCompleted(processContext);

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getConclusion()).isPresent();
        assertThat(result.getConclusion()).contains(COMPLETING_CONCLUSION);
        assertThat(result.getName()).isPresent();
        assertThat(result.getName()).contains(COMPLETING_NAME);
    }

    @Test
    void testIsCompleted_whenCompletingDomainEventNotInProcessContext_thenUncompleted()  {
        ProcessContext processContext = createProcessContextWithEvents(List.of(OTHER_DOMAIN_EVENT_NAME_1, OTHER_DOMAIN_EVENT_NAME_2));
        MessageProcessCompletionCondition condition =
                new MessageProcessCompletionCondition(COMPLETING_DOMAIN_EVENT_NAME, COMPLETING_CONCLUSION,  COMPLETING_NAME);

        ProcessCompletionConditionResult result = condition.isProcessCompleted(processContext);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getConclusion()).isEmpty();
        assertThat(result.getName()).isEmpty();
    }

    @Test
    void testIsCompleted_whenNoDomainEventInProcessContext_thenUncompleted()  {
        ProcessContext processContext = createProcessContextWithEvents(List.of());
        MessageProcessCompletionCondition condition =
                new MessageProcessCompletionCondition(COMPLETING_DOMAIN_EVENT_NAME, COMPLETING_CONCLUSION, null);

        ProcessCompletionConditionResult result = condition.isProcessCompleted(processContext);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getConclusion()).isEmpty();
        assertThat(result.getName()).isEmpty();
    }

    private ProcessContext createProcessContextWithEvents(List<String> messageNames) {
        List<Message> messages = messageNames.stream().map(name -> Message.builder().name(name).build()).toList();
        return ProcessContext.builder()
                    .originProcessId("id")
                    .processName("name")
                    .processState(ProcessState.STARTED)
                .messages(messages)
                    .tasks(List.of())
                    .build();
    }

}
