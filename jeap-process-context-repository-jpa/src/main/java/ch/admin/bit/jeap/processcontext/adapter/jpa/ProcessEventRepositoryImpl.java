package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEvent;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProcessEventRepositoryImpl implements ProcessEventRepository {
    private final ProcessEventJpaRepository processEventJpaRepository;

    @Override
    public void saveAll(Iterable<ProcessEvent> processEvents) {
        processEventJpaRepository.saveAll(processEvents);
    }

    @Override
    public List<ProcessEvent> findByOriginProcessId(String originProcessId) {
        return processEventJpaRepository.findAllByOriginProcessId(originProcessId);
    }

    @Override
    public long deleteAllByOriginProcessIdIn(Set<String> originProcessIds) {
        final long count = processEventJpaRepository.countAllByOriginProcessIdIn(originProcessIds);
        processEventJpaRepository.deleteAllByOriginProcessIdIn(originProcessIds);
        return count;
    }
}
