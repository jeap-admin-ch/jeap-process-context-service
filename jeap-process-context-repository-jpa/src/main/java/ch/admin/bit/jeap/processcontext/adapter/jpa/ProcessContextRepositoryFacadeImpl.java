package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.api.ProcessContextRepositoryFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessContextRepositoryFacadeImpl implements ProcessContextRepositoryFacade {

    private final ProcessInstanceJpaRepository processInstanceJpaRepository;

    /**
     * @see ProcessInstanceJpaRepository#isAllTasksInFinalState(UUID)
     */
    @Override
    public boolean isAllTasksInFinalState(UUID processInstanceId) {
        processInstanceJpaRepository.flush();
        return processInstanceJpaRepository.isAllTasksInFinalState(processInstanceId);
    }
}
