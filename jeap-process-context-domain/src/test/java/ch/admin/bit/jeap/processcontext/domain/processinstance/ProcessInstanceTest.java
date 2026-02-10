package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextFactory;
import ch.admin.bit.jeap.processcontext.domain.processinstance.snapshot.ProcessSnapshotCondition;
import ch.admin.bit.jeap.processcontext.domain.processinstance.snapshot.ProcessSnapshotConditionResult;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import static ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceStubs.createProcessWithProcessSnapshotCondition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProcessInstanceTest {

    @Test
    void getProcessCompletion_whenInProgess_thenCompletionNotPresent() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleStaticTask();
        assertSame(ProcessState.STARTED, processInstance.getState());
        assertFalse(processInstance.getProcessCompletion().isPresent());
    }

    @Test
    void getProcessCompletion_whenOldCompletedProcessInstanceWithoutCompletionData_thenCompletionDerivedFromProcessInstancePresent() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleStaticTask();
        ReflectionTestUtils.setField(processInstance, "state", ProcessState.COMPLETED);
        ReflectionTestUtils.setField(processInstance, "modifiedAt", ZonedDateTime.now());

        Optional<ProcessCompletion> completion = processInstance.getProcessCompletion();

        assertTrue(completion.isPresent());
        assertEquals(ProcessCompletionConclusion.SUCCEEDED, completion.get().getConclusion());
        assertEquals(processInstance.getModifiedAt(), completion.get().getCompletedAt());
    }

    @Test
    void onAfterLoadFromPersistentState() throws Exception {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTask();
        ProcessTemplate processTemplate = processInstance.getProcessTemplate();
        ProcessContextRepositoryFacadeStub repositoryFacade = new ProcessContextRepositoryFacadeStub();
        ProcessContextFactory processContextFactory = new ProcessContextFactory(repositoryFacade);

        onAfterLoadFromPersistentStateToNull(processInstance);

        processInstance.onAfterLoadFromPersistentState(processTemplate, processContextFactory);

        assertSame(processTemplate, processInstance.getProcessTemplate());
    }

    /**
     * Simulate empty template properties after loading the domain object from persistent state
     */
    private static void onAfterLoadFromPersistentStateToNull(ProcessInstance processInstance) throws Exception {
        ReflectionTestUtils.setField(processInstance, "processTemplate", null);
        assertNull(processInstance.getProcessTemplate());
    }

    @Test
    void testEvaluateSnapshotConditions() {
        ProcessSnapshotCondition alwaysTrueProcessSnapshotCondition = new AlwaysTrueProcessSnapshotCondition();
        ProcessInstance processInstance = createProcessWithProcessSnapshotCondition(alwaysTrueProcessSnapshotCondition);
        assertThat(processInstance.getSnapshotNames()).isEmpty();

        // register some snapshot name other than the AlwaysTrueProcessSnapshotCondition
        processInstance.registerSnapshot("some-unrelated-snapshot-name");
        assertThat(processInstance.getSnapshotNames()).containsExactly("some-unrelated-snapshot-name");

        // AlwaysTrueProcessSnapshotCondition should trigger as its name has not yet been registerd as already triggered
        Set<String> snapshotNamesFirstEvaluation = processInstance.evaluateSnapshotConditions();
        assertThat(snapshotNamesFirstEvaluation).containsExactly(AlwaysTrueProcessSnapshotCondition.SNAPSHOT_CONDITION_NAME);

        // AlwaysTrueProcessSnapshotCondition should trigger again as the snapshot triggere still has not been registered
        Set<String> snapshotNamesSecondEvaluation = processInstance.evaluateSnapshotConditions();
        assertThat(snapshotNamesSecondEvaluation).containsExactly(AlwaysTrueProcessSnapshotCondition.SNAPSHOT_CONDITION_NAME);

        // Register the triggered snapshot
        processInstance.registerSnapshot(snapshotNamesSecondEvaluation.iterator().next());
        assertThat(processInstance.getSnapshotNames()).containsExactly(
                "some-unrelated-snapshot-name", AlwaysTrueProcessSnapshotCondition.SNAPSHOT_CONDITION_NAME);

        // AlwaysTrueProcessSnapshotCondition should no longer trigger as the snapshot has now been registered as already triggered
        Set<String> snapshotNamesThirdEvaluation = processInstance.evaluateSnapshotConditions();
        assertThat(snapshotNamesThirdEvaluation).isEmpty();
    }

    static class AlwaysTrueProcessSnapshotCondition implements ProcessSnapshotCondition {
        static final String SNAPSHOT_CONDITION_NAME = "AlwaysTrueCondition";

        @Override
        public ProcessSnapshotConditionResult triggerSnapshot(ProcessInstance processInstance) {
            return ProcessSnapshotConditionResult.triggeredFor("AlwaysTrueCondition");
        }
    }
}
