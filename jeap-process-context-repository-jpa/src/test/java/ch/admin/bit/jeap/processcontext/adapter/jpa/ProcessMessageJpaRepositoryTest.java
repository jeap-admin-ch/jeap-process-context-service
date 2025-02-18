package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processevent.EventType;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEvent;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventRepository;
import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEventStubs;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class ProcessMessageJpaRepositoryTest {

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;
    @Autowired
    private ProcessEventRepository repository;

    @Test
    void persistedSuccessfully() {
        String processId = Generators.timeBasedEpochGenerator().generate().toString();
        String milestone = "milestone";
        ProcessEvent savedCompletedEvent = ProcessEventStubs.createProcessCompleted(processId);
        ProcessEvent savedMilestoneEvent = ProcessEventStubs.createMilestoneReached(processId, milestone);
        repository.saveAll(List.of(savedCompletedEvent, savedMilestoneEvent));

        List<ProcessEvent> processEvents = repository.findByOriginProcessId(processId);
        assertEquals(2, processEvents.size());
        ProcessEvent completedEvent = processEvents.get(0);
        assertSame(EventType.PROCESS_COMPLETED, completedEvent.getEventType());
        assertNull(completedEvent.getName());
        ProcessEvent milestoneEvent = processEvents.get(1);
        assertSame(EventType.MILESTONE_REACHED, milestoneEvent.getEventType());
        assertEquals(milestone, milestoneEvent.getName());
    }

}
