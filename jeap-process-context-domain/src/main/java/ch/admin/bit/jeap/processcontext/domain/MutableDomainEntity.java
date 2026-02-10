package ch.admin.bit.jeap.processcontext.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.ZonedDateTime;

@MappedSuperclass
public abstract class MutableDomainEntity extends ImmutableDomainEntity {

    @Getter(value = AccessLevel.PROTECTED)
    protected ZonedDateTime modifiedAt;

    @SuppressWarnings("unused")
    @Version
    private int version;

    @PreUpdate
    void onPreUpdate() {
        modifiedAt = ZonedDateTime.now();
    }
}
