package ch.admin.bit.jeap.processcontext.adapter.kafka;

import ch.admin.bit.jeap.messaging.kafka.contract.ContractsProvider;
import ch.admin.bit.jeap.messaging.kafka.contract.NoContractException;
import ch.admin.bit.jeap.messaging.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessContextContractsValidatorTest {

    @Mock
    private ContractsProvider contractsProvider;

    private ProcessContextContractsValidator validator;

    @BeforeEach
    void setUp() {
        when(contractsProvider.getContracts()).thenReturn(List.of());
        validator = new ProcessContextContractsValidator("test", contractsProvider);
    }

    @Test
    void internalMessageDoesNotRequireApplicationContracts() {
        MessageType type = messageType("ProcessContextOutdatedEvent");

        assertThatCode(() -> validator.ensurePublisherContract(type, "outdated")).doesNotThrowAnyException();
        assertThatCode(() -> validator.ensureConsumerContract(type, "outdated")).doesNotThrowAnyException();
    }

    @Test
    void applicationMessageStillRequiresContracts() {
        MessageType type = messageType("ApplicationEvent");

        assertThatThrownBy(() -> validator.ensurePublisherContract(type, "application-topic"))
                .isInstanceOf(NoContractException.class);
        assertThatThrownBy(() -> validator.ensureConsumerContract(type, "application-topic"))
                .isInstanceOf(NoContractException.class);
    }

    private static MessageType messageType(String name) {
        MessageType type = mock(MessageType.class);
        when(type.getName()).thenReturn(name);
        return type;
    }
}
