package ch.admin.bit.jeap.processcontext.adapter.kafka.command.consumer;

import ch.admin.bit.jeap.processcontext.adapter.kafka.TopicConfiguration;
import ch.admin.bit.jeap.processcontext.adapter.kafka.message.consumer.KafkaEventListenerException;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.CreateProcessInstanceCommand;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.CreateProcessInstanceCommandPayload;
import ch.admin.bit.jeap.processcontext.command.process.instance.create.ProcessData;
import ch.admin.bit.jeap.processcontext.domain.port.MetricsListener;
import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateProcessInstanceCommandConsumer {
    private final ProcessInstanceService processInstanceService;
    private final MetricsListener metricsListener;

    @KafkaListener(
            topics = TopicConfiguration.CREATE_PROCESS_INSTANCE_TOPIC_NAME,
            groupId = "${spring.application.name}-create-process-instance")
    public void consume(final CreateProcessInstanceCommand command, Acknowledgment ack) {
        try {
            final CreateProcessInstanceCommandPayload payload = command.getPayload();

            metricsListener.commandReceived(command.getType());
            processInstanceService.createProcessInstance(
                    command.getProcessId(),
                    payload.getProcessTemplateName(),
                    toProcessData(payload.getProcessData()));
        } catch (Exception ex) {
            throw KafkaEventListenerException.from(ex);
        }
        ack.acknowledge();
    }

    private Set<ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData> toProcessData(List<ProcessData> processData) {
        if (processData == null) {
            return Collections.emptySet();
        }
        return processData.stream().map(pd -> new ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessData(pd.getKey(), pd.getValue(), pd.getRole())).collect(Collectors.toSet());
    }

}
