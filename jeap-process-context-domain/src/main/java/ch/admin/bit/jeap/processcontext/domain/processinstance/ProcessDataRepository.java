package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.List;

public interface ProcessDataRepository {
    /**
     * Find ProcessData by processInstance, type, key and optional role
     * @param processInstance Process instance to search data for
     * @param processDataKey Process data key
     * @param processDataRole Optional, may be null
     * @return List of matching ProcessData, may be empty
     */
    List<ProcessData> findProcessData(ProcessInstance processInstance,  String processDataKey, String processDataRole);
}
