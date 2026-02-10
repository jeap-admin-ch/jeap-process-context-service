package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReference;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessTemplateStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
class ProcessInstanceRepositoryImplTest {

    @Mock
    @SuppressWarnings("unused")
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private ProcessInstance processInstance;
    @Mock
    private ProcessContextFactory processContextFactory;
    @Mock
    private ProcessInstanceJpaRepository processInstanceJpaRepository;
    @Mock
    private MessageReference messageReference;

    private ProcessInstanceRepositoryImpl processInstanceRepository;

    @Test
    void findByOriginProcessId_withoutEventReferences() {
        String originProcessId = "originProcessId";
        String templateName = "templateName";
        ProcessTemplate processTemplate = ProcessTemplateStubs.createSingleTaskProcessTemplate("taskName");
        doReturn(Optional.of(processInstance)).when(processInstanceJpaRepository).findByOriginProcessId(originProcessId);
        doReturn(templateName).when(processInstance).getProcessTemplateName();
        doReturn(Optional.of(processTemplate)).when(processTemplateRepository).findByName(templateName);

        Optional<ProcessInstance> processInstanceOptional = processInstanceRepository.findByOriginProcessId(originProcessId);

        assertTrue(processInstanceOptional.isPresent());
    }

    @Test
    void findByOriginProcessId_whenTemplateIsNotFound_thenShouldThrowException() {
        String originProcessId = "originProcessId";
        String templateName = "templateName";
        doReturn(Optional.of(processInstance)).when(processInstanceJpaRepository).findByOriginProcessId(originProcessId);
        doReturn(templateName).when(processInstance).getProcessTemplateName();
        doReturn(Optional.empty()).when(processTemplateRepository).findByName(templateName);

        assertThrows(ProcessTemplateNotFoundException.class, () -> processInstanceRepository.findByOriginProcessId(originProcessId));
    }

    @Test
    void findByAll() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        String templateName = "templateName";
        ProcessTemplate processTemplate = ProcessTemplateStubs.createSingleTaskProcessTemplate("taskName");
        doReturn(new PageImpl<>(List.of(processInstance))).when(processInstanceJpaRepository).findAll(pageRequest);
        doReturn(templateName).when(processInstance).getProcessTemplateName();
        doReturn(Optional.of(processTemplate)).when(processTemplateRepository).findByName(templateName);

        Page<ProcessInstance> processInstancePage = processInstanceRepository.findAll(PageRequest.of(0, 10));

        assertEquals(1, processInstancePage.getTotalElements());
    }

    @Test
    void findLatestMessageReferenceByMessageType_whenFound_returnsFirst() {
        UUID processInstanceId = UUID.randomUUID();
        doReturn(processInstanceId).when(processInstance).getId();
        doReturn(List.of(messageReference)).when(processInstanceJpaRepository)
                .findMessageReferencesByMessageType(processInstanceId, "TestType", PageRequest.of(0, 1));

        Optional<MessageReference> result = processInstanceRepository.findLatestMessageReferenceByMessageType(processInstance, "TestType");

        assertTrue(result.isPresent());
        assertEquals(messageReference, result.get());
    }

    @Test
    void findLatestMessageReferenceByMessageType_whenNotFound_returnsEmpty() {
        UUID processInstanceId = UUID.randomUUID();
        doReturn(processInstanceId).when(processInstance).getId();
        doReturn(List.of()).when(processInstanceJpaRepository)
                .findMessageReferencesByMessageType(processInstanceId, "TestType", PageRequest.of(0, 1));

        Optional<MessageReference> result = processInstanceRepository.findLatestMessageReferenceByMessageType(processInstance, "TestType");

        assertTrue(result.isEmpty());
    }

    @Test
    void findLatestMessageReferenceByMessageTypeAndOriginTaskId_whenFound_returnsFirst() {
        UUID processInstanceId = UUID.randomUUID();
        doReturn(processInstanceId).when(processInstance).getId();
        doReturn(List.of(messageReference)).when(processInstanceJpaRepository)
                .findMessageReferencesByMessageTypeAndOriginTaskId(processInstanceId, "TestType", "task-1", PageRequest.of(0, 1));

        Optional<MessageReference> result = processInstanceRepository.findLatestMessageReferenceByMessageTypeAndOriginTaskId(processInstance, "TestType", "task-1");

        assertTrue(result.isPresent());
        assertEquals(messageReference, result.get());
    }

    @Test
    void findLatestMessageReferenceByMessageTypeAndOriginTaskId_whenNotFound_returnsEmpty() {
        UUID processInstanceId = UUID.randomUUID();
        doReturn(processInstanceId).when(processInstance).getId();
        doReturn(List.of()).when(processInstanceJpaRepository)
                .findMessageReferencesByMessageTypeAndOriginTaskId(processInstanceId, "TestType", "task-1", PageRequest.of(0, 1));

        Optional<MessageReference> result = processInstanceRepository.findLatestMessageReferenceByMessageTypeAndOriginTaskId(processInstance, "TestType", "task-1");

        assertTrue(result.isEmpty());
    }

    @BeforeEach
    void setUp() {
        processInstanceRepository = new ProcessInstanceRepositoryImpl(processInstanceJpaRepository, processTemplateRepository, processContextFactory);
    }
}
