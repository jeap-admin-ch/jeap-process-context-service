package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.List;
import java.util.UUID;

public interface ProcessDataRepository {
    /**
     * Find ProcessData by processInstance, type, key and optional role
     * @param processInstance Process instance to search data for
     * @param processDataKey Process data key
     * @param processDataRole Optional, may be null
     * @return List of matching ProcessData, may be empty
     */
    List<ProcessData> findProcessData(ProcessInstance processInstance,  String processDataKey, String processDataRole);

    List<ProcessData> findByProcessInstanceId(UUID processInstanceId);

    /**
     * Saves the given process data if it does not already exist (based on the unique constraint
     * on process_instance_id, key, value, role).
     *
     * @return true if the ProcessData has been saved, or null if it already existed
     */
    boolean saveIfNew(ProcessData processData);

}
