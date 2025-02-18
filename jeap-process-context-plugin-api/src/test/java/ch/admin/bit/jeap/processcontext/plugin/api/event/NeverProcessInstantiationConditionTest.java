package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.Message;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NeverProcessInstantiationConditionTest {

    @Test
    void testTriggersProcessInstantiation() {
        ProcessInstantiationCondition<Message> condition = new NeverProcessInstantiationCondition();
        assertFalse(condition.triggersProcessInstantiation(Mockito.mock(Message.class)));
    }

}
