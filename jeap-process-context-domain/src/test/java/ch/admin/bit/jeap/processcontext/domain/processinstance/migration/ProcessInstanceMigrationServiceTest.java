package ch.admin.bit.jeap.processcontext.domain.processinstance.migration;

import ch.admin.bit.jeap.processcontext.domain.port.InternalMessageProducer;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceMigrationServiceTest {

    private static final String CHANGED_TEMPLATE_ORIGIN_PROCESS_ID = "changedTemplateOriginProcessId";

    @Mock
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private InternalMessageProducer internalMessageProducer;

    @Test
    void triggerMigrationForModifiedTemplates() {
        ProcessInstanceMigrationService migrationService =
                new ProcessInstanceMigrationService(processTemplateRepository, processInstanceRepository, internalMessageProducer);
        ZonedDateTime afterStub = ZonedDateTime.now();
        ProcessTemplate template = ProcessInstanceStubs.createSimpleProcessTemplate();
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(template));
        when(processInstanceRepository.findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
                nullable(ZonedDateTime.class), same(template), eq(Pageable.ofSize(10))))
                .thenReturn(new PageImpl<>(List.of(CHANGED_TEMPLATE_ORIGIN_PROCESS_ID)));

        migrationService.triggerMigrationForModifiedTemplates(afterStub);

        verify(internalMessageProducer).produceProcessContextOutdatedEventSynchronously(CHANGED_TEMPLATE_ORIGIN_PROCESS_ID);
        verifyNoMoreInteractions(internalMessageProducer);
    }
}