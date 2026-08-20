package ch.admin.bit.jeap.processcontext.domain.maintenance;

public enum MaintenanceTaskState {
    CREATED,
    EVENT_QUEUED,
    PROCESSING,
    SUCCEEDED,
    NOT_FOUND,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == NOT_FOUND || this == FAILED;
    }
}
