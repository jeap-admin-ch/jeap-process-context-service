package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.MessagePayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EmptySetPayloadExtractorTest {

    @Test
    void getEventData() {
        assertTrue(new EmptySetPayloadExtractor().getMessageData(mock(MessagePayload.class)).isEmpty());
    }

}
