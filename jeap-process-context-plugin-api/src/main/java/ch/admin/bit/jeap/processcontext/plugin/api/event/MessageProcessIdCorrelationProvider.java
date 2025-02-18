package ch.admin.bit.jeap.processcontext.plugin.api.event;

import ch.admin.bit.jeap.messaging.model.Message;
import lombok.EqualsAndHashCode;

/**
 * Default implementation of {@link MessageCorrelationProvider} that correlates messages to processes by
 * using the (optional) processId field on {@link Message#getOptionalProcessId() Message}. If no process
 * ID is found, the event will be ignored.
 * <br>
 * Using this implementation is mostly useful for milestone or task completion conditions that rely on certain events
 * to be present in the process context, without correlating them to a specific task.
 */
@EqualsAndHashCode
public class MessageProcessIdCorrelationProvider implements MessageCorrelationProvider<Message> {
}
