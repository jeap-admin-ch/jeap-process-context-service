package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessTemplateStubs;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import com.fasterxml.uuid.Generators;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
class ProcessInstanceRepositoryImplTest {

    @Mock
    @SuppressWarnings("unused")
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private ProcessInstance processInstance;
    @Mock
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    private ProcessInstanceRepositoryImpl processInstanceRepository;

    @Test
    void findByOriginProcessId_withEventReferences() {
        String originProcessId = "originProcessId";
        String templateName = "templateName";
        UUID processInstanceId = Generators.timeBasedEpochGenerator().generate();
        ProcessTemplate processTemplate = ProcessTemplateStubs.createSingleTaskProcessTemplate("taskName");
        doReturn(Optional.of(processInstance)).when(processInstanceJpaRepository).findByOriginProcessId(originProcessId);
        doReturn(templateName).when(processInstance).getProcessTemplateName();
        doReturn(processInstanceId).when(processInstance).getId();
        doReturn(Optional.of(processTemplate)).when(processTemplateRepository).findByName(templateName);

        Optional<ProcessInstance> processInstanceOptional = processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId);

        assertTrue(processInstanceOptional.isPresent());
        ProcessInstance processInstance = processInstanceOptional.get();
        verify(processInstance).setProcessTemplate(same(processTemplate));
        verify(processInstance).setMessageReferenceMessageDTOS(any());
    }

    @Test
    void findByOriginProcessId_withoutEventReferences() {
        String originProcessId = "originProcessId";
        String templateName = "templateName";
        ProcessTemplate processTemplate = ProcessTemplateStubs.createSingleTaskProcessTemplate("taskName");
        doReturn(Optional.of(processInstance)).when(processInstanceJpaRepository).findByOriginProcessId(originProcessId);
        doReturn(templateName).when(processInstance).getProcessTemplateName();
        doReturn(Optional.of(processTemplate)).when(processTemplateRepository).findByName(templateName);

        Optional<ProcessInstance> processInstanceOptional = processInstanceRepository.findByOriginProcessIdWithoutLoadingMessages(originProcessId);

        assertTrue(processInstanceOptional.isPresent());
        ProcessInstance processInstance = processInstanceOptional.get();
        verify(processInstance, never()).setMessageReferenceMessageDTOS(any());
    }

    @Test
    void findByOriginProcessId_whenTemplateIsNotFound_thenShouldThrowException() {
        String originProcessId = "originProcessId";
        String templateName = "templateName";
        doReturn(Optional.of(processInstance)).when(processInstanceJpaRepository).findByOriginProcessId(originProcessId);
        doReturn(templateName).when(processInstance).getProcessTemplateName();
        doReturn(Optional.empty()).when(processTemplateRepository).findByName(templateName);

        assertThrows(ProcessTemplateNotFoundException.class, () -> processInstanceRepository.findByOriginProcessIdLoadingMessages(originProcessId));
    }

    @Test
    void findByAll() {
        PageRequest pageRequest = PageRequest.of(0, 10);

        String templateName = "templateName";
        ProcessTemplate processTemplate = ProcessTemplateStubs.createSingleTaskProcessTemplate("taskName");
        doReturn( new PageImpl<>(List.of(processInstance))).when(processInstanceJpaRepository).findAll(pageRequest);
        doReturn(templateName).when(processInstance).getProcessTemplateName();
        doReturn(Optional.of(processTemplate)).when(processTemplateRepository).findByName(templateName);

        Page<ProcessInstance> processInstancePage = processInstanceRepository.findAll(PageRequest.of(0, 10));

        assertEquals(1, processInstancePage.getTotalElements());
    }

    @BeforeEach
    void setUp() {
        processInstanceRepository = new ProcessInstanceRepositoryImpl(processInstanceJpaRepository, processTemplateRepository);
    }

}
