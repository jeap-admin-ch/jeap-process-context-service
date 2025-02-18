package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.Message;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AlwaysProcessInstantiationConditionTest {

    @Test
    void testTriggersProcessInstantiation() {
        ProcessInstantiationCondition<Message> condition = new AlwaysProcessInstantiationCondition();
        assertTrue(condition.triggersProcessInstantiation(Mockito.mock(Message.class)));
    }

}
