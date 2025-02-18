package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.model.Message;
import org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class EventListenerStub<T extends Message> implements MessageListener<T> {

    protected static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final List<T> events = new CopyOnWriteArrayList<>();

    @Override
    public void receive(T event) {
        synchronized (events) {
            events.add(event);
        }
    }

    List<T> peekEvents() {
        synchronized (events) {
            return events;
        }
    }

    T awaitEvent(Predicate<T> predicate) {
        Awaitility.waitAtMost(TIMEOUT)
                .until(() -> events.stream().anyMatch(predicate));

        synchronized (events) {
            return events.stream().filter(predicate).findFirst().orElseThrow();
        }
    }

    public boolean noneMatch(Predicate<T> predicate) {
        synchronized (events) {
            return events.stream().noneMatch(predicate);
        }
    }
}
