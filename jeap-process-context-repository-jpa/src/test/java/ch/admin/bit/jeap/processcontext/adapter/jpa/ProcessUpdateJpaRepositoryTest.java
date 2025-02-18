package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplateRepository;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdate;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateType;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = JpaAdapterConfig.class)
class ProcessUpdateJpaRepositoryTest {

    private static final String ORIGIN_PROCESS_ID = Generators.timeBasedEpochGenerator().generate().toString();
    private static final String EVENT_NAME = "eventName";
    private static final String IDEMPOTENCE_ID = "idempotenceId";
    private static final UUID EVENT_REFERENCE = Generators.timeBasedEpochGenerator().generate();

    @MockitoBean
    private ProcessTemplateRepository processTemplateRepository;
    @Autowired
    private ProcessUpdateJpaRepository processUpdateJpaRepository;
    @PersistenceContext
    EntityManager entityManager;

    @Test
    void testSaveAndGetDomainEventReceivedProcessUpdate() {
        ProcessUpdate processUpdate = ProcessUpdate.messageReceived()
                .originProcessId(ORIGIN_PROCESS_ID)
                .messageReference(EVENT_REFERENCE)
                .messageName(EVENT_NAME)
                .idempotenceId(IDEMPOTENCE_ID)
                .build();

        ProcessUpdate processUpdateSaved = processUpdateJpaRepository.saveAndFlush(processUpdate);

        entityManager.detach(processUpdateSaved);
        ProcessUpdate processUpdateRead = processUpdateJpaRepository.getReferenceById(processUpdateSaved.getId());
        assertProcessUpdateData(processUpdateRead, processUpdateSaved.getId(), ProcessUpdateType.DOMAIN_EVENT, null, EVENT_NAME, EVENT_REFERENCE, IDEMPOTENCE_ID);
    }

    @Test
    void testSaveAndGetProcessCreatedProcessUpdate() {
        ProcessUpdate processUpdate = ProcessUpdate.processCreated()
                .originProcessId(ORIGIN_PROCESS_ID)
                .build();

        ProcessUpdate processUpdateSaved = processUpdateJpaRepository.saveAndFlush(processUpdate);

        entityManager.detach(processUpdateSaved);
        ProcessUpdate processUpdateRead = processUpdateJpaRepository.getReferenceById(processUpdateSaved.getId());
        assertProcessUpdateData(processUpdateRead, processUpdateSaved.getId(), ProcessUpdateType.PROCESS_CREATED, null, "createProcessInstance", null, ORIGIN_PROCESS_ID);
    }

    @Test
    void testFindByOriginProcessIdAndHandledFalse() {
        saveProcessUpdate(IDEMPOTENCE_ID, false);
        saveProcessUpdate("other idempotence id", true);

        List<ProcessUpdate> updates = processUpdateJpaRepository.findByOriginProcessIdAndHandledFalse(ORIGIN_PROCESS_ID);

        assertThat(updates).hasSize(1);
        assertThat(updates.get(0).isHandled()).isFalse();
    }

    @Test
    void testFindByEventNameAndIdempotenceId() {
        saveProcessUpdate(IDEMPOTENCE_ID, false);

        Optional<ProcessUpdate> updateFound = processUpdateJpaRepository.findByOriginProcessIdAndMessageNameAndIdempotenceId(ORIGIN_PROCESS_ID, EVENT_NAME, IDEMPOTENCE_ID);
        Optional<ProcessUpdate> updateNotFound = processUpdateJpaRepository.findByOriginProcessIdAndMessageNameAndIdempotenceId(ORIGIN_PROCESS_ID, EVENT_NAME, "other idempotence id");

        assertThat(updateFound).isPresent();
        assertThat(updateNotFound).isNotPresent();
    }


    @Test
    void testDeleteAllByOriginProcessIdIn() {
        saveProcessUpdate("id_1");
        saveProcessUpdate("id_2");
        saveProcessUpdate("id_3");
        saveProcessUpdate("id_4");

        Set<String> originProcessIds = new HashSet<>();
        originProcessIds.add("id_1");
        originProcessIds.add("id_2");
        originProcessIds.add("id_4");
        assertThat(processUpdateJpaRepository.findAll()).hasSize(4);

        final long count = processUpdateJpaRepository.countAllByOriginProcessIdIn(originProcessIds);
        processUpdateJpaRepository.deleteAllByOriginProcessIdIn(originProcessIds);

        assertThat(count).isEqualTo(3);
        assertThat(processUpdateJpaRepository.findAll()).hasSize(1);
        assertThat(processUpdateJpaRepository.findAll().get(0).getOriginProcessId()).isEqualTo("id_3");
    }


    private void assertProcessUpdateData(ProcessUpdate processUpdate, UUID id, ProcessUpdateType processUpdateType, String name, String eventName, UUID eventReference, String idempotenceId) {
        assertThat(processUpdate.getId()).isEqualTo(id);
        assertThat(processUpdate.getProcessUpdateType()).isEqualByComparingTo(processUpdateType);
        assertThat(processUpdate.getOriginProcessId()).isEqualTo(ORIGIN_PROCESS_ID);
        assertThat(processUpdate.getMessageReference()).isEqualTo(Optional.ofNullable(eventReference));
        assertThat(processUpdate.getMessageName()).isEqualTo(eventName);
        assertThat(processUpdate.getIdempotenceId()).isEqualTo(idempotenceId);
        assertThat(processUpdate.getName()).isEqualTo(name);
    }

    private void saveProcessUpdate(String idempotenceId, boolean handled) {
        ProcessUpdate processUpdate = ProcessUpdate.messageReceived()
                .originProcessId(ORIGIN_PROCESS_ID)
                .messageReference(EVENT_REFERENCE)
                .messageName(EVENT_NAME)
                .idempotenceId(idempotenceId)
                .build();
        if (handled) {
            processUpdate.setHandled();
        }
        processUpdateJpaRepository.saveAndFlush(processUpdate);
    }

    private void saveProcessUpdate(String originProcessId) {
        ProcessUpdate processUpdate = ProcessUpdate.messageReceived()
                .originProcessId(originProcessId)
                .messageReference(Generators.timeBasedEpochGenerator().generate())
                .messageName(EVENT_NAME)
                .idempotenceId(Generators.timeBasedEpochGenerator().generate().toString())
                .build();
        processUpdateJpaRepository.saveAndFlush(processUpdate);
    }

}
