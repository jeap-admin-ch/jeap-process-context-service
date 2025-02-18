package ch.admin.bit.jeap.processcontext;

import ch.admin.bit.jeap.processcontext.domain.processinstance.ProcessInstanceRepository;
import ch.admin.bit.jeap.processcontext.domain.processupdate.ProcessUpdateRepository;
import ch.admin.bit.jeap.processcontext.event.test5.Test5CreatingProcessInstanceEvent;
import ch.admin.bit.jeap.processcontext.processinstantiation.TestProcessInstantiationCondition;
import ch.admin.bit.jeap.processcontext.testevent.Test5CreatingProcessInstanceEventBuilder;
import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.resource.extension.WithAuthentication;
import com.fasterxml.uuid.Generators;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Slf4j
class ProcessInstanceCreatedByDomainMessageConditionalIT extends ProcessInstanceMockS3ITBase {

    private static final String PROCESS_TEMPLATE_NAME = "domainEventTriggersProcessInstantiationConditional";

    @Autowired
    private ProcessInstanceRepository processInstanceRepository;

    @Autowired
    private ProcessUpdateRepository processUpdateRepository;


    public JeapAuthenticationToken viewAndCreateRoleToken() {
        return super.viewAndCreateRoleToken();
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void test_whenReceivedDomainMeetsCondition_thenProcessInstanceCreated() {
        final String originProcessIdForConditionMet = Generators.timeBasedEpochGenerator().generate().toString();
        assertThat(processInstanceRepository.existsByOriginProcessId(originProcessIdForConditionMet)).isFalse();

        // Send event that meets the condition to trigger a process instantiation
        sendTest5CreatingProcessInstanceEvent(originProcessIdForConditionMet, TestProcessInstantiationCondition.TRIGGER);

        assertProcessInstanceCreatedEvent(originProcessIdForConditionMet, PROCESS_TEMPLATE_NAME);
    }

    @Test
    @WithAuthentication("viewAndCreateRoleToken")
    void test_whenReceivedDomainDoesNotMeetCondition_thenEventProcessedButNoProcessInstanceCreated() {
        final String originProcessIdForConditionNotMet = Generators.timeBasedEpochGenerator().generate().toString();
        assertThat(processInstanceRepository.existsByOriginProcessId(originProcessIdForConditionNotMet)).isFalse();
        assertThat(processUpdateRepository.countAllByOriginProcessIdIn(Set.of(originProcessIdForConditionNotMet))).isEqualTo(0);

        // Send event that does not meet the condition to trigger a process instantiation
        sendTest5CreatingProcessInstanceEvent(originProcessIdForConditionNotMet, TestProcessInstantiationCondition.NO_TRIGGER);


        waitForProcessUpdateToBeCreated(originProcessIdForConditionNotMet);
        assertThatThrownBy(() -> waitForProcessToBeCreated(originProcessIdForConditionNotMet, Duration.ofSeconds(5)))
                .isInstanceOf(ConditionTimeoutException.class);
    }

    private void sendTest5CreatingProcessInstanceEvent(String originProcessId, String trigger) {
        Test5CreatingProcessInstanceEvent event = Test5CreatingProcessInstanceEventBuilder
                .createForProcessId(originProcessId)
                .trigger(trigger)
                .build();
       sendSync("topic.test5creatingprocessinstance", event);
    }

    private void waitForProcessUpdateToBeCreated(String originProcessId) {
        Awaitility.await()
                .pollInSameThread()
                .atMost(TIMEOUT)
                .until(() -> processUpdateRepository.countAllByOriginProcessIdIn(Set.of(originProcessId)) == 1);
    }

    private void waitForProcessToBeCreated(String originProcessId, Duration duration) {
        Awaitility.await()
                .pollInSameThread()
                .atMost(duration)
                .until(() -> processInstanceRepository.existsByOriginProcessId(originProcessId));
    }

}
