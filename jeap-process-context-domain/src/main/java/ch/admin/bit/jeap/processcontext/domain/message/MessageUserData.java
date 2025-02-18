package ch.admin.bit.jeap.processcontext.domain.message;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Objects;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@EqualsAndHashCode
public class MessageUserData {

    public static final String KEY_ID = "id";
    public static final String KEY_FAMILY_NAME = "familyName";
    public static final String KEY_GIVEN_NAME = "givenName";
    public static final String KEY_BUSINESS_PARTNER_NAME = "businessPartnerName";
    public static final String KEY_BUSINESS_PARTNER_ID = "businessPartnerId";

    @NotNull
    @Column(name = "key_")
    private String key;

    @NotNull
    @Column(name = "value_")
    private String value;

    public MessageUserData(String key, String value) {
        Objects.requireNonNull(key, "Key is mandatory.");
        Objects.requireNonNull(value, "Value is mandatory.");
        this.key = key;
        this.value = value;
    }

}
