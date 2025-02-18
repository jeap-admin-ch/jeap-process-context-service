package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.Message;

/**
 * @deprecated Replaced by {@link MessageCorrelationProvider}
 */
@Deprecated(since = "7.0.0", forRemoval = true)
public interface DomainEventCorrelationProvider<M extends Message> extends MessageCorrelationProvider<M> {

}
