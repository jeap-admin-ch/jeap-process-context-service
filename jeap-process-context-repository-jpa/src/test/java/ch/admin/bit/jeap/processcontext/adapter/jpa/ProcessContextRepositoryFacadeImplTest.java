package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.plugin.api.message.MessageData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
@Import(ProcessContextRepositoryFacadeImpl.class)
class ProcessContextRepositoryFacadeImplTest {

    @Autowired
    private ProcessContextRepositoryFacadeImpl facade;

    @Autowired
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    @Autowired
    private MessageRepository messageRepository;

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;

    @MockitoBean
    private ProcessContextFactory processContextFactory;

    @Test
    void containsMessageOfType_messageExists_expectTrue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageOfType(savedProcessInstance.getId(), "sourceEventName");

        assertThat(result).isTrue();
    }

    @Test
    void containsMessageOfType_messageNotExists_expectFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageOfType(savedProcessInstance.getId(), "nonExistentType");

        assertThat(result).isFalse();
    }

    @Test
    void containsMessageOfAnyType_oneTypeMatches_expectTrue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageOfAnyType(savedProcessInstance.getId(),
                Set.of("sourceEventName", "nonExistentType"));

        assertThat(result).isTrue();
    }

    @Test
    void containsMessageOfAnyType_emptySet_expectFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageOfAnyType(savedProcessInstance.getId(), Set.of());

        assertThat(result).isFalse();
    }

    @Test
    void getMessageDataForMessageType_messageExists_expectMessageData() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        Set<MessageData> result = facade.getMessageDataForMessageType(savedProcessInstance.getId(), "sourceEventName");

        assertThat(result).hasSize(2);
        Set<String> keys = result.stream().map(MessageData::getKey).collect(java.util.stream.Collectors.toSet());
        assertThat(keys).containsOnly("sourceEventDataKey");
    }

    @Test
    void getMessageDataForMessageType_messageNotExists_expectEmpty() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        Set<MessageData> result = facade.getMessageDataForMessageType(savedProcessInstance.getId(), "nonExistentType");

        assertThat(result).isEmpty();
    }

    @Test
    void countMessagesByType_multipleTypes_expectCorrectCounts() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        Map<String, Long> result = facade.countMessagesByTypes(savedProcessInstance.getId(),
                Set.of("sourceEventName", "anotherSourceEventName", "nonExistent"));

        assertThat(result)
                .containsEntry("sourceEventName", 1L)
                .containsEntry("anotherSourceEventName", 1L)
                .containsEntry("nonExistent", 0L);
    }

    @Test
    void countMessagesByType_emptySet_expectEmptyMap() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        Map<String, Long> result = facade.countMessagesByTypes(savedProcessInstance.getId(), Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    void countMessagesByTypeWithMessageData_matchingData_expectCount() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        long result = facade.countMessagesByTypeWithMessageData(savedProcessInstance.getId(),
                "sourceEventName", "sourceEventDataKey", "someValue");

        assertThat(result).isEqualTo(1);
    }

    @Test
    void countMessagesByTypeWithMessageData_noMatchingData_expectZero() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        long result = facade.countMessagesByTypeWithMessageData(savedProcessInstance.getId(),
                "sourceEventName", "sourceEventDataKey", "nonExistentValue");

        assertThat(result).isZero();
    }

    @Test
    void countMessagesByTypeWithAnyMessageData_matchingData_expectCount() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        long result = facade.countMessagesByTypeWithAnyMessageData(savedProcessInstance.getId(),
                "sourceEventName", Map.of("sourceEventDataKey", "someValue"));

        assertThat(result).isEqualTo(1);
    }

    @Test
    void countMessagesByTypeWithAnyMessageData_noMatchingData_expectZero() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        long result = facade.countMessagesByTypeWithAnyMessageData(savedProcessInstance.getId(),
                "sourceEventName", Map.of("sourceEventDataKey", "nonExistentValue"));

        assertThat(result).isZero();
    }

    @Test
    void countMessagesByTypeWithAnyMessageData_emptyFilter_expectZero() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        long result = facade.countMessagesByTypeWithAnyMessageData(savedProcessInstance.getId(),
                "sourceEventName", Map.of());

        assertThat(result).isZero();
    }

    @Test
    void containsMessageByTypeWithMessageData_matchingValue_expectTrue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageByTypeWithMessageData(savedProcessInstance.getId(),
                "sourceEventName", "sourceEventDataKey", "someValue");

        assertThat(result).isTrue();
    }

    @Test
    void containsMessageByTypeWithMessageData_noMatchingValue_expectFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageByTypeWithMessageData(savedProcessInstance.getId(),
                "sourceEventName", "sourceEventDataKey", "nonExistent");

        assertThat(result).isFalse();
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataValue_matchingValue_expectTrue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageByTypeWithAnyMessageDataValue(savedProcessInstance.getId(),
                "sourceEventName", "sourceEventDataKey", Set.of("someValue", "otherValue"));

        assertThat(result).isTrue();
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataValue_noMatchingValue_expectFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageByTypeWithAnyMessageDataValue(savedProcessInstance.getId(),
                "sourceEventName", "sourceEventDataKey", Set.of("nonExistent1", "nonExistent2"));

        assertThat(result).isFalse();
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataValue_emptyFilter_expectFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageByTypeWithAnyMessageDataValue(savedProcessInstance.getId(),
                "sourceEventName", "sourceEventDataKey", Set.of());

        assertThat(result).isFalse();
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataKeyValue_matchingValue_expectTrue() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageByTypeWithAnyMessageDataKeyValue(savedProcessInstance.getId(),
                "sourceEventName", Map.of("sourceEventDataKey", Set.of("someValue", "otherValue")));

        assertThat(result).isTrue();
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataKeyValue_noMatchingValue_expectFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageByTypeWithAnyMessageDataKeyValue(savedProcessInstance.getId(),
                "sourceEventName", Map.of("sourceEventDataKey", Set.of("nonExistent1", "nonExistent2")));

        assertThat(result).isFalse();
    }

    @Test
    void containsMessageByTypeWithAnyMessageDataKeyValue_emptyFilter_expectFalse() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithEventDataProcessData(messageRepository);
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        boolean result = facade.containsMessageByTypeWithAnyMessageDataKeyValue(savedProcessInstance.getId(),
                "sourceEventName", Map.of());

        assertThat(result).isFalse();
    }
}
