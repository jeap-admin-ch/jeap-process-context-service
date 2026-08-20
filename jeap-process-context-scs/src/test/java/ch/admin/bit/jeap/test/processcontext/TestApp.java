package ch.admin.bit.jeap.test.processcontext;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContract;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContract;
import ch.admin.bit.jeap.processcontext.internal.event.outdated.ProcessContextOutdatedEvent;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@JeapMessageConsumerContract(value = ProcessContextOutdatedEvent.TypeRef.class, topic = "outdated")
@JeapMessageProducerContract(value = ProcessContextOutdatedEvent.TypeRef.class, topic = "outdated")
public class TestApp {
}
