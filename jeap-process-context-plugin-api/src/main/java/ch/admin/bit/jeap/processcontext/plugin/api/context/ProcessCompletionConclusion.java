package ch.admin.bit.jeap.processcontext.plugin.api.context;

public enum ProcessCompletionConclusion {

    // process completed regularly
    SUCCEEDED,

    // process was cancelled on purpose in a controlled manner leaving the participating systems in a consistent state
    CANCELLED,

    // process was aborted because of a problem leaving the participating systems in a possibly inconsistent state
    ABORTED

}
