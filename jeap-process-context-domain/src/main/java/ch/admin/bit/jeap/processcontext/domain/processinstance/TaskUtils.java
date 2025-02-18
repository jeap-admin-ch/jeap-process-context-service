package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import lombok.experimental.UtilityClass;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;

@UtilityClass
class TaskUtils {

    Set<TaskType> taskTypesWithoutTaskInstance(List<TaskInstance> tasks, ProcessTemplate processTemplate) {
        Set<TaskType> taskTypesWithTaskInstance = tasks.stream().filter(t -> t.getTaskType().isPresent()).map(t -> t.getTaskType().get()).collect(Collectors.toSet());
        return processTemplate.getTaskTypes().stream()
                .filter(type -> !taskTypesWithTaskInstance.contains(type))
                .collect(toCollection(LinkedHashSet::new)); // Use LinkedHashSet to retain order as defined in template
    }

}
