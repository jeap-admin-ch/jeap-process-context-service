package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.RelationRepository;
import ch.admin.bit.jeap.processcontext.domain.tx.Transactions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationPublicationJobServiceTest {
    private static final UUID JOB_ID = UUID.fromString("88dbb65f-9634-4685-bc86-17b72d715d3e");
    private static final UUID RELATION_1 = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb321");
    private static final UUID RELATION_2 = UUID.fromString("019c8c72-6fd1-7f25-a9a1-3b3d51fbb322");

    @Mock private MaintenanceJobRepository maintenanceJobRepository;
    @Mock private MaintenanceEventPublisher maintenanceEventPublisher;
    @Mock private RelationRepository relationRepository;
    @Mock private Transactions transactions;

    private MaintenanceProperties properties;
    private RelationPublicationJobService service;

    @BeforeEach
    void setUp() {
        properties = new MaintenanceProperties();
        properties.getLimits().setMaxTasksPerJob(10);
        service = new RelationPublicationJobService(maintenanceJobRepository, maintenanceEventPublisher,
                relationRepository, properties, transactions);
        lenient().when(transactions.withinNewTransactionWithResult(any())).thenAnswer(invocation ->
                invocation.getArgument(0, Supplier.class).get());
    }

    @Test
    void submit_sortsRelationsAndResolvesAvailableOwners() {
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.empty());
        when(relationRepository.findOriginProcessIdsByIds(List.of(RELATION_1, RELATION_2)))
                .thenReturn(Map.of(RELATION_1, "process-1"));

        boolean created = service.submit(submission(List.of(RELATION_2, RELATION_1), "submitter"));

        ArgumentCaptor<MaintenanceJob> captor = ArgumentCaptor.forClass(MaintenanceJob.class);
        verify(maintenanceJobRepository).create(captor.capture());
        MaintenanceJob job = captor.getValue();
        assertThat(job.jobType()).isEqualTo(MaintenanceJobType.RELATION_REPUBLICATION);
        assertThat(job.processTemplateName()).isNull();
        assertThat(job.tasks()).extracting(MaintenanceTask::relationId)
                .containsExactly(RELATION_1, RELATION_2);
        assertThat(job.tasks()).extracting(MaintenanceTask::targetKey)
                .containsExactly(RELATION_1.toString(), RELATION_2.toString());
        assertThat(job.tasks()).extracting(MaintenanceTask::originProcessId)
                .containsExactly("process-1", null);
        assertThat(job.tasks()).allMatch(task -> task.targetType() == MaintenanceTargetType.RELATION
                && task.taskState() == MaintenanceTaskState.EVENT_QUEUED);
        job.tasks().forEach(task -> verify(maintenanceEventPublisher).publish(job, task));
        assertThat(created).isTrue();
    }

    @Test
    void submit_sameRelationsInDifferentOrderAndSubmitter_isIdempotent() {
        RelationPublicationJobSubmission first = submission(List.of(RELATION_2, RELATION_1), "first");
        MaintenanceJob existing = MaintenanceJob.createRepublication(
                first.normalized(properties.getLimits()), Map.of());
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.of(existing));

        boolean created = service.submit(submission(List.of(RELATION_1, RELATION_2), "second"));

        assertThat(created).isFalse();
        verify(maintenanceJobRepository, never()).create(any());
        verifyNoInteractions(maintenanceEventPublisher, relationRepository);
    }

    @Test
    void submit_sameJobIdWithDifferentRelations_throwsConflict() {
        RelationPublicationJobSubmission first = submission(List.of(RELATION_1), "first");
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.of(
                MaintenanceJob.createRepublication(first.normalized(properties.getLimits()), Map.of())));
        RelationPublicationJobSubmission conflictingSubmission = submission(List.of(RELATION_2), "second");

        assertThatThrownBy(() -> service.submit(conflictingSubmission))
                .isInstanceOf(MaintenanceJobException.class)
                .extracting("reason").isEqualTo(MaintenanceJobExceptionReason.CONFLICT);
    }

    @Test
    void submit_concurrentEquivalentCreation_isIdempotent() {
        RelationPublicationJobSubmission submission = submission(List.of(RELATION_1), "first");
        MaintenanceJob existing = MaintenanceJob.createRepublication(
                submission.normalized(properties.getLimits()), Map.of());
        when(maintenanceJobRepository.findById(JOB_ID)).thenReturn(Optional.empty(), Optional.of(existing));
        when(relationRepository.findOriginProcessIdsByIds(List.of(RELATION_1))).thenReturn(Map.of());
        doThrow(MaintenanceJobAlreadyExistsException.class).when(maintenanceJobRepository).create(any());

        assertThat(service.submit(submission)).isFalse();
        verifyNoInteractions(maintenanceEventPublisher);
    }

    @Test
    void submit_rejectsNullDuplicateEmptyAndTooManyRelationIds() {
        assertInvalid(new RelationPublicationJobSubmission(null, List.of(RELATION_1), null));
        assertInvalid(new RelationPublicationJobSubmission(JOB_ID, List.of(), null));
        assertInvalid(new RelationPublicationJobSubmission(JOB_ID, java.util.Arrays.asList(RELATION_1, null), null));
        assertInvalid(new RelationPublicationJobSubmission(JOB_ID, List.of(RELATION_1, RELATION_1), null));
        properties.getLimits().setMaxTasksPerJob(1);
        assertInvalid(new RelationPublicationJobSubmission(JOB_ID, List.of(RELATION_1, RELATION_2), null));
    }

    private void assertInvalid(RelationPublicationJobSubmission submission) {
        assertThatThrownBy(() -> service.submit(submission))
                .isInstanceOf(MaintenanceJobException.class)
                .extracting("reason").isEqualTo(MaintenanceJobExceptionReason.INVALID_REQUEST);
    }

    private static RelationPublicationJobSubmission submission(List<UUID> relationIds, String submitter) {
        return new RelationPublicationJobSubmission(JOB_ID, relationIds,
                new MaintenanceJobSubmitter(submitter, submitter));
    }
}
