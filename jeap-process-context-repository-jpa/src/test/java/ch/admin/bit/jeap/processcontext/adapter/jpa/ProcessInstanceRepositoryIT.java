package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
    @MockitoBean
    private ProcessContextFactory processContextFactory;

    private ProcessInstanceRepositoryImpl processInstanceRepository;

    @BeforeEach
    void setUp() {
        processInstanceRepository = new ProcessInstanceRepositoryImpl(processInstanceJpaRepository, processTemplateRepository, processContextFactory);
    }

    @Test
    void shouldLenientlySetNewTemplate() {
        // given
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleTaskInstance();
        ProcessTemplate processTemplate = ProcessTemplate.builder()
                .name(processInstance.getProcessTemplateName())
                .templateHash("ptHash")
                .taskTypes(processInstance.getProcessTemplate().getTaskTypes())
                .build();
        doReturn(Optional.of(processTemplate)).when(processTemplateRepository).findByName(processInstance.getProcessTemplateName());
        processInstanceJpaRepository.saveAndFlush(processInstance);
        Optional<ProcessInstance> processInstanceReadOptional = processInstanceRepository.findByOriginProcessId(processInstance.getOriginProcessId());
        assertTrue(processInstanceReadOptional.isPresent());

        // when
        Optional<ProcessInstance> processInstanceSecondRead = processInstanceRepository.findByOriginProcessId(processInstance.getOriginProcessId());

        // then
        assertThat(processInstanceSecondRead).isPresent();
    }
}
