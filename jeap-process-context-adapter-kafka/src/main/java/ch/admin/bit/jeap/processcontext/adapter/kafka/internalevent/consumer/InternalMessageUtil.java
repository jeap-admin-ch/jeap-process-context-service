package ch.admin.bit.jeap.processcontext.adapter.kafka.internalevent.consumer;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;

import java.util.function.Consumer;

@UtilityClass
@Slf4j
class InternalMessageUtil {

    static void handleAndAcknowledge(String originProcessId, Acknowledgment ack, Consumer<String> originProcessIdConsumer) {
        try {
            originProcessIdConsumer.accept(originProcessId);
            ack.acknowledge();
        } catch (Exception e) {
            log.warn("Exception while handling internal message", e);
            throw InternalMessageConsumerException.from(e);
        }
    }
}
