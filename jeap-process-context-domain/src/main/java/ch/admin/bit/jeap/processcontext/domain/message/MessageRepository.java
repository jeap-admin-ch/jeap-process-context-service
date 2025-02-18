package ch.admin.bit.jeap.processcontext.domain.message;


import java.util.Set;
import java.util.UUID;

public interface MessageRepository extends MessageQueryRepository {
    Message save(Message message);

    void deleteMessageDataByMessageIds(Set<UUID> messageIds);

    void deleteMessageUserDataByMessageIds(Set<UUID> messageIds);

    void deleteOriginTaskIdByMessageIds(Set<UUID> messageIds);

    void deleteMessageByIds(Set<UUID> messageIds);

}
