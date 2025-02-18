package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processevent.ProcessEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProcessEventJpaRepository extends JpaRepository<ProcessEvent, String> {

    List<ProcessEvent> findAllByOriginProcessId(String originProcessId);

    @Query(nativeQuery = true, value = "SELECT count(*) FROM process_event p WHERE p.origin_process_id in (:originProcessIds) ")
    int countAllByOriginProcessIdIn(@Param("originProcessIds") Set<String> originProcessIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM process_event p WHERE p.origin_process_id in (:originProcessIds) ")
    void deleteAllByOriginProcessIdIn(@Param("originProcessIds") Set<String> originProcessIds);

}
