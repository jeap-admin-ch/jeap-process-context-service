package ch.admin.bit.jeap.processcontext.domain.processinstance;

import java.util.List;

// Note: This will be removed with JEAP-6538 when process data is separated from the ProcessInstance entity
public record AddedMessage(MessageReferenceMessageDTO messageReference, List<ProcessData> newProcessData) {
}
