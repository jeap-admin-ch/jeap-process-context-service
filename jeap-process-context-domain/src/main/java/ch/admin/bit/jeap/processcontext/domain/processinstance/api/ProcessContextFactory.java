package ch.admin.bit.jeap.processcontext.domain.processinstance.api;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessContextFactory {

    private final ProcessContextRepositoryFacade repositoryFacade;

    public ProcessContext createProcessContext(ProcessInstance processInstance) {
        return ProcessContextImpl.builder()
                .processInstanceId(processInstance.getId())
                .originProcessId(processInstance.getOriginProcessId())
                .processTemplate(processInstance.getProcessTemplateName())
                .repositoryFacade(repositoryFacade)
                .build();
    }
}
