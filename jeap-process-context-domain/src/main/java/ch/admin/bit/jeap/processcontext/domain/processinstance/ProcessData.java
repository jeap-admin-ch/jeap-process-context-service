package ch.admin.bit.jeap.processcontext.domain.processinstance;

import ch.admin.bit.jeap.processcontext.domain.ImmutableDomainEntity;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import static lombok.AccessLevel.PACKAGE;

@Entity
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
@Table(name="process_instance_process_data")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessData extends ImmutableDomainEntity {

    @Id
    @NotNull
    @EqualsAndHashCode.Exclude
    private UUID id = Generators.timeBasedEpochGenerator().generate();

    @Setter(PACKAGE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_instance_id")
    private ProcessInstance processInstance;

    @NotNull
    @Column(name = "key_")
    private String key;

    @NotNull
    @Column(name = "value_")
    private String value;

    private String role;

    public ProcessData(String key, String value) {
        Objects.requireNonNull(key, "Key is mandatory.");
        Objects.requireNonNull(value, "Value is mandatory.");
        this.key = key;
        this.value = value;
        this.role = null;
        this.createdAt = ZonedDateTime.now();
    }

    public ProcessData(String key, String value, String role) {
        this(key, value);
        this.role = role;
    }

    public ZonedDateTime getCreatedAt() {
        return super.getCreatedAt();
    }
}
