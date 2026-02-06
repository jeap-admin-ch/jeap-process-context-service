package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.message.MessageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessDataServiceTest {

    @Mock
    private ProcessDataRepository processDataRepository;

    private ProcessDataService processDataService;

    @BeforeEach
    void setUp() {
        processDataService = new ProcessDataService(processDataRepository);
    }

    @Test
    void copyMessageDataToProcessData_noTemplateDefined() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();

        MessageData messageData = MessageData.builder()
                .templateName(processInstance.getProcessTemplateName())
                .key("sourceEventDatakey")
                .value("someValue")
                .role("someRole")
                .build();
        Message domainMessage = Message.messageBuilder()
                .messageName("sourceEventName")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .originTaskIds(Set.of())
                .messageData(Set.of(messageData))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        List<ProcessData> result = processDataService.copyMessageDataToProcessData(processInstance, domainMessage);

        assertThat(result).isEmpty();
        verify(processDataRepository, never()).saveIfNew(any());
    }

    @Test
    void copyMessageDataToProcessData_noEventNameMatches() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventData();

        MessageData messageData = MessageData.builder()
                .templateName(processInstance.getProcessTemplateName())
                .key("sourceEventDatakey")
                .value("someValue")
                .role("someRole")
                .build();
        Message domainMessage = Message.messageBuilder()
                .messageName("nonMatchingEventName")
                .messageId("eventId")
                .idempotenceId("idempotenceId")
                .messageData(Set.of(messageData))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        List<ProcessData> result = processDataService.copyMessageDataToProcessData(processInstance, domainMessage);

        assertThat(result).isEmpty();
        verify(processDataRepository, never()).saveIfNew(any());
    }

    @Test
    void copyMessageDataToProcessData() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventData();
        String templateName = processInstance.getProcessTemplateName();
        // Simulate DB unique constraint: first save per (key, value, role) succeeds, duplicates return null
        Set<ProcessData> saved = new HashSet<>();
        when(processDataRepository.saveIfNew(any())).thenAnswer(invocation -> {
            ProcessData pd = invocation.getArgument(0);
            return saved.add(pd);
        });

        MessageData messageData1 = new MessageData(templateName, "sourceEventDataKey", "someValue", "someRole");
        MessageData messageData2 = new MessageData(templateName, "sourceEventDataKey", "someValueOtherValue", "someOtherRole");
        Message domainMessage1 = Message.messageBuilder()
                .messageName("sourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData1, messageData2))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        List<ProcessData> result1 = processDataService.copyMessageDataToProcessData(processInstance, domainMessage1);

        assertThat(result1)
                .hasSize(2)
                .contains(
                        new ProcessData("targetKeyName", "someValue", "someRole"),
                        new ProcessData("targetKeyName", "someValueOtherValue", "someOtherRole"));

        // Second call with same data - saveIfNew returns null for already existing data
        List<ProcessData> result2 = processDataService.copyMessageDataToProcessData(processInstance, domainMessage1);

        assertThat(result2).isEmpty();
    }

    @Test
    void copyMessageDataToProcessData_newDataForDifferentSourceEvent() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventData();
        String templateName = processInstance.getProcessTemplateName();
        when(processDataRepository.saveIfNew(any())).thenReturn(true);

        MessageData messageData = new MessageData(templateName, "anotherSourceEventDataKey", "anotherValue");
        Message domainMessage = Message.messageBuilder()
                .messageName("anotherSourceEventName")
                .idempotenceId("idempotenceId")
                .messageId("eventId")
                .messageData(Set.of(messageData))
                .createdAt(ZonedDateTime.now())
                .messageCreatedAt(ZonedDateTime.now())
                .build();

        List<ProcessData> result = processDataService.copyMessageDataToProcessData(processInstance, domainMessage);

        assertThat(result)
                .hasSize(1)
                .contains(new ProcessData("anotherTargetKeyName", "anotherValue"));
    }
}
