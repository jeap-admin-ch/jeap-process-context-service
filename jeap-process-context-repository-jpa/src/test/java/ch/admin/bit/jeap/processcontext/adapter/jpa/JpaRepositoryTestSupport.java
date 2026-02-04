package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.message.Message;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
class JpaRepositoryTestSupport {

    private final MessageReferenceJpaRepository messageReferenceJpaRepository;

    /**
     * Creates a process instance with messages and persists the MessageReference entities.
     */
    static ProcessInstance createProcessInstanceWithPersistedMessageReferences(ProcessInstance processInstance, ProcessInstanceJpaRepository processInstanceJpaRepository, MessageJpaRepository messageJpaRepository, MessageReferenceJpaRepository messageReferenceJpaRepository) {
        ProcessInstance savedProcessInstance = processInstanceJpaRepository.saveAndFlush(processInstance);

        // Get the messages that were saved by the stub and create MessageReferences for them
        List<Message> messages = messageJpaRepository.findAll();
        JpaRepositoryTestSupport jpaRepositoryTestSupport = new JpaRepositoryTestSupport(messageReferenceJpaRepository);
        for (Message message : messages) {
            jpaRepositoryTestSupport.createAndPersist(message, savedProcessInstance);
        }
        messageReferenceJpaRepository.flush();

        return savedProcessInstance;
    }

    /**
     * Creates a MessageReference for the given message and process instance, and persists it.
     */
    MessageReference createAndPersist(Message message, ProcessInstance processInstance) {
        MessageReference messageReference = MessageReference.from(message, processInstance);
        return messageReferenceJpaRepository.save(messageReference);
    }
}
