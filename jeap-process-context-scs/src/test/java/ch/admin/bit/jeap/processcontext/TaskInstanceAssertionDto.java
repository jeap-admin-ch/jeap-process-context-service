package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.adapter.restapi.model.ProcessInstanceDTO;
import ch.admin.bit.jeap.processcontext.adapter.restapi.model.TaskDataDTO;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor
public class TaskInstanceAssertionDto {
    String originTaskId;
    Map<String, String> labels;
    String lifecycle;
    String cardinality;
    String state;
    Set<TaskDataAssertionDTO> taskDataAssertionDTOSs;

    public static TaskInstanceAssertionDto taskWithoutOriginTaskId(String name, String lifecycle, String cardinality, String state) {
        return new TaskInstanceAssertionDto(null, Map.of("de",name), lifecycle, cardinality, state, Set.of());
    }

    public static TaskInstanceAssertionDto task(String originTaskId, String name, String lifecycle, String cardinality, String state) {
        return task(originTaskId, name, lifecycle, cardinality, state, Set.of());
    }

    public static TaskInstanceAssertionDto task(String originTaskId, String name, String lifecycle, String cardinality, String state, Set<TaskDataAssertionDTO> taskDataAssertionDTOs) {
        return new TaskInstanceAssertionDto(originTaskId, Map.of("de", name), lifecycle, cardinality, state, taskDataAssertionDTOs);
    }

    public static Set<TaskInstanceAssertionDto> toTaskInstanceAssertions(ProcessInstanceDTO processInstanceDTO) {
        return processInstanceDTO.getTasks().stream().map(task -> task(task.getOriginTaskId(), task.getName().get("de"), task.getLifecycle(),
                task.getCardinality(), task.getState(), toTaskDataAssertionDTOs(task.getTaskData()))).
                collect(Collectors.toSet());
    }

    private static Set<TaskDataAssertionDTO> toTaskDataAssertionDTOs(Set<TaskDataDTO> taskDataDTOs) {
        if (taskDataDTOs == null) {
            return Set.of();
        } else {
            return taskDataDTOs.stream().map(TaskInstanceAssertionDto::toTaskDataAssertionDTO).collect(Collectors.toSet());
        }
    }

    private static TaskDataAssertionDTO toTaskDataAssertionDTO(TaskDataDTO taskDataDTO) {
        return new TaskDataAssertionDTO(taskDataDTO.getKey(), taskDataDTO.getValue(), taskDataDTO.getLabels().get("de"));
    }

    @Value
    @AllArgsConstructor
    public static class TaskDataAssertionDTO {
        String key;
        String value;
        String labelDe;
    }

}
