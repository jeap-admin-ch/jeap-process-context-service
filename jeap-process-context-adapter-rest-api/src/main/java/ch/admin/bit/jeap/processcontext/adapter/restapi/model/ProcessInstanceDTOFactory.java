package ch.admin.bit.jeap.processcontext.adapter.restapi.model;

import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.ProcessSnapshot;
import ch.admin.bit.jeap.processcontext.archive.processsnapshot.v2.Task;
import ch.admin.bit.jeap.processcontext.domain.TranslateService;
import ch.admin.bit.jeap.processcontext.domain.message.MessageReferenceRepository;
import ch.admin.bit.jeap.processcontext.domain.message.MessageRepository;
import ch.admin.bit.jeap.processcontext.domain.processinstance.MessageReferenceMessageDTO;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskInstance;
import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

@Component
@RequiredArgsConstructor
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

    private final TranslateService translateService;
    private final MessageRepository messageRepository;
    private final MessageReferenceRepository messageReferenceRepository;
    private final TaskInstanceRepository taskInstanceRepository;

    public ProcessInstanceDTO createFromProcessInstance(ProcessInstance processInstance) {
        List<TaskInstance> taskInstances = taskInstanceRepository.findByProcessInstanceId(processInstance.getProcessTemplate(), processInstance.getId());
        List<TaskInstanceDTO> tasks = createTasks(taskInstances, processInstance.getProcessTemplate().getName(), translateService, messageRepository);
        ProcessCompletionDTO processCompletion = ProcessCompletionDTO.create(processInstance.getProcessCompletion(), processInstance.getProcessTemplate().getName(), translateService);

        return ProcessInstanceDTO.builder()
                .originProcessId(processInstance.getOriginProcessId())
                .templateName(processInstance.getProcessTemplateName())
                .name(translateService.translateProcessTemplateName(processInstance.getProcessTemplate().getName()))
                .state(processInstance.getState().name())
                .tasks(tasks)
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
                .templateName(snap.getTemplateName())
                .name(translateService.translateProcessTemplateName(snap.getTemplateName()))
                .state(snap.getState())
                .tasks(createTasksFromSnapshot(snap.getTasks(), snap.getTemplateName(), translateService))
                .createdAt(toZonedDateTime(snap.getOptionalDateTimeCreated()))
                .modifiedAt(toZonedDateTime(snap.getOptionalDateTimeModified()))
                .processCompletion(processCompletion)
                .snapshot(true)
                .snapshotCreatedAt(snap.getDateTimeCreated().atZone(ZoneId.systemDefault()))
                .build();
    }

    List<TaskInstanceDTO> createTasks(List<TaskInstance> tasks, String processTemplateName, TranslateService translateService, MessageRepository messageRepository) {
        return tasks.stream()
                .sorted(TASK_INSTANCE_COMPARATOR)
                .map(t -> createTaskInstanceDTO(processTemplateName, translateService, messageRepository, t))
                .toList();
    }

    private TaskInstanceDTO createTaskInstanceDTO(String processTemplateName, TranslateService translateService, MessageRepository messageRepository, TaskInstance taskInstance) {
        List<MessageDTO> messages = new ArrayList<>();
        addMessageDTO(taskInstance.getPlannedBy(), taskInstance, messages);
        addMessageDTO(taskInstance.getCompletedBy(), taskInstance, messages);
        return TaskInstanceDTO.create(taskInstance, messages, processTemplateName, translateService, messageRepository);
    }

    private void addMessageDTO(UUID messageId, TaskInstance taskInstance, List<MessageDTO> messages) {
        if (messageId != null) {
            MessageReferenceMessageDTO dto = messageReferenceRepository.findByProcessInstanceIdAndMessageId(taskInstance.getProcessInstance().getId(), messageId);
            if (dto != null) {
                messages.add(MessageDTO.createWithFullMessageData(dto));
            }
        }
    }

    static List<MessageDTO> createMessages(List<MessageReferenceMessageDTO> messageReferences) {
        return messageReferences.stream()
                .sorted(MESSAGE_COMPARATOR)
                .map(MessageDTO::create)
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
