package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelation;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.*;
import ch.admin.bit.jeap.processcontext.domain.Language;
import ch.admin.bit.jeap.processcontext.domain.PcsConfigProperties;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationView;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessRelationRoleType;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessSnapshotServiceTest {

    @Mock
    private ProcessTemplateRepository processTemplateRepository;
    @Mock
    private TranslateService translateService;
    @Mock
    private ProcessSnapshotRepository processSnapshotRepository;
    @Mock
    private ProcessRelationsService processRelationsService;
    @Mock
    private MessageRepository messageRepository;

    private PcsConfigProperties pcsConfigProperties;

    private ProcessSnapshotService target;

    @BeforeEach
    void setUp() {
        pcsConfigProperties = new PcsConfigProperties();
        target = new ProcessSnapshotService(
                translateService,
                pcsConfigProperties,
                Optional.of(processSnapshotRepository),
                processTemplateRepository,
                processRelationsService,
                messageRepository);
    }

    @Test
    void testCreateProcessSnapshotArchiveData() {
        ProcessInstance processInstance = mock(ProcessInstance.class);

        pcsConfigProperties.setProcessSnapshotLanguage(Language.FR);
        pcsConfigProperties.setProcessSnapshotArchiveRetentionPeriodMonths(10);

        final ZonedDateTime processCreatedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        final ZonedDateTime processModifiedAt = processCreatedAt.plusSeconds(5);
        final ZonedDateTime processCompletedAt = processModifiedAt.plusSeconds(5);

        final ZonedDateTime taskCreatedAt = ZonedDateTime.now().plusSeconds(1).truncatedTo(ChronoUnit.MILLIS);
        final ZonedDateTime taskPlannedAt = taskCreatedAt.plusSeconds(5);
        final ZonedDateTime taskCompletedAt = taskPlannedAt.plusSeconds(5);
        final UUID taskPlannedByMessageId = UUID.randomUUID();
        final UUID taskCompletedByMessageId = UUID.randomUUID();

        final String originProcessId = "test-origin-process-id";
        final String templateName = "test-template-name";
        final String templateLabel = "test-template-label";
        final String templateDescription = "test-template-description";
        final String taskTypeLabel = "test-task-type-label";
        final String taskTypeDescription = "test-task-type-description";

        final ProcessData processDataWithRole = new ProcessData("key1", "value1", "role1");
        final ProcessData processDataWithoutRole = new ProcessData("key2", "value2");

        final TaskType taskTypeWithLabel = mock(TaskType.class);
        when(taskTypeWithLabel.getName()).thenReturn("task-type-with-label");
        when(taskTypeWithLabel.getTaskData()).thenReturn(Set.of(
                ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskData.builder().
                        sourceMessage("messageNamePlannedBy").
                        messageDataKeys(Set.of("taskDataPlanndedByKey1", "taskDataPlanndedByKey2")).
                        build()));
        final TaskType taskTypeWithoutLabel = mock(TaskType.class);
        when(taskTypeWithoutLabel.getName()).thenReturn("task-type-without-label");
        when(taskTypeWithoutLabel.getTaskData()).thenReturn(Set.of(
                ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskData.builder().
                        sourceMessage("messageNameCompletedBy").
                        messageDataKeys(Set.of("taskDataCompletedByKey1", "taskDataCompletedByKey2")).
                        build()));

        final TaskInstance plannedTaskWithOriginTaskId = TaskInstance.createTaskInstanceWithOriginTaskIdAndState(
                taskTypeWithLabel, processInstance, "planned-origin-task-id", TaskState.PLANNED, taskPlannedAt, taskPlannedByMessageId);
        ReflectionTestUtils.setField(plannedTaskWithOriginTaskId, "createdAt", taskCreatedAt);
        final TaskInstance completedTaskWithoutOriginTaskId = TaskInstance.createTaskInstanceWithOriginTaskIdAndState(
                taskTypeWithoutLabel, processInstance, null, TaskState.COMPLETED, taskCompletedAt, null);
        ReflectionTestUtils.setField(completedTaskWithoutOriginTaskId, "createdAt", taskCreatedAt);
        ReflectionTestUtils.setField(completedTaskWithoutOriginTaskId, "completedBy", taskCompletedByMessageId);

        //
        // start with a rather empty initial process
        //
        when(processInstance.getOriginProcessId()).thenReturn(originProcessId);
        when(processInstance.getProcessTemplateName()).thenReturn(templateName);
        when(processInstance.getState()).thenReturn(ProcessState.STARTED);
        when(processInstance.getCreatedAt()).thenReturn(processCreatedAt);
        when(processInstance.getProcessData()).thenReturn(Set.of());
        when(processInstance.getTasks()).thenReturn(List.of());
        when(translateService.translateProcessTemplateName(templateName)).
                thenReturn(Map.of(Language.FR.name().toLowerCase(), templateDescription));

        ProcessSnapshotArchiveData initialProcessSnapshotArchiveData = target.createProcessSnapshotArchiveData(processInstance);

        ProcessSnapshotMetadata processSnapshotMetadata = initialProcessSnapshotArchiveData.getMetadata();
        assertThat(processSnapshotMetadata.getSystemName()).isEqualTo("JEAP");
        assertThat(processSnapshotMetadata.getSchemaName()).isEqualTo("ProcessSnapshot");
        assertThat(processSnapshotMetadata.getSchemaVersion()).isEqualTo(2);
        assertThat(processSnapshotMetadata.getRetentionPeriodMonths()).isEqualTo(10);
        ProcessSnapshot initialProcessSnapshot = initialProcessSnapshotArchiveData.getProcessSnapshot();
        assertThat(initialProcessSnapshot).isNotNull();
        assertThat(initialProcessSnapshot.getSnapshotDateTimeCreated()).isNotNull();
        assertThat(initialProcessSnapshot.getOriginProcessId()).isEqualTo(originProcessId);
        assertThat(initialProcessSnapshot.getTemplateName()).isEqualTo(templateName);
        assertThat(initialProcessSnapshot.getOptionalState()).isPresent();
        assertThat(initialProcessSnapshot.getOptionalState()).contains("STARTED");
        assertThat(initialProcessSnapshot.getProcessData()).isEmpty();
        assertThat(initialProcessSnapshot.getTasks()).isEmpty();
        assertThat(initialProcessSnapshot.getDateTimeCreated()).isEqualTo(processCreatedAt.toInstant());
        assertThat(initialProcessSnapshot.getDateTimeModified()).isNull();
        assertThat(initialProcessSnapshot.getDateTimeCompleted()).isNull();

        //
        // populate the process with data
        //
        when(translateService.translateProcessTemplateName(templateName)).
                thenReturn(Map.of(Language.FR.name().toLowerCase(), templateLabel));
        when(translateService.translateTaskTypeName(templateName, taskTypeWithLabel.getName())).
                thenReturn(Map.of(Language.FR.name().toLowerCase(), taskTypeLabel));
        when(translateService.translateTaskTypeName(templateName, taskTypeWithoutLabel.getName())).
                thenReturn(Map.of(Language.FR.name().toLowerCase(), taskTypeDescription));
        when(processInstance.getModifiedAt()).thenReturn(processModifiedAt);
        var processCompletion = mock(ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletion.class);
        when(processCompletion.getCompletedAt()).thenReturn(processCompletedAt);
        when(processInstance.getProcessCompletion()).thenReturn(Optional.of(processCompletion));
        when(processInstance.getProcessData()).thenReturn(Set.of(processDataWithRole, processDataWithoutRole));
        when(processInstance.getTasks()).thenReturn(List.of(plannedTaskWithOriginTaskId, completedTaskWithoutOriginTaskId));
        MessageReferenceMessageDTO messagePlannedBy = mock(MessageReferenceMessageDTO.class);
        when(messagePlannedBy.getMessageId()).thenReturn(taskPlannedByMessageId);
        when(messagePlannedBy.getMessageName()).thenReturn("messageNamePlannedBy");
        when(messagePlannedBy.getMessageData()).thenReturn(new LinkedHashSet<>(List.of(
                MessageReferenceMessageDataDTO.builder().
                        messageDataKey("taskDataPlanndedByKey1").
                        messageDataValue("taskDataPlanndedByValue1").
                        build(),
                MessageReferenceMessageDataDTO.builder().
                        messageDataKey("taskDataPlanndedByKey2").
                        messageDataValue("taskDataPlanndedByValue2").
                        build(),
                MessageReferenceMessageDataDTO.builder().
                        messageDataKey("otherKey").
                        messageDataValue("otherValue").
                        build())));
        MessageReferenceMessageDTO messageCompletedBy = mock(MessageReferenceMessageDTO.class);
        when(messageCompletedBy.getMessageId()).thenReturn(taskCompletedByMessageId);
        when(messageCompletedBy.getMessageName()).thenReturn("messageNameCompletedBy");
        when(messageCompletedBy.getMessageData()).thenReturn(Set.of(
                MessageReferenceMessageDataDTO.builder().
                        messageDataKey("taskDataCompletedByKey1").
                        messageDataValue("taskDataCompletedByValue1").
                        build(),
                MessageReferenceMessageDataDTO.builder().
                        messageDataKey("otherKey").
                        messageDataValue("otherValue").
                        build()));
        when(processInstance.getMessageReferences()).thenReturn(List.of(messagePlannedBy, messageCompletedBy));
        when(messageRepository.findMessageUserDataByMessageId(taskPlannedByMessageId)).thenReturn(List.of(
                new String[] {"plannedByUserDataKey1", "plannedByUserDataValue1"},
                new String[] {"plannedByUserDataKey2", "plannedByUserDataValue2"}));
        when(messageRepository.findMessageUserDataByMessageId(taskCompletedByMessageId)).thenReturn(List.of());
        when(translateService.translateUserDataKey("plannedByUserDataKey1")).thenReturn(Map.of(
                Language.DE.name().toLowerCase(), "translation-de-plannedByUserDataKey1",
                Language.FR.name().toLowerCase(), "translation-fr-plannedByUserDataKey1"));
        when(translateService.translateUserDataKey("plannedByUserDataKey2")).thenReturn(Map.of());
        when(translateService.translateTaskDataKey("test-template-name", "task-type-with-label", "taskDataPlanndedByKey1")).thenReturn(Map.of(
                Language.DE.name().toLowerCase(), "translation-de-taskDataPlanndedByKey1",
                Language.FR.name().toLowerCase(), "translation-fr-taskDataPlanndedByKey1"));
        when(translateService.translateTaskDataKey("test-template-name", "task-type-with-label","taskDataPlanndedByKey2")).thenReturn(Map.of());
        List<ProcessRelationView> processRelations = List.of(
                ProcessRelationView.builder()
                        .relationName("relation-name")
                        .originRole("origin-role")
                        .targetRole("target-role")
                        .processId("target-process-id")
                        .processTemplateName("process-template")
                        .processName(Map.of("fr", "process-label"))
                        .processState("COMPLETED")
                        .relationRole(ProcessRelationRoleType.TARGET)
                        .build());
        when(processRelationsService.createProcessRelations(processInstance))
                .thenReturn(processRelations);

        ProcessSnapshotArchiveData populatedProcessSnapshotArchiveData = target.createProcessSnapshotArchiveData(processInstance);

        ProcessSnapshot populatedProcessSnapshot = populatedProcessSnapshotArchiveData.getProcessSnapshot();
        assertThat(populatedProcessSnapshot.getSnapshotDateTimeCreated()).isAfter(initialProcessSnapshot.getSnapshotDateTimeCreated());
        assertThat(populatedProcessSnapshot.getDateTimeModified()).isEqualTo(processModifiedAt.toInstant());
        assertThat(populatedProcessSnapshot.getDateTimeCompleted()).isEqualTo(processCompletedAt.toInstant());
        assertThat(populatedProcessSnapshot.getProcessData()).containsExactlyInAnyOrder(
                new ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData(
                        processDataWithRole.getKey(), processDataWithRole.getValue(), processDataWithRole.getRole()),
                new ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData(
                        processDataWithoutRole.getKey(), processDataWithoutRole.getValue(), null));
        assertThat(populatedProcessSnapshot.getTasks()).containsExactly(
                new Task(taskTypeWithLabel.getName(), taskTypeLabel,
                        plannedTaskWithOriginTaskId.getOriginTaskId(), "PLANNED",
                        plannedTaskWithOriginTaskId.getCreatedAt().toInstant(),
                        plannedTaskWithOriginTaskId.getPlannedAt().toInstant(),
                        new User(List.of(
                                new UserData("plannedByUserDataKey1", "translation-fr-plannedByUserDataKey1", "plannedByUserDataValue1"),
                                new UserData("plannedByUserDataKey2", null, "plannedByUserDataValue2"))),
                        null,
                        null,
                        List.of(
                                new TaskData("taskDataPlanndedByKey1", "translation-fr-taskDataPlanndedByKey1", "taskDataPlanndedByValue1"),
                                new TaskData("taskDataPlanndedByKey2", null, "taskDataPlanndedByValue2"))),
                new Task(taskTypeWithoutLabel.getName(), taskTypeDescription,
                        null, "COMPLETED",
                        completedTaskWithoutOriginTaskId.getCreatedAt().toInstant(),
                        completedTaskWithoutOriginTaskId.getPlannedAt().toInstant(),
                        null,
                        completedTaskWithoutOriginTaskId.getCompletedAt().toInstant(),
                        null,
                        List.of(new TaskData("taskDataCompletedByKey1", null, "taskDataCompletedByValue1")))
        );
        assertThat(populatedProcessSnapshot.getProcessRelations()).containsExactly(
                new ProcessRelation(ProcessRelationRole.TARGET,
                        "relation-name",
                        "origin-role",
                        "target-role",
                        "target-process-id",
                        "process-template",
                        "process-label",
                        "COMPLETED")
        );
    }
}
