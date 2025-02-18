package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.MessageReferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EmptySetReferenceExtractorTest {

    @Test
    void getEventData() {
        assertTrue(new EmptySetReferenceExtractor().getMessageData(mock(MessageReferences.class)).isEmpty());
    }

}
