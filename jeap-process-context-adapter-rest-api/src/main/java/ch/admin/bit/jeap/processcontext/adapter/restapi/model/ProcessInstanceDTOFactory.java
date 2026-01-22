package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.Task;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.domain.processrelation.ProcessRelationsService;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

@UtilityClass
public class ProcessInstanceDTOFactory {

    /**
     * Transmit tasks in template definition order, then ordered by task ID
     */
    private static final Comparator<TaskInstance> TASK_INSTANCE_COMPARATOR = Comparator
            .<TaskInstance, Integer>comparing(task -> task.getTaskType().isPresent() ? task.getTaskType().get().getIndex() : Integer.MAX_VALUE)
            .thenComparing(TaskInstance::getOriginTaskId, nullsLast(naturalOrder()));

    private static final Comparator<Task> TASK_SNAPSHOT_COMPARATOR = Comparator
            .comparing(Task::getDateTimeCreated);

    private static final Comparator<MessageReferenceMessageDTO> MESSAGE_COMPARATOR = Comparator
            .comparing(MessageReferenceMessageDTO::getMessageCreatedAt).reversed()
            .thenComparing(MessageReferenceMessageDTO::getMessageName);

    private static final Comparator<Relation> RELATIONS_COMPARATOR = Comparator
            .comparing(Relation::getCreatedAt);

    private static final Comparator<ProcessDataDTO> PROCESS_DATA_COMPARATOR = Comparator
            .comparing(ProcessDataDTO::getKey)
            .thenComparing(ProcessDataDTO::getValue);

    public static ProcessInstanceDTO createFromProcessInstance(ProcessInstance processInstance,
                                                               TranslateService translateService,
                                                               ProcessRelationsService processRelationsService,
                                                               MessageRepository messageRepository) {
        List<MessageDTO> messages = createMessages(processInstance.getMessageReferences());
        List<TaskInstanceDTO> tasks = createTasks(processInstance.getTasks(), messages, processInstance.getProcessTemplate().getName(), translateService, messageRepository);
        List<RelationDTO> relations = createRelations(processInstance.getRelations());
        List<ProcessRelationDTO> processRelations = createProcessRelations(processInstance, processRelationsService);
        List<ProcessDataDTO> processDataDTOList = createProcessData(processInstance.getProcessData());
        ProcessCompletionDTO processCompletion = ProcessCompletionDTO.create(processInstance.getProcessCompletion(), processInstance.getProcessTemplate().getName(), translateService);

        return ProcessInstanceDTO.builder()
                .originProcessId(processInstance.getOriginProcessId())
                .name(translateService.translateProcessTemplateName(processInstance.getProcessTemplate().getName()))
                .state(processInstance.getState().name())
                .tasks(tasks)
                .messages(messages)
                .processData(processDataDTOList)
                .relations(relations)
                .processRelations(processRelations)
                .processCompletion(processCompletion)
                .createdAt(processInstance.getCreatedAt())
                .modifiedAt(processInstance.getModifiedAt())
                .build();
    }

    public static ProcessInstanceDTO createFromSnapshot(ProcessSnapshot snap, TranslateService translateService) {

        ProcessCompletionDTO processCompletion = null;
        if (snap.getDateTimeCompleted() != null) {
            processCompletion = ProcessCompletionDTO.create(snap, translateService);
        }

        return ProcessInstanceDTO.builder()
                .originProcessId(snap.getOriginProcessId())
                .name(translateService.translateProcessTemplateName(snap.getTemplateName()))
                .state(snap.getState())
                .tasks(createTasksFromSnapshot(snap.getTasks(), snap.getTemplateName(), translateService))
                .processData(createProcessData(snap.getProcessData()))
                .processRelations(createProcessRelations(snap.getProcessRelations(), translateService))
                .createdAt(toZonedDateTime(snap.getOptionalDateTimeCreated()))
                .modifiedAt(toZonedDateTime(snap.getOptionalDateTimeModified()))
                .processCompletion(processCompletion)
                .messages(List.of())
                .relations(List.of())
                .snapshot(true)
                .snapshotCreatedAt(snap.getDateTimeCreated().atZone(ZoneId.systemDefault()))
                .build();
    }

    private static List<ProcessRelationDTO> createProcessRelations(
            List<ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelation> processRelations,
            TranslateService translateService) {
        return processRelations.stream()
                .map((ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessRelation relation) -> ProcessRelationDTO
                        .fromSnapshot(relation, translateService))
                .toList();
    }

    static List<TaskInstanceDTO> createTasks(List<TaskInstance> tasks,  List<MessageDTO> messages, String processTemplateName, TranslateService translateService, MessageRepository messageRepository) {
        return tasks.stream()
                .sorted(TASK_INSTANCE_COMPARATOR)
                .map(t -> TaskInstanceDTO.create(t, messages, processTemplateName, translateService, messageRepository))
                .toList();
    }

    static List<MessageDTO> createMessages(List<MessageReferenceMessageDTO> messageReferenceMessageDTOS) {
        return messageReferenceMessageDTOS.stream()
                .sorted(MESSAGE_COMPARATOR)
                .map(MessageDTO::create)
                .toList();
    }

    static List<RelationDTO> createRelations(Set<Relation> relations) {
        return relations.stream()
                .sorted(RELATIONS_COMPARATOR)
                .map(RelationDTO::create)
                .toList();
    }

    static List<ProcessRelationDTO> createProcessRelations(ProcessInstance processInstance,
                                                           ProcessRelationsService processRelationsService) {
        return processRelationsService.createProcessRelations(processInstance).stream()
                .map(ProcessRelationDTO::fromView)
                .toList();
    }

    static List<ProcessDataDTO> createProcessData(Set<ProcessData> processDataSet) {
        return processDataSet.stream()
                .map(ProcessDataDTO::create)
                .sorted(PROCESS_DATA_COMPARATOR)
                .toList();
    }

    private static List<ProcessDataDTO> createProcessData(List<ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessData> processData) {
        if (processData == null) {
            return List.of();
        }

        return processData.stream()
                .map(ProcessDataDTO::create)
                .sorted(PROCESS_DATA_COMPARATOR)
                .toList();
    }

    private static List<TaskInstanceDTO> createTasksFromSnapshot(List<Task> tasks, String processTemplateName, TranslateService translateService) {
        if (tasks == null) {
            return List.of();
        }

        return tasks.stream()
                .sorted(TASK_SNAPSHOT_COMPARATOR)
                .map(t -> TaskInstanceDTO.create(t, processTemplateName, translateService))
                .toList();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static ZonedDateTime toZonedDateTime(Optional<Instant> instantOptional) {
        return instantOptional
                .map(instant -> instant.atZone(ZoneId.systemDefault()))
                .orElse(null);
    }

}
