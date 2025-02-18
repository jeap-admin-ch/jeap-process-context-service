package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.Message;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class MessageProcessIdCorrelationProviderTest {

    @Test
    void getOriginProcessId() {
        Message message = mock(Message.class);
        doReturn(Optional.of("id")).when(message).getOptionalProcessId();
        MessageProcessIdCorrelationProvider correlationProvider = new MessageProcessIdCorrelationProvider();

        Set<String> originProcessIds = correlationProvider.getOriginProcessIds(message);

        assertEquals(Set.of("id"),originProcessIds);
        assertEquals(Set.of(), correlationProvider.getRelatedOriginTaskIds(message));
    }
}
