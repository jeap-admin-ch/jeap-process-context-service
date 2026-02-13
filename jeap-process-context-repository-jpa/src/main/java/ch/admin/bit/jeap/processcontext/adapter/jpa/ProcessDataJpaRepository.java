package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
interface ProcessDataJpaRepository extends JpaRepository<ProcessData, UUID> {

    List<ProcessData> findByProcessInstanceAndKey(ProcessInstance processInstance, String key);

    List<ProcessData> findByProcessInstanceAndKeyAndRole(ProcessInstance processInstance, String key, String role);

    List<ProcessData> findByProcessInstanceId(UUID processInstanceId);

    Page<ProcessData> findByProcessInstanceId(UUID processInstanceId, Pageable pageable);

    @Modifying
    @Query(value = """
            insert into process_instance_process_data (id, process_instance_id, key_, value_, role, created_at)
            values (:id, :processInstanceId, :key, :value, :role, :createdAt)
            on conflict do nothing
            """, nativeQuery = true)
    int saveIfNew(@Param("id") UUID id,
                  @Param("processInstanceId") UUID processInstanceId,
                  @Param("key") String key,
                  @Param("value") String value,
                  @Param("role") String role,
                  @Param("createdAt") ZonedDateTime createdAt);
}
