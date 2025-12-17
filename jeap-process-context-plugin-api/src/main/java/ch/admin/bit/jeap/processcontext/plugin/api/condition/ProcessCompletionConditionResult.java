package ch.admin.bit.jeap.processcontext.plugin.api.condition;

import ch.admin.bit.jeap.processcontext.plugin.api.context.ProcessCompletionConclusion;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessCompletionConditionResult {

    @Getter
    private final boolean completed;
    private ProcessCompletionConclusion conclusion;
    private String name;

    /**
     * Completion result constant to report that a process did not yet complete.
     */
    public static final ProcessCompletionConditionResult IN_PROGRESS = new ProcessCompletionConditionResult(false, null, null);

    /**
     * Builder for a completion result to report that a process completed.
     * @param conclusion Indicate how the process completed
     * @param name completion name to define the completion uniquely
     * @return The completion result built
     */
    @Builder(builderMethodName="completedBuilder")
    private static ProcessCompletionConditionResult completed(@NotNull ProcessCompletionConclusion conclusion, @NotNull String name) {
        return new ProcessCompletionConditionResult(true, conclusion, name);
    }

    public Optional<ProcessCompletionConclusion> getConclusion() {
        return Optional.ofNullable(conclusion);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

}
