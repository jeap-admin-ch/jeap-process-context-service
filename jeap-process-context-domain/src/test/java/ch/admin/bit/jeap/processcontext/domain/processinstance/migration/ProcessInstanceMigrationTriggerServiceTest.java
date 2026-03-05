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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessInstanceMigrationTriggerServiceTest {

    private static final String CHANGED_TEMPLATE_ORIGIN_PROCESS_ID = "changedTemplateOriginProcessId";

    @Mock
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private ProcessInstanceRepository processInstanceRepository;
    @Mock
    private InternalMessageProducer internalMessageProducer;

    @Test
    void triggerMigrationForModifiedTemplates() {
        ProcessInstanceMigrationTriggerService migrationService =
                new ProcessInstanceMigrationTriggerService(processTemplateRepository, processInstanceRepository, internalMessageProducer);
        ZonedDateTime afterStub = ZonedDateTime.now();
        String idempotenceId = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(afterStub);
        ProcessTemplate template = ProcessInstanceStubs.createSimpleProcessTemplate();
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(template));
        when(processInstanceRepository.findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
                nullable(ZonedDateTime.class), same(template), eq(10000)))
                .thenReturn(List.of(CHANGED_TEMPLATE_ORIGIN_PROCESS_ID));

        int result = migrationService.triggerMigrationForModifiedTemplates(afterStub, 10000);

        assertThat(result).isEqualTo(1);
        verify(internalMessageProducer).produceProcessContextOutdatedMigrationTriggerEvents(
                List.of(CHANGED_TEMPLATE_ORIGIN_PROCESS_ID), idempotenceId);
    }

    @Test
    void triggerMigrationForModifiedTemplates_batchSizeLimitsQuery() {
        ProcessInstanceMigrationTriggerService migrationService =
                new ProcessInstanceMigrationTriggerService(processTemplateRepository, processInstanceRepository, internalMessageProducer);
        ZonedDateTime afterStub = ZonedDateTime.now();
        ProcessTemplate template = ProcessInstanceStubs.createSimpleProcessTemplate();
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(template));

        List<String> fiveIds = IntStream.range(0, 5).mapToObj(i -> "process-" + i).toList();
        when(processInstanceRepository.findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
                nullable(ZonedDateTime.class), same(template), eq(5)))
                .thenReturn(fiveIds);

        int result = migrationService.triggerMigrationForModifiedTemplates(afterStub, 5);

        assertThat(result).isEqualTo(5);
        verify(internalMessageProducer).produceProcessContextOutdatedMigrationTriggerEvents(eq(fiveIds), anyString());
    }

    @Test
    void triggerMigrationForModifiedTemplates_noResults() {
        ProcessInstanceMigrationTriggerService migrationService =
                new ProcessInstanceMigrationTriggerService(processTemplateRepository, processInstanceRepository, internalMessageProducer);
        ZonedDateTime afterStub = ZonedDateTime.now();
        ProcessTemplate template = ProcessInstanceStubs.createSimpleProcessTemplate();
        when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(template));
        when(processInstanceRepository.findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
                nullable(ZonedDateTime.class), same(template), eq(10000)))
                .thenReturn(List.of());

        int result = migrationService.triggerMigrationForModifiedTemplates(afterStub, 10000);

        assertThat(result).isZero();
        verifyNoInteractions(internalMessageProducer);
    }
}
