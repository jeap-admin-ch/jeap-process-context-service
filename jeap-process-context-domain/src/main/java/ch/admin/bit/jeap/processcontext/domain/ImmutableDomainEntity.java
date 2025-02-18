package ch.admin.bit.jeap.processcontext.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.ZonedDateTime;

@MappedSuperclass
@SuppressWarnings({"java:S1068", "findbugs:URF_UNREAD_FIELD"}) // createdA are never read, only used by JPA
public abstract class ImmutableDomainEntity {

    @Getter(value = AccessLevel.PROTECTED)
    protected ZonedDateTime createdAt;

    @PrePersist
    void onPrePersist() {
        // Allow setting createdAt manually, too
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }

}
