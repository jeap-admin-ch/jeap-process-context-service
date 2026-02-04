package ch.admin.bit.jeap.processcontext.domain.processinstance;

public enum TaskState {
    NOT_PLANNED,
    PLANNED,
    COMPLETED,
    NOT_REQUIRED,
    DELETED,
    /** New static task added due to a template migration, unknown if completed or not */
    UNKNOWN;

    public boolean isFinalState() {
        return this == COMPLETED || this == NOT_REQUIRED || this == DELETED;
    }
}
