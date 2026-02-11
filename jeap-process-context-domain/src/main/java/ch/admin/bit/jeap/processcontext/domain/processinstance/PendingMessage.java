package ch.admin.bit.jeap.processcontext.domain.processinstance;


import ch.admin.bit.jeap.processcontext.domain.ImmutableDomainEntity;
import ch.admin.bit.jeap.processcontext.domain.message.Message;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@SuppressWarnings({"FieldMayBeFinal", "JpaDataSourceORMInspection"}) // JPA spec mandates non-final fields
@NoArgsConstructor(access = PROTECTED) // for JPA
@AllArgsConstructor(access = PRIVATE)
@Getter
@ToString
@Entity
@Table(name = "pending_message")
public class PendingMessage extends ImmutableDomainEntity {

    @Id
    private Long id;

    @Getter
    private String originProcessId;

    @Getter
    private UUID messageId;

    public static PendingMessage from(Message message, String originProcessId) {
        return new PendingMessage(null, originProcessId, message.getId());
    }
}
