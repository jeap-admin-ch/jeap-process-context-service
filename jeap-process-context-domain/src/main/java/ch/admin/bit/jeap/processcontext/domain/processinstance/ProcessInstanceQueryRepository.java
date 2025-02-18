package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProcessInstanceQueryRepository {

    boolean existsByOriginProcessId(String originProcessId);

    /**
     * For the given origin process id, find and load the corresponding process instance and its associated messages.
     *
     * @param originProcessId Origin process id of the process instance to load
     * @return The process instance with associated messages
     */
    Optional<ProcessInstance> findByOriginProcessIdLoadingMessages(String originProcessId);

    /**
     * For the given origin process id, find and load the corresponding process instance but forgo the loading of its
     * associated messages. Use only if the associated messages are not required for the intended processing on the process
     * instance.
     *
     * @param originProcessId Origin process id of the process instance to load
     * @return The process instance with associated messages
     */
    Optional<ProcessInstance> findByOriginProcessIdWithoutLoadingMessages(String originProcessId);


    /**
     * For the given origin process id, find and load the corresponding process instance summary. The instance summary
     * is only a small subset of some process instance attributes.
     * @param originProcessId Origin process id of the process instance summary to load
     * @return A small subset of the process instance entity. The process instance summary contains only the template
     * name, id and state
     */
    Optional<ProcessInstanceSummary> findProcessInstanceSummaryByOriginProcessId(String originProcessId);

    Optional<String> getProcessTemplateNameByOriginProcessId(String originProcessId);

    Page<ProcessInstance> findAll(Pageable pageable);

    Page<ProcessInstance> findByProcessData(String processData, Pageable pageable);

    /**
     * Find all not yet completed process instances of a given process template that have the given process data.
     *
     * @param processTemplateName Name of the process template the process instances must belong to.
     * @param processDataKey Key of the process data to look for.
     * @param processDataValue Value of the process data.
     * @param processDataRole Role of the process data. May be null.
     * @return The origin process ids of the process instances found.
     */
    Set<String> findUncompletedProcessInstancesHavingProcessData(String processTemplateName, String processDataKey, String processDataValue, String processDataRole);

    Slice<ProcessInstanceQueryResult> findProcessInstances(ProcessState state, ZonedDateTime modifiedAt, Pageable pageable);

    Slice<String> findUncompletedProcessInstanceOriginIdsByTemplateHashChanged(
            ZonedDateTime lastModifiedAfter, ProcessTemplate template, Pageable ofSize);

    /**
     * Get information about the process template associated with the process instance having the given origin process id.
     * @param originProcessId origin process id of the process instance to get the template information from.
     * @return The template information associated  with the process instance having the given origin process id, if a process
     *         instance with that origin process id exists.
     */
    Optional<ProcessInstanceTemplate> findProcessInstanceTemplate(String originProcessId);

    List<MessageReferenceMessageDTO> findMessageReferenceMessageDTOs(UUID processInstanceId);

    Optional<Integer> getLatestProcessSnapshotVersion(String originProcessId);
}
