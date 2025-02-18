package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProcessTemplateTest {

    private ProcessTemplate processTemplate;
    private TaskType singleTaskType;

    @BeforeEach
    void setUp() {
        ProcessRelationPattern processRelationPattern = ProcessRelationPattern.builder()
                .name("ThisIsAProcessRelation")
                .roleType(ProcessRelationRoleType.ORIGIN)
                .originRole("OriginRole")
                .targetRole("TargetRole")
                .visibility(ProcessRelationRoleVisibility.BOTH)
                .source(ProcessRelationSource.builder()
                        .messageName("msgName")
                        .messageDataKey("msgDataKey")
                        .build())
                .build();


        singleTaskType = TaskType.builder()
                .name("task")
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        TaskType optionalTaskType = TaskType.builder()
                .name("dynamicTask")
                .lifecycle(TaskLifecycle.DYNAMIC)
                .cardinality(TaskCardinality.MULTI_INSTANCE)
                .build();
        processTemplate = ProcessTemplate.builder()
                .name("processTemplate")
                .templateHash("hash")
                .taskTypes(List.of(singleTaskType, optionalTaskType))
                .processRelationPatterns(List.of(processRelationPattern))
                .build();
    }

    @Test
    void findTaskTypeByName() {
        Optional<TaskType> task = processTemplate.getTaskTypeByName("task");
        Optional<TaskType> unknownTask = processTemplate.getTaskTypeByName("unknown");

        assertTrue(task.isPresent());
        assertTrue(unknownTask.isEmpty());
    }

    @Test
    void getTaskTypeByName() {
        assertSame(singleTaskType, processTemplate.getTaskTypeByName("task").orElseThrow());
        assertSame(Optional.empty(), processTemplate.getTaskTypeByName("unknown"));
    }

    @Test
    void getProcessRelations() {
        List<ProcessRelationPattern> processRelationPatternList = processTemplate.getProcessRelationPatterns();
        assertEquals(1, processRelationPatternList.size());
        ProcessRelationPattern aProcessRelationPattern = processRelationPatternList.get(0);
        assertSame(ProcessRelationRoleType.ORIGIN, aProcessRelationPattern.getRoleType());
        assertSame(ProcessRelationRoleVisibility.BOTH, aProcessRelationPattern.getVisibility());
    }
}
