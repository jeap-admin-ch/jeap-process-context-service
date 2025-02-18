package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import ch.admin.bit.jeap.processcontext.plugin.api.condition.ProcessSnapshotCondition;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessTemplateRepositoryTest {


    @Test
    void testHasProcessSnapshotsConfigured_WhenSnapshotConditionsConfigured_ThenReturnTrue() {
        final ProcessTemplate templateWithSnapshotCondition = createProcessTemplate(List.of(createProcessSnapshotCondition()));
        final ProcessTemplateRepository processTemplateRepository = Mockito.mock(ProcessTemplateRepository.class);
        Mockito.when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(templateWithSnapshotCondition));
        Mockito.when(processTemplateRepository.hasProcessSnapshotsConfigured()).thenCallRealMethod();

        assertTrue(processTemplateRepository.hasProcessSnapshotsConfigured());
    }


    @Test
    void testHasProcessSnapshotsConfigured_WhenNoSnapshotConditionsConfigured_ThenReturnFalse() {
        final ProcessTemplate templateWithoutSnapshotConditions = createProcessTemplate(List.of());
        final ProcessTemplateRepository processTemplateRepository = Mockito.mock(ProcessTemplateRepository.class);
        Mockito.when(processTemplateRepository.getAllTemplates()).thenReturn(List.of(templateWithoutSnapshotConditions));
        Mockito.when(processTemplateRepository.hasProcessSnapshotsConfigured()).thenCallRealMethod();

        assertFalse(processTemplateRepository.hasProcessSnapshotsConfigured());
    }


    @Test
    void testHasProcessSnapshotsConfigured_WhenNoTemplate_ThenReturnFalse() {
        final ProcessTemplateRepository processTemplateRepository = Mockito.mock(ProcessTemplateRepository.class);
        Mockito.when(processTemplateRepository.getAllTemplates()).thenReturn(null);
        Mockito.when(processTemplateRepository.hasProcessSnapshotsConfigured()).thenCallRealMethod();

        assertFalse(processTemplateRepository.hasProcessSnapshotsConfigured());
    }


    private static ProcessTemplate createProcessTemplate(List<ProcessSnapshotCondition> snapshotConditions) {
        TaskType taskType = TaskType.builder()
                .name("task").cardinality(TaskCardinality.SINGLE_INSTANCE).lifecycle(TaskLifecycle.STATIC)
                .build();
        return ProcessTemplate.builder()
                .name("name").taskTypes(List.of(taskType)).templateHash("hash")
                .processSnapshotConditions(snapshotConditions)
                .build();
    }

    private static ProcessSnapshotCondition createProcessSnapshotCondition() {
        return Mockito.mock(ProcessSnapshotCondition.class);
    }

}
