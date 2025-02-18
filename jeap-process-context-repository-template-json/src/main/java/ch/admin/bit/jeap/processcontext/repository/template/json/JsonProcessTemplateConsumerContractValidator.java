package ch.admin.bit.jeap.processcontext.repository.template.json;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsValidator;
import ch.admin.bit.jeap.processcontext.domain.processtemplate.ProcessTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class JsonProcessTemplateConsumerContractValidator {

    private final ContractsValidator contractsValidator;

    protected void validateContract(ProcessTemplate processTemplate) {
        processTemplate.getMessageReferences().forEach(messageReference -> {
            contractsValidator.ensureConsumerContract(messageReference.getMessageName(), messageReference.getTopicName());
        });
    }
}
