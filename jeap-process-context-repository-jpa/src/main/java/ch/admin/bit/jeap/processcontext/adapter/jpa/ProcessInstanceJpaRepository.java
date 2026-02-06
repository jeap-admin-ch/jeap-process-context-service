package ch.admin.bit.jeap.processcontext.adapter.jpa;

import ch.admin.bit.jeap.processcontext.domain.processinstance.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@SuppressWarnings({"SpringDataRepositoryMethodReturnTypeInspection"})
interface ProcessInstanceJpaRepository extends JpaRepository<ProcessInstance, UUID> {

    String FIND_UNCOMPLETED_PROCESS_INSTANCES_HAVING_PROCESS_DATA_BASE_QUERY =
            """
                    SELECT p.originProcessId FROM ProcessInstance p, ProcessData d \
                    WHERE d.processInstance = p AND p.processTemplateName = :processTemplateName AND p.state <> 'COMPLETED' \
                    AND d.key = :processDataKey AND d.value = :processDataValue\
                    """;

    String FIND_MESSAGE_REFERENCES_MESSAGE_DATA_QUERY = """
                select r.id as messageReferenceId, d.key as messageDataKey, d.value as messageDataValue, d.role as messageDataRole
                    from MessageReference r join r.processInstance p join events e on e.id = r.messageId join e.messageData d
            where p.id = :processInstanceId and d.templateName = p.processTemplateName
            """;

    String FIND_MESSAGE_REFERENCES_RELATED_ORIGIN_TASK_IDS_QUERY = """
                select r.id as messageReferenceId, oti.originTaskId as relatedOriginTaskId
                from MessageReference r join r.processInstance p join events e on e.id = r.messageId join e.originTaskIds oti
            where p.id = :processInstanceId and oti.templateName = p.processTemplateName
            """;

    String FIND_MESSAGE_REFERENCES_MESSAGES_QUERY = """
                    select r.id as messageReferenceId, e.id as messageId, e.messageName as messageName, e.createdAt as messageReceivedAt, e.messageCreatedAt as messageCreatedAt, e.traceId as traceId
                from MessageReference r join r.processInstance p join events e on e.id = r.messageId
            where p.id = :processInstanceId
            """;

    boolean existsByOriginProcessId(String originProcessId);

    Optional<ProcessInstance> findByOriginProcessId(String originProcessId);

    @Query("SELECT p.processTemplateName as processTemplateName, " +
            "p.originProcessId as originProcessId, " +
            "p.state as state " +
            "FROM ProcessInstance p where p.originProcessId = :originProcessId")
    Optional<ProcessInstanceSummary> findProcessInstanceSummaryByOriginProcessId(@Param("originProcessId") String originProcessId);

    @Query("SELECT p.processTemplateName as templateName, p.processTemplateHash as templateHash from ProcessInstance p where p.originProcessId = :originProcessId")
    Optional<ProcessInstanceTemplate> findProcessInstanceTemplate(@Param("originProcessId") String originProcessId);

    @Query(FIND_UNCOMPLETED_PROCESS_INSTANCES_HAVING_PROCESS_DATA_BASE_QUERY + " AND d.role = :processDataRole")
    Set<String> findUncompletedProcessInstancesHavingProcessData(@Param("processTemplateName") String processTemplateName,
                                                                 @Param("processDataKey") String processDataKey,
                                                                 @Param("processDataValue") String processDataValue,
                                                                 @Param("processDataRole") String processDataRole);

    @Query(FIND_UNCOMPLETED_PROCESS_INSTANCES_HAVING_PROCESS_DATA_BASE_QUERY + " AND d.role IS NULL")
    Set<String> findUncompletedProcessInstancesHavingProcessDataWithoutRole(@Param("processTemplateName") String processTemplateName,
                                                                            @Param("processDataKey") String processDataKey,
                                                                            @Param("processDataValue") String processDataValue);

    Slice<ProcessInstanceQueryResult> findIdOriginProcessIdByStateAndModifiedAtBefore(ProcessState state, ZonedDateTime modifiedBefore, Pageable pageable);

    @Query("""
            SELECT p.originProcessId FROM ProcessInstance p WHERE \
            p.state <> 'COMPLETED' AND p.modifiedAt > :lastModifiedAfter AND \
            p.processTemplateName = :templateName AND p.processTemplateHash <> :templateHash\
            """)
    Slice<String> findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
            @Param("templateName") String templatename,
            @Param("templateHash") String templateHash,
            @Param("lastModifiedAfter") ZonedDateTime lastModifiedAfter,
            Pageable pageable);

    @Query("SELECT DISTINCT p FROM ProcessInstance p, ProcessData d WHERE d.processInstance = p AND d.value = :processData")
    Page<ProcessInstance> findByProcessDataValue(@Param("processData") String processData, Pageable pageable);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM event_reference e WHERE e.process_instance_id in (:processInstanceIds) ")
    void deleteEventReferenceByProcessInstanceIds(@Param("processInstanceIds") Iterable<? extends UUID> processInstanceIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM process_instance_process_data p WHERE p.process_instance_id in (:processInstanceIds) ")
    void deleteProcessDataByProcessInstanceIds(@Param("processInstanceIds") Iterable<? extends UUID> processInstanceIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM process_instance_relations p WHERE p.process_instance_id in (:processInstanceIds) ")
    void deleteProcessRelationsByProcessInstanceIds(@Param("processInstanceIds") Iterable<? extends UUID> processInstanceIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM process_instance_process_relations r WHERE r.process_instance_id in (:processInstanceIds) ")
    void deleteProcessInstanceProcessRelationsByProcessInstanceIds(@Param("processInstanceIds") Iterable<? extends UUID> processInstanceIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM process_instance_process_relations r WHERE r.related_process_id in (:relatedOriginProcessIds) ")
    void deleteProcessInstanceProcessRelationsByRelatedOriginProcessIds(@Param("relatedOriginProcessIds") Set<String> relatedOriginProcessIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM task_instance p WHERE p.process_instance_id in (:processInstanceIds) ")
    void deleteTaskInstanceByProcessInstanceIds(@Param("processInstanceIds") Iterable<? extends UUID> processInstanceIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM process_instance p WHERE p.id in (:processInstanceIds) ")
    void deleteByProcessInstanceIds(@Param("processInstanceIds") Iterable<? extends UUID> processInstanceIds);

    @Query(nativeQuery = true, value = "SELECT p.origin_process_id FROM process_instance p WHERE p.id in (:processInstanceIds) ")
    Set<String> getOriginProcessIdsByInstanceIds(@Param("processInstanceIds") Set<UUID> processInstanceIds);

    /**
     * Checks whether all tasks in the process instance are in a final state. For this to be true,
     * the following conditions must be met:
     * <ul>
     *   <li>the process instance contains at least one task</li>
     *   <li>all existing tasks are in a final state (COMPLETED, NOT_REQUIRED or DELETED)</li>
     * </ul>
     *
     * @return true if all tasks are in a final state and at least one task exists, false otherwise
     */
    @Query("""
            select case when
            exists (
                  select 1
                  from TaskInstance t
                  where t.processInstance.id = :processInstanceId
            )
            and not exists (
                  select 1
                  from TaskInstance t2
                  where t2.processInstance.id = :processInstanceId
                    and t2.state not in ('COMPLETED', 'NOT_REQUIRED', 'DELETED')
            )
            then true else false end
            from ProcessInstance p
            where p.id = :processInstanceId
            """)
    Boolean areAllTasksInFinalState(UUID processInstanceId);

    @Query("""
            SELECT r FROM MessageReference r
            JOIN r.processInstance p
            JOIN events e ON e.id = r.messageId
            WHERE p.id = :processInstanceId AND e.messageName = :messageType
            ORDER BY e.createdAt DESC
            """)
    List<MessageReference> findMessageReferencesByMessageType(
            @Param("processInstanceId") UUID processInstanceId,
            @Param("messageType") String messageType,
            Pageable pageable);

    @Query("""
            SELECT r FROM MessageReference r
            JOIN r.processInstance p
            JOIN events e ON e.id = r.messageId
            JOIN e.originTaskIds oti
            WHERE p.id = :processInstanceId
              AND e.messageName = :messageType
              AND oti.originTaskId = :originTaskId
              AND oti.templateName = p.processTemplateName
            ORDER BY e.createdAt DESC
            """)
    List<MessageReference> findMessageReferencesByMessageTypeAndOriginTaskId(
            @Param("processInstanceId") UUID processInstanceId,
            @Param("messageType") String messageType,
            @Param("originTaskId") String originTaskId,
            Pageable pageable);
}
