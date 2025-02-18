package ch.admin.bit.jeap.processcontext.domain.message;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@EqualsAndHashCode
public class OriginTaskId {

    @NotNull
    private String templateName;

    @NotNull
    private String originTaskId;

    OriginTaskId(String templateName, String originTaskId) {
        Objects.requireNonNull(templateName, "Template name is mandatory.");
        Objects.requireNonNull(originTaskId, "Origin task id is mandatory.");
        this.templateName = templateName;
        this.originTaskId = originTaskId;
    }

    public static OriginTaskId from(String templateName, String originTaskId) {
        return new OriginTaskId(templateName, originTaskId);
    }

    public static Set<OriginTaskId> from(String templateName, Collection<String> originTaskIds) {
        return originTaskIds.stream().map(originTaskId -> from(templateName, originTaskId)).collect(Collectors.toSet());
    }

}
