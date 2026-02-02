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
     * @see ProcessInstanceJpaRepository#areAllTasksInFinalState(UUID)
     */
    @Override
    public boolean areAllTasksInFinalState(UUID processInstanceId) {
        processInstanceJpaRepository.flush();
        Boolean result = processInstanceJpaRepository.areAllTasksInFinalState(processInstanceId);
        return Boolean.TRUE.equals(result);
    }
}
