package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class ProcessInstanceRepositoryIT {

    @MockitoBean
    @SuppressWarnings("unused")
    private ProcessTemplateRepository processTemplateRepository;
    @Autowired
    private ProcessInstanceJpaRepository processInstanceJpaRepository;

    private ProcessInstanceRepositoryImpl processInstanceRepository;

    @BeforeEach
    void setUp() {
        processInstanceRepository = new ProcessInstanceRepositoryImpl(processInstanceJpaRepository, processTemplateRepository);
    }

    @Test
    void shouldLenientlySetNewTemplate() {
        // given
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(processInstance.getProcessTemplateName())
                .templateHash("ptHash")
                .taskTypes(List.of(processInstance.getTasks().get(0).requireTaskType()))
                .build();
        doReturn(Optional.of(processTemplate)).when(processTemplateRepository).findByName(processInstance.getProcessTemplateName());
        processInstanceJpaRepository.saveAndFlush(processInstance);
        Optional<ProcessInstance> processInstanceReadOptional = processInstanceRepository.findByOriginProcessIdWithoutLoadingMessages(processInstance.getOriginProcessId());
        assertTrue(processInstanceReadOptional.isPresent());

        // when
        Optional<ProcessInstance> processInstanceSecondRead = processInstanceRepository.findByOriginProcessIdWithoutLoadingMessages(processInstance.getOriginProcessId());

        // then
        assertThat(processInstanceSecondRead).isPresent();
    }
}
