package ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer;

import ch.admin.bit.jeap.domainevent.avro.AvroDomainEventBuilder;
import ch.admin.bit.jme.test.JmeSimpleTestEvent;
import ch.admin.bit.jme.test.JmeSimpleTestEventPayload;
import ch.admin.bit.jme.test.JmeSimpleTestEventReferences;
import lombok.Getter;

@Getter
public class JmeSimpleTestEventBuilder extends AvroDomainEventBuilder<JmeSimpleTestEventBuilder, JmeSimpleTestEvent> {
    private String serviceName;
    private final String systemName = "JEAP";
    private String message;

    private JmeSimpleTestEventBuilder() {
        super(JmeSimpleTestEvent::new);
    }

    public static JmeSimpleTestEventBuilder create() {
        return new JmeSimpleTestEventBuilder();
    }

    public JmeSimpleTestEventBuilder message(String message) {
        this.message = message;
        return self();
    }

    public JmeSimpleTestEventBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return self();
    }

    @Override
    protected JmeSimpleTestEventBuilder self() {
        return this;
    }

    @Override
    public JmeSimpleTestEvent build() {
        setReferences(JmeSimpleTestEventReferences.newBuilder().build());
        setPayload(JmeSimpleTestEventPayload.newBuilder().setMessage(message).build());
        return super.build();
    }
}
