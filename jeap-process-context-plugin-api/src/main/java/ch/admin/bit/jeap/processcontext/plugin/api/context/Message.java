package ch.admin.bit.jeap.processcontext.plugin.api.context;

import ch.admin.bit.jeap.processcontext.plugin.api.event.MessageData;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class Message {

    String name;

    Set<String> relatedOriginTaskIds;

    Set<MessageData> messageData;

}
