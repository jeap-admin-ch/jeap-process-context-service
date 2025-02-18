package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskCardinality;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskLifecycle;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.TaskType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static java.util.Collections.singletonList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessTemplateStubs {

    public static ProcessTemplate createSingleTaskProcessTemplate(String taskName) {
        TaskType taskType = TaskType.builder()
                .name(taskName)
                .lifecycle(TaskLifecycle.STATIC)
                .cardinality(TaskCardinality.SINGLE_INSTANCE)
                .build();
        return ProcessTemplate.builder()
                .name("template")
                .templateHash("hash")
                .taskTypes(singletonList(
                        taskType))
                .build();
    }

}
