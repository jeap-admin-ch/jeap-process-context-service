package ch.admin.bit.jeap.processcontext.domain.maintenance;

public enum MaintenanceTaskState {
    CREATED,
    COMMAND_QUEUED,
    EVENT_QUEUED,
    SUCCEEDED,
    NOT_FOUND,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == NOT_FOUND || this == FAILED;
    }
}
