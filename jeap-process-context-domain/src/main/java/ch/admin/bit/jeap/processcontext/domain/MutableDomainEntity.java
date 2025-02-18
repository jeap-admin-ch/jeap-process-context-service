package ch.admin.bit.jeap.processcontext.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.ZonedDateTime;

@MappedSuperclass
@SuppressWarnings({"java:S1068", "findbugs:URF_UNREAD_FIELD"}) // modifiedAt are never read, only used by JPA
public abstract class MutableDomainEntity extends ImmutableDomainEntity {

    @Getter(value = AccessLevel.PROTECTED)
    private ZonedDateTime modifiedAt;

    @SuppressWarnings("unused")
    @Version
    private int version;

    @PreUpdate
    void onPreUpdate() {
        modifiedAt = ZonedDateTime.now();
    }
}
