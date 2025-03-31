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
import org.springframework.transaction.annotation.Transactional;

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
                    SELECT p.originProcessId FROM ProcessInstance p INNER JOIN p.processData d \
                    WHERE p.processTemplateName = :processTemplateName AND p.state <> 'COMPLETED' \
                    AND d.key = :processDataKey AND d.value = :processDataValue\
                    """;

    String FIND_MESSAGE_REFERENCES_MESSAGE_DATA_QUERY = """
            select r.id as messageReferenceId, d.key as messageDataKey, d.value as messageDataValue, d.role as messageDataRole
                from ProcessInstance p join p.messageReferences r join events e on e.id = r.messageId join e.messageData d
        where p.id = :processInstanceId and d.templateName = p.processTemplateName
        """;

    String FIND_MESSAGE_REFERENCES_RELATED_ORIGIN_TASK_IDS_QUERY = """
            select r.id as messageReferenceId, oti.originTaskId as relatedOriginTaskId
            from ProcessInstance p join p.messageReferences r join events e on e.id = r.messageId join e.originTaskIds oti
        where p.id = :processInstanceId and oti.templateName = p.processTemplateName
        """;

    String FIND_MESSAGE_REFERENCES_MESSAGES_QUERY = """
                select r.id as messageReferenceId, e.id as messageId, e.messageName as messageName, e.createdAt as messageReceivedAt, e.messageCreatedAt as messageCreatedAt, e.traceId as traceId
            from ProcessInstance p join p.messageReferences r join events e on e.id = r.messageId
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

    @Query("SELECT p.processTemplateName FROM ProcessInstance p where p.originProcessId = :originProcessId")
    Optional<String> getProcessTemplateNameByOriginProcessId(@Param("originProcessId") String originProcessId);

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

    @Query(FIND_MESSAGE_REFERENCES_MESSAGE_DATA_QUERY)
    List<MessageReferenceMessageData> findMessageReferencesMessageData(@Param("processInstanceId") UUID processInstanceId);

    @Query(FIND_MESSAGE_REFERENCES_RELATED_ORIGIN_TASK_IDS_QUERY)
    List<MessageReferenceRelatedTaskId> findMessageReferencesRelatedOriginTaskIds(@Param("processInstanceId") UUID processInstanceId);

    @Query(FIND_MESSAGE_REFERENCES_MESSAGES_QUERY)
    List<MessageReferenceMessage> findMessageReferencesMessages(@Param("processInstanceId") UUID processInstanceId);


    @Transactional
    @Modifying
    @Query("""
            UPDATE ProcessInstance p SET p.processTemplateHash = :templateHash \
            WHERE p.processTemplateHash IS NULL AND p.processTemplateName = :templateName\
            """)
    void setInitialTemplateHashIfNotSet(@Param("templateName") String templateName, @Param("templateHash") String templateHash);

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

    Page<ProcessInstance> findDistinctByProcessData_value(@Param("processData") String processData, Pageable pageable);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM event_reference e WHERE e.process_instance_id in (:processInstanceIds) ")
    void deleteEventReferenceByProcessInstanceIds(@Param("processInstanceIds") Iterable<? extends UUID> processInstanceIds);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM milestone m WHERE m.process_instance_id in (:processInstanceIds) ")
    void deleteMilestoneByProcessInstanceIds(@Param("processInstanceIds") Iterable<? extends UUID> processInstanceIds);

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

    @Query(nativeQuery = true, value = "SELECT p.latest_snapshot_version FROM process_instance p WHERE p.origin_process_id = :originProcessId")
    Integer getLatestProcessSnapshotVersion(@Param("originProcessId") String originProcessId);
}
