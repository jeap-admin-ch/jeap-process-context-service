package ch.admin.bit.jeap.processcontext.plugin.api.context;

public enum TaskState {

    NOT_PLANNED(false),
    PLANNED(false),
    COMPLETED(true),
    NOT_REQUIRED(true),
    DELETED(true),
    UNKNOWN(false);

    private final boolean finalState;

    TaskState(boolean isFinalState) {
        this.finalState = isFinalState;
    }

    public boolean isFinalState() {
        return finalState;
    }

}
