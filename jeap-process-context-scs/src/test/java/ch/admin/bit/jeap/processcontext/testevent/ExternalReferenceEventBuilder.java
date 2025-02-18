package ch.admin.bit.jeap.processcontext.testevent;

import ch.admin.bit.jeap.processcontext.event.externalreference.ExternalReferenceEvent;
import ch.admin.bit.jeap.processcontext.event.externalreference.ExternalReferenceEventPayload;
import ch.admin.bit.jeap.processcontext.event.externalreference.ExternalReferenceEventReferences;


public class ExternalReferenceEventBuilder extends AbstractTestEventBuilder<ExternalReferenceEventBuilder, ExternalReferenceEvent> {

    private String externalReference;

    private ExternalReferenceEventBuilder() {
        super(ExternalReferenceEvent::new, ExternalReferenceEventReferences::new, "ExternalReferenceEvent", null);
    }

    public static ExternalReferenceEventBuilder create() {
        return new ExternalReferenceEventBuilder();
    }

    public ExternalReferenceEventBuilder externalReference(String externalReference) {
        this.externalReference = externalReference;
        return this;
    }

    @Override
    public ExternalReferenceEvent build() {
        ExternalReferenceEvent event = super.build();
        ExternalReferenceEventPayload payload = new ExternalReferenceEventPayload();
        payload.setExternalReference(externalReference);
        event.setPayload(payload);
        return event;
    }
}
