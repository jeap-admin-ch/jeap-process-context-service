package ch.admin.bit.jeap.processcontext.plugin.api.message;

import ch.admin.bit.jeap.messaging.model.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EmptySetMessageExtractorTest {

    @Test
    void getMessageData() {
        assertTrue(new EmptySetMessageExtractor().getMessageData(mock(Message.class)).isEmpty());
    }

}
