package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ProcessContextFactoryTest {

    @Test
    void createProcessContext() {
        ProcessInstance processInstance = ProcessInstanceStubs.createProcessWithSingleDynamicTaskInstance();
        String templateName = processInstance.getProcessTemplateName();
        Set<String> taskNames = Set.of("taskId1", "taskId2");
        ReflectionTestUtils.setField(processInstance, "processCompletion", new ProcessCompletion(
                ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessCompletionConclusion.SUCCEEDED, "all good", ZonedDateTime.now()));

        ProcessContextFactory processContextFactory = new ProcessContextFactory(mock(ProcessContextRepositoryFacade.class));
        ProcessContext processContext = processContextFactory.createProcessContext(processInstance);

        assertEquals(processInstance.getOriginProcessId(), processContext.getOriginProcessId());
        assertEquals(processInstance.getProcessTemplate().getName(), processContext.getProcessName());
    }

    @Test
    void allTaskStatesMappedInApi() {
        Set<String> domainStateNames = Arrays.stream(ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.values())
                .map(Enum::name)
                .collect(toSet());

        domainStateNames.forEach(TaskState::valueOf);

        assertEquals(TaskState.values().length, domainStateNames.size());
    }

    @Test
    void allProcessStatesMappedInApi() {
        Set<String> domainStateNames = Arrays.stream(ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessState.values())
                .map(Enum::name)
                .collect(toSet());

        domainStateNames.forEach(ProcessState::valueOf);

        assertEquals(ProcessState.values().length, domainStateNames.size());
    }

    @Test
    void allProcessConclusionsMappedInApi() {
        Set<String> processCompletionConclusionNames = Arrays.stream(ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessCompletionConclusion.values())
                .map(Enum::name)
                .collect(toSet());

        processCompletionConclusionNames.forEach(ProcessCompletionConclusion::valueOf);

        assertEquals(ProcessCompletionConclusion.values().length, processCompletionConclusionNames.size());
    }
}
