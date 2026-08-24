package ch.admin.bit.jeap.processcontext.domain.maintenance;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessDataValue;

import java.util.List;

public record BackfillJobEntry(String originProcessId, List<ProcessDataValue> processData) {

    public BackfillJobEntry {
        if (processData != null) {
            processData = List.copyOf(processData);
        }
    }
}
