package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface ProcessDataJpaRepository extends JpaRepository<ProcessData, UUID> {

    List<ProcessData> findByProcessInstanceAndKey(ProcessInstance processInstance, String key);

    List<ProcessData> findByProcessInstanceAndKeyAndRole(ProcessInstance processInstance, String key, String role);

    List<ProcessData> findByProcessInstanceId(UUID processInstanceId);

    @Modifying
    @Query(value = """
            insert into process_instance_process_data (id, process_instance_id, key_, value_, role, created_at)
            values (:#{#pd.id},:#{#pd.processInstance.id},:#{#pd.key},:#{#pd.value},:#{#pd.role},:#{#pd.createdAt})
            on conflict do nothing
            """, nativeQuery = true)
    int saveIfNew(@Param("pd") ProcessData pd);
}
