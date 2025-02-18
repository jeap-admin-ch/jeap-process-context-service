package ch.admin.bit.jeap.processcontext.domain.processtemplate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaskTypeTest {

    @ParameterizedTest
    @CsvSource({
            "STATIC,,false,true,false",  // static lifecycle with default cardinality (single-instance)
            "DYNAMIC,,true,true,true",   // dynamic lifecycle with default cardinality (multi-instance)
            "DYNAMIC,SINGLE_INSTANCE,true,true,false",  // dynamic lifecycle with explicit cardinality single-instance
            "OBSERVED,,true,true,true"   // observed lifecycle with default cardinality (multi-instance)
    })
    void testIsValidInstanceCount(TaskLifecycle lifecycle, TaskCardinality cardinality,
                                  boolean isZeroValidCardinality, boolean isSingleValidCardinality, boolean isMultipleValidCardinality) {
        TaskType taskTypeWithLifecycle = createTaskType(lifecycle, cardinality);

        assertFalse(taskTypeWithLifecycle.isValidInstanceCount(-1));
        assertEquals(isZeroValidCardinality, taskTypeWithLifecycle.isValidInstanceCount(0));
        assertEquals(isSingleValidCardinality, taskTypeWithLifecycle.isValidInstanceCount(1));
        assertEquals(isMultipleValidCardinality, taskTypeWithLifecycle.isValidInstanceCount(2));
        assertEquals(isMultipleValidCardinality, taskTypeWithLifecycle.isValidInstanceCount(10));
    }

    private TaskType createTaskType(TaskLifecycle lifecycle, TaskCardinality cardinality) {
        if (cardinality == null) {
            cardinality = lifecycle.getDefaultCardinality();
        }
        return TaskType.builder()
                .lifecycle(lifecycle)
                .cardinality(cardinality)
                .name("test-dummy")
                .build();
    }

}
