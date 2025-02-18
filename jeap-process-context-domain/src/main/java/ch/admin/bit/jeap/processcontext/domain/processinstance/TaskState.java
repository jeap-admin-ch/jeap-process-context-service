package ch.admin.bit.jeap.processcontext.domain.processinstance;

public enum TaskState {
    NOT_PLANNED,
    PLANNED,
    COMPLETED,
    NOT_REQUIRED,
    DELETED,
    UNKNOWN;

    public boolean isFinalState() {
        return this == COMPLETED || this == NOT_REQUIRED || this == DELETED;
    }
}
