package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessContext;

/**
 * Check whether a process has completed (or not) and how and why it completed.
 */
public interface ProcessCompletionCondition {

    /**
     * Did the process represented by the given process context complete?
     *
     * @param processContext The process context of the process to check for completion.
     * @return The result of the completion check.
     */
    ProcessCompletionConditionResult isProcessCompleted(ProcessContext processContext);

}
