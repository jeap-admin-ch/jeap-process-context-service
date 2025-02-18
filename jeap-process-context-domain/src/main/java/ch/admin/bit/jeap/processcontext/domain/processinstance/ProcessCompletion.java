package ch.admin.bit.jeap.processcontext.domain.processinstance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;


@Data
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessCompletion {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "process_completion_conclusion")
    ProcessCompletionConclusion conclusion;

    @NotNull
    @Column(name = "process_completion_name")
    String name;

    @NotNull
    @Column(name = "process_completion_at")
    ZonedDateTime completedAt;

}