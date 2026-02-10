package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.TaskInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
interface TaskInstanceJpaRepository extends JpaRepository<TaskInstance, UUID> {

    List<TaskInstance> findByProcessInstanceId(UUID processInstanceId);

    @Query("select distinct t.taskTypeName from TaskInstance t where t.processInstance.id = :processInstanceId")
    Set<String> findTaskTypeNamesByProcessInstanceId(@Param("processInstanceId") UUID processInstanceId);

    boolean existsByProcessInstance_IdAndTaskTypeNameAndOriginTaskId(UUID processInstanceId, String taskTypeName, String originTaskId);

    @Query("""
                select t from TaskInstance t
                 where t.processInstance.id = :processInstanceId
                   and t.taskTypeName in :taskTypeNames
                   and t.state not in (ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.COMPLETED, ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.NOT_REQUIRED, ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.DELETED)
            """)
    List<TaskInstance> findByTaskTypeNameInAndStateNotFinal(@Param("processInstanceId") UUID processInstanceId,
                                                            @Param("taskTypeNames") Set<String> taskTypeNames);

    @Modifying
    @Query("""
                update TaskInstance t
                   set t.state = ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.DELETED,
                       t.modifiedAt = :now
                 where t.processInstance.id = :processInstanceId
                   and t.state not in (ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.COMPLETED, ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.NOT_REQUIRED, ch.admin.bit.jeap.processcontext.domain.processinstance.TaskState.DELETED)
                   and t.taskTypeName not in :remainingTaskTypeNames
            """)
    void deleteRemovedOpenTasks(@Param("processInstanceId") UUID processInstanceId,
                                @Param("remainingTaskTypeNames") Set<String> remainingTaskTypeNames,
                                @Param("now") ZonedDateTime now);
}
