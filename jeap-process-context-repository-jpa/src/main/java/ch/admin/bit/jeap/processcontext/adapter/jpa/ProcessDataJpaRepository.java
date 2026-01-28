package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface ProcessDataJpaRepository extends JpaRepository<ProcessData, UUID> {

    List<ProcessData> findByProcessInstanceAndKey(ProcessInstance processInstance, String key);

    List<ProcessData> findByProcessInstanceAndKeyAndRole(ProcessInstance processInstance, String key, String role);
}
