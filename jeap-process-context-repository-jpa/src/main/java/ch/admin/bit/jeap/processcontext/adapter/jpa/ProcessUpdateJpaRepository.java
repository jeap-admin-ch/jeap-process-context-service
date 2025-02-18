package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdate;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

@Repository
interface ProcessUpdateJpaRepository extends JpaRepository<ProcessUpdate, UUID>, ProcessUpdateRepository {

    @Override
    @Modifying
    @Query("update ProcessUpdate set handled = true, failed = true where id = :processUpdateId")
    void markHandlingFailed(@Param("processUpdateId") UUID processUpdateId);

    @Override
    @Modifying
    @Query("update ProcessUpdate set handled = true where id = :processUpdateId")
    void markHandled(@Param("processUpdateId") UUID processUpdateId);

    @Override
    @Query(nativeQuery = true, value = "SELECT count(*) FROM process_update p WHERE p.origin_process_id in (:originProcessIds) ")
    int countAllByOriginProcessIdIn(@Param("originProcessIds") Set<String> originProcessIds);

    @Override
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM process_update p WHERE p.origin_process_id in (:originProcessIds) ")
    void deleteAllByOriginProcessIdIn(@Param("originProcessIds") Set<String> originProcessIds);

    @Override
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM process_update p WHERE p.id in (:ids) ")
    void deleteAllById(@Param("ids") Set<UUID> ids);

    @Override
    @Query("select p.id from ProcessUpdate p where p.createdAt < :createdBefore and p.handled = false")
    Slice<UUID> findProcessUpdateIdWithHandledFalse(@Param("createdBefore") ZonedDateTime createdBefore, Pageable pageable);
}
